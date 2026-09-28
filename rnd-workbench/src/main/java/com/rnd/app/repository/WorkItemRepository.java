package com.rnd.app.repository;

import com.rnd.app.entity.WorkItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface WorkItemRepository extends JpaRepository<WorkItem, String>, JpaSpecificationExecutor<WorkItem> {

    @Modifying
    @Query("update WorkItem w set w.type = :newName where w.type = :oldName")
    int updateTypeName(@Param("oldName") String oldName, @Param("newName") String newName);

    // 看板：按状态分组
    List<WorkItem> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    // 统计项目内某类型最大 seq_no
    @Query("SELECT COALESCE(MAX(w.seqNo), 0) FROM WorkItem w WHERE w.projectId = :projectId AND w.type = :type")
    Integer maxSeqNo(@Param("projectId") Long projectId, @Param("type") String type);

    // 工作台汇总
    long countByProjectIdAndStatusIn(Long projectId, List<String> statuses);
    long countByProjectIdAndStatus(Long projectId, String status);
    long countByProjectIdAndDueDateBetweenAndStatusNotIn(Long projectId, Instant from, Instant to, List<String> statuses);
    long countByProjectIdAndPriorityAndStatusNotIn(Long projectId, String priority, List<String> statuses);

    // 逾期/到期
    List<WorkItem> findByProjectIdAndOwnerIdAndStatusNotInOrderByDueDateAsc(
            Long projectId, Long ownerId, List<String> statuses);

    // 迭代筛选
    List<WorkItem> findByProjectIdAndSprintId(Long projectId, Long sprintId);

    // 子任务
    List<WorkItem> findByParentId(String parentId);
}
