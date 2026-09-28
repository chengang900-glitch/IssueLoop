package com.rnd.app.repository;

import com.rnd.app.entity.ModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModuleRepository extends JpaRepository<ModuleEntity, Long> {
    List<ModuleEntity> findByProjectIdOrderByName(Long projectId);
    boolean existsByProjectIdAndName(Long projectId, String name);
}