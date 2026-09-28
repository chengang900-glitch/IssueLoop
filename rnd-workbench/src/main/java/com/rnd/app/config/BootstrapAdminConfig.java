package com.rnd.app.config;

import com.rnd.app.entity.User;
import com.rnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class BootstrapAdminConfig {
    private static final String DEFAULT_ADMIN_HASH = "$2b$12$eZewZfZxhlHYeOpi1cVV3OhCALG.vyP9rnvLddPoVgSTw7hP.45zu";

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Bean
    public SmartInitializingSingleton bootstrapAdmin(@Value("${APP_BOOTSTRAP_ADMIN_PASSWORD:}") String bootstrapPassword) {
        return () -> {
            User admin = userRepo.findByUsername("admin@uhoo.cn").orElse(null);
            if (admin == null || !DEFAULT_ADMIN_HASH.equals(admin.getPasswordHash())) return;
            if (bootstrapPassword.isBlank()) {
                if (environment.acceptsProfiles(Profiles.of("prod"))) {
                    throw new IllegalStateException("首次生产启动必须设置 APP_BOOTSTRAP_ADMIN_PASSWORD");
                }
                return;
            }
            if (bootstrapPassword.length() < 12 || passwordEncoder.matches(bootstrapPassword, DEFAULT_ADMIN_HASH)) {
                throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_PASSWORD 至少 12 位且不能使用默认口令");
            }
            admin.setPasswordHash(passwordEncoder.encode(bootstrapPassword));
            admin.setMustChangePassword(true);
            admin.setTokenVersion(admin.getTokenVersion() + 1);
            userRepo.save(admin);
        };
    }
}
