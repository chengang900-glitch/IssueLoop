package com.rnd.app.service;

import com.rnd.app.dto.AnalyticsQuery;
import com.rnd.app.entity.*;
import com.rnd.app.repository.*;
import com.rnd.app.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {
    private final WorkItemRepository items;
    private final ProjectService projects;
    private final ProjectMemberRepository members;
    private final UserRepository users;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Set<String> OPEN = Set.of("新建", "进行中", "延期处理", "验收不通过");
    private static final Set<String> COMPLETE = Set.of("已完成", "已验收");
    private static final Set<String> LANES = Set.of("all", "pending", "inbox", "progress", "overdue", "upcoming", "review", "accepted", "open", "returned", "approval", "completed", "hours", "estimated", "mine", "submitted");

    private static final class Dataset {
        List<WorkItem> rows;
        List<Project> visible;
        Map<Long, String> names;
        Set<Long> adminProjects;
        Set<Long> writerProjects;
        Long userId;
        Instant now = Instant.now();
    }

    private void validate(AnalyticsQuery q) {
        if (!Set.of("personal", "project", "report").contains(q.getScope())
                || !Set.of("created", "completed").contains(q.getDateBasis())
                || !Set.of("project", "person", "type", "time").contains(q.getGroupBy())
                || !Set.of("week", "month").contains(q.getPeriod()) || !LANES.contains(q.getLane())
                || (q.getDrillDimension() != null && !Set.of("project", "person", "type", "time", "status", "createdMonth", "completedMonth").contains(q.getDrillDimension()))
                || (q.getFrom() != null && q.getTo() != null && q.getFrom().isAfter(q.getTo()))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "统计筛选条件无效");
        }
        if ("project".equals(q.getScope()) && q.getProjectId() == null)
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择单个项目");
        q.setPage(Math.max(1, q.getPage()));
        q.setSize(Math.max(1, Math.min(100, q.getSize())));
    }

    private Dataset dataset(AnalyticsQuery q, Long userId) {
        validate(q);
        Dataset d = new Dataset();
        d.userId = userId;
        d.visible = projects.listVisibleProjects(userId);
        Set<Long> ids = d.visible.stream().map(Project::getId).collect(Collectors.toSet());
        if (q.getProjectId() != null) {
            if (!ids.contains(q.getProjectId())) throw new BusinessException(ErrorCode.FORBIDDEN, "无权访问该项目或项目已归档");
            ids = Set.of(q.getProjectId());
        }
        List<ProjectMember> memberships = members.findByUserId(userId);
        d.adminProjects = memberships.stream().filter(m -> "PROJECT_ADMIN".equals(m.getRole())).map(ProjectMember::getProjectId).collect(Collectors.toSet());
        d.writerProjects = memberships.stream().filter(m -> !"GUEST".equals(m.getRole())).map(ProjectMember::getProjectId).collect(Collectors.toSet());
        final Set<Long> selectedIds = ids;
        Specification<WorkItem> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            p.add(root.get("projectId").in(selectedIds));
            if ("personal".equals(q.getScope()))
                p.add(cb.or(cb.equal(root.get("ownerId"), userId), WorkItemScope.pending(root, query, cb, userId)));
            if (q.getOwnerId() != null) p.add(q.getOwnerId() == 0 ? cb.isNull(root.get("ownerId")) : cb.equal(root.get("ownerId"), q.getOwnerId()));
            if (has(q.getType())) p.add(cb.equal(root.get("type"), q.getType()));
            if (has(q.getStatus())) p.add(cb.equal(root.get("status"), q.getStatus()));
            if (has(q.getKeyword())) {
                String word = "%" + q.getKeyword().trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("title")), word, '\\'), cb.like(cb.lower(root.get("id")), word, '\\')));
            }
            if ("report".equals(q.getScope())) {
                String field = "created".equals(q.getDateBasis()) ? "createdAt" : "actualCompletedAt";
                if ("completed".equals(q.getDateBasis())) {
                    p.add(root.get("status").in(COMPLETE));
                    p.add(cb.isNotNull(root.get(field)));
                }
                if (q.getFrom() != null) p.add(cb.greaterThanOrEqualTo(root.get(field), q.getFrom().atStartOfDay(ZONE).toInstant()));
                if (q.getTo() != null) p.add(cb.lessThan(root.get(field), q.getTo().plusDays(1).atStartOfDay(ZONE).toInstant()));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        d.rows = ids.isEmpty() ? new ArrayList<>() : items.findAll(spec);
        Set<Long> userIds = d.rows.stream().map(WorkItem::getOwnerId).filter(Objects::nonNull).collect(Collectors.toSet());
        for (Long id : selectedIds) members.findByProjectId(id).forEach(m -> userIds.add(m.getUserId()));
        d.names = users.findAllById(userIds).stream().collect(Collectors.toMap(User::getId, u -> has(u.getNickname()) ? u.getNickname() : u.getUsername()));
        return d;
    }

    public Map<String, Object> analyze(AnalyticsQuery q, Long userId) {
        Dataset d = dataset(q, userId);
        List<WorkItem> own = d.rows.stream().filter(w -> Objects.equals(w.getOwnerId(), userId)).collect(Collectors.toList());
        List<WorkItem> base = "personal".equals(q.getScope()) ? own : d.rows;
        List<WorkItem> details = details(d, q);
        int page = Math.min(q.getPage(), Math.max(1, (details.size() + q.getSize() - 1) / q.getSize()));
        int start = (page - 1) * q.getSize();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", metrics(base, d.now));
        result.put("personal", metrics(own, d.now));
        result.put("pendingCount", d.rows.stream().filter(w -> pending(w, d)).count());
        result.put("groups", groups(base, q.getGroupBy(), q, d));
        result.put("projects", groups(base, "project", q, d));
        result.put("people", groups(base, "person", q, d));
        result.put("types", groups(base, "type", q, d));
        result.put("statuses", groups(base, "status", q, d));
        result.put("trend", trend(base, d.now));
        result.put("list", details.subList(start, Math.min(start + q.getSize(), details.size())).stream().map(w -> row(w, d)).collect(Collectors.toList()));
        result.put("total", details.size()); result.put("page", page); result.put("size", q.getSize());
        result.put("options", d.names.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(e -> Map.of("id", e.getKey(), "name", e.getValue())).collect(Collectors.toList()));
        return result;
    }

    private List<WorkItem> details(Dataset d, AnalyticsQuery q) {
        return d.rows.stream().filter(w -> {
            if ("personal".equals(q.getScope()) && !Set.of("pending", "review", "approval").contains(q.getLane()) && !Objects.equals(w.getOwnerId(), d.userId)) return false;
            if (!lane(w, q.getLane(), d)) return false;
            return q.getDrillDimension() == null || Objects.equals(key(w, q.getDrillDimension(), q), q.getDrillKey());
        }).sorted(Comparator.comparing((WorkItem w) -> !overdue(w, d.now))
                .thenComparing(WorkItem::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(WorkItem::getPriority).thenComparing(WorkItem::getId)).collect(Collectors.toList());
    }

    private boolean pending(WorkItem w, Dataset d) {
        if (!d.writerProjects.contains(w.getProjectId())) return false;
        boolean owner = Objects.equals(w.getOwnerId(), d.userId);
        if (Set.of("新建", "进行中", "验收不通过").contains(w.getStatus())) return owner;
        return Set.of("已完成", "延期处理").contains(w.getStatus()) && (owner || Objects.equals(w.getCreatorId(), d.userId) || d.adminProjects.contains(w.getProjectId()));
    }

    private boolean lane(WorkItem w, String lane, Dataset d) {
        switch (lane) {
            case "pending": return pending(w, d);
            case "inbox": return "新建".equals(w.getStatus());
            case "progress": return "进行中".equals(w.getStatus());
            case "overdue": return overdue(w, d.now);
            case "upcoming": return upcoming(w, d.now);
            case "review": return "已完成".equals(w.getStatus());
            case "submitted": return "已完成".equals(w.getStatus()) && Objects.equals(w.getOwnerId(), d.userId);
            case "accepted": return "已验收".equals(w.getStatus());
            case "returned": return "验收不通过".equals(w.getStatus());
            case "approval": return "延期处理".equals(w.getStatus()) && pending(w, d);
            case "completed": return COMPLETE.contains(w.getStatus()) && w.getActualCompletedAt() != null;
            case "open": return OPEN.contains(w.getStatus());
            case "hours": return w.getActualHours() != null;
            case "estimated": return w.getEstimatedHours() != null;
            default: return true;
        }
    }

    private Map<String, Object> metrics(List<WorkItem> rows, Instant now) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", rows.size());
        m.put("open", rows.stream().filter(w -> OPEN.contains(w.getStatus())).count());
        m.put("inbox", count(rows, "新建")); m.put("progress", count(rows, "进行中"));
        m.put("review", count(rows, "已完成")); m.put("accepted", count(rows, "已验收"));
        m.put("rejected", count(rows, "已拒绝"));
        m.put("overdue", rows.stream().filter(w -> overdue(w, now)).count());
        m.put("upcoming", rows.stream().filter(w -> upcoming(w, now)).count());
        m.put("estimated", sum(rows, WorkItem::getEstimatedHours));
        m.put("hours", sum(rows, WorkItem::getActualHours));
        m.put("missingHours", rows.stream().filter(w -> w.getActualHours() == null).count());
        m.put("missingEstimated", rows.stream().filter(w -> w.getEstimatedHours() == null).count());
        m.put("completedHours", sum(rows.stream().filter(w -> COMPLETE.contains(w.getStatus()) && w.getActualCompletedAt() != null).collect(Collectors.toList()), WorkItem::getActualHours));
        m.put("nearestDue", rows.stream().filter(w -> OPEN.contains(w.getStatus())).map(WorkItem::getDueDate).filter(Objects::nonNull).min(Comparator.naturalOrder()).map(Instant::toString).orElse(""));
        return m;
    }

    private List<Map<String, Object>> groups(List<WorkItem> rows, String dimension, AnalyticsQuery q, Dataset d) {
        Map<String, List<WorkItem>> grouped = rows.stream().collect(Collectors.groupingBy(w -> key(w, dimension, q), TreeMap::new, Collectors.toList()));
        // Project members with zero tasks remain visible in the people table.
        if ("person".equals(dimension) && "project".equals(q.getScope())) d.names.keySet().forEach(id -> grouped.putIfAbsent(id.toString(), List.of()));
        return grouped.entrySet().stream().map(e -> {
            Map<String, Object> group = new LinkedHashMap<>(metrics(e.getValue(), d.now));
            group.put("key", e.getKey()); group.put("label", label(e.getKey(), dimension, d));
            return group;
        }).collect(Collectors.toList());
    }

    private String key(WorkItem w, String dimension, AnalyticsQuery q) {
        switch (dimension) {
            case "project": return w.getProjectId().toString();
            case "person": return w.getOwnerId() == null ? "0" : w.getOwnerId().toString();
            case "type": return w.getType();
            case "status": return w.getStatus();
            case "createdMonth": return month(w.getCreatedAt());
            case "completedMonth": return COMPLETE.contains(w.getStatus()) ? month(w.getActualCompletedAt()) : "";
            case "time":
                Instant date = "created".equals(q.getDateBasis()) ? w.getCreatedAt() : w.getActualCompletedAt();
                if (date == null) return "未登记日期";
                LocalDate day = date.atZone(ZONE).toLocalDate();
                return "week".equals(q.getPeriod()) ? day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString() : YearMonth.from(day).toString();
            default: return "";
        }
    }

    private String label(String key, String dimension, Dataset d) {
        if ("project".equals(dimension)) return d.visible.stream().filter(p -> p.getId().toString().equals(key)).map(Project::getName).findFirst().orElse("未知项目");
        if ("person".equals(dimension)) return "0".equals(key) ? "未分配" : d.names.getOrDefault(Long.valueOf(key), "已移除人员 #" + key);
        if ("status".equals(dimension) && "已完成".equals(key)) return "待验收";
        return key;
    }

    private List<Map<String, Object>> trend(List<WorkItem> rows, Instant now) {
        List<Map<String, Object>> result = new ArrayList<>();
        YearMonth current = YearMonth.from(now.atZone(ZONE));
        for (int i = 5; i >= 0; i--) {
            String key = current.minusMonths(i).toString();
            result.add(Map.of("key", key, "created", rows.stream().filter(w -> key.equals(month(w.getCreatedAt()))).count(),
                    "completed", rows.stream().filter(w -> COMPLETE.contains(w.getStatus()) && key.equals(month(w.getActualCompletedAt()))).count()));
        }
        return result;
    }

    private Map<String, Object> row(WorkItem w, Dataset d) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", w.getId()); r.put("title", w.getTitle()); r.put("projectId", w.getProjectId());
        r.put("projectName", label(w.getProjectId().toString(), "project", d));
        r.put("ownerId", w.getOwnerId()); r.put("ownerName", label(w.getOwnerId() == null ? "0" : w.getOwnerId().toString(), "person", d));
        r.put("type", w.getType()); r.put("status", w.getStatus()); r.put("priority", w.getPriority());
        r.put("dueDate", w.getDueDate()); r.put("createdAt", w.getCreatedAt()); r.put("actualCompletedAt", w.getActualCompletedAt());
        r.put("estimatedHours", w.getEstimatedHours()); r.put("actualHours", w.getActualHours());
        r.put("overdue", overdue(w, d.now)); r.put("actionable", pending(w, d));
        return r;
    }

    public byte[] export(AnalyticsQuery q, Long userId, String format) {
        if (!Set.of("summary", "details").contains(format)) throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的导出格式");
        Dataset d = dataset(q, userId);
        StringBuilder csv = new StringBuilder("\uFEFF");
        append(csv, List.of("统计口径", "当前负责人归属；完成任务工时按上海时区任务完成日期归集，非每日实际投入"));
        append(csv, Arrays.asList("日期口径", "completed".equals(q.getDateBasis()) ? "任务完成日期" : "任务创建日期", "开始", q.getFrom(), "结束", q.getTo(), "项目", q.getProjectId(), "人员", q.getOwnerId(), "类型", q.getType(), "状态", q.getStatus(), "搜索", q.getKeyword(), "分组", q.getGroupBy(), "明细条件", q.getLane(), q.getDrillDimension(), q.getDrillKey()));
        if ("details".equals(format)) {
            List<WorkItem> rows = details(d, q);
            limit(rows.size());
            append(csv, List.of("编号", "任务", "项目", "当前负责人", "类型", "状态", "优先级", "截止时间", "创建时间", "实际完成时间", "预计工时", "已登记实际工时"));
            for (WorkItem w : rows) append(csv, Arrays.asList(w.getId(), w.getTitle(), label(w.getProjectId().toString(), "project", d), label(w.getOwnerId() == null ? "0" : w.getOwnerId().toString(), "person", d), w.getType(), "已完成".equals(w.getStatus()) ? "待验收" : w.getStatus(), w.getPriority(), local(w.getDueDate()), local(w.getCreatedAt()), local(w.getActualCompletedAt()), w.getEstimatedHours(), w.getActualHours()));
        } else {
            List<WorkItem> base = "personal".equals(q.getScope()) ? d.rows.stream().filter(w -> Objects.equals(w.getOwnerId(), userId)).collect(Collectors.toList()) : d.rows;
            List<Map<String, Object>> rows = groups(base, q.getGroupBy(), q, d);
            limit(rows.size());
            append(csv, List.of("分组", "任务数", "未完成", "进行中", "待验收", "已验收", "逾期", "预计工时", "已登记实际工时", "完成任务工时", "未登记实际工时任务数"));
            for (Map<String, Object> r : rows) append(csv, Arrays.asList(r.get("label"), r.get("total"), r.get("open"), r.get("progress"), r.get("review"), r.get("accepted"), r.get("overdue"), r.get("estimated"), r.get("hours"), r.get("completedHours"), r.get("missingHours")));
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void limit(int rows) {
        if (rows > 10000) throw new BusinessException(ErrorCode.BAD_REQUEST, "导出结果超过 10000 条，请缩小筛选条件后重试");
    }
    private static void append(StringBuilder csv, List<?> cells) {
        csv.append(cells.stream().map(cell -> {
            String value = cell == null ? "" : cell.toString();
            if (value.stripLeading().matches("(?s)^[=+@\\-].*") || value.startsWith("\t") || value.startsWith("\r") || value.startsWith("\n")) value = "'" + value;
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }).collect(Collectors.joining(","))).append("\r\n");
    }
    private static boolean has(String s) { return s != null && !s.trim().isEmpty(); }
    private static long count(List<WorkItem> rows, String status) { return rows.stream().filter(w -> status.equals(w.getStatus())).count(); }
    private static BigDecimal sum(List<WorkItem> rows, Function<WorkItem, BigDecimal> field) { return rows.stream().map(field).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add); }
    private static boolean overdue(WorkItem w, Instant now) { return OPEN.contains(w.getStatus()) && w.getDueDate() != null && w.getDueDate().isBefore(now); }
    private static boolean upcoming(WorkItem w, Instant now) { return OPEN.contains(w.getStatus()) && w.getDueDate() != null && !w.getDueDate().isBefore(now) && w.getDueDate().isBefore(now.atZone(ZONE).toLocalDate().plusDays(7).atStartOfDay(ZONE).toInstant()); }
    private static String month(Instant date) { return date == null ? "" : YearMonth.from(date.atZone(ZONE)).toString(); }
    private static String local(Instant date) { return date == null ? "" : date.atZone(ZONE).toLocalDateTime().toString(); }
}
