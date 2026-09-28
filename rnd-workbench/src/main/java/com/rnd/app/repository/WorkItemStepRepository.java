package com.rnd.app.repository;

import com.rnd.app.entity.WorkItemStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkItemStepRepository extends JpaRepository<WorkItemStep, Long> {
    List<WorkItemStep> findByWorkItemIdOrderBySeq(String workItemId);
    void deleteByWorkItemId(String workItemId);
}