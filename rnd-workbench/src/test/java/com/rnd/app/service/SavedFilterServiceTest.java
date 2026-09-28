package com.rnd.app.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnd.app.dto.SavedFilterRequest;
import com.rnd.app.entity.SavedFilter;
import com.rnd.app.repository.SavedFilterRepository;
import com.rnd.app.util.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedFilterServiceTest {
    @Mock SavedFilterRepository repository;
    SavedFilterService service;

    @BeforeEach
    void setUp() { service = new SavedFilterService(repository, new ObjectMapper()); }

    @Test
    void rejectsUnsupportedConditionsAndSorts() {
        SavedFilterRequest unsupported = request("测试", Map.of("sql", "delete"), "createdAt,desc", false);
        assertThrows(BusinessException.class, () -> service.create(1L, 9L, unsupported));

        SavedFilterRequest badView = request("测试", Map.of("view", "other-user"), "createdAt,desc", false);
        assertThrows(BusinessException.class, () -> service.create(1L, 9L, badView));

        SavedFilterRequest badSort = request("测试", Map.of(), "id,desc", false);
        assertThrows(BusinessException.class, () -> service.create(1L, 9L, badSort));
    }

    @Test
    void rejectsDuplicateName() {
        when(repository.existsByProjectIdAndUserIdAndName(1L, 9L, "我的缺陷")).thenReturn(true);
        assertThrows(BusinessException.class, () -> service.create(1L, 9L,
                request("我的缺陷", Map.of("type", "缺陷"), "createdAt,desc", false)));
    }

    @Test
    void settingDefaultClearsOtherDefaultsInSameUserProject() {
        when(repository.existsByProjectIdAndUserIdAndName(any(), any(), any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        service.create(1L, 9L, request("默认", Map.of("priority", "P0"), "updatedAt,desc", true));
        verify(repository).clearDefaults(1L, 9L);
    }

    @Test
    void anotherUsersFilterIsNotAccessible() {
        when(repository.findByIdAndUserId(7L, 9L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> service.delete(7L, 9L));
        verify(repository, never()).delete(any(SavedFilter.class));
    }

    private SavedFilterRequest request(String name, Map<String, Object> conditions, String sort, boolean defaultFilter) {
        SavedFilterRequest request = new SavedFilterRequest();
        request.setName(name);
        request.setConditions(new LinkedHashMap<>(conditions));
        request.setSort(sort);
        request.setDefaultFilter(defaultFilter);
        return request;
    }
}
