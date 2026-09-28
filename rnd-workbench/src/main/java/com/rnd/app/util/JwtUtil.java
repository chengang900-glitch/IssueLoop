package com.rnd.app.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final com.rnd.app.config.AppConfig config;
    private Key key;

    @PostConstruct
    void init() {
        byte[] bytes = config.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 64) {
            // pad to 64 bytes for HS512 minimum
            byte[] padded = new byte[64];
            System.arraycopy(bytes, 0, padded, 0, bytes.length);
            bytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String generate(Long userId, String username, String systemRole, int tokenVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("systemRole", systemRole);
        claims.put("tokenVersion", tokenVersion);

        Duration ttl = Duration.parse("PT" + config.getJwt().getTtl().toUpperCase().replaceAll("(\\d+)([HM])", "$1$2"));
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
