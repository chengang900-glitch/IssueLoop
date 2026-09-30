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
    /** 人员/项目分组键是数值 ID，必须按数值排序（历史实现用 TreeMap<String> 会给出 1,10,2）。 */
    @Test
    @SuppressWarnings("unchecked")
    void peopleGroupsAreOrderedNumericallyNotLexicographically() {
        WorkItemRepository items = mock(WorkItemRepository.class);
        ProjectService projects = mock(ProjectService.class);
        ProjectMemberRepository members = mock(ProjectMemberRepository.class);
        UserRepository users = mock(UserRepository.class);
        when(projects.listVisibleProjects(1L)).thenReturn(List.of(Project.builder().id(1L).name("测试项目").build()));
        when(members.findByUserId(1L)).thenReturn(List.of());
        when(members.findByProjectId(1L)).thenReturn(List.of());
        WorkItem owner2 = WorkItem.builder().id("T-2").projectId(1L).title("二号任务").type("任务")
                .status("新建").priority("P2").ownerId(2L).build();
        WorkItem owner10 = WorkItem.builder().id("T-10").projectId(1L).title("十号任务").type("任务")
                .status("新建").priority("P2").ownerId(10L).build();
        when(items.findAll(any(Specification.class))).thenReturn(List.of(owner10, owner2));
        when(users.findAllById(any())).thenReturn(List.of(
                User.builder().id(2L).username("u2").nickname("二号").build(),
                User.builder().id(10L).username("u10").nickname("十号").build()));
        AnalyticsQuery q = new AnalyticsQuery();
        q.setScope("project");
        q.setProjectId(1L);
        q.setGroupBy("person");
        AnalyticsService service = new AnalyticsService(items, projects, members, users);

        Map<String, Object> result = service.analyze(q, 1L);

        List<Map<String, Object>> people = (List<Map<String, Object>>) result.get("people");
        List<String> keys = people.stream().map(group -> String.valueOf(group.get("key"))).collect(java.util.stream.Collectors.toList());
        assertEquals(List.of("2", "10"), keys, "数值型分组键必须按数值排序");
        assertEquals("二号", people.get(0).get("label"));
        assertEquals("十号", people.get(1).get("label"));
    }

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
