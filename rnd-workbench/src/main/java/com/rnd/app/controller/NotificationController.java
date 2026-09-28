package com.rnd.app.controller;

import com.rnd.app.entity.Notification;
import com.rnd.app.repository.NotificationRepository;
import com.rnd.app.util.ApiResponse;
import com.rnd.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepo;

    @GetMapping
    public ApiResponse list(@RequestParam(defaultValue = "1") int page,
                            @RequestParam(defaultValue = "20") int size,@RequestParam(required=false) Boolean read,@RequestParam(required=false) String type,@RequestParam(required=false) String workItemId) {
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> result = notificationRepo.search(SecurityUtil.currentUserId(),read,type,workItemId,pageable);
        return ApiResponse.page(result.getContent(), result.getTotalElements(), page, size);
    }

    @GetMapping("/unread-count")
    public ApiResponse unreadCount() {
        long count = notificationRepo.countByUserIdAndIsReadFalse(SecurityUtil.currentUserId());
        return ApiResponse.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public ApiResponse markRead(@PathVariable Long id) {
        Long uid = SecurityUtil.currentUserId();
        notificationRepo.findById(id).ifPresent(n -> {
            if (n.getUserId().equals(uid)) {
                n.setIsRead(true);
                notificationRepo.save(n);
            }
        });
        return ApiResponse.ok();
    }

    @PutMapping("/read-all")
    @Transactional
    public ApiResponse readAll() {
        notificationRepo.markAllReadByUserId(SecurityUtil.currentUserId());
        return ApiResponse.ok();
    }
}
