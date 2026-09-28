package com.rnd.app.dto;

public class ModuleDto {
    private Long id;
    private Long projectId;
    private String name;
    private Long parentId;

    public ModuleDto() {}
    public ModuleDto(Long id, Long projectId, String name) { this.id = id; this.projectId = projectId; this.name = name; }

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long v) { this.projectId = v; }
    public String getName() { return name; }
    public void setName(String v) { this.name = v; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long v) { this.parentId = v; }
}
