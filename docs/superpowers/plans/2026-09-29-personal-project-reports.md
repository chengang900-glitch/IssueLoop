# Personal and Project Reporting Implementation Plan

**Goal:** 落实已批准的方案 A，实现个人首页、单项目分析和多维报表。

**Architecture:** Spring Boot 服务统一权限过滤、统计口径、分页及 CSV 导出。独立 insights.js/insights.css 负责三个分析页面，复用现有详情侧栏与任务操作。

**Tech Stack:** Java 11 compatible / Spring Boot 2.7 / JPA / vanilla JS / CSS / SVG，无新依赖。

## Global Constraints

不增加工时填报、不删除分区数据、不改变流转权限；实际工时按当前负责人和完成日期归集，不充当每日投入。

## Tasks

- [x] 1. 新增 WorkItemScope、AnalyticsQuery、AnalyticsService、AnalyticsController，统一个人待处理条件和分析统计。测试 AnalyticsIntegrationTest：权限、201+ 条任务、空值、上海日期边界、归集、导出和下钻一致性；更新 WorkItemPersonalViewTest 的待处理定义。
- [x] 2. 新增 static/insights.js、insights.css，修改 index.html 菜单与页面容器，修改 script.js 的登录、导航和详情刷新连接。保留分区实现，注释入口，隐藏档案字段并去除保存请求中的 workZone。
- [x] 3. 前端语法检查；执行 `mvn -o -q test` 和 `mvn -o -q package`，失败仅在相关范围修复。临时 H2 实例浏览器验证登录首页、切换、图表/明细、筛选导出、原任务侧栏、无项目状态及窄屏。结束时保留验证说明。

## Interfaces

`GET /api/v1/analytics`：AnalyticsQuery 绑定 scope(personal/project/report), projectId, ownerId, type, status, keyword, dateBasis(created/completed), from/to(yyyy-MM-dd), groupBy(project/person/type/time), period(week/month), lane, drillDimension, drillKey, page, size。
返回 summary, groups, projects, people, statuses, types, trend, personal, list, total, page, size, options。

`GET /api/v1/analytics/export?format=summary|details`：相同筛选，UTF-8 BOM CSV，公式安全处理，超 10000 行拒绝。

`showInsights(page, reset)`：显示 dashboard/project-dashboard/project-reports；`isInsightsPage()` 用于现有刷新路径；`bindInsightsEvents()` 注册独立页面事件。

## 完成记录

2026-09-29：后端全套 71 项测试通过；前端语法检查通过。桌面与 390px 窄屏浏览器验收已完成，详细记录见../specs/2026-09-29-personal-project-reports-validation.md。新页面窄屏适配及账号切换数据清空已补齐。未修改现有业务数据库、未部署现有服务。
