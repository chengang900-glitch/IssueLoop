package com.rnd.app.repository;

import com.rnd.app.entity.ProjectMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, Long> {
    List<ProjectMilestone> findByProjectIdOrderByPlannedDateAsc(Long projectId);
}
