package com.rnd.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnd.app.entity.WorkItem;
import com.rnd.app.dto.CreateWorkItemRequest;
import com.rnd.app.dto.UpdateWorkItemRequest;
import com.rnd.app.repository.*;
import com.rnd.app.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.math.BigDecimal;
import java.util.Optional;
import java.time.LocalDate;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkItemServiceTest {
    @Mock WorkItemRepository workItemRepo;
    @Mock WorkItemIdGenerator idGenerator;
    @Mock WorkItemStepRepository stepRepo;
    @Mock ActivityRepository activityRepo;
    @Mock UserRepository userRepo;
    @Mock WorkItemWatcherRepository watcherRepo;
    @Mock WorkItemRelationRepository relationRepo;
    @Mock ProjectMemberRepository projectMemberRepo;
    @Mock ModuleRepository moduleRepo;
    @Mock SprintRepository sprintRepo;
    @Mock NotificationService notificationService;
    @Mock RichTextSanitizer richTextSanitizer;
    @Mock TaskTypeService taskTypeService;
    @Mock ObjectMapper mapper;
    @InjectMocks WorkItemService service;

    @Test
    void rejectsIllegalTransition() {
        when(workItemRepo.findById("TASK-1001")).thenReturn(Optional.of(item("TASK-1001", "新建", null)));
        assertThrows(BusinessException.class,
                () -> service.transitionStatus("TASK-1001", "已完成", 9L));
    }

    @Test
    void assignMeRequiresUnassignedItemAndValidState() {
        when(stepRepo.findByWorkItemIdOrderBySeq(any())).thenReturn(Collections.emptyList());
        when(workItemRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(workItemRepo.findById("TASK-1001")).thenReturn(Optional.of(item("TASK-1001", "新建", null)));
        assertEquals("进行中", service.assignMe("TASK-1001", 9L).getStatus());

        when(workItemRepo.findById("TASK-1002")).thenReturn(Optional.of(item("TASK-1002", "新建", 8L)));
        assertThrows(BusinessException.class, () -> service.assignMe("TASK-1002", 9L));
    }

    @Test
    void watchAndUnwatchAreIdempotent() {
        when(watcherRepo.existsByWorkItemIdAndUserId("TASK-1001", 9L)).thenReturn(false, true);

        service.watch("TASK-1001", 9L);
        service.watch("TASK-1001", 9L);
        service.unwatch("TASK-1001", 9L);
        service.unwatch("TASK-1001", 9L);

        verify(watcherRepo).save(any());
        verify(watcherRepo, never()).deleteById(any());
        verify(watcherRepo, org.mockito.Mockito.times(2)).deleteByWorkItemIdAndUserId("TASK-1001", 9L);
    }

    @Test
    void validStatusTransitionNotifiesWatchers() {
        when(workItemRepo.findById("TASK-1003")).thenReturn(Optional.of(item("TASK-1003", "新建", 8L)));
        when(workItemRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stepRepo.findByWorkItemIdOrderBySeq(any())).thenReturn(Collections.emptyList());
        service.transitionStatus("TASK-1003", "进行中", 9L);
        org.mockito.Mockito.verify(notificationService).notifyWatchers(
                org.mockito.ArgumentMatchers.eq("TASK-1003"), org.mockito.ArgumentMatchers.eq(9L),
                org.mockito.ArgumentMatchers.eq("STATUS_CHANGED"), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void completingItemRequiresAndSavesActualCompletionData() {
        WorkItem workItem = item("TASK-1005", "进行中", 8L);
        when(workItemRepo.findById("TASK-1005")).thenReturn(Optional.of(workItem));

        assertThrows(BusinessException.class,
                () -> service.transitionStatus("TASK-1005", "已完成", 9L, null, null, null, null, null));

        Instant completedAt = Instant.parse("2026-07-16T08:30:00Z");
        BigDecimal actualHours = new BigDecimal("3.75");
        when(workItemRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stepRepo.findByWorkItemIdOrderBySeq(any())).thenReturn(Collections.emptyList());

        com.rnd.app.dto.WorkItemDto completed = service.transitionStatus(
                "TASK-1005", "已完成", 9L, null, null, null, completedAt, actualHours);

        assertEquals(completedAt, completed.getActualCompletedAt());
        assertEquals(actualHours, completed.getActualHours());
    }

    @Test
    void createPersistsWatcherAndHalfHourEstimate() {
        CreateWorkItemRequest request = new CreateWorkItemRequest();
        request.setType("任务");
        request.setTitle("任务标题");
        request.setDescription("<p>任务描述</p>");
        request.setEstimatedHours(new BigDecimal("2.5"));
        request.setPlannedStartDate(LocalDate.of(2026, 7, 20));
        request.setActualHours(new BigDecimal("1.25"));
        request.setWatcherIds(List.of(8L));
        when(richTextSanitizer.sanitize("<p>任务描述</p>")).thenReturn("<p>任务描述</p>");
        when(richTextSanitizer.hasText("<p>任务描述</p>")).thenReturn(true);
        when(projectMemberRepo.existsByProjectIdAndUserId(1L, 8L)).thenReturn(true);
        when(idGenerator.next("任务")).thenReturn(new WorkItemIdGenerator.GeneratedId("TASK", 1));
        when(workItemRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stepRepo.findByWorkItemIdOrderBySeq(any())).thenReturn(Collections.emptyList());

        com.rnd.app.dto.WorkItemDto created = service.create(1L, request, 9L);
        assertEquals(new BigDecimal("2.5"), created.getEstimatedHours());
        assertEquals(LocalDate.of(2026, 7, 20), created.getPlannedStartDate());
        assertEquals(new BigDecimal("1.25"), created.getActualHours());
        verify(watcherRepo).save(any());
    }

    @Test
    void quickUpdateCanClearOwnerAndRejectsInvalidPriority() {
        WorkItem workItem = item("TASK-1004", "进行中", 8L);
        when(workItemRepo.findById("TASK-1004")).thenReturn(Optional.of(workItem));
        when(workItemRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(activityRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(stepRepo.findByWorkItemIdOrderBySeq(any())).thenReturn(Collections.emptyList());

        UpdateWorkItemRequest clearOwner = new UpdateWorkItemRequest();
        clearOwner.setOwnerId(0L);
        assertEquals(null, service.update("TASK-1004", clearOwner, 9L).getOwnerId());

        UpdateWorkItemRequest invalidPriority = new UpdateWorkItemRequest();
        invalidPriority.setPriority("紧急");
        assertThrows(BusinessException.class, () -> service.update("TASK-1004", invalidPriority, 9L));
    }

    private WorkItem item(String id, String status, Long ownerId) {
        return WorkItem.builder().id(id).seqNo(1).projectId(1L).type("任务").title("测试")
                .status(status).ownerId(ownerId).priority("P2").severity("普通").creatorId(9L).build();
    }
}
