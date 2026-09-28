package com.rnd.app.repository;

import com.rnd.app.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByWorkItemIdOrderByCreatedAtDesc(String workItemId, Pageable pageable);
}