package com.rnd.app.repository;

import com.rnd.app.entity.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface TaskTypeRepository extends JpaRepository<TaskType, Long> {
    List<TaskType> findAllByOrderBySortOrderAscIdAsc();
    List<TaskType> findByEnabledTrueOrderBySortOrderAscIdAsc();
    Optional<TaskType> findByName(String name);
    boolean existsByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TaskType t where t.name = :name")
    Optional<TaskType> findByNameForUpdate(@Param("name") String name);
}
