package com.rnd.app.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;

@Getter @Setter
public class SavedFilterRequest {
    @NotBlank @Size(max = 64)
    private String name;
    private Map<String, Object> conditions = new LinkedHashMap<>();
    private String sort = "createdAt,desc";
    private Boolean defaultFilter = false;
}
