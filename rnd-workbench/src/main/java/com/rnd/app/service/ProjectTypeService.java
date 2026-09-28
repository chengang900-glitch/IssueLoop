package com.rnd.app.service;

import com.rnd.app.dto.ProjectTypeDto;
import com.rnd.app.entity.ProjectType;
import com.rnd.app.repository.ProjectRepository;
import com.rnd.app.repository.ProjectTypeRepository;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectTypeService {
    private final ProjectTypeRepository repository;
    private final ProjectRepository projectRepository;

    public List<ProjectTypeDto> list(boolean includeDisabled) {
        List<ProjectType> types = includeDisabled ? repository.findAllByOrderBySortOrderAscIdAsc()
                : repository.findByEnabledTrueOrderBySortOrderAscIdAsc();
        return types.stream().map(this::toDto).collect(Collectors.toList());
    }

    public ProjectType requireActive(String value) {
        String name = normalizeName(value);
        ProjectType type = repository.findByName(name)
                .orElseThrow(() -> new BusinessException(ErrorCode.BAD_REQUEST, "项目类型不存在"));
        if (!Boolean.TRUE.equals(type.getEnabled()))
            throw new BusinessException(ErrorCode.BAD_REQUEST, "该项目类型已停用");
        return type;
    }

    @Transactional
    public ProjectTypeDto create(ProjectTypeDto request) {
        String name = normalizeName(request.getName());
        if (repository.existsByName(name)) throw new BusinessException(ErrorCode.DUPLICATE, "项目类型名称已存在");
        int sortOrder = repository.findAllByOrderBySortOrderAscIdAsc().stream()
                .mapToInt(ProjectType::getSortOrder).max().orElse(0) + 10;
        return toDto(repository.save(ProjectType.builder().name(name).enabled(true).sortOrder(sortOrder).build()));
    }

    @Transactional
    public ProjectTypeDto update(Long id, ProjectTypeDto request) {
        ProjectType type = repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        String name = normalizeName(request.getName());
        repository.findByName(name).filter(existing -> !existing.getId().equals(id)).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.DUPLICATE, "项目类型名称已存在");
        });
        if (!type.getName().equals(name)) {
            projectRepository.updateProjectTypeName(type.getName(), name);
            type.setName(name);
        }
        return toDto(repository.save(type));
    }

    @Transactional
    public ProjectTypeDto setEnabled(Long id, boolean enabled) {
        ProjectType type = repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        type.setEnabled(enabled);
        return toDto(repository.save(type));
    }

    private String normalizeName(String value) {
        String name = value == null ? "" : value.trim();
        if (!StringUtils.hasText(name) || name.length() > 16)
            throw new BusinessException(ErrorCode.BAD_REQUEST, "项目类型名称需为 1-16 个字符");
        return name;
    }

    private ProjectTypeDto toDto(ProjectType type) {
        ProjectTypeDto dto = new ProjectTypeDto();
        dto.setId(type.getId()); dto.setName(type.getName()); dto.setEnabled(type.getEnabled()); dto.setSortOrder(type.getSortOrder());
        return dto;
    }
}
