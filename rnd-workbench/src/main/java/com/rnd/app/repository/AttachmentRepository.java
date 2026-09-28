package com.rnd.app.repository;

import com.rnd.app.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByWorkItemId(String workItemId);
}