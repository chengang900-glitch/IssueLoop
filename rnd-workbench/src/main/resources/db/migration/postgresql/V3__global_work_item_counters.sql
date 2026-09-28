CREATE TABLE work_item_counters (
    type       VARCHAR(8) PRIMARY KEY,
    next_value INTEGER NOT NULL
);

INSERT INTO work_item_counters (type, next_value)
SELECT t.type, COALESCE((SELECT MAX(w.seq_no) + 1 FROM work_items w WHERE w.type = t.type), 1)
FROM (VALUES ('需求'), ('任务'), ('测试'), ('缺陷')) AS t(type);
