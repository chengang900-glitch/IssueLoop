package com.rnd.app.controller;

import com.rnd.app.dto.AuthDto;
import com.rnd.app.dto.ThirdPartyLoginProvidersDto;
import com.rnd.app.dto.ThirdPartyLoginSettingsDto;
import com.rnd.app.service.AuthService;
import com.rnd.app.service.SystemSettingService;
import com.rnd.app.service.ExternalAuthService;
import com.rnd.app.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SystemSettingService settingService;
    private final ExternalAuthService externalAuthService;

    @PostMapping("/auth/login")
    public ApiResponse login(@Valid @RequestBody AuthDto.LoginRequest req) {
        log.info("用户 {} 尝试登录", req.getUsername());
        String token = authService.login(req.getUsername(), req.getPassword());
        return ApiResponse.ok(new AuthDto.LoginResponse(token, null));
    }

    @GetMapping("/auth/login-providers")
    public ApiResponse loginProviders() {
        ThirdPartyLoginSettingsDto settings = settingService.getEnabledThirdPartyLoginSettings();
        List<String> providers = new ArrayList<>();
        if (settings.isFeishuEnabled()) providers.add("feishu");
        if (settings.isDingtalkEnabled()) providers.add("dingtalk");
        if (settings.isWecomEnabled()) providers.add("wecom");
        List<String> available = externalAuthService.availableProviders();
        providers.removeIf(provider -> !available.contains(provider));
        return ApiResponse.ok(new ThirdPartyLoginProvidersDto(available.contains("keycloak"), !providers.isEmpty(), providers));
    }

    @GetMapping("/auth/{provider}/start")
    public void startExternal(@PathVariable String provider, HttpServletResponse response) throws java.io.IOException {
        response.sendRedirect(externalAuthService.start(provider));
    }

    @GetMapping("/auth/{provider}/callback")
    public void callbackExternal(@PathVariable String provider, @RequestParam(required = false) String code,
                                 @RequestParam(required = false) String state, @RequestParam(required = false) String error,
                                 @RequestParam(name = "error_description", required = false) String errorDescription,
                                 HttpServletResponse response) throws java.io.IOException {
        externalAuthService.callback(provider, code, state, error, errorDescription);
        response.sendRedirect("/?auth_state=" + java.net.URLEncoder.encode(state == null ? "" : state, java.nio.charset.StandardCharsets.UTF_8));
    }

    @PostMapping("/auth/exchange")
    public ApiResponse exchange(@RequestBody AuthExchangeRequest request) {
        return ApiResponse.ok(externalAuthService.exchange(request.getState()));
    }

    @PostMapping("/auth/bind")
    public ApiResponse bind(@RequestBody AuthBindRequest request) {
        return ApiResponse.ok(externalAuthService.bind(request.getState(), request.getUsername(), request.getPassword(), authService));
    }

    @lombok.Data
    public static class AuthExchangeRequest { private String state; }
    @lombok.Data
    public static class AuthBindRequest { private String state; private String username; private String password; }
}
