package com.rnd.app.repository;

import com.rnd.app.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant; import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("update Notification n set n.isRead = true where n.userId = ?1 and n.isRead = false")
    void markAllReadByUserId(Long userId);
    Optional<Notification> findTopByUserIdAndWorkItemIdAndTypeAndCreatedAtAfterOrderByCreatedAtDesc(Long userId,String workItemId,String type,Instant after);
    @Query("select n from Notification n where n.userId=:uid and (:read is null or n.isRead=:read) and (:type is null or n.type=:type) and (:wid is null or n.workItemId=:wid)")
    Page<Notification> search(@Param("uid")Long uid,@Param("read")Boolean read,@Param("type")String type,@Param("wid")String wid,Pageable pageable);
}
