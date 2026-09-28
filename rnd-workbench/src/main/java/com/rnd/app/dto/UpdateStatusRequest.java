package com.rnd.app.dto;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.Instant;

public class UpdateStatusRequest {
    @NotBlank
    private String status;
    private String reason;
    private Instant dueDate;
    private Boolean delayApproved;
    private Instant actualCompletedAt;
    private BigDecimal actualHours;

    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public Instant getDueDate() { return dueDate; }
    public void setDueDate(Instant v) { this.dueDate = v; }
    public Boolean getDelayApproved() { return delayApproved; }
    public void setDelayApproved(Boolean v) { this.delayApproved = v; }
    public Instant getActualCompletedAt() { return actualCompletedAt; }
    public void setActualCompletedAt(Instant v) { this.actualCompletedAt = v; }
    public BigDecimal getActualHours() { return actualHours; }
    public void setActualHours(BigDecimal v) { this.actualHours = v; }
}
