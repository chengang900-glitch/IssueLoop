package com.rnd.app.controller;

import com.rnd.app.dto.AuthDto;
import com.rnd.app.service.AuthService;
import com.rnd.app.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/login")
    public ApiResponse login(@Valid @RequestBody AuthDto.LoginRequest req) {
        log.info("用户 {} 尝试登录", req.getUsername());
        String token = authService.login(req.getUsername(), req.getPassword());
        return ApiResponse.ok(new AuthDto.LoginResponse(token, null));
    }
}