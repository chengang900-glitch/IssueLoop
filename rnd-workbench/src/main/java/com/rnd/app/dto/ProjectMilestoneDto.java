package com.rnd.app.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;

@Getter @Setter
public class ProjectMilestoneDto {
    private Long id;
    private Long projectId;
    private String name;
    private LocalDate plannedDate;
    private LocalDate completedDate;
    private String status;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
}
