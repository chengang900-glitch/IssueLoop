UPDATE work_items SET status = CASE
    WHEN status IN ('新提交', '待评估') THEN '新建'
    WHEN status IN ('已分配', '处理中', '待测试', '测试中') THEN '进行中'
    WHEN status IN ('待验收', '已完成') THEN '已完成'
    WHEN status = '已关闭' THEN '已验收'
    ELSE status
END;
