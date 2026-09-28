const people = ["未分配", "李娜", "王强", "陈晨", "刘洋", "周涛", "张伟"];

const projects = [
  { id: "aurora", name: "Aurora 研发平台", short: "A", tone: "" },
  { id: "nebula", name: "Nebula 支付平台", short: "N", tone: "green" },
  { id: "orbit", name: "Orbit 数据中台", short: "O", tone: "orange" },
];

let workItems = [
  {
    id: "BUG-2412",
    type: "缺陷",
    title: "导出报表时间格式不正确",
    status: "新提交",
    owner: "未分配",
    priority: "P1",
    sprint: "Sprint 23",
    due: "今天 10:00",
    module: "报表中心",
    severity: "严重",
    creator: "李娜",
    desc: "导出 Excel 报表时，时间列显示为 1970-01-01 08:00:00，与实际时间不符。",
    expected: "时间列应显示正确的本地时间格式。",
    actual: "时间列显示为 1970-01-01 08:00:00。",
    steps: ["进入报表中心", "选择任意报表并导出为 Excel", "打开导出的文件", "查看时间列数据"],
    tags: ["报表", "Excel", "高频"],
    attachments: [],
    comments: ["李娜：客户演示前必须修复。"],
  },
  {
    id: "BUG-2408",
    type: "缺陷",
    title: "文件上传进度显示异常",
    status: "待验收",
    owner: "张伟",
    priority: "P2",
    sprint: "Sprint 23",
    due: "昨天 17:00",
    module: "附件服务",
    severity: "一般",
    creator: "陈晨",
    desc: "大文件上传时进度条偶发回退，影响用户判断。",
    expected: "进度只递增，上传完成后稳定显示 100%。",
    actual: "进度在 67% 附近回退到 42%。",
    steps: ["选择 200MB 以上附件", "观察上传进度", "弱网环境下重复上传"],
    tags: ["附件", "弱网"],
    attachments: [
      { name: "上传进度异常录屏.mp4", size: 12400000, type: "video/mp4", url: "" },
      { name: "弱网环境截图.png", size: 1260000, type: "image/png", url: "" },
    ],
    comments: ["王强：已补充弱网重试逻辑，请验收。"],
  },
  {
    id: "TASK-1128",
    type: "任务",
    title: "用户登录接口开发",
    status: "处理中",
    owner: "王强",
    priority: "P1",
    sprint: "Sprint 23",
    due: "今天 14:00",
    module: "账号体系",
    severity: "重要",
    creator: "张伟",
    desc: "完成账号密码登录、失败次数限制和基础审计日志。",
    expected: "接口通过联调并补齐错误码。",
    actual: "开发中。",
    steps: ["定义接口契约", "实现登录校验", "补充审计日志", "联调前端"],
    tags: ["后端", "账号"],
    attachments: [],
    comments: ["王强：错误码已整理。"],
  },
  {
    id: "REQ-1003",
    type: "需求",
    title: "批量导入需求",
    status: "待评估",
    owner: "张伟",
    priority: "P2",
    sprint: "Sprint 24",
    due: "明天 09:30",
    module: "需求管理",
    severity: "一般",
    creator: "刘洋",
    desc: "支持通过模板批量导入需求，并返回失败原因。",
    expected: "产品与研发确认范围。",
    actual: "待评估。",
    steps: ["确认模板字段", "确认错误提示", "确认权限边界"],
    tags: ["导入", "需求"],
    attachments: [],
    comments: ["刘洋：希望 Sprint 24 能进入开发。"],
  },
  {
    id: "CASE-889",
    type: "测试",
    title: "搜索接口性能测试",
    status: "待测试",
    owner: "刘洋",
    priority: "P2",
    sprint: "Sprint 23",
    due: "明天 11:00",
    module: "搜索服务",
    severity: "一般",
    creator: "陈晨",
    desc: "覆盖关键词搜索、筛选搜索和分页搜索的接口压测。",
    expected: "输出压测报告和瓶颈定位。",
    actual: "测试准备中。",
    steps: ["准备测试数据", "配置压测脚本", "执行压测", "记录瓶颈"],
    tags: ["性能", "测试"],
    attachments: [],
    comments: ["刘洋：数据集已准备一半。"],
  },
  {
    id: "TASK-1122",
    type: "任务",
    title: "权限校验中间件重构",
    status: "已分配",
    owner: "陈晨",
    priority: "P3",
    sprint: "Sprint 24",
    due: "5-28",
    module: "权限中心",
    severity: "普通",
    creator: "张伟",
    desc: "统一服务端权限校验逻辑，减少重复判断。",
    expected: "不改变现有权限行为。",
    actual: "待开始。",
    steps: ["梳理现有判断", "抽取中间件", "补充回归测试"],
    tags: ["技术债"],
    attachments: [],
    comments: [],
  },
  {
    id: "BUG-2405",
    type: "缺陷",
    title: "列表页排序偶现错误",
    status: "已关闭",
    owner: "陈晨",
    priority: "P3",
    sprint: "Sprint 22",
    due: "5-20",
    module: "工作项",
    severity: "普通",
    creator: "周涛",
    desc: "在快速切换筛选条件后，更新时间排序偶尔不正确。",
    expected: "排序结果稳定。",
    actual: "已关闭。",
    steps: ["切换状态筛选", "切换负责人筛选", "观察排序"],
    tags: ["列表"],
    attachments: [],
    comments: ["周涛：回归通过。"],
  },
  {
    id: "REQ-1001",
    type: "需求",
    title: "用户权限需求",
    status: "已完成",
    owner: "李娜",
    priority: "P2",
    sprint: "Sprint 24",
    due: "明天到期",
    module: "权限中心",
    severity: "一般",
    creator: "张伟",
    desc: "定义项目成员、管理员、访客三类权限。",
    expected: "权限模型可支撑首版上线。",
    actual: "已完成。",
    steps: ["角色定义", "权限矩阵", "验收确认"],
    tags: ["权限"],
    attachments: [],
    comments: ["张伟：权限矩阵已归档。"],
  },
];

workItems = [
  ...workItems.map((item) => ({ ...item, projectId: "aurora" })),
  {
    id: "TASK-2101",
    projectId: "nebula",
    type: "任务",
    title: "支付回调幂等处理",
    status: "处理中",
    owner: "王强",
    priority: "P1",
    sprint: "Sprint 18",
    due: "今天 16:00",
    module: "支付网关",
    severity: "重要",
    creator: "张伟",
    desc: "补齐支付回调重复通知场景下的幂等保护。",
    expected: "重复回调不会产生重复订单状态变更。",
    actual: "开发中。",
    steps: ["确认回调协议", "实现幂等键", "补充回归测试"],
    tags: ["支付", "后端"],
    attachments: [],
    comments: ["张伟：优先保证线上回调稳定。"],
  },
  {
    id: "BUG-3108",
    projectId: "nebula",
    type: "缺陷",
    title: "退款状态偶发不同步",
    status: "待验收",
    owner: "刘洋",
    priority: "P2",
    sprint: "Sprint 18",
    due: "明天 12:00",
    module: "退款中心",
    severity: "一般",
    creator: "李娜",
    desc: "第三方退款成功后，订单详情页偶发仍显示处理中。",
    expected: "退款成功后订单详情状态同步更新。",
    actual: "已提交修复，待验收。",
    steps: ["发起退款", "等待第三方回调", "刷新订单详情"],
    tags: ["退款", "状态同步"],
    attachments: [],
    comments: ["刘洋：准备回归。"],
  },
  {
    id: "REQ-2201",
    projectId: "orbit",
    type: "需求",
    title: "指标血缘关系视图",
    status: "待评估",
    owner: "张伟",
    priority: "P2",
    sprint: "Sprint 9",
    due: "明天 15:00",
    module: "指标管理",
    severity: "一般",
    creator: "陈晨",
    desc: "支持查看指标引用来源、下游报表和影响范围。",
    expected: "产品确认首版血缘视图范围。",
    actual: "待评估。",
    steps: ["确认实体关系", "确定首版字段", "评估开发量"],
    tags: ["指标", "血缘"],
    attachments: [],
    comments: [],
  },
];

let currentProjectId = "aurora";
let selectedId = "BUG-2412";
let activeType = "all";
let boardMode = false;
let drawerTab = "details";
let pendingAttachments = [];

const statusFlow = ["新提交", "待评估", "已分配", "处理中", "待测试", "测试中", "待验收", "已完成", "已关闭"];

const typeClass = {
  需求: "green",
  任务: "blue",
  测试: "purple",
  缺陷: "red",
};

function $(selector) {
  return document.querySelector(selector);
}

function $all(selector) {
  return [...document.querySelectorAll(selector)];
}

function getSelected() {
  return workItems.find((item) => item.id === selectedId) || projectItems()[0] || workItems[0];
}

function getCurrentProject() {
  return projects.find((project) => project.id === currentProjectId) || projects[0];
}

function getItemProject(item) {
  return projects.find((project) => project.id === item.projectId) || getCurrentProject();
}

function projectItems() {
  return workItems.filter((item) => item.projectId === currentProjectId);
}

function chip(text, tone = "") {
  return `<span class="chip ${tone}">${text}</span>`;
}

function ownerCell(name) {
  if (name === "未分配") return `<span class="chip orange">未分配</span>`;
  return `<span class="owner"><span class="mini-avatar">${name.slice(0, 1)}</span>${name}</span>`;
}

function filteredItems() {
  const keyword = $("#globalSearch").value.trim().toLowerCase();
  const status = $("#statusFilter").value;
  const owner = $("#ownerFilter").value;

  return projectItems().filter((item) => {
    const matchType = activeType === "all" || item.type === activeType;
    const matchKeyword =
      !keyword ||
      item.title.toLowerCase().includes(keyword) ||
      item.id.toLowerCase().includes(keyword) ||
      item.module.toLowerCase().includes(keyword);
    const matchStatus = status === "all" || item.status === status;
    const matchOwner = owner === "all" || item.owner === owner;
    return matchType && matchKeyword && matchStatus && matchOwner;
  });
}

function renderSummaries() {
  const items = projectItems();
  $("#todoCount").textContent = items.filter((item) => ["新提交", "处理中", "待验收"].includes(item.status)).length;
  $("#inboxCount").textContent = items.filter((item) => item.status === "新提交").length;
}

function renderTopPanels() {
  const items = projectItems();
  const todo = items.filter((item) => item.owner === "张伟" || item.owner === "未分配").slice(0, 4);
  $("#todoList").innerHTML = todo
    .map(
      (item) => `
        <button class="compact-row" data-open="${item.id}">
          <span class="row-main">
            <span class="row-title">${item.title}</span>
            ${chip(item.type, typeClass[item.type])}
          </span>
          <span class="row-meta"><span>${item.status}</span><span>${item.due}</span><span>${item.priority}</span></span>
        </button>
      `,
    )
    .join("");

  $("#activityList").innerHTML = [
    `${getCurrentProject().name} 有新的工作项更新`,
    "王强 完成了一个开发任务",
    "张伟 更新了需求范围",
    "刘洋 提交了测试反馈",
  ]
    .map(
      (text, index) => `
        <div class="activity-row">
          <span class="row-main"><span class="row-title">${text}</span><span class="row-meta">${index === 0 ? "10:24" : "昨天"}</span></span>
        </div>
      `,
    )
    .join("");

  $("#dueList").innerHTML = items
    .filter((item) => item.due.includes("今天") || item.due.includes("昨天") || item.due.includes("明天"))
    .slice(0, 4)
    .map(
      (item) => `
        <button class="compact-row" data-open="${item.id}">
          <span class="row-main">
            <span class="row-title">${item.title}</span>
            <span class="chip ${item.due.includes("昨天") ? "red" : "orange"}">${item.due}</span>
          </span>
          <span class="row-meta"><span>${item.type}</span><span>${item.owner}</span><span>${item.status}</span></span>
        </button>
      `,
    )
    .join("");
}

function renderTable() {
  const rows = filteredItems();
  $("#workItemsTable").innerHTML = rows
    .map(
      (item) => `
        <button class="table-row ${item.id === selectedId ? "selected" : ""}" data-open="${item.id}">
          <span><input type="checkbox" data-check="${item.id}" /></span>
          <span>${item.id}</span>
          <span class="item-title">${item.title}</span>
          <span>${chip(item.type, typeClass[item.type])}</span>
          <span>${chip(item.status, item.status === "新提交" ? "orange" : item.status.includes("完成") || item.status.includes("关闭") ? "green" : "blue")}</span>
          <span>${ownerCell(item.owner)}</span>
          <span>${chip(item.priority, item.priority === "P1" || item.priority === "P0" ? "red" : "orange")}</span>
          <span>${item.sprint}</span>
          <span class="${item.due.includes("昨天") || item.due.includes("今天") ? "chip red" : ""}">${item.due}</span>
        </button>
      `,
    )
    .join("");
}

function renderBoard() {
  const columns = ["新提交", "已分配", "处理中", "待测试", "待验收", "已完成"];
  $("#boardView").innerHTML = columns
    .map((status) => {
      const items = projectItems().filter((item) => item.status === status || (status === "新提交" && item.status === "待评估"));
      return `
        <div class="board-column">
          <div class="column-title"><span>${status}</span><span>${items.length}</span></div>
          ${items
            .map(
              (item) => `
                <button class="board-card ${item.id === selectedId ? "selected" : ""}" data-open="${item.id}">
                  <span class="row-meta"><span>${item.id}</span>${chip(item.type, typeClass[item.type])}</span>
                  <strong>${item.title}</strong>
                  <span class="row-meta"><span>${item.priority}</span><span>${item.owner}</span><span>${item.due}</span></span>
                </button>
              `,
            )
            .join("")}
          <button class="text-button" data-add-status="${status}">+ 添加工作项</button>
        </div>
      `;
    })
    .join("");
}

function renderActionBar(item) {
  const actions = [];
  if (item.owner === "未分配") actions.push(["分配给我", () => updateItem(item.id, { owner: "张伟", status: "已分配" })]);
  if (["新提交", "待评估", "已分配"].includes(item.status)) actions.push(["接受并分配", () => updateItem(item.id, { owner: "张伟", status: "已分配" })]);
  if (["已分配", "待评估"].includes(item.status)) actions.push(["开始处理", () => updateItem(item.id, { status: "处理中" })]);
  if (item.status === "处理中") actions.push(["提交测试", () => updateItem(item.id, { status: "待测试" })]);
  if (item.status === "待测试") actions.push(["测试通过", () => updateItem(item.id, { status: "待验收" })]);
  if (item.status === "待验收") actions.push(["验收通过", () => updateItem(item.id, { status: "已完成" })]);
  if (!["已完成", "已关闭"].includes(item.status)) actions.push(["拒绝", () => updateItem(item.id, { status: "已关闭" })]);
  if (["已完成", "已关闭"].includes(item.status)) actions.push(["重新打开", () => updateItem(item.id, { status: "新提交" })]);

  $("#actionBar").innerHTML = actions
    .map((action, index) => `<button class="${index === 0 ? "primary-button" : "secondary-button"}" data-action="${index}">${action[0]}</button>`)
    .join("");
  $("#actionBar")._actions = actions;
}

function renderDrawer() {
  const item = getSelected();
  const project = getItemProject(item);
  $("#drawerMeta").textContent = `${project.name} · ${item.type} #${item.id}`;
  $("#drawerTitle").textContent = item.title;
  renderActionBar(item);

  if (drawerTab === "details") {
    $("#drawerBody").innerHTML = `
      <section class="detail-section">
        <h3>基本信息</h3>
        <div class="field-grid">
          <div class="field"><span>所属项目</span>${project.name}</div>
          <div class="field"><span>状态</span>${chip(item.status, "blue")}</div>
          <div class="field"><span>优先级</span>${chip(item.priority, item.priority === "P1" || item.priority === "P0" ? "red" : "orange")}</div>
          <div class="field"><span>类型</span>${chip(item.type, typeClass[item.type])}</div>
          <div class="field"><span>严重程度</span>${item.severity}</div>
          <div class="field"><span>模块</span>${item.module}</div>
          <div class="field"><span>所属迭代</span>${item.sprint}</div>
          <div class="field"><span>负责人</span>${ownerCell(item.owner)}</div>
          <div class="field"><span>创建人</span>${item.creator}</div>
        </div>
      </section>
      <section class="detail-section">
        <h3>描述</h3>
        <p>${item.desc}</p>
      </section>
      <section class="detail-section">
        <h3>验收清单</h3>
        <div class="check-list">
          ${item.steps.map((step, index) => `<label class="check-row"><input type="checkbox" ${index < 2 ? "checked" : ""} />${step}</label>`).join("")}
        </div>
      </section>
      <section class="detail-section">
        <h3>期望结果</h3>
        <p>${item.expected}</p>
        <h3>实际结果</h3>
        <p>${item.actual}</p>
      </section>
      <section>
        <h3>标签</h3>
        <div class="row-meta">${item.tags.map((tag) => chip(tag)).join("")}</div>
      </section>
    `;
  } else if (drawerTab === "activity") {
    $("#drawerBody").innerHTML = `
      <section class="detail-section">
        <h3>状态时间线</h3>
        <div class="timeline">
          ${statusFlow
            .map(
              (status) => `
              <div class="timeline-row">
                <span class="timeline-dot" style="background:${statusFlow.indexOf(status) <= statusFlow.indexOf(item.status) ? "var(--blue)" : "var(--line-dark)"}"></span>
                <div><strong>${status}</strong><p class="eyebrow">${status === item.status ? "当前状态" : "流程节点"}</p></div>
              </div>
            `,
            )
            .join("")}
        </div>
      </section>
      <section>
        <h3>评论</h3>
        ${item.comments.map((comment) => `<p>${comment}</p>`).join("") || "<p>暂无评论</p>"}
      </section>
    `;
  } else if (drawerTab === "links") {
    const related = projectItems().filter((entry) => entry.id !== item.id).slice(0, 2);
    $("#drawerBody").innerHTML = `<section class="detail-section"><h3>关联工作项</h3>${related.map((entry) => `<p>${entry.id} ${entry.title}</p>`).join("") || "<p>暂无关联工作项</p>"}</section>`;
  } else if (drawerTab === "files") {
    const files = item.attachments?.length
      ? item.attachments
      : [
          { name: "20250522_102345.png", size: 120000, type: "image/png", url: "" },
          { name: "复现录屏.mp4", size: 12400000, type: "video/mp4", url: "" },
        ];
    $("#drawerBody").innerHTML = `
      <section class="detail-section">
        <h3>附件</h3>
        <div class="attachment-list">
          ${files
            .map(
              (file) => `
                <div class="attachment-item">
                  <span class="attachment-thumb">${file.url ? `<img src="${file.url}" alt="${file.name}" />` : "FILE"}</span>
                  <span>
                    <span class="attachment-name">${file.name}</span>
                    <span class="attachment-meta">${formatSize(file.size || 0)}</span>
                  </span>
                </div>
              `,
            )
            .join("")}
        </div>
      </section>
    `;
  } else {
    $("#drawerBody").innerHTML = `<section class="detail-section"><h3>子任务</h3><p>TASK-2076 权限模型重构</p><p>CASE-889 搜索接口性能测试</p></section>`;
  }
}

function updateItem(id, patch) {
  workItems = workItems.map((item) =>
    item.id === id
      ? {
          ...item,
          ...patch,
          comments: [...item.comments, `系统：${Object.entries(patch).map(([key, value]) => `${key} 更新为 ${value}`).join("，")}`],
        }
      : item,
  );
  renderAll();
  showToast("工作项已更新");
}

function renderAll() {
  renderProjectContext();
  renderSummaries();
  renderTopPanels();
  renderTable();
  renderBoard();
  renderDrawer();
}

function openItem(id) {
  selectedId = id;
  const item = getSelected();
  if (item.projectId !== currentProjectId) currentProjectId = item.projectId;
  renderAll();
  $("#detailDrawer").classList.add("open");
}

function switchProject(projectId) {
  currentProjectId = projectId;
  const firstItem = projectItems()[0];
  if (firstItem) selectedId = firstItem.id;
  $("#detailDrawer").classList.remove("open");
  renderAll();
  showToast(`已切换到 ${getCurrentProject().name}`);
}

function renderProjectContext() {
  const project = getCurrentProject();
  const items = projectItems();
  if (!items.some((item) => item.id === selectedId) && items[0]) selectedId = items[0].id;
  $("#projectSwitchDot").textContent = project.short;
  $("#projectSwitchDot").className = `project-dot ${project.tone}`;
  $("#projectSwitchName").textContent = project.name;
  $("#currentProjectName").textContent = project.name;
  $("#currentContextLine").textContent = "当前项目 / Sprint 23";
  $all(".project-row").forEach((row) => row.classList.toggle("active", row.dataset.projectId === currentProjectId));
  $("#newProject").innerHTML = projects.map((entry) => `<option value="${entry.id}">${entry.name}</option>`).join("");
  $("#newProject").value = currentProjectId;

  const counts = {
    all: items.length,
    需求: items.filter((item) => item.type === "需求").length,
    任务: items.filter((item) => item.type === "任务").length,
    测试: items.filter((item) => item.type === "测试").length,
    缺陷: items.filter((item) => item.type === "缺陷").length,
  };
  $all("#typeTabs .tab").forEach((tab) => {
    const count = counts[tab.dataset.type] ?? 0;
    const label = tab.dataset.type === "all" ? "全部" : tab.dataset.type;
    tab.innerHTML = `${label} <span>${count}</span>`;
  });
}

function showToast(message) {
  const toast = $("#toast");
  toast.textContent = message;
  toast.classList.remove("hidden");
  window.setTimeout(() => toast.classList.add("hidden"), 1800);
}

function formatSize(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function addAttachments(files) {
  const nextFiles = [...files].map((file) => ({
    id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
    file,
    name: file.name || `粘贴截图-${new Date().toLocaleTimeString("zh-CN", { hour12: false }).replaceAll(":", "")}.png`,
    size: file.size,
    type: file.type,
    url: file.type.startsWith("image/") ? URL.createObjectURL(file) : "",
  }));
  pendingAttachments = [...pendingAttachments, ...nextFiles];
  renderAttachmentList();
}

function renderAttachmentList() {
  $("#attachmentList").innerHTML = pendingAttachments
    .map(
      (attachment) => `
        <div class="attachment-item">
          <span class="attachment-thumb">
            ${attachment.url ? `<img src="${attachment.url}" alt="${attachment.name}" />` : "FILE"}
          </span>
          <span>
            <span class="attachment-name">${attachment.name}</span>
            <span class="attachment-meta">${formatSize(attachment.size)}</span>
          </span>
          <button type="button" class="icon-button" data-remove-file="${attachment.id}" aria-label="删除附件">×</button>
        </div>
      `,
    )
    .join("");
}

function resetCreateModal(options = {}) {
  if (!options.keepAttachmentUrls) {
    pendingAttachments.forEach((attachment) => {
      if (attachment.url) URL.revokeObjectURL(attachment.url);
    });
  }
  pendingAttachments = [];
  $("#createModal").reset();
  renderAttachmentList();
}

function setPage(page) {
  const titles = {
    dashboard: "工作台",
    inbox: "收件箱",
    mine: "我的工作",
    items: "全部工作项",
    board: "流程看板",
    reports: "项目简报",
  };
  $("#pageTitle").textContent = titles[page] || "工作台";
  $all(".nav-item").forEach((button) => button.classList.toggle("active", button.dataset.page === page));

  if (page === "board") {
    boardMode = true;
    $("#listView").classList.add("hidden");
    $("#boardView").classList.remove("hidden");
    $("#toggleViewBtn").textContent = "切换列表";
  } else if (page === "inbox") {
    activeType = "all";
    $("#statusFilter").value = "新提交";
    boardMode = false;
    $("#listView").classList.remove("hidden");
    $("#boardView").classList.add("hidden");
    $("#toggleViewBtn").textContent = "切换看板";
  }
  renderAll();
}

function bindEvents() {
  document.addEventListener("click", (event) => {
    const openTarget = event.target.closest("[data-open]");
    if (openTarget) openItem(openTarget.dataset.open);

    const navTarget = event.target.closest("[data-page]");
    if (navTarget) setPage(navTarget.dataset.page);

    const pageLink = event.target.closest("[data-page-link]");
    if (pageLink) setPage(pageLink.dataset.pageLink);

    const projectTarget = event.target.closest("[data-project-id]");
    if (projectTarget) switchProject(projectTarget.dataset.projectId);

    const actionButton = event.target.closest("[data-action]");
    if (actionButton) {
      const action = $("#actionBar")._actions?.[Number(actionButton.dataset.action)];
      if (action) action[1]();
    }

    const addStatus = event.target.closest("[data-add-status]");
    if (addStatus) {
      $("#modalBackdrop").classList.remove("hidden");
      $("#newProject").value = currentProjectId;
      $("#newType").value = "任务";
    }
  });

  $("#globalSearch").addEventListener("input", renderAll);
  $("#statusFilter").addEventListener("change", renderAll);
  $("#ownerFilter").addEventListener("change", renderAll);

  $("#typeTabs").addEventListener("click", (event) => {
    const tab = event.target.closest(".tab");
    if (!tab) return;
    activeType = tab.dataset.type;
    $all(".tab").forEach((item) => item.classList.toggle("active", item === tab));
    renderAll();
  });

  $("#toggleViewBtn").addEventListener("click", () => {
    boardMode = !boardMode;
    $("#listView").classList.toggle("hidden", boardMode);
    $("#boardView").classList.toggle("hidden", !boardMode);
    $("#toggleViewBtn").textContent = boardMode ? "切换列表" : "切换看板";
  });

  $("#toggleSidebar").addEventListener("click", () => $(".app-shell").classList.toggle("collapsed-sidebar"));
  $("#createBtn").addEventListener("click", () => {
    $("#newProject").value = currentProjectId;
    $("#modalBackdrop").classList.remove("hidden");
  });
  $("#closeModal").addEventListener("click", () => {
    $("#modalBackdrop").classList.add("hidden");
    resetCreateModal();
  });
  $("#cancelCreate").addEventListener("click", () => {
    $("#modalBackdrop").classList.add("hidden");
    resetCreateModal();
  });
  $("#closeDrawer").addEventListener("click", () => $("#detailDrawer").classList.remove("open"));
  $("#viewSettingsBtn").addEventListener("click", () => showToast("视图设置已保存为当前项目默认视图"));
  $("#importBtn").addEventListener("click", () => showToast("导入入口已打开，原型中暂不上传文件"));
  $("#notifyBtn").addEventListener("click", () => showToast("你有 5 条项目通知"));
  $("#addProjectBtn").addEventListener("click", () => showToast("新建项目将在完整版本中进入项目向导"));

  $("#pickFilesBtn").addEventListener("click", () => $("#newFiles").click());
  $("#newFiles").addEventListener("change", (event) => {
    addAttachments(event.target.files);
    event.target.value = "";
  });

  $("#newDesc").addEventListener("paste", (event) => {
    const imageFiles = [...event.clipboardData.files].filter((file) => file.type.startsWith("image/"));
    if (!imageFiles.length) return;
    addAttachments(imageFiles);
    showToast("截图已添加到附件");
  });

  $("#uploadZone").addEventListener("dragover", (event) => {
    event.preventDefault();
    $("#uploadZone").classList.add("dragging");
  });
  $("#uploadZone").addEventListener("dragleave", () => $("#uploadZone").classList.remove("dragging"));
  $("#uploadZone").addEventListener("drop", (event) => {
    event.preventDefault();
    $("#uploadZone").classList.remove("dragging");
    addAttachments(event.dataTransfer.files);
  });

  $("#attachmentList").addEventListener("click", (event) => {
    const removeButton = event.target.closest("[data-remove-file]");
    if (!removeButton) return;
    const attachment = pendingAttachments.find((item) => item.id === removeButton.dataset.removeFile);
    if (attachment?.url) URL.revokeObjectURL(attachment.url);
    pendingAttachments = pendingAttachments.filter((item) => item.id !== removeButton.dataset.removeFile);
    renderAttachmentList();
  });

  $(".drawer-tabs").addEventListener("click", (event) => {
    const tab = event.target.closest(".drawer-tab");
    if (!tab) return;
    drawerTab = tab.dataset.drawerTab;
    $all(".drawer-tab").forEach((item) => item.classList.toggle("active", item === tab));
    renderDrawer();
  });

  $("#commentBtn").addEventListener("click", () => {
    const value = $("#commentInput").value.trim();
    if (!value) return;
    const item = getSelected();
    workItems = workItems.map((entry) => (entry.id === item.id ? { ...entry, comments: [...entry.comments, `张伟：${value}`] } : entry));
    $("#commentInput").value = "";
    drawerTab = "activity";
    $all(".drawer-tab").forEach((tab) => tab.classList.toggle("active", tab.dataset.drawerTab === "activity"));
    renderAll();
    showToast("评论已添加");
  });

  $("#createModal").addEventListener("submit", (event) => {
    event.preventDefault();
    const type = $("#newType").value;
    const projectId = $("#newProject").value;
    const prefix = type === "缺陷" ? "BUG" : type === "需求" ? "REQ" : type === "测试" ? "CASE" : "TASK";
    const item = {
      id: `${prefix}-${Math.floor(3000 + Math.random() * 6000)}`,
      projectId,
      type,
      title: $("#newTitle").value.trim(),
      status: "新提交",
      owner: $("#newOwner").value,
      priority: $("#newPriority").value,
      sprint: "Sprint 23",
      due: $("#newDue").value || "未设置",
      module: "未分类",
      severity: type === "缺陷" ? "一般" : "普通",
      creator: "张伟",
      desc: $("#newDesc").value.trim() || "暂无描述。",
      expected: "等待补充验收标准。",
      actual: "新建工作项。",
      steps: ["确认范围", "分配负责人", "进入流程"],
      tags: [type],
      attachments: pendingAttachments.map((attachment) => ({
        name: attachment.name,
        size: attachment.size,
        type: attachment.type,
        url: attachment.url,
      })),
      comments: [
        "系统：工作项已创建。",
        pendingAttachments.length ? `系统：已添加 ${pendingAttachments.length} 个附件。` : "",
      ].filter(Boolean),
    };
    workItems = [item, ...workItems];
    selectedId = item.id;
    currentProjectId = projectId;
    resetCreateModal({ keepAttachmentUrls: true });
    $("#modalBackdrop").classList.add("hidden");
    $("#detailDrawer").classList.add("open");
    renderAll();
    showToast("工作项已创建");
  });
}

bindEvents();
renderAll();
