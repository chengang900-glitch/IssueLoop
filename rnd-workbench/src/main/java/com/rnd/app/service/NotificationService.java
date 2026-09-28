package com.rnd.app.service;

import com.rnd.app.entity.Notification;
import com.rnd.app.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final com.rnd.app.repository.WorkItemWatcherRepository watcherRepo;

    public Notification notify(Long userId, String type, String workItemId, String content) {
        return notificationRepo.save(Notification.builder()
                .userId(userId).type(type).workItemId(workItemId).content(content).build());
    }
    @org.springframework.transaction.annotation.Transactional
    public Notification notifyDeduplicated(Long userId,String type,String workItemId,String content){ java.time.Instant after=java.time.Instant.now().minus(java.time.Duration.ofMinutes(5)); var existing=notificationRepo.findTopByUserIdAndWorkItemIdAndTypeAndCreatedAtAfterOrderByCreatedAtDesc(userId,workItemId,type,after); if(existing.isPresent()){var n=existing.get();n.setContent(content);n.setIsRead(false);return notificationRepo.save(n);} return notify(userId,type,workItemId,content); }
    public void notifyWatchers(String workItemId,Long actorId,String type,String content){ for(var watcher:watcherRepo.findByWorkItemId(workItemId)) if(!watcher.getUserId().equals(actorId)) notifyDeduplicated(watcher.getUserId(),type,workItemId,content); }
}
