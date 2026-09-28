package com.rnd.app.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "saved_filters", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "project_id", "name"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SavedFilter {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "project_id", nullable = false) private Long projectId;
    @Column(nullable = false, length = 64) private String name;
    @Column(name = "conditions_json", nullable = false, columnDefinition = "TEXT") private String conditionsJson;
    @Column(name = "sort_json", columnDefinition = "TEXT") private String sortJson;
    @Column(name = "group_by", length = 32) private String groupBy;
    @Column(name = "columns_json", columnDefinition = "TEXT") private String columnsJson;
    @Column(name = "is_default", nullable = false) @Builder.Default private Boolean defaultFilter = false;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
