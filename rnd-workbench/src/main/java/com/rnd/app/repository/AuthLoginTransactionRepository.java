package com.rnd.app.repository;

import com.rnd.app.entity.AuthLoginTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface AuthLoginTransactionRepository extends JpaRepository<AuthLoginTransaction, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AuthLoginTransaction t where t.state = :state")
    Optional<AuthLoginTransaction> findForUpdate(@Param("state") String state);
}
