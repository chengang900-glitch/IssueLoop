package com.rnd.app.entity;

import lombok.*;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "auth_login_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthLoginTransaction {
    @Id @Column(length = 96) private String state;
    @Column(nullable = false, length = 16) private String provider;
    @Column(name = "code_verifier", length = 128) private String codeVerifier;
    @Column(name = "redirect_uri", nullable = false, length = 512) private String redirectUri;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(nullable = false) @Builder.Default private boolean consumed = false;
    @Column(name = "local_user_id") private Long localUserId;
    @Column(name = "external_subject", length = 255) private String externalSubject;
    @Column(name = "external_display_name", length = 128) private String externalDisplayName;
    @Column(name = "external_email", length = 255) private String externalEmail;
    @Column(name = "external_avatar", length = 512) private String externalAvatar;
    @Column(name = "error_message", length = 255) private String errorMessage;
    @Column(name = "created_at", nullable = false) @Builder.Default private Instant createdAt = Instant.now();
}
