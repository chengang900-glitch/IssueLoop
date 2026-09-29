package com.rnd.app.controller;

import com.rnd.app.dto.AuthDto;
import com.rnd.app.dto.ThirdPartyLoginProvidersDto;
import com.rnd.app.dto.ThirdPartyLoginSettingsDto;
import com.rnd.app.service.AuthService;
import com.rnd.app.service.SystemSettingService;
import com.rnd.app.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
        return ApiResponse.ok(new ThirdPartyLoginProvidersDto(true, !providers.isEmpty(), providers));
    }
}
