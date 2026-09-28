package com.rnd.app.repository;
import com.rnd.app.entity.UserViewPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserViewPreferenceRepository extends JpaRepository<UserViewPreference, Long> {
    Optional<UserViewPreference> findByProjectIdAndUserId(Long projectId, Long userId);
}
