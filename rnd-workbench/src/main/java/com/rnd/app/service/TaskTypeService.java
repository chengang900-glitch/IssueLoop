package com.rnd.app.service;

import com.rnd.app.dto.TaskTypeDto;
import com.rnd.app.entity.TaskType;
import com.rnd.app.repository.TaskTypeRepository;
import com.rnd.app.repository.WorkItemRepository;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskTypeService {
    private final TaskTypeRepository repository;
    private final WorkItemRepository workItemRepository;

    public List<TaskTypeDto> list(boolean includeDisabled) {
        List<TaskType> types = includeDisabled ? repository.findAllByOrderBySortOrderAscIdAsc()
                : repository.findByEnabledTrueOrderBySortOrderAscIdAsc();
        return types.stream().map(this::toDto).collect(Collectors.toList());
    }

    public TaskType requireActive(String name) {
        TaskType type = repository.findByName(name)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "任务类型不存在"));
        if (!Boolean.TRUE.equals(type.getEnabled()))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该任务类型已停用");
        return type;
    }

    @Transactional
    public TaskTypeDto create(TaskTypeDto request) {
        String name = normalizeName(request.getName());
        if (repository.existsByName(name)) throw new BusinessException(ErrorCode.DUPLICATE, "任务类型名称已存在");
        int sortOrder = repository.findAllByOrderBySortOrderAscIdAsc().stream()
                .mapToInt(TaskType::getSortOrder).max().orElse(0) + 10;
        TaskType type = repository.save(TaskType.builder().name(name).codePrefix(newPrefix())
                .enabled(true).sortOrder(sortOrder).nextValue(1).build());
        return toDto(type);
    }

    @Transactional
    public TaskTypeDto update(Long id, TaskTypeDto request) {
        TaskType type = repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        String name = normalizeName(request.getName());
        repository.findByName(name).filter(existing -> !existing.getId().equals(id)).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.DUPLICATE, "任务类型名称已存在");
        });
        if (!type.getName().equals(name)) {
            workItemRepository.updateTypeName(type.getName(), name);
            type.setName(name);
        }
        return toDto(repository.save(type));
    }

    @Transactional
    public TaskTypeDto setEnabled(Long id, boolean enabled) {
        TaskType type = repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        type.setEnabled(enabled);
        return toDto(repository.save(type));
    }

    private String normalizeName(String value) {
        String name = value == null ? "" : value.trim();
        if (!StringUtils.hasText(name) || name.length() > 16)
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务类型名称需为 1-16 个字符");
        return name;
    }

    private String newPrefix() {
        return "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    private TaskTypeDto toDto(TaskType type) {
        TaskTypeDto dto = new TaskTypeDto();
        dto.setId(type.getId()); dto.setName(type.getName()); dto.setEnabled(type.getEnabled()); dto.setSortOrder(type.getSortOrder());
        return dto;
    }
}
