package com.rnd.app.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "work_item_counters")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WorkItemCounter {
    @Id
    @Column(length = 8)
    private String type;

    @Column(name = "next_value", nullable = false)
    private Integer nextValue;
}
