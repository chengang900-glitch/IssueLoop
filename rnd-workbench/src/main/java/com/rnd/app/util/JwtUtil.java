package com.rnd.app.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    /** 密钥最小长度（字节）：低于 256 bit 的 HMAC 密钥强度不足，直接拒绝启动而不是补零。 */
    static final int MIN_SECRET_BYTES = 32;

    /** 仓库内置的占位值，一旦被用于签发令牌即视为配置事故。 */
    private static final Set<String> FORBIDDEN_SECRETS = Set.of(
            "CHANGE_ME_PRODUCTION_RANDOM_64_CHARS",
            "changeme", "change-me", "secret", "jwt-secret");

    private final com.rnd.app.config.AppConfig config;
    private Key key;
    private Duration ttl;

    @PostConstruct
    void init() {
        com.rnd.app.config.AppConfig.JwtConfig jwt = config.getJwt();
        if (jwt == null) {
            throw new IllegalStateException("缺少 app.jwt 配置");
        }
        this.key = resolveKey(jwt.getSecret());
        this.ttl = parseTtl(jwt.getTtl());
    }

    private Key resolveKey(String secret) {
        if (secret == null || secret.isBlank()) {
            // 未配置密钥：生成一次性随机密钥，保证“默认配置不可被利用”。
            // 代价是进程重启后已签发令牌全部失效，生产必须显式注入 JWT_SECRET。
            byte[] random = new byte[64];
            new SecureRandom().nextBytes(random);
            log.warn("未配置 app.jwt.secret（环境变量 JWT_SECRET），已生成临时随机密钥："
                    + "进程重启后所有已签发的登录令牌都会失效。生产环境请注入至少 {} 字节的随机密钥。", MIN_SECRET_BYTES);
            return Keys.hmacShaKeyFor(random);
        }
        if (FORBIDDEN_SECRETS.contains(secret.trim())) {
            throw new IllegalStateException("app.jwt.secret 使用了仓库内置的占位值，请通过环境变量 JWT_SECRET 注入随机密钥");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.jwt.secret 至少需要 " + MIN_SECRET_BYTES
                    + " 字节（当前 " + bytes.length + " 字节）；密钥不再自动补零，请配置足够长度的随机密钥");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    private static Duration parseTtl(String value) {
        String text = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (text.isEmpty()) {
            throw new IllegalStateException("app.jwt.ttl 不能为空（示例：8h / 30m）");
        }
        try {
            return Duration.parse(text.startsWith("P") ? text : "PT" + text);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("app.jwt.ttl 格式不正确（示例：8h / 30m / PT8H）：" + value, e);
        }
    }

    public String generate(Long userId, String username, String systemRole, int tokenVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("systemRole", systemRole);
        claims.put("tokenVersion", tokenVersion);

        Instant now = Instant.now();

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> verify(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }
}
