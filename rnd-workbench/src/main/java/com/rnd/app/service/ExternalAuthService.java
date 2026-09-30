package com.rnd.app.service;

import com.rnd.app.config.AppConfig;
import com.rnd.app.entity.AuthLoginTransaction;
import com.rnd.app.entity.ExternalIdentity;
import com.rnd.app.entity.User;
import com.rnd.app.repository.AuthLoginTransactionRepository;
import com.rnd.app.repository.ExternalIdentityRepository;
import com.rnd.app.repository.UserRepository;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import com.rnd.app.util.JwtUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalAuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AppConfig config;
    private final AuthLoginTransactionRepository transactionRepo;
    private final ExternalIdentityRepository identityRepo;
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    public List<String> availableProviders() {
        return Arrays.asList("keycloak", "feishu", "dingtalk", "wecom").stream()
                .filter(provider -> providerConfig(provider).isEnabled() && isConfigured(providerConfig(provider)))
                .toList();
    }

    public String start(String provider) {
        AppConfig.ProviderConfig pc = configuredProvider(provider);
        String state = randomToken(48);
        String verifier = pc.isUsePkce() ? randomToken(64) : null;
        String redirect = redirectUri(provider, pc);
        transactionRepo.save(AuthLoginTransaction.builder().state(state).provider(provider).codeVerifier(verifier)
                .redirectUri(redirect).expiresAt(Instant.now().plus(Duration.ofMinutes(5))).build());
        UriComponentsBuilder uri = UriComponentsBuilder.fromUriString(pc.getAuthorizationUri())
                .queryParam("response_type", "code")
                .queryParam("redirect_uri", redirect).queryParam("scope", pc.getScope()).queryParam("state", state);
        if ("wecom".equalsIgnoreCase(pc.getTokenRequestMode())) uri.queryParam("appid", pc.getClientId());
        else uri.queryParam("client_id", pc.getClientId());
        if (verifier != null) uri.queryParam("code_challenge", challenge(verifier)).queryParam("code_challenge_method", "S256");
        if ("wecom".equalsIgnoreCase(pc.getTokenRequestMode())) uri.fragment("wechat_redirect");
        return uri.build(true).toUriString();
    }

    @Transactional
    public void callback(String provider, String code, String state, String error, String errorDescription) {
        if (state == null || state.isBlank()) throw new BusinessException(ErrorCode.BAD_REQUEST, "登录状态缺失");
        AuthLoginTransaction transaction = transactionRepo.findForUpdate(state)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "登录状态无效或已过期"));
        if (!provider.equals(transaction.getProvider()) || transaction.isConsumed() || transaction.getExpiresAt().isBefore(Instant.now()))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "登录状态无效或已过期");
        if (error != null) {
            transaction.setErrorMessage(errorDescription == null ? error : errorDescription);
            transactionRepo.save(transaction);
            return;
        }
        if (code == null || code.isBlank()) throw new BusinessException(ErrorCode.BAD_REQUEST, "认证平台未返回授权码");
        AppConfig.ProviderConfig pc = configuredProvider(provider);
        try {
            Map<String, Object> token = exchangeCode(pc, code, transaction.getRedirectUri(), transaction.getCodeVerifier());
            String accessToken = text(token, "access_token", "accessToken", "data.access_token", "data.accessToken");
            Map<String, Object> info = accessToken == null ? token : loadUserInfo(pc, accessToken, code);
            ExternalProfile profile = extractProfile(provider, pc, info, token);
            transaction.setExternalSubject(profile.subject); transaction.setExternalDisplayName(profile.displayName);
            transaction.setExternalEmail(profile.email); transaction.setExternalAvatar(profile.avatar);
            identityRepo.findByProviderAndProviderInstanceAndSubject(provider, providerInstance(pc), profile.subject)
                    .ifPresent(identity -> transaction.setLocalUserId(identity.getUserId()));
            transactionRepo.save(transaction);
        } catch (RuntimeException ex) {
            log.warn("External authentication failed for provider {}", provider, ex);
            transaction.setErrorMessage("外部认证失败，请稍后重试");
            transactionRepo.save(transaction);
        }
    }

    @Transactional
    public ExchangeResult exchange(String state) {
        AuthLoginTransaction tx = transactionRepo.findForUpdate(state).orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "登录状态无效"));
        if (tx.getErrorMessage() != null) throw new BusinessException(ErrorCode.BAD_REQUEST, tx.getErrorMessage());
        if (tx.getExpiresAt().isBefore(Instant.now()) || tx.isConsumed() || tx.getExternalSubject() == null)
            throw new BusinessException(ErrorCode.BAD_REQUEST, "登录状态无效或已过期");
        if (tx.getLocalUserId() == null) return ExchangeResult.bindingRequired(state, tx.getProvider(), tx.getExternalDisplayName());
        User user = enabledUser(tx.getLocalUserId());
        tx.setConsumed(true); transactionRepo.save(tx);
        touchIdentity(tx, user.getId());
        return ExchangeResult.success(jwtUtil.generate(user.getId(), user.getUsername(), user.getSystemRole(), user.getTokenVersion()));
    }

    /**
     * 绑定本地账号。
     *
     * <p>注意 noRollbackFor：{@link AuthService#authenticate} 依靠抛出 BusinessException
     * 来传递“口令错误”，但失败计数（fail_count / lock_until）必须在同一个事务里提交，
     * 否则异常会让外层事务整体回滚，撞库防护（5 次锁定）在这条匿名接口上失效。
     * 因此调用 authenticate 的入口都必须声明 noRollbackFor = BusinessException.class。</p>
     */
    @Transactional(noRollbackFor = BusinessException.class)
    public ExchangeResult bind(String state, String username, String password, AuthService authService) {
        AuthLoginTransaction tx = transactionRepo.findForUpdate(state).orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "关联状态无效"));
        if (tx.isConsumed() || tx.getExpiresAt().isBefore(Instant.now()) || tx.getExternalSubject() == null)
            throw new BusinessException(ErrorCode.BAD_REQUEST, "关联状态无效或已过期");
        User user = authService.authenticate(username, password);
        ExternalIdentity identity = identityRepo.findByProviderAndProviderInstanceAndSubject(tx.getProvider(), providerInstance(tx.getProvider()), tx.getExternalSubject()).orElse(null);
        if (identity != null && !identity.getUserId().equals(user.getId())) throw new BusinessException(ErrorCode.STATUS_CONFLICT, "该外部身份已绑定其他账号");
        if (identity == null) identityRepo.save(ExternalIdentity.builder().provider(tx.getProvider()).providerInstance(providerInstance(tx.getProvider())).subject(tx.getExternalSubject()).userId(user.getId()).displayName(tx.getExternalDisplayName()).email(tx.getExternalEmail()).avatar(tx.getExternalAvatar()).lastLoginAt(Instant.now()).build());
        user.setFailCount(0); user.setLockUntil(null); userRepo.save(user);
        tx.setLocalUserId(user.getId()); tx.setConsumed(true); transactionRepo.save(tx);
        return ExchangeResult.success(jwtUtil.generate(user.getId(), user.getUsername(), user.getSystemRole(), user.getTokenVersion()));
    }

    private void touchIdentity(AuthLoginTransaction tx, Long userId) {
        identityRepo.findByProviderAndProviderInstanceAndSubject(tx.getProvider(), providerInstance(tx.getProvider()), tx.getExternalSubject()).ifPresent(identity -> { identity.setUserId(userId); identity.setLastLoginAt(Instant.now()); identityRepo.save(identity); });
    }
    private User enabledUser(Long id) { User user = userRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN)); if (!Integer.valueOf(1).equals(user.getStatus())) throw new BusinessException(ErrorCode.FORBIDDEN, "账号已停用，请联系管理员"); return user; }
    private AppConfig.ProviderConfig configuredProvider(String provider) { AppConfig.ProviderConfig pc = providerConfig(provider); if (!pc.isEnabled() || !isConfigured(pc)) throw new BusinessException(ErrorCode.BAD_REQUEST, "该登录方式尚未完成配置"); return pc; }
    private boolean isConfigured(AppConfig.ProviderConfig pc) { return has(pc.getAuthorizationUri()) && has(pc.getTokenUri()) && has(pc.getUserInfoUri()) && has(pc.getClientId()) && has(pc.getClientSecret()) && (!"feishu".equalsIgnoreCase(pc.getTokenRequestMode()) || has(pc.getAppTokenUri())); }
    private AppConfig.ProviderConfig providerConfig(String provider) { switch (provider) { case "keycloak": return config.getExternalAuth().getKeycloak(); case "feishu": return config.getExternalAuth().getFeishu(); case "dingtalk": return config.getExternalAuth().getDingtalk(); case "wecom": return config.getExternalAuth().getWecom(); default: throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的登录平台"); } }
    private String providerInstance(AppConfig.ProviderConfig pc) { return has(pc.getClientId()) ? pc.getClientId() : "default"; }
    private String providerInstance(String provider) { return providerInstance(providerConfig(provider)); }
    private String redirectUri(String provider, AppConfig.ProviderConfig pc) { return has(pc.getRedirectUri()) ? pc.getRedirectUri() : config.getExternalAuth().getPublicBaseUrl() + "/api/v1/auth/" + provider + "/callback"; }
    private Map<String, Object> exchangeCode(AppConfig.ProviderConfig pc, String code, String redirect, String verifier) {
        RestTemplate rest = new RestTemplate();
        if ("wecom".equalsIgnoreCase(pc.getTokenRequestMode())) {
            String uri = UriComponentsBuilder.fromUriString(pc.getTokenUri()).queryParam("corpid", pc.getClientId()).queryParam("corpsecret", pc.getClientSecret()).queryParam("code", code).build(true).toUriString();
            Map<String, Object> result = rest.getForObject(uri, Map.class); return result == null ? Collections.emptyMap() : result;
        }
        if ("feishu".equalsIgnoreCase(pc.getTokenRequestMode())) {
            Map<String, Object> appTokenBody = new LinkedHashMap<>(); appTokenBody.put("app_id", pc.getClientId()); appTokenBody.put("app_secret", pc.getClientSecret());
            Map<String, Object> appTokenResult = rest.postForObject(pc.getAppTokenUri(), new HttpEntity<>(appTokenBody, headers(MediaType.APPLICATION_JSON)), Map.class);
            String appAccessToken = text(appTokenResult == null ? Collections.emptyMap() : appTokenResult, "app_access_token", "data.app_access_token");
            if (!has(appAccessToken)) throw new IllegalStateException("Feishu app access token missing");
            Map<String, Object> body = new LinkedHashMap<>(); body.put("grant_type", "authorization_code"); body.put("code", code); body.put("app_id", pc.getClientId()); body.put("app_secret", pc.getClientSecret());
            if (has(redirect)) body.put("redirect_uri", redirect); if (verifier != null) body.put("code_verifier", verifier);
            HttpHeaders requestHeaders = headers(MediaType.APPLICATION_JSON); requestHeaders.setBearerAuth(appAccessToken);
            Map<String, Object> result = rest.postForObject(pc.getTokenUri(), new HttpEntity<>(body, requestHeaders), Map.class); return result == null ? Collections.emptyMap() : result;
        }
        if ("dingtalk".equalsIgnoreCase(pc.getTokenRequestMode())) {
            Map<String, Object> body = new LinkedHashMap<>(); body.put("clientId", pc.getClientId()); body.put("clientSecret", pc.getClientSecret()); body.put("code", code); body.put("grantType", "authorization_code");
            Map<String, Object> result = rest.postForObject(pc.getTokenUri(), new HttpEntity<>(body, headers(MediaType.APPLICATION_JSON)), Map.class); return result == null ? Collections.emptyMap() : result;
        }
        if ("json".equalsIgnoreCase(pc.getTokenRequestMode())) {
            Map<String, Object> body = new LinkedHashMap<>(); body.put("grant_type", "authorization_code"); body.put("client_id", pc.getClientId()); body.put("client_secret", pc.getClientSecret()); body.put("code", code); if (has(redirect)) body.put("redirect_uri", redirect); if (verifier != null) body.put("code_verifier", verifier);
            Map<String, Object> result = rest.postForObject(pc.getTokenUri(), new HttpEntity<>(body, headers(MediaType.APPLICATION_JSON)), Map.class); return result == null ? Collections.emptyMap() : result;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>(); form.add("grant_type", "authorization_code"); form.add("client_id", pc.getClientId()); form.add("client_secret", pc.getClientSecret()); form.add("code", code); form.add("redirect_uri", redirect); if (verifier != null) form.add("code_verifier", verifier);
        Map<String, Object> result = rest.postForObject(pc.getTokenUri(), new HttpEntity<>(form, headers(MediaType.APPLICATION_FORM_URLENCODED)), Map.class); return result == null ? Collections.emptyMap() : result;
    }
    private Map<String, Object> loadUserInfo(AppConfig.ProviderConfig pc, String token, String code) {
        RestTemplate rest = new RestTemplate(); String uri = pc.getUserInfoUri().replace("{access_token}", java.net.URLEncoder.encode(token, StandardCharsets.UTF_8)).replace("{code}", java.net.URLEncoder.encode(code, StandardCharsets.UTF_8)); HttpHeaders headers = new HttpHeaders(); if (!pc.getUserInfoUri().contains("{access_token}")) headers.setBearerAuth(token); ResponseEntity<Map> result = rest.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), Map.class); return result.getBody() == null ? Collections.emptyMap() : result.getBody();
    }
    private ExternalProfile extractProfile(String provider, AppConfig.ProviderConfig pc, Map<String, Object> info, Map<String, Object> token) {
        String subject = text(info, "sub", "open_id", "openId", "union_id", "unionId", "unionid", "userid", "userId", "UserId", "user_id", "OpenId", "openid", "external_userid", "data.open_id"); if (subject == null) subject = text(token, "sub", "open_id", "openId", "union_id", "unionId");
        if (subject == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "认证平台未返回稳定用户标识");
        return new ExternalProfile(subject, text(info, "name", "en_name", "nick", "nickname", "display_name", "data.name"), text(info, "email", "data.email"), text(info, "avatar_url", "avatarUrl", "avatar", "picture", "data.avatar_url"));
    }
    private HttpHeaders headers(MediaType type) { HttpHeaders h = new HttpHeaders(); h.setContentType(type); return h; }
    private String text(Map<String, Object> map, String... paths) { for (String path : paths) { Object value = map; for (String part : path.split("\\.")) { if (!(value instanceof Map)) { value = null; break; } value = ((Map<?, ?>) value).get(part); } if (value != null && !value.toString().isBlank()) return value.toString(); } return null; }
    private static boolean has(String value) { return value != null && !value.isBlank(); }
    private static String randomToken(int bytes) { byte[] b = new byte[bytes]; RANDOM.nextBytes(b); return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    private static String challenge(String verifier) { try { return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII))); } catch (Exception e) { throw new IllegalStateException(e); } }

    @Data @AllArgsConstructor
    public static class ExchangeResult { private boolean bindingRequired; private String token; private String bindingToken; private String provider; private String displayName; static ExchangeResult success(String token) { return new ExchangeResult(false, token, null, null, null); } static ExchangeResult bindingRequired(String state, String provider, String name) { return new ExchangeResult(true, null, state, provider, name); } }
    @Data @AllArgsConstructor private static class ExternalProfile { private String subject; private String displayName; private String email; private String avatar; }
}
