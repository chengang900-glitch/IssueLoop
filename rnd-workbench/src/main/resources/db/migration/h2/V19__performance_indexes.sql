-- V19__performance_indexes.sql
-- 补齐外键列与常用筛选/排序列的索引（PostgreSQL 不会为外键自动建索引）。
-- 这些列在列表、详情装配、统计与级联删除的 WHERE 条件里被高频使用，
-- 缺索引时随数据增长会出现全表扫描。
-- 注意：生产库表较大时应改用 CREATE INDEX CONCURRENTLY（需放在事务外执行）。
CREATE INDEX idx_work_item_steps_item       ON work_item_steps(work_item_id, seq);
CREATE INDEX idx_comments_item_created      ON comments(work_item_id, created_at);
CREATE INDEX idx_attachments_item           ON attachments(work_item_id);
CREATE INDEX idx_sprints_project_start      ON sprints(project_id, start_date);
CREATE INDEX idx_project_members_user       ON project_members(user_id);
CREATE INDEX idx_work_items_project_created ON work_items(project_id, created_at);
CREATE INDEX idx_work_items_project_due     ON work_items(project_id, due_date);
CREATE INDEX idx_work_items_project_owner   ON work_items(project_id, owner_id);
CREATE INDEX idx_work_items_module          ON work_items(module_id);
CREATE INDEX idx_work_items_submodule       ON work_items(submodule_id);
CREATE INDEX idx_work_items_parent          ON work_items(parent_id);
CREATE INDEX idx_export_jobs_project        ON export_jobs(project_id);
