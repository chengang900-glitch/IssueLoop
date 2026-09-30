package com.rnd.app.repository;

import com.rnd.app.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByIdIn(List<Long> ids);

    /** 归档过滤下推到 SQL，避免把全部项目取回内存再过滤。 */
    List<Project> findByIdInAndArchivedFalse(Collection<Long> ids);

    @Modifying
    @Query("update Project p set p.projectType = :newName where p.projectType = :oldName")
    void updateProjectTypeName(@Param("oldName") String oldName, @Param("newName") String newName);
}
