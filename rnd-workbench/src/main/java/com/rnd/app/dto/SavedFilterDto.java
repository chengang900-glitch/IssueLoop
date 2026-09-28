package com.rnd.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter @AllArgsConstructor
public class SavedFilterDto {
    private Long id;
    private Long projectId;
    private String name;
    private Map<String, Object> conditions;
    private String sort;
    private boolean defaultFilter;
    private Instant createdAt;
    private Instant updatedAt;
}
