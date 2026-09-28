package com.rnd.app.dto;

public class StepDto {
    private Long id;
    private Integer seq;
    private String content;
    private Boolean done;

    public StepDto() {}
    public StepDto(Long id, Integer seq, String content, Boolean done) {
        this.id = id; this.seq = seq; this.content = content; this.done = done;
    }

    public Long getId() { return id; }
    public void setId(Long v) { this.id = v; }
    public Integer getSeq() { return seq; }
    public void setSeq(Integer v) { this.seq = v; }
    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public Boolean getDone() { return done; }
    public void setDone(Boolean v) { this.done = v; }
}