package com.rnd.app.service;

import com.rnd.app.entity.ProjectMember;
import com.rnd.app.entity.Project;
import com.rnd.app.entity.ProjectCodeCounter;
import com.rnd.app.entity.ProjectMilestone;
import com.rnd.app.dto.CreateProjectRequest;
import com.rnd.app.dto.ProjectMilestoneDto;
import com.rnd.app.repository.*;
import com.rnd.app.util.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    @Mock ProjectRepository projectRepo;
    @Mock ProjectMemberRepository memberRepo;
    @Mock ModuleRepository moduleRepo;
    @Mock SprintRepository sprintRepo;
    @Mock UserRepository userRepo;
    @Mock WorkItemRepository workItemRepo;
    @Mock ProjectCodeCounterRepository codeCounterRepo;
    @Mock ProjectMilestoneRepository milestoneRepo;
    @Mock SystemSettingService systemSettingService;
    @Mock ProjectTypeService projectTypeService;
    @InjectMocks ProjectService service;

    @Test
    void guestCannotWriteButMemberCan() {
        when(memberRepo.findByProjectIdAndUserId(1L, 10L))
                .thenReturn(Optional.of(ProjectMember.builder().projectId(1L).userId(10L).role("GUEST").build()));
        when(memberRepo.findByProjectIdAndUserId(1L, 11L))
                .thenReturn(Optional.of(ProjectMember.builder().projectId(1L).userId(11L).role("MEMBER").build()));
        when(projectRepo.findById(1L)).thenReturn(Optional.of(Project.builder().id(1L).archived(false).build()));

        assertThrows(BusinessException.class, () -> service.ensureProjectWriter(1L, 10L));
        assertDoesNotThrow(() -> service.ensureProjectWriter(1L, 11L));
    }

    @Test
    void nonMemberCannotUseMemberActions() {
        when(memberRepo.existsByProjectIdAndUserId(1L, 12L)).thenReturn(false);
        assertThrows(BusinessException.class, () -> service.ensureProjectMember(1L, 12L));
    }

    @Test
    void unsupportedProjectRolesAreRejected() {
        assertThrows(BusinessException.class, () -> service.addMember(1L, 10L, "OWNER"));
        assertThrows(BusinessException.class, () -> service.updateMemberRole(1L, 10L, "OWNER"));
    }

    @Test
    void archivedProjectRejectsWritersAndProjectAdmins() {
        when(projectRepo.findById(1L)).thenReturn(Optional.of(Project.builder().id(1L).archived(true).build()));
        when(memberRepo.findByProjectIdAndUserId(1L, 11L))
                .thenReturn(Optional.of(ProjectMember.builder().projectId(1L).userId(11L).role("MEMBER").build()));
        when(memberRepo.findByProjectIdAndUserId(1L, 12L))
                .thenReturn(Optional.of(ProjectMember.builder().projectId(1L).userId(12L).role("PROJECT_ADMIN").build()));

        assertThrows(BusinessException.class, () -> service.ensureProjectWriter(1L, 11L));
        assertThrows(BusinessException.class, () -> service.ensureProjectAdmin(1L, 12L));
    }

    @Test
    void projectDtoIncludesCurrentUserProjectRole() {
        Project project = Project.builder().id(1L).name("项目").shortName("P").createdBy(1L).build();
        when(memberRepo.findByProjectIdAndUserId(1L, 11L))
                .thenReturn(Optional.of(ProjectMember.builder().projectId(1L).userId(11L).role("PROJECT_ADMIN").build()));

        assertEquals("PROJECT_ADMIN", service.toDto(project, 11L).getCurrentUserProjectRole());
    }

    @Test
    void summaryUsesRealDueAndP0Counts() {
        when(workItemRepo.countByProjectIdAndStatusIn(1L, java.util.Arrays.asList("新建", "进行中", "延期处理", "已完成"))).thenReturn(3L);
        when(workItemRepo.countByProjectIdAndStatus(1L, "新建")).thenReturn(2L);
        when(workItemRepo.countByProjectIdAndStatus(1L, "进行中")).thenReturn(1L);
        when(workItemRepo.countByProjectIdAndStatus(1L, "已完成")).thenReturn(1L);
        when(workItemRepo.countByProjectIdAndDueDateBetweenAndStatusNotIn(any(), any(), any(), anyList())).thenReturn(4L);
        when(workItemRepo.countByProjectIdAndPriorityAndStatusNotIn(1L, "P0", java.util.Arrays.asList("已验收", "已拒绝"))).thenReturn(2L);

        var summary = service.summary(1L);

        assertEquals(4L, summary.getTodayDue());
        assertEquals(2L, summary.getP0Urgent());
    }

    @Test
    void createProjectGeneratesMonthlyCodeAndDefaults() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("客户实施"); request.setShortName("实施");
        when(systemSettingService.getProjectCodePrefixForUpdate()).thenReturn("PRJ");
        when(projectTypeService.requireActive("实施")).thenReturn(com.rnd.app.entity.ProjectType.builder().name("实施").enabled(true).build());
        when(codeCounterRepo.findForUpdate(any(), any())).thenReturn(Optional.empty());
        when(projectRepo.save(any(Project.class))).thenAnswer(invocation -> {
            Project project = invocation.getArgument(0); project.setId(1L); return project;
        });

        Project project = service.createProject(request, 9L);

        org.junit.jupiter.api.Assertions.assertTrue(project.getCode().matches("PRJ-\\d{6}-001"));
        assertEquals("实施", project.getProjectType());
        assertEquals("未启动", project.getProjectStatus());
        assertEquals("未分区", project.getWorkZone());
        assertEquals("未分区", service.toDto(project).getWorkZone());
        verify(codeCounterRepo).save(any(ProjectCodeCounter.class));
    }

    @Test
    void unsupportedProjectWorkZoneIsRejected() {
        CreateProjectRequest request = new CreateProjectRequest();
        request.setName("分区项目"); request.setShortName("分区"); request.setWorkZone("E");
        when(systemSettingService.getProjectCodePrefixForUpdate()).thenReturn("PRJ");
        when(projectTypeService.requireActive("实施")).thenReturn(com.rnd.app.entity.ProjectType.builder().name("实施").enabled(true).build());
        when(codeCounterRepo.findForUpdate(any(), any())).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.createProject(request, 9L));
    }

    @Test
    void completedMilestoneRequiresCompletionDate() {
        ProjectMilestoneDto request = new ProjectMilestoneDto();
        request.setName("上线"); request.setPlannedDate(java.time.LocalDate.now()); request.setStatus("已完成");
        assertThrows(BusinessException.class, () -> service.createMilestone(1L, request));
    }

    @Test
    void milestoneWithCompletionDateIsCompleted() {
        ProjectMilestoneDto request = new ProjectMilestoneDto();
        request.setName("上线"); request.setPlannedDate(java.time.LocalDate.now());
        request.setCompletedDate(java.time.LocalDate.now()); request.setStatus("进行中");
        when(milestoneRepo.save(any(ProjectMilestone.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals("已完成", service.createMilestone(1L, request).getStatus());
    }
}
