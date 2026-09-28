package com.rnd.app.repository;

import com.rnd.app.entity.SavedFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavedFilterRepository extends JpaRepository<SavedFilter, Long> {
    List<SavedFilter> findByProjectIdAndUserIdOrderByCreatedAtAsc(Long projectId, Long userId);
    Optional<SavedFilter> findByIdAndUserId(Long id, Long userId);
    boolean existsByProjectIdAndUserIdAndName(Long projectId, Long userId, String name);
    boolean existsByProjectIdAndUserIdAndNameAndIdNot(Long projectId, Long userId, String name, Long id);

    @Modifying
    @Query("UPDATE SavedFilter f SET f.defaultFilter = false WHERE f.projectId = :projectId AND f.userId = :userId")
    void clearDefaults(@Param("projectId") Long projectId, @Param("userId") Long userId);
}
