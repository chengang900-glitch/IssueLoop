package com.rnd.app.repository;

import com.rnd.app.entity.WorkItemRelation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WorkItemRelationRepository extends JpaRepository<WorkItemRelation, Long> {
    boolean existsBySourceWorkItemIdAndTargetWorkItemId(String sourceWorkItemId, String targetWorkItemId);
    List<WorkItemRelation> findBySourceWorkItemIdOrTargetWorkItemId(String sourceWorkItemId, String targetWorkItemId);

    /** 列表/看板一次取回这批工作项的全部关联关系。 */
    List<WorkItemRelation> findBySourceWorkItemIdInOrTargetWorkItemIdIn(Collection<String> sourceWorkItemIds, Collection<String> targetWorkItemIds);
    long deleteBySourceWorkItemIdOrTargetWorkItemId(String sourceWorkItemId, String targetWorkItemId);
}
