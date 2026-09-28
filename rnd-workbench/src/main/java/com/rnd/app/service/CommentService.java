package com.rnd.app.service;

import com.rnd.app.dto.CommentDto;
import com.rnd.app.entity.Comment;
import com.rnd.app.entity.User;
import com.rnd.app.repository.CommentRepository;
import com.rnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    public Page<CommentDto> list(String workItemId, Pageable pageable) {
        return commentRepo.findByWorkItemIdOrderByCreatedAtDesc(workItemId, pageable)
                .map(this::toDto);
    }

    @Transactional
    public CommentDto add(String workItemId, String content, Long authorId) {
        Comment c = commentRepo.save(com.rnd.app.entity.Comment.builder()
                .workItemId(workItemId).authorId(authorId).content(content).build());
        notificationService.notifyWatchers(workItemId,authorId,"COMMENT_ADDED","工作项有新评论："+content);
        return toDto(c);
    }

    private CommentDto toDto(Comment c) {
        return new CommentDto(c.getId(), c.getAuthorId(),
                userRepo.findById(c.getAuthorId()).map(User::getNickname).orElse(null),
                c.getContent(), c.getCreatedAt());
    }
}
