package com.rnd.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnd.app.dto.ViewPreferenceDto;
import com.rnd.app.entity.UserViewPreference;
import com.rnd.app.repository.UserViewPreferenceRepository;
import com.rnd.app.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @RequiredArgsConstructor
public class ViewPreferenceService {
    public static final List<String> DEFAULT_COLUMNS = List.of("project", "type", "status", "owner", "priority", "sprint", "dueDate");
    private static final Set<String> COLUMNS = Set.of("project", "type", "status", "owner", "priority", "sprint", "module", "severity", "creator", "plannedStartDate", "actualCompletedAt", "actualHours", "dueDate", "createdAt", "updatedAt");
    private static final Set<String> SORTS = Set.of("createdAt", "updatedAt", "dueDate", "priority", "title");
    private static final Set<String> GROUPS = Set.of("status", "owner", "type", "sprint");
    private final UserViewPreferenceRepository repository;
    private final ObjectMapper mapper;

    public ViewPreferenceDto get(Long projectId, Long userId) {
        return repository.findByProjectIdAndUserId(projectId, userId).map(this::toDto)
                .orElse(new ViewPreferenceDto(DEFAULT_COLUMNS, "createdAt,desc", null));
    }

    @Transactional
    public ViewPreferenceDto save(Long projectId, Long userId, ViewPreferenceDto dto) {
        validate(dto);
        UserViewPreference value = repository.findByProjectIdAndUserId(projectId, userId)
                .orElseGet(() -> UserViewPreference.builder().projectId(projectId).userId(userId).build());
        try { value.setColumnsJson(mapper.writeValueAsString(dto.getColumns())); }
        catch (JsonProcessingException e) { throw new BusinessException(ErrorCode.BAD_REQUEST, "列配置无法序列化"); }
        value.setSortJson(dto.getSort()); value.setGroupBy(dto.getGroupBy());
        return toDto(repository.save(value));
    }

    private void validate(ViewPreferenceDto dto) {
        List<String> columns = dto.getColumns();
        if (columns == null || columns.isEmpty() || !COLUMNS.containsAll(columns) || new HashSet<>(columns).size() != columns.size())
            throw new BusinessException(ErrorCode.BAD_REQUEST, "显示列配置不合法");
        String[] sort = dto.getSort() == null ? new String[0] : dto.getSort().split(",", -1);
        if (sort.length != 2 || !SORTS.contains(sort[0]) || !("asc".equals(sort[1]) || "desc".equals(sort[1])))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "排序配置不合法");
        if (dto.getGroupBy() != null && !dto.getGroupBy().isBlank() && !GROUPS.contains(dto.getGroupBy()))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "分组配置不合法");
    }

    private ViewPreferenceDto toDto(UserViewPreference value) {
        try { return new ViewPreferenceDto(mapper.readValue(value.getColumnsJson(), new TypeReference<List<String>>() {}), value.getSortJson(), value.getGroupBy()); }
        catch (JsonProcessingException e) { throw new BusinessException(ErrorCode.BAD_REQUEST, "视图偏好数据已损坏"); }
    }
}
