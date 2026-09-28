package com.rnd.app.repository;

import com.rnd.app.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserIdAndTargetType(Long userId, String targetType);
    void deleteByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, String targetId);
    boolean existsByUserIdAndTargetTypeAndTargetId(Long userId, String targetType, String targetId);
}