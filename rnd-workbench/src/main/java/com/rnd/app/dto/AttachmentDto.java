package com.rnd.app.dto;

import java.time.Instant;

public class AttachmentDto {
    private Long id;
    private String fileName;
    private Long size;
    private String mimeType;
    private String storagePath;
    private Long uploaderId;
    private Instant createdAt;

    public AttachmentDto() {}
    public AttachmentDto(Long id, String fileName, Long size, String mimeType, String storagePath, Long uploaderId, Instant createdAt) {
        this.id = id; this.fileName = fileName; this.size = size; this.mimeType = mimeType;
        this.storagePath = storagePath; this.uploaderId = uploaderId; this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public String getFileName() { return fileName; }
    public void setFileName(String v) { this.fileName = v; }
    public Long getSize() { return size; }
    public void setSize(Long v) { this.size = v; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String v) { this.mimeType = v; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String v) { this.storagePath = v; }
    public Long getUploaderId() { return uploaderId; }
    public void setUploaderId(Long v) { this.uploaderId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
}