package com.rnd.app.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "project_code_counters")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@IdClass(ProjectCodeCounterId.class)
public class ProjectCodeCounter {
    @Id @Column(length = 16) private String prefix;
    @Id @Column(length = 6) private String period;
    @Column(name = "last_sequence", nullable = false) private Integer lastSequence;
}
