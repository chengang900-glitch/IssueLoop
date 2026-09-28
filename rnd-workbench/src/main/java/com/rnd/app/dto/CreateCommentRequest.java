package com.rnd.app.dto;

import javax.validation.constraints.NotBlank;

public class CreateCommentRequest {
    @NotBlank
    private String content;
    private java.util.List<Long> mentions;

    public String getContent() { return content; }
    public void setContent(String v) { this.content = v; }
    public java.util.List<Long> getMentions() { return mentions; }
    public void setMentions(java.util.List<Long> v) { this.mentions = v; }
}