# 登录页与工作台 UI 升级 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将研发工作台的登录页与首页升级为明亮协作空间风格，同时保持所有现有数据与交互行为。

**Architecture:** 仅调整静态 HTML、CSS 和登录视图模板。HTML 为工作台欢迎/协作焦点提供稳定容器；JavaScript 继续使用既有 API 与事件处理，只更改 `installLoginView()` 的静态模板；CSS 通过设计 token 和局部组件样式统一侧栏、顶栏、数据卡与登录页视觉。

**Tech Stack:** 原生 HTML、CSS、JavaScript；Spring Boot 静态资源；Maven。

## Global Constraints

- 不修改后端接口、数据结构、页面路由、元素 ID 或既有交互语义。
- 不删除或重命名 JavaScript 动态渲染依赖的 DOM ID、class 与数据属性。
- 保持现有桌面端最小宽度策略；登录页窄屏可降级为单栏，不新增移动端功能。
- 协作绿用于主操作与激活状态，浅蓝、暖黄和红色仅承担辅助状态语义。

---

### Task 1: 重构登录页的静态模板与样式

**Files:**
- Modify: `rnd-workbench/src/main/resources/static/script.js:54-66`
- Modify: `rnd-workbench/src/main/resources/static/styles.css:1-44`

**Interfaces:**
- Consumes: `handleLogin(event)`, `#loginForm`, `#loginUsername`, `#loginPassword`, `#loginError`。
- Produces: 保持不变的登录 DOM ID 和提交事件，新增仅用于样式的 `login-layout`、`login-story`、`login-form-panel` 类。

- [ ] **Step 1: 将 `installLoginView()` 的单卡模板替换为双栏模板**

```js
view.innerHTML = `<div class="login-layout">
  <section class="login-story" aria-label="产品介绍">
    <div class="login-story-brand"><span class="brand-mark">R</span><span>研发工作台</span></div>
    <div><p class="login-kicker">团队协作空间</p><h1>协作更顺畅，<br />交付更有把握</h1><p>让需求、任务与项目进展始终保持同频。</p></div>
  </section>
  <section class="login-form-panel"><form class="login-card" id="loginForm">
    <div class="login-form-brand"><span class="brand-mark">R</span><span>研发工作台</span></div>
    <div><p class="login-kicker">欢迎回来</p><h1>登录工作台</h1><p>使用团队账号继续协作。</p></div>
    <label>账号<input id="loginUsername" autocomplete="username" required value="admin@uhoo.cn"></label>
    <label>密码<input id="loginPassword" type="password" autocomplete="current-password" required></label>
    <div class="login-error" id="loginError"></div>
    <button class="primary-button" type="submit">登录工作台</button>
  </form></section>
</div>`;
```

- [ ] **Step 2: 为登录页增加明亮协作风格和窄屏降级规则**

```css
.login-view { background: linear-gradient(135deg, #f5faf7, #f5f8fd); }
.login-layout { display:grid; grid-template-columns:minmax(420px, .95fr) minmax(380px, .72fr); width:min(1080px, calc(100vw - 64px)); min-height:640px; overflow:hidden; border:1px solid var(--line); border-radius:24px; background:var(--surface); box-shadow:var(--shadow); }
.login-story { position:relative; display:grid; align-content:space-between; padding:48px; overflow:hidden; color:#174438; background:linear-gradient(135deg, #dff7ea, #d9eff3 58%, #f8edc8); }
.login-form-panel { display:grid; place-items:center; padding:40px; }
.login-card { width:min(360px, 100%); border:0; box-shadow:none; }
@media (max-width:760px) { .login-layout { display:block; width:min(440px, calc(100vw - 32px)); min-height:0; } .login-story { display:none; } .login-form-panel { padding:28px 24px; } }
```

- [ ] **Step 3: 执行 JavaScript 语法检查**

Run: `node --check rnd-workbench/src/main/resources/static/script.js`  
Expected: exit code 0，且无输出。

### Task 2: 建立协作色彩 token 并升级应用全局框架

**Files:**
- Modify: `rnd-workbench/src/main/resources/static/styles.css:1-398`

**Interfaces:**
- Consumes: 既有 `--blue`、`--green`、`--line` 等 CSS token 与 `.sidebar`、`.topbar`、`.nav-item`、`.primary-button` 类。
- Produces: 所有页面共用的暖白背景、协作绿主操作与浅绿激活态；不改变任一选择器的行为职责。

- [ ] **Step 1: 替换基础颜色 token 并保留状态 token 名称**

```css
:root {
  --bg:#f6f8f7; --surface:#ffffff; --surface-soft:#f1f7f4;
  --line:#e3ebe7; --line-dark:#d3dfd8; --text:#20332c;
  --muted:#718078; --faint:#9baaa2; --blue:#2f7dbe;
  --blue-soft:#e8f3fb; --green:#238b62; --green-soft:#e7f5ed;
  --red:#d95057; --red-soft:#fcebed; --orange:#c98a23;
  --orange-soft:#fff5de; --purple:#7567d5; --purple-soft:#efedff;
  --shadow:0 18px 48px rgba(35, 72, 54, .10);
}
```

- [ ] **Step 2: 调整侧栏、顶栏、导航与按钮的视觉层级**

```css
.sidebar { padding:22px 16px; gap:20px; background:rgba(255,255,255,.82); border-right-color:var(--line); }
.nav-item.active, .project-row.active { color:#197352; background:var(--green-soft); }
.primary-button { background:var(--green); border-color:var(--green); box-shadow:0 7px 16px rgba(35,139,98,.18); }
.topbar { height:70px; padding:0 30px; background:rgba(255,255,255,.82); border-bottom-color:var(--line); }
.search-wrap { border:0; border-radius:12px; background:var(--surface-soft); }
```

- [ ] **Step 3: 人工检查非工作台页面的共用按钮、输入框、标签和弹窗**

Run: 启动应用后依次打开工作项表格、筛选弹窗、详情抽屉和快速创建弹窗。  
Expected: 文字、边框和焦点可辨识；原有交互可用；未出现蓝色主操作残留。

### Task 3: 重排工作台首屏并增加欢迎协作焦点区

**Files:**
- Modify: `rnd-workbench/src/main/resources/static/index.html:80-147`
- Modify: `rnd-workbench/src/main/resources/static/styles.css:351-448`

**Interfaces:**
- Consumes: `#profileName`（由 `updateProfile()` 写入）、既有统计 ID（`#todoCount`、`#inboxCount`、`#progressCount`、`#verifyCount`）及既有面板容器。
- Produces: `#dashboardWelcomeName` 与静态协作焦点区；四个既有统计卡和三组既有动态列表继续由原 JavaScript 填充。

- [ ] **Step 1: 在统计卡前插入欢迎与协作焦点区，并保留原统计卡及其 ID**

```html
<section class="dashboard-hero">
  <div class="dashboard-welcome">
    <p class="eyebrow">今日协作概览</p>
    <h2>你好，<span id="dashboardWelcomeName">用户</span></h2>
    <p>把优先事项处理完，再推进团队共同目标。</p>
  </div>
  <div class="collaboration-focus">
    <span>团队协作</span><strong>项目正在稳步推进</strong><small>优先关注待我解决与临近到期事项</small>
  </div>
</section>
<div class="summary-grid" id="summaryGrid">
  <article class="summary-card"><span>我的待办</span><strong id="todoCount">8</strong><small>3 个今天到期</small></article>
  <article class="summary-card"><span>待接收</span><strong id="inboxCount">7</strong><small>2 个 P0 紧急</small></article>
  <article class="summary-card"><span>处理中</span><strong id="progressCount">0</strong><small>开发与修复中</small></article>
  <article class="summary-card"><span>待验收</span><strong id="verifyCount">0</strong><small>需要产品确认</small></article>
</div>
```

- [ ] **Step 2: 在 `updateProfile()` 中同步欢迎名称，不改变用户资料写入逻辑**

```js
const dashboardWelcomeName = $("#dashboardWelcomeName");
if (dashboardWelcomeName) dashboardWelcomeName.textContent = name;
```

- [ ] **Step 3: 添加工作台首屏布局与状态卡样式**

```css
.dashboard-hero { display:grid; grid-template-columns:1.25fr .75fr; gap:16px; margin-top:24px; }
.dashboard-welcome, .collaboration-focus { min-height:138px; padding:24px; border-radius:18px; }
.dashboard-welcome { background:var(--surface); border:1px solid var(--line); }
.collaboration-focus { display:grid; align-content:center; gap:6px; color:#173e30; background:linear-gradient(120deg, #dff6e8, #d8edf2 68%, #f7ecc4); }
.summary-card { position:relative; padding:20px; border-radius:14px; box-shadow:0 8px 22px rgba(35,72,54,.045); }
.summary-card:first-child { border-color:#cfe9da; background:#f6fcf8; }
```

- [ ] **Step 4: 检查动态数据绑定与工作台布局**

Run: 登录后切换两个项目，观察统计数字、待办、项目动态和到期列表。  
Expected: 所有计数与列表仍会更新；欢迎区显示当前用户；三列面板在现有断点下按既有规则排列。

### Task 4: 完整回归验证与交付

**Files:**
- Modify: `rnd-workbench/src/main/resources/static/index.html`
- Modify: `rnd-workbench/src/main/resources/static/styles.css`
- Modify: `rnd-workbench/src/main/resources/static/script.js`

**Interfaces:**
- Consumes: 登录模板、全局样式与工作台首屏的静态资源。
- Produces: 可打包、可运行且功能不回归的 JAR。

- [ ] **Step 1: 复核关键 DOM ID 未被删除或重复**

Run: `rg -n 'id="(loginForm|loginUsername|loginPassword|loginError|todoCount|inboxCount|progressCount|verifyCount|dashboardWelcomeName)"' rnd-workbench/src/main/resources/static/{index.html,script.js}`  
Expected: 原登录与统计 ID 各保留一个；`dashboardWelcomeName` 仅出现一次。

- [ ] **Step 2: 运行后端测试与打包**

Run: `mvn test package`  
Working directory: `rnd-workbench`  
Expected: Maven 以 exit code 0 完成，并生成 `target/rnd-workbench-1.0.0-SNAPSHOT.jar`。

- [ ] **Step 3: 启动打包产物并执行手工冒烟验证**

Run: `java -jar target/rnd-workbench-1.0.0-SNAPSHOT.jar --server.port=3002`  
Working directory: `rnd-workbench`  
Expected: 服务启动成功；浏览器中可完成登录、工作台首屏加载、侧栏收起、项目切换、搜索、快速创建、通知与退出。

- [ ] **Step 4: 记录验证结果**

在交付说明中列出实际执行的语法检查、Maven 测试/打包与浏览器冒烟检查结果；若任何检查未通过，交付时明确列出失败命令和原因。
