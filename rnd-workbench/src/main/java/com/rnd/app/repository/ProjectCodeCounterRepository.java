package com.rnd.app.repository;

import com.rnd.app.entity.ProjectCodeCounter;
import com.rnd.app.entity.ProjectCodeCounterId;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import javax.persistence.LockModeType;
import java.util.Optional;

public interface ProjectCodeCounterRepository extends JpaRepository<ProjectCodeCounter, ProjectCodeCounterId> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ProjectCodeCounter c where c.prefix = :prefix and c.period = :period")
    Optional<ProjectCodeCounter> findForUpdate(@Param("prefix") String prefix, @Param("period") String period);
}
