package com.rnd.app;

import com.rnd.app.entity.AuthLoginTransaction;
import com.rnd.app.entity.User;
import com.rnd.app.repository.AuthLoginTransactionRepository;
import com.rnd.app.repository.UserRepository;
import com.rnd.app.service.AuthService;
import com.rnd.app.service.ExternalAuthService;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityFlowIntegrationTest {
    @Autowired private AuthService authService;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwords;
    @Autowired private JwtUtil jwt;
    @Autowired private MockMvc mvc;
    @Autowired private ExternalAuthService externalAuthService;
    @Autowired private AuthLoginTransactionRepository loginTransactions;

    @Test
    void failedLoginAttemptsPersistAndLockAccount() {
        User user = createUser("USER", false);
        for (int i = 0; i < 5; i++) {
            assertThrows(BusinessException.class, () -> authService.login(user.getUsername(), "wrong-password"));
        }
        User saved = users.findById(user.getId()).orElseThrow();
        assertEquals(5, saved.getFailCount());
        assertTrue(saved.isLocked());
        assertThrows(BusinessException.class, () -> authService.login(user.getUsername(), "OriginalPwd12"));
    }

    @Test
    void existingTokenUsesCurrentAccountStatusAndRole() throws Exception {
        User user = createUser("ADMIN", false);
        String token = jwt.generate(user.getId(), user.getUsername(), "ADMIN", user.getTokenVersion());
        user.setSystemRole("USER");
        users.save(user);

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.systemRole").value("USER"));
        mvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        user.setStatus(0);
        users.save(user);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forcedPasswordChangeBlocksBusinessApisAndInvalidatesOldToken() throws Exception {
        User user = createUser("USER", true);
        String token = jwt.generate(user.getId(), user.getUsername(), "USER", user.getTokenVersion());

        mvc.perform(get("/api/v1/projects").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/users/me").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"department\":\"研发\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/users/me").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"oldPassword\":\"OriginalPwd12\",\"newPassword\":\"ReplacementPwd34\"}"))
                .andExpect(status().isOk());

        User saved = users.findById(user.getId()).orElseThrow();
        assertFalse(saved.isMustChangePassword());
        assertEquals(1, saved.getTokenVersion());
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        String newToken = authService.login(user.getUsername(), "ReplacementPwd34");
        mvc.perform(get("/api/v1/projects").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());
    }

    @Test
    void resetPasswordReturnsUniqueTemporarySecretAndRevokesExistingToken() throws Exception {
        User admin = createUser("ADMIN", false);
        User user = createUser("USER", false);
        String adminToken = jwt.generate(admin.getId(), admin.getUsername(), "ADMIN", admin.getTokenVersion());
        String oldToken = jwt.generate(user.getId(), user.getUsername(), "USER", user.getTokenVersion());

        String result = mvc.perform(post("/api/v1/users/" + user.getId() + "/reset-password")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.initialPassword").isString())
                .andReturn().getResponse().getContentAsString();
        String temporaryPassword = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result).path("data").path("initialPassword").asText();
        assertEquals(16, temporaryPassword.length());
        assertTrue(passwords.matches(temporaryPassword, users.findById(user.getId()).orElseThrow().getPasswordHash()));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void newUserReceivesOneTimeRandomPassword() throws Exception {
        User admin = createUser("ADMIN", false);
        String adminToken = jwt.generate(admin.getId(), admin.getUsername(), "ADMIN", admin.getTokenVersion());
        String username = UUID.randomUUID() + "@example.test";
        String result = mvc.perform(post("/api/v1/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"nickname\":\"New user\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.initialPassword").isString())
                .andReturn().getResponse().getContentAsString();
        String temporaryPassword = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(result).path("data").path("initialPassword").asText();
        User saved = users.findByUsername(username).orElseThrow();
        assertEquals(16, temporaryPassword.length());
        assertTrue(saved.isMustChangePassword());
        assertTrue(passwords.matches(temporaryPassword, saved.getPasswordHash()));
    }

    /**
     * 回归用例：/api/v1/auth/bind 是匿名接口，历史实现里外层事务会随 BusinessException
     * 回滚，导致 fail_count / lock_until 永远写不进库，撞库（口令喷洒）可无限进行。
     */
    @Test
    void externalBindingFailuresPersistAndLockAccount() {
        User user = createUser("USER", false);

        for (int i = 0; i < 5; i++) {
            String state = UUID.randomUUID().toString();
            loginTransactions.save(AuthLoginTransaction.builder()
                    .state(state).provider("keycloak")
                    .redirectUri("http://127.0.0.1:3002/api/v1/auth/keycloak/callback")
                    .expiresAt(Instant.now().plus(Duration.ofMinutes(5)))
                    .externalSubject("subject-" + i)
                    .build());
            assertThrows(BusinessException.class,
                    () -> externalAuthService.bind(state, user.getUsername(), "wrong-password", authService));
        }

        User saved = users.findById(user.getId()).orElseThrow();
        assertEquals(5, saved.getFailCount());
        assertTrue(saved.isLocked());
        // 账号已锁定：即使口令正确也不能登录，撞库被阻断
        assertThrows(BusinessException.class, () -> authService.login(user.getUsername(), "OriginalPwd12"));
    }

    private User createUser(String role, boolean mustChangePassword) {
        return users.save(User.builder()
                .username(UUID.randomUUID() + "@example.test")
                .nickname("Security test")
                .passwordHash(passwords.encode("OriginalPwd12"))
                .systemRole(role)
                .mustChangePassword(mustChangePassword)
                .build());
    }
}
