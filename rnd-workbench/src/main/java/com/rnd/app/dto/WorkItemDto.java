package com.rnd.app.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

public class WorkItemDto {
    private String id;
    private Long projectId;
    private String type;
    private String title;
    private String status;
    private Long ownerId;
    private String ownerName;
    private String priority;
    private Long sprintId;
    private String module;
    private Long moduleId;
    private Long submoduleId;
    private String submodule;
    private BigDecimal estimatedHours;
    private LocalDate plannedStartDate;
    private Instant actualCompletedAt;
    private BigDecimal actualHours;
    private String severity;
    private Long creatorId;
    private String creatorName;
    private Instant dueDate;
    private String description;
    private String expected;
    private String actual;
    private String parentId;
    private List<String> tags;
    private List<StepDto> steps;
    private boolean watched;
    private List<java.util.Map<String, Object>> watchers;
    private List<WorkItemRelationDto> relatedWorkItems;
    private Instant createdAt;
    private Instant updatedAt;

    public WorkItemDto() {}

    // builder-style
    public static WorkItemDto builder() { return new WorkItemDto(); }
    public WorkItemDto id(String v) { this.id = v; return this; }
    public WorkItemDto projectId(Long v) { this.projectId = v; return this; }
    public WorkItemDto type(String v) { this.type = v; return this; }
    public WorkItemDto title(String v) { this.title = v; return this; }
    public WorkItemDto status(String v) { this.status = v; return this; }
    public WorkItemDto ownerId(Long v) { this.ownerId = v; return this; }
    public WorkItemDto ownerName(String v) { this.ownerName = v; return this; }
    public WorkItemDto priority(String v) { this.priority = v; return this; }
    public WorkItemDto sprintId(Long v) { this.sprintId = v; return this; }
    public WorkItemDto module(String v) { this.module = v; return this; }
    public WorkItemDto moduleId(Long v) { this.moduleId = v; return this; }
    public WorkItemDto submoduleId(Long v) { this.submoduleId = v; return this; }
    public WorkItemDto submodule(String v) { this.submodule = v; return this; }
    public WorkItemDto estimatedHours(BigDecimal v) { this.estimatedHours = v; return this; }
    public WorkItemDto plannedStartDate(LocalDate v) { this.plannedStartDate = v; return this; }
    public WorkItemDto actualCompletedAt(Instant v) { this.actualCompletedAt = v; return this; }
    public WorkItemDto actualHours(BigDecimal v) { this.actualHours = v; return this; }
    public WorkItemDto severity(String v) { this.severity = v; return this; }
    public WorkItemDto creatorId(Long v) { this.creatorId = v; return this; }
    public WorkItemDto creatorName(String v) { this.creatorName = v; return this; }
    public WorkItemDto dueDate(Instant v) { this.dueDate = v; return this; }
    public WorkItemDto description(String v) { this.description = v; return this; }
    public WorkItemDto expected(String v) { this.expected = v; return this; }
    public WorkItemDto actual(String v) { this.actual = v; return this; }
    public WorkItemDto parentId(String v) { this.parentId = v; return this; }
    public WorkItemDto tags(List<String> v) { this.tags = v; return this; }
    public WorkItemDto steps(List<StepDto> v) { this.steps = v; return this; }
    public WorkItemDto watched(boolean v) { this.watched = v; return this; }
    public WorkItemDto watchers(List<java.util.Map<String, Object>> v) { this.watchers = v; return this; }
    public WorkItemDto relatedWorkItems(List<WorkItemRelationDto> v) { this.relatedWorkItems = v; return this; }
    public WorkItemDto createdAt(Instant v) { this.createdAt = v; return this; }
    public WorkItemDto updatedAt(Instant v) { this.updatedAt = v; return this; }

    // getters/setters
    public String getId() { return id; }
    public void setId(String v) { this.id = v; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long v) { this.projectId = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long v) { this.ownerId = v; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String v) { this.ownerName = v; }
    public String getPriority() { return priority; }
    public void setPriority(String v) { this.priority = v; }
    public Long getSprintId() { return sprintId; }
    public void setSprintId(Long v) { this.sprintId = v; }
    public String getModule() { return module; }
    public void setModule(String v) { this.module = v; }
    public Long getModuleId() { return moduleId; }
    public void setModuleId(Long v) { this.moduleId = v; }
    public Long getSubmoduleId() { return submoduleId; }
    public void setSubmoduleId(Long v) { this.submoduleId = v; }
    public String getSubmodule() { return submodule; }
    public void setSubmodule(String v) { this.submodule = v; }
    public BigDecimal getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(BigDecimal v) { this.estimatedHours = v; }
    public LocalDate getPlannedStartDate() { return plannedStartDate; }
    public void setPlannedStartDate(LocalDate v) { this.plannedStartDate = v; }
    public Instant getActualCompletedAt() { return actualCompletedAt; }
    public void setActualCompletedAt(Instant v) { this.actualCompletedAt = v; }
    public BigDecimal getActualHours() { return actualHours; }
    public void setActualHours(BigDecimal v) { this.actualHours = v; }
    public String getSeverity() { return severity; }
    public void setSeverity(String v) { this.severity = v; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long v) { this.creatorId = v; }
    public String getCreatorName() { return creatorName; }
    public void setCreatorName(String v) { this.creatorName = v; }
    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant v) { this.dueDate = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getExpected() { return expected; }
    public void setExpected(String v) { this.expected = v; }
    public String getActual() { return actual; }
    public void setActual(String v) { this.actual = v; }
    public String getParentId() { return parentId; }
    public void setParentId(String v) { this.parentId = v; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> v) { this.tags = v; }
    public List<StepDto> getSteps() { return steps; }
    public void setSteps(List<StepDto> v) { this.steps = v; }
    public boolean isWatched() { return watched; }
    public void setWatched(boolean v) { this.watched = v; }
    public List<java.util.Map<String, Object>> getWatchers() { return watchers; }
    public void setWatchers(List<java.util.Map<String, Object>> v) { this.watchers = v; }
    public List<WorkItemRelationDto> getRelatedWorkItems() { return relatedWorkItems; }
    public void setRelatedWorkItems(List<WorkItemRelationDto> v) { this.relatedWorkItems = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
