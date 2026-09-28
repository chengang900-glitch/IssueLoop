package com.rnd.app.service;

import com.rnd.app.entity.User;
import com.rnd.app.repository.UserRepository;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository userRepo;
    @Mock PasswordEncoder encoder;
    @Mock JwtUtil jwtUtil;
    @InjectMocks AuthService service;

    @Test
    void disabledUserCannotLogIn() {
        when(userRepo.findByUsernameForUpdate("disabled"))
                .thenReturn(Optional.of(User.builder().status(0).passwordHash("hash").build()));

        assertThrows(BusinessException.class, () -> service.login("disabled", "password"));
    }
}
