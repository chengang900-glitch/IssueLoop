package com.rnd.app.dto;

import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

@Getter @Setter
public class CreateProjectRequest {
    @com.fasterxml.jackson.annotation.JsonIgnore
    private final java.util.Set<String> suppliedDates = new java.util.HashSet<>();
    public boolean hasDate(String field) { return suppliedDates.contains(field); }
    @NotBlank private String name;
    private String shortName;
    private String description;
    private java.util.List<Long> memberIds;
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

    @com.fasterxml.jackson.annotation.JsonSetter
    public void setPlannedStartDate(LocalDate value) { this.plannedStartDate = value; suppliedDates.add("plannedStartDate"); }

    @com.fasterxml.jackson.annotation.JsonSetter
    public void setPlannedEndDate(LocalDate value) { this.plannedEndDate = value; suppliedDates.add("plannedEndDate"); }

    @com.fasterxml.jackson.annotation.JsonSetter
    public void setActualStartDate(LocalDate value) { this.actualStartDate = value; suppliedDates.add("actualStartDate"); }

    @com.fasterxml.jackson.annotation.JsonSetter
    public void setActualEndDate(LocalDate value) { this.actualEndDate = value; suppliedDates.add("actualEndDate"); }

    @com.fasterxml.jackson.annotation.JsonSetter
    public void setGoLiveDate(LocalDate value) { this.goLiveDate = value; suppliedDates.add("goLiveDate"); }

    @com.fasterxml.jackson.annotation.JsonSetter
    public void setSupportEndDate(LocalDate value) { this.supportEndDate = value; suppliedDates.add("supportEndDate"); }

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public String getShortName() { return shortName; }
    public void setShortName(String v) { this.shortName = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public java.util.List<Long> getMemberIds() { return memberIds; }
    public void setMemberIds(java.util.List<Long> v) { this.memberIds = v; }
}
