/* Personal workspace and reporting. Existing task editing stays in script.js. */
let insightState = {};
let insightData = null;
let insightRequest = 0;
let insightExtra = {};
let insightCountScope = null;
const insightTitles = { dashboard: "任务看板", "project-dashboard": "项目看板", "project-reports": "项目报表" };
const insightNote = "人员按当前负责人归属；工时为任务累计登记值。完成任务工时按上海时区完成日期归集，不代表每日实际投入。";
const insightColors = ["#3478f6", "#39c7e9", "#20c8aa", "#9366f1", "#f4a759", "#e57891", "#7b90b4"];

function isInsightsPage() { return Object.hasOwn(insightTitles, activePage); }
function insightDate(value) { return value ? new Date(value).toLocaleDateString("zh-CN", { timeZone: "Asia/Shanghai" }) : "未设置"; }
function insightNumber(value) { return Number(value || 0).toLocaleString("zh-CN", { maximumFractionDigits: 2 }); }
function insightStatus(value) { return value === "已完成" ? "待验收" : value; }
function insightToday() { return new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Shanghai", year: "numeric", month: "2-digit", day: "2-digit" }).format(new Date()); }

function resetInsights(page) {
  const today = insightToday();
  insightState = { scope: page === "dashboard" ? "personal" : page === "project-dashboard" ? "project" : "report", projectId: "", ownerId: "", type: "", status: "", keyword: "", dateBasis: "completed", from: "", to: "", groupBy: "project", period: "month", lane: page === "dashboard" ? "pending" : "all", page: 1, size: 20 };
  insightState.size = page === "dashboard" ? 8 : page === "project-dashboard" ? 10 : 20;
  if (page === "dashboard") insightState.keyword = $("#globalSearch").value.trim();
  if (page === "project-dashboard") insightState.projectId = String((currentProjectId !== ALL_PROJECTS && currentProjectId) || projects[0]?.id || "");
  if (page === "project-reports") { insightState.from = today.slice(0, 7) + "-01"; insightState.to = today; }
}

function insightQuery() {
  return new URLSearchParams(Object.entries(insightState).filter(([, value]) => value !== "" && value != null));
}

async function showInsights(page = activePage, reset = false, detailsOnly = false) {
  if (reset || !insightState.scope || activePage !== page) resetInsights(page);
  activePage = page;
  showListView();
  syncPageNavigation();
  $(".content").classList.remove("project-management-mode", "project-brief-mode", "task-management-mode", "zone-dashboard-mode");
  $(".content").classList.add("insights-mode");
  $(".content").classList.toggle("task-dashboard-mode", page === "dashboard");
  $("#detailDrawer").classList.remove("open");
  if (page === "dashboard") insightState.projectId = isAllProjects() ? "" : String(currentProjectId || "");
  renderProjectNavigation();
  const loadId = ++insightRequest;
  const root = $("#insightsRoot");
  if (!detailsOnly) root.innerHTML = page === "dashboard"
    ? '<p class="empty-state" role="status">正在加载任务看板…</p>'
    : `<div class="insight-heading"><div><p class="eyebrow">项目协作 / ${escapeHtml(insightTitles[page])}</p><h1>${escapeHtml(insightTitles[page])}</h1></div></div><p class="empty-state" role="status">正在加载完整统计…</p>`;
  if (!projects.length) {
    root.innerHTML += '<div class="insight-panel empty-state">当前账号尚未加入项目，请联系项目管理员。加入项目后，这里会显示任务与统计。</div>';
    root.querySelector('[role="status"]')?.remove();
    return;
  }
  try {
    if (page === "project-dashboard" && !insightState.projectId) insightState.projectId = String(projects[0].id);
    const data = await api(`/analytics?${insightQuery()}`);
    if (loadId !== insightRequest || activePage !== page) return;
    insightData = data;
    insightState.page = data.page;
    insightExtra = {};
    if (page === "project-dashboard") {
      const id = insightState.projectId;
      const results = await Promise.allSettled([api(`/projects/${id}/milestones`), api(`/projects/${id}/activities`)]);
      if (loadId !== insightRequest || activePage !== page) return;
      insightExtra.milestones = results[0].status === "fulfilled" ? results[0].value : null;
      insightExtra.activities = results[1].status === "fulfilled" ? (results[1].value.content || results[1].value.list || results[1].value) : null;
    }
    if (detailsOnly && page === "dashboard" && root.querySelector("#insightDetails")) {
      const details = root.querySelector("#insightDetails");
      const updated = document.createElement("div");
      updated.innerHTML = insightTaskTable();
      details.style.minHeight = `${details.offsetHeight}px`;
      details.innerHTML = updated.firstElementChild.innerHTML;
      root.querySelectorAll(".insight-metric[data-insight-lane]").forEach(card => card.classList.toggle("selected", card.dataset.insightLane === insightState.lane));
    } else renderInsights();
    const countScope = `${currentUser?.userId}:${currentProjectId}`;
    if (insightCountScope !== countScope) await refreshNavigationCounts();
  } catch (error) {
    if (loadId !== insightRequest || activePage !== page) return;
    if (detailsOnly) { showToast(error.message); return; }
    insightData = null;
    root.innerHTML = `${page === "dashboard" ? "" : `<div class="insight-heading"><h1>${escapeHtml(insightTitles[page])}</h1></div>`}<div class="insight-panel empty-state"><p>${escapeHtml(error.message)}</p><button class="secondary-button" data-insight-retry>重新加载</button></div>`;
  }
}

function insightOptions(values, current) {
  return values.map(([value, label]) => `<option value="${escapeHtml(value)}" ${String(value) === String(current) ? "selected" : ""}>${escapeHtml(label)}</option>`).join("");
}

function insightFilters() {
  if (activePage === "dashboard") return "";
  const report = activePage === "project-reports";
  const projectOptions = projects.map(p => [p.id, p.name]);
  if (activePage !== "project-dashboard") projectOptions.unshift(["", "全部可访问项目"]);
  return `<form id="insightFilters" class="insight-filters">
    <label>项目<select name="projectId">${insightOptions(projectOptions, insightState.projectId)}</select></label>
    ${report ? `<label>当前负责人<select name="ownerId">${insightOptions([["", "全部人员"], ["0", "未分配"], ...insightData.options.map(p => [p.id, p.name])], insightState.ownerId)}</select></label>
    <label>任务类型<select name="type">${insightOptions([["", "全部类型"], ...taskTypes.map(t => [t.name, t.name])], insightState.type)}</select></label>
    <label>当前状态<select name="status">${insightOptions([["", "全部状态"], ...statusFlow.map(s => [s, insightStatus(s)])], insightState.status)}</select></label>
    <label>日期口径<select name="dateBasis">${insightOptions([["completed", "任务完成日期"], ["created", "任务创建日期"]], insightState.dateBasis)}</select></label>
    <label>开始日期<input type="date" name="from" value="${escapeHtml(insightState.from)}"></label>
    <label>结束日期<input type="date" name="to" value="${escapeHtml(insightState.to)}"></label>
    <label>时间分组<select name="period">${insightOptions([["month", "按月"], ["week", "按周（周一开始）"]], insightState.period)}</select></label>` : ""}
    ${activePage !== "project-dashboard" ? `<label class="insight-search">任务搜索<input name="keyword" type="search" placeholder="任务名称或编号" value="${escapeHtml(insightState.keyword)}"></label>` : ""}
    <button type="submit" class="primary-button">查询</button><button type="button" class="secondary-button" data-insight-reset>重置</button>
  </form>`;
}

function insightCard(label, value, lane, hint = "", tone = "") {
  return `<button class="insight-metric ${tone} ${insightState.lane === lane ? "selected" : ""}" data-insight-lane="${lane}"><span>${label}</span><strong>${insightNumber(value)}</strong><small>${escapeHtml(hint || "点击查看任务明细")}</small></button>`;
}

function insightMetrics() {
  const m = insightData.summary;
  if (activePage === "dashboard") return `<div class="insight-metrics personal">${insightCard("我的待接收", m.inbox, "inbox")}${insightCard("我的进行中", m.progress, "progress")}${insightCard("我的已逾期", m.overdue, "overdue", "仅未完成任务", "danger")}${insightCard("未来 7 天到期", m.upcoming, "upcoming", "含今天；与进行中可能重叠", "warning")}</div>`;
  const hoursHint = `${m.missingHours} 项未登记 · 单位：小时`;
  return `<div class="insight-metrics">${insightCard("任务总数", m.total, "all")}${insightCard("未完成", m.open, "open", "不含待验收及已拒绝")}${insightCard("待验收", m.review, "review")}${insightCard("已逾期", m.overdue, "overdue", "仅未完成任务", "danger")}${insightCard("预计工时", m.estimated, "estimated", `${m.missingEstimated} 项未登记 · 小时`)}${insightCard("已登记实际工时", m.hours, "hours", hoursHint)}</div>`;
}

function insightDrill(dimension, key, label, extra = "") {
  return `<button class="text-button" data-insight-dimension="${escapeHtml(dimension)}" data-insight-key="${escapeHtml(key)}" ${extra}>${escapeHtml(label)}</button>`;
}

function insightGroupTable(groups, dimension) {
  const cols = [["total", "任务数"], ["open", "未完成"], ["progress", "进行中"], ["review", "待验收"], ["accepted", "已验收"], ["overdue", "逾期"], ["estimated", "预计工时"], ["hours", "已登记实际工时"], ["completedHours", "完成任务工时"]];
  return `<div class="insight-table-wrap"><table class="insight-table"><thead><tr><th>${{ project: "项目", person: "当前负责人", type: "任务类型", time: "时间" }[dimension]}</th>${cols.map(([, label]) => `<th>${label}</th>`).join("")}<th>工时未登记任务</th></tr></thead><tbody>${groups.map(g => `<tr><td>${insightDrill(dimension, g.key, g.label)}</td>${cols.map(([key]) => `<td>${insightDrill(dimension, g.key, insightNumber(g[key]), `data-insight-metric="${({ total: "all", completedHours: "completed" })[key] || key}"`)}</td>`).join("")}<td>${g.missingHours}</td></tr>`).join("") || '<tr><td colspan="11" class="empty-state">暂无符合条件的数据</td></tr>'}</tbody></table></div>`;
}

function insightPersonalProjects() {
  return `<section class="insight-panel"><div class="insight-panel-heading"><h2>我的项目</h2><span>按我负责的任务统计</span></div><div class="insight-projects">${insightData.projects.map(p => `<article class="insight-project"><button class="text-button project-title" data-insight-project="${escapeHtml(p.key)}">${escapeHtml(p.label)}</button><div>${insightDrill("project", p.key, `${p.open} 项未完成`, 'data-insight-metric="open"')}${insightDrill("project", p.key, `${p.overdue} 项逾期`, 'data-insight-metric="overdue"')}</div><small>最近截止：${insightDate(p.nearestDue)}</small></article>`).join("") || '<p class="empty-state">暂无指派给你的任务</p>'}</div></section>`;
}

function insightTaskTable() {
  const page = insightData.page, pages = Math.max(1, Math.ceil(insightData.total / insightData.size));
  const lanes = { all: "全部任务", mine: "我负责的", pending: "待我处理", inbox: "待接收", progress: "进行中", overdue: "已逾期", upcoming: "未来 7 天到期", review: "待验收", accepted: "已验收", open: "未完成", returned: "验收退回", approval: "延期审批", completed: "已完成任务", hours: "有实际工时记录", estimated: "有预计工时记录", submitted: "我提交的待验收" };
  const title = lanes[insightState.lane] || "任务明细";
  const dimension = insightState.drillDimension;
  const source = ({ project: insightData.projects, person: insightData.people, type: insightData.types, status: insightData.statuses, time: insightData.groups })[dimension] || [];
  const drillLabel = source.find(g => String(g.key) === String(insightState.drillKey))?.label || insightState.drillKey || "";
  return `<section class="insight-panel" id="insightDetails"><div class="insight-panel-heading"><h2>${title}<small> ${insightData.total} 项</small></h2><div>${insightState.drillDimension ? `<span class="insight-drill-label">${escapeHtml(drillLabel)} · 已筛选</span><button class="text-button" data-insight-clear-drill>清除下钻</button>` : ""}${activePage === "project-reports" ? '<button class="secondary-button" data-insight-export="details">导出当前明细</button>' : ""}</div></div>
    ${activePage === "dashboard" ? `<div class="insight-tabs">${[["pending", `待我处理 ${insightData.pendingCount}`], ["returned", "验收退回"], ["review", "待验收事项"], ["approval", "延期审批"], ["mine", "我负责的全部"], ["submitted", "我提交的待验收"], ["accepted", "我的已验收"]].map(([lane, label]) => `<button class="${insightState.lane === lane ? "active" : ""}" data-insight-lane="${lane}">${label}</button>`).join("")}</div>` : ""}
    <div class="insight-table-wrap"><table class="insight-table task-detail"><thead><tr><th>任务 / 编号</th><th>所属项目</th><th>类型</th><th>状态</th><th>负责人</th><th>优先级</th><th>截止日期</th>${activePage === "project-reports" ? "<th>预计工时</th><th>实际工时</th><th>完成日期</th>" : ""}<th>操作</th></tr></thead><tbody>${insightData.list.map(w => `<tr><td><button class="text-button task-title" data-open="${escapeHtml(w.id)}">${escapeHtml(w.title)}</button><small>${escapeHtml(w.id)}</small></td><td>${escapeHtml(w.projectName)}</td><td>${escapeHtml(w.type)}</td><td>${chip(insightStatus(w.status), w.overdue ? "red" : "blue")}</td><td>${escapeHtml(w.ownerName)}</td><td>${escapeHtml(w.priority)}</td><td class="${w.overdue ? "insight-danger" : ""}">${insightDate(w.dueDate)}${w.overdue ? " · 逾期" : ""}</td>${activePage === "project-reports" ? `<td>${w.estimatedHours == null ? "未登记" : insightNumber(w.estimatedHours)}</td><td>${w.actualHours == null ? "未登记" : insightNumber(w.actualHours)}</td><td>${insightDate(w.actualCompletedAt)}</td>` : ""}<td><button class="text-button" data-open="${escapeHtml(w.id)}">${w.actionable ? "处理" : "查看"}</button></td></tr>`).join("") || `<tr><td colspan="11" class="empty-state">暂无符合条件的任务</td></tr>`}</tbody></table></div>
    <div class="insight-pagination"><span>共 ${insightData.total} 项 · 第 ${page} / ${pages} 页</span><button class="secondary-button" data-insight-page="${page - 1}" ${page <= 1 ? "disabled" : ""}>上一页</button><button class="secondary-button" data-insight-page="${page + 1}" ${page >= pages ? "disabled" : ""}>下一页</button></div></section>`;
}

function insightBars(groups, dimension, field, second) {
  const sorted = [...groups].sort((a, b) => Number(b[field]) - Number(a[field]));
  const max = Math.max(1, ...groups.flatMap(g => [Number(g[field]), second ? Number(g[second]) : 0]));
  return `<div class="insight-bars">${sorted.map(g => `<div class="insight-bar-row">${insightDrill(dimension, g.key, g.label)}<div class="insight-bar-tracks"><button class="insight-bar-track" data-insight-dimension="${dimension}" data-insight-key="${escapeHtml(g.key)}" data-insight-metric="${field === "total" ? "all" : field}" aria-label="${escapeHtml(g.label)} ${field === "total" ? "任务数" : "预计工时"} ${g[field]}"><span style="width:${Number(g[field]) / max * 100}%"></span><b>${insightNumber(g[field])}</b></button>${second ? `<button class="insight-bar-track secondary" data-insight-dimension="${dimension}" data-insight-key="${escapeHtml(g.key)}" data-insight-metric="${second}" aria-label="${escapeHtml(g.label)} 已登记实际工时 ${g[second]}"><span style="width:${Number(g[second]) / max * 100}%"></span><b>${insightNumber(g[second])}</b></button>` : ""}</div></div>`).join("") || '<p class="empty-state">暂无数据</p>'}</div>`;
}

function insightDonut() {
  const groups = insightData.statuses, total = insightData.summary.total;
  let offset = 0;
  const segments = groups.map((g, i) => { const start = offset; offset += Number(g.total) / Math.max(1, total) * 100; return `${insightColors[i % insightColors.length]} ${start}% ${offset}%`; });
  return `<div class="insight-donut-layout"><div class="insight-donut" role="img" aria-label="共 ${total} 个任务，状态明细见右侧图例" style="background:conic-gradient(${segments.join(",") || "var(--line) 0% 100%"})"><div><strong>${total}</strong><span>任务总数</span></div></div><div class="insight-legend">${groups.map((g, i) => `<div><i style="background:${insightColors[i % insightColors.length]}"></i>${insightDrill("status", g.key, `${g.label} · ${g.total}`)}</div>`).join("") || "暂无任务"}</div></div>`;
}

function insightTrend() {
  const trend = insightData.trend;
  if (!trend.length) return '<p class="insight-caption">暂无趋势数据</p>';
  const max = Math.max(1, ...trend.flatMap(g => [g.created, g.completed]));
  const x = i => 48 + i * 522 / Math.max(1, trend.length - 1);
  const y = value => 168 - Number(value) / max * 128;
  const points = key => trend.map((g, i) => ({ x: x(i), y: y(g[key]) }));
  const line = values => values.map((point, i) => i ? `L${point.x},${point.y}` : `M${point.x},${point.y}`).join(" ");
  const created = points("created"), completed = points("completed");
  const area = `${line(created)} L${created.at(-1).x},168 L${created[0].x},168 Z`;
  const ticks = [max, Math.round(max / 2), 0];
  return `<div class="insight-trend-chart"><svg viewBox="0 0 600 210" preserveAspectRatio="none" aria-hidden="true">
    <defs><linearGradient id="insightTrendFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#3478f6" stop-opacity=".16"/><stop offset="1" stop-color="#3478f6" stop-opacity="0"/></linearGradient></defs>
    ${ticks.map((tick, i) => `<line class="trend-grid-line" x1="48" y1="${40 + i * 64}" x2="570" y2="${40 + i * 64}"/><text class="trend-axis-label" x="38" y="${44 + i * 64}" text-anchor="end">${tick}</text>`).join("")}
    <path d="${area}" fill="url(#insightTrendFill)"/><path class="trend-line-created" d="${line(created)}"/><path class="trend-line-completed" d="${line(completed)}"/>
    ${trend.map((g, i) => `<text class="trend-axis-label" x="${x(i)}" y="196" text-anchor="middle">${escapeHtml(g.key.slice(2))}</text>`).join("")}
  </svg>${[["created", "新增", created], ["completed", "完成", completed]].flatMap(([key, label, values]) => values.map((point, i) => `<button class="insight-trend-point ${key}" style="left:${point.x / 600 * 100}%;top:${point.y / 210 * 100}%" data-insight-dimension="${key}Month" data-insight-key="${escapeHtml(trend[i].key)}" aria-label="${escapeHtml(trend[i].key)} ${label} ${trend[i][key]} 个任务" title="${escapeHtml(trend[i].key)} ${label} ${trend[i][key]} 个任务"></button>`)).join("")}</div><p class="insight-caption insight-trend-legend"><span>新增任务</span><span>完成任务（当前待验收 / 已验收）</span></p>`;
}

function insightProjectOverview() {
  const p = projects.find(p => String(p.id) === insightState.projectId);
  const m = insightData.summary, effective = m.total - m.rejected;
  const manager = insightData.options.find(o => o.id === p.projectManagerId)?.name || "未设置";
  return `<section class="insight-project-overview"><div><p class="eyebrow">${escapeHtml(p.code || "")}</p><h2>${escapeHtml(p.name)}</h2><p>${escapeHtml(p.customerName || "客户未设置")} · 项目经理：${escapeHtml(manager)}</p></div><div class="insight-project-facts"><span>阶段 <b>${escapeHtml(p.phase || "未设置")}</b></span><span>状态 <b>${escapeHtml(p.projectStatus || "未设置")}</b></span><span>健康状态（档案） <b>${escapeHtml(p.healthStatus || "未设置")}</b></span><span>计划周期 <b>${escapeHtml(p.plannedStartDate || "未设置")} — ${escapeHtml(p.plannedEndDate || "未设置")}</b></span><span>任务验收率 <b>${effective ? (m.accepted / effective * 100).toFixed(1) + "%" : "无有效任务"}</b></span></div><p class="insight-caption">任务验收率 = 已验收 ÷（任务总数 − 已拒绝），不代表项目进度。</p></section>`;
}

function insightProjectExtras() {
  const p = projects.find(p => String(p.id) === insightState.projectId);
  const milestones = insightExtra.milestones;
  const activities = insightExtra.activities;
  return `<div class="insight-two-column"><section class="insight-panel"><h2>里程碑与风险</h2>${milestones === null ? '<p>里程碑加载失败，请刷新重试。</p>' : (milestones || []).map(m => `<div class="insight-milestone"><strong>${escapeHtml(m.name || m.title)}</strong><span>${escapeHtml(m.status)} · ${escapeHtml(m.plannedDate || m.dueDate || "未设置日期")}</span></div>`).join("") || '<p class="insight-caption">暂无里程碑</p>'}<p><b>风险说明：</b>${escapeHtml(p.riskDescription || "未填写")}</p><p><b>当前问题：</b>${escapeHtml(p.currentIssues || "未填写")}</p><p><b>下一步：</b>${escapeHtml(p.nextSteps || "未填写")}</p><button class="text-button" data-project-profile="${p.id}">查看项目档案</button></section><section class="insight-panel"><h2>近期动态</h2>${activities === null ? '<p>项目动态加载失败，请刷新重试。</p>' : (activities || []).slice(0, 6).map(a => `<div class="insight-activity"><p>${escapeHtml(a.actorName || "系统")} ${escapeHtml(a.content)}</p><small>${formatDate(a.createdAt)}</small></div>`).join("") || '<p class="empty-state">暂无项目动态</p>'}</section></div>`;
}

function renderInsights() {
  const page = activePage;
  const intro = page === "project-dashboard" ? "一个项目的任务、人员与投入，集中查看。" : "从项目、人员、时间和任务类型查看任务与工时。";
  let html = `${page === "dashboard" ? "" : `<header class="insight-heading"><div><p class="eyebrow">项目协作 / 数据分析</p><h1>${insightTitles[page]}</h1><p>${intro}</p></div>${page === "project-reports" ? '<button class="secondary-button" data-insight-export="summary">导出汇总 CSV</button>' : ""}</header>`}${insightFilters()}`;
  if (page === "project-dashboard") html += insightProjectOverview();
  if (page === "project-reports") html += `<p class="insight-scope">当前按<strong>${insightState.dateBasis === "completed" ? "任务完成日期" : "任务创建日期"}</strong>筛选，所有列统计同一批任务。${insightState.dateBasis === "completed" ? "仅含当前待验收或已验收且有完成日期的任务；查看未完成任务请切换创建日期。" : "工时为这批任务的累计登记值，不表示所选期间的实际投入。"}</p>`;
  html += insightMetrics();
  if (page === "dashboard") {
    html += insightTaskTable() + insightPersonalProjects();
  } else if (page === "project-dashboard") {
    html += `<p class="insight-caption">${insightNote}</p><div class="insight-charts"><section class="insight-panel"><h2>任务状态分布</h2>${insightDonut()}</section><section class="insight-panel"><h2>各类型任务数</h2>${insightBars(insightData.types, "type", "total")}</section><section class="insight-panel"><h2>人员工时对比</h2><p class="insight-caption">蓝色：预计 · 青色：已登记实际 · 单位：小时</p>${insightBars(insightData.people, "person", "estimated", "hours")}</section><section class="insight-panel"><h2>近 6 个月任务趋势</h2>${insightTrend()}</section></div><section class="insight-panel"><h2>人员任务与工时</h2>${insightGroupTable(insightData.people, "person")}</section>${insightTaskTable()}${insightProjectExtras()}`;
  } else {
    html += `<p class="insight-caption">${insightNote}</p><section class="insight-panel"><div class="insight-tabs">${[["project", "按项目"], ["person", "按人员"], ["time", "按时间"], ["type", "按任务类型"]].map(([key, label]) => `<button data-insight-group="${key}" class="${insightState.groupBy === key ? "active" : ""}">${label}</button>`).join("")}</div>${insightGroupTable(insightData.groups, insightState.groupBy)}</section>${insightTaskTable()}`;
  }
  $("#insightsRoot").innerHTML = html;
}

async function exportInsights(format, button) {
  button.disabled = true;
  try {
    const params = insightQuery(); params.set("format", format);
    const response = await fetch(`${API_BASE}/analytics/export?${params}`, { headers: { Authorization: `Bearer ${token}` } });
    if (response.status === 401) { logout(); throw new Error("登录已失效，请重新登录"); }
    if (!response.ok) { const error = await response.json(); throw new Error(error.message || "导出失败"); }
    const url = URL.createObjectURL(await response.blob());
    const link = document.createElement("a"); link.href = url; link.download = `项目报表-${format === "summary" ? "汇总" : "明细"}-${insightToday()}.csv`; link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  } catch (error) { showToast(error.message); }
  finally { button.disabled = false; }
}

function bindInsightsEvents() {
  document.addEventListener("submit", async event => {
    if (event.target.id !== "insightFilters") return;
    event.preventDefault();
    const values = Object.fromEntries(new FormData(event.target));
    if (values.from && values.to && values.from > values.to) { showToast("开始日期不能晚于结束日期"); return; }
    Object.assign(insightState, values, { page: 1, drillDimension: null, drillKey: null });
    if (activePage === "project-reports") insightState.lane = "all";
    await showInsights();
  });
  document.addEventListener("click", async event => {
    const target = event.target.closest("button");
    if (!target || target.disabled || !isInsightsPage()) return;
    if (target.hasAttribute("data-insight-export")) { await exportInsights(target.dataset.insightExport, target); return; }
    if (target.hasAttribute("data-insight-project")) {
      const id = target.dataset.insightProject;
      resetInsights("project-dashboard"); insightState.projectId = id;
      activePage = "project-dashboard"; await showInsights(); return;
    }
    let refresh = false, scroll = false, detailsOnly = false;
    if (target.hasAttribute("data-insight-lane")) { insightState.lane = target.dataset.insightLane; insightState.drillDimension = null; insightState.drillKey = null; refresh = true; detailsOnly = activePage === "dashboard"; scroll = !detailsOnly || !!target.closest(".insight-metric"); }
    if (target.hasAttribute("data-insight-dimension")) { insightState.drillDimension = target.dataset.insightDimension; insightState.drillKey = target.dataset.insightKey; insightState.lane = target.dataset.insightMetric || "all"; refresh = true; scroll = true; }
    if (target.hasAttribute("data-insight-group")) { insightState.groupBy = target.dataset.insightGroup; insightState.drillDimension = null; insightState.drillKey = null; insightState.lane = "all"; refresh = true; }
    if (target.hasAttribute("data-insight-clear-drill")) { insightState.drillDimension = null; insightState.drillKey = null; insightState.lane = "all"; refresh = true; scroll = true; }
    if (target.hasAttribute("data-insight-reset")) { resetInsights(activePage); refresh = true; }
    if (target.hasAttribute("data-insight-retry")) refresh = true;
    if (refresh) insightState.page = 1;
    if (target.hasAttribute("data-insight-page")) { insightState.page = Number(target.dataset.insightPage); refresh = true; scroll = true; }
    if (refresh) { await showInsights(activePage, false, detailsOnly); if (scroll) $("#insightDetails")?.scrollIntoView({ block: "start", behavior: "smooth" }); }
  });
}

async function refreshNavigationCounts() {
  const userId = currentUser?.userId;
  const projectId = currentProjectId;
  try {
    const selectedProjects = currentProjectId && currentProjectId !== ALL_PROJECTS ? projects.filter(p => p.id === currentProjectId) : projects;
    const results = await Promise.all(selectedProjects.map(p => loadTaskCounts(p.id)));
    if (currentUser?.userId !== userId || currentProjectId !== projectId || !isInsightsPage()) return;
    taskCounts = results.reduce((total, next) => {
      Object.keys(total).forEach(key => { total[key] += next[key] || 0; });
      return total;
    }, { all: 0, unclosed: 0, "created-by-me": 0, "assigned-to-me": 0, "pending-for-me": 0 });
    insightCountScope = `${userId}:${projectId}`;
    renderTaskNavigation();
  } catch (error) { showToast(`菜单任务数量刷新失败：${error.message}`); }
}

// The cross-project task list must not silently stop at the per-project API page limit.
async function loadCompleteProjectItems(projectId, query) {
  const params = new URLSearchParams(query);
  params.set("size", "200"); params.set("page", "1");
  const first = await api(`/projects/${projectId}/work-items?${params}`);
  const list = [...(first.list || [])];
  for (let page = 2; page <= Math.ceil(first.total / 200); page++) {
    params.set("page", String(page));
    const next = await api(`/projects/${projectId}/work-items?${params}`);
    list.push(...(next.list || []));
  }
  return { ...first, list };
}
