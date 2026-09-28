package com.rnd.app.repository;

import com.rnd.app.entity.Project;
import com.rnd.app.entity.User;
import com.rnd.app.entity.WorkItem;
import com.rnd.app.entity.WorkItemWatcher;
import com.rnd.app.service.WorkItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class WorkItemPersonalViewTest {
    @Autowired UserRepository userRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired WorkItemRepository workItemRepository;
    @Autowired WorkItemWatcherRepository watcherRepository;
    @Autowired WorkItemService workItemService;

    @Test
    void personalViewsApplyTheirStatusAndUserRules() {
        User admin = userRepository.findByUsername("admin@uhoo.cn").orElseThrow();
        User other = userRepository.save(User.builder()
                .username("personal-view-user@uhoo.cn")
                .passwordHash("not-used")
                .nickname("视图测试用户")
                .build());
        Project project = projectRepository.save(Project.builder()
                .code("PVIEW-TEST-001")
                .name("个人视图测试项目")
                .shortName("PVIEW")
                .createdBy(admin.getId())
                .build());

        saveItem("PV-9001", 9001, project.getId(), admin.getId(), other.getId(), "新建");
        saveItem("PV-9002", 9002, project.getId(), other.getId(), admin.getId(), "进行中");
        saveItem("PV-9003", 9003, project.getId(), other.getId(), admin.getId(), "已完成");
        saveItem("PV-9004", 9004, project.getId(), other.getId(), other.getId(), "已验收");
        watcherRepository.save(WorkItemWatcher.builder()
                .workItemId("PV-9001")
                .userId(admin.getId())
                .build());

        assertEquals(List.of("PV-9001"), ids(project.getId(), "created-by-me", admin.getId()));
        assertEquals(List.of("PV-9002", "PV-9003"), ids(project.getId(), "assigned-to-me", admin.getId()));
        assertEquals(List.of("PV-9002", "PV-9003"), ids(project.getId(), "pending-for-me", admin.getId()));
        assertEquals(List.of("PV-9001"), ids(project.getId(), "watched-by-me", admin.getId()));
        assertEquals(List.of("PV-9001", "PV-9002", "PV-9003"), ids(project.getId(), "unclosed", admin.getId()));
        assertEquals(List.of("PV-9001", "PV-9002", "PV-9003", "PV-9004"), ids(project.getId(), null, admin.getId()));
    }

    private void saveItem(String id, int seqNo, Long projectId, Long creatorId, Long ownerId, String status) {
        workItemRepository.save(WorkItem.builder()
                .id(id)
                .seqNo(seqNo)
                .projectId(projectId)
                .type("任务")
                .title(id)
                .status(status)
                .creatorId(creatorId)
                .ownerId(ownerId)
                .build());
    }

    private List<String> ids(Long projectId, String view, Long currentUserId) {
        return workItemService.search(
                        projectId, null, null, null, null, null, null, null,
                        null, null, null, null, null, view, currentUserId,
                        PageRequest.of(0, 20))
                .getContent().stream()
                .map(item -> item.getId())
                .sorted()
                .collect(Collectors.toList());
    }
}
