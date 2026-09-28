package com.rnd.app.service;

import com.rnd.app.dto.TaskTypeDto;
import com.rnd.app.entity.TaskType;
import com.rnd.app.repository.TaskTypeRepository;
import com.rnd.app.repository.WorkItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskTypeServiceTest {
    @Mock TaskTypeRepository repository;
    @Mock WorkItemRepository workItemRepository;
    @InjectMocks TaskTypeService service;

    @Test
    void createsEnabledTaskType() {
        TaskTypeDto request = new TaskTypeDto(); request.setName("文档");
        when(repository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(Collections.emptyList());
        when(repository.save(any())).thenAnswer(invocation -> {
            TaskType type = invocation.getArgument(0); type.setId(5L); return type;
        });
        TaskTypeDto created = service.create(request);
        assertEquals("文档", created.getName());
        assertTrue(created.getEnabled());
    }

    @Test
    void renamesHistoricalWorkItemsWithType() {
        TaskType type = TaskType.builder().id(1L).name("任务").codePrefix("TASK").enabled(true).sortOrder(10).nextValue(3).build();
        TaskTypeDto request = new TaskTypeDto(); request.setName("实施任务");
        when(repository.findById(1L)).thenReturn(Optional.of(type));
        when(repository.findByName("实施任务")).thenReturn(Optional.empty());
        when(repository.save(type)).thenReturn(type);
        service.update(1L, request);
        verify(workItemRepository).updateTypeName("任务", "实施任务");
        assertEquals("实施任务", type.getName());
    }
}
