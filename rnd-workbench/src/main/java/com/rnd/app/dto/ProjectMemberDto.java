package com.rnd.app.dto;

public class ProjectMemberDto {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private String role;

    public ProjectMemberDto() {}
    public ProjectMemberDto(Long userId, String username, String nickname, String role) {
        this.userId = userId; this.username = username; this.nickname = nickname; this.role = role;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getNickname() { return nickname; }
    public void setNickname(String v) { this.nickname = v; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String v) { this.avatar = v; }
    public String getRole() { return role; }
    public void setRole(String v) { this.role = v; }
}