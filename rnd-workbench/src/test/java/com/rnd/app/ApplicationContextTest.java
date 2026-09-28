package com.rnd.app;

import com.rnd.app.repository.UserRepository;
import com.rnd.app.repository.WorkItemCounterRepository;
import com.rnd.app.repository.WorkItemRelationRepository;
import com.rnd.app.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ApplicationContextTest {
    @Autowired UserRepository userRepository;
    @Autowired WorkItemCounterRepository counterRepository;
    @Autowired AuthService authService;
    @Autowired WorkItemRelationRepository relationRepository;

    @Test
    void h2MigrationsCreateSchemaAndSeedData() {
        assertTrue(userRepository.findByUsername("admin@uhoo.cn").isPresent());
        assertEquals(4, counterRepository.count());
        assertEquals(0, relationRepository.count());
        assertTrue(authService.login("admin@uhoo.cn", "admin123").length() > 20);
    }
}
