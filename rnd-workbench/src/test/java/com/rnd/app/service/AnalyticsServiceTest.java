package com.rnd.app.service;

import com.rnd.app.dto.AnalyticsQuery;
import com.rnd.app.entity.*;
import com.rnd.app.repository.*;
import com.rnd.app.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AnalyticsServiceTest {
    @Test void oversizedExportIsRejectedRatherThanTruncated() {
        WorkItemRepository items = mock(WorkItemRepository.class);
        ProjectService projects = mock(ProjectService.class);
        ProjectMemberRepository members = mock(ProjectMemberRepository.class);
        UserRepository users = mock(UserRepository.class);
        when(projects.listVisibleProjects(1L)).thenReturn(List.of(Project.builder().id(1L).name("测试项目").build()));
        when(members.findByUserId(1L)).thenReturn(List.of());
        when(members.findByProjectId(1L)).thenReturn(List.of());
        when(users.findAllById(any())).thenReturn(List.of());
        WorkItem item = WorkItem.builder().id("T-1").projectId(1L).title("测试").status("进行中").priority("P2").type("任务").build();
        when(items.findAll(any(Specification.class))).thenReturn(Collections.nCopies(10001, item));
        AnalyticsQuery q = new AnalyticsQuery(); q.setScope("project"); q.setProjectId(1L);
        AnalyticsService service = new AnalyticsService(items, projects, members, users);
        BusinessException error = assertThrows(BusinessException.class, () -> service.export(q, 1L, "details"));
        assertTrue(error.getMessage().contains("10000"));
    }
}
