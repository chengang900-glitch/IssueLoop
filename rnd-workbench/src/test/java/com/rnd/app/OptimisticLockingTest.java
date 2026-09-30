package com.rnd.app;

import com.rnd.app.entity.User;
import com.rnd.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 回归用例：并发“读-改-写”不能再用旧快照覆盖新数据。
 * 场景即审查里指出的 P0：管理员重置口令（password_hash + token_version）时，
 * 用户并发的“改昵称”请求若用旧快照整行 merge，会把口令与 token_version 一起写回旧值，
 * 导致已吊销的旧 JWT 复活。
 */
@SpringBootTest
class OptimisticLockingTest {

    @Autowired UserRepository users;
    @Autowired EntityManagerFactory entityManagerFactory;

    @Test
    void staleSnapshotCannotOverwriteNewerSecurityFields() {
        User user = users.save(User.builder()
                .username(UUID.randomUUID() + "@example.test")
                .nickname("并发测试")
                .passwordHash("$2a$10$OLDHASHOLDHASHOLDHASHOLDHAS")
                .systemRole("USER")
                .build());

        EntityManager staleSession = entityManagerFactory.createEntityManager();
        EntityManager adminSession = entityManagerFactory.createEntityManager();
        try {
            // 会话一（先读，拿到 version=0 的旧快照）
            staleSession.getTransaction().begin();
            User stale = staleSession.find(User.class, user.getId());

            // 会话二（管理员重置口令：新 hash + token_version+1）先提交
            adminSession.getTransaction().begin();
            User managed = adminSession.find(User.class, user.getId());
            managed.setPasswordHash("$2a$10$NEWHASHNEWHASHNEWHASHNEWHAS");
            managed.setTokenVersion(managed.getTokenVersion() + 1);
            adminSession.getTransaction().commit();

            // 旧快照提交必须失败，而不是把整行（含安全字段）覆盖回去
            stale.setNickname("改昵称");
            assertThrows(PersistenceException.class, () -> staleSession.getTransaction().commit());
            staleSession.getTransaction().rollback();
        } finally {
            if (staleSession.isOpen()) staleSession.close();
            if (adminSession.isOpen()) adminSession.close();
        }

        User reloaded = users.findById(user.getId()).orElseThrow();
        assertEquals("$2a$10$NEWHASHNEWHASHNEWHASHNEWHAS", reloaded.getPasswordHash());
        assertEquals(1, reloaded.getTokenVersion());
        assertEquals("并发测试", reloaded.getNickname());
    }
}
