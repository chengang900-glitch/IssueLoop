package com.rnd.app.service;

import com.rnd.app.dto.ProjectTypeDto;
import com.rnd.app.entity.ProjectType;
import com.rnd.app.repository.ProjectRepository;
import com.rnd.app.repository.ProjectTypeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectTypeServiceTest {
    @Mock ProjectTypeRepository repository;
    @Mock ProjectRepository projectRepository;
    @InjectMocks ProjectTypeService service;

    @Test
    void createsEnabledProjectType() {
        ProjectTypeDto request = new ProjectTypeDto(); request.setName("咨询");
        when(repository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(Collections.emptyList());
        when(repository.save(any())).thenAnswer(invocation -> {
            ProjectType type = invocation.getArgument(0); type.setId(5L); return type;
        });

        ProjectTypeDto created = service.create(request);

        assertEquals("咨询", created.getName());
        assertTrue(created.getEnabled());
    }

    @Test
    void renamesHistoricalProjectsWithType() {
        ProjectType type = ProjectType.builder().id(1L).name("实施").enabled(true).sortOrder(10).build();
        ProjectTypeDto request = new ProjectTypeDto(); request.setName("项目实施");
        when(repository.findById(1L)).thenReturn(Optional.of(type));
        when(repository.findByName("项目实施")).thenReturn(Optional.empty());
        when(repository.save(type)).thenReturn(type);

        service.update(1L, request);

        verify(projectRepository).updateProjectTypeName("实施", "项目实施");
        assertEquals("项目实施", type.getName());
    }
}
