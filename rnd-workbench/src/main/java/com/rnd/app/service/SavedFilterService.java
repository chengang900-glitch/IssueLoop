package com.rnd.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnd.app.dto.SavedFilterDto;
import com.rnd.app.dto.SavedFilterRequest;
import com.rnd.app.entity.SavedFilter;
import com.rnd.app.repository.SavedFilterRepository;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SavedFilterService {
    private static final Set<String> CONDITION_FIELDS = Set.of("view", "type", "status", "ownerId", "creatorId", "sprintId", "priority", "severity", "module", "tag", "dueFrom", "dueTo", "keyword");
    private static final Set<String> VIEWS = Set.of("created-by-me", "assigned-to-me", "pending-for-me", "watched-by-me", "unclosed");
    private static final Set<String> TYPES = Set.of("需求", "任务", "测试", "缺陷");
    private static final Set<String> STATUSES = Set.of("新建", "进行中", "延期处理", "已完成", "已验收", "验收不通过", "已拒绝");
    private static final Set<String> PRIORITIES = Set.of("P0", "P1", "P2", "P3");
    private static final Set<String> SEVERITIES = Set.of("致命", "严重", "普通", "轻微");
    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "updatedAt", "dueDate", "priority", "title");

    private final SavedFilterRepository repository;
    private final ObjectMapper mapper;

    public List<SavedFilterDto> list(Long projectId, Long userId) {
        return repository.findByProjectIdAndUserIdOrderByCreatedAtAsc(projectId, userId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public SavedFilterDto create(Long projectId, Long userId, SavedFilterRequest request) {
        validate(request);
        if (repository.existsByProjectIdAndUserIdAndName(projectId, userId, request.getName().trim()))
            throw new BusinessException(ErrorCode.DUPLICATE, "筛选器名称已存在");
        if (Boolean.TRUE.equals(request.getDefaultFilter())) repository.clearDefaults(projectId, userId);
        SavedFilter filter = SavedFilter.builder().projectId(projectId).userId(userId)
                .name(request.getName().trim()).conditionsJson(write(request.getConditions()))
                .sortJson(request.getSort()).defaultFilter(Boolean.TRUE.equals(request.getDefaultFilter())).build();
        return toDto(repository.save(filter));
    }

    @Transactional
    public SavedFilterDto update(Long id, Long userId, SavedFilterRequest request) {
        validate(request);
        SavedFilter filter = owned(id, userId);
        if (repository.existsByProjectIdAndUserIdAndNameAndIdNot(filter.getProjectId(), userId, request.getName().trim(), id))
            throw new BusinessException(ErrorCode.DUPLICATE, "筛选器名称已存在");
        if (Boolean.TRUE.equals(request.getDefaultFilter())) repository.clearDefaults(filter.getProjectId(), userId);
        filter.setName(request.getName().trim());
        filter.setConditionsJson(write(request.getConditions()));
        filter.setSortJson(request.getSort());
        filter.setDefaultFilter(Boolean.TRUE.equals(request.getDefaultFilter()));
        return toDto(repository.save(filter));
    }

    @Transactional
    public SavedFilterDto makeDefault(Long id, Long userId) {
        SavedFilter filter = owned(id, userId);
        repository.clearDefaults(filter.getProjectId(), userId);
        filter.setDefaultFilter(true);
        return toDto(repository.save(filter));
    }

    @Transactional
    public void delete(Long id, Long userId) {
        repository.delete(owned(id, userId));
    }

    @Transactional
    public SavedFilterDto copy(Long id, Long userId) {
        SavedFilter source = owned(id, userId);
        String base = source.getName() + "（副本）";
        String name = base;
        int suffix = 2;
        while (repository.existsByProjectIdAndUserIdAndName(source.getProjectId(), userId, name)) {
            name = base + suffix++;
        }
        SavedFilter copied = SavedFilter.builder().projectId(source.getProjectId()).userId(userId)
                .name(name).conditionsJson(source.getConditionsJson()).sortJson(source.getSortJson())
                .defaultFilter(false).build();
        return toDto(repository.save(copied));
    }

    public Long projectId(Long id, Long userId) { return owned(id, userId).getProjectId(); }

    private SavedFilter owned(Long id, Long userId) {
        return repository.findByIdAndUserId(id, userId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private void validate(SavedFilterRequest request) {
        Map<String, Object> conditions = request.getConditions() == null ? Collections.emptyMap() : request.getConditions();
        if (!CONDITION_FIELDS.containsAll(conditions.keySet())) throw new BusinessException(ErrorCode.BAD_REQUEST, "包含不支持的筛选字段");
        validateEnum(conditions, "view", VIEWS);
        validateEnum(conditions, "type", TYPES);
        validateEnum(conditions, "status", STATUSES);
        validateEnum(conditions, "priority", PRIORITIES);
        validateEnum(conditions, "severity", SEVERITIES);
        for (String field : List.of("ownerId", "creatorId", "sprintId")) {
            Object value = conditions.get(field);
            if (value != null && !(value instanceof Number)) throw new BusinessException(ErrorCode.BAD_REQUEST, field + " 必须为数字");
        }
        for (String field : List.of("dueFrom", "dueTo")) {
            Object value = conditions.get(field);
            if (value != null) try { Instant.parse(value.toString()); } catch (DateTimeParseException e) { throw new BusinessException(ErrorCode.BAD_REQUEST, field + " 日期格式错误"); }
        }
        String sort = request.getSort() == null ? "createdAt,desc" : request.getSort();
        String[] parts = sort.split(",", -1);
        if (parts.length != 2 || !SORT_FIELDS.contains(parts[0]) || !("asc".equals(parts[1]) || "desc".equals(parts[1])))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "排序配置不合法");
    }

    private void validateEnum(Map<String, Object> conditions, String field, Set<String> allowed) {
        Object value = conditions.get(field);
        if (value != null && !allowed.contains(value.toString())) throw new BusinessException(ErrorCode.BAD_REQUEST, field + " 值不合法");
    }

    private String write(Map<String, Object> conditions) {
        try { return mapper.writeValueAsString(conditions == null ? Collections.emptyMap() : conditions); }
        catch (JsonProcessingException e) { throw new BusinessException(ErrorCode.BAD_REQUEST, "筛选条件无法序列化"); }
    }

    private SavedFilterDto toDto(SavedFilter filter) {
        try {
            Map<String, Object> conditions = mapper.readValue(filter.getConditionsJson(), new TypeReference<Map<String, Object>>() {});
            return new SavedFilterDto(filter.getId(), filter.getProjectId(), filter.getName(), conditions,
                    filter.getSortJson(), Boolean.TRUE.equals(filter.getDefaultFilter()), filter.getCreatedAt(), filter.getUpdatedAt());
        } catch (JsonProcessingException e) { throw new BusinessException(ErrorCode.BAD_REQUEST, "筛选器数据已损坏"); }
    }
}
