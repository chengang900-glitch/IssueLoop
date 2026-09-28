CREATE TABLE saved_filters (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id      BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name            VARCHAR(64) NOT NULL,
    conditions_json TEXT NOT NULL,
    sort_json       TEXT,
    group_by        VARCHAR(32),
    columns_json    TEXT,
    is_default      BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, project_id, name)
);
CREATE INDEX idx_saved_filters_user_project ON saved_filters(user_id, project_id);

CREATE TABLE user_view_preferences (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    project_id   BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    columns_json TEXT,
    sort_json    TEXT,
    group_by     VARCHAR(32),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, project_id)
);

CREATE TABLE work_item_watchers (
    id           BIGSERIAL PRIMARY KEY,
    work_item_id VARCHAR(16) NOT NULL REFERENCES work_items(id) ON DELETE CASCADE,
    user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (work_item_id, user_id)
);
CREATE INDEX idx_work_item_watchers_user ON work_item_watchers(user_id, created_at DESC);

CREATE TABLE export_jobs (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    creator_id   BIGINT NOT NULL REFERENCES users(id),
    status       VARCHAR(16) NOT NULL,
    request_json TEXT NOT NULL,
    file_name    VARCHAR(255),
    storage_path VARCHAR(512),
    expires_at   TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_export_jobs_creator ON export_jobs(creator_id, created_at DESC);
