package com.rnd.app.entity;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "external_identities", uniqueConstraints = {
        @UniqueConstraint(name = "uq_external_identity_subject", columnNames = {"provider", "provider_instance", "subject"}),
        @UniqueConstraint(name = "uq_external_identity_user_provider", columnNames = {"user_id", "provider", "provider_instance"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExternalIdentity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 16) private String provider;
    @Column(name = "provider_instance", nullable = false, length = 255) private String providerInstance;
    @Column(nullable = false, length = 255) private String subject;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(length = 128) private String displayName;
    @Column(length = 255) private String email;
    @Column(length = 512) private String avatar;
    @Column(name = "last_login_at") private Instant lastLoginAt;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
}
