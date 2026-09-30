package com.rnd.app.util;

import com.rnd.app.config.AppConfig;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String VALID_SECRET = "unit-test-secret-with-enough-entropy-1234";

    private JwtUtil jwtUtil(String secret, String ttl) {
        AppConfig config = new AppConfig();
        AppConfig.JwtConfig jwt = new AppConfig.JwtConfig();
        jwt.setSecret(secret);
        jwt.setTtl(ttl);
        config.setJwt(jwt);
        JwtUtil util = new JwtUtil(config);
        util.init();
        return util;
    }

    @Test
    void rejectsShortSecretInsteadOfPaddingIt() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> jwtUtil("short-secret", "8h"));
        assertTrue(error.getMessage().contains("32"), error.getMessage());
    }

    @Test
    void rejectsRepositoryPlaceholderSecret() {
        assertThrows(IllegalStateException.class, () -> jwtUtil("CHANGE_ME_PRODUCTION_RANDOM_64_CHARS", "8h"));
    }

    @Test
    void generatesOneOffRandomKeyWhenSecretIsBlank() {
        JwtUtil util = jwtUtil("   ", "8h");
        String token = util.generate(1L, "user", "USER", 0);

        assertEquals("user", util.verify(token).getBody().getSubject());
        // 另一次“启动”的随机密钥不同，无法互相验证
        assertThrows(Exception.class, () -> jwtUtil("", "8h").verify(token));
    }

    @Test
    void signsAndVerifiesWithConfiguredSecret() {
        JwtUtil util = jwtUtil(VALID_SECRET, "2h");
        String token = util.generate(7L, "tester", "ADMIN", 3);

        Claims claims = util.verify(token).getBody();
        assertEquals("tester", claims.getSubject());
        assertEquals(7L, ((Number) claims.get("userId")).longValue());
        assertEquals("ADMIN", claims.get("systemRole"));
        assertEquals(3, ((Number) claims.get("tokenVersion")).intValue());
        assertTrue(claims.getExpiration().toInstant().isBefore(Instant.now().plus(Duration.ofHours(3))));
    }

    @Test
    void acceptsHourAndMinuteTtlAndRejectsGarbage() {
        assertTrue(jwtUtil(VALID_SECRET, "30m").generate(1L, "u", "USER", 0).length() > 20);
        assertTrue(jwtUtil(VALID_SECRET, "PT8H30M").generate(1L, "u", "USER", 0).length() > 20);
        assertThrows(IllegalStateException.class, () -> jwtUtil(VALID_SECRET, "eight-hours"));
        assertThrows(IllegalStateException.class, () -> jwtUtil(VALID_SECRET, ""));
    }

    @Test
    void rejectsMissingJwtConfiguration() {
        AppConfig config = new AppConfig();
        config.setJwt(null);
        JwtUtil util = new JwtUtil(config);

        assertThrows(IllegalStateException.class, util::init);
    }
}
