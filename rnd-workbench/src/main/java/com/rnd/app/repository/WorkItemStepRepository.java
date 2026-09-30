package com.rnd.app.repository;

import com.rnd.app.entity.WorkItemStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WorkItemStepRepository extends JpaRepository<WorkItemStep, Long> {
    List<WorkItemStep> findByWorkItemIdOrderBySeq(String workItemId);

    /** 列表/看板一次取回多条工作项的步骤，避免逐条查询。 */
    List<WorkItemStep> findByWorkItemIdInOrderBySeqAsc(Collection<String> workItemIds);
    void deleteByWorkItemId(String workItemId);
}