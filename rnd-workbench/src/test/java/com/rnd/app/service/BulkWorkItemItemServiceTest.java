package com.rnd.app.service;

import com.rnd.app.dto.BulkUpdateRequest;
import com.rnd.app.entity.WorkItem;
import com.rnd.app.repository.*;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkWorkItemItemServiceTest {
    @Mock WorkItemRepository workItems;
    @Mock ProjectMemberRepository members;
    @Mock SprintRepository sprints;
    @Mock ActivityRepository activities;
    @Mock NotificationService notifications;
    @Mock WorkItemService workItemService;
    @InjectMocks BulkWorkItemItemService service;

    private BulkUpdateRequest statusRequest(String status) {
        BulkUpdateRequest request = new BulkUpdateRequest();
        request.setStatus(status);
        return request;
    }

    /** 批量更新没有说明/工时字段，不能用来绕过单条流转的必填校验。 */
    @Test
    void rejectsStatusesThatNeedReasonOrCompletionDataInSingleFlow() {
        // 每种目标状态都取一条在状态机下"合法可达"的当前状态，
        // 确保拦截来自必填校验而不是"状态流转非法"。
        java.util.Map<String, String> cases = new java.util.LinkedHashMap<>();
        cases.put("进行中", "延期处理");
        cases.put("进行中", "已完成");
        cases.put("新建", "已拒绝");
        cases.put("已完成", "验收不通过");
        // "延期处理 → 进行中"在单条流转里要落地延期审批结果，批量无法表达
        cases.put("延期处理", "进行中");

        for (java.util.Map.Entry<String, String> entry : cases.entrySet()) {
            WorkItem item = WorkItem.builder().id("TASK-1").projectId(1L).type("任务")
                    .status(entry.getKey()).ownerId(8L).creatorId(9L).build();
            when(workItems.findById("TASK-1")).thenReturn(Optional.of(item));

            BusinessException error = assertThrows(BusinessException.class,
                    () -> service.update(1L, "TASK-1", statusRequest(entry.getValue()), 9L));
            assertTrue(error.getMessage().contains("单条流转"),
                    entry.getKey() + " -> " + entry.getValue() + " : " + error.getMessage());
        }
        verify(workItems, never()).save(any());
    }

    /** 批量流转同样要执行"负责人/创建人/项目管理员"校验。 */
    @Test
    void enforcesTransitionActorForBulkStatusChange() {
        WorkItem item = WorkItem.builder().id("TASK-2").projectId(1L).type("任务").status("新建").ownerId(8L).creatorId(9L).build();
        when(workItems.findById("TASK-2")).thenReturn(Optional.of(item));
        doThrow(new BusinessException(ErrorCode.FORBIDDEN))
                .when(workItemService).requireTransitionActor(item, 7L, "进行中");

        assertThrows(BusinessException.class, () -> service.update(1L, "TASK-2", statusRequest("进行中"), 7L));
        verify(workItems, never()).save(any());
    }

    /** 允许的流转（无需说明/工时）仍可批量执行。 */
    @Test
    void allowsPlainStatusTransitionForAuthorizedActor() {
        WorkItem item = WorkItem.builder().id("TASK-3").projectId(1L).type("任务").status("新建").ownerId(8L).creatorId(9L).build();
        when(workItems.findById("TASK-3")).thenReturn(Optional.of(item));
        when(workItems.save(any(WorkItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update(1L, "TASK-3", statusRequest("进行中"), 8L);

        verify(workItemService).requireTransitionActor(item, 8L, "进行中");
        verify(workItems).save(item);
    }
}
