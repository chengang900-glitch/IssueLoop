package com.rnd.app.service;

import com.rnd.app.dto.ActivityDto;
import com.rnd.app.entity.Activity;
import com.rnd.app.entity.User;
import com.rnd.app.repository.ActivityRepository;
import com.rnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ActivityRepository activityRepo;
    private final UserRepository userRepo;

    public List<ActivityDto> listByProject(Long projectId, Pageable pageable) {
        return activityRepo.findByProjectIdOrderByCreatedAtDesc(projectId, pageable)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<ActivityDto> listByWorkItem(String workItemId) {
        return activityRepo.findByWorkItemIdOrderByCreatedAtDesc(workItemId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    private ActivityDto toDto(Activity a) {
        return new ActivityDto(a.getId(), a.getWorkItemId(), a.getActorId(),
                userRepo.findById(a.getActorId()).map(User::getNickname).orElse(null),
                a.getType(), a.getContent(), a.getCreatedAt());
    }
}