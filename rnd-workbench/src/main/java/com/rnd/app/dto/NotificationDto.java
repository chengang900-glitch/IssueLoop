package com.rnd.app.dto;

import java.time.Instant;

public class NotificationDto {
    private Long id;
    private String type;
    private String workItemId;
    private String content;
    private Boolean isRead;
    private Instant createdAt;

    public NotificationDto() {}
    public NotificationDto(Long id, String type, String workItemId, String content, Boolean isRead, Instant createdAt) {
        this.id = id; this.type = type; this.workItemId = workItemId; this.content = content; this.isRead = isRead; this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getType() { return type; }
    public void setType(String v) { this.type = v; }
    public String getWorkItemId() { return workItemId; }
    public void setWorkItemId(String v) { this.workItemId = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean v) { this.isRead = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}