const fs = require("fs");
const path = require("path");
const {
  AlignmentType, BorderStyle, Document, Footer, HeadingLevel, LevelFormat,
  PageNumber, Packer, Paragraph, Table, TableCell, TableRow, TextRun,
  WidthType, ShadingType,
} = require("docx");

const output = path.resolve(__dirname, "..", "研发工作台-API设计-V1.1.docx");
const blue = "286FF0";
const lightBlue = "EAF1FF";
const gray = "667085";
const border = { style: BorderStyle.SINGLE, size: 1, color: "D7DDE8" };
const borders = { top: border, bottom: border, left: border, right: border };
const p = (text) => new Paragraph({ spacing: { after: 120, line: 320 }, children: [new TextRun({ text, font: "Arial", size: 22 })] });
const code = (text) => new Paragraph({ spacing: { after: 100 }, shading: { fill: "F3F6FB", type: ShadingType.CLEAR }, indent: { left: 240, right: 240 }, children: [new TextRun({ text, font: "Menlo", size: 18 })] });
const h1 = (text) => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun(text)] });
const h2 = (text) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(text)] });
const bullet = (text) => new Paragraph({ numbering: { reference: "bullets", level: 0 }, spacing: { after: 80 }, children: [new TextRun({ text, size: 22, font: "Arial" })] });

function table(headers, rows, widths) {
  const cell = (text, width, header = false) => new TableCell({
    width: { size: width, type: WidthType.DXA }, borders,
    shading: header ? { fill: lightBlue, type: ShadingType.CLEAR } : undefined,
    margins: { top: 90, bottom: 90, left: 110, right: 110 },
    children: [new Paragraph({ children: [new TextRun({ text: String(text), bold: header, size: 19, font: "Arial" })] })],
  });
  return new Table({ width: { size: widths.reduce((a, b) => a + b, 0), type: WidthType.DXA }, columnWidths: widths,
    rows: [new TableRow({ children: headers.map((x, i) => cell(x, widths[i], true)) }), ...rows.map((row) => new TableRow({ children: row.map((x, i) => cell(x, widths[i])) }))] });
}

const children = [
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 1800, after: 260 }, children: [new TextRun({ text: "研发工作台", bold: true, size: 48, color: blue, font: "Arial" })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 220 }, children: [new TextRun({ text: "后端 API 设计文档", bold: true, size: 34, font: "Arial" })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 1200 }, children: [new TextRun({ text: "V1.1 · 配套 PRD V1.1", size: 26, color: gray, font: "Arial" })] }),
  table(["项目", "内容"], [["版本", "V1.1"], ["日期", "2026-07-07"], ["状态", "待评审"], ["兼容原则", "保留已有 /api/v1 接口；新增接口不破坏 V1 客户端"], ["数据库", "开发 H2 文件库；生产 PostgreSQL；Flyway 双迁移目录"]], [2200, 6826]),

  h1("1. 概述"),
  p("本文档定义研发工作台 V1.1 新增协作效率接口，覆盖个人视图、保存筛选器、列表偏好、批量处理、工作项关注、通知增强和 Excel 导出。V1.0 已有认证、项目、工作项、评论、活动和附件接口保持兼容。"),
  h2("1.1 通用约定"),
  table(["项目", "约定"], [["Base URL", "/api/v1"], ["鉴权", "Authorization: Bearer <token>"], ["时间", "ISO 8601，带时区"], ["响应", "{ code, message, data }"], ["分页", "{ list, total, page, size }"], ["Content-Type", "application/json；文件下载除外"]], [2200, 6826]),
  h2("1.2 错误码"),
  table(["code", "HTTP", "含义"], [["0", "200/201", "成功"], ["1001", "401", "未登录或 token 失效"], ["1002", "403", "无权限"], ["1003", "404", "资源不存在"], ["1004", "400", "参数或筛选条件非法"], ["1005", "409", "状态流转冲突"], ["1006", "409", "名称或唯一约束冲突"], ["1500", "500", "内部错误"]], [1400, 1400, 6226]),

  h1("2. 权限模型"),
  table(["能力", "访客", "成员", "项目管理员"], [["查询/筛选", "允许", "允许", "允许"], ["保存个人配置", "允许", "允许", "允许"], ["关注/取消关注", "允许", "允许", "允许"], ["Excel 导出", "允许", "允许", "允许"], ["批量修改", "禁止", "允许", "允许"], ["批量删除", "禁止", "禁止", "允许"]], [3200, 1900, 1900, 2026]),
  bullet("所有通过工作项、筛选器、导出任务 ID 访问的接口必须先解析所属项目。"),
  bullet("个人视图中的当前用户由 JWT 解析，客户端不得指定他人身份。"),

  h1("3. 工作项查询扩展"),
  h2("3.1 GET /projects/{projectId}/work-items"),
  p("在 V1 查询参数基础上新增以下参数。"),
  table(["参数", "类型", "说明"], [["view", "string", "created-by-me / assigned-to-me / pending-for-me / watched-by-me / unclosed"], ["creatorId", "long", "创建人"], ["priority", "string", "P0/P1/P2/P3"], ["severity", "string", "严重程度"], ["module", "string", "模块"], ["tag", "string", "标签"], ["dueFrom/dueTo", "datetime", "截止时间范围"], ["sort", "string", "updatedAt,desc 等白名单字段"], ["groupBy", "string", "status/owner/type/sprint"]], [2200, 1600, 5226]),
  code("GET /api/v1/projects/1/work-items?view=pending-for-me&priority=P1&sort=dueDate,asc&page=1&size=20"),
  p("groupBy 不改变分页数据结构；响应 data 可增加 groups 元数据，前端仍可使用 list 渲染普通列表。"),

  h1("4. 保存筛选器"),
  table(["方法", "路径", "说明"], [["GET", "/projects/{projectId}/saved-filters", "当前用户筛选器列表"], ["POST", "/projects/{projectId}/saved-filters", "创建筛选器"], ["PUT", "/saved-filters/{filterId}", "更新名称和配置"], ["DELETE", "/saved-filters/{filterId}", "删除筛选器"], ["PUT", "/saved-filters/{filterId}/default", "设为项目默认筛选器"]], [1200, 4300, 3526]),
  h2("4.1 创建/更新请求"),
  code('{ "name": "本迭代高优先级缺陷", "conditions": [{ "field": "type", "operator": "EQ", "value": "缺陷" }, { "field": "priority", "operator": "IN", "value": ["P0", "P1"] }], "sort": [{ "field": "updatedAt", "direction": "DESC" }], "groupBy": "status", "columns": ["id", "title", "status", "owner", "priority", "dueDate"] }'),
  h2("4.2 条件白名单"),
  table(["字段类型", "运算符"], [["枚举/成员", "EQ、NE、IN、NOT_IN"], ["文本", "EQ、CONTAINS"], ["时间", "GT、GTE、LT、LTE、BETWEEN"], ["空值", "IS_NULL、IS_NOT_NULL"]], [3000, 6026]),
  bullet("服务端拒绝未知字段、未知运算符和类型不匹配的值。"),
  bullet("同一用户和项目内筛选器名称唯一。"),

  h1("5. 列表偏好"),
  table(["方法", "路径", "说明"], [["GET", "/projects/{projectId}/view-preference", "读取当前用户配置"], ["PUT", "/projects/{projectId}/view-preference", "覆盖保存配置"]], [1200, 4800, 3026]),
  code('{ "columns": ["id", "title", "type", "status", "owner", "priority", "sprint", "dueDate"], "sort": [{ "field": "updatedAt", "direction": "DESC" }], "groupBy": "status" }'),
  bullet("columns 仅接受可展示字段白名单。"),
  bullet("groupBy 允许 null，表示取消分组。"),

  h1("6. 批量处理"),
  table(["方法", "路径", "权限"], [["POST", "/projects/{projectId}/work-items/bulk-update", "MEMBER 及以上"], ["POST", "/projects/{projectId}/work-items/bulk-delete", "PROJECT_ADMIN"]], [1200, 5200, 2626]),
  h2("6.1 批量更新请求"),
  code('{ "ids": ["BUG-1001", "BUG-1002"], "changes": { "ownerId": 5, "priority": "P1", "status": "处理中" } }'),
  h2("6.2 响应"),
  code('{ "successes": [{ "id": "BUG-1001" }], "failures": [{ "id": "BUG-1002", "code": 1005, "message": "不允许从待验收转移到处理中" }] }'),
  bullet("单次最多 200 条，超出返回 400。"),
  bullet("逐项开启短事务并校验项目归属、权限和状态机。"),
  bullet("部分失败不回滚已经成功的项目。"),
  bullet("批量删除记录操作人、工作项 ID 和结果摘要。"),

  h1("7. 工作项关注"),
  table(["方法", "路径", "说明"], [["GET", "/work-items/{workItemId}/watchers", "关注者列表"], ["POST", "/work-items/{workItemId}/watchers/me", "当前用户关注"], ["DELETE", "/work-items/{workItemId}/watchers/me", "取消关注"], ["GET", "/projects/{projectId}/watched-work-items", "当前用户关注列表"]], [1200, 4800, 3026]),
  bullet("重复关注保持幂等，返回当前关注状态。"),
  bullet("状态变化和新评论通知关注者，操作人本人不重复接收。"),

  h1("8. 通知中心增强"),
  h2("8.1 GET /notifications"),
  table(["参数", "说明"], [["read", "true/false"], ["type", "ASSIGN/STATUS/MENTION/DUE/WATCHED_UPDATE"], ["workItemId", "关联工作项"], ["page/size", "分页"]], [2500, 6526]),
  bullet("同一用户、工作项和通知类型在 5 分钟内可合并更新。"),
  bullet("保留 PUT /notifications/{id}/read 和 PUT /notifications/read-all。"),

  h1("9. Excel 导出"),
  table(["方法", "路径", "说明"], [["POST", "/projects/{projectId}/exports/work-items", "创建导出任务"], ["GET", "/exports/{exportId}", "查询任务状态"], ["GET", "/exports/{exportId}/download", "下载文件"]], [1200, 4800, 3026]),
  h2("9.1 创建导出请求"),
  code('{ "conditions": [{ "field": "status", "operator": "NE", "value": "已关闭" }], "sort": [{ "field": "updatedAt", "direction": "DESC" }], "columns": ["id", "title", "type", "status", "owner", "priority", "dueDate"] }'),
  h2("9.2 状态响应"),
  code('{ "id": 101, "status": "READY", "fileName": "研发工作台-工作项-20260707.xlsx", "expiresAt": "2026-07-08T12:00:00+08:00" }'),
  bullet("导出只包含当前用户有权访问的数据。"),
  bullet("文件保留 24 小时，过期后下载返回 404。"),
  bullet("V1.1 可同步生成，但接口保留 PENDING/PROCESSING/READY/FAILED 状态。"),

  h1("10. 数据库设计"),
  h2("10.1 saved_filters"),
  code("id BIGINT PK; user_id BIGINT; project_id BIGINT; name VARCHAR(64); conditions_json TEXT; sort_json TEXT; group_by VARCHAR(32); columns_json TEXT; is_default BOOLEAN; created_at; updated_at"),
  h2("10.2 user_view_preferences"),
  code("id BIGINT PK; user_id BIGINT; project_id BIGINT; columns_json TEXT; sort_json TEXT; group_by VARCHAR(32); updated_at; UNIQUE(user_id, project_id)"),
  h2("10.3 work_item_watchers"),
  code("id BIGINT PK; work_item_id VARCHAR(16); user_id BIGINT; created_at; UNIQUE(work_item_id, user_id)"),
  h2("10.4 export_jobs"),
  code("id BIGINT PK; project_id BIGINT; creator_id BIGINT; status VARCHAR(16); request_json TEXT; file_name VARCHAR(255); storage_path VARCHAR(512); expires_at; created_at"),
  h2("10.5 索引"),
  bullet("saved_filters(user_id, project_id)"), bullet("work_item_watchers(user_id, created_at)"), bullet("export_jobs(creator_id, created_at DESC)"),

  h1("11. Flyway 迁移"),
  table(["数据库", "脚本"], [["PostgreSQL", "db/migration/V4__v1_1_collaboration.sql"], ["H2", "db/migration/h2/V4__v1_1_collaboration.sql"]], [2500, 6526]),
  bullet("两套脚本保持表名、列名、索引和唯一约束一致。"),
  bullet("JSON 配置使用 TEXT/VARCHAR 保存，由应用层序列化和白名单校验。"),
  bullet("生产继续使用 PostgreSQL Profile，开发默认 H2 文件库。"),

  h1("12. 安全与审计"),
  bullet("批量接口、导出接口设置项目级权限校验。"),
  bullet("筛选字段和排序字段使用枚举白名单，禁止拼接客户端 SQL。"),
  bullet("导出文件不包含密码、令牌、附件存储路径等敏感字段。"),
  bullet("批量修改、批量删除和导出记录活动日志。"),
  bullet("下载导出文件时再次校验创建人和项目权限。"),

  h1("13. 测试要求"),
  bullet("个人视图身份不可伪造。"),
  bullet("保存筛选器名称唯一、默认筛选器唯一。"),
  bullet("条件白名单和类型校验覆盖非法输入。"),
  bullet("批量操作覆盖全部成功、部分失败和全部失败。"),
  bullet("关注接口幂等，通知不重复发送给操作人。"),
  bullet("导出字段、顺序、筛选结果和权限正确。"),
  bullet("H2 与 PostgreSQL V4 迁移均通过。"),

  h1("14. 实施顺序"),
  bullet("查询扩展与个人视图。"), bullet("保存筛选器与列表偏好。"), bullet("批量处理。"), bullet("关注和通知增强。"), bullet("Excel 导出。"),
];

const doc = new Document({
  styles: { default: { document: { run: { font: "Arial", size: 22, color: "1E2532" } } }, paragraphStyles: [
    { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true, run: { size: 32, bold: true, color: blue, font: "Arial" }, paragraph: { spacing: { before: 340, after: 180 }, outlineLevel: 0 } },
    { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true, run: { size: 27, bold: true, color: "1E2532", font: "Arial" }, paragraph: { spacing: { before: 220, after: 130 }, outlineLevel: 1 } },
  ] },
  numbering: { config: [{ reference: "bullets", levels: [{ level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 480, hanging: 240 } } } }] }] },
  sections: [{ properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1200, right: 1440, bottom: 1200, left: 1440 } } },
    footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: "研发工作台 API V1.1  ·  ", color: gray, size: 18 }), new TextRun({ children: [PageNumber.CURRENT], color: gray, size: 18 })] })] }) }, children }],
});

Packer.toBuffer(doc).then((buffer) => { fs.writeFileSync(output, buffer); console.log(output); });
