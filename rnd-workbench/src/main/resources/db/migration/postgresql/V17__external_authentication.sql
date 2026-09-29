CREATE TABLE external_identities (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(16) NOT NULL,
    provider_instance VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id),
    display_name VARCHAR(128),
    email VARCHAR(255),
    avatar VARCHAR(512),
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_external_identity_subject UNIQUE (provider, provider_instance, subject),
    CONSTRAINT uq_external_identity_user_provider UNIQUE (user_id, provider, provider_instance)
);
CREATE TABLE auth_login_transactions (
    state VARCHAR(96) PRIMARY KEY,
    provider VARCHAR(16) NOT NULL,
    code_verifier VARCHAR(128),
    redirect_uri VARCHAR(512) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed BOOLEAN NOT NULL DEFAULT FALSE,
    local_user_id BIGINT,
    external_subject VARCHAR(255),
    external_display_name VARCHAR(128),
    external_email VARCHAR(255),
    external_avatar VARCHAR(512),
    error_message VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_auth_login_transactions_expiry ON auth_login_transactions(expires_at);
