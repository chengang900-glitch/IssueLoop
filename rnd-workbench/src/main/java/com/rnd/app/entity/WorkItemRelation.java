package com.rnd.app.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(name = "work_item_relations", uniqueConstraints = @UniqueConstraint(columnNames = {"source_work_item_id", "target_work_item_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkItemRelation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_work_item_id", nullable = false, length = 16)
    private String sourceWorkItemId;

    @Column(name = "target_work_item_id", nullable = false, length = 16)
    private String targetWorkItemId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
