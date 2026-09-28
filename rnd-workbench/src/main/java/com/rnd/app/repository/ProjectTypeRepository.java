package com.rnd.app.repository;

import com.rnd.app.entity.ProjectType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectTypeRepository extends JpaRepository<ProjectType, Long> {
    List<ProjectType> findAllByOrderBySortOrderAscIdAsc();
    List<ProjectType> findByEnabledTrueOrderBySortOrderAscIdAsc();
    Optional<ProjectType> findByName(String name);
    boolean existsByName(String name);
}
