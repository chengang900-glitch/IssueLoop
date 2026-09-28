package com.rnd.app.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "projects")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Project {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(name = "short_name", nullable = false, length = 8)
    private String shortName;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(name = "project_type", nullable = false, length = 16)
    @Builder.Default private String projectType = "实施";

    @Column(name = "work_zone", nullable = false, length = 8)
    @Builder.Default private String workZone = "未分区";

    @Column(name = "business_line", length = 64) private String businessLine;
    @Column(name = "customer_name", length = 128) private String customerName;
    @Column(name = "delivery_location", length = 128) private String deliveryLocation;
    @Column(name = "project_manager_id") private Long projectManagerId;
    @Column(name = "implementation_lead_id") private Long implementationLeadId;
    @Column(name = "development_lead_id") private Long developmentLeadId;
    @Column(name = "customer_contact", length = 64) private String customerContact;
    @Column(name = "planned_start_date") private LocalDate plannedStartDate;
    @Column(name = "planned_end_date") private LocalDate plannedEndDate;
    @Column(name = "actual_start_date") private LocalDate actualStartDate;
    @Column(name = "actual_end_date") private LocalDate actualEndDate;
    @Column(nullable = false, length = 16) @Builder.Default private String phase = "立项";
    @Column(name = "project_status", nullable = false, length = 16) @Builder.Default private String projectStatus = "未启动";
    @Column(name = "health_status", nullable = false, length = 16) @Builder.Default private String healthStatus = "正常";
    @Column(columnDefinition = "TEXT") private String scope;
    @Column(columnDefinition = "TEXT") private String deliverables;
    @Column(name = "acceptance_criteria", columnDefinition = "TEXT") private String acceptanceCriteria;
    @Column(name = "risk_description", columnDefinition = "TEXT") private String riskDescription;
    @Column(name = "current_issues", columnDefinition = "TEXT") private String currentIssues;
    @Column(name = "next_steps", columnDefinition = "TEXT") private String nextSteps;
    @Column(name = "implementation_mode", nullable = false, length = 16) @Builder.Default private String implementationMode = "混合";
    @Column(name = "go_live_date") private LocalDate goLiveDate;
    @Column(name = "support_end_date") private LocalDate supportEndDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(nullable = false)
    @Builder.Default
    private boolean archived = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
