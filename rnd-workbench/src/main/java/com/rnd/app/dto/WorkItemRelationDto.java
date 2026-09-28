package com.rnd.app.dto;

public class WorkItemRelationDto {
    private String id;
    private String title;
    private String type;
    private String status;

    public WorkItemRelationDto() {}
    public WorkItemRelationDto(String id, String title, String type, String status) {
        this.id = id; this.title = title; this.type = type; this.status = status;
    }
    public String getId() { return id; }
    public void setId(String v) { this.id = v; }
    public String getTitle() { return title; }
    public void setTitle(String v) { this.title = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
}
