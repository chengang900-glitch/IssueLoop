package com.rnd.app.dto;

import java.time.Instant;
import java.time.LocalDate;

public class SprintDto {
    private Long id;
    private Long projectId;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    public SprintDto() {}
    public SprintDto(Long id, Long projectId, String name, LocalDate startDate, LocalDate endDate, String status) {
        this.id = id; this.projectId = projectId; this.name = name; this.startDate = startDate; this.endDate = endDate; this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long v) { this.projectId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate v) { this.startDate = v; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate v) { this.endDate = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
}