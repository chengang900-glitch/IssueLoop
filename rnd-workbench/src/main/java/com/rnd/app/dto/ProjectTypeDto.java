package com.rnd.app.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ProjectTypeDto {
    private Long id;
    private String name;
    private Boolean enabled;
    private Integer sortOrder;
}
