# 用户管理列表页 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将系统管理员的用户管理从列表弹框改为主内容区列表页面，同时保留新增、编辑、启停和重置密码。

**Architecture:** 复用项目管理页面的主内容区模式与现有 `/users` 接口；新增用户目录状态与渲染函数，用户编辑继续使用既有编辑弹框。页面访问在前端再次检查 `ADMIN`，服务端既有管理员接口校验保持不变。

**Tech Stack:** Vanilla JavaScript、CSS、Spring Boot、JUnit 5。

## Global Constraints

- 只有系统管理员可见和访问用户管理页面。
- 保留新增用户、编辑、启用/禁用、重置密码能力与现有接口。
- 不修改用户编辑弹框和后端用户接口契约。
- 工作区未初始化 Git；实施时不执行提交操作。

---

### Task 1: 建立主内容区用户目录

**Files:**
- Modify: `rnd-workbench/src/main/resources/static/index.html:399-401`
- Modify: `rnd-workbench/src/main/resources/static/script.js:238-340,521-524,596-652`
- Modify: `rnd-workbench/src/main/resources/static/styles.css:1289-1315`

**Interfaces:**
- Consumes: `GET /api/v1/users?page=1&size=100` 的 `{list}` 响应。
- Produces: `showUserDirectory()`、`renderUserManagementList()`，以及 `#userSearch`、`#userStatusFilter`、`#userManagementList` DOM 接口。

- [ ] **Step 1: 写出失败的目录页面检查**

在浏览器 DOM 检查中验证点击用户管理后具有主内容区结构，而非 `#usersModalBackdrop`：

```javascript
document.querySelector('[data-page="users"]').click();
expect(document.querySelector('#workItemsTable #userManagementList')).not.toBeNull();
expect(document.querySelector('#usersModalBackdrop')).toBeNull();
```

- [ ] **Step 2: 运行检查确认当前页面仍打开弹框**

Run: `node --check rnd-workbench/src/main/resources/static/script.js`

Expected: PASS；手工点击“用户管理”时仍显示旧的用户列表弹框。

- [ ] **Step 3: 实现用户目录加载与筛选渲染**

新增以下状态和函数：

```javascript
let userDirectoryEntries = [];

async function showUserDirectory() {
  if (!isAdmin()) return showToast("无权访问用户管理");
  showListView();
  $(".content").classList.add("project-management-mode");
  $("#pageTitle").textContent = "用户管理";
  $("#workItemsTable").innerHTML = '<p class="empty-state">正在加载用户…</p>';
  const page = await api("/users?page=1&size=100");
  userDirectoryEntries = page.list || [];
  $("#workItemsTable").innerHTML = '<section class="project-management"><div class="project-management-toolbar"><input id="userSearch" type="search" placeholder="搜索姓名或邮箱" /><select id="userStatusFilter"><option value="all">全部状态</option><option value="enabled">启用</option><option value="disabled">已禁用</option></select><button class="primary-button" id="createUserPageBtn">+ 新增用户</button></div><div id="userManagementList"></div></section>';
  renderUserManagementList();
}
```

`renderUserManagementList()` 过滤昵称、用户名和状态，输出姓名、邮箱、部门/岗位、系统权限、状态和操作六列。操作沿用 `data-edit-user`、`data-toggle-user`、`data-reset-user`，确保现有处理逻辑可以复用。

- [ ] **Step 4: 移除旧列表弹框并接入页面事件**

删除 `#usersModalBackdrop`、`openUsersModal()` 和 `loadUsers()`。将 `data-page="users"` 分支改为 `await showUserDirectory()`；在 `#workItemsTable` 的 `input`、`change`、`click` 代理中处理：

```javascript
if (event.target.id === "userSearch") renderUserManagementList();
if (event.target.id === "userStatusFilter") renderUserManagementList();
if (event.target.closest("#createUserPageBtn")) openUserEditor();
```

启停、编辑保存成功后，将原 `loadUsers()` 调用改为 `showUserDirectory()`，使列表刷新并保留主内容区上下文。

- [ ] **Step 5: 添加专用列表样式并验证返回导航**

复用 `.project-management` 容器，新增 `.user-list-head` 与 `.user-list-row` 六列网格样式；窄屏时允许操作列换行。确认 `renderAll()` 会移除 `project-management-mode`，从用户管理切回其他导航时恢复普通页面。

- [ ] **Step 6: 运行验证**

Run: `node --check rnd-workbench/src/main/resources/static/script.js`

Expected: PASS。管理员登录后验证搜索、状态筛选、新增、编辑、启停、重置密码及切回项目管理；普通用户不可见入口且直接调用页面函数只收到无权限提示。

### Task 2: 回归与交付检查

**Files:**
- Verify: `rnd-workbench/src/main/resources/static/{index.html,script.js,styles.css}`
- Verify: `rnd-workbench/src/test/java/com/rnd/app/controller/UserControllerTest.java`

- [ ] **Step 1: 运行后端回归**

Run: `cd rnd-workbench && mvn test`

Expected: PASS；用户接口权限测试继续通过。

- [ ] **Step 2: 打包交付物**

Run: `cd rnd-workbench && mvn package -DskipTests`

Expected: PASS，生成 `target/rnd-workbench-1.0.0-SNAPSHOT.jar`。

## Self-Review

- **规格覆盖：** 管理员可见性、主内容区列表、搜索、状态筛选和全部既有用户操作均在 Task 1 覆盖。
- **范围控制：** 未调整用户接口、数据模型或编辑弹框，只替换用户列表承载方式。
- **一致性：** 页面函数统一使用 `showUserDirectory()` 和 `renderUserManagementList()`；操作数据属性复用既有接口处理。
