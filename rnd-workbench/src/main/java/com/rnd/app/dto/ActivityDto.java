package com.rnd.app.dto;

import java.time.Instant;

public class ActivityDto {
    private Long id;
    private String workItemId;
    private Long actorId;
    private String actorName;
    private String type;
    private String content;
    private Instant createdAt;

    public ActivityDto() {}
    public ActivityDto(Long id, String workItemId, Long actorId, String actorName, String type, String content, Instant createdAt) {
        this.id = id; this.workItemId = workItemId; this.actorId = actorId; this.actorName = actorName;
        this.type = type; this.content = content; this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getWorkItemId() { return workItemId; }
    public void setWorkItemId(String v) { this.workItemId = v; }
    public Long getActorId() { return actorId; }
    public void setActorId(Long v) { this.actorId = v; }
    public String getActorName() { return actorName; }
    public void setActorName(String v) { this.actorName = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}