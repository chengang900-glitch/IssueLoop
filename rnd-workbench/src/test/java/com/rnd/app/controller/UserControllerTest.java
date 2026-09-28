package com.rnd.app.controller;

import com.rnd.app.config.RndPrincipal;
import com.rnd.app.entity.User;
import com.rnd.app.repository.UserRepository;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {
    @Mock UserRepository userRepo;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserController controller;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void systemAdminCannotCreateUnsupportedSystemRole() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new RndPrincipal(1L, "admin", "ADMIN"), null, Collections.emptyList()));
        UserController.CreateUserRequest request = new UserController.CreateUserRequest();
        request.setUsername("new-user@example.com");
        request.setNickname("新用户");
        request.setSystemRole("SUPER_ADMIN");
        when(userRepo.existsByUsername("new-user@example.com")).thenReturn(false);

        assertThrows(BusinessException.class, () -> controller.createUser(request));
    }

    @Test
    void rejectsInvalidStatusBeforeChangingUser() {
        authenticateAdmin();
        UserController.StatusRequest request = new UserController.StatusRequest();
        request.setStatus(2);

        BusinessException error = assertThrows(BusinessException.class, () -> controller.updateStatus(3L, request));
        assertEquals(ErrorCode.BAD_REQUEST, error.getErrorCode());
        verify(userRepo, never()).findById(3L);
    }

    @Test
    void cannotDisableLastEnabledAdministrator() {
        authenticateAdmin();
        UserController.StatusRequest request = new UserController.StatusRequest();
        request.setStatus(0);
        when(userRepo.findById(1L)).thenReturn(Optional.of(User.builder()
                .id(1L).systemRole("ADMIN").status(1).build()));
        when(userRepo.countBySystemRoleAndStatus("ADMIN", 1)).thenReturn(1L);

        BusinessException error = assertThrows(BusinessException.class, () -> controller.updateStatus(1L, request));
        assertEquals(ErrorCode.STATUS_CONFLICT, error.getErrorCode());
    }

    private void authenticateAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new RndPrincipal(1L, "admin", "ADMIN"), null, Collections.emptyList()));
    }
}
