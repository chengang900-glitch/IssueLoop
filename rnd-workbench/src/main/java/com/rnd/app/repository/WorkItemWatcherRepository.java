package com.rnd.app.repository;

import com.rnd.app.entity.WorkItemWatcher;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WorkItemWatcherRepository extends JpaRepository<WorkItemWatcher, Long> {
    boolean existsByWorkItemIdAndUserId(String workItemId, Long userId);

    long deleteByWorkItemIdAndUserId(String workItemId, Long userId);
    long deleteByWorkItemId(String workItemId);
    List<WorkItemWatcher> findByWorkItemId(String workItemId);
    List<WorkItemWatcher> findByUserId(Long userId);
}
