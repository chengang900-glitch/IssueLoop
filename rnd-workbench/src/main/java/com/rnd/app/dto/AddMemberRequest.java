package com.rnd.app.dto;

public class AddMemberRequest {
    private Long userId;
    private String role;

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }
}