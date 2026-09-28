package com.rnd.app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnd.app.entity.User;
import com.rnd.app.repository.UserRepository;
import com.rnd.app.util.ApiResponse;
import com.rnd.app.util.ErrorCode;
import com.rnd.app.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepo;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }
        User currentUser = null;
        try {
            Jws<Claims> jws = jwtUtil.verify(header.substring(7));
            Claims claims = jws.getBody();
            Long userId = claims.get("userId", Long.class);
            Integer tokenVersion = claims.get("tokenVersion", Integer.class);
            currentUser = userRepo.findById(userId).orElse(null);
            if (currentUser != null && Integer.valueOf(1).equals(currentUser.getStatus())
                    && tokenVersion != null && tokenVersion == currentUser.getTokenVersion()) {
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                new RndPrincipal(userId, currentUser.getUsername(), currentUser.getSystemRole()), null,
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + currentUser.getSystemRole())));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (currentUser != null && currentUser.isMustChangePassword()
                && SecurityContextHolder.getContext().getAuthentication() != null
                && path.startsWith("/api/v1/")
                && !"/api/v1/auth/login".equals(path)
                && !("GET".equals(request.getMethod()) && "/api/v1/auth/me".equals(path))
                && !("PUT".equals(request.getMethod()) && "/api/v1/users/me".equals(path))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.fail(ErrorCode.FORBIDDEN, "请先修改初始密码")));
            return;
        }
        chain.doFilter(request, response);
    }
}
