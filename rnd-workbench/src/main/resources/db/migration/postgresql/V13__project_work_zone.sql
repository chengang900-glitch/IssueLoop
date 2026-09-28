ALTER TABLE projects ADD COLUMN work_zone VARCHAR(8) NOT NULL DEFAULT '未分区';
CREATE INDEX idx_projects_work_zone ON projects(work_zone);
