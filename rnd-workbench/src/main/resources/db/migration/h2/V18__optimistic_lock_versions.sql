-- V18__optimistic_lock_versions.sql
-- 为并发写入密集的表增加乐观锁版本列。
-- 背景：所有写路径都是「findById → 改字段 → save()（整实体 merge UPDATE）」，
-- 并发时后提交的旧快照会覆盖整行，曾导致管理员重置口令 / 递增 token_version 被
-- 并发的“改昵称”请求回写，旧 JWT 复活、重置被静默撤销。加上 @Version 后，
-- 陈旧快照的提交会被拒绝（应用层返回 409），而不是静默覆盖。
ALTER TABLE users ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE projects ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE work_items ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
