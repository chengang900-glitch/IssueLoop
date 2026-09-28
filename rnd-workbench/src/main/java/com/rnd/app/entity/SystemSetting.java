package com.rnd.app.entity;

import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "system_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SystemSetting {
    @Id @Column(name = "setting_key", length = 64) private String settingKey;
    @Column(name = "setting_value", nullable = false, length = 255) private String settingValue;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
