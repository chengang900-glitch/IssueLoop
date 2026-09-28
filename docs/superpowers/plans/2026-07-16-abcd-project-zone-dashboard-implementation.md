# ABCD Project Zone Dashboard Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a permission-aware ABCD project zone field and a polished four-quadrant dashboard with shared filters plus persistent list/card view switching.

**Architecture:** Store the project zone on `projects`, expose it through the existing project entity/request/DTO/service path, and reuse the existing visible/all-project APIs plus member APIs for dashboard data. Render the dashboard inside the current single-page static frontend, keeping filters and view mode as isolated browser-side state.

**Tech Stack:** Java 17, Spring Boot 2.7, Spring Data JPA, Flyway, H2/PostgreSQL, static HTML/CSS/JavaScript, JUnit 5, Mockito, Browser/IAB.

## Global Constraints

- Work zone values are exactly `A`, `B`, `C`, `D`, and `未分区`; default is `未分区`.
- A=`硬仗清单`, B=`调优策略`, C=`部门级重点工作`, D=`日常周期性工作`.
- Unpartitioned and archived projects are not shown on the zone dashboard.
- Normal users see participated projects; system administrators see all projects.
- Only system administrators or project administrators can modify a project's work zone.
- Project rows/cards show only project name, project manager, phase, project status, health status, and planned end date.
- All dashboard filters affect all four zones and update zone counts immediately.
- Default view is list; the current browser remembers list/card preference.
- Do not add drag-and-drop, inline zone editing, export, charts, or bulk assignment.

---

### Task 1: Persist and validate project work zones

**Files:**
- Create: `rnd-workbench/src/main/resources/db/migration/h2/V13__project_work_zone.sql`
- Create: `rnd-workbench/src/main/resources/db/migration/postgresql/V13__project_work_zone.sql`
- Modify: `rnd-workbench/src/main/java/com/rnd/app/entity/Project.java`
- Modify: `rnd-workbench/src/main/java/com/rnd/app/dto/CreateProjectRequest.java`
- Modify: `rnd-workbench/src/main/java/com/rnd/app/dto/ProjectDto.java`
- Modify: `rnd-workbench/src/main/java/com/rnd/app/service/ProjectService.java`
- Test: `rnd-workbench/src/test/java/com/rnd/app/service/ProjectServiceTest.java`

**Interfaces:**
- Consumes: existing `ProjectService.createProject`, `updateProject`, `toDto`, and `valueOrDefault`.
- Produces: `Project.workZone`, `CreateProjectRequest.workZone`, and `ProjectDto.workZone`, all using `String`.

- [ ] **Step 1: Write failing work-zone service tests**

Add to `ProjectServiceTest`:

```java
@Test
void projectWorkZoneDefaultsAndMapsToDto() {
    CreateProjectRequest request = new CreateProjectRequest();
    request.setName("分区项目"); request.setShortName("分区");
    when(systemSettingService.getProjectCodePrefixForUpdate()).thenReturn("PRJ");
    when(codeCounterRepo.findForUpdate(any(), any())).thenReturn(Optional.empty());
    when(projectRepo.save(any(Project.class))).thenAnswer(invocation -> {
        Project project = invocation.getArgument(0); project.setId(1L); return project;
    });

    Project project = service.createProject(request, 9L);

    assertEquals("未分区", project.getWorkZone());
    assertEquals("未分区", service.toDto(project).getWorkZone());
}

@Test
void rejectsUnsupportedProjectWorkZone() {
    CreateProjectRequest request = new CreateProjectRequest();
    request.setName("分区项目"); request.setShortName("分区"); request.setWorkZone("E");
    when(systemSettingService.getProjectCodePrefixForUpdate()).thenReturn("PRJ");
    when(codeCounterRepo.findForUpdate(any(), any())).thenReturn(Optional.empty());

    assertThrows(BusinessException.class, () -> service.createProject(request, 9L));
}
```

- [ ] **Step 2: Run the tests and confirm the missing-field failure**

Run:

```bash
cd rnd-workbench
mvn -Dtest=ProjectServiceTest test
```

Expected: compilation fails because `getWorkZone` / `setWorkZone` do not exist.

- [ ] **Step 3: Add H2 and PostgreSQL migrations**

Use the same SQL in both dialect folders:

```sql
ALTER TABLE projects ADD COLUMN work_zone VARCHAR(8) NOT NULL DEFAULT '未分区';
CREATE INDEX idx_projects_work_zone ON projects(work_zone);
```

- [ ] **Step 4: Add the entity and DTO properties**

Add to `Project`:

```java
@Column(name = "work_zone", nullable = false, length = 8)
@Builder.Default
private String workZone = "未分区";
```

Add to both `CreateProjectRequest` and `ProjectDto`:

```java
private String workZone;
```

Lombok already supplies accessors; do not add duplicate handwritten methods.

- [ ] **Step 5: Validate, store, and map the work zone**

Add to `ProjectService` constants:

```java
private static final List<String> WORK_ZONES = List.of("A", "B", "C", "D", "未分区");
```

Add in `applyProjectProfile`:

```java
p.setWorkZone(valueOrDefault(req.getWorkZone(), p.getWorkZone(), "未分区", WORK_ZONES, "工作分区"));
```

Add in `toDto`:

```java
dto.setWorkZone(p.getWorkZone());
```

- [ ] **Step 6: Run focused and migration tests**

Run:

```bash
mvn -Dtest=ProjectServiceTest,ApplicationContextTest test
```

Expected: both test classes pass and Flyway validates 13 migrations.

- [ ] **Step 7: Record the task checkpoint**

The current workspace has no `.git` directory, so do not initialize Git. Record the passing command and changed files in the execution notes instead of committing.

---

### Task 2: Add the work-zone field to the project profile

**Files:**
- Modify: `rnd-workbench/src/main/resources/static/script.js`
- Test: `rnd-workbench/src/test/java/com/rnd/app/service/ProjectServiceTest.java`

**Interfaces:**
- Consumes: `ProjectDto.workZone` and `PUT /api/v1/projects/{id}`.
- Produces: profile control `#profileWorkZone` and update payload property `workZone`.

- [ ] **Step 1: Add the profile select in the “基础身份” grid**

Insert after project type in `showProjectProfile`:

```javascript
<label><span>工作分区</span><select id="profileWorkZone" ${disabled}>
  ${[["未分区","未分区"],["A","A区 · 硬仗清单"],["B","B区 · 调优策略"],["C","C区 · 部门级重点工作"],["D","D区 · 日常周期性工作"]]
    .map(([value,label]) => `<option value="${value}" ${project.workZone === value ? "selected" : ""}>${label}</option>`).join("")}
</select></label>
```

The existing `disabled` calculation already restricts editing to system admins and project admins.

- [ ] **Step 2: Submit the selected value**

Add this property to the `projectProfileForm` body:

```javascript
workZone: fieldValue("#profileWorkZone"),
```

- [ ] **Step 3: Run syntax and backend regression checks**

Run:

```bash
node --check src/main/resources/static/script.js
mvn -Dtest=ProjectServiceTest test
```

Expected: JavaScript syntax passes and project service tests pass.

- [ ] **Step 4: Record the task checkpoint**

List `script.js` and the passing checks in execution notes; do not initialize Git.

---

### Task 3: Build the four-zone dashboard, filters, and view switch

**Files:**
- Modify: `rnd-workbench/src/main/resources/static/index.html`
- Modify: `rnd-workbench/src/main/resources/static/script.js`
- Modify: `rnd-workbench/src/main/resources/static/styles.css`

**Interfaces:**
- Consumes: `GET /api/v1/projects`, admin-only `GET /api/v1/projects/manage`, `GET /api/v1/projects/{id}/members`, and `ProjectDto.workZone`.
- Produces: navigation page `data-page="zone-dashboard"`, renderer `showZoneDashboard()`, filter state `zoneFilters`, and local-storage key `rndZoneViewMode`.

- [ ] **Step 1: Add navigation and isolated dashboard state**

Place before the existing task dashboard button in `index.html`:

```html
<button class="nav-item" data-page="zone-dashboard">分区看板</button>
```

Add state in `script.js`:

```javascript
let zoneDirectoryEntries = [];
let zoneViewMode = localStorage.getItem("rndZoneViewMode") === "card" ? "card" : "list";
let zoneFilters = { keyword: "", managerId: "", phase: "", status: "", health: "", endFrom: "", endTo: "" };
const zoneDefinitions = {
  A: { title: "硬仗清单", tone: "red" },
  B: { title: "调优策略", tone: "blue" },
  C: { title: "部门级重点工作", tone: "green" },
  D: { title: "日常周期性工作", tone: "amber" },
};
```

- [ ] **Step 2: Load permission-aware project data**

Add:

```javascript
async function showZoneDashboard() {
  activePage = "zone-dashboard";
  showListView();
  const content = $(".content");
  content.classList.remove("task-management-mode", "project-brief-mode");
  content.classList.add("project-management-mode", "zone-dashboard-mode");
  $("#pageTitle").textContent = "ABCD 分区看板";
  $("#workItemsTable").innerHTML = '<p class="empty-state">正在加载分区看板…</p>';
  try {
    const visible = await api("/projects");
    const source = isAdmin() ? await api("/projects/manage") : visible;
    const active = source.filter(project => !project.archived && ["A","B","C","D"].includes(project.workZone));
    zoneDirectoryEntries = await Promise.all(active.map(async project => ({
      project,
      members: await api(`/projects/${project.id}/members`),
    })));
    renderZoneDashboard();
  } catch (error) {
    $("#workItemsTable").innerHTML = `<div class="empty-state"><p>${escapeHtml(error.message)}</p><button class="secondary-button" data-retry-zone-dashboard>重新加载</button></div>`;
  }
}
```

- [ ] **Step 3: Implement shared filtering and sorting**

Add:

```javascript
function filteredZoneEntries(zone) {
  const keyword = zoneFilters.keyword.trim().toLowerCase();
  return zoneDirectoryEntries.filter(({project}) => {
    if (project.workZone !== zone) return false;
    if (keyword && !String(project.name || "").toLowerCase().includes(keyword)) return false;
    if (zoneFilters.managerId && Number(project.projectManagerId) !== Number(zoneFilters.managerId)) return false;
    if (zoneFilters.phase && project.phase !== zoneFilters.phase) return false;
    if (zoneFilters.status && project.projectStatus !== zoneFilters.status) return false;
    if (zoneFilters.health && project.healthStatus !== zoneFilters.health) return false;
    if (zoneFilters.endFrom && (!project.plannedEndDate || project.plannedEndDate < zoneFilters.endFrom)) return false;
    if (zoneFilters.endTo && (!project.plannedEndDate || project.plannedEndDate > zoneFilters.endTo)) return false;
    return true;
  }).sort((a,b) => {
    const ad = a.project.plannedEndDate || "9999-12-31";
    const bd = b.project.plannedEndDate || "9999-12-31";
    return ad.localeCompare(bd) || a.project.name.localeCompare(b.project.name, "zh-CN");
  });
}
```

- [ ] **Step 4: Render filters and both row/card variants**

Implement `renderZoneDashboard()` with:

```javascript
function renderZoneDashboard() {
  const managers = new Map();
  zoneDirectoryEntries.forEach(({project,members}) => {
    const manager = members.find(member => Number(member.userId) === Number(project.projectManagerId));
    if (manager) managers.set(String(manager.userId), manager.nickname || manager.username);
  });
  const managerOptions = [...managers.entries()].sort((a,b) => a[1].localeCompare(b[1], "zh-CN"))
    .map(([id,name]) => `<option value="${id}" ${zoneFilters.managerId === id ? "selected" : ""}>${escapeHtml(name)}</option>`).join("");
  const optionList = (values, selected) => `<option value="">不限</option>${values.map(value => `<option ${selected === value ? "selected" : ""}>${value}</option>`).join("")}`;
  const projectItem = ({project,members}) => {
    const manager = members.find(member => Number(member.userId) === Number(project.projectManagerId));
    const name = manager?.nickname || manager?.username || "未设置";
    return `<button type="button" class="zone-project ${zoneViewMode}" data-project-profile="${project.id}">
      <strong>${escapeHtml(project.name)}</strong>
      <span><b>项目经理</b>${escapeHtml(name)}</span><span><b>当前阶段</b>${escapeHtml(project.phase || "立项")}</span>
      <span><b>项目状态</b>${escapeHtml(project.projectStatus || "未启动")}</span><span><b>健康状态</b>${escapeHtml(project.healthStatus || "正常")}</span>
      <span><b>计划结束</b>${dateValue(project.plannedEndDate) || "未设置"}</span>
    </button>`;
  };
  const zones = Object.entries(zoneDefinitions).map(([zone,definition]) => {
    const entries = filteredZoneEntries(zone);
    const empty = zoneDirectoryEntries.some(entry => entry.project.workZone === zone) ? "没有符合条件的项目" : "当前没有项目";
    return `<section class="zone-panel zone-${definition.tone}"><header><span class="zone-letter">${zone}</span><div><h2>${definition.title}</h2><p>${entries.length} 个项目</p></div></header><div class="zone-projects ${zoneViewMode}">${entries.map(projectItem).join("") || `<p class="zone-empty">${empty}</p>`}</div></section>`;
  }).join("");
  $("#workItemsTable").innerHTML = `<section class="zone-dashboard"><div class="zone-filterbar">
    <input id="zoneKeyword" type="search" placeholder="搜索项目名称" value="${escapeHtml(zoneFilters.keyword)}" />
    <select id="zoneManager"><option value="">项目经理</option>${managerOptions}</select>
    <select id="zonePhase">${optionList(["立项","实施","开发","测试","上线","验收","运维"], zoneFilters.phase)}</select>
    <select id="zoneStatus">${optionList(["未启动","进行中","已暂停","已完成","已关闭"], zoneFilters.status)}</select>
    <select id="zoneHealth">${optionList(["正常","关注","风险"], zoneFilters.health)}</select>
    <label>结束日期从<input id="zoneEndFrom" type="date" value="${zoneFilters.endFrom}" /></label>
    <label>至<input id="zoneEndTo" type="date" value="${zoneFilters.endTo}" /></label>
    <button type="button" class="secondary-button" data-reset-zone-filters>重置筛选</button>
    <button type="button" class="secondary-button" data-toggle-zone-view>${zoneViewMode === "list" ? "卡片视图" : "列表视图"}</button>
  </div><div class="zone-grid">${zones}</div></section>`;
}
```

- [ ] **Step 5: Wire navigation, filtering, retry, reset, and view switching**

In the delegated page click handler:

```javascript
if (page?.dataset.page === "zone-dashboard") { await showZoneDashboard(); return; }
if (event.target.closest("[data-retry-zone-dashboard]")) { await showZoneDashboard(); return; }
if (event.target.closest("[data-reset-zone-filters]")) { zoneFilters = { keyword:"", managerId:"", phase:"", status:"", health:"", endFrom:"", endTo:"" }; renderZoneDashboard(); return; }
if (event.target.closest("[data-toggle-zone-view]")) { zoneViewMode = zoneViewMode === "list" ? "card" : "list"; localStorage.setItem("rndZoneViewMode", zoneViewMode); renderZoneDashboard(); return; }
```

In delegated `input`/`change` handling, map IDs to state keys and call `renderZoneDashboard()`:

```javascript
const zoneFilterKeys = { zoneKeyword:"keyword", zoneManager:"managerId", zonePhase:"phase", zoneStatus:"status", zoneHealth:"health", zoneEndFrom:"endFrom", zoneEndTo:"endTo" };
const key = zoneFilterKeys[event.target.id];
if (key) { zoneFilters[key] = event.target.value; renderZoneDashboard(); }
```

- [ ] **Step 6: Add the visual system and responsive rules**

Add CSS using the existing variables:

```css
.zone-dashboard { display: grid; gap: 18px; padding-bottom: 28px; }
.zone-filterbar { display: grid; grid-template-columns: minmax(190px,1.4fr) repeat(4,minmax(120px,.8fr)) repeat(2,minmax(130px,.8fr)) auto auto; gap: 10px; align-items: end; padding: 14px; border: 1px solid var(--glass-line); border-radius: 18px; background: var(--glass-panel); box-shadow: var(--shadow-soft); }
.zone-filterbar label { display: grid; gap: 5px; color: var(--muted); font-size: 12px; font-weight: 700; }
.zone-grid { display: grid; grid-template-columns: repeat(2,minmax(0,1fr)); gap: 16px; }
.zone-panel { --zone:#d94b43; --zone-soft:#fff2f0; min-height: 300px; overflow: hidden; border: 1px solid color-mix(in srgb,var(--zone) 28%,var(--line)); border-radius: 22px; background: color-mix(in srgb,var(--zone-soft) 52%,var(--glass-panel)); box-shadow: var(--shadow-soft); }
.zone-blue { --zone:#3478c8; --zone-soft:#eef6ff; } .zone-green { --zone:#2f8d62; --zone-soft:#effaf4; } .zone-amber { --zone:#c98612; --zone-soft:#fff8e8; }
.zone-panel > header { display:flex; align-items:center; gap:12px; padding:16px 18px; border-bottom:1px solid color-mix(in srgb,var(--zone) 18%,var(--line)); }
.zone-letter { display:grid; place-items:center; width:44px; height:44px; border-radius:14px; color:white; background:var(--zone); font-size:24px; font-weight:850; }
.zone-panel h2,.zone-panel p { margin:0; } .zone-panel header p { color:var(--muted); margin-top:3px; }
.zone-projects { display:grid; gap:9px; padding:14px; } .zone-projects.card { grid-template-columns:repeat(2,minmax(0,1fr)); }
.zone-project { width:100%; border:1px solid var(--line); border-left:4px solid var(--zone); border-radius:12px; background:var(--surface); color:var(--text); text-align:left; }
.zone-project.list { display:grid; grid-template-columns:minmax(160px,1.5fr) repeat(5,minmax(90px,1fr)); align-items:center; gap:10px; padding:11px 13px; }
.zone-project.card { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:10px; padding:15px; border-radius:16px; }
.zone-project.card strong { grid-column:1/-1; font-size:16px; } .zone-project span { display:grid; gap:3px; } .zone-project b { color:var(--muted); font-size:11px; }
.zone-project:hover { transform:translateY(-1px); border-color:color-mix(in srgb,var(--zone) 45%,var(--line)); box-shadow:0 10px 22px rgba(33,50,41,.08); }
.zone-empty { padding:38px 10px; color:var(--muted); text-align:center; }
@media (max-width:1100px) { .zone-filterbar { grid-template-columns:repeat(3,minmax(0,1fr)); } .zone-project.list { grid-template-columns:repeat(2,minmax(0,1fr)); } .zone-project.list strong { grid-column:1/-1; } }
@media (max-width:760px) { .zone-grid,.zone-filterbar { grid-template-columns:1fr; } .zone-projects.card { grid-template-columns:1fr; } }
```

- [ ] **Step 7: Run static validation**

Run:

```bash
node --check src/main/resources/static/script.js
rg -n "分区看板|profileWorkZone|zone-dashboard|zoneViewMode" src/main/resources/static
```

Expected: syntax passes and every required UI surface is present.

- [ ] **Step 8: Record the task checkpoint**

List the three frontend files and static validation result in execution notes; do not initialize Git.

---

### Task 4: Full regression and visual interaction verification

**Files:**
- Test: all files under `rnd-workbench/src/test/java`
- Verify: `rnd-workbench/src/main/resources/static/index.html`
- Verify: `rnd-workbench/src/main/resources/static/script.js`
- Verify: `rnd-workbench/src/main/resources/static/styles.css`

**Interfaces:**
- Consumes: completed database, backend, profile, dashboard, filter, and view-switch behavior.
- Produces: passing test log plus desktop/mobile screenshots outside the repository.

- [ ] **Step 1: Run the full automated test suite**

Run:

```bash
mvn test
```

Expected: all tests pass with 13 validated migrations.

- [ ] **Step 2: Start a temporary isolated UI instance**

Run on an available temporary port:

```bash
mvn spring-boot:run '-Dspring-boot.run.arguments=--server.port=3004 --spring.datasource.url=jdbc:h2:mem:zoneqa;DB_CLOSE_DELAY=-1 --app.storage.path=/tmp/rnd-zoneqa-attachments --app.export.path=/tmp/rnd-zoneqa-exports'
```

Expected: application starts and applies migrations through V13.

- [ ] **Step 3: Seed and exercise the core flow through Browser/IAB**

Use the admin test login and create at least five temporary projects: one each in A/B/C/D plus one unpartitioned. Verify:

```text
project profile -> select work zone -> save -> open zone dashboard
```

Expected: only the four assigned, non-archived projects appear; each appears in the correct quadrant; clicking a project opens its profile.

- [ ] **Step 4: Verify all filters and sorting**

Exercise keyword, manager, phase, project status, health status, start/end date, and reset. Confirm counts change in all zones. Create two projects in one zone with different planned end dates and confirm ascending order with blank dates last.

- [ ] **Step 5: Verify list/card mode persistence**

Switch to card mode, reload, and confirm card mode remains. Switch back to list mode and confirm the compact row layout returns without changing filter results.

- [ ] **Step 6: Verify responsive layout and console health**

Capture desktop and mobile-sized screenshots to `/tmp`. Confirm desktop is 2×2, mobile is A/B/C/D single-column, controls do not clip, and Browser console has no relevant errors or warnings.

- [ ] **Step 7: Compare reference and implementation visually**

Use `view_image` on the user reference image and the final desktop screenshot. Check at least: A/B/C/D ordering, color identity, four-zone balance, filter-bar hierarchy, list/card readability, typography, and whitespace. Fix any material mismatch before handoff.

- [ ] **Step 8: Stop the temporary server and report**

Stop the temporary process, retain only `/tmp` screenshots needed for the final QA evidence, and report automated tests, Browser checks, responsive checks, console health, and any intentional deviation.
