package com.rnd.app.service;

import com.rnd.app.entity.User;
import com.rnd.app.repository.UserRepository;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import com.rnd.app.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    private static final int MAX_FAIL = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    @Transactional(noRollbackFor = BusinessException.class)
    public String login(String username, String password) {
        Optional<User> opt = userRepo.findByUsernameForUpdate(username);
        if (opt.isEmpty()) throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名或密码错误");

        User user = opt.get();

        if (!Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已停用，请联系管理员");
        }

        // 检查锁定
        if (user.isLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "账号已锁定，请于 " + user.getLockUntil() + " 后再试");
        }

        if (!encoder.matches(password, user.getPasswordHash())) {
            user.setFailCount(user.getFailCount() + 1);
            if (user.getFailCount() >= MAX_FAIL) {
                user.setLockUntil(Instant.now().plus(LOCK_DURATION));
                log.warn("用户 {} 连续失败 {} 次，锁定至 {}", username, MAX_FAIL, user.getLockUntil());
            }
            userRepo.save(user);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名或密码错误");
        }

        // 成功：重置
        user.setFailCount(0);
        user.setLockUntil(null);
        userRepo.save(user);

        return jwtUtil.generate(user.getId(), user.getUsername(), user.getSystemRole(), user.getTokenVersion());
    }
}
