# 研发工作台备份、恢复与生产巡检

## 每日巡检

1. 检查应用进程和 `/api/v1/auth/login` 的响应日志，无连续 500 错误。
2. 检查 PostgreSQL 可连接、磁盘剩余空间和当天备份结果。
3. 检查附件目录与导出目录可读写，清理已超过 24 小时的导出文件。
4. 检查 Flyway 版本与发布 JAR 内迁移一致；本版本应包含 V15。

## 备份

每天在低峰期执行 PostgreSQL 逻辑备份，并同时备份附件存储目录和 `data/exports`（导出目录可不长期保留）：

```bash
pg_dump -Fc -h "$DB_HOST" -U "$DB_USERNAME" "$DB_NAME" > rnd-workbench-$(date +%F).dump
tar -czf rnd-workbench-attachments-$(date +%F).tgz ./data/attachments
```

备份至少保留 7 个日备份和 3 个周备份；备份文件应存放到与应用主机隔离的位置。

## 恢复演练

1. 在隔离环境停止应用并创建空数据库。
2. 使用 `pg_restore -d <database> <backup.dump>` 恢复数据库。
3. 恢复附件目录后，以生产 profile 启动 JAR。
4. 验证管理员登录、项目列表、附件下载和一条工作项详情。
5. 记录恢复耗时和失败原因；每季度至少演练一次。

## 发布前检查

1. 执行 `mvn -q test package`。
2. 用生产连接串在备份库验证 Flyway 迁移；不得手工跳过迁移。
3. 配置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`JWT_SECRET`，并确认密钥不是默认值。`JWT_SECRET` 至少 32 字节：未配置时应用会用进程随机密钥启动（重启即失效，仅便于本地开发），使用仓库内置占位值或长度不足会直接拒绝启动。若初始管理员仍使用种子口令，首次生产启动前还须设置至少 12 位的 `APP_BOOTSTRAP_ADMIN_PASSWORD`；应用会轮换口令并要求管理员首次登录后修改。
4. 替换 JAR 后重启服务，检查登录、项目管理、创建项目、工作项列表和附件下载。
