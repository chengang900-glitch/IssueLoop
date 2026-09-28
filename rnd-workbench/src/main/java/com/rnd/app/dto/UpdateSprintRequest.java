package com.rnd.app.dto;

import java.time.LocalDate;

public class UpdateSprintRequest {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate v) { this.startDate = v; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate v) { this.endDate = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
}