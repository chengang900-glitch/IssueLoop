package com.rnd.app.dto;

public class ProjectSummaryDto {
    private long todoCount;
    private long inboxCount;
    private long inProgressCount;
    private long toBeVerifiedCount;
    private long todayDue;
    private long p0Urgent;

    public ProjectSummaryDto() {}
    public ProjectSummaryDto(long todo, long inbox, long progress, long verify, long due, long p0) {
        this.todoCount = todo; this.inboxCount = inbox; this.inProgressCount = progress;
        this.toBeVerifiedCount = verify; this.todayDue = due; this.p0Urgent = p0;
    }

    public long getTodoCount() { return todoCount; }
    public void setTodoCount(long v) { this.todoCount = v; }
    public long getInboxCount() { return inboxCount; }
    public void setInboxCount(long v) { this.inboxCount = v; }
    public long getInProgressCount() { return inProgressCount; }
    public void setInProgressCount(long v) { this.inProgressCount = v; }
    public long getToBeVerifiedCount() { return toBeVerifiedCount; }
    public void setToBeVerifiedCount(long v) { this.toBeVerifiedCount = v; }
    public long getTodayDue() { return todayDue; }
    public void setTodayDue(long v) { this.todayDue = v; }
    public long getP0Urgent() { return p0Urgent; }
    public void setP0Urgent(long v) { this.p0Urgent = v; }
}