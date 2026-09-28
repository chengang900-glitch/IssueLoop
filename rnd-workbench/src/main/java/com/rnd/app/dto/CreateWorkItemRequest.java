package com.rnd.app.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import javax.validation.constraints.NotBlank;

public class CreateWorkItemRequest {
    @NotBlank
    private String type;
    @NotBlank
    private String title;
    private String priority;
    private Long ownerId;
    private Long sprintId;
    private String module;
    private String severity;
    private Instant dueDate;
    private String description;
    private String expected;
    private String actual;
    private List<String> steps;
    private List<String> tags;
    private Long moduleId;
    private Long submoduleId;
    private BigDecimal estimatedHours;
    private LocalDate plannedStartDate;
    private Instant actualCompletedAt;
    private BigDecimal actualHours;
    private List<Long> watcherIds;
    private List<String> relatedWorkItemIds;

    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getPriority() { return priority; }
    public void setPriority(String v) { this.priority = v; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long v) { this.ownerId = v; }
    public Long getSprintId() { return sprintId; }
    public void setSprintId(Long v) { this.sprintId = v; }
    public String getModule() { return module; }
    public void setModule(String v) { this.module = v; }
    public String getSeverity() { return severity; }
    public void setSeverity(String v) { this.severity = v; }
    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant v) { this.dueDate = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public String getExpected() { return expected; }
    public void setExpected(String v) { this.expected = v; }
    public String getActual() { return actual; }
    public void setActual(String v) { this.actual = v; }
    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> v) { this.steps = v; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> v) { this.tags = v; }
    public Long getModuleId() { return moduleId; }
    public void setModuleId(Long v) { this.moduleId = v; }
    public Long getSubmoduleId() { return submoduleId; }
    public void setSubmoduleId(Long v) { this.submoduleId = v; }
    public BigDecimal getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(BigDecimal v) { this.estimatedHours = v; }
    public LocalDate getPlannedStartDate() { return plannedStartDate; }
    public void setPlannedStartDate(LocalDate v) { this.plannedStartDate = v; }
    public Instant getActualCompletedAt() { return actualCompletedAt; }
    public void setActualCompletedAt(Instant v) { this.actualCompletedAt = v; }
    public BigDecimal getActualHours() { return actualHours; }
    public void setActualHours(BigDecimal v) { this.actualHours = v; }
    public List<Long> getWatcherIds() { return watcherIds; }
    public void setWatcherIds(List<Long> v) { this.watcherIds = v; }
    public List<String> getRelatedWorkItemIds() { return relatedWorkItemIds; }
    public void setRelatedWorkItemIds(List<String> v) { this.relatedWorkItemIds = v; }
}
