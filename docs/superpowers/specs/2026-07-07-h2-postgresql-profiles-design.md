# H2 开发库与 PostgreSQL 生产库设计

日期：2026-07-07  
状态：待实施

## 1. 目标

项目默认使用 H2 文件数据库，使开发和体验环境无需安装 PostgreSQL 即可启动；生产环境通过 Spring Profile 切换到 PostgreSQL。数据库差异不得进入控制器或业务 Service。

## 2. 配置结构

- `application.yml`：默认开发配置，使用 H2 文件库 `./data/rnd-workbench`。
- `application-prod.yml`：生产配置，使用 PostgreSQL，连接信息通过环境变量覆盖。
- 默认启动不指定 Profile；生产启动指定 `--spring.profiles.active=prod`。
- H2 控制台仅在默认开发配置启用，生产配置明确关闭。

## 3. 数据库迁移

- H2 使用 `classpath:db/migration/h2`。
- PostgreSQL 继续使用 `classpath:db/migration` 中现有迁移。
- 两套迁移保持相同表名、列名、约束和初始管理员数据。
- H2 脚本使用 H2 支持的类型和语法，不依赖 PostgreSQL 专属的 `BIGSERIAL`、`TIMESTAMPTZ` 或 `ON CONFLICT`。
- 后续数据库结构变更必须同时增加同版本号的 H2 与 PostgreSQL 迁移。

## 4. 工作项编号

新增 `WorkItemCounter` JPA 实体，计数表以工作项类型为主键。编号生成器在事务内使用 `PESSIMISTIC_WRITE` 锁定对应计数行，读取当前值后递增保存。四种工作项类型的计数行由迁移脚本预置，因此不会发生首次并发插入竞争。

该实现同时支持 H2 和 PostgreSQL，不再执行数据库专属原生 SQL。

## 5. H2 数据持久化

- 使用文件模式，不使用内存模式。
- 数据文件位于项目运行目录的 `data` 文件夹。
- 关闭应用并重新启动后，项目、工作项、评论和附件元数据仍保留。
- H2 控制台路径为 `/h2-console`，仅用于本地排查。

## 6. 安全与生产配置

- PostgreSQL URL、用户名、密码和 JWT 密钥通过环境变量提供。
- `application-prod.yml` 不包含真实生产密码。
- 生产环境禁用 H2 控制台。
- 生产环境继续使用 `ddl-auto: validate`，结构由 Flyway 管理。

## 7. 测试与验收

1. Maven 测试能启动 H2 Spring 上下文并完成全部 Flyway 迁移。
2. 默认运行 JAR 无需 PostgreSQL 即可显示登录页面并成功登录。
3. 创建项目和工作项后重启应用，数据仍存在。
4. 工作项编号连续且不重复。
5. 使用 `prod` Profile 打包和配置解析正常，不引用 H2 驱动或 H2 迁移目录。
6. 现有单元测试继续通过。

## 8. 不在本次范围

- 自动安装或托管 PostgreSQL。
- H2 数据自动迁移到 PostgreSQL。
- 数据库主从、备份或高可用配置。

