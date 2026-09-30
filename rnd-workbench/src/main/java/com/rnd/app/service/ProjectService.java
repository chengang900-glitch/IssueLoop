package com.rnd.app.service;

import com.rnd.app.dto.*;
import com.rnd.app.entity.*;
import com.rnd.app.repository.*;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepo;
    private final ProjectMemberRepository memberRepo;
    private final ModuleRepository moduleRepo;
    private final SprintRepository sprintRepo;
    private final UserRepository userRepo;
    private final WorkItemRepository workItemRepo;
    private final ProjectCodeCounterRepository codeCounterRepo;
    private final ProjectMilestoneRepository milestoneRepo;
    private final SystemSettingService systemSettingService;
    private final ProjectTypeService projectTypeService;

    private static final List<String> WORK_ZONES = List.of("A", "B", "C", "D", "未分区");
    private static final List<String> PHASES = List.of("立项", "实施", "开发", "测试", "上线", "验收", "运维");
    private static final List<String> PROJECT_STATUSES = List.of("未启动", "进行中", "已暂停", "已完成", "已关闭");
    private static final List<String> HEALTH_STATUSES = List.of("正常", "关注", "风险");
    private static final List<String> IMPLEMENTATION_MODES = List.of("驻场", "远程", "混合");
    private static final List<String> MILESTONE_STATUSES = List.of("未开始", "进行中", "已完成", "已延期");

    // ==================== 项目 ====================

    public List<Project> listVisibleProjects(Long userId) {
        List<Long> projectIds = memberRepo.findByUserId(userId).stream()
                .map(ProjectMember::getProjectId).collect(Collectors.toList());
        // 空集合时直接返回：既不产生 IN ()，也省一次查询
        if (projectIds.isEmpty()) return List.of();
        return projectRepo.findByIdInAndArchivedFalse(projectIds);
    }

    public List<Project> listAllProjects() {
        return projectRepo.findAll();
    }

    /** 按 ID 取单个项目（不再为了取一行而 findAll 全表）。 */
    public Project requireProject(Long projectId) {
        return projectRepo.findById(projectId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Transactional
    public Project createProject(CreateProjectRequest req, Long creatorId) {
        Project p = Project.builder()
                .name(req.getName()).shortName(req.getShortName())
                .description(req.getDescription()).createdBy(creatorId)
                .code(nextProjectCode()).build();
        applyProjectProfile(p, req, true);
        p = projectRepo.save(p);
        // 创建者自动为项目管理员
        memberRepo.save(ProjectMember.builder()
                .projectId(p.getId()).userId(creatorId).role("PROJECT_ADMIN").build());
        // 可选初始成员
        if (req.getMemberIds() != null) {
            for (Long mid : req.getMemberIds()) {
                if (!mid.equals(creatorId))
                    memberRepo.save(ProjectMember.builder().projectId(p.getId()).userId(mid).build());
            }
        }
        return p;
    }

    public ProjectDto toDto(Project p) {
        ProjectDto dto = new ProjectDto(p.getId(), p.getName(), p.getShortName(), p.getDescription(), p.getCreatedAt());
        dto.setArchived(p.isArchived());
        dto.setUpdatedAt(p.getUpdatedAt());
        dto.setCode(p.getCode()); dto.setProjectType(p.getProjectType()); dto.setWorkZone(p.getWorkZone()); dto.setBusinessLine(p.getBusinessLine());
        dto.setCustomerName(p.getCustomerName()); dto.setDeliveryLocation(p.getDeliveryLocation());
        dto.setProjectManagerId(p.getProjectManagerId()); dto.setImplementationLeadId(p.getImplementationLeadId());
        dto.setDevelopmentLeadId(p.getDevelopmentLeadId()); dto.setCustomerContact(p.getCustomerContact());
        dto.setPlannedStartDate(p.getPlannedStartDate()); dto.setPlannedEndDate(p.getPlannedEndDate());
        dto.setActualStartDate(p.getActualStartDate()); dto.setActualEndDate(p.getActualEndDate());
        dto.setPhase(p.getPhase()); dto.setProjectStatus(p.getProjectStatus()); dto.setHealthStatus(p.getHealthStatus());
        dto.setScope(p.getScope()); dto.setDeliverables(p.getDeliverables()); dto.setAcceptanceCriteria(p.getAcceptanceCriteria());
        dto.setRiskDescription(p.getRiskDescription()); dto.setCurrentIssues(p.getCurrentIssues()); dto.setNextSteps(p.getNextSteps());
        dto.setImplementationMode(p.getImplementationMode()); dto.setGoLiveDate(p.getGoLiveDate()); dto.setSupportEndDate(p.getSupportEndDate());
        return dto;
    }

    public ProjectDto toDto(Project p, Long currentUserId) {
        ProjectDto dto = toDto(p);
        memberRepo.findByProjectIdAndUserId(p.getId(), currentUserId)
                .ifPresent(member -> dto.setCurrentUserProjectRole(member.getRole()));
        return dto;
    }

    @Transactional
    public void updateProject(Long projectId, CreateProjectRequest req) {
        Project p = projectRepo.findById(projectId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (req.getName() != null) p.setName(req.getName());
        if (req.getShortName() != null) p.setShortName(req.getShortName());
        if (req.getDescription() != null) p.setDescription(req.getDescription());
        applyProjectProfile(p, req, false);
        projectRepo.save(p);
    }

    private String nextProjectCode() {
        String prefix = systemSettingService.getProjectCodePrefixForUpdate();
        String period = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        ProjectCodeCounter counter = codeCounterRepo.findForUpdate(prefix, period)
                .orElseGet(() -> new ProjectCodeCounter(prefix, period, 0));
        counter.setLastSequence(counter.getLastSequence() + 1);
        codeCounterRepo.save(counter);
        return String.format("%s-%s-%03d", prefix, period, counter.getLastSequence());
    }

    private void applyProjectProfile(Project p, CreateProjectRequest req, boolean creating) {
        if (creating && req.getProjectType() == null) p.setProjectType(projectTypeService.requireActive("实施").getName());
        if (req.getProjectType() != null && !req.getProjectType().trim().equals(p.getProjectType()))
            p.setProjectType(projectTypeService.requireActive(req.getProjectType()).getName());
        p.setWorkZone(valueOrDefault(req.getWorkZone(), p.getWorkZone(), "未分区", WORK_ZONES, "工作分区"));
        p.setPhase(valueOrDefault(req.getPhase(), p.getPhase(), "立项", PHASES, "当前阶段"));
        p.setProjectStatus(valueOrDefault(req.getProjectStatus(), p.getProjectStatus(), creating ? "未启动" : "进行中", PROJECT_STATUSES, "项目状态"));
        p.setHealthStatus(valueOrDefault(req.getHealthStatus(), p.getHealthStatus(), "正常", HEALTH_STATUSES, "健康状态"));
        p.setImplementationMode(valueOrDefault(req.getImplementationMode(), p.getImplementationMode(), "混合", IMPLEMENTATION_MODES, "实施方式"));
        if (req.getBusinessLine() != null) p.setBusinessLine(req.getBusinessLine());
        if (req.getCustomerName() != null) p.setCustomerName(req.getCustomerName());
        if (req.getDeliveryLocation() != null) p.setDeliveryLocation(req.getDeliveryLocation());
        if (req.getCustomerContact() != null) p.setCustomerContact(req.getCustomerContact());
        if (req.getProjectManagerId() != null) { if (req.getProjectManagerId() <= 0) p.setProjectManagerId(null); else { ensureActiveUser(req.getProjectManagerId()); p.setProjectManagerId(req.getProjectManagerId()); } }
        if (req.getImplementationLeadId() != null) { if (req.getImplementationLeadId() <= 0) p.setImplementationLeadId(null); else { ensureActiveUser(req.getImplementationLeadId()); p.setImplementationLeadId(req.getImplementationLeadId()); } }
        if (req.getDevelopmentLeadId() != null) { if (req.getDevelopmentLeadId() <= 0) p.setDevelopmentLeadId(null); else { ensureActiveUser(req.getDevelopmentLeadId()); p.setDevelopmentLeadId(req.getDevelopmentLeadId()); } }
        if (req.getPlannedStartDate() != null) p.setPlannedStartDate(req.getPlannedStartDate());
        if (req.getPlannedEndDate() != null) p.setPlannedEndDate(req.getPlannedEndDate());
        if (req.getActualStartDate() != null) p.setActualStartDate(req.getActualStartDate());
        if (req.getActualEndDate() != null) p.setActualEndDate(req.getActualEndDate());
        if (req.getScope() != null) p.setScope(req.getScope()); if (req.getDeliverables() != null) p.setDeliverables(req.getDeliverables());
        if (req.getAcceptanceCriteria() != null) p.setAcceptanceCriteria(req.getAcceptanceCriteria()); if (req.getRiskDescription() != null) p.setRiskDescription(req.getRiskDescription());
        if (req.getCurrentIssues() != null) p.setCurrentIssues(req.getCurrentIssues()); if (req.getNextSteps() != null) p.setNextSteps(req.getNextSteps());
        if (req.getGoLiveDate() != null) p.setGoLiveDate(req.getGoLiveDate()); if (req.getSupportEndDate() != null) p.setSupportEndDate(req.getSupportEndDate());
        validateDateRange(p.getPlannedStartDate(), p.getPlannedEndDate(), "计划结束日期不得早于计划开始日期");
        validateDateRange(p.getActualStartDate(), p.getActualEndDate(), "实际结束日期不得早于实际开始日期");
    }

    private String valueOrDefault(String value, String existing, String fallback, List<String> allowed, String label) {
        String result = value != null ? value : (existing != null ? existing : fallback);
        if (!allowed.contains(result)) throw new BusinessException(ErrorCode.BAD_REQUEST, label + "无效");
        return result;
    }

    private void ensureActiveUser(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "负责人不存在"));
        if (!Integer.valueOf(1).equals(user.getStatus())) throw new BusinessException(ErrorCode.BAD_REQUEST, "负责人已禁用");
    }

    private void validateDateRange(LocalDate start, LocalDate end, String message) {
        if (start != null && end != null && end.isBefore(start)) throw new BusinessException(ErrorCode.BAD_REQUEST, message);
    }

    @Transactional
    public void setArchived(Long projectId, boolean archived) {
        Project project = projectRepo.findById(projectId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        project.setArchived(archived);
        projectRepo.save(project);
    }

    // ==================== 里程碑 ====================

    public List<ProjectMilestoneDto> listMilestones(Long projectId) {
        return milestoneRepo.findByProjectIdOrderByPlannedDateAsc(projectId).stream()
                .map(this::toMilestoneDto).collect(Collectors.toList());
    }

    @Transactional
    public ProjectMilestoneDto createMilestone(Long projectId, ProjectMilestoneDto req) {
        ProjectMilestone milestone = ProjectMilestone.builder().projectId(projectId).build();
        applyMilestone(milestone, req);
        return toMilestoneDto(milestoneRepo.save(milestone));
    }

    @Transactional
    public ProjectMilestoneDto updateMilestone(Long milestoneId, ProjectMilestoneDto req) {
        ProjectMilestone milestone = milestoneRepo.findById(milestoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        applyMilestone(milestone, req);
        return toMilestoneDto(milestoneRepo.save(milestone));
    }

    @Transactional
    public void deleteMilestone(Long milestoneId) {
        if (!milestoneRepo.existsById(milestoneId)) throw new BusinessException(ErrorCode.NOT_FOUND);
        milestoneRepo.deleteById(milestoneId);
    }

    public Long milestoneProjectId(Long milestoneId) {
        return milestoneRepo.findById(milestoneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND)).getProjectId();
    }

    private void applyMilestone(ProjectMilestone milestone, ProjectMilestoneDto req) {
        if (req.getName() != null) milestone.setName(req.getName().trim());
        if (milestone.getName() == null || milestone.getName().isEmpty())
            throw new BusinessException(ErrorCode.BAD_REQUEST, "里程碑名称不能为空");
        if (req.getPlannedDate() != null) milestone.setPlannedDate(req.getPlannedDate());
        if (milestone.getPlannedDate() == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "计划日期不能为空");
        milestone.setCompletedDate(req.getCompletedDate());
        String status = req.getStatus() != null ? req.getStatus() : (milestone.getStatus() != null ? milestone.getStatus() : "未开始");
        if (!MILESTONE_STATUSES.contains(status)) throw new BusinessException(ErrorCode.BAD_REQUEST, "里程碑状态无效");
        if (milestone.getCompletedDate() != null && !"已延期".equals(status)) status = "已完成";
        if (milestone.getCompletedDate() == null && "已完成".equals(status))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "已完成里程碑必须填写实际完成日期");
        milestone.setStatus(status);
        if (req.getDescription() != null) milestone.setDescription(req.getDescription());
    }

    private ProjectMilestoneDto toMilestoneDto(ProjectMilestone milestone) {
        ProjectMilestoneDto dto = new ProjectMilestoneDto();
        dto.setId(milestone.getId()); dto.setProjectId(milestone.getProjectId()); dto.setName(milestone.getName());
        dto.setPlannedDate(milestone.getPlannedDate()); dto.setCompletedDate(milestone.getCompletedDate());
        dto.setStatus(milestone.getStatus()); dto.setDescription(milestone.getDescription());
        dto.setCreatedAt(milestone.getCreatedAt()); dto.setUpdatedAt(milestone.getUpdatedAt());
        return dto;
    }

    // ==================== 成员 ====================

    public List<ProjectMemberDto> listMembers(Long projectId) {
        return memberRepo.findByProjectId(projectId).stream().map(m -> {
            User u = userRepo.findById(m.getUserId()).orElse(null);
            return new ProjectMemberDto(m.getUserId(),
                    u != null ? u.getUsername() : null,
                    u != null ? u.getNickname() : null,
                    m.getRole());
        }).collect(Collectors.toList());
    }

    @Transactional
    public void addMember(Long projectId, Long userId, String role) {
        String memberRole = role != null ? role : "MEMBER";
        validateProjectRole(memberRole);
        if (memberRepo.existsByProjectIdAndUserId(projectId, userId))
            throw new BusinessException(ErrorCode.DUPLICATE, "该用户已是项目成员");
        memberRepo.save(ProjectMember.builder()
                .projectId(projectId).userId(userId)
                .role(memberRole).build());
    }

    @Transactional
    public void updateMemberRole(Long projectId, Long userId, String role) {
        validateProjectRole(role);
        ProjectMember pm = memberRepo.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if ("PROJECT_ADMIN".equals(pm.getRole()) && !"PROJECT_ADMIN".equals(role)
                && memberRepo.countByProjectIdAndRole(projectId, "PROJECT_ADMIN") <= 1) {
            throw new BusinessException(ErrorCode.STATUS_CONFLICT, "项目至少保留一名项目管理员");
        }
        pm.setRole(role);
        memberRepo.save(pm);
    }

    private void validateProjectRole(String role) {
        if (!List.of("PROJECT_ADMIN", "MEMBER", "GUEST").contains(role)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的项目角色");
        }
    }

    @Transactional
    public void removeMember(Long projectId, Long userId) {
        memberRepo.findByProjectIdAndUserId(projectId, userId).ifPresent(member -> {
            if ("PROJECT_ADMIN".equals(member.getRole())
                    && memberRepo.countByProjectIdAndRole(projectId, "PROJECT_ADMIN") <= 1) {
                throw new BusinessException(ErrorCode.STATUS_CONFLICT, "项目至少保留一名项目管理员");
            }
            memberRepo.delete(member);
        });
    }

    // ==================== 模块 ====================

    public List<ModuleEntity> listModules(Long projectId) {
        return moduleRepo.findByProjectIdOrderByName(projectId);
    }

    @Transactional
    public ModuleEntity createModule(Long projectId, String name, Long parentId) {
        if (moduleRepo.existsByProjectIdAndName(projectId, name))
            throw new BusinessException(ErrorCode.DUPLICATE, "模块名已存在");
        if (parentId != null) {
            ModuleEntity parent = moduleRepo.findById(parentId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
            if (!projectId.equals(parent.getProjectId()) || parent.getParentId() != null)
                throw new BusinessException(ErrorCode.BAD_REQUEST, "子模块必须归属于当前项目的主模块");
        }
        return moduleRepo.save(ModuleEntity.builder().projectId(projectId).name(name).parentId(parentId).build());
    }

    @Transactional
    public void deleteModule(Long moduleId) {
        moduleRepo.deleteById(moduleId);
    }

    // ==================== 迭代 ====================

    public List<Sprint> listSprints(Long projectId) {
        return sprintRepo.findByProjectIdOrderByStartDateDesc(projectId);
    }

    @Transactional
    public Sprint createSprint(Long projectId, CreateSprintRequest req) {
        return sprintRepo.save(Sprint.builder()
                .projectId(projectId).name(req.getName())
                .startDate(req.getStartDate()).endDate(req.getEndDate()).build());
    }

    @Transactional
    public Sprint updateSprint(Long sprintId, UpdateSprintRequest req) {
        Sprint s = sprintRepo.findById(sprintId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (req.getName() != null) s.setName(req.getName());
        if (req.getStartDate() != null) s.setStartDate(req.getStartDate());
        if (req.getEndDate() != null) s.setEndDate(req.getEndDate());
        if (req.getStatus() != null) s.setStatus(req.getStatus());
        return sprintRepo.save(s);
    }

    @Transactional
    public void deleteSprint(Long sprintId) {
        sprintRepo.deleteById(sprintId);
    }

    // ==================== 权限校验 ====================

    public void ensureProjectMember(Long projectId, Long userId) {
        if (!memberRepo.existsByProjectIdAndUserId(projectId, userId))
            throw new BusinessException(ErrorCode.FORBIDDEN, "不是项目成员");
    }

    public void ensureProjectWriter(Long projectId, Long userId) {
        ProjectMember pm = memberRepo.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
        if ("GUEST".equals(pm.getRole()))
            throw new BusinessException(ErrorCode.FORBIDDEN, "访客仅有只读权限");
        ensureActiveProject(projectId);
    }

    public void ensureProjectAdmin(Long projectId, Long userId) {
        ProjectMember pm = memberRepo.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
        if (!"PROJECT_ADMIN".equals(pm.getRole()))
            throw new BusinessException(ErrorCode.FORBIDDEN, "需要项目管理员权限");
        ensureActiveProject(projectId);
    }

    public void ensureActiveProject(Long projectId) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (project.isArchived()) {
            throw new BusinessException(ErrorCode.STATUS_CONFLICT, "项目已归档，仅可查看和导出");
        }
    }

    public Long moduleProjectId(Long moduleId) {
        return moduleRepo.findById(moduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND)).getProjectId();
    }

    public Long sprintProjectId(Long sprintId) {
        return sprintRepo.findById(sprintId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND)).getProjectId();
    }

    // ==================== 汇总 ====================

    public ProjectSummaryDto summary(Long projectId) {
        long todo = workItemRepo.countByProjectIdAndStatusIn(projectId, Arrays.asList("新建", "进行中", "延期处理", "已完成"));
        long inbox = workItemRepo.countByProjectIdAndStatus(projectId, "新建");
        long progress = workItemRepo.countByProjectIdAndStatus(projectId, "进行中");
        long verify = workItemRepo.countByProjectIdAndStatus(projectId, "已完成");
        java.time.ZoneId zone = java.time.ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zone);
        Instant from = today.atStartOfDay(zone).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(zone).minusNanos(1).toInstant();
        List<String> finalStatuses = Arrays.asList("已验收", "已拒绝");
        long due = workItemRepo.countByProjectIdAndDueDateBetweenAndStatusNotIn(projectId, from, to, finalStatuses);
        long p0 = workItemRepo.countByProjectIdAndPriorityAndStatusNotIn(projectId, "P0", finalStatuses);
        return new ProjectSummaryDto(todo, inbox, progress, verify, due, p0);
    }

    // ==================== 可访问项目 ID 列表 ====================
    public List<Long> userProjectIds(Long userId) {
        return memberRepo.findByUserId(userId).stream()
                .map(ProjectMember::getProjectId).collect(Collectors.toList());
    }
}
