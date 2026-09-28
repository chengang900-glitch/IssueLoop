package com.rnd.app.dto;

import java.time.Instant;

public class CommentDto {
    private Long id;
    private Long authorId;
    private String authorName;
    private String content;
    private Instant createdAt;

    public CommentDto() {}
    public CommentDto(Long id, Long authorId, String authorName, String content, Instant createdAt) {
        this.id = id; this.authorId = authorId; this.authorName = authorName; this.content = content; this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long v) { this.authorId = v; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String v) { this.authorName = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}