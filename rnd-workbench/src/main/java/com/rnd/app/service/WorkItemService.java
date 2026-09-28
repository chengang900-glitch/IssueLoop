package com.rnd.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnd.app.dto.*;
import com.rnd.app.entity.*;
import com.rnd.app.repository.*;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.criteria.Predicate;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkItemService {

    private final WorkItemRepository workItemRepo;
    private final WorkItemIdGenerator idGenerator;
    private final WorkItemStepRepository stepRepo;
    private final ActivityRepository activityRepo;
    private final UserRepository userRepo;
    private final WorkItemWatcherRepository watcherRepo;
    private final WorkItemRelationRepository relationRepo;
    private final ProjectMemberRepository projectMemberRepo;
    private final ModuleRepository moduleRepo;
    private final SprintRepository sprintRepo;
    private final ObjectMapper mapper;
    private final NotificationService notificationService;
    private final RichTextSanitizer richTextSanitizer;
    private final TaskTypeService taskTypeService;

    // 状态机规则
    private static final Map<String, Set<String>> STATE_TRANSITIONS = new HashMap<>();
    static {
        STATE_TRANSITIONS.put("新建", new HashSet<>(Arrays.asList("进行中", "已拒绝")));
        STATE_TRANSITIONS.put("进行中", new HashSet<>(Arrays.asList("延期处理", "已完成", "已拒绝")));
        STATE_TRANSITIONS.put("延期处理", new HashSet<>(Collections.singletonList("进行中")));
        STATE_TRANSITIONS.put("已完成", new HashSet<>(Arrays.asList("已验收", "验收不通过")));
        STATE_TRANSITIONS.put("验收不通过", new HashSet<>(Collections.singletonList("进行中")));
        STATE_TRANSITIONS.put("已拒绝", new HashSet<>(Collections.singletonList("进行中")));
        STATE_TRANSITIONS.put("已验收", Collections.emptySet());
    }

    private static final List<String> FINAL_STATUSES = Arrays.asList("已验收", "已拒绝");
    private static final Set<String> PRIORITIES = Set.of("P0", "P1", "P2", "P3");

    public static boolean isValidTransition(String from, String to) {
        Set<String> allowed = STATE_TRANSITIONS.get(from);
        return allowed != null && allowed.contains(to);
    }

    // ==================== 查询 ====================

    private static final Set<String> PERSONAL_VIEWS = new HashSet<>(Arrays.asList(
            "created-by-me", "assigned-to-me", "pending-for-me", "watched-by-me", "unclosed"));

    public Page<WorkItemDto> search(Long projectId, String type, String status, Long ownerId,
                                    Long creatorId, Long sprintId, String priority, String severity,
                                    String module, String tag, Instant dueFrom, Instant dueTo,
                                    String keyword, String view, Long currentUserId, Pageable pageable) {
        if (view != null && !PERSONAL_VIEWS.contains(view))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的个人视图");
        String selectedView = view == null ? "all" : view;
        Specification<WorkItem> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("projectId"), projectId));
            if (StringUtils.hasText(type)) predicates.add(builder.equal(root.get("type"), type));
            if (StringUtils.hasText(status)) predicates.add(builder.equal(root.get("status"), status));
            if (ownerId != null) predicates.add(builder.equal(root.get("ownerId"), ownerId));
            if (creatorId != null) predicates.add(builder.equal(root.get("creatorId"), creatorId));
            if (sprintId != null) predicates.add(builder.equal(root.get("sprintId"), sprintId));
            if (StringUtils.hasText(priority)) predicates.add(builder.equal(root.get("priority"), priority));
            if (StringUtils.hasText(severity)) predicates.add(builder.equal(root.get("severity"), severity));
            if (StringUtils.hasText(module)) predicates.add(builder.equal(root.get("module"), module));
            if (StringUtils.hasText(tag)) predicates.add(builder.like(root.get("tags"), "%" + tag + "%"));
            if (dueFrom != null) predicates.add(builder.greaterThanOrEqualTo(root.get("dueDate"), dueFrom));
            if (dueTo != null) predicates.add(builder.lessThanOrEqualTo(root.get("dueDate"), dueTo));
            if (StringUtils.hasText(keyword)) {
                String pattern = "%" + keyword + "%";
                predicates.add(builder.or(
                        builder.like(root.get("title"), pattern), builder.like(root.get("id"), pattern),
                        builder.like(root.get("module"), pattern), builder.like(root.get("description"), pattern),
                        builder.like(root.get("tags"), pattern)));
            }
            switch (selectedView) {
                case "created-by-me":
                    predicates.add(builder.equal(root.get("creatorId"), currentUserId));
                    break;
                case "assigned-to-me":
                    predicates.add(builder.equal(root.get("ownerId"), currentUserId));
                    predicates.add(root.get("status").in(FINAL_STATUSES).not());
                    break;
                case "pending-for-me":
                    predicates.add(builder.equal(root.get("ownerId"), currentUserId));
                    predicates.add(root.get("status").in(FINAL_STATUSES).not());
                    break;
                case "watched-by-me":
                    javax.persistence.criteria.Subquery<Long> watcherQuery = query.subquery(Long.class);
                    javax.persistence.criteria.Root<WorkItemWatcher> watcher = watcherQuery.from(WorkItemWatcher.class);
                    watcherQuery.select(watcher.get("id")).where(
                            builder.equal(watcher.get("workItemId"), root.get("id")),
                            builder.equal(watcher.get("userId"), currentUserId));
                    predicates.add(builder.exists(watcherQuery));
                    break;
                case "unclosed":
                    predicates.add(root.get("status").in(FINAL_STATUSES).not());
                    break;
                default:
                    break;
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
        Page<WorkItem> page = workItemRepo.findAll(specification, pageable);
        return page.map(this::toDto);
    }

    public List<WorkItemDto> board(Long projectId) {
        return workItemRepo.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toDto).collect(Collectors.toList());
    }

    public WorkItemDto detail(String id) {
        WorkItem w = workItemRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return toDto(w);
    }

    public boolean isWatched(String workItemId, Long userId) {
        return watcherRepo.existsByWorkItemIdAndUserId(workItemId, userId);
    }

    @Transactional
    public void watch(String workItemId, Long userId) {
        if (!watcherRepo.existsByWorkItemIdAndUserId(workItemId, userId)) {
            watcherRepo.save(WorkItemWatcher.builder()
                    .workItemId(workItemId)
                    .userId(userId)
                    .build());
        }
    }

    @Transactional
    public void unwatch(String workItemId, Long userId) {
        watcherRepo.deleteByWorkItemIdAndUserId(workItemId, userId);
    }

    public List<Map<String, Object>> watchers(String workItemId) {
        return watcherRepo.findByWorkItemId(workItemId).stream().map(watcher -> {
            User user = userRepo.findById(watcher.getUserId()).orElse(null);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("userId", watcher.getUserId());
            data.put("nickname", user != null ? user.getNickname() : null);
            data.put("username", user != null ? user.getUsername() : null);
            data.put("createdAt", watcher.getCreatedAt());
            return data;
        }).collect(Collectors.toList());
    }

    public List<WorkItemDto> watchedItems(Long projectId, Long userId) {
        return watcherRepo.findByUserId(userId).stream()
                .map(watcher -> workItemRepo.findById(watcher.getWorkItemId()).orElse(null))
                .filter(Objects::nonNull)
                .filter(item -> projectId.equals(item.getProjectId()))
                .map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public void replaceDeletedImageMarker(String workItemId, Long attachmentId) {
        WorkItem item = workItemRepo.findById(workItemId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        String marker = "[[image:" + attachmentId + "]]";
        if (item.getDescription() != null && item.getDescription().contains(marker)) {
            item.setDescription(item.getDescription().replace(marker, "[图片已删除]"));
            workItemRepo.save(item);
        }
    }

    public List<WorkItemDto> dueItems(Long projectId, Long userId) {
        List<WorkItem> all = workItemRepo.findByProjectIdAndOwnerIdAndStatusNotInOrderByDueDateAsc(
                projectId, userId, FINAL_STATUSES);
        Instant sevenDaysLater = Instant.now().plus(java.time.Duration.ofDays(7));
        return all.stream()
                .filter(w -> w.getDueDate() != null && !w.getDueDate().isAfter(sevenDaysLater))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ==================== 创建 ====================

    @Transactional
    public WorkItemDto create(Long projectId, CreateWorkItemRequest req, Long creatorId) {
        String description = richTextSanitizer.sanitize(req.getDescription());
        if (!richTextSanitizer.hasText(description))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务描述不能为空");
        taskTypeService.requireActive(req.getType());
        validateTaskAttributes(projectId, req.getOwnerId(), req.getWatcherIds(), req.getModuleId(), req.getSubmoduleId(),
                req.getSprintId(), req.getEstimatedHours(), req.getActualHours(), req.getRelatedWorkItemIds(), null);
        // 生成 ID
        WorkItemIdGenerator.GeneratedId generatedId = idGenerator.next(req.getType());
        int seq = generatedId.getSequence();
        String id = String.format("%s-%d", generatedId.getPrefix(), 1000 + seq);

        WorkItem item = WorkItem.builder()
                .id(id).seqNo(seq).projectId(projectId)
                .type(req.getType()).title(req.getTitle())
                .status("新建").priority(req.getPriority() != null ? req.getPriority() : "P2")
                .ownerId(req.getOwnerId()).sprintId(req.getSprintId())
                .module(req.getModuleId() != null ? moduleName(req.getModuleId()) : req.getModule()).severity(req.getSeverity() != null ? req.getSeverity() : "普通")
                .creatorId(creatorId).dueDate(req.getDueDate())
                .description(description).expected(req.getExpected()).actual(req.getActual())
                .moduleId(req.getModuleId()).submoduleId(req.getSubmoduleId()).estimatedHours(req.getEstimatedHours())
                .plannedStartDate(req.getPlannedStartDate()).actualCompletedAt(req.getActualCompletedAt()).actualHours(req.getActualHours())
                .tags(req.getTags() != null && !req.getTags().isEmpty() ? String.join(",", req.getTags()) : null)
                .build();
        item = workItemRepo.save(item);

        replaceWatchers(id, req.getWatcherIds());
        replaceRelations(id, projectId, req.getRelatedWorkItemIds());

        // 保存步骤
        if (req.getSteps() != null) {
            for (int i = 0; i < req.getSteps().size(); i++) {
                stepRepo.save(WorkItemStep.builder()
                        .workItemId(id).seq(i + 1).content(req.getSteps().get(i)).build());
            }
        }

        // 写活动
        activityRepo.save(Activity.builder()
                .projectId(projectId).workItemId(id)
                .actorId(creatorId).type("CREATE")
                .content("创建了工作项 " + id).build());
        notificationService.notifyWatchers(id, creatorId, "TASK_CREATED", "创建了任务：" + item.getTitle());

        return toDto(item);
    }

    // ==================== 编辑 ====================

    @Transactional
    public WorkItemDto update(String id, UpdateWorkItemRequest req, Long actorId) {
        WorkItem item = workItemRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (req.getTitle() != null) item.setTitle(req.getTitle());
        if (req.getPriority() != null) {
            if (!PRIORITIES.contains(req.getPriority())) throw new BusinessException(ErrorCode.BAD_REQUEST, "优先级无效");
            item.setPriority(req.getPriority());
        }
        if (req.getOwnerId() != null) item.setOwnerId(req.getOwnerId() <= 0 ? null : req.getOwnerId());
        if (req.getSprintId() != null) item.setSprintId(req.getSprintId());
        if (req.getModule() != null) item.setModule(req.getModule());
        if (req.getSeverity() != null) item.setSeverity(req.getSeverity());
        if (req.getDueDate() != null) item.setDueDate(req.getDueDate());
        if (req.getDescription() != null) {
            String description = richTextSanitizer.sanitize(req.getDescription());
            if (!richTextSanitizer.hasText(description)) throw new BusinessException(ErrorCode.BAD_REQUEST, "任务描述不能为空");
            item.setDescription(description);
        }
        if (req.getExpected() != null) item.setExpected(req.getExpected());
        if (req.getActual() != null) item.setActual(req.getActual());
        if (req.getTags() != null) item.setTags(String.join(",", req.getTags()));
        if (req.getModuleId() != null) item.setModuleId(req.getModuleId());
        if (req.getSubmoduleId() != null) item.setSubmoduleId(req.getSubmoduleId());
        if (req.getEstimatedHours() != null) item.setEstimatedHours(req.getEstimatedHours());
        if (req.getPlannedStartDate() != null) item.setPlannedStartDate(req.getPlannedStartDate());
        if (req.getActualCompletedAt() != null) item.setActualCompletedAt(req.getActualCompletedAt());
        if (req.getActualHours() != null) item.setActualHours(req.getActualHours());
        validateTaskAttributes(item.getProjectId(), req.getOwnerId() != null ? item.getOwnerId() : null, req.getWatcherIds(),
                req.getModuleId() != null ? req.getModuleId() : item.getModuleId(),
                req.getSubmoduleId() != null ? req.getSubmoduleId() : item.getSubmoduleId(),
                req.getSprintId(), req.getEstimatedHours(), req.getActualHours(), req.getRelatedWorkItemIds(), id);

        item = workItemRepo.save(item);
        if (req.getWatcherIds() != null) replaceWatchers(id, req.getWatcherIds());
        if (req.getRelatedWorkItemIds() != null) replaceRelations(id, item.getProjectId(), req.getRelatedWorkItemIds());

        // 更新步骤
        if (req.getSteps() != null) {
            stepRepo.deleteByWorkItemId(id);
            for (int i = 0; i < req.getSteps().size(); i++) {
                stepRepo.save(WorkItemStep.builder()
                        .workItemId(id).seq(i + 1).content(req.getSteps().get(i)).build());
            }
        }

        activityRepo.save(Activity.builder()
                .projectId(item.getProjectId()).workItemId(id)
                .actorId(actorId).type("UPDATE").content("更新了工作项").build());

        return toDto(item);
    }

    // ==================== 状态流转 ====================

    @Transactional
    public WorkItemDto transitionStatus(String id, String newStatus, Long actorId) {
        return transitionStatus(id, newStatus, actorId, null);
    }

    @Transactional
    public WorkItemDto transitionStatus(String id, String newStatus, Long actorId, String reason) {
        return transitionStatus(id, newStatus, actorId, reason, null, null);
    }

    @Transactional
    public WorkItemDto transitionStatus(String id, String newStatus, Long actorId, String reason, Instant requestedDueDate, Boolean delayApproved) {
        return transitionStatus(id, newStatus, actorId, reason, requestedDueDate, delayApproved, null, null);
    }

    @Transactional
    public WorkItemDto transitionStatus(String id, String newStatus, Long actorId, String reason, Instant requestedDueDate, Boolean delayApproved,
                                        Instant actualCompletedAt, BigDecimal actualHours) {
        WorkItem item = workItemRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (!isValidTransition(item.getStatus(), newStatus)) {
            throw new BusinessException(ErrorCode.STATUS_CONFLICT,
                    String.format("不允许从「%s」转移到「%s」", item.getStatus(), newStatus));
        }
        ensureTransitionActor(item, actorId, newStatus);
        boolean delayRejected = "延期处理".equals(item.getStatus()) && "进行中".equals(newStatus) && Boolean.FALSE.equals(delayApproved);
        boolean needsReason = "延期处理".equals(newStatus) || "已拒绝".equals(newStatus) || delayRejected
                || "验收不通过".equals(newStatus) || (("验收不通过".equals(item.getStatus()) || "已拒绝".equals(item.getStatus())) && "进行中".equals(newStatus));
        if (needsReason && !StringUtils.hasText(reason)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该操作必须填写说明");
        }
        if ("延期处理".equals(newStatus) && requestedDueDate == null)
            throw new BusinessException(ErrorCode.BAD_REQUEST, "申请延期必须填写新的截止日期");
        if ("已完成".equals(newStatus)) {
            if (actualCompletedAt == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "提交完成必须填写实际完成时间");
            if (actualHours == null) throw new BusinessException(ErrorCode.BAD_REQUEST, "提交完成必须填写实际花费工时");
            if (actualHours.compareTo(BigDecimal.ZERO) < 0 || actualHours.scale() > 2)
                throw new BusinessException(ErrorCode.BAD_REQUEST, "实际花费工时必须是非负数，最多保留两位小数");
        }

        String oldStatus = item.getStatus();
        item.setStatus(newStatus);
        if ("已完成".equals(newStatus)) {
            item.setActualCompletedAt(actualCompletedAt);
            item.setActualHours(actualHours);
        }
        if ("延期处理".equals(newStatus)) item.setDelayRequestedDueDate(requestedDueDate);
        if ("延期处理".equals(oldStatus) && "进行中".equals(newStatus)) {
            if (Boolean.FALSE.equals(delayApproved)) item.setDelayRequestedDueDate(null);
            else { item.setDueDate(item.getDelayRequestedDueDate()); item.setDelayRequestedDueDate(null); }
        }
        if (needsReason) item.setClosureReason(reason.trim());
        item = workItemRepo.save(item);

        activityRepo.save(Activity.builder()
                .projectId(item.getProjectId()).workItemId(id)
                .actorId(actorId).type("STATUS")
                .content(String.format("状态从「%s」变更为「%s」%s", oldStatus, newStatus,
                        needsReason ? "，说明：「" + reason.trim() + "」" : "")).build());
        notificationService.notifyWatchers(id, actorId, "STATUS_CHANGED", String.format("状态从「%s」变更为「%s」",oldStatus,newStatus));

        return toDto(item);
    }

    @Transactional
    public WorkItemDto assignMe(String id, Long actorId) {
        WorkItem item = workItemRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (item.getOwnerId() != null) {
            throw new BusinessException(ErrorCode.STATUS_CONFLICT, "工作项已有负责人");
        }
        if (!isValidTransition(item.getStatus(), "进行中")) {
            throw new BusinessException(ErrorCode.STATUS_CONFLICT,
                    String.format("当前状态「%s」不能分配", item.getStatus()));
        }

        item.setOwnerId(actorId);
        item.setStatus("进行中");
        item = workItemRepo.save(item);

        activityRepo.save(Activity.builder()
                .projectId(item.getProjectId()).workItemId(id)
                .actorId(actorId).type("STATUS")
                .content("分配给自己并转入「进行中」").build());

        return toDto(item);
    }

    // ==================== 删除 ====================

    @Transactional
    public void delete(String id) {
        workItemRepo.deleteById(id);
    }

    // ==================== DTO ====================

    public WorkItemDto toDto(WorkItem w) {
        List<WorkItemStep> steps = stepRepo.findByWorkItemIdOrderBySeq(w.getId());
        return WorkItemDto.builder()
                .id(w.getId()).projectId(w.getProjectId())
                .type(w.getType()).title(w.getTitle()).status(w.getStatus())
                .ownerId(w.getOwnerId())
                .ownerName(w.getOwnerId() != null ? userRepo.findById(w.getOwnerId()).map(User::getNickname).orElse(null) : null)
                .priority(w.getPriority()).sprintId(w.getSprintId())
                .module(w.getModule()).severity(w.getSeverity())
                .moduleId(w.getModuleId()).submoduleId(w.getSubmoduleId()).submodule(moduleName(w.getSubmoduleId()))
                .estimatedHours(w.getEstimatedHours())
                .plannedStartDate(w.getPlannedStartDate()).actualCompletedAt(w.getActualCompletedAt()).actualHours(w.getActualHours())
                .creatorId(w.getCreatorId())
                .creatorName(w.getCreatorId() != null ? userRepo.findById(w.getCreatorId()).map(User::getNickname).orElse(null) : null)
                .dueDate(w.getDueDate()).description(w.getDescription())
                .expected(w.getExpected()).actual(w.getActual())
                .parentId(w.getParentId())
                .tags(w.getTags() != null ? Arrays.asList(w.getTags().split(",")) : Collections.emptyList())
                .watchers(watchers(w.getId())).relatedWorkItems(relatedItems(w.getId()))
                .steps(steps.stream().map(s -> new StepDto(s.getId(), s.getSeq(), s.getContent(), s.getDone()))
                        .collect(Collectors.toList()))
                .createdAt(w.getCreatedAt()).updatedAt(w.getUpdatedAt());
    }

    private void validateTaskAttributes(Long projectId, Long ownerId, List<Long> watcherIds, Long moduleId,
                                        Long submoduleId, Long sprintId, BigDecimal estimatedHours, BigDecimal actualHours,
                                        List<String> relatedIds, String currentId) {
        if (ownerId != null && !projectMemberRepo.existsByProjectIdAndUserId(projectId, ownerId))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "负责人必须是项目成员");
        if (watcherIds != null) for (Long watcherId : new LinkedHashSet<>(watcherIds)) {
            if (watcherId == null || !projectMemberRepo.existsByProjectIdAndUserId(projectId, watcherId))
                throw new BusinessException(ErrorCode.BAD_REQUEST, "关注人必须是项目成员");
        }
        if (estimatedHours != null && (estimatedHours.compareTo(BigDecimal.ZERO) < 0 || estimatedHours.scale() > 1
                || estimatedHours.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() > 0))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "预计工时必须是非负的 0.5 小时粒度");
        if (actualHours != null && (actualHours.compareTo(BigDecimal.ZERO) < 0 || actualHours.scale() > 2))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "实际花费工时必须是非负数，最多保留两位小数");
        ModuleEntity main = moduleId == null ? null : moduleRepo.findById(moduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (main != null && (!projectId.equals(main.getProjectId()) || main.getParentId() != null))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "主模块不属于当前项目");
        if (submoduleId != null) {
            ModuleEntity sub = moduleRepo.findById(submoduleId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
            if (main == null || !projectId.equals(sub.getProjectId()) || !moduleId.equals(sub.getParentId()))
                throw new BusinessException(ErrorCode.BAD_REQUEST, "子模块必须属于已选主模块");
        }
        if (sprintId != null) {
            Sprint sprint = sprintRepo.findById(sprintId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
            if (!projectId.equals(sprint.getProjectId())) throw new BusinessException(ErrorCode.BAD_REQUEST, "迭代不属于当前项目");
        }
        if (relatedIds != null) for (String relatedId : new LinkedHashSet<>(relatedIds)) {
            if (!StringUtils.hasText(relatedId) || relatedId.equals(currentId))
                throw new BusinessException(ErrorCode.BAD_REQUEST, "任务不能关联自身");
            WorkItem related = workItemRepo.findById(relatedId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
            if (!projectId.equals(related.getProjectId())) throw new BusinessException(ErrorCode.BAD_REQUEST, "关联任务必须属于当前项目");
        }
    }

    private void replaceWatchers(String workItemId, List<Long> watcherIds) {
        watcherRepo.deleteByWorkItemId(workItemId);
        if (watcherIds == null) return;
        for (Long userId : new LinkedHashSet<>(watcherIds)) watcherRepo.save(WorkItemWatcher.builder().workItemId(workItemId).userId(userId).build());
    }

    private void replaceRelations(String workItemId, Long projectId, List<String> relatedIds) {
        relationRepo.deleteBySourceWorkItemIdOrTargetWorkItemId(workItemId, workItemId);
        if (relatedIds == null) return;
        for (String relatedId : new LinkedHashSet<>(relatedIds)) {
            String[] pair = relationPair(workItemId, relatedId);
            if (!relationRepo.existsBySourceWorkItemIdAndTargetWorkItemId(pair[0], pair[1]))
                relationRepo.save(WorkItemRelation.builder().sourceWorkItemId(pair[0]).targetWorkItemId(pair[1]).build());
        }
    }

    private String[] relationPair(String left, String right) {
        if (left.equals(right)) throw new BusinessException(ErrorCode.BAD_REQUEST, "任务不能关联自身");
        return left.compareTo(right) < 0 ? new String[] { left, right } : new String[] { right, left };
    }

    private void ensureTransitionActor(WorkItem item, Long actorId, String newStatus) {
        boolean isOwner = actorId.equals(item.getOwnerId());
        boolean isCreator = actorId.equals(item.getCreatorId());
        boolean isProjectAdmin = projectMemberRepo.findByProjectIdAndUserId(item.getProjectId(), actorId)
                .map(member -> "PROJECT_ADMIN".equals(member.getRole())).orElse(false);
        if (("验收不通过".equals(item.getStatus()) || "已拒绝".equals(item.getStatus())) && "进行中".equals(newStatus)) {
            if (!isOwner) throw new BusinessException(ErrorCode.FORBIDDEN, "仅任务负责人可以重新开始");
            return;
        }
        if (!isOwner && !isCreator && !isProjectAdmin)
            throw new BusinessException(ErrorCode.FORBIDDEN, "仅负责人、创建人或项目管理员可以执行该流程操作");
    }

    private String moduleName(Long moduleId) {
        return moduleId == null ? null : moduleRepo.findById(moduleId).map(ModuleEntity::getName).orElse(null);
    }

    private List<WorkItemRelationDto> relatedItems(String workItemId) {
        return relationRepo.findBySourceWorkItemIdOrTargetWorkItemId(workItemId, workItemId).stream().map(relation -> {
            String relatedId = workItemId.equals(relation.getSourceWorkItemId()) ? relation.getTargetWorkItemId() : relation.getSourceWorkItemId();
            return workItemRepo.findById(relatedId).map(item -> new WorkItemRelationDto(item.getId(), item.getTitle(), item.getType(), item.getStatus())).orElse(null);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
