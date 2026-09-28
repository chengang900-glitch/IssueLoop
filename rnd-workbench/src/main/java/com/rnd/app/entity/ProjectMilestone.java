package com.rnd.app.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "project_milestones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProjectMilestone {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "project_id", nullable = false) private Long projectId;
    @Column(nullable = false, length = 128) private String name;
    @Column(name = "planned_date", nullable = false) private LocalDate plannedDate;
    @Column(name = "completed_date") private LocalDate completedDate;
    @Column(nullable = false, length = 16) @Builder.Default private String status = "未开始";
    @Column(columnDefinition = "TEXT") private String description;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
