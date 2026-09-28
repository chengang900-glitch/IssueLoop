package com.rnd.app.repository;

import com.rnd.app.entity.Activity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByProjectIdOrderByCreatedAtDesc(Long projectId, Pageable pageable);
    List<Activity> findByWorkItemIdOrderByCreatedAtDesc(String workItemId);
}