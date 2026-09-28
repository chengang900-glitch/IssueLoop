package com.rnd.app.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.Principal;

/**
 * 当前请求的用户身份
 */
@Getter
@AllArgsConstructor
public class RndPrincipal implements Principal {
    private Long userId;
    private String username;
    private String systemRole;

    @Override
    public String getName() { return username; }
}