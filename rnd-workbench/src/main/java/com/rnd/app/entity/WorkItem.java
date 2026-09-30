package com.rnd.app.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_items", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "type", "seq_no"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkItem {
    @Id @Column(length = 16)
    private String id;

    /**
     * 乐观锁版本号：整实体 save() 时作为并发写保护（冲突由 GlobalExceptionHandler 映射为 409）。
     */
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "seq_no", nullable = false)
    private Integer seqNo;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 8)
    private String type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 16)
    @Builder.Default
    private String status = "新建";

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(nullable = false, length = 4)
    @Builder.Default
    private String priority = "P2";

    @Column(name = "sprint_id")
    private Long sprintId;

    @Column(length = 64)
    private String module;

    @Column(name = "module_id")
    private Long moduleId;

    @Column(name = "submodule_id")
    private Long submoduleId;

    @Column(name = "estimated_hours", precision = 8, scale = 1)
    private BigDecimal estimatedHours;

    @Column(name = "planned_start_date")
    private LocalDate plannedStartDate;

    @Column(name = "actual_completed_at")
    private Instant actualCompletedAt;

    @Column(name = "actual_hours", precision = 8, scale = 2)
    private BigDecimal actualHours;

    @Column(nullable = false, length = 8)
    @Builder.Default
    private String severity = "普通";

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "delay_requested_due_date")
    private Instant delayRequestedDueDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String expected;

    @Column(columnDefinition = "TEXT")
    private String actual;

    @Column(name = "closure_reason", columnDefinition = "TEXT")
    private String closureReason;

    @Column(name = "parent_id", length = 16)
    private String parentId;

    @Column(columnDefinition = "TEXT") // JSON array stored as text
    private String tags;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
