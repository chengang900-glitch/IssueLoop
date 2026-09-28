package com.rnd.app.entity;

import lombok.*;
import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "work_item_steps")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkItemStep {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_item_id", nullable = false, length = 16)
    private String workItemId;

    @Column(nullable = false)
    private Integer seq;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Boolean done = false;
}