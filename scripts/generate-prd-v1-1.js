const fs = require("fs");
const path = require("path");
const {
  AlignmentType, BorderStyle, Document, Footer, HeadingLevel, LevelFormat,
  PageNumber, Packer, Paragraph, Table, TableCell, TableRow, TextRun,
  WidthType, ShadingType,
} = require("docx");

const output = path.resolve(__dirname, "..", "研发工作台-PRD-V1.1.docx");
const blue = "286FF0";
const lightBlue = "EAF1FF";
const gray = "667085";
const border = { style: BorderStyle.SINGLE, size: 1, color: "D7DDE8" };
const borders = { top: border, bottom: border, left: border, right: border };

const p = (text, options = {}) => new Paragraph({
  spacing: { after: 120, line: 320 },
  ...options,
  children: [new TextRun({ text, font: "Arial", size: 22, ...options.run })],
});
const h1 = (text) => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun(text)] });
const h2 = (text) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(text)] });
const h3 = (text) => new Paragraph({ heading: HeadingLevel.HEADING_3, children: [new TextRun(text)] });
const bullet = (text) => new Paragraph({ numbering: { reference: "bullets", level: 0 }, spacing: { after: 80 }, children: [new TextRun({ text, font: "Arial", size: 22 })] });

function table(headers, rows, widths) {
  const makeCell = (text, width, header = false) => new TableCell({
    width: { size: width, type: WidthType.DXA }, borders,
    shading: header ? { fill: lightBlue, type: ShadingType.CLEAR } : undefined,
    margins: { top: 100, bottom: 100, left: 120, right: 120 },
    children: [new Paragraph({ children: [new TextRun({ text: String(text), bold: header, size: 20, font: "Arial" })] })],
  });
  return new Table({
    width: { size: widths.reduce((a, b) => a + b, 0), type: WidthType.DXA },
    columnWidths: widths,
    rows: [
      new TableRow({ children: headers.map((item, index) => makeCell(item, widths[index], true)) }),
      ...rows.map((row) => new TableRow({ children: row.map((item, index) => makeCell(item, widths[index])) })),
    ],
  });
}

const children = [
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { before: 1800, after: 280 }, children: [new TextRun({ text: "研发工作台", bold: true, size: 48, color: blue, font: "Arial" })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 220 }, children: [new TextRun({ text: "产品需求文档（PRD）", bold: true, size: 34, font: "Arial" })] }),
  new Paragraph({ alignment: AlignmentType.CENTER, spacing: { after: 1200 }, children: [new TextRun({ text: "V1.1 · 协作效率增强版", size: 26, color: gray, font: "Arial" })] }),
  table(["项目", "内容"], [
    ["文档版本", "V1.1"], ["日期", "2026-07-07"], ["状态", "待评审"],
    ["基线", "研发工作台 PRD V1.0"], ["产品定位", "面向小型研发团队的轻量研发协作平台"],
    ["部署形态", "Spring Boot 单 JAR；开发默认 H2 文件库，生产支持 PostgreSQL"],
  ], [2200, 6826]),

  h1("1. 产品概述"),
  h2("1.1 背景"),
  p("研发工作台用于统一管理需求、任务、测试和缺陷。V1.0 已建立登录、项目、工作项、状态机、评论、附件、活动记录和通知等基础能力。V1.1 的重点不是扩展为专业云测试平台，而是补齐成熟问题管理工具中高频、通用的协作效率能力。"),
  h2("1.2 产品边界"),
  bullet("聚焦项目协作、工作项跟踪、团队沟通和研发过程透明化。"),
  bullet("借鉴成熟工具的筛选、批量处理、关注通知和列表配置能力。"),
  bullet("不自建云真机、兼容测试、安全扫描、众测等专业测试基础设施。"),
  bullet("通过 Webhook/API 为后续对接外部测试平台保留扩展能力。"),
  h2("1.3 V1.1 目标"),
  bullet("让不同角色可以在 3 次操作以内定位本人需要处理或关注的工作项。"),
  bullet("让高频列表配置和筛选条件可以保存并跨登录恢复。"),
  bullet("让批量操作、导出和通知形成可控、可追溯的效率闭环。"),

  h1("2. 用户与权限"),
  table(["角色", "主要职责", "V1.1 权限"], [
    ["系统管理员", "用户和项目管理", "创建项目、管理用户及系统配置"],
    ["项目管理员", "项目协调与治理", "项目内全部读写、成员配置、批量删除"],
    ["项目成员", "创建和处理工作项", "读写、流转、评论、附件、批量修改"],
    ["项目访客", "查看和跟踪项目", "查询、个人筛选、关注、导出；不可修改工作项"],
  ], [1800, 2800, 4426]),
  h2("2.1 权限原则"),
  bullet("任何查询、筛选、批量操作和导出都必须遵守项目成员边界。"),
  bullet("个人筛选器、显示列和默认视图只影响当前用户。"),
  bullet("批量修改要求项目成员及以上；批量删除仅项目管理员可用。"),

  h1("3. V1 已有基础能力"),
  table(["模块", "已有能力"], [
    ["认证", "账号密码登录、JWT、失败锁定、用户信息"],
    ["项目", "项目、成员、项目角色、模块、Sprint、收藏"],
    ["工作项", "需求/任务/测试/缺陷、CRUD、状态机、看板、搜索和基础筛选"],
    ["协作", "评论、附件、活动记录、站内通知"],
    ["部署", "H2 文件库开发模式、PostgreSQL 生产 Profile、Flyway 迁移"],
  ], [2200, 6826]),

  h1("4. V1.1 功能需求"),
  h2("4.1 个人工作视图"),
  table(["视图", "规则"], [
    ["我创建的", "creatorId 为当前用户"],
    ["指派给我的", "ownerId 为当前用户且状态不是已关闭"],
    ["待我解决", "ownerId 为当前用户且状态不是已完成、已关闭"],
    ["我跟踪的", "当前用户已关注的工作项"],
    ["未关闭问题", "状态不是已关闭"],
  ], [2200, 6826]),
  bullet("每个预设视图显示实时数量。"),
  bullet("用户进入项目时默认打开其个人默认筛选器；未设置时打开全部工作项。"),

  h2("4.2 自定义筛选器"),
  bullet("支持类型、状态、负责人、创建人、优先级、严重程度、Sprint、模块、标签和截止时间组合筛选。"),
  bullet("支持保存、重命名、复制和删除个人筛选器。"),
  bullet("支持将筛选器设为当前项目默认视图。"),
  bullet("V1.1 不提供共享筛选器，避免增加权限和配置治理复杂度。"),

  h2("4.3 列表配置"),
  bullet("可选择显示字段并调整列顺序；配置按用户、项目保存。"),
  bullet("支持按更新时间、创建时间、截止时间和优先级单字段排序。"),
  bullet("支持按状态、负责人、类型和 Sprint 分组或取消分组。"),
  bullet("列表支持直接修改状态、负责人、优先级和标签；修改必须执行权限及状态机校验。"),

  h2("4.4 搜索与高级检索"),
  bullet("普通搜索匹配工作项 ID、标题、描述、模块和标签。"),
  bullet("高级检索使用字段、运算符和值组成的结构化条件构建器。"),
  bullet("多个条件默认使用 AND；同一字段的多个值使用 OR。"),
  bullet("V1.1 不提供自定义查询语言。"),

  h2("4.5 批量处理"),
  table(["操作", "规则"], [
    ["批量修改状态", "逐项校验状态机；返回成功项和失败原因"],
    ["批量指派", "修改负责人，可设置为未分配"],
    ["批量修改属性", "优先级、Sprint、模块和标签"],
    ["批量删除", "仅项目管理员；二次确认；记录活动日志"],
  ], [2600, 6426]),
  bullet("批量操作最多处理 200 条，超过时提示缩小筛选范围。"),
  bullet("部分失败不回滚已经成功的项目，结果页必须列出失败原因。"),

  h2("4.6 关注与通知中心"),
  bullet("用户可以关注或取消关注工作项。"),
  bullet("被指派、状态变化、评论提及和临近到期继续产生站内通知。"),
  bullet("关注者接收状态变化和新评论通知，但不接收普通字段修改通知。"),
  bullet("通知中心支持全部、未读筛选，单条已读和全部已读。"),
  bullet("同一工作项同类事件在短时间内合并展示，减少通知噪音。"),

  h2("4.7 Excel 导出"),
  bullet("导出当前筛选结果，字段和顺序遵循当前显示列。"),
  bullet("仅导出当前用户有权访问的项目数据。"),
  bullet("导出文件包含筛选条件、导出人和导出时间。"),
  bullet("V1.1 不做 Excel 批量导入。"),

  h2("4.8 Webhook 预留"),
  p("V1.1 仅完成接口边界设计，不提供可视化规则配置。后续可针对工作项创建、更新、状态变化和评论事件向外部系统发送签名后的 JSON 消息，用于对接 Testin、企业 IM 或内部平台。"),

  h1("5. 数据模型增量"),
  table(["实体", "关键字段", "约束"], [
    ["saved_filter", "userId、projectId、name、conditions、sort、groupBy、columns、isDefault", "用户和项目内名称唯一"],
    ["work_item_watcher", "workItemId、userId、createdAt", "工作项和用户联合唯一"],
    ["user_view_preference", "userId、projectId、columns、sort、groupBy", "用户和项目联合唯一"],
  ], [2200, 4600, 2226]),

  h1("6. 非功能需求"),
  bullet("单项目 1000 个工作项时，普通筛选和分页查询响应目标小于 500ms。"),
  bullet("批量操作必须记录操作人、时间、范围和结果。"),
  bullet("筛选条件 JSON 必须经过服务端字段白名单校验，禁止直接拼接 SQL。"),
  bullet("导出任务不得绕过权限校验，不得包含密码、令牌或附件存储路径。"),
  bullet("支持 Chrome、Edge、Firefox 最新两个大版本。"),

  h1("7. V1.1 验收标准"),
  table(["编号", "验收条件"], [
    ["A01", "用户能通过五个个人视图获取正确的实时工作项集合"],
    ["A02", "组合筛选保存后，重新登录仍可使用"],
    ["A03", "显示列、排序、分组配置只影响当前用户"],
    ["A04", "列表行内修改遵守角色权限和状态机"],
    ["A05", "批量操作逐项返回结果，失败原因可定位"],
    ["A06", "关注者能收到状态和评论通知，取消关注后不再接收"],
    ["A07", "导出内容与当前筛选结果和显示列一致"],
    ["A08", "访客无法通过批量接口或行内编辑修改数据"],
  ], [1200, 7826]),

  h1("8. 后续路线图"),
  h2("8.1 V1.2"),
  bullet("自定义字段和自定义问题类型。"),
  bullet("共享筛选器与共享标签。"),
  bullet("细粒度自定义角色权限。"),
  bullet("工时、Excel 导入、日报/周报/月报订阅。"),
  bullet("Webhook 可视化配置和事件重试。"),
  h2("8.2 V2"),
  bullet("里程碑、项目统计中心和可配置报表。"),
  bullet("自动化规则：触发事件、条件和执行动作。"),
  bullet("用例库、测试计划、执行记录和缺陷关联。"),
  bullet("企业微信 SSO、消息推送和对象存储。"),

  h1("9. 明确不做"),
  bullet("云真机、设备实验室及远程设备调试。"),
  bullet("兼容测试、安全扫描、性能拨测和众测资源平台。"),
  bullet("在 V1.1 内迁移 React/Vue 或重构为微服务。"),
  bullet("在 V1.1 内提供可编程自动化脚本。"),

  h1("10. 下一步"),
  bullet("评审并确认 V1.1 范围、验收标准和版本边界。"),
  bullet("基于确认后的 PRD 产出 V1.1 API 与数据库设计。"),
  bullet("按个人视图与筛选 → 列表配置 → 批量处理 → 关注通知 → 导出的顺序实施。"),
];

const doc = new Document({
  styles: {
    default: { document: { run: { font: "Arial", size: 22, color: "1E2532" } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true, run: { size: 32, bold: true, color: blue, font: "Arial" }, paragraph: { spacing: { before: 360, after: 180 }, outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true, run: { size: 27, bold: true, color: "1E2532", font: "Arial" }, paragraph: { spacing: { before: 240, after: 140 }, outlineLevel: 1 } },
      { id: "Heading3", name: "Heading 3", basedOn: "Normal", next: "Normal", quickFormat: true, run: { size: 24, bold: true, color: "344054", font: "Arial" }, paragraph: { spacing: { before: 180, after: 100 }, outlineLevel: 2 } },
    ],
  },
  numbering: { config: [{ reference: "bullets", levels: [{ level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 480, hanging: 240 } } } }] }] },
  sections: [{
    properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1200, right: 1440, bottom: 1200, left: 1440 } } },
    footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER, children: [new TextRun({ text: "研发工作台 PRD V1.1  ·  ", color: gray, size: 18 }), new TextRun({ children: [PageNumber.CURRENT], color: gray, size: 18 })] })] }) },
    children,
  }],
});

Packer.toBuffer(doc).then((buffer) => {
  fs.writeFileSync(output, buffer);
  console.log(output);
});
