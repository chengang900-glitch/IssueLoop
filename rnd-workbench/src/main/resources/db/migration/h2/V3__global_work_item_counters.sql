CREATE TABLE work_item_counters (
    type VARCHAR(8) PRIMARY KEY,
    next_value INTEGER NOT NULL
);

INSERT INTO work_item_counters (type, next_value) VALUES ('需求', 1);
INSERT INTO work_item_counters (type, next_value) VALUES ('任务', 1);
INSERT INTO work_item_counters (type, next_value) VALUES ('测试', 1);
INSERT INTO work_item_counters (type, next_value) VALUES ('缺陷', 1);
