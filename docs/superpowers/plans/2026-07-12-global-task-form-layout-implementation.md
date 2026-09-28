# 全局任务表单左右布局与任务详情排序 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 统一任务表单为左右字段布局，将任务描述置于详情首位，并将流程说明改为左上角入口的弹框。

**Architecture:** 保持 Spring Boot 接口和任务数据不变，仅调整静态前端。HTML 提供核心字段分组和独立的流程说明弹框；JavaScript 只负责弹框开关和详情字段输出顺序；CSS 以任务表单作用域提供复用布局和窄屏回退，避免影响其他表单。

**Tech Stack:** Vanilla JavaScript、HTML、CSS。

## Global Constraints

- 不修改后端接口、数据库、任务字段或状态流转规则。
- 桌面端字段以标签左、控件或内容右的形式显示；窄屏回退为上下布局。
- 任务描述在详情内容首位；所属项目与任务类型在新建表单中同一行并列。
- 流程说明弹框关闭后不得清空新建表单已填写内容。
- 工作区未初始化 Git；不执行提交。

---

## File Structure

- `rnd-workbench/src/main/resources/static/index.html`：重组新建工作项核心字段，新增流程说明弹框结构。
- `rnd-workbench/src/main/resources/static/script.js`：绑定流程说明开关事件，并保证详情渲染先输出描述。
- `rnd-workbench/src/main/resources/static/styles.css`：定义任务表单左右字段行、并列字段、流程说明弹框及窄屏回退。

### Task 1: 重组任务表单和流程说明结构

**Files:**
- Modify: `rnd-workbench/src/main/resources/static/index.html:212-352`

**Interfaces:**
- Consumes: 既有 `#modalBackdrop`、`#createModal`、`#newTitle`、`#newProject`、`#newType`、`#newDescriptionEditor` 的 JavaScript 查询与提交逻辑。
- Produces: `#openTaskFlowGuide`、`#taskFlowGuideBackdrop`、`#closeTaskFlowGuide`，供 Task 2 绑定事件。

- [ ] **Step 1: 修改前端结构**

将内嵌 `<details class="task-flow-guide">` 替换为标题区的按钮；保持原流程图与三条说明文案，移动至独立遮罩层中的弹框。核心字段使用一个标题行和一个双列行：标题独占，所属项目与任务类型同级；任务描述紧随核心字段之后。

```html
<div class="modal-head task-modal-head">
  <div class="task-modal-heading">
    <button type="button" class="task-flow-trigger" id="openTaskFlowGuide">任务流程说明</button>
    <h2>新建工作项</h2>
  </div>
  <button type="button" class="icon-button" id="closeModal">×</button>
</div>
<section class="create-section task-core-fields" data-create-section="core">
  <label class="task-field-row task-field-wide">…#newTitle…</label>
  <div class="task-field-pair">
    <label class="task-field-row">…#newProject…</label>
    <label class="task-field-row">…#newType…</label>
  </div>
</section>
```

将弹框追加在新建工作项的 `#modalBackdrop` 之后，使用 `hidden` 初始状态，且弹框内关闭按钮为 `type="button"`，防止触发表单提交。

- [ ] **Step 2: 静态结构检查**

Run: `rg -n 'openTaskFlowGuide|taskFlowGuideBackdrop|newTitle|newProject|newType|newDescriptionEditor' rnd-workbench/src/main/resources/static/index.html`

Expected: 每个既有表单控件 ID 均保留一次，新增流程说明的打开、遮罩和关闭 ID 各出现一次。

### Task 2: 加入无副作用的流程说明开关并调整详情顺序

**Files:**
- Modify: `rnd-workbench/src/main/resources/static/script.js:470-530,760-800`

**Interfaces:**
- Consumes: Task 1 新增的 `#openTaskFlowGuide`、`#taskFlowGuideBackdrop`、`#closeTaskFlowGuide`。
- Produces: 仅改变两个遮罩的 `hidden` class；不调用重置表单、不改 `pendingAttachments`。

- [ ] **Step 1: 将描述字段作为详情第一个输出项**

在 `renderDrawer()` 的详情分支中，先输出任务描述 section，再输出标题、项目、类型、状态和其他既有字段。保持富文本描述经过既有 `renderDescription` / `escapeHtml` 处理的安全逻辑，不更改内容来源。

```javascript
const descriptionSection = `<section class="detail-description"><h3>任务描述</h3><div>${renderDescription(item.description || "") || "<p>暂无描述</p>"}</div></section>`;
$("#drawerBody").innerHTML = `${descriptionSection}${existingDetailFields}`;
```

- [ ] **Step 2: 绑定流程说明开关**

在现有 DOM 事件绑定区增加三个监听：打开按钮移除 `hidden`；关闭按钮添加 `hidden`；点击遮罩本身（`event.target === event.currentTarget`）添加 `hidden`。不得绑定到新建弹框关闭逻辑。

```javascript
$("#openTaskFlowGuide").addEventListener("click", () => $("#taskFlowGuideBackdrop").classList.remove("hidden"));
$("#closeTaskFlowGuide").addEventListener("click", () => $("#taskFlowGuideBackdrop").classList.add("hidden"));
$("#taskFlowGuideBackdrop").addEventListener("click", (event) => {
  if (event.target === event.currentTarget) event.currentTarget.classList.add("hidden");
});
```

- [ ] **Step 3: 脚本语法检查**

Run: `node --check rnd-workbench/src/main/resources/static/script.js`

Expected: 退出码为 0。

### Task 3: 实现限定范围的左右布局与响应式回退

**Files:**
- Modify: `rnd-workbench/src/main/resources/static/styles.css:1370-1414`

**Interfaces:**
- Consumes: Task 1 中的 `.task-modal-head`、`.task-modal-heading`、`.task-flow-trigger`、`.task-core-fields`、`.task-field-row`、`.task-field-pair` 与流程弹框 class。
- Produces: 任务相关表单和详情字段的可复用桌面及窄屏布局。

- [ ] **Step 1: 添加任务字段行和并列字段样式**

将新建工作项的字段标签改为 grid 行：桌面端 `grid-template-columns: 124px minmax(0, 1fr)`，标签文字垂直居中，输入控件占满右侧。所属项目与任务类型使用两列容器；标题和描述占满可用宽度。

```css
.task-field-row { display:grid; grid-template-columns:124px minmax(0,1fr); align-items:center; gap:12px; }
.task-field-pair { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:12px; }
.task-field-row > input, .task-field-row > select { width:100%; }
```

为流程说明触发器设置与现有次级按钮一致的边框、背景和焦点态；为流程说明弹框设定 `width: min(760px, calc(100vw - 32px))`，保留现有状态图的自动换行能力。

- [ ] **Step 2: 处理任务详情字段左右布局**

仅对详情标签字段容器设置左右网格，描述 section 维持整行富文本阅读宽度。避免覆盖 `.form-grid`，以免高级筛选、批量处理等非任务弹框被意外改变。

- [ ] **Step 3: 增加窄屏回退**

在既有 `@media (max-width: 720px)` 中，将 `.task-field-row` 改为单列、`.task-field-pair` 改为单列，允许流程弹框在屏幕内滚动；不变更全局 `body` 最小宽度。

- [ ] **Step 4: 样式选择器检查**

Run: `rg -n 'task-field-row|task-field-pair|task-flow-trigger|taskFlowGuide' rnd-workbench/src/main/resources/static/{index.html,styles.css,script.js}`

Expected: HTML、CSS、JavaScript 中的新增选择器和 ID 名称一致，无孤立引用。

### Task 4: 浏览器回归验证

**Files:**
- Verify only: `rnd-workbench/src/main/resources/static/{index.html,script.js,styles.css}`

**Interfaces:**
- Consumes: Tasks 1-3 的静态前端修改。
- Produces: 桌面和窄屏的截图或可复现验证记录。

- [ ] **Step 1: 启动现有应用并登录**

Run: `cd rnd-workbench && mvn spring-boot:run`

Expected: 应用启动，静态页面可从本地端口访问；使用既有管理员账号登录并进入可新建任务的项目。

- [ ] **Step 2: 验证桌面端新建工作项**

打开“新建工作项”，确认任务标题独占一行、所属项目与任务类型并列、任务描述随后显示；输入标题和描述后打开/关闭流程说明，确认输入仍保留。

- [ ] **Step 3: 验证详情排序与窄屏回退**

创建或打开已有任务，确认详情第一个内容区为任务描述。将浏览器宽度调整到 720px 以下，确认项目与类型以及单个字段均回退为上下布局，流程说明弹框可关闭且不溢出视口。

- [ ] **Step 4: 运行静态回归检查**

Run: `node --check rnd-workbench/src/main/resources/static/script.js`

Expected: 退出码为 0；新建、关闭、详情抽屉不出现 JavaScript 控制台错误。

## Self-Review

- 设计覆盖：Task 1 覆盖流程入口、弹框、标题/项目/类型排序；Task 2 覆盖弹框交互和详情描述置顶；Task 3 覆盖桌面左右布局和窄屏回退；Task 4 覆盖交互回归。
- 范围检查：计划未涉及接口、数据库或状态机。
- 命名检查：流程说明的三个 ID 在 Task 1 与 Task 2 一致；任务表单 class 在 Task 1 与 Task 3 一致。
