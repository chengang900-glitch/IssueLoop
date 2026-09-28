-- V1__init_schema.sql: 核心表 DDL
-- 用户
CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    nickname      VARCHAR(64)  NOT NULL,
    avatar        VARCHAR(255),
    system_role   VARCHAR(16)  NOT NULL DEFAULT 'USER',
    status        INTEGER      NOT NULL DEFAULT 1,
    fail_count    INTEGER      NOT NULL DEFAULT 0,
    lock_until    TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 项目
CREATE TABLE projects (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    short_name  VARCHAR(8)   NOT NULL,
    description TEXT,
    created_by  BIGINT       NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 项目成员
CREATE TABLE project_members (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id    BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role       VARCHAR(16)  NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (project_id, user_id)
);

-- 模块
CREATE TABLE modules (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name       VARCHAR(64)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (project_id, name)
);

-- 迭代
CREATE TABLE sprints (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name       VARCHAR(64)  NOT NULL,
    start_date DATE,
    end_date   DATE,
    status     VARCHAR(16)  NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 工作项
CREATE TABLE work_items (
    id           VARCHAR(16)  PRIMARY KEY,
    seq_no       INTEGER      NOT NULL,
    project_id   BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    type         VARCHAR(8)   NOT NULL,
    title        VARCHAR(255) NOT NULL,
    status       VARCHAR(16)  NOT NULL DEFAULT '新提交',
    owner_id     BIGINT       REFERENCES users(id),
    priority     VARCHAR(4)   NOT NULL DEFAULT 'P2',
    sprint_id    BIGINT       REFERENCES sprints(id),
    module       VARCHAR(64),
    severity     VARCHAR(8)   NOT NULL DEFAULT '普通',
    creator_id   BIGINT       NOT NULL REFERENCES users(id),
    due_date     TIMESTAMPTZ,
    description  TEXT,
    expected     TEXT,
    actual       TEXT,
    parent_id    VARCHAR(16)  REFERENCES work_items(id),
    tags         TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (project_id, type, seq_no)
);
CREATE INDEX idx_work_items_project ON work_items(project_id);
CREATE INDEX idx_work_items_owner ON work_items(owner_id);
CREATE INDEX idx_work_items_sprint ON work_items(sprint_id);
CREATE INDEX idx_work_items_status ON work_items(project_id, status);

-- 工作项步骤
CREATE TABLE work_item_steps (
    id           BIGSERIAL PRIMARY KEY,
    work_item_id VARCHAR(16)  NOT NULL REFERENCES work_items(id) ON DELETE CASCADE,
    seq          INTEGER      NOT NULL,
    content      VARCHAR(500) NOT NULL,
    done         BOOLEAN      NOT NULL DEFAULT false
);

-- 评论
CREATE TABLE comments (
    id           BIGSERIAL PRIMARY KEY,
    work_item_id VARCHAR(16)  NOT NULL REFERENCES work_items(id) ON DELETE CASCADE,
    author_id    BIGINT       NOT NULL REFERENCES users(id),
    content      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 活动
CREATE TABLE activities (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT       NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    work_item_id VARCHAR(16)  NOT NULL REFERENCES work_items(id) ON DELETE CASCADE,
    actor_id     BIGINT       NOT NULL REFERENCES users(id),
    type         VARCHAR(32)  NOT NULL,
    content      TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_activities_project ON activities(project_id, created_at DESC);
CREATE INDEX idx_activities_item ON activities(work_item_id, created_at DESC);

-- 附件
CREATE TABLE attachments (
    id           BIGSERIAL PRIMARY KEY,
    work_item_id VARCHAR(16)  NOT NULL REFERENCES work_items(id) ON DELETE CASCADE,
    file_name    VARCHAR(255) NOT NULL,
    size         BIGINT       NOT NULL,
    mime_type    VARCHAR(128),
    storage_path VARCHAR(512) NOT NULL,
    uploader_id  BIGINT       NOT NULL REFERENCES users(id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 通知
CREATE TABLE notifications (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type         VARCHAR(32)  NOT NULL,
    work_item_id VARCHAR(16),
    content      TEXT,
    is_read      BOOLEAN      NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user ON notifications(user_id, is_read, created_at DESC);

-- 收藏
CREATE TABLE favorites (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type VARCHAR(16)  NOT NULL,
    target_id   VARCHAR(64)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (user_id, target_type, target_id)
);
