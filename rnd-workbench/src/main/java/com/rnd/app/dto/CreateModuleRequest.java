package com.rnd.app.dto;

public class CreateModuleRequest {
    private String name;
    private Long parentId;

    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long v) { this.parentId = v; }
}
