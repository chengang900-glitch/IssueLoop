package com.rnd.app.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;

@Getter @Setter
public class ProjectDto {
    private Long id;
    private String name;
    private String shortName;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private boolean archived;
    private String currentUserProjectRole;
    private String code;
    private String projectType;
    private String workZone;
    private String businessLine;
    private String customerName;
    private String deliveryLocation;
    private Long projectManagerId;
    private Long implementationLeadId;
    private Long developmentLeadId;
    private String customerContact;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private String phase;
    private String projectStatus;
    private String healthStatus;
    private String scope;
    private String deliverables;
    private String acceptanceCriteria;
    private String riskDescription;
    private String currentIssues;
    private String nextSteps;
    private String implementationMode;
    private LocalDate goLiveDate;
    private LocalDate supportEndDate;

    public ProjectDto() {}
    public ProjectDto(Long id, String name, String shortName, String description, Instant createdAt) {
        this.id = id; this.name = name; this.shortName = shortName;
        this.description = description; this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getShortName() { return shortName; }
    public void setShortName(String v) { this.shortName = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean v) { this.archived = v; }
    public String getCurrentUserProjectRole() { return currentUserProjectRole; }
    public void setCurrentUserProjectRole(String v) { this.currentUserProjectRole = v; }
}
