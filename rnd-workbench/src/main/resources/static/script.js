const API_BASE = "/api/v1";
const statusFlow = ["新建", "进行中", "延期处理", "已完成", "已验收", "验收不通过", "已拒绝"];
const typeClass = { 需求: "green", 任务: "blue", 测试: "purple", 缺陷: "red" };
const themeKeys = ["green", "blue", "orange", "purple", "graphite"];
const ALL_PROJECTS = "all";

let token = sessionStorage.getItem("rndToken") || "";
let loginProviders = [];
let enterpriseLoginAvailable = false;
let externalAuthState = "";
let currentUser = null;
let projects = [];
let taskTypes = [];
let projectTypes = [];
let members = [];
let workItems = [];
let activities = [];
let dueItems = [];
let currentProjectId = null;
let selectedId = null;
let activeType = "all";
let boardMode = false;
let drawerTab = "details";
let pendingAttachments = [];
let currentView = null;
let advancedFilters = {};
let currentSort = "createdAt,desc";
let taskPage = 1;
let taskPageSize = Number(localStorage.getItem("rndTaskPageSize") || "20");
let taskTotal = 0;
let savedFilters = [];
let activeSavedFilterId = null;
let savedFiltersProjectId = null;
let viewPreference = { columns: ["type", "status", "owner", "priority", "sprint", "dueDate"], sort: "createdAt,desc" };
let collapsedGroups = new Set();
let selectedIds = new Set();
let managingProjectId = null;
let memberCandidates = [];
let editingItemId = null;
let createModules = [];
let createSprints = [];
let createRelatedItems = [];
let projectDirectoryEntries = [];
let projectDirectoryLoadId = 0;
let userDirectoryEntries = [];
let projectManagementPage = 1;
let userManagementPage = 1;
let managementPageSize = Number(localStorage.getItem("rndManagementPageSize") || "20");
let projectLoadId = 0;
let activePage = "dashboard";
let activeProfileProjectId = null;
let activeMilestones = [];
let zoneDirectoryEntries = [];
let zoneViewMode = localStorage.getItem("rndZoneViewMode") === "card" ? "card" : "list";
let zoneFilters = { keyword: "", managerId: "", phase: "", status: "", health: "", endFrom: "", endTo: "" };
let imagePreviewObjectUrl = null;
let imagePreviewScale = 1;
let imagePreviewDrag = null;
const zoneDefinitions = {
  A: { title: "硬仗清单", tone: "red" },
  B: { title: "调优策略", tone: "blue" },
  C: { title: "部门级重点工作", tone: "green" },
  D: { title: "日常周期性工作", tone: "amber" },
};
let taskCounts = { all: 0, unclosed: 0, "created-by-me": 0, "assigned-to-me": 0, "pending-for-me": 0 };
let currentSummary = { todoCount: 0, inboxCount: 0, inProgressCount: 0, toBeVerifiedCount: 0 };
const columnDefinitions = {
  project: ["项目", (i) => escapeHtml(projectName(i.projectId))],
  type: ["类型", (i) => chip(i.type, typeClass[i.type])], status: ["状态", (i) => quickField(i, "status")], owner: ["负责人", (i) => quickField(i, "owner")],
  priority: ["优先级", (i) => quickField(i, "priority")], sprint: ["迭代", (i) => escapeHtml(i.sprintId || "-")],
  module: ["模块", (i) => escapeHtml(i.module || "-")], creator: ["创建人", (i) => escapeHtml(i.creatorName || "-")],
  plannedStartDate: ["计划开始日期", (i) => dateValue(i.plannedStartDate) || "-"], actualCompletedAt: ["实际完成时间", (i) => formatDate(i.actualCompletedAt)],
  actualHours: ["实际工时", (i) => i.actualHours == null ? "-" : `${i.actualHours} 小时`],
  dueDate: ["到期日", (i) => formatDate(i.dueDate)], createdAt: ["创建时间", (i) => formatDate(i.createdAt)], updatedAt: ["更新时间", (i) => formatDate(i.updatedAt)]
};

const $ = (selector) => document.querySelector(selector);
const $all = (selector) => [...document.querySelectorAll(selector)];
const escapeHtml = (value) => String(value ?? "")
  .replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;")
  .replaceAll('"', "&quot;").replaceAll("'", "&#39;");

async function api(path, options = {}) {
  const headers = new Headers(options.headers || {});
  if (token) headers.set("Authorization", `Bearer ${token}`);
  if (options.body && !(options.body instanceof FormData)) headers.set("Content-Type", "application/json");
  const response = await fetch(`${API_BASE}${path}`, { ...options, headers });
  if (response.status === 401) {
    logout();
    throw new Error("登录已失效，请重新登录");
  }
  const payload = await response.json().catch(() => ({ code: response.status, message: "服务器响应格式错误" }));
  if (!response.ok || payload.code !== 0) throw new Error(payload.message || `请求失败 (${response.status})`);
  return payload.data;
}

function installLoginView() {
  const view = document.createElement("div");
  view.id = "loginView";
  view.className = "login-view hidden";
  view.innerHTML = `<div class="login-layout">
    <section class="login-story" aria-label="IssueLoop 问题闭环管理系统概览">
      <div class="login-brandline"><div class="brand-mark">I</div><span class="login-brand-text">IssueLoop<small>问题闭环管理系统</small></span></div>
      <div class="login-story-copy">
        <p class="eyebrow">问题与需求 · 全程闭环</p>
        <h1>让每个问题有结果，<br>让每项需求有着落。</h1>
        <p>从提出到验收关闭，连接项目成员，明确处理责任，持续跟进问题与需求。</p>
      </div>
      <div class="login-preview" aria-hidden="true">
        <div class="preview-card wide"><span>今日待办</span><strong>8</strong><small>3 项今天到期</small></div>
        <div class="preview-card"><span>进行中</span><strong>12</strong><small>处理与协作</small></div>
        <div class="preview-card accent"><span>待验收</span><strong>5</strong><small>等待验收确认</small></div>
      </div>
    </section>
    <section class="login-form-panel">
      <form class="login-card" id="loginForm">
        <div class="login-form-brand"><div class="brand-mark">I</div><span class="login-brand-text">IssueLoop<small>问题闭环管理系统</small></span></div>
        <div>
          <p class="eyebrow">欢迎回来</p>
          <h1>登录 IssueLoop</h1>
          <p>使用团队账号，继续跟进问题与需求。</p>
        </div>
        <button type="button" class="primary-button login-enterprise-button hidden" id="enterpriseLoginBtn">企业统一认证登录</button>
        <div id="thirdPartyLoginButtons" class="third-party-login-buttons" aria-label="第三方协同 APP 登录入口"></div>
        <div class="login-divider"><span>或使用账号密码</span></div>
        <div id="externalBindingPanel" class="external-binding-panel hidden">
          <p class="eyebrow">首次使用</p><strong id="externalBindingTitle">关联已有工作台账号</strong>
          <p>认证已成功，请使用已有账号完成一次关联。</p>
          <label>账号<input id="externalBindingUsername" autocomplete="username"></label>
          <label>密码<input id="externalBindingPassword" type="password" autocomplete="current-password"></label>
          <button type="button" class="secondary-button" id="externalBindingBtn">验证并关联</button>
        </div>
        <label>账号<input id="loginUsername" autocomplete="username" required value="admin@uhoo.cn"></label>
        <label>密码<input id="loginPassword" type="password" autocomplete="current-password" required></label>
        <div class="login-error" id="loginError"></div>
        <button class="primary-button" type="submit">登录</button>
      </form>
    </section>
  </div>`;
  document.body.append(view);
  $("#loginForm").addEventListener("submit", handleLogin);
  $("#enterpriseLoginBtn").addEventListener("click", () => { window.location.assign(`${API_BASE}/auth/keycloak/start`); });
  $("#externalBindingBtn").addEventListener("click", bindExternalIdentity);
  loadLoginProviders();
}

const loginProviderNames = { feishu: "飞书登录", dingtalk: "钉钉登录", wecom: "企微登录" };

async function loadLoginProviders() {
  try {
    const config = await api("/auth/login-providers");
    enterpriseLoginAvailable = config?.enterprise === true;
    loginProviders = Array.isArray(config?.providers) ? config.providers : [];
    renderLoginProviders();
  } catch (error) {
    enterpriseLoginAvailable = false;
    loginProviders = [];
    renderLoginProviders();
  }
}

function renderLoginProviders() {
  $("#enterpriseLoginBtn")?.classList.toggle("hidden", !enterpriseLoginAvailable);
  const container = $("#thirdPartyLoginButtons");
  if (!container) return;
  container.innerHTML = loginProviders.filter((provider) => loginProviderNames[provider]).map((provider) =>
    `<button type="button" class="secondary-button login-provider-button" data-login-provider="${provider}">${loginProviderNames[provider]}</button>`
  ).join("");
  $all("[data-login-provider]").forEach((button) => button.addEventListener("click", () => {
    window.location.assign(`${API_BASE}/auth/${button.dataset.loginProvider}/start`);
  }));
}

async function completeExternalAuth() {
  const params = new URLSearchParams(window.location.search);
  const state = params.get("auth_state");
  if (!state) return false;
  window.history.replaceState({}, document.title, window.location.pathname);
  try {
    const result = await api("/auth/exchange", { method: "POST", body: JSON.stringify({ state }) });
    if (result.bindingRequired) {
      externalAuthState = result.bindingToken;
      $("#externalBindingPanel").classList.remove("hidden");
      $("#externalBindingTitle").textContent = `${result.displayName || "外部身份"}，关联已有工作台账号`;
      $("#loginError").textContent = "认证成功，请完成一次账号关联。";
      return true;
    }
    token = result.token; sessionStorage.setItem("rndToken", token); await initializeApp(); return true;
  } catch (error) { showLogin(error.message); return true; }
}

async function bindExternalIdentity() {
  try {
    const result = await api("/auth/bind", { method: "POST", body: JSON.stringify({ state: externalAuthState, username: $("#externalBindingUsername").value.trim(), password: $("#externalBindingPassword").value }) });
    token = result.token; sessionStorage.setItem("rndToken", token); await initializeApp();
  } catch (error) { $("#loginError").textContent = error.message; }
}

function showLogin(message = "") {
  $(".app-shell").classList.add("hidden");
  $("#loginView").classList.remove("hidden");
  externalAuthState = "";
  $("#externalBindingPanel")?.classList.add("hidden");
  if ($("#externalBindingUsername")) $("#externalBindingUsername").value = "";
  if ($("#externalBindingPassword")) $("#externalBindingPassword").value = "";
  $("#loginError").textContent = message;
}

function showApp() {
  $("#loginView").classList.add("hidden");
  $(".app-shell").classList.remove("hidden");
}

async function handleLogin(event) {
  event.preventDefault();
  $("#loginError").textContent = "";
  try {
    const result = await api("/auth/login", { method: "POST", body: JSON.stringify({
      username: $("#loginUsername").value.trim(), password: $("#loginPassword").value,
    }) });
    token = result.token;
    sessionStorage.setItem("rndToken", token);
    await initializeApp();
  } catch (error) {
    showLogin(error.message);
  }
}

function logout() {
  insightRequest += 1; insightData = null; insightState = {}; insightCountScope = null;
  projectLoadId += 1; projectDirectoryLoadId += 1;
  projects = []; members = []; workItems = []; activities = []; dueItems = []; savedFilters = [];
  selectedId = null; currentProjectId = null;
  taskCounts = taskCountsFromItems([]);
  renderTaskNavigation();
  $("#insightsRoot").replaceChildren();
  $("#detailDrawer").classList.remove("open");
  $("#drawerBody").replaceChildren();
  $("#actionBar").replaceChildren();
  $("#workItemsTable").replaceChildren();
  token = "";
  currentUser = null;
  sessionStorage.removeItem("rndToken");
  showLogin();
}

async function initializeApp() {
  try {
    currentUser = await api("/auth/me");
    if (currentUser.mustChangePassword) {
      updateProfile();
      showApp();
      openAccountModal(true);
      return;
    }
    [projects, taskTypes, projectTypes] = await Promise.all([api("/projects"), api("/task-types"), api("/project-types")]);
    const savedSelection = localStorage.getItem(projectSelectionKey());
    currentProjectId = savedSelection === ALL_PROJECTS || projects.some((project) => String(project.id) === savedSelection)
      ? (savedSelection === ALL_PROJECTS ? ALL_PROJECTS : Number(savedSelection))
      : (projects[0]?.id || null);
    workItems = []; members = []; selectedId = null;
    taskCounts = taskCountsFromItems([]); renderTaskNavigation();
    updateProfile();
    $("#userManagementBtn").classList.toggle("hidden", !isAdmin());
    $("#systemSettingsBtn").classList.toggle("hidden", !isAdmin());
    $("#basicDataNav").classList.toggle("hidden", !isAdmin());
    showApp();
    insightCountScope = null;
    await showInsights("dashboard", true);
    await loadUnreadCount();
  } catch (error) {
    if (token) showToast(error.message);
    else showLogin(error.message);
  }
}

async function loadProject(projectId) {
  const loadId = ++projectLoadId;
  projectDirectoryLoadId += 1;
  const nextProjectId = Number(projectId);
  rememberProject(nextProjectId);
  localStorage.setItem(projectSelectionKey(), String(nextProjectId));
  if (savedFiltersProjectId !== nextProjectId) {
    collapsedGroups = new Set();
    currentProjectId = nextProjectId;
    savedFilters = await api(`/projects/${nextProjectId}/saved-filters`);
    viewPreference = await api(`/projects/${nextProjectId}/view-preference`);
    viewPreference.columns = (viewPreference.columns || []).filter((key) => key !== "severity" && columnDefinitions[key]);
    currentSort = viewPreference.sort;
    savedFiltersProjectId = nextProjectId;
    const defaultFilter = savedFilters.find((item) => item.defaultFilter);
    applySavedFilterState(defaultFilter || null);
  } else currentProjectId = nextProjectId;
  const query = buildWorkItemQuery();
  const [summary, page, projectMembers, projectActivities, projectDue, nextTaskCounts] = await Promise.all([
    api(`/projects/${currentProjectId}/summary`),
    api(`/projects/${currentProjectId}/work-items?${query}`),
    api(`/projects/${currentProjectId}/members`),
    api(`/projects/${currentProjectId}/activities`),
    api(`/projects/${currentProjectId}/due-items`),
    loadTaskCounts(currentProjectId),
  ]);
  if (loadId !== projectLoadId) return;
  taskPage = Number(page.page || taskPage);
  taskPageSize = Number(page.size || taskPageSize);
  taskTotal = Number(page.total || 0);
  if (!page.list?.length && taskTotal > 0 && taskPage > 1) {
    taskPage = Math.max(1, Math.ceil(taskTotal / taskPageSize));
    return loadProject(currentProjectId);
  }
  workItems = page.list || [];
  selectedIds = new Set([...selectedIds].filter((id) => workItems.some((item) => item.id === id)));
  members = projectMembers || [];
  activities = projectActivities.content || projectActivities.list || projectActivities || [];
  dueItems = projectDue || [];
  taskCounts = nextTaskCounts;
  currentSummary = summary;
  selectedId = workItems.some((item) => item.id === selectedId) ? selectedId : workItems[0]?.id || null;
  renderProjectNavigation();
  renderSummary(summary);
  renderAll();
}

async function loadAllProjects() {
  const loadId = ++projectLoadId;
  projectDirectoryLoadId += 1;
  currentProjectId = ALL_PROJECTS;
  localStorage.setItem(projectSelectionKey(), ALL_PROJECTS);
  const filteredProjectId = Number(advancedFilters.projectId) || null;
  const scopedProjects = filteredProjectId ? projects.filter((project) => project.id === filteredProjectId) : projects;
  savedFilters = [];
  savedFiltersProjectId = null;
  viewPreference = { columns: ["project", "type", "status", "owner", "priority", "sprint", "dueDate"], sort: currentSort, groupBy: null };
  if (!scopedProjects.length) {
    workItems = []; members = []; activities = []; dueItems = []; taskCounts = taskCountsFromItems([]); taskTotal = 0;
    currentSummary = { todoCount: 0, inboxCount: 0, inProgressCount: 0, toBeVerifiedCount: 0 };
    renderProjectNavigation(); renderAll();
    return;
  }
  const query = buildWorkItemQuery({ omitProject: true, paginate: false });
  const loaded = await Promise.all(scopedProjects.map(async (project) => {
    const [summary, page, projectMembers, projectActivities, projectDue, counts] = await Promise.all([
      api(`/projects/${project.id}/summary`), loadCompleteProjectItems(project.id, query),
      api(`/projects/${project.id}/members`), api(`/projects/${project.id}/activities`), api(`/projects/${project.id}/due-items`), loadTaskCounts(project.id),
    ]);
    return { summary, page, projectMembers, projectActivities, projectDue, counts };
  }));
  if (loadId !== projectLoadId) return;
  const allProjectItems = loaded.flatMap((item) => item.page.list || []);
  taskTotal = allProjectItems.length;
  if (taskTotal > 0 && taskPage > Math.ceil(taskTotal / taskPageSize)) {
    taskPage = Math.max(1, Math.ceil(taskTotal / taskPageSize));
    return loadAllProjects();
  }
  workItems = allProjectItems.slice((taskPage - 1) * taskPageSize, taskPage * taskPageSize);
  members = [...new Map(loaded.flatMap((item) => item.projectMembers || []).map((member) => [member.userId, member])).values()];
  activities = loaded.flatMap((item) => item.projectActivities.content || item.projectActivities.list || item.projectActivities || [])
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
  dueItems = loaded.flatMap((item) => item.projectDue || []).sort((a, b) => new Date(a.dueDate) - new Date(b.dueDate));
  taskCounts = loaded.reduce((total, item) => { Object.keys(total).forEach(key => { total[key] += item.counts[key] || 0; }); return total; }, taskCountsFromItems([]));
  currentSummary = loaded.reduce((total, item) => ({
    todoCount: total.todoCount + Number(item.summary.todoCount || 0), inboxCount: total.inboxCount + Number(item.summary.inboxCount || 0),
    inProgressCount: total.inProgressCount + Number(item.summary.inProgressCount || 0), toBeVerifiedCount: total.toBeVerifiedCount + Number(item.summary.toBeVerifiedCount || 0),
  }), { todoCount: 0, inboxCount: 0, inProgressCount: 0, toBeVerifiedCount: 0 });
  selectedIds.clear(); selectedId = workItems.some((item) => item.id === selectedId) ? selectedId : workItems[0]?.id || null;
  renderProjectNavigation(); renderAll();
}

async function loadCurrentSelection() {
  if (isInsightsPage()) { insightCountScope = null; await showInsights(); return; }
  if (isAllProjects()) return loadAllProjects();
  return loadProject(currentProjectId);
}

function taskCountsFromItems(items) {
  const mine = (item) => item.ownerId === currentUser?.userId && !["已验收", "已拒绝"].includes(item.status);
  return { all: items.length, unclosed: items.filter((item) => !["已验收", "已拒绝"].includes(item.status)).length,
    "created-by-me": items.filter((item) => item.creatorId === currentUser?.userId).length,
    "assigned-to-me": items.filter(mine).length, "pending-for-me": items.filter(mine).length };
}

async function loadTaskCounts(projectId) {
  const views = ["all", "unclosed", "created-by-me", "assigned-to-me", "pending-for-me"];
  const pages = await Promise.all(views.map((view) => {
    const params = new URLSearchParams({ page: "1", size: "1", sort: currentSort });
    if (view !== "all") params.set("view", view);
    return api(`/projects/${projectId}/work-items?${params}`);
  }));
  return Object.fromEntries(views.map((view, index) => [view, Number(pages[index].total || 0)]));
}

async function selectProject(projectId) {
  taskPage = 1;
  if (projectId === ALL_PROJECTS) { await loadAllProjects(); return; }
  const nextProjectId = Number(projectId);
  if (!projects.some((project) => project.id === nextProjectId)) {
    projects = await api("/projects");
    renderProjectNavigation();
  }
  if (!projects.some((project) => project.id === nextProjectId)) {
    showToast("你不是该项目成员，请先在“项目管理 → 设置团队”中添加自己");
    return;
  }
  await loadProject(nextProjectId);
}

function updateProfile() {
  const name = currentUser?.nickname || currentUser?.username || "用户";
  $("#profileName").textContent = name;
  $("#profileAvatar").textContent = name.slice(0, 1);
  $("#profileRole").textContent = currentUser?.systemRole === "ADMIN" ? "系统管理员" : "项目成员";
  const dashboardName = $("#dashboardWelcomeName");
  if (dashboardName) dashboardName.textContent = name;
}

function renderProjectNavigation() {
  const recentIds = JSON.parse(localStorage.getItem("rndRecentProjects") || "[]");
  const ordered = [...projects].sort((a, b) => {
    const ai = recentIds.indexOf(a.id), bi = recentIds.indexOf(b.id);
    return (ai < 0 ? Number.MAX_SAFE_INTEGER : ai) - (bi < 0 ? Number.MAX_SAFE_INTEGER : bi);
  });
  const current = currentProject();
  $("#projectSwitch").disabled = !ordered.length;
  $("#projectSwitchLabel").textContent = isAllProjects() ? "全部项目" : (current?.name || "选择项目");
  $("#projectSwitchDot").textContent = isAllProjects() ? "全" : (current?.shortName?.slice(0, 1) || "项");
  renderProjectSwitchOptions(ordered);
  $("#newProject").innerHTML = `<option value="">请选择项目</option>${projects.map((project) => `<option value="${project.id}">${escapeHtml(project.name)}</option>`).join("")}`;
  $("#newType").innerHTML = taskTypes.map((type) => `<option value="${escapeHtml(type.name)}">${escapeHtml(type.name)}</option>`).join("");
  $("#newOwner").innerHTML = `<option value="">未分配</option>${members.map((member) => `<option value="${member.userId}">${escapeHtml(member.nickname || member.username)}</option>`).join("")}`;
  renderSavedFilters();
}

function orderedProjects() {
  const recentIds = JSON.parse(localStorage.getItem("rndRecentProjects") || "[]");
  return [...projects].sort((a, b) => {
    const ai = recentIds.indexOf(a.id), bi = recentIds.indexOf(b.id);
    return (ai < 0 ? Number.MAX_SAFE_INTEGER : ai) - (bi < 0 ? Number.MAX_SAFE_INTEGER : bi);
  });
}

function renderProjectSwitchOptions(source = orderedProjects()) {
  const keyword = $("#projectSwitchSearch")?.value.trim().toLowerCase() || "";
  const matches = source.filter((project) => `${project.code || ""} ${project.name || ""} ${project.shortName || ""}`.toLowerCase().includes(keyword));
  const allOption = `<button type="button" role="option" data-switch-project="${ALL_PROJECTS}" aria-selected="${isAllProjects()}"><strong>全部项目</strong><small>权限范围内的全部项目</small></button>`;
  $("#projectSwitchOptions").innerHTML = allOption + (matches.map((project) => `<button type="button" role="option" data-switch-project="${project.id}" aria-selected="${project.id === currentProjectId}"><strong>${escapeHtml(project.name)}</strong><small>${escapeHtml(project.code || project.shortName || "")}</small></button>`).join("") || '<p class="empty-state">没有匹配的项目</p>');
}

function projectSelectionKey() { return `rndProjectSelection:${currentUser?.userId || "anonymous"}`; }
function isAllProjects() { return currentProjectId === ALL_PROJECTS; }

function rememberProject(projectId) {
  const ids = JSON.parse(localStorage.getItem("rndRecentProjects") || "[]").filter((id) => Number(id) !== Number(projectId));
  localStorage.setItem("rndRecentProjects", JSON.stringify([Number(projectId), ...ids].slice(0, 20)));
}

function showListView() {
  projectLoadId += 1;
  boardMode = false;
  $(".content").classList.add("list-mode");
  $("#listView").classList.remove("hidden");
  $("#boardView").classList.add("hidden");
}

function syncPageNavigation() {
  $(".app-shell").classList.remove("insight-menu-open");
  $("#toggleSidebar").setAttribute("aria-expanded", "false");
  if (!isInsightsPage()) { insightRequest += 1; $(".content").classList.remove("insights-mode"); }
  $all("[data-page]").forEach((node) => node.classList.toggle("active", node.dataset.page === activePage));
}

async function showProjectDirectory() {
  activePage = "project-management";
  projectManagementPage = 1;
  syncPageNavigation();
  hideWorkItemsPagination();
  const loadId = ++projectDirectoryLoadId;
  showListView();
  const content = $(".content");
  content.classList.remove("task-management-mode", "project-brief-mode", "zone-dashboard-mode");
  content.classList.add("project-management-mode");
  $("#pageTitle").textContent = "项目管理";
  $("#workItemsTable").innerHTML = '<p class="empty-state">正在加载项目…</p>';
  try {
    const visible = await api("/projects");
    const allProjects = isAdmin() ? await api("/projects/manage") : visible;
    const roleByProjectId = new Map(visible.map((project) => [project.id, project.currentUserProjectRole]));
    projectDirectoryEntries = await Promise.all(allProjects.map(async (project) => ({
      project: { ...project, currentUserProjectRole: project.currentUserProjectRole || roleByProjectId.get(project.id) || null },
      members: await api(`/projects/${project.id}/members`)
    })));
    if (loadId !== projectDirectoryLoadId) return;
    showListView();
    $("#listView").style.setProperty("display", "block", "important");
    $("#workItemsTable").innerHTML = `<section class="project-management"><div class="project-management-toolbar"><input id="projectSearch" type="search" placeholder="搜索项目名称或简称" /><select id="projectStatusFilter"><option value="active">进行中</option><option value="archived">已归档</option><option value="all">全部</option></select><select id="projectScopeFilter"><option value="all">我参与</option><option value="managed">我负责</option></select>${isAdmin() ? '<button class="primary-button" data-create-project>+ 新增项目</button>' : ""}</div><div id="projectManagementList"></div><div id="projectManagementPagination" class="management-pagination"></div></section>`;
    renderProjectManagementList();
  } catch (error) {
    if (loadId !== projectDirectoryLoadId) return;
    $("#workItemsTable").innerHTML = `<div class="empty-state"><p>${escapeHtml(error.message || "项目列表加载失败")}</p><button class="secondary-button" data-retry-project-management>重试</button></div>`;
  }
}

function zoneManager(entry) {
  return entry.members.find((member) => Number(member.userId) === Number(entry.project.projectManagerId));
}

function filteredZoneEntries(zone) {
  const keyword = zoneFilters.keyword.trim().toLowerCase();
  return zoneDirectoryEntries.filter(({ project }) => {
    if (project.workZone !== zone) return false;
    if (keyword && !String(project.name || "").toLowerCase().includes(keyword)) return false;
    if (zoneFilters.managerId && Number(project.projectManagerId) !== Number(zoneFilters.managerId)) return false;
    if (zoneFilters.phase && project.phase !== zoneFilters.phase) return false;
    if (zoneFilters.status && project.projectStatus !== zoneFilters.status) return false;
    if (zoneFilters.health && project.healthStatus !== zoneFilters.health) return false;
    if (zoneFilters.endFrom && (!project.plannedEndDate || project.plannedEndDate < zoneFilters.endFrom)) return false;
    if (zoneFilters.endTo && (!project.plannedEndDate || project.plannedEndDate > zoneFilters.endTo)) return false;
    return true;
  }).sort((a, b) => {
    const aDate = a.project.plannedEndDate || "9999-12-31";
    const bDate = b.project.plannedEndDate || "9999-12-31";
    return aDate.localeCompare(bDate) || String(a.project.name).localeCompare(String(b.project.name), "zh-CN");
  });
}

function zoneProjectMarkup(entry) {
  const { project } = entry;
  const manager = zoneManager(entry);
  const managerName = manager?.nickname || manager?.username || "未设置";
  return `<button type="button" class="zone-project ${zoneViewMode}" data-project-profile="${project.id}">
    <strong>${escapeHtml(project.name)}</strong>
    <span><b>项目经理</b>${escapeHtml(managerName)}</span>
    <span><b>当前阶段</b>${escapeHtml(project.phase || "立项")}</span>
    <span><b>项目状态</b>${escapeHtml(project.projectStatus || "未启动")}</span>
    <span><b>健康状态</b>${escapeHtml(project.healthStatus || "正常")}</span>
    <span><b>计划结束</b>${dateValue(project.plannedEndDate) || "未设置"}</span>
  </button>`;
}

function renderZonePanels() {
  const grid = $("#zoneGrid");
  if (!grid) return;
  grid.innerHTML = Object.entries(zoneDefinitions).map(([zone, definition]) => {
    const entries = filteredZoneEntries(zone);
    const hasProjects = zoneDirectoryEntries.some((entry) => entry.project.workZone === zone);
    return `<section class="zone-panel zone-${definition.tone}">
      <header><span class="zone-letter">${zone}</span><div><h2>${definition.title}</h2><p>${entries.length} 个项目</p></div></header>
      <div class="zone-projects ${zoneViewMode}">${entries.map(zoneProjectMarkup).join("") || `<p class="zone-empty">${hasProjects ? "没有符合条件的项目" : "当前没有项目"}</p>`}</div>
    </section>`;
  }).join("");
}

function zoneOptions(values, selected, placeholder) {
  return `<option value="">${placeholder}</option>${values.map((value) => `<option value="${value}" ${selected === value ? "selected" : ""}>${value}</option>`).join("")}`;
}

function renderZoneDashboard() {
  const managers = new Map();
  zoneDirectoryEntries.forEach((entry) => {
    const manager = zoneManager(entry);
    if (manager) managers.set(String(manager.userId), manager.nickname || manager.username);
  });
  const managerOptions = [...managers.entries()].sort((a, b) => a[1].localeCompare(b[1], "zh-CN"))
    .map(([id, name]) => `<option value="${id}" ${zoneFilters.managerId === id ? "selected" : ""}>${escapeHtml(name)}</option>`).join("");
  $("#workItemsTable").innerHTML = `<section class="zone-dashboard">
    <div class="zone-filterbar" aria-label="分区看板筛选">
      <label class="zone-search"><span>项目名称</span><input id="zoneKeyword" type="search" placeholder="搜索项目名称" value="${escapeHtml(zoneFilters.keyword)}" /></label>
      <label><span>项目经理</span><select id="zoneManager"><option value="">全部经理</option>${managerOptions}</select></label>
      <label><span>当前阶段</span><select id="zonePhase">${zoneOptions(["立项","实施","开发","测试","上线","验收","运维"], zoneFilters.phase, "全部阶段")}</select></label>
      <label><span>项目状态</span><select id="zoneStatus">${zoneOptions(["未启动","进行中","已暂停","已完成","已关闭"], zoneFilters.status, "全部状态")}</select></label>
      <label><span>健康状态</span><select id="zoneHealth">${zoneOptions(["正常","关注","风险"], zoneFilters.health, "全部健康状态")}</select></label>
      <label><span>计划结束从</span><input id="zoneEndFrom" type="date" value="${zoneFilters.endFrom}" /></label>
      <label><span>计划结束至</span><input id="zoneEndTo" type="date" value="${zoneFilters.endTo}" /></label>
      <div class="zone-filter-actions"><button type="button" class="secondary-button" data-reset-zone-filters>重置</button><button type="button" class="secondary-button zone-view-toggle" data-toggle-zone-view>${zoneViewMode === "list" ? "卡片视图" : "列表视图"}</button></div>
    </div>
    <div class="zone-grid" id="zoneGrid"></div>
  </section>`;
  renderZonePanels();
}

async function showZoneDashboard() {
  activePage = "zone-dashboard";
  syncPageNavigation();
  hideWorkItemsPagination();
  showListView();
  const content = $(".content");
  content.classList.remove("task-management-mode", "project-brief-mode", "zone-dashboard-mode");
  content.classList.add("project-management-mode", "zone-dashboard-mode");
  $("#pageTitle").textContent = "ABCD 分区看板";
  $("#currentContextLine").textContent = "项目工作分区";
  $("#workItemsTable").innerHTML = '<p class="empty-state">正在加载分区看板…</p>';
  try {
    const visible = await api("/projects");
    const source = isAdmin() ? await api("/projects/manage") : visible;
    const active = source.filter((project) => !project.archived && ["A", "B", "C", "D"].includes(project.workZone) && (isAllProjects() || project.id === currentProjectId));
    zoneDirectoryEntries = await Promise.all(active.map(async (project) => ({ project, members: await api(`/projects/${project.id}/members`) })));
    renderZoneDashboard();
  } catch (error) {
    $("#workItemsTable").innerHTML = `<div class="empty-state"><p>${escapeHtml(error.message || "分区看板加载失败")}</p><button class="secondary-button" data-retry-zone-dashboard>重新加载</button></div>`;
  }
}

async function showUserDirectory() {
  if (!isAdmin()) return showToast("无权访问用户管理");
  activePage = "users";
  userManagementPage = 1;
  syncPageNavigation();
  hideWorkItemsPagination();
  showListView();
  const content = $(".content");
  content.classList.remove("task-management-mode", "project-brief-mode", "zone-dashboard-mode");
  content.classList.add("project-management-mode");
  $("#pageTitle").textContent = "用户管理";
  $("#workItemsTable").innerHTML = '<p class="empty-state">正在加载用户…</p>';
  try {
    const page = await api("/users?page=1&size=200");
    userDirectoryEntries = page.list || [];
    $("#workItemsTable").innerHTML = '<section class="project-management"><div class="project-management-toolbar"><input id="userSearch" type="search" placeholder="搜索姓名或邮箱" /><select id="userStatusFilter"><option value="all">全部状态</option><option value="enabled">启用</option><option value="disabled">已禁用</option></select><button class="primary-button" id="createUserPageBtn">+ 新增用户</button></div><div id="userManagementList"></div><div id="userManagementPagination" class="management-pagination"></div></section>';
    renderUserManagementList();
  } catch (error) {
    $("#workItemsTable").innerHTML = `<div class="empty-state"><p>${escapeHtml(error.message || "用户列表加载失败")}</p><button class="secondary-button" data-retry-user-management>重试</button></div>`;
  }
}

const projectSelectOptions = (members, selected) => `<option value="">未设置</option>${members.map((m) => `<option value="${m.userId}" ${Number(selected) === Number(m.userId) ? "selected" : ""}>${escapeHtml(m.nickname || m.username)}</option>`).join("")}`;
const projectTypeOptions = (selected) => {
  const types = projectTypes.filter((type) => type.enabled || type.name === selected);
  return types.map((type) => `<option ${type.name === selected ? "selected" : ""} ${type.enabled ? "" : "disabled"}>${escapeHtml(type.name)}</option>`).join("");
};
const dateValue = (value) => value ? String(value).slice(0, 10) : "";
const fieldValue = (id) => $(id)?.value ?? "";
const nullableNumber = (id) => fieldValue(id) ? Number(fieldValue(id)) : 0;

function openMilestoneModal(milestone = null) {
  $("#milestoneModal").reset();
  $("#milestoneId").value = milestone?.id || "";
  $("#milestoneModalTitle").textContent = milestone ? "编辑里程碑" : "新增里程碑";
  $("#milestoneName").value = milestone?.name || "";
  $("#milestonePlannedDate").value = dateValue(milestone?.plannedDate);
  $("#milestoneCompletedDate").value = dateValue(milestone?.completedDate);
  $("#milestoneStatus").value = milestone?.status || "未开始";
  $("#milestoneDescription").value = milestone?.description || "";
  $("#milestoneModalBackdrop").classList.remove("hidden");
}

async function saveMilestone(event) {
  event.preventDefault();
  const id = $("#milestoneId").value;
  const body = { name: $("#milestoneName").value.trim(), plannedDate: $("#milestonePlannedDate").value, completedDate: $("#milestoneCompletedDate").value || null, status: $("#milestoneStatus").value, description: $("#milestoneDescription").value.trim() };
  try {
    await api(id ? `/milestones/${id}` : `/projects/${activeProfileProjectId}/milestones`, { method: id ? "PUT" : "POST", body: JSON.stringify(body) });
    $("#milestoneModalBackdrop").classList.add("hidden");
    await showProjectProfile(activeProfileProjectId);
    showToast(id ? "里程碑已更新" : "里程碑已新增");
  } catch (error) { showToast(error.message); }
}

async function showProjectProfile(projectId) {
  activePage = "project-profile";
  syncPageNavigation();
  hideWorkItemsPagination();
  activeProfileProjectId = Number(projectId);
  showListView();
  const content = $(".content");
  content.classList.remove("task-management-mode", "project-brief-mode", "zone-dashboard-mode");
  content.classList.add("project-management-mode");
  $("#pageTitle").textContent = "项目档案";
  $("#workItemsTable").innerHTML = '<p class="empty-state">正在加载项目档案…</p>';
  try {
    const [project, projectMembers, milestones] = await Promise.all([api(`/projects/${projectId}`), api(`/projects/${projectId}/members`), api(`/projects/${projectId}/milestones`)]);
    activeMilestones = milestones || [];
    const canManage = isAdmin() || project.currentUserProjectRole === "PROJECT_ADMIN";
    const disabled = canManage ? "" : "disabled";
    const milestoneRows = activeMilestones.map((m) => `<div class="milestone-row"><span><strong>${escapeHtml(m.name)}</strong><small>${escapeHtml(m.description || "")}</small></span><span>${dateValue(m.plannedDate) || "-"}</span><span>${dateValue(m.completedDate) || "-"}</span><span>${escapeHtml(m.status)}</span><span>${canManage ? `<button type="button" class="text-button" data-edit-milestone="${m.id}">编辑</button><button type="button" class="text-button" data-delete-milestone="${m.id}">删除</button>` : ""}</span></div>`).join("") || '<p class="empty-state">暂无里程碑</p>';
    const textInput = (label, id, value, maxLength = "") => `<label><span>${label}</span><input id="${id}" value="${escapeHtml(value || "")}" ${maxLength} ${disabled} /></label>`;
    const dateInput = (label, id, value) => `<label><span>${label}</span><input id="${id}" type="date" value="${dateValue(value)}" ${disabled} /></label>`;
    const selectInput = (label, id, value, options) => `<label><span>${label}</span><select id="${id}" ${disabled}>${options}</select></label>`;
    const options = (values, value) => values.map((item) => `<option ${item === value ? "selected" : ""}>${item}</option>`).join("");
    const milestoneTable = `<section class="profile-card"><div class="section-heading"><h3>项目里程碑</h3>${canManage ? '<button type="button" class="primary-button" data-add-milestone>+ 新增里程碑</button>' : ""}</div><div class="milestone-head"><span>里程碑</span><span>计划日期</span><span>完成日期</span><span>状态</span><span>操作</span></div>${milestoneRows}</section>`;
    $("#workItemsTable").innerHTML = `<section class="project-profile"><div class="project-profile-head"><div><p class="eyebrow">${escapeHtml(project.code || "")}</p><h2>${escapeHtml(project.name)}</h2></div><button type="button" class="secondary-button" data-back-projects>返回项目管理</button></div><form id="projectProfileForm"><section class="profile-card"><h3>基础身份</h3><div class="profile-form-grid"><label><span>项目编号</span><input value="${escapeHtml(project.code || "")}" disabled /></label>${textInput("项目名称", "profileNameField", project.name)}${textInput("项目简称", "profileShortName", project.shortName, 'maxlength="8"')}${selectInput("项目类型", "profileProjectType", project.projectType, projectTypeOptions(project.projectType))}<div hidden>${selectInput("工作分区", "profileWorkZone", project.workZone, [["未分区", "未分区"], ["A", "A区 · 硬仗清单"], ["B", "B区 · 调优策略"], ["C", "C区 · 部门级重点工作"], ["D", "D区 · 日常周期性工作"]].map(([value, label]) => `<option value="${value}" ${project.workZone === value ? "selected" : ""}>${label}</option>`).join(""))}</div>${textInput("所属业务线", "profileBusinessLine", project.businessLine)}${textInput("客户名称", "profileCustomerName", project.customerName)}${textInput("交付地点", "profileDeliveryLocation", project.deliveryLocation)}${textInput("客户方负责人", "profileCustomerContact", project.customerContact)}</div><label class="profile-textarea"><span>项目说明 / 目标</span><textarea id="profileDescription" ${disabled}>${escapeHtml(project.description || "")}</textarea></label></section><section class="profile-card"><h3>组织与计划</h3><div class="profile-form-grid">${selectInput("项目经理", "profileProjectManager", project.projectManagerId, projectSelectOptions(projectMembers, project.projectManagerId))}${selectInput("实施负责人", "profileImplementationLead", project.implementationLeadId, projectSelectOptions(projectMembers, project.implementationLeadId))}${selectInput("开发负责人", "profileDevelopmentLead", project.developmentLeadId, projectSelectOptions(projectMembers, project.developmentLeadId))}${selectInput("当前阶段", "profilePhase", project.phase, options(["立项", "实施", "开发", "测试", "上线", "验收", "运维"], project.phase))}${selectInput("项目状态", "profileStatus", project.projectStatus, options(["未启动", "进行中", "已暂停", "已完成", "已关闭"], project.projectStatus))}${selectInput("健康状态", "profileHealth", project.healthStatus, options(["正常", "关注", "风险"], project.healthStatus))}${dateInput("计划开始", "profilePlannedStart", project.plannedStartDate)}${dateInput("计划结束", "profilePlannedEnd", project.plannedEndDate)}${dateInput("实际开始", "profileActualStart", project.actualStartDate)}${dateInput("实际结束", "profileActualEnd", project.actualEndDate)}</div></section><section class="profile-card"><h3>交付与管理</h3><div class="profile-form-grid">${selectInput("实施方式", "profileImplementationMode", project.implementationMode, options(["驻场", "远程", "混合"], project.implementationMode))}${dateInput("上线日期", "profileGoLiveDate", project.goLiveDate)}${dateInput("运维支持截止", "profileSupportEndDate", project.supportEndDate)}</div><div class="profile-text-grid">${[["项目范围 / 主要模块", "profileScope", project.scope], ["关键交付物", "profileDeliverables", project.deliverables], ["验收标准", "profileAcceptance", project.acceptanceCriteria], ["风险说明", "profileRisks", project.riskDescription], ["当前问题", "profileIssues", project.currentIssues], ["下阶段重点工作", "profileNextSteps", project.nextSteps]].map(([label, id, value]) => `<label><span>${label}</span><textarea id="${id}" ${disabled}>${escapeHtml(value || "")}</textarea></label>`).join("")}</div></section>${canManage ? '<div class="modal-actions"><button class="primary-button" type="submit">保存项目档案</button></div>' : ""}</form>${milestoneTable}</section>`;
  } catch (error) { $("#workItemsTable").innerHTML = `<p class="empty-state">${escapeHtml(error.message)}</p>`; }
}

async function showSystemSettings() {
  if (!isAdmin()) return showToast("无权访问系统设置");
  activePage = "system-settings"; showListView();
  syncPageNavigation();
  hideWorkItemsPagination();
  $(".content").classList.remove("task-management-mode", "project-brief-mode", "zone-dashboard-mode"); $(".content").classList.add("project-management-mode");
  $("#pageTitle").textContent = "系统设置";
  const [settings, loginSettings] = await Promise.all([api("/settings/project-code"), api("/settings/third-party-login")]);
  $("#workItemsTable").innerHTML = `<section class="settings-page"><form id="projectCodeSettingsForm" class="profile-card"><h2>项目编号规则</h2><p class="row-meta">仅影响新建项目，既有项目编号保持不变。</p><label class="settings-field"><span>编号前缀</span><input id="projectCodePrefix" maxlength="16" pattern="[A-Za-z0-9-]{1,16}" value="${escapeHtml(settings.projectCodePrefix || "PRJ")}" required /></label><p>生成示例：<strong id="projectCodeExample">${escapeHtml(settings.projectCodePrefix || "PRJ")}-YYYYMM-001</strong></p><div class="modal-actions"><button class="primary-button" type="submit">保存设置</button></div></form><form id="thirdPartyLoginSettingsForm" class="profile-card"><div class="section-heading"><div><h2>集成第三方协同 APP 登录</h2><p class="row-meta">开启后，登录页只显示已选择且完成配置的平台入口。</p></div><a class="secondary-button settings-help-button" href="./external-auth-help.html" target="_blank" rel="noopener">查看配置帮助</a></div><label class="settings-switch"><input id="thirdPartyLoginEnabled" type="checkbox" ${loginSettings.enabled ? "checked" : ""}><span>启用集成第三方协同 APP 登录</span></label><div id="thirdPartyProviderOptions" class="third-party-provider-options"><label><input id="thirdPartyFeishuEnabled" type="checkbox" ${loginSettings.feishuEnabled ? "checked" : ""}>飞书</label><label><input id="thirdPartyDingtalkEnabled" type="checkbox" ${loginSettings.dingtalkEnabled ? "checked" : ""}>钉钉</label><label><input id="thirdPartyWecomEnabled" type="checkbox" ${loginSettings.wecomEnabled ? "checked" : ""}>企微</label></div><p class="row-meta">当前版本只保存启用策略；各平台 App ID、密钥和回调地址由部署配置提供。</p><div class="modal-actions"><button class="primary-button" type="submit">保存设置</button></div></form></section>`;
  $("#thirdPartyLoginEnabled").addEventListener("change", () => syncThirdPartyLoginOptions());
  syncThirdPartyLoginOptions();
}

function syncThirdPartyLoginOptions() {
  const enabled = $("#thirdPartyLoginEnabled")?.checked;
  $("#thirdPartyProviderOptions")?.classList.toggle("hidden", !enabled);
  $all("#thirdPartyProviderOptions input").forEach((input) => { input.disabled = !enabled; });
}

function showBasicData() {
  if (!isAdmin()) return showToast("无权访问基础资料");
  activePage = "basic-data"; showListView();
  syncPageNavigation();
  hideWorkItemsPagination();
  $(".content").classList.remove("task-management-mode", "project-brief-mode", "zone-dashboard-mode"); $(".content").classList.add("project-management-mode");
  $("#pageTitle").textContent = "基础资料";
  $("#workItemsTable").innerHTML = `<section class="basic-data-page"><button type="button" class="basic-data-card" data-page="task-types"><strong>任务类型</strong><span>维护任务分类，支持新增、修改、启用与停用。</span></button><button type="button" class="basic-data-card" data-page="project-types"><strong>项目类型</strong><span>维护项目分类，支持新增、修改、启用与停用。</span></button></section>`;
}

async function showTaskTypes() {
  if (!isAdmin()) return showToast("无权访问基础资料");
  activePage = "task-types"; showListView();
  syncPageNavigation();
  hideWorkItemsPagination();
  $(".content").classList.remove("task-management-mode", "project-brief-mode", "zone-dashboard-mode"); $(".content").classList.add("project-management-mode");
  $("#pageTitle").textContent = "任务类型";
  const allTaskTypes = await api("/task-types?includeDisabled=true");
  const typeRows = allTaskTypes.map((type) => `<div class="task-type-row"><span><strong>${escapeHtml(type.name)}</strong></span><span>${type.enabled ? "启用" : "停用"}</span><span><button type="button" class="text-button" data-edit-task-type="${type.id}" data-task-type-name="${escapeHtml(type.name)}">修改</button><button type="button" class="text-button" data-toggle-task-type="${type.id}" data-task-type-enabled="${type.enabled}">${type.enabled ? "停用" : "启用"}</button></span></div>`).join("");
  $("#workItemsTable").innerHTML = `<section class="settings-page"><section class="profile-card"><div class="section-heading"><div><h2>任务类型</h2><p class="row-meta">停用后新建任务不可选择，历史任务仍保留。</p></div></div><form id="taskTypeCreateForm" class="task-type-create"><input id="taskTypeName" maxlength="16" required placeholder="输入新的任务类型" /><button class="primary-button" type="submit">新增类型</button></form><div class="task-type-head"><span>类型名称</span><span>状态</span><span>操作</span></div>${typeRows}</section></section>`;
}

async function showProjectTypes() {
  if (!isAdmin()) return showToast("无权访问基础资料");
  activePage = "project-types"; showListView();
  syncPageNavigation();
  hideWorkItemsPagination();
  $(".content").classList.remove("task-management-mode", "project-brief-mode", "zone-dashboard-mode"); $(".content").classList.add("project-management-mode");
  $("#pageTitle").textContent = "项目类型";
  const allProjectTypes = await api("/project-types?includeDisabled=true");
  const typeRows = allProjectTypes.map((type) => `<div class="task-type-row"><span><strong>${escapeHtml(type.name)}</strong></span><span>${type.enabled ? "启用" : "停用"}</span><span><button type="button" class="text-button" data-edit-project-type="${type.id}" data-project-type-name="${escapeHtml(type.name)}">修改</button><button type="button" class="text-button" data-toggle-project-type="${type.id}" data-project-type-enabled="${type.enabled}">${type.enabled ? "停用" : "启用"}</button></span></div>`).join("");
  $("#workItemsTable").innerHTML = `<section class="settings-page"><section class="profile-card"><div class="section-heading"><div><h2>项目类型</h2><p class="row-meta">停用后新建项目不可选择，历史项目仍保留。</p></div></div><form id="projectTypeCreateForm" class="task-type-create"><input id="projectTypeName" maxlength="16" required placeholder="输入新的项目类型" /><button class="primary-button" type="submit">新增类型</button></form><div class="task-type-head"><span>类型名称</span><span>状态</span><span>操作</span></div>${typeRows}</section></section>`;
}

async function showProjectBrief() {
  if (!requireProject()) return;
  activePage = "reports";
  showListView();
  hideWorkItemsPagination();
  const content = $(".content");
  content.classList.remove("project-management-mode", "task-management-mode", "zone-dashboard-mode");
  content.classList.add("project-brief-mode");
  const summary = isAllProjects() ? currentSummary : await api(`/projects/${currentProjectId}/summary`);
  $("#pageTitle").textContent = "项目简报";
  $("#currentContextLine").textContent = "项目概览";
  $("#workItemsTable").innerHTML = `<div class="project-overview"><strong>${escapeHtml(isAllProjects() ? "全部项目" : (currentProject()?.name || ""))}</strong><span>我的待办 ${summary.todoCount}</span><span>新建 ${summary.inboxCount}</span><span>进行中 ${summary.inProgressCount}</span><span>待验收 ${summary.toBeVerifiedCount}</span><span>今日到期 ${summary.todayDue || 0}</span><span>P0 紧急 ${summary.p0Urgent || 0}</span></div><div class="report-links"><button class="secondary-button" data-report-section="due">查看逾期 / 到期事项</button><button class="secondary-button" data-report-section="activity">查看项目动态</button></div><div id="reportDetail" class="compact-list"></div>`;
  $all(".nav-item").forEach((node) => node.classList.remove("active"));
}

function renderUserManagementList() {
  const keyword = $("#userSearch")?.value.trim().toLowerCase() || "";
  const status = $("#userStatusFilter")?.value || "all";
  const filtered = userDirectoryEntries.filter((user) => {
    const matchesKeyword = `${user.nickname || ""} ${user.username || ""}`.toLowerCase().includes(keyword);
    const matchesStatus = status === "all" || (status === "enabled" ? user.status === 1 : user.status !== 1);
    return matchesKeyword && matchesStatus;
  });
  const pageCount = Math.max(1, Math.ceil(filtered.length / managementPageSize));
  userManagementPage = Math.min(userManagementPage, pageCount);
  const rows = filtered.slice((userManagementPage - 1) * managementPageSize, userManagementPage * managementPageSize)
    .map((user) => `<div class="user-list-row"><span>${escapeHtml(user.nickname || "-")}</span><span>${escapeHtml(user.username || "-")}</span><span>${escapeHtml(user.department || "-")} / ${escapeHtml(user.jobRole || "-")}</span><span>${user.systemRole === "ADMIN" ? "系统管理员" : "普通用户"}</span><span>${user.status === 1 ? "启用" : "已禁用"}</span><span><button class="text-button" data-edit-user='${JSON.stringify(user).replaceAll("'", "&#39;")}'>编辑</button><button class="text-button" data-toggle-user="${user.userId}" data-status="${user.status}">${user.status === 1 ? "禁用" : "启用"}</button><button class="text-button" data-reset-user="${user.userId}">重置密码</button></span></div>`);
  $("#userManagementList").innerHTML = `<div class="user-list-scroll"><div class="user-list-head"><span>姓名</span><span>邮箱</span><span>部门 / 岗位</span><span>系统权限</span><span>状态</span><span>操作</span></div>${rows.join("") || '<p class="empty-state">没有符合条件的用户</p>'}</div>`;
  renderManagementPagination("#userManagementPagination", userManagementPage, pageCount, filtered.length);
}

function renderProjectManagementList() {
  const keyword = $("#projectSearch")?.value.trim().toLowerCase() || "";
  const status = $("#projectStatusFilter")?.value || "active";
  const scope = $("#projectScopeFilter")?.value || "all";
  const filtered = projectDirectoryEntries.filter(({ project }) => {
    const matchesKeyword = `${project.code || ""} ${project.name} ${project.shortName} ${project.customerName || ""}`.toLowerCase().includes(keyword);
    const matchesStatus = status === "all" || (status === "archived" ? project.archived : !project.archived);
    const matchesScope = scope !== "managed" || project.currentUserProjectRole === "PROJECT_ADMIN";
    return matchesKeyword && matchesStatus && matchesScope;
  });
  const pageCount = Math.max(1, Math.ceil(filtered.length / managementPageSize));
  projectManagementPage = Math.min(projectManagementPage, pageCount);
  const rows = filtered.slice((projectManagementPage - 1) * managementPageSize, projectManagementPage * managementPageSize).map(({ project, members }) => {
    const administrators = members.filter((member) => member.role === "PROJECT_ADMIN").map((member) => member.nickname || member.username).join("、") || "未设置";
    const canManage = isAdmin() || project.currentUserProjectRole === "PROJECT_ADMIN";
    const manager = members.find((member) => Number(member.userId) === Number(project.projectManagerId));
    const actions = [`<button class="text-button" data-project-profile="${project.id}">项目档案</button>`];
    if (canManage && !project.archived) actions.push(`<button class="text-button" data-manage-members="${project.id}">设置团队</button>`);
    if (isAdmin()) actions.push(`<button class="text-button" data-archive-project="${project.id}" data-archived="${project.archived}">${project.archived ? "恢复" : "归档"}</button>`);
    return `<div class="project-list-row"><span><span class="project-dot">${escapeHtml(project.shortName?.slice(0, 1) || "项")}</span><span><strong>${escapeHtml(project.name)}</strong><small>${escapeHtml(project.code || "")}</small></span></span><span>${escapeHtml(project.customerName || "-")}</span><span>${escapeHtml(manager?.nickname || manager?.username || administrators)}</span><span>${escapeHtml(project.phase || "立项")}</span><span>${escapeHtml(project.projectStatus || (project.archived ? "已归档" : "进行中"))}</span><span>${escapeHtml(project.healthStatus || "正常")}</span><span>${actions.join("")}</span></div>`;
  });
  $("#projectManagementList").innerHTML = `<div class="project-list-scroll"><div class="project-list-head"><span>项目</span><span>客户</span><span>项目经理</span><span>阶段</span><span>状态</span><span>健康度</span><span>操作</span></div>${rows.join("") || '<p class="empty-state">没有符合条件的项目</p>'}</div>`;
  renderManagementPagination("#projectManagementPagination", projectManagementPage, pageCount, filtered.length);
}

function renderManagementPagination(selector, page, pageCount, total) {
  const container = $(selector);
  if (!container) return;
  container.innerHTML = `<span>共 ${total} 条</span><label>每页 <select data-management-page-size>${[10, 20, 50, 100].map((size) => `<option value="${size}" ${size === managementPageSize ? "selected" : ""}>${size}</option>`).join("")} </select> 条</label><button type="button" class="secondary-button" data-management-page="${page - 1}" ${page <= 1 ? "disabled" : ""}>上一页</button><span>第 ${page} / ${pageCount} 页</span><button type="button" class="secondary-button" data-management-page="${page + 1}" ${page >= pageCount ? "disabled" : ""}>下一页</button>`;
}
async function openMemberModal(projectId) { managingProjectId = Number(projectId); const [users, members] = await Promise.all([api("/users?page=1&size=100"), api(`/projects/${managingProjectId}/members`)]); const memberIds = new Set(members.map((m) => m.userId)); memberCandidates = (users.list || []).filter((u) => u.status === 1 && !memberIds.has(u.userId)); renderMemberCandidates(); $("#memberTable").innerHTML = members.map((m) => `<div class="table-row users-row"><span>${escapeHtml(m.nickname || m.username)}</span><span>${escapeHtml(m.username)}</span><span><select data-member-role="${m.userId}"><option value="PROJECT_ADMIN" ${m.role === "PROJECT_ADMIN" ? "selected" : ""}>项目管理员</option><option value="MEMBER" ${m.role === "MEMBER" ? "selected" : ""}>成员</option><option value="GUEST" ${m.role === "GUEST" ? "selected" : ""}>访客</option></select></span><span><button class="text-button" data-remove-member="${m.userId}">移除</button></span></div>`).join("") || "<p>暂无成员</p>"; $("#memberModalBackdrop").classList.remove("hidden"); }
function renderMemberCandidates() { const keyword = $("#memberSearch")?.value.toLowerCase() || ""; $("#memberUserSelect").innerHTML = memberCandidates.filter((u) => `${u.nickname} ${u.username}`.toLowerCase().includes(keyword)).map((u) => `<option value="${u.userId}">${escapeHtml(u.nickname)}（${escapeHtml(u.username)}）</option>`).join("") || "<option value=\"\">暂无可添加用户</option>"; }

function buildWorkItemQuery({ omitProject = false, paginate = true } = {}) {
  const params = new URLSearchParams({ page: paginate ? String(taskPage) : "1", size: paginate ? String(taskPageSize) : "200", sort: currentSort });
  const conditions = { ...advancedFilters, type: activeType === "all" ? advancedFilters.type : activeType, view: currentView || undefined };
  delete conditions.severity;
  if (omitProject) delete conditions.projectId;
  Object.entries(conditions).forEach(([key, value]) => { if (value !== undefined && value !== null && value !== "") params.set(key, String(value)); });
  return params.toString();
}

function applySavedFilterState(filter) {
  activeSavedFilterId = filter?.id || null;
  const conditions = { ...(filter?.conditions || {}) };
  currentView = conditions.view || null;
  delete conditions.view;
  delete conditions.severity;
  advancedFilters = conditions;
  currentSort = filter?.sort || "createdAt,desc";
}

function renderSavedFilters() {
  if (!$("#savedFilterList")) return;
  const container = $("#savedFilterList");
  container.innerHTML = savedFilters.map((filter) => `<div class="saved-filter-row ${filter.id === activeSavedFilterId ? "active" : ""}">
    <button class="mini-link" data-saved-filter="${filter.id}">${filter.defaultFilter ? "★ " : ""}${escapeHtml(filter.name)}</button>
    <button class="filter-action" data-filter-rename="${filter.id}" title="重命名">✎</button>
    <button class="filter-action" data-filter-copy="${filter.id}" title="复制">⧉</button>
    <button class="filter-action" data-filter-default="${filter.id}" title="设为默认">★</button>
    <button class="filter-action" data-filter-delete="${filter.id}" title="删除">×</button>
  </div>`).join("") || '<p class="row-meta">暂无保存筛选</p>';
}

function renderSummary(summary) {
  currentSummary = summary;
  $("#todoCount").textContent = summary.todoCount;
  $("#inboxCount").textContent = summary.inboxCount;
  $("#progressCount").textContent = summary.inProgressCount;
  $("#verifyCount").textContent = summary.toBeVerifiedCount;
}

function currentProject() { return projects.find((project) => project.id === currentProjectId); }
function projectName(projectId) { return projects.find((project) => project.id === Number(projectId))?.name || "未知项目"; }
function selectedItem() { return workItems.find((item) => item.id === selectedId); }
function chip(text, tone = "") { return `<span class="chip ${tone}">${escapeHtml(text || "-")}</span>`; }
function ownerName(item) { return item.ownerName || "未分配"; }
function ownerCell(name) { return name === "未分配" ? chip(name, "orange") : `<span class="owner"><span class="mini-avatar">${escapeHtml(name.slice(0, 1))}</span>${escapeHtml(name)}</span>`; }
function formatDate(value) { return value ? new Date(value).toLocaleString("zh-CN", { hour12: false }) : "未设置"; }

const statusTransitions = {
  "新建": ["进行中", "已拒绝"], "进行中": ["延期处理", "已完成", "已拒绝"], "延期处理": ["进行中"],
  "已完成": ["已验收", "验收不通过"], "验收不通过": ["进行中"], "已拒绝": ["进行中"], "已验收": [],
};

function canQuickEdit(item, field) {
  if (isAllProjects()) return false;
  const project = currentProject();
  if (!project || project.archived || project.currentUserProjectRole === "GUEST") return false;
  if (["owner", "priority"].includes(field)) return true;
  if (field !== "status" || !(statusTransitions[item.status] || []).length) return false;
  const isProjectAdmin = project.currentUserProjectRole === "PROJECT_ADMIN";
  const isActor = isProjectAdmin || item.ownerId === currentUser?.userId || item.creatorId === currentUser?.userId;
  if (!isActor) return false;
  if (["验收不通过", "已拒绝"].includes(item.status)) return item.ownerId === currentUser?.userId;
  return true;
}

function quickField(item, field) {
  if (!canQuickEdit(item, field)) {
    if (field === "owner") return ownerCell(ownerName(item));
    return chip(item[field], field === "status" ? "blue" : (["P0", "P1"].includes(item.priority) ? "red" : "orange"));
  }
  let value = item[field];
  let options = [];
  if (field === "status") options = [item.status, ...(statusTransitions[item.status] || [])].map((status) => [status, status]);
  if (field === "owner") { value = item.ownerId || 0; options = [[0, "未分配"], ...members.map((member) => [member.userId, member.nickname || member.username])]; }
  if (field === "priority") options = ["P0", "P1", "P2", "P3"].map((priority) => [priority, priority]);
  return `<select class="quick-field quick-field-${field}" data-quick-field="${field}" data-item-id="${item.id}" data-previous="${escapeHtml(value)}" aria-label="快捷修改${field === "status" ? "状态" : field === "owner" ? "负责人" : "优先级"}">${options.map(([optionValue, label]) => `<option value="${escapeHtml(optionValue)}" ${String(optionValue) === String(value) ? "selected" : ""}>${escapeHtml(label)}</option>`).join("")}</select>`;
}

function filteredItems() {
  const keyword = $("#globalSearch").value.trim().toLowerCase();
  const status = $("#statusFilter").value;
  const owner = $("#ownerFilter").value;
  return workItems.filter((item) =>
    (activeType === "all" || item.type === activeType) &&
    (!keyword || [item.title, item.id, item.module, projectName(item.projectId)].some((value) => String(value || "").toLowerCase().includes(keyword))) &&
    (status === "all" || item.status === status) &&
    (owner === "all" || ownerName(item) === owner));
}

function renderAll() {
  if (isInsightsPage()) { renderDrawer(); return; }
  $(".content").classList.remove("project-management-mode", "project-brief-mode", "zone-dashboard-mode");
  $(".content").classList.toggle("task-management-mode", activePage === "tasks");
  renderProjectContext();
  renderSummary(currentSummary);
  renderTopPanels();
  renderTable();
  renderBoard();
  renderDrawer();
}

function renderProjectContext() {
  const project = currentProject();
  if (!project && !isAllProjects()) return;
  $("#projectSwitchDot").textContent = isAllProjects() ? "全" : (project.shortName?.slice(0, 1) || "项");
  $("#currentProjectName").textContent = isAllProjects() ? "全部项目" : project.name;
  const viewNames = { "created-by-me": "我创建的", "assigned-to-me": "指派给我的", "pending-for-me": "待我处理的", unclosed: "未完成任务" };
  $("#currentContextLine").textContent = viewNames[currentView] || "全部任务";
  $("#pageTitle").textContent = activePage === "tasks" ? "任务管理" : "任务看板";
  const counts = { all: workItems.length };
  taskTypes.forEach((type) => { counts[type.name] = workItems.filter((item) => item.type === type.name).length; });
  $("#typeTabs").innerHTML = `<button class="tab ${activeType === "all" ? "active" : ""}" data-type="all">全部 <span>${counts.all}</span></button>${taskTypes.map((type) => `<button class="tab ${activeType === type.name ? "active" : ""}" data-type="${escapeHtml(type.name)}">${escapeHtml(type.name)} <span>${counts[type.name] || 0}</span></button>`).join("")}`;
  $all("#typeTabs .tab").forEach((tab) => { tab.innerHTML = `${tab.dataset.type === "all" ? "全部" : tab.dataset.type} <span>${counts[tab.dataset.type] || 0}</span>`; });
  $("#ownerFilter").innerHTML = `<option value="all">负责人</option><option value="未分配">未分配</option>${members.map((member) => `<option>${escapeHtml(member.nickname || member.username)}</option>`).join("")}`;
  $("#projectFilter").classList.toggle("hidden", !isAllProjects());
  $("#projectFilter").innerHTML = `<option value="">全部项目</option>${projects.map((item) => `<option value="${item.id}" ${String(advancedFilters.projectId || "") === String(item.id) ? "selected" : ""}>${escapeHtml(item.name)}</option>`).join("")}`;
  renderTaskNavigation();
}

function renderTaskNavigation() {
  const countIds = { all: "taskCountAll", unclosed: "taskCountUnclosed", "created-by-me": "taskCountCreated", "assigned-to-me": "taskCountAssigned", "pending-for-me": "taskCountPending" };
  Object.entries(countIds).forEach(([view, id]) => { const node = $("#" + id); if (node) node.textContent = taskCounts[view] || 0; });
  $all(".task-nav-item").forEach((node) => node.classList.toggle("active", activePage === "tasks" && node.dataset.taskView === (currentView || "all")));
  syncPageNavigation();
}

function openFilterModal() {
  const values = { View: currentView, Type: advancedFilters.type, Status: advancedFilters.status, Priority: advancedFilters.priority,
    Owner: advancedFilters.ownerId, Creator: advancedFilters.creatorId, Sprint: advancedFilters.sprintId,
    Module: advancedFilters.module, Tag: advancedFilters.tag, Keyword: advancedFilters.keyword, Project: advancedFilters.projectId };
  const memberOptions = `<option value="">不限</option>${members.map((member) => `<option value="${member.userId}">${escapeHtml(member.nickname || member.username)}</option>`).join("")}`;
  $("#filterOwner").innerHTML = memberOptions; $("#filterCreator").innerHTML = memberOptions;
  $("#filterType").innerHTML = `<option value="">不限</option>${taskTypes.map((type) => `<option value="${escapeHtml(type.name)}">${escapeHtml(type.name)}</option>`).join("")}`;
  $("#filterProject").innerHTML = `<option value="">不限</option>${projects.map((project) => `<option value="${project.id}">${escapeHtml(project.name)}</option>`).join("")}`;
  Object.entries(values).forEach(([name, value]) => { $("#filter" + name).value = value ?? ""; });
  $("#filterDueFrom").value = advancedFilters.dueFrom ? advancedFilters.dueFrom.slice(0, 10) : "";
  $("#filterDueTo").value = advancedFilters.dueTo ? advancedFilters.dueTo.slice(0, 10) : "";
  $("#filterSort").value = currentSort;
  $("#filterModalBackdrop").classList.remove("hidden");
}

function readFilterForm() {
  const raw = { type: $("#filterType").value, status: $("#filterStatus").value, priority: $("#filterPriority").value,
    ownerId: $("#filterOwner").value, creatorId: $("#filterCreator").value,
    projectId: $("#filterProject").value,
    sprintId: $("#filterSprint").value, module: $("#filterModule").value.trim(), tag: $("#filterTag").value.trim(), keyword: $("#filterKeyword").value.trim(),
    dueFrom: $("#filterDueFrom").value ? new Date(`${$("#filterDueFrom").value}T00:00:00`).toISOString() : "",
    dueTo: $("#filterDueTo").value ? new Date(`${$("#filterDueTo").value}T23:59:59`).toISOString() : "" };
  ["ownerId", "creatorId", "sprintId", "projectId"].forEach((key) => { if (raw[key]) raw[key] = Number(raw[key]); });
  return Object.fromEntries(Object.entries(raw).filter(([, value]) => value !== ""));
}

async function refreshSavedFilters() {
  savedFilters = await api(`/projects/${currentProjectId}/saved-filters`);
  renderSavedFilters();
}

async function saveCurrentFilter() {
  const name = window.prompt("请输入筛选器名称");
  if (!name?.trim()) return;
  try {
    const created = await api(`/projects/${currentProjectId}/saved-filters`, { method: "POST", body: JSON.stringify({ name: name.trim(), conditions: { ...advancedFilters, ...(currentView ? { view: currentView } : {}) }, sort: currentSort, defaultFilter: false }) });
    activeSavedFilterId = created.id; await refreshSavedFilters(); showToast("筛选器已保存");
  } catch (error) { showToast(error.message); }
}

function renderTopPanels() {
  const mine = workItems.filter((item) => item.ownerId === currentUser?.userId || item.ownerId == null).slice(0, 4);
  $("#todoList").innerHTML = mine.map(compactItem).join("") || "<p>暂无待办</p>";
  $("#activityList").innerHTML = activities.slice(0, 4).map((item) => `<div class="activity-row"><span class="row-main"><span class="row-title">${escapeHtml(item.actorName || "系统")} ${escapeHtml(item.content)}</span><span class="row-meta">${formatDate(item.createdAt)}</span></span></div>`).join("") || "<p>暂无动态</p>";
  $("#dueList").innerHTML = dueItems.slice(0, 4).map(compactItem).join("") || "<p>暂无临近到期工作项</p>";
}

function compactItem(item) {
  return `<div class="compact-row" data-open="${item.id}"><span class="row-main"><span class="row-title">${escapeHtml(item.title)}</span>${chip(item.type, typeClass[item.type])}</span><span class="compact-quick-fields">${quickField(item, "status")}${quickField(item, "owner")}${quickField(item, "priority")}</span><span class="row-meta">截止时间：${formatDate(item.dueDate)}</span></div>`;
}

function renderTable() {
  const columns = viewPreference.columns || [];
  const template = `34px 100px minmax(220px,2fr) ${columns.map(() => "minmax(100px,1fr)").join(" ")}`;
  const head = $(".table-head"); head.style.gridTemplateColumns = template;
  const items = filteredItems();
  head.innerHTML = `<span><input type="checkbox" data-select-all ${items.length && items.every((item) => selectedIds.has(item.id)) ? "checked" : ""} /></span><span>ID</span><span>标题</span>${columns.map((key) => `<span>${columnDefinitions[key][0]}</span>`).join("")}`;
  const row = (item) => `<div style="grid-template-columns:${template}" class="table-row ${item.id === selectedId ? "selected" : ""}" data-open="${item.id}"><span><input type="checkbox" data-select-id="${item.id}" ${selectedIds.has(item.id) ? "checked" : ""}></span><span>${item.id}</span><span class="item-title">${escapeHtml(item.title)}</span>${columns.map((key) => `<span>${columnDefinitions[key][1](item)}</span>`).join("")}</div>`;
  if (!viewPreference.groupBy) $("#workItemsTable").innerHTML = items.map(row).join("") || '<p class="empty-state">暂无工作项</p>';
  else {
    const groups = new Map(); items.forEach((item) => { const key = groupName(item, viewPreference.groupBy); if (!groups.has(key)) groups.set(key, []); groups.get(key).push(item); });
    $("#workItemsTable").innerHTML = [...groups.entries()].map(([name, values]) => `<section><button class="work-group-title" data-group-toggle="${escapeHtml(name)}"><span>${collapsedGroups.has(name) ? "▸" : "▾"} ${escapeHtml(name)}</span><span>${values.length}</span></button>${collapsedGroups.has(name) ? "" : values.map(row).join("")}</section>`).join("") || '<p class="empty-state">暂无工作项</p>';
  }
  $("#bulkSelectedCount").textContent = selectedIds.size; $("#bulkUpdateBtn").classList.toggle("hidden", isAllProjects() || !selectedIds.size);
  renderWorkItemsPagination();
}

function renderWorkItemsPagination() {
  const container = $("#workItemsPagination");
  if (!container) return;
  const totalPages = Math.max(1, Math.ceil(taskTotal / taskPageSize));
  const start = taskTotal ? (taskPage - 1) * taskPageSize + 1 : 0;
  const end = Math.min(taskPage * taskPageSize, taskTotal);
  container.classList.remove("hidden");
  container.innerHTML = `<div>共 ${taskTotal} 条，当前 ${start}-${end} 条</div>
    <div class="pagination-controls">
      <label>每页
        <select id="taskPageSize">
          ${[10, 20, 50, 100, 200].map((size) => `<option value="${size}" ${size === taskPageSize ? "selected" : ""}>${size}</option>`).join("")}
        </select>
      </label>
      <button type="button" class="secondary-button" data-task-page="prev" ${taskPage <= 1 ? "disabled" : ""}>上一页</button>
      <span>${taskPage} / ${totalPages}</span>
      <button type="button" class="secondary-button" data-task-page="next" ${taskPage >= totalPages ? "disabled" : ""}>下一页</button>
    </div>`;
}

function hideWorkItemsPagination() {
  $("#workItemsPagination")?.classList.add("hidden");
}

function splitTags(value) { return [...new Set(value.split(/[,，]/).map((tag) => tag.trim()).filter(Boolean))]; }
function openBulkModal() { $("#bulkSummary").textContent = `已选择 ${selectedIds.size} 个工作项`; $("#bulkOwner").innerHTML = `<option value="">不修改</option>${members.map((m) => `<option value="${m.userId}">${escapeHtml(m.nickname || m.username)}</option>`).join("")}`; $("#bulkResult").textContent = ""; $("#bulkModalBackdrop").classList.remove("hidden"); }

function groupName(item, groupBy) { if (groupBy === "owner") return ownerName(item); if (groupBy === "sprint") return item.sprintId ? `迭代 ${item.sprintId}` : "无迭代"; return item[groupBy] || "未分类"; }

function openViewModal() { renderColumnSettings(); $("#viewSort").value = viewPreference.sort; $("#viewGroup").value = viewPreference.groupBy || ""; $("#viewModalBackdrop").classList.remove("hidden"); }
function renderColumnSettings() {
  const selected = viewPreference.columns; const ordered = [...selected, ...Object.keys(columnDefinitions).filter((key) => !selected.includes(key))];
  $("#columnSettings").innerHTML = ordered.map((key, index) => `<div class="column-setting"><label><input type="checkbox" data-column="${key}" ${selected.includes(key) ? "checked" : ""}>${columnDefinitions[key][0]}</label><button type="button" data-column-up="${key}" ${index === 0 ? "disabled" : ""}>↑</button><button type="button" data-column-down="${key}" ${index === ordered.length - 1 ? "disabled" : ""}>↓</button></div>`).join("");
}

function renderBoard() {
  const columns = ["新建", "进行中", "延期处理", "已完成", "已验收", "验收不通过", "已拒绝"];
  const filtered = filteredItems();
  $("#boardView").innerHTML = columns.map((status) => {
    const items = filtered.filter((item) => item.status === status);
    return `<div class="board-column" data-board-status="${escapeHtml(status)}"><div class="column-title"><span>${status}</span><span>${items.length}</span></div>${items.map((item) => `<div class="board-card" draggable="${canQuickEdit(item, "status")}" data-board-card="${item.id}" data-open="${item.id}"><span class="row-meta"><span>${item.id}</span>${chip(item.type, typeClass[item.type])}</span><strong>${escapeHtml(item.title)}</strong><span class="board-quick-fields">${quickField(item, "status")}${quickField(item, "owner")}${quickField(item, "priority")}</span></div>`).join("")}</div>`;
  }).join("");
}

async function moveBoardCardToStatus(itemId, targetStatus) {
  const item = workItems.find((workItem) => workItem.id === itemId);
  if (!item || item.status === targetStatus) return;
  if (!canQuickEdit(item, "status")) { showToast("当前没有权限拖动变更该任务状态"); return; }
  if (!(statusTransitions[item.status] || []).includes(targetStatus)) {
    showToast(`不允许从「${item.status}」转移到「${targetStatus}」`);
    return;
  }
  const payload = buildStatusPayload(item, targetStatus);
  if (!payload) return;
  try {
    await api(`/work-items/${item.id}/status`, { method: "PATCH", body: JSON.stringify(payload) });
    selectedIds.delete(item.id);
    await loadCurrentSelection();
    showToast("状态已更新");
  } catch (error) {
    renderBoard();
    showToast(error.message);
  }
}

function descriptionWithImagePreviews(html) {
  return (html || "<p>暂无描述</p>").replace(/\[\[image:(\d+)\]\]/g, '<img class="inline-description-image" data-image-id="$1" alt="工作项图片" />');
}

async function hydrateDescriptionImages(root = document) {
  for (const image of [...root.querySelectorAll(".inline-description-image")]) {
    try {
      const response = await fetch(`${API_BASE}/attachments/${image.dataset.imageId}/download`, { headers: { Authorization: `Bearer ${token}` } });
      if (!response.ok) throw new Error("图片加载失败");
      image.src = URL.createObjectURL(await response.blob());
    } catch (_) {
      image.replaceWith(Object.assign(document.createElement("span"), { className: "field-hint", textContent: "图片加载失败或文件不存在" }));
    }
  }
}
function renderDrawer() {
  const item = selectedItem();
  if (!item) { $("#detailDrawer").classList.remove("open"); return; }
  $("#drawerMeta").textContent = `${projectName(item.projectId)} · ${item.type} #${item.id}`;
  $("#drawerTitle").textContent = item.title;
  renderActionBar(item);
  if (drawerTab === "details") {
    const description = descriptionWithImagePreviews(item.description);
    const watcherNames = (item.watchers || []).map((watcher) => escapeHtml(watcher.nickname || watcher.username)).join("、") || "无";
    $("#drawerBody").innerHTML = `<section class="detail-section detail-description"><h3>任务描述</h3><div id="descriptionContent">${description}</div></section><section class="detail-section"><h3>基本信息</h3><div class="field-grid"><div class="field"><span>状态</span>${chip(item.status, "blue")}</div><div class="field"><span>优先级</span>${chip(item.priority)}</div><div class="field"><span>类型</span>${chip(item.type, typeClass[item.type])}</div><div class="field"><span>负责人</span>${ownerCell(ownerName(item))}</div><div class="field"><span>关注人</span>${watcherNames}</div><div class="field"><span>主模块</span>${escapeHtml(item.module || "未分类")}</div><div class="field"><span>子模块</span>${escapeHtml(item.submodule || "-")}</div><div class="field"><span>迭代</span>${escapeHtml(item.sprintId || "-")}</div><div class="field"><span>计划开始日期</span>${dateValue(item.plannedStartDate) || "未设置"}</div><div class="field"><span>截止时间</span>${formatDate(item.dueDate)}</div><div class="field"><span>实际完成时间</span>${formatDate(item.actualCompletedAt)}</div><div class="field"><span>预计工时</span>${item.estimatedHours == null ? "-" : `${item.estimatedHours} 小时`}</div><div class="field"><span>实际花费工时</span>${item.actualHours == null ? "-" : `${item.actualHours} 小时`}</div></div></section><section><h3>验收清单 / 步骤</h3>${(item.steps || []).map((step) => `<p>□ ${escapeHtml(step.content)}</p>`).join("") || "<p>暂无</p>"}</section>`; hydrateDescriptionImages();
  } else if (drawerTab === "activity") loadActivityTab(item);
  else if (drawerTab === "files") loadFilesTab(item);
  else if (drawerTab === "links") $("#drawerBody").innerHTML = `<section class="detail-section"><h3>关联任务</h3>${(item.relatedWorkItems || []).map((related) => `<button class="compact-row" data-open="${related.id}"><span class="row-title">${escapeHtml(related.id)} · ${escapeHtml(related.title)}</span><span class="row-meta">${escapeHtml(related.type)} · ${escapeHtml(related.status)}</span></button>`).join("") || "<p>暂无关联任务</p>"}</section>`;
  else $("#drawerBody").innerHTML = "<section><p>本阶段暂不实现该功能。</p></section>";
}

async function loadActivityTab(item) {
  $("#drawerBody").innerHTML = "<p>加载中...</p>";
  try {
    const [itemActivities, comments] = await Promise.all([api(`/work-items/${item.id}/activities`), api(`/work-items/${item.id}/comments?page=1&size=50`)]);
    $("#drawerBody").innerHTML = `<section><h3>活动</h3>${(itemActivities || []).map((entry) => `<p>${escapeHtml(entry.actorName || "系统")}：${escapeHtml(entry.content)} <small>${formatDate(entry.createdAt)}</small></p>`).join("") || "<p>暂无活动</p>"}</section><section><h3>评论</h3>${(comments.list || []).map((entry) => `<p>${escapeHtml(entry.authorName || "用户")}：${escapeHtml(entry.content)}</p>`).join("") || "<p>暂无评论</p>"}</section>`;
  } catch (error) { $("#drawerBody").innerHTML = `<p>${escapeHtml(error.message)}</p>`; }
}

async function loadFilesTab(item) {
  $("#drawerBody").innerHTML = "<p>加载中...</p>";
  try {
    const files = await api(`/work-items/${item.id}/attachments`);
    $("#drawerBody").innerHTML = `<section><h3>附件</h3><div class="detail-attachment-list">${files.map(attachmentRow).join("") || "<p>暂无附件</p>"}</div></section>`;
  } catch (error) { $("#drawerBody").innerHTML = `<p>${escapeHtml(error.message)}</p>`; }
}

function isImageAttachment(file) {
  return file.mimeType?.startsWith("image/");
}

function attachmentRow(file) {
  const canPreview = isImageAttachment(file);
  return `<div class="detail-attachment-item">
    <div class="attachment-thumb">${canPreview ? "图" : "文"}</div>
    <div>
      <div class="attachment-name">${escapeHtml(file.fileName)}</div>
      <div class="attachment-meta">${escapeHtml(file.mimeType || "未知类型")} · ${formatSize(file.size)}</div>
    </div>
    <div class="detail-attachment-actions">
      ${canPreview ? `<button type="button" class="text-button" data-preview-attachment="${file.id}">预览</button>` : ""}
      <button type="button" class="text-button" data-download="${file.id}" data-file-name="${escapeHtml(file.fileName)}">下载</button>
    </div>
  </div>`;
}

function renderActionBar(item) {
  const actions = [];
  actions.push([item.watched ? "取消关注" : "关注", () => mutate(`/work-items/${item.id}/watchers/me`, { method: item.watched ? "DELETE" : "POST" }, item.id)]);
  if (item.ownerId == null && item.status === "新建") actions.push(["分配给我并开始", () => mutate(`/work-items/${item.id}/assign-me`, { method: "POST" }, item.id)]);
  if (item.status === "新建") { actions.push(["开始处理", () => transitionItem(item.id, "进行中")]); actions.push(["拒绝任务", () => transitionItem(item.id, "已拒绝", { reason: true })]); }
  if (item.status === "进行中") { actions.push(["申请延期", () => transitionItem(item.id, "延期处理", { reason: true, dueDate: true })]); actions.push(["提交完成", () => transitionItem(item.id, "已完成")]); actions.push(["拒绝任务", () => transitionItem(item.id, "已拒绝", { reason: true })]); }
  if (item.status === "延期处理") { actions.push(["批准延期", () => transitionItem(item.id, "进行中", { delayApproved: true })]); actions.push(["驳回延期", () => transitionItem(item.id, "进行中", { delayApproved: false, reason: true })]); }
  if (item.status === "已完成") { actions.push(["验收通过", () => transitionItem(item.id, "已验收")]); actions.push(["验收不通过", () => transitionItem(item.id, "验收不通过", { reason: true })]); }
  if (["验收不通过", "已拒绝"].includes(item.status)) actions.push(["重新开始", () => transitionItem(item.id, "进行中", { reason: true })]);
  actions.push(["编辑图文", async () => { resetCreateModal(); editingItemId = item.id; await loadCreateOptions(item.projectId); $("#newProject").value = item.projectId; $("#newTitle").value = item.title; $("#newDescriptionEditor").innerHTML = descriptionWithImagePreviews(item.description); await hydrateDescriptionImages($("#newDescriptionEditor")); $("#createModal h2").textContent = "编辑工作项图文"; $("#createModal button[type='submit']").textContent = "保存"; $("#modalBackdrop").classList.remove("hidden"); }]);
  const project = projects.find(p => p.id === item.projectId);
  const writer = project && project.currentUserProjectRole !== "GUEST";
  const actor = writer && (project.currentUserProjectRole === "PROJECT_ADMIN" || item.ownerId === currentUser?.userId || item.creatorId === currentUser?.userId);
  const visibleActions = actions.filter(([label]) => {
    if (["关注", "取消关注"].includes(label)) return true;
    if (["编辑图文", "分配给我并开始"].includes(label)) return writer;
    if (label === "重新开始") return writer && item.ownerId === currentUser?.userId;
    return actor;
  });
  $("#actionBar").innerHTML = visibleActions.map((action, index) => `<button class="${index ? "secondary-button" : "primary-button"}" data-action="${index}">${action[0]}</button>`).join("");
  $("#actionBar")._actions = visibleActions;
}

async function transitionItem(id, status, options = {}) {
  const item = workItems.find((workItem) => workItem.id === id);
  const payload = buildStatusPayload(item, status, options);
  if (!payload) return;
  await mutate(`/work-items/${id}/status`, { method: "PATCH", body: JSON.stringify(payload) }, id);
}

function promptRequired(message, initialValue = "") {
  const value = window.prompt(message, initialValue);
  return value?.trim() ? value.trim() : null;
}

function promptDateTimeValue(value) {
  const date = value ? new Date(value) : new Date();
  const pad = (number) => String(number).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function buildStatusPayload(item, status, options = {}) {
  if (!item) return null;
  const payload = { status, reason: null, dueDate: null, delayApproved: options.delayApproved };
  const requiresReason = options.reason || ["延期处理", "已拒绝", "验收不通过"].includes(status)
    || (["验收不通过", "已拒绝"].includes(item.status) && status === "进行中");
  if (item.status === "延期处理" && status === "进行中" && options.delayApproved == null) {
    const decision = promptRequired("请输入延期处理结果：批准 或 驳回", "批准");
    if (!decision || !["批准", "驳回"].includes(decision)) { if (decision) showToast("请输入“批准”或“驳回”"); return null; }
    payload.delayApproved = decision === "批准";
  }
  if (requiresReason || payload.delayApproved === false) {
    payload.reason = promptRequired("请填写说明");
    if (!payload.reason) return null;
  }
  if (options.dueDate || status === "延期处理") {
    const value = promptRequired("请输入新的截止日期（YYYY-MM-DD）");
    if (!value) return null;
    const date = new Date(`${value}T23:59:59`);
    if (Number.isNaN(date.getTime())) { showToast("截止日期格式不正确"); return null; }
    payload.dueDate = date.toISOString();
  }
  if (status === "已完成") {
    const completedValue = promptRequired("请输入实际完成时间（YYYY-MM-DD HH:mm）", promptDateTimeValue(item.actualCompletedAt));
    if (!completedValue) return null;
    const completedAt = new Date(completedValue.replace(" ", "T"));
    if (Number.isNaN(completedAt.getTime())) { showToast("实际完成时间格式不正确"); return null; }
    const hoursValue = promptRequired("请输入实际花费工时（支持小数）", item.actualHours == null ? "" : String(item.actualHours));
    if (!hoursValue) return null;
    if (!/^\d+(\.\d{1,2})?$/.test(hoursValue)) { showToast("实际花费工时需为非负数，最多保留两位小数"); return null; }
    payload.actualCompletedAt = completedAt.toISOString();
    payload.actualHours = Number(hoursValue);
  }
  return payload;
}

async function saveQuickField(select) {
  const item = workItems.find((workItem) => workItem.id === select.dataset.itemId);
  if (!item) return;
  const previous = select.dataset.previous;
  select.disabled = true;
  try {
    if (select.dataset.quickField === "status") {
      const payload = buildStatusPayload(item, select.value);
      if (!payload) { select.value = previous; return; }
      await api(`/work-items/${item.id}/status`, { method: "PATCH", body: JSON.stringify(payload) });
    } else {
      const body = select.dataset.quickField === "owner" ? { ownerId: Number(select.value) } : { priority: select.value };
      await api(`/work-items/${item.id}`, { method: "PUT", body: JSON.stringify(body) });
    }
    await loadCurrentSelection();
    showToast("修改已保存");
  } catch (error) {
    select.value = previous;
    showToast(error.message);
  } finally { select.disabled = false; }
}

async function mutate(path, options, reopenId = null) {
  try { await api(path, options); await loadCurrentSelection(); if (reopenId) await openItem(reopenId); showToast("操作成功"); }
  catch (error) { showToast(error.message); }
}

async function openItem(id) {
  try { selectedId = id; const detail = await api(`/work-items/${id}`); workItems = workItems.filter((item) => item.id !== id); workItems.push(detail); $("#detailDrawer").classList.add("open"); renderAll(); }
  catch (error) { showToast(error.message); }
}

function closeDetailDrawer() {
  $("#detailDrawer").classList.remove("open");
}

function renderEmptyProjectState() {
  showApp();
  $("#currentProjectName").textContent = "暂无项目";
  $("#workItemsTable").innerHTML = isAdmin()
    ? '<div class="empty-state"><p>还没有项目。创建第一个项目后即可使用工作台功能。</p><button class="primary-button" data-create-project>创建第一个项目</button></div>'
    : '<p class="empty-state">当前账号尚未加入任何项目，请联系项目管理员将你加入项目。</p>';
}

function isAdmin() { return currentUser?.systemRole === "ADMIN"; }
function requireProject() { if (currentProjectId) return true; showToast(isAdmin() ? "请先创建或选择项目" : "请等待项目管理员将你加入项目"); return false; }
function openProjectModal() { if (!isAdmin()) return showToast("仅系统管理员可以创建项目"); $("#projectType").innerHTML = projectTypeOptions(); $("#projectModalBackdrop").classList.remove("hidden"); }
async function createProject(event) { event.preventDefault(); try { const name = $("#projectName").value.trim(); const project = await api("/projects", { method: "POST", body: JSON.stringify({ name, shortName: $("#projectShortName").value.trim() || name.slice(0, 8), projectType: $("#projectType").value }) }); projects = await api("/projects"); currentProjectId = project.id; renderProjectNavigation(); $("#projectModalBackdrop").classList.add("hidden"); $("#projectModal").reset(); await showProjectProfile(project.id); showToast(`项目已创建：${project.code}`); } catch (error) { showToast(error.message); } }
function openAccountModal(force = false) { $("#accountUsername").value = currentUser.username || ""; $("#accountNickname").value = currentUser.nickname || ""; $("#accountJobRole").value = currentUser.jobRole || "未设置"; $("#accountDepartment").value = currentUser.department || ""; $("#accountModal").dataset.force = force ? "true" : "false"; $("#accountModal .eyebrow").textContent = force ? "首次登录必须修改密码" : "修改密码（不修改请留空）"; $("#accountModalBackdrop").classList.remove("hidden"); }
async function saveAccount(event) { event.preventDefault(); const next = $("#accountNewPassword").value; const confirm = $("#accountConfirmPassword").value; if (next && next !== confirm) return showToast("两次输入的新密码不一致"); if ($("#accountModal").dataset.force === "true" && !next) return showToast("请先修改初始密码"); try { currentUser = await api("/users/me", { method: "PUT", body: JSON.stringify({ department: $("#accountDepartment").value.trim(), oldPassword: $("#accountOldPassword").value, newPassword: next }) }); $("#accountModalBackdrop").classList.add("hidden"); $("#accountModal").reset(); if (next) { logout(); showLogin("密码已修改，请重新登录"); return; } updateProfile(); showToast("账户信息已保存"); } catch (error) { showToast(error.message); } }
function openUserEditor(user = null) { $("#userEditModal").reset(); $("#userEditId").value = user?.userId || ""; $("#userEditTitle").textContent = user ? "编辑用户" : "新增用户"; $("#userEmail").value = user?.username || ""; $("#userEmail").disabled = !!user; $("#userNickname").value = user?.nickname || ""; $("#userDepartment").value = user?.department || ""; $("#userJobRole").value = user?.jobRole || ""; $("#userSystemRole").value = user?.systemRole || "USER"; $("#userEditModalBackdrop").classList.remove("hidden"); }
async function saveUser(event) { event.preventDefault(); const id = $("#userEditId").value; const body = { nickname: $("#userNickname").value.trim(), department: $("#userDepartment").value.trim(), jobRole: $("#userJobRole").value || null, systemRole: $("#userSystemRole").value }; if (!id) body.username = $("#userEmail").value.trim(); try { const result = await api(id ? `/users/${id}` : "/users", { method: id ? "PUT" : "POST", body: JSON.stringify(body) }); $("#userEditModalBackdrop").classList.add("hidden"); if (!id) window.prompt("用户已创建。请复制一次性初始密码并安全交付：", result.initialPassword); await showUserDirectory(); if (id) showToast("用户已更新"); } catch (error) { showToast(error.message); } }

function showToast(message) {
  const toast = $("#toast"); toast.textContent = message; toast.classList.remove("hidden");
  window.setTimeout(() => toast.classList.add("hidden"), 2500);
}

async function loadUnreadCount() { try { const data = await api("/notifications/unread-count"); $("#notifyCount").textContent = data.count; } catch (_) {} }
async function loadNotifications() { const read=$("#notificationRead").value,type=$("#notificationType").value; const q=new URLSearchParams({page:"1",size:"50"});if(read)q.set("read",read);if(type)q.set("type",type);const data=await api(`/notifications?${q}`);$("#notificationList").innerHTML=(data.list||[]).map((n)=>`<button class="compact-row" data-notification-id="${n.id}" data-notification-item="${escapeHtml(n.workItemId||"")}"><span class="row-title">${n.isRead?"":"● "}${escapeHtml(n.content)}</span><span class="row-meta">${formatDate(n.createdAt)}</span></button>`).join("")||"<p>暂无通知</p>"; }
async function exportCurrentView(){if(isAllProjects())return showToast("请选择具体项目后再导出");try{const body={...advancedFilters,...(currentView?{view:currentView}:{}),sort:currentSort,columns:viewPreference.columns};const job=await api(`/projects/${currentProjectId}/exports/work-items`,{method:"POST",body:JSON.stringify(body)});const response=await fetch(`${API_BASE}/exports/${job.id}/download`,{headers:{Authorization:`Bearer ${token}`}});if(!response.ok)throw new Error("导出下载失败");const url=URL.createObjectURL(await response.blob()),a=document.createElement("a");a.href=url;a.download=job.fileName;a.click();URL.revokeObjectURL(url);showToast("导出完成");}catch(error){showToast(error.message);}}

function formatSize(bytes) { return bytes < 1024 * 1024 ? `${(bytes / 1024).toFixed(1)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`; }
function addAttachments(files) { pendingAttachments = [...pendingAttachments, ...files]; $("#attachmentList").innerHTML = pendingAttachments.map((file, index) => `<div class="attachment-item"><span>${escapeHtml(file.name)} · ${formatSize(file.size)}</span><button type="button" data-remove-file="${index}">×</button></div>`).join(""); }

const selectedValues = (selector) => [...$(selector).selectedOptions].map((option) => option.value).filter(Boolean);
const optionLabel = (member) => escapeHtml(member.nickname || member.username || "未命名成员");

function resetCreateModal() {
  editingItemId = null;
  pendingAttachments = [];
  $("#createModal").reset();
  $("#createModal h2").textContent = "新建工作项";
  $("#createModal button[type='submit']").textContent = "创建";
  $("#newDescriptionEditor").innerHTML = "";
  $("#attachmentList").innerHTML = "";
}

async function openCreateModal() {
  if (!requireProject()) return;
  resetCreateModal();
  $("#newProject").value = isAllProjects() ? "" : currentProjectId;
  if (!isAllProjects()) await loadCreateOptions(currentProjectId);
  $("#modalBackdrop").classList.remove("hidden");
}

function closeCreateModal() {
  $("#modalBackdrop").classList.add("hidden");
  resetCreateModal();
}

async function loadCreateOptions(projectId) {
  const [nextMembers, nextModules, nextSprints, relatedPage] = await Promise.all([
    api(`/projects/${projectId}/members`), api(`/projects/${projectId}/modules`),
    api(`/projects/${projectId}/sprints`), api(`/projects/${projectId}/work-items?page=1&size=200`),
  ]);
  members = nextMembers || [];
  createModules = nextModules || [];
  createSprints = nextSprints || [];
  createRelatedItems = relatedPage.list || [];
  $("#newType").innerHTML = taskTypes.map((type) => `<option value="${escapeHtml(type.name)}">${escapeHtml(type.name)}</option>`).join("");
  $("#newOwner").innerHTML = `<option value="">未分配</option>${members.map((member) => `<option value="${member.userId}">${optionLabel(member)}</option>`).join("")}`;
  $("#newWatchers").innerHTML = members.map((member) => `<option value="${member.userId}">${optionLabel(member)}</option>`).join("");
  $("#newMainModule").innerHTML = `<option value="">未选择</option>${createModules.filter((module) => !module.parentId).map((module) => `<option value="${module.id}">${escapeHtml(module.name)}</option>`).join("")}`;
  $("#newSubmodule").innerHTML = '<option value="">请先选择主模块</option>';
  $("#newSubmodule").disabled = true;
  $("#newSprint").innerHTML = `<option value="">未选择</option>${createSprints.map((sprint) => `<option value="${sprint.id}">${escapeHtml(sprint.name)}</option>`).join("")}`;
  $("#newRelatedWorkItems").innerHTML = createRelatedItems.map((item) => `<option value="${item.id}">${escapeHtml(item.id)} · ${escapeHtml(item.title)}</option>`).join("");
}

function refreshSubmoduleOptions() {
  const mainId = Number($("#newMainModule").value);
  const submodule = $("#newSubmodule");
  if (!mainId) { submodule.innerHTML = '<option value="">请先选择主模块</option>'; submodule.disabled = true; return; }
  submodule.disabled = false;
  submodule.innerHTML = `<option value="">未选择</option>${createModules.filter((module) => Number(module.parentId) === mainId).map((module) => `<option value="${module.id}">${escapeHtml(module.name)}</option>`).join("")}`;
}

function insertEditorHtml(html) {
  const editor = $("#newDescriptionEditor"); editor.focus(); document.execCommand("insertHTML", false, html);
}

function editorHtmlWithTemporaryImages() {
  return $("#newDescriptionEditor").innerHTML
    .replace(/<img[^>]*data-pending-image="(\d+)"[^>]*>/g, "[[image:temporary-$1]]")
    .replace(/<img[^>]*data-image-id="(\d+)"[^>]*>/g, "[[image:$1]]");
}

function applyTheme(theme) {
  const nextTheme = themeKeys.includes(theme) ? theme : "green";
  document.documentElement.dataset.theme = nextTheme;
  localStorage.setItem("rndTheme", nextTheme);
  $all("[data-theme]").forEach((node) => node.classList.toggle("active", node.dataset.theme === nextTheme));
}

function bindEvents() {
  document.addEventListener("click", async (event) => {
    if (!event.target.closest(".profile")) $("#themeMenu")?.classList.add("hidden");
    if (!event.target.closest("#projectSwitchWrap")) { $("#projectSwitchMenu").classList.add("hidden"); $("#projectSwitch").setAttribute("aria-expanded", "false"); }
    if ($("#detailDrawer").classList.contains("open") && !event.target.closest("#detailDrawer") && !event.target.closest(".modal-backdrop")) closeDetailDrawer();
    const switchProject = event.target.closest("[data-switch-project]");
    if (switchProject) { $("#projectSwitchMenu").classList.add("hidden"); $("#projectSwitch").setAttribute("aria-expanded", "false"); await selectProject(switchProject.dataset.switchProject); return; }
    const quickFieldSelect = event.target.closest("[data-quick-field]"); if (quickFieldSelect) { event.stopPropagation(); return; }
    const select = event.target.closest("[data-select-id]"); if (select) { event.stopPropagation(); select.checked ? selectedIds.add(select.dataset.selectId) : selectedIds.delete(select.dataset.selectId); $("#bulkSelectedCount").textContent = selectedIds.size; $("#bulkUpdateBtn").classList.toggle("hidden", isAllProjects() || !selectedIds.size); return; }
    const all = event.target.closest("[data-select-all]"); if (all) { filteredItems().forEach((item) => all.checked ? selectedIds.add(item.id) : selectedIds.delete(item.id)); renderTable(); return; }
    const open = event.target.closest("[data-open]"); if (open) await openItem(open.dataset.open);
    const project = event.target.closest("[data-project-id]"); if (project) { await selectProject(project.dataset.projectId); return; }
    const createProject = event.target.closest("[data-create-project]"); if (createProject) { openProjectModal(); return; }
    const profileProject = event.target.closest("[data-project-profile]"); if (profileProject) { await showProjectProfile(profileProject.dataset.projectProfile); return; }
    if (event.target.closest("[data-back-projects]")) { await showProjectDirectory(); return; }
    if (event.target.closest("[data-add-milestone]")) { openMilestoneModal(); return; }
    const editMilestone = event.target.closest("[data-edit-milestone]"); if (editMilestone) { openMilestoneModal(activeMilestones.find((m) => Number(m.id) === Number(editMilestone.dataset.editMilestone))); return; }
    const deleteMilestone = event.target.closest("[data-delete-milestone]");
    if (deleteMilestone && window.confirm("确认删除该里程碑？")) { try { await api(`/milestones/${deleteMilestone.dataset.deleteMilestone}`, { method: "DELETE" }); await showProjectProfile(activeProfileProjectId); showToast("里程碑已删除"); } catch (error) { showToast(error.message); } return; }
    const taskView = event.target.closest("[data-task-view]");
    if (taskView) {
      if (!requireProject()) return;
      const fromInsights = isInsightsPage();
      activePage = "tasks";
      if (fromInsights) { applySavedFilterState(null); savedFiltersProjectId = currentProjectId; }
      showListView();
      activeSavedFilterId = null;
      currentView = taskView.dataset.taskView === "all" ? null : taskView.dataset.taskView;
      taskPage = 1;
      await loadCurrentSelection();
      return;
    }
    const view = event.target.closest("[data-view]"); if (view) { if (!requireProject()) return; activeSavedFilterId = null; currentView = view.dataset.view || null; taskPage = 1; await loadCurrentSelection(); }
    const pageButton = event.target.closest("[data-task-page]");
    if (pageButton) {
      taskPage += pageButton.dataset.taskPage === "next" ? 1 : -1;
      taskPage = Math.max(1, taskPage);
      selectedIds.clear();
      await loadCurrentSelection();
      return;
    }
    const managementPageButton = event.target.closest("[data-management-page]");
    if (managementPageButton && !managementPageButton.disabled) {
      const nextPage = Number(managementPageButton.dataset.managementPage);
      if (activePage === "users") userManagementPage = Math.max(1, nextPage);
      if (activePage === "project-management") projectManagementPage = Math.max(1, nextPage);
      if (activePage === "users") renderUserManagementList(); else if (activePage === "project-management") renderProjectManagementList();
      return;
    }
    const page = event.target.closest("[data-page]");
    const editProject = event.target.closest("[data-edit-project]"); if (editProject) { const entry = projectDirectoryEntries.find((item) => item.project.id === Number(editProject.dataset.editProject)); const p = entry?.project; const name = p && window.prompt("项目名称", p.name); if (p && name?.trim()) { await api(`/projects/${p.id}`, { method: "PUT", body: JSON.stringify({ name: name.trim(), shortName: p.shortName, description: p.description || "" }) }); await showProjectDirectory(); } return; }
    const archiveProject = event.target.closest("[data-archive-project]"); if (archiveProject) { archiveProject.disabled = true; try { const projectId = Number(archiveProject.dataset.archiveProject); await api(`/projects/${projectId}/archive?archived=${archiveProject.dataset.archived !== "true"}`, { method: "PUT" }); projects = await api("/projects"); if (currentProjectId === projectId && archiveProject.dataset.archived !== "true") { currentProjectId = null; selectedId = null; $("#detailDrawer").classList.remove("open"); } renderProjectNavigation(); await showProjectDirectory(); showToast(archiveProject.dataset.archived === "true" ? "项目已恢复" : "项目已归档"); } catch (error) { showToast(error.message); archiveProject.disabled = false; } return; }
    const manageMembers = event.target.closest("[data-manage-members]"); if (manageMembers) { await openMemberModal(manageMembers.dataset.manageMembers); return; }
    const removeMember = event.target.closest("[data-remove-member]"); if (removeMember && window.confirm("确认移除该成员？")) { await api(`/projects/${managingProjectId}/members/${removeMember.dataset.removeMember}`, { method: "DELETE" }); await openMemberModal(managingProjectId); return; }
    if (page && Object.hasOwn(insightTitles, page.dataset.page)) {
      await showInsights(page.dataset.page, true);
      return;
    }
    if (page?.dataset.page === "project-management") { await showProjectDirectory(); return; }
    // 分区入口停用：if (page?.dataset.page === "zone-dashboard") { await showZoneDashboard(); return; }
    if (page?.dataset.page === "system-settings") { await showSystemSettings(); return; }
    if (page?.dataset.page === "basic-data") { showBasicData(); return; }
    if (page?.dataset.page === "task-types") { await showTaskTypes(); return; }
    if (page?.dataset.page === "project-types") { await showProjectTypes(); return; }
    const reportSection = event.target.closest("[data-report-section]");
    if (reportSection) { const rows = reportSection.dataset.reportSection === "due" ? dueItems.map(compactItem) : activities.map((item) => `<div class="activity-row"><span class="row-title">${escapeHtml(item.actorName || "系统")} ${escapeHtml(item.content)}</span><span class="row-meta">${formatDate(item.createdAt)}</span></div>`); $("#reportDetail").innerHTML = rows.join("") || "<p>暂无相关数据</p>"; return; }
    if (page?.dataset.page === "users") { await showUserDirectory(); return; }
    const saved = event.target.closest("[data-saved-filter]");
    if (saved) { const filter = savedFilters.find((item) => item.id === Number(saved.dataset.savedFilter)); if (filter) { applySavedFilterState(filter); taskPage = 1; await loadCurrentSelection(); } }
    const rename = event.target.closest("[data-filter-rename]");
    if (rename) {
      const filter = savedFilters.find((item) => item.id === Number(rename.dataset.filterRename));
      const name = filter && window.prompt("新名称", filter.name);
      if (filter && name?.trim()) try { await api(`/saved-filters/${filter.id}`, { method: "PUT", body: JSON.stringify({ name: name.trim(), conditions: filter.conditions, sort: filter.sort, defaultFilter: filter.defaultFilter }) }); await refreshSavedFilters(); } catch (error) { showToast(error.message); }
    }
    const makeDefault = event.target.closest("[data-filter-default]");
    if (makeDefault) try { await api(`/saved-filters/${makeDefault.dataset.filterDefault}/default`, { method: "PUT" }); await refreshSavedFilters(); showToast("已设为默认"); } catch (error) { showToast(error.message); }
    const copyFilter = event.target.closest("[data-filter-copy]");
    if (copyFilter) try { await api(`/saved-filters/${copyFilter.dataset.filterCopy}/copy`, { method: "POST" }); await refreshSavedFilters(); showToast("筛选器已复制"); } catch (error) { showToast(error.message); }
    const removeFilter = event.target.closest("[data-filter-delete]");
    if (removeFilter && window.confirm("确定删除该筛选器？")) try { const id = Number(removeFilter.dataset.filterDelete); await api(`/saved-filters/${id}`, { method: "DELETE" }); if (activeSavedFilterId === id) activeSavedFilterId = null; await refreshSavedFilters(); } catch (error) { showToast(error.message); }
    const action = event.target.closest("[data-action]"); if (action) $("#actionBar")._actions?.[Number(action.dataset.action)]?.[1]();
    const groupToggle = event.target.closest("[data-group-toggle]"); if (groupToggle) { const name = groupToggle.dataset.groupToggle; collapsedGroups.has(name) ? collapsedGroups.delete(name) : collapsedGroups.add(name); renderTable(); }
    const previewAttachmentButton = event.target.closest("[data-preview-attachment]"); if (previewAttachmentButton) { event.preventDefault(); await previewAttachment(previewAttachmentButton.dataset.previewAttachment); return; }
    const download = event.target.closest("[data-download]"); if (download) { event.preventDefault(); downloadAttachment(download.dataset.download, download.dataset.fileName || download.textContent); }
    const notice=event.target.closest("[data-notification-id]");if(notice){await api(`/notifications/${notice.dataset.notificationId}/read`,{method:"PUT"});await loadUnreadCount();if(notice.dataset.notificationItem){$("#notificationModalBackdrop").classList.add("hidden");await openItem(notice.dataset.notificationItem);}else await loadNotifications();}
    const editUser = event.target.closest("[data-edit-user]"); if (editUser) { openUserEditor(JSON.parse(editUser.dataset.editUser)); return; }
    const toggleUser = event.target.closest("[data-toggle-user]"); if (toggleUser) { try { await api(`/users/${toggleUser.dataset.toggleUser}/status`, { method: "PUT", body: JSON.stringify({ status: Number(toggleUser.dataset.status) === 1 ? 0 : 1 }) }); await showUserDirectory(); } catch (error) { showToast(error.message); } return; }
    const resetUser = event.target.closest("[data-reset-user]"); if (resetUser) { if (!window.confirm("确认重置该用户密码？旧密码和已登录会话将失效。")) return; try { const result = await api(`/users/${resetUser.dataset.resetUser}/reset-password`, { method: "POST" }); window.prompt("请复制一次性初始密码并安全交付：", result.initialPassword); } catch (error) { showToast(error.message); } }
  });
  document.addEventListener("change", async (event) => {
    const select = event.target.closest("[data-quick-field]");
    if (select) await saveQuickField(select);
    if (event.target.id === "taskPageSize") {
      taskPageSize = Number(event.target.value);
      localStorage.setItem("rndTaskPageSize", String(taskPageSize));
      taskPage = 1;
      selectedIds.clear();
      await loadCurrentSelection();
    }
  });
  $("#globalSearch").addEventListener("input", () => { renderTable(); renderBoard(); });
  $("#statusFilter").addEventListener("change", () => { renderTable(); renderBoard(); });
  $("#ownerFilter").addEventListener("change", () => { renderTable(); renderBoard(); });
  $("#projectFilter").addEventListener("change", async () => { advancedFilters.projectId = $("#projectFilter").value ? Number($("#projectFilter").value) : undefined; if (!advancedFilters.projectId) delete advancedFilters.projectId; taskPage = 1; await loadAllProjects(); });
  $("#toggleSidebar").addEventListener("click", () => {
    if (isInsightsPage() && matchMedia("(max-width: 760px)").matches) {
      const expanded = $(".app-shell").classList.toggle("insight-menu-open");
      $("#toggleSidebar").setAttribute("aria-expanded", String(expanded));
    } else $(".app-shell").classList.toggle("collapsed-sidebar");
  });
  $("#insightNavBackdrop").addEventListener("click", () => { $(".app-shell").classList.remove("insight-menu-open"); $("#toggleSidebar").setAttribute("aria-expanded", "false"); });
  $("#projectSwitch").addEventListener("click", () => { const menu = $("#projectSwitchMenu"); menu.classList.toggle("hidden"); $("#projectSwitch").setAttribute("aria-expanded", String(!menu.classList.contains("hidden"))); if (!menu.classList.contains("hidden")) { $("#projectSwitchSearch").value = ""; renderProjectSwitchOptions(); $("#projectSwitchSearch").focus(); } });
  $("#projectSwitchSearch").addEventListener("input", () => renderProjectSwitchOptions());
  $("#projectBriefBtn").addEventListener("click", () => showInsights("project-dashboard", true));
  $("#projectModal").addEventListener("submit", createProject);
  ["#closeProjectModal", "#cancelProjectModal"].forEach((id) => $(id).addEventListener("click", () => $("#projectModalBackdrop").classList.add("hidden")));
  $("#milestoneModal").addEventListener("submit", saveMilestone);
  ["#closeMilestoneModal", "#cancelMilestoneModal"].forEach((id) => $(id).addEventListener("click", () => $("#milestoneModalBackdrop").classList.add("hidden")));
  $("#profileAccount").addEventListener("click", (event) => { if (!event.target.closest("#themeToggle, #themeMenu")) openAccountModal(); });
  $("#accountModal").addEventListener("submit", saveAccount);
  ["#closeAccountModal", "#cancelAccountModal"].forEach((id) => $(id).addEventListener("click", () => { $("#accountModalBackdrop").classList.add("hidden"); if ($("#accountModal").dataset.force === "true") logout(); }));
  $("#closeMemberModal").addEventListener("click", () => $("#memberModalBackdrop").classList.add("hidden"));
  $("#addMemberBtn").addEventListener("click", async () => { const userIds = [...$("#memberUserSelect").selectedOptions].map((option) => Number(option.value)).filter(Boolean); if (!userIds.length) return; await Promise.all(userIds.map((userId) => api(`/projects/${managingProjectId}/members`, { method: "POST", body: JSON.stringify({ userId, role: $("#memberRoleSelect").value }) }))); projects = await api("/projects"); renderProjectNavigation(); await openMemberModal(managingProjectId); showToast(`已添加 ${userIds.length} 位成员`); });
  $("#memberSearch").addEventListener("input", renderMemberCandidates);
  $("#memberTable").addEventListener("change", async (event) => { const role = event.target.closest("[data-member-role]"); if (!role) return; await api(`/projects/${managingProjectId}/members/${role.dataset.memberRole}`, { method: "PUT", body: JSON.stringify({ role: role.value }) }); showToast("成员角色已更新"); });
  $("#workItemsTable").addEventListener("input", (event) => {
    if (event.target.id === "projectSearch") { projectManagementPage = 1; renderProjectManagementList(); }
    if (event.target.id === "userSearch") { userManagementPage = 1; renderUserManagementList(); }
    if (event.target.id === "zoneKeyword") { zoneFilters.keyword = event.target.value; renderZonePanels(); }
  });
  $("#workItemsTable").addEventListener("change", (event) => {
    if (["projectStatusFilter", "projectScopeFilter"].includes(event.target.id)) { projectManagementPage = 1; renderProjectManagementList(); }
    if (event.target.id === "userStatusFilter") { userManagementPage = 1; renderUserManagementList(); }
    if (event.target.matches("[data-management-page-size]")) {
      managementPageSize = Number(event.target.value) || 20;
      localStorage.setItem("rndManagementPageSize", String(managementPageSize));
      projectManagementPage = 1; userManagementPage = 1;
      if (activePage === "users") renderUserManagementList(); else if (activePage === "project-management") renderProjectManagementList();
    }
    const zoneFilterKeys = { zoneManager: "managerId", zonePhase: "phase", zoneStatus: "status", zoneHealth: "health", zoneEndFrom: "endFrom", zoneEndTo: "endTo" };
    const key = zoneFilterKeys[event.target.id];
    if (key) { zoneFilters[key] = event.target.value; renderZonePanels(); }
  });
  $("#workItemsTable").addEventListener("input", (event) => { if (event.target.id === "projectCodePrefix") $("#projectCodeExample").textContent = `${event.target.value.toUpperCase() || "PRJ"}-YYYYMM-001`; });
  $("#workItemsTable").addEventListener("submit", async (event) => {
    if (event.target.id === "projectProfileForm") {
      event.preventDefault();
      const body = { name: fieldValue("#profileNameField").trim(), shortName: fieldValue("#profileShortName").trim(), projectType: fieldValue("#profileProjectType"), businessLine: fieldValue("#profileBusinessLine").trim(), customerName: fieldValue("#profileCustomerName").trim(), deliveryLocation: fieldValue("#profileDeliveryLocation").trim(), customerContact: fieldValue("#profileCustomerContact").trim(), projectManagerId: nullableNumber("#profileProjectManager"), implementationLeadId: nullableNumber("#profileImplementationLead"), developmentLeadId: nullableNumber("#profileDevelopmentLead"), phase: fieldValue("#profilePhase"), projectStatus: fieldValue("#profileStatus"), healthStatus: fieldValue("#profileHealth"), plannedStartDate: fieldValue("#profilePlannedStart") || null, plannedEndDate: fieldValue("#profilePlannedEnd") || null, actualStartDate: fieldValue("#profileActualStart") || null, actualEndDate: fieldValue("#profileActualEnd") || null, implementationMode: fieldValue("#profileImplementationMode"), goLiveDate: fieldValue("#profileGoLiveDate") || null, supportEndDate: fieldValue("#profileSupportEndDate") || null, description: fieldValue("#profileDescription"), scope: fieldValue("#profileScope"), deliverables: fieldValue("#profileDeliverables"), acceptanceCriteria: fieldValue("#profileAcceptance"), riskDescription: fieldValue("#profileRisks"), currentIssues: fieldValue("#profileIssues"), nextSteps: fieldValue("#profileNextSteps") };
      try { await api(`/projects/${activeProfileProjectId}`, { method: "PUT", body: JSON.stringify(body) }); await showProjectProfile(activeProfileProjectId); showToast("项目档案已保存"); } catch (error) { showToast(error.message); }
    }
    if (event.target.id === "projectCodeSettingsForm") {
      event.preventDefault();
      try { await api("/settings/project-code", { method: "PUT", body: JSON.stringify({ projectCodePrefix: $("#projectCodePrefix").value.trim().toUpperCase() }) }); await showSystemSettings(); showToast("项目编号规则已保存"); } catch (error) { showToast(error.message); }
    }
    if (event.target.id === "thirdPartyLoginSettingsForm") {
      event.preventDefault();
      const body = { enabled: $("#thirdPartyLoginEnabled").checked, feishuEnabled: $("#thirdPartyFeishuEnabled").checked, dingtalkEnabled: $("#thirdPartyDingtalkEnabled").checked, wecomEnabled: $("#thirdPartyWecomEnabled").checked };
      try { await api("/settings/third-party-login", { method: "PUT", body: JSON.stringify(body) }); await showSystemSettings(); showToast("第三方协同 APP 登录设置已保存"); } catch (error) { showToast(error.message); }
    }
    if (event.target.id === "taskTypeCreateForm") {
      event.preventDefault();
      try { await api("/task-types", { method: "POST", body: JSON.stringify({ name: $("#taskTypeName").value.trim() }) }); taskTypes = await api("/task-types"); await showTaskTypes(); showToast("任务类型已新增"); } catch (error) { showToast(error.message); }
    }
    if (event.target.id === "projectTypeCreateForm") {
      event.preventDefault();
      try { await api("/project-types", { method: "POST", body: JSON.stringify({ name: $("#projectTypeName").value.trim() }) }); projectTypes = await api("/project-types"); await showProjectTypes(); showToast("项目类型已新增"); } catch (error) { showToast(error.message); }
    }
  });
  $("#workItemsTable").addEventListener("click", async (event) => {
    if (event.target.closest("[data-retry-zone-dashboard]")) { await showZoneDashboard(); return; }
    if (event.target.closest("[data-reset-zone-filters]")) { zoneFilters = { keyword: "", managerId: "", phase: "", status: "", health: "", endFrom: "", endTo: "" }; renderZoneDashboard(); return; }
    if (event.target.closest("[data-toggle-zone-view]")) { zoneViewMode = zoneViewMode === "list" ? "card" : "list"; localStorage.setItem("rndZoneViewMode", zoneViewMode); renderZoneDashboard(); return; }
    if (event.target.closest("[data-retry-project-management]")) await showProjectDirectory();
    if (event.target.closest("[data-retry-user-management]")) await showUserDirectory();
    if (event.target.closest("#createUserPageBtn")) openUserEditor();
    const editType = event.target.closest("[data-edit-task-type]");
    if (editType) { const name = window.prompt("修改任务类型名称", editType.dataset.taskTypeName); if (name?.trim()) try { await api(`/task-types/${editType.dataset.editTaskType}`, { method: "PUT", body: JSON.stringify({ name: name.trim() }) }); taskTypes = await api("/task-types"); if (activeType === editType.dataset.taskTypeName) activeType = name.trim(); await showTaskTypes(); showToast("任务类型已修改"); } catch (error) { showToast(error.message); } }
    const toggleType = event.target.closest("[data-toggle-task-type]");
    if (toggleType) try { const enabled = toggleType.dataset.taskTypeEnabled !== "true"; await api(`/task-types/${toggleType.dataset.toggleTaskType}/status?enabled=${enabled}`, { method: "PUT" }); taskTypes = await api("/task-types"); if (!enabled && activeType === toggleType.closest(".task-type-row").querySelector("strong").textContent) activeType = "all"; await showTaskTypes(); showToast(enabled ? "任务类型已启用" : "任务类型已停用"); } catch (error) { showToast(error.message); }
    const editProjectType = event.target.closest("[data-edit-project-type]");
    if (editProjectType) { const name = window.prompt("修改项目类型名称", editProjectType.dataset.projectTypeName); if (name?.trim()) try { await api(`/project-types/${editProjectType.dataset.editProjectType}`, { method: "PUT", body: JSON.stringify({ name: name.trim() }) }); projectTypes = await api("/project-types"); await showProjectTypes(); showToast("项目类型已修改"); } catch (error) { showToast(error.message); } }
    const toggleProjectType = event.target.closest("[data-toggle-project-type]");
    if (toggleProjectType) try { const enabled = toggleProjectType.dataset.projectTypeEnabled !== "true"; await api(`/project-types/${toggleProjectType.dataset.toggleProjectType}/status?enabled=${enabled}`, { method: "PUT" }); projectTypes = await api("/project-types"); await showProjectTypes(); showToast(enabled ? "项目类型已启用" : "项目类型已停用"); } catch (error) { showToast(error.message); }
  });
  $("#userEditModal").addEventListener("submit", saveUser);
  ["#closeUserEditModal", "#cancelUserEditModal"].forEach((id) => $(id).addEventListener("click", () => $("#userEditModalBackdrop").classList.add("hidden")));
  $("#themeToggle").addEventListener("click", (event) => {
    event.stopPropagation();
    const menu = $("#themeMenu");
    menu.classList.toggle("hidden");
    $("#themeToggle").setAttribute("aria-expanded", String(!menu.classList.contains("hidden")));
  });
  $("#themeMenu").addEventListener("click", (event) => {
    const swatch = event.target.closest("[data-theme]");
    if (!swatch) return;
    applyTheme(swatch.dataset.theme);
    $("#themeMenu").classList.add("hidden");
    $("#themeToggle").setAttribute("aria-expanded", "false");
  });
  $("#typeTabs").addEventListener("click", async (event) => { const tab = event.target.closest(".tab"); if (!tab) return; activeType = tab.dataset.type; taskPage = 1; selectedIds.clear(); $all(".tab").forEach((node) => node.classList.toggle("active", node === tab)); await loadCurrentSelection(); });
  $("#toggleViewBtn").addEventListener("click", () => {
    boardMode = !boardMode;
    $(".content").classList.toggle("list-mode", !boardMode);
    $("#listView").classList.toggle("hidden", boardMode);
    $("#boardView").classList.toggle("hidden", !boardMode);
  });
  $("#boardView").addEventListener("dragstart", (event) => {
    const card = event.target.closest("[data-board-card]");
    if (!card || card.getAttribute("draggable") !== "true") return;
    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData("text/plain", card.dataset.boardCard);
    card.classList.add("dragging");
  });
  $("#boardView").addEventListener("dragover", (event) => {
    const column = event.target.closest("[data-board-status]");
    if (!column) return;
    event.preventDefault();
    event.dataTransfer.dropEffect = "move";
    column.classList.add("drag-over");
  });
  $("#boardView").addEventListener("dragleave", (event) => {
    const column = event.target.closest("[data-board-status]");
    if (column && !column.contains(event.relatedTarget)) column.classList.remove("drag-over");
  });
  $("#boardView").addEventListener("drop", async (event) => {
    const column = event.target.closest("[data-board-status]");
    if (!column) return;
    event.preventDefault();
    $all(".board-column.drag-over").forEach((node) => node.classList.remove("drag-over"));
    await moveBoardCardToStatus(event.dataTransfer.getData("text/plain"), column.dataset.boardStatus);
  });
  $("#boardView").addEventListener("dragend", () => {
    $all(".board-card.dragging").forEach((node) => node.classList.remove("dragging"));
    $all(".board-column.drag-over").forEach((node) => node.classList.remove("drag-over"));
  });
  $("#bulkUpdateBtn").addEventListener("click", openBulkModal);
  ["#closeBulkModal", "#cancelBulkModal"].forEach((id) => $(id).addEventListener("click", () => $("#bulkModalBackdrop").classList.add("hidden")));
  $("#bulkModal").addEventListener("submit", async (event) => {
    event.preventDefault(); const body = { ids: [...selectedIds] }; const fields = { status: $("#bulkStatus").value, ownerId: $("#bulkOwner").value, priority: $("#bulkPriority").value, sprintId: $("#bulkSprint").value, module: $("#bulkModule").value.trim() };
    Object.entries(fields).forEach(([key, value]) => { if (value !== "") body[key] = ["ownerId", "sprintId"].includes(key) ? Number(value) : value; }); const addTags = splitTags($("#bulkAddTags").value), removeTags = splitTags($("#bulkRemoveTags").value); if (addTags.length) body.addTags = addTags; if (removeTags.length) body.removeTags = removeTags;
    try { const result = await api(`/projects/${currentProjectId}/work-items/bulk-update`, { method: "POST", body: JSON.stringify(body) }); selectedIds = new Set(result.failures.map((item) => item.id)); $("#bulkResult").textContent = `成功 ${result.successes.length} 项，失败 ${result.failures.length} 项${result.failures.length ? "：" + result.failures.map((item) => `${item.id} ${item.reason}`).join("；") : ""}`; await loadProject(currentProjectId); if (!result.failures.length) $("#bulkModalBackdrop").classList.add("hidden"); } catch (error) { $("#bulkResult").textContent = error.message; }
  });
  $("#bulkDeleteBtn").addEventListener("click",async()=>{if(!window.confirm("批量删除不可恢复，是否继续？"))return;const confirmation=window.prompt("请输入：确认删除");if(confirmation!=="确认删除")return;try{const result=await api(`/projects/${currentProjectId}/work-items/bulk-delete`,{method:"POST",body:JSON.stringify({ids:[...selectedIds],confirmation})});selectedIds=new Set(result.failures.map((x)=>x.id));$("#bulkResult").textContent=`删除成功 ${result.successes.length} 项，失败 ${result.failures.length} 项`;await loadProject(currentProjectId);if(!result.failures.length)$("#bulkModalBackdrop").classList.add("hidden");}catch(error){$("#bulkResult").textContent=error.message;}});
  $("#advancedFilterBtn").addEventListener("click", openFilterModal);
  $("#viewSettingsBtn").addEventListener("click", openViewModal);
  ["#closeViewModal", "#cancelViewModal"].forEach((id) => $(id).addEventListener("click", () => $("#viewModalBackdrop").classList.add("hidden")));
  $("#columnSettings").addEventListener("click", (event) => {
    const up = event.target.closest("[data-column-up]"); const down = event.target.closest("[data-column-down]"); const row = event.target.closest(".column-setting");
    if (up && row?.previousElementSibling) row.parentNode.insertBefore(row, row.previousElementSibling);
    if (down && row?.nextElementSibling) row.parentNode.insertBefore(row.nextElementSibling, row);
  });
  $("#viewModal").addEventListener("submit", async (event) => {
    event.preventDefault(); const columns = [...$("#columnSettings").querySelectorAll("[data-column]:checked")].map((node) => node.dataset.column);
    if (!columns.length) return showToast("至少保留一个可配置列");
    if (isAllProjects()) { $("#viewModalBackdrop").classList.add("hidden"); return showToast("全部项目视图使用统一列配置"); }
    try { viewPreference = await api(`/projects/${currentProjectId}/view-preference`, { method: "PUT", body: JSON.stringify({ columns, sort: $("#viewSort").value, groupBy: $("#viewGroup").value || null }) }); currentSort = viewPreference.sort; collapsedGroups = new Set(); $("#viewModalBackdrop").classList.add("hidden"); await loadCurrentSelection(); showToast("视图设置已保存"); } catch (error) { showToast(error.message); }
  });
  $("#saveFilterBtn").addEventListener("click", saveCurrentFilter);
  ["#closeFilterModal", "#cancelFilterBtn"].forEach((id) => $(id).addEventListener("click", () => $("#filterModalBackdrop").classList.add("hidden")));
  $("#clearFilterBtn").addEventListener("click", async () => { applySavedFilterState(null); taskPage = 1; $("#filterModalBackdrop").classList.add("hidden"); await loadCurrentSelection(); });
  $("#filterModal").addEventListener("submit", async (event) => {
    event.preventDefault(); activeSavedFilterId = null; currentView = $("#filterView").value || null; advancedFilters = readFilterForm(); currentSort = $("#filterSort").value; taskPage = 1;
    if (advancedFilters.projectId && !isAllProjects() && Number(advancedFilters.projectId) !== Number(currentProjectId)) currentProjectId = ALL_PROJECTS;
    $("#filterModalBackdrop").classList.add("hidden");
    if (!isAllProjects() && currentSort !== viewPreference.sort) { viewPreference = await api(`/projects/${currentProjectId}/view-preference`, { method: "PUT", body: JSON.stringify({ columns: viewPreference.columns, sort: currentSort, groupBy: viewPreference.groupBy || null }) }); }
    await loadCurrentSelection();
  });
  $("#createBtn").addEventListener("click", openCreateModal);
  ["#closeModal", "#cancelCreate"].forEach((id) => $(id).addEventListener("click", closeCreateModal));
  $("#openTaskFlowGuide").addEventListener("click", () => $("#taskFlowGuideBackdrop").classList.remove("hidden"));
  $("#closeTaskFlowGuide").addEventListener("click", () => $("#taskFlowGuideBackdrop").classList.add("hidden"));
  $("#taskFlowGuideBackdrop").addEventListener("click", (event) => { if (event.target === event.currentTarget) event.currentTarget.classList.add("hidden"); });
  $("#closeDrawer").addEventListener("click", closeDetailDrawer);
  $(".drawer-tabs").addEventListener("click", (event) => { const tab = event.target.closest(".drawer-tab"); if (!tab) return; drawerTab = tab.dataset.drawerTab; $all(".drawer-tab").forEach((node) => node.classList.toggle("active", node === tab)); renderDrawer(); });
  $("#newFiles").addEventListener("change", (event) => addAttachments([...event.target.files]));
  $("#pickFilesBtn").addEventListener("click", () => $("#newFiles").click());
  $("#newProject").addEventListener("change", async () => { const projectId = Number($("#newProject").value); if (projectId) await loadCreateOptions(projectId); });
  $("#newMainModule").addEventListener("change", refreshSubmoduleOptions);
  $(".rich-toolbar").addEventListener("click", (event) => { const button = event.target.closest("[data-editor-command]"); if (!button) return; document.execCommand(button.dataset.editorCommand, false, button.dataset.editorValue || null); $("#newDescriptionEditor").focus(); });
  $("#insertLinkBtn").addEventListener("click", () => { const url = window.prompt("请输入链接地址（http://、https:// 或 mailto:）"); if (url) document.execCommand("createLink", false, url); });
  $("#insertTableBtn").addEventListener("click", () => insertEditorHtml("<table><tbody><tr><th>字段</th><th>内容</th></tr><tr><td>示例</td><td>请填写</td></tr></tbody></table><p><br></p>"));
  $("#newDescriptionEditor").addEventListener("click", (event) => { const image = event.target.closest(".inline-description-image"); if (!image?.src) return; openImagePreview(image.src); });
  $("#newDescriptionEditor").addEventListener("paste", (event) => { const images = [...event.clipboardData.files].filter((file) => file.type.startsWith("image/")); if (!images.length) return; event.preventDefault(); const start = pendingAttachments.length; addAttachments(images); images.forEach((file, index) => insertEditorHtml(`<img src="${URL.createObjectURL(file)}" data-pending-image="${start + index}" alt="待上传图片" />`)); showToast(`已添加 ${images.length} 张图片`); });
  $("#attachmentList").addEventListener("click", (event) => { const button = event.target.closest("[data-remove-file]"); if (!button) return; pendingAttachments.splice(Number(button.dataset.removeFile), 1); addAttachments([]); });
  $("#commentBtn").addEventListener("click", async () => { const value = $("#commentInput").value.trim(); if (!value || !selectedId) return; await mutate(`/work-items/${selectedId}/comments`, { method: "POST", body: JSON.stringify({ content: value, mentions: [] }) }); $("#commentInput").value = ""; drawerTab = "activity"; renderDrawer(); });
  $("#drawerBody").addEventListener("click", (event) => { const image = event.target.closest(".inline-description-image"); if (!image?.src) return; openImagePreview(image.src); });
  $("#closeImagePreview").addEventListener("click", closeImagePreview);
  $("#imagePreviewBackdrop").addEventListener("click", (event) => { if (event.target === event.currentTarget) closeImagePreview(); });
  $("#imageZoomIn").addEventListener("click", () => changeImageZoom(0.25));
  $("#imageZoomOut").addEventListener("click", () => changeImageZoom(-0.25));
  $("#imageZoomReset").addEventListener("click", () => setImageZoom(1));
  $(".image-preview-stage").addEventListener("mousedown", startImagePreviewDrag);
  $(".image-preview-stage").addEventListener("wheel", zoomImagePreviewByWheel, { passive: false });
  document.addEventListener("mousemove", moveImagePreviewDrag);
  document.addEventListener("mouseup", stopImagePreviewDrag);
  $("#createModal").addEventListener("submit", createWorkItem);
  $("#notifyBtn").addEventListener("click",async()=>{$("#notificationModalBackdrop").classList.remove("hidden");await loadNotifications();});
  $("#closeNotificationModal").addEventListener("click",()=>$("#notificationModalBackdrop").classList.add("hidden"));
  $("#notificationRead").addEventListener("change",loadNotifications);$("#notificationType").addEventListener("change",loadNotifications);
  $("#readAllNotifications").addEventListener("click",async()=>{await api("/notifications/read-all",{method:"PUT"});await loadNotifications();await loadUnreadCount();});
  $("#exportBtn").addEventListener("click",exportCurrentView);
  $("#logoutBtn").addEventListener("click", logout);
}

async function createWorkItem(event) {
  event.preventDefault();
  if (editingItemId) {
    try {
      const uploaded = [];
      for (const file of pendingAttachments) { const form = new FormData(); form.append("file", file); uploaded.push(await api(`/work-items/${editingItemId}/attachments`, { method: "POST", body: form })); }
      const refs = uploaded.filter((file) => file.mimeType?.startsWith("image/")).map((file) => `[[image:${file.id}]]`).join("\n");
      await api(`/work-items/${editingItemId}`, { method: "PUT", body: JSON.stringify({ description: editorHtmlWithTemporaryImages().replace(/\[\[image:temporary-(\d+)\]\]/g, (_, index) => `[[image:${uploaded[Number(index)]?.id || ""}]]`) }) });
      const id = editingItemId; closeCreateModal(); await loadCurrentSelection(); await openItem(id); showToast("图文已更新");
    } catch (error) { showToast(error.message); }
    return;
  }
  const projectId = Number($("#newProject").value);
  const description = editorHtmlWithTemporaryImages();
  if (!$("#newTitle").value.trim() || !description.replace(/<[^>]+>/g, "").replace(/\[\[image:[^]]+\]\]/g, "").trim()) return showToast("请填写任务标题和任务描述");
  const hoursValue = $("#newEstimatedHours").value;
  if (hoursValue && (Number(hoursValue) < 0 || !Number.isInteger(Number(hoursValue) * 2))) return showToast("预计工时需为非负的 0.5 小时粒度");
  const actualHoursValue = $("#newActualHours").value;
  if (actualHoursValue && (Number(actualHoursValue) < 0 || !/^\d+(\.\d{1,2})?$/.test(actualHoursValue))) return showToast("实际花费工时需为非负数，最多保留两位小数");
  const due = $("#newDue").value ? new Date(`${$("#newDue").value}T23:59:59`).toISOString() : null;
  const actualCompletedAt = $("#newActualCompletedAt").value ? new Date($("#newActualCompletedAt").value).toISOString() : null;
  try {
    const item = await api(`/projects/${projectId}/work-items`, { method: "POST", body: JSON.stringify({
      type: $("#newType").value, title: $("#newTitle").value.trim(), priority: $("#newPriority").value,
      ownerId: $("#newOwner").value ? Number($("#newOwner").value) : null, dueDate: due,
      description, steps: [], tags: $("#newTags").value.split(",").map((tag) => tag.trim()).filter(Boolean),
      watcherIds: selectedValues("#newWatchers").map(Number), moduleId: $("#newMainModule").value ? Number($("#newMainModule").value) : null,
      submoduleId: $("#newSubmodule").value ? Number($("#newSubmodule").value) : null, sprintId: $("#newSprint").value ? Number($("#newSprint").value) : null,
      estimatedHours: hoursValue === "" ? null : Number(hoursValue), plannedStartDate: $("#newPlannedStartDate").value || null,
      actualCompletedAt, actualHours: actualHoursValue === "" ? null : Number(actualHoursValue), relatedWorkItemIds: selectedValues("#newRelatedWorkItems"),
    }) });
    const uploaded = []; for (const file of pendingAttachments) { const form = new FormData(); form.append("file", file); uploaded.push(await api(`/work-items/${item.id}/attachments`, { method: "POST", body: form })); }
    const finalDescription = description.replace(/\[\[image:temporary-(\d+)\]\]/g, (_, index) => `[[image:${uploaded[Number(index)]?.id || ""}]]`);
    if (finalDescription !== description) await api(`/work-items/${item.id}`, { method: "PUT", body: JSON.stringify({ description: finalDescription }) });
    closeCreateModal();
    await loadCurrentSelection(); await openItem(item.id); showToast("工作项已创建");
  } catch (error) { showToast(error.message); }
}

async function downloadAttachment(id, name) {
  try { const response = await fetch(`${API_BASE}/attachments/${id}/download`, { headers: { Authorization: `Bearer ${token}` } }); if (!response.ok) throw new Error("附件下载失败"); const url = URL.createObjectURL(await response.blob()); const link = document.createElement("a"); link.href = url; link.download = name; link.click(); URL.revokeObjectURL(url); }
  catch (error) { showToast(error.message); }
}

async function previewAttachment(id) {
  try {
    const response = await fetch(`${API_BASE}/attachments/${id}/download`, { headers: { Authorization: `Bearer ${token}` } });
    if (!response.ok) throw new Error("附件预览失败");
    if (imagePreviewObjectUrl) URL.revokeObjectURL(imagePreviewObjectUrl);
    imagePreviewObjectUrl = URL.createObjectURL(await response.blob());
    openImagePreview(imagePreviewObjectUrl, true);
  } catch (error) { showToast(error.message); }
}

function openImagePreview(src, ownsObjectUrl = false) {
  if (imagePreviewObjectUrl && imagePreviewObjectUrl !== src) URL.revokeObjectURL(imagePreviewObjectUrl);
  imagePreviewObjectUrl = ownsObjectUrl ? src : null;
  $("#imagePreviewFull").src = src;
  $("#imagePreviewBackdrop").classList.remove("hidden");
  setImageZoom(1);
  $(".image-preview-stage").scrollTo(0, 0);
}

function setImageZoom(scale) {
  imagePreviewScale = Math.max(0.5, Math.min(3, scale));
  $("#imagePreviewFull").style.width = `${imagePreviewScale * 100}%`;
  $("#imagePreviewFull").style.maxHeight = imagePreviewScale === 1 ? "80vh" : "none";
  $(".image-preview-stage").classList.toggle("zoomed", imagePreviewScale > 1);
  if (imagePreviewScale === 1) $(".image-preview-stage").scrollTo(0, 0);
  $("#imageZoomLabel").textContent = `${Math.round(imagePreviewScale * 100)}%`;
}

function changeImageZoom(delta) {
  setImageZoom(imagePreviewScale + delta);
}

function zoomImagePreviewByWheel(event) {
  event.preventDefault();
  changeImageZoom(event.deltaY < 0 ? 0.25 : -0.25);
}

function closeImagePreview() {
  $("#imagePreviewBackdrop").classList.add("hidden");
  stopImagePreviewDrag();
  if (imagePreviewObjectUrl) {
    URL.revokeObjectURL(imagePreviewObjectUrl);
    imagePreviewObjectUrl = null;
  }
  setImageZoom(1);
}

function startImagePreviewDrag(event) {
  if (imagePreviewScale <= 1 || !event.target.closest("#imagePreviewFull")) return;
  const stage = $(".image-preview-stage");
  imagePreviewDrag = {
    x: event.clientX,
    y: event.clientY,
    left: stage.scrollLeft,
    top: stage.scrollTop,
  };
  stage.classList.add("dragging");
  event.preventDefault();
}

function moveImagePreviewDrag(event) {
  if (!imagePreviewDrag) return;
  const stage = $(".image-preview-stage");
  stage.scrollLeft = imagePreviewDrag.left - (event.clientX - imagePreviewDrag.x);
  stage.scrollTop = imagePreviewDrag.top - (event.clientY - imagePreviewDrag.y);
}

function stopImagePreviewDrag() {
  imagePreviewDrag = null;
  $(".image-preview-stage")?.classList.remove("dragging");
}

installLoginView();
applyTheme(localStorage.getItem("rndTheme"));
bindEvents();
bindInsightsEvents();
if (token) initializeApp(); else completeExternalAuth().then((handled) => { if (!handled) showLogin(); });
