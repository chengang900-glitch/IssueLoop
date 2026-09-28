package com.rnd.app.repository;

import com.rnd.app.entity.WorkItemRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkItemRelationRepository extends JpaRepository<WorkItemRelation, Long> {
    boolean existsBySourceWorkItemIdAndTargetWorkItemId(String sourceWorkItemId, String targetWorkItemId);
    List<WorkItemRelation> findBySourceWorkItemIdOrTargetWorkItemId(String sourceWorkItemId, String targetWorkItemId);
    long deleteBySourceWorkItemIdOrTargetWorkItemId(String sourceWorkItemId, String targetWorkItemId);
}
