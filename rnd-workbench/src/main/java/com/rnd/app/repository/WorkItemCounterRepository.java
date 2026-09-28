package com.rnd.app.repository;

import com.rnd.app.entity.WorkItemCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface WorkItemCounterRepository extends JpaRepository<WorkItemCounter, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from WorkItemCounter c where c.type = :type")
    Optional<WorkItemCounter> findForUpdate(@Param("type") String type);
}
