package com.rnd.app.util;

import com.rnd.app.config.RndPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 取当前登录用户
 */
public class SecurityUtil {

    public static RndPrincipal currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof RndPrincipal) {
            return (RndPrincipal) auth.getPrincipal();
        }
        return null;
    }

    public static Long currentUserId() {
        RndPrincipal u = currentUser();
        return u != null ? u.getUserId() : null;
    }
}