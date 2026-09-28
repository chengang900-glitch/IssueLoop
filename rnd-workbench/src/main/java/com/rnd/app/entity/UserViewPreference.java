package com.rnd.app.entity;

import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_view_preferences", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "project_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserViewPreference {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "project_id", nullable = false) private Long projectId;
    @Column(name = "columns_json", columnDefinition = "TEXT") private String columnsJson;
    @Column(name = "sort_json", columnDefinition = "TEXT") private String sortJson;
    @Column(name = "group_by", length = 32) private String groupBy;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
