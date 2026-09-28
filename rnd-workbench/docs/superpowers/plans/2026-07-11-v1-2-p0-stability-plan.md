# V1.2 P0 Stability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the sidebar project shortcut list with a permission-aware project-management list, while making archived-project access and dashboard data behave consistently with the approved V1.2 product baseline.

**Architecture:** Keep the current Spring Boot + native HTML/CSS/JavaScript architecture. Centralize the archived-project write guard in `ProjectService`, compute summary counts in repository queries, and keep project-directory layout structure aligned with its CSS grid. The API stays under `/api/v1` and existing successful response shapes remain compatible.

**Tech Stack:** Java 11, Spring Boot 2.7, Spring Data JPA, H2 test database, PostgreSQL production, vanilla HTML/CSS/JavaScript.

## Global Constraints

- Do not change the database schema in P0.
- Archived projects remain readable to existing project members and exportable, but every write action must return a business conflict.
- A system administrator can maintain project membership through project management; project-content reads/writes still require membership.
- Run `node --check src/main/resources/static/script.js`, `mvn -q test package`, and JAR content verification before handoff.

---

### Task 1: Guard archived projects at every write boundary

**Files:**
- Modify: `src/main/java/com/rnd/app/service/ProjectService.java`
- Modify: `src/main/java/com/rnd/app/controller/ProjectController.java`
- Modify: `src/main/java/com/rnd/app/controller/WorkItemController.java`
- Modify: `src/main/java/com/rnd/app/controller/CommentController.java`
- Modify: `src/main/java/com/rnd/app/controller/MiscController.java`
- Test: `src/test/java/com/rnd/app/service/ProjectServiceTest.java`

**Interfaces:**
- Produces `ProjectService.ensureActiveProject(Long projectId)`, which throws `BusinessException(ErrorCode.STATUS_CONFLICT, "项目已归档，仅可查看和导出")` when the project is archived.
- `ensureProjectWriter` and `ensureProjectAdmin` call `ensureActiveProject` after role verification.

- [ ] **Step 1: Add failing service tests for archived projects**

```java
@Test
void archivedProjectRejectsWritersAndProjectAdmins() {
    Project archived = Project.builder().id(1L).archived(true).build();
    when(projectRepo.findById(1L)).thenReturn(Optional.of(archived));
    when(memberRepo.findByProjectIdAndUserId(1L, 11L))
            .thenReturn(Optional.of(ProjectMember.builder().projectId(1L).userId(11L).role("MEMBER").build()));
    assertThrows(BusinessException.class, () -> service.ensureProjectWriter(1L, 11L));
}
```

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `mvn -q -Dtest=ProjectServiceTest#archivedProjectRejectsWritersAndProjectAdmins test`  
Expected: FAIL because archived projects are currently writable.

- [ ] **Step 3: Add the centralized active-project guard**

```java
public void ensureActiveProject(Long projectId) {
    Project project = projectRepo.findById(projectId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    if (project.isArchived()) {
        throw new BusinessException(ErrorCode.STATUS_CONFLICT, "项目已归档，仅可查看和导出");
    }
}
```

Call it from `ensureProjectWriter` and `ensureProjectAdmin`. For system-admin-only project mutations such as project member management, call it before mutating, but do not call it from read endpoints, download endpoints, or export endpoints.

- [ ] **Step 4: Route every write endpoint through the guard**

Use the existing service permission methods for work item creation, update, status transition, assignment, bulk update/delete, comments, uploads, attachment deletion, modules, sprints, and members. Ensure the system-admin-only membership path also calls `ensureActiveProject(id)` before adding, changing, or removing a member.

- [ ] **Step 5: Re-run focused service tests**

Run: `mvn -q -Dtest=ProjectServiceTest test`  
Expected: PASS.

### Task 2: Expose the current user's project role to the project list

**Files:**
- Modify: `src/main/java/com/rnd/app/dto/ProjectDto.java`
- Modify: `src/main/java/com/rnd/app/service/ProjectService.java`
- Modify: `src/main/java/com/rnd/app/controller/ProjectController.java`
- Test: `src/test/java/com/rnd/app/service/ProjectServiceTest.java`

**Interfaces:**
- `GET /projects` adds nullable `currentUserProjectRole` to each existing project DTO.
- `GET /projects/manage` preserves the existing fields; a system administrator can manage every returned project regardless of this nullable field.

- [ ] **Step 1: Write a service test for the returned membership role**

Mock `ProjectMemberRepository.findByProjectIdAndUserId(1L, 11L)` to return a `PROJECT_ADMIN` membership and assert that the DTO returned by the user-aware mapping has `currentUserProjectRole` equal to `PROJECT_ADMIN`.

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `mvn -q -Dtest=ProjectServiceTest#projectDtoIncludesCurrentUserProjectRole test`  
Expected: FAIL because `ProjectDto` currently has no membership-role field.

- [ ] **Step 3: Add user-aware project mapping without changing existing identifiers**

Add `currentUserProjectRole` to `ProjectDto`, and add `ProjectService.toDto(Project project, Long currentUserId)` that delegates existing fields to `toDto(project)` and fills this role from `ProjectMemberRepository`. Make `GET /projects` use the user-aware overload. Do not expose membership role for another user.

- [ ] **Step 4: Re-run the focused test**

Run: `mvn -q -Dtest=ProjectServiceTest test`  
Expected: PASS.

### Task 3: Make project management permissions match the product baseline

**Files:**
- Modify: `src/main/java/com/rnd/app/controller/ProjectController.java`
- Test: `src/test/java/com/rnd/app/controller/ProjectControllerTest.java`

**Interfaces:**
- `POST`, `PUT`, and `DELETE /projects/{id}/members...` allow either a system administrator or a project administrator.
- Module and sprint management remain project-administrator-only.

- [ ] **Step 1: Write controller tests for system administrator membership maintenance**

Use a `RndPrincipal` with `systemRole="ADMIN"` and verify member add/update/remove does not invoke `ensureProjectAdmin`; use a non-admin principal to verify existing project-admin checks remain in place.

- [ ] **Step 2: Run the controller test and verify it fails**

Run: `mvn -q -Dtest=ProjectControllerTest test`  
Expected: FAIL because current member endpoints always require project administrator membership.

- [ ] **Step 3: Add one controller helper for membership authority**

Implement a private `ensureMemberManagementAuthority(Long projectId)` that:

1. reads `SecurityUtil.currentUser()`;
2. returns for `ADMIN` after `projectService.ensureActiveProject(projectId)`;
3. otherwise calls `projectService.ensureProjectAdmin(projectId, SecurityUtil.currentUserId())`.

Use it only for the three membership mutation endpoints.

- [ ] **Step 4: Re-run the controller test**

Run: `mvn -q -Dtest=ProjectControllerTest test`  
Expected: PASS.

### Task 4: Correct dashboard summary counts

**Files:**
- Modify: `src/main/java/com/rnd/app/repository/WorkItemRepository.java`
- Modify: `src/main/java/com/rnd/app/service/ProjectService.java`
- Test: `src/test/java/com/rnd/app/service/ProjectServiceTest.java`

**Interfaces:**
- `ProjectService.summary(projectId)` returns actual counts for new/unassigned, in-progress, pending acceptance, today due, and P0 non-final work.
- Existing `ProjectSummaryDto` JSON field names stay unchanged.

- [ ] **Step 1: Add repository count declarations and service tests**

Add derived repository methods for:

```java
long countByProjectIdAndDueDateBetweenAndStatusNotIn(Long projectId, Instant from, Instant to, List<String> finalStatuses);
long countByProjectIdAndPriorityAndStatusNotIn(Long projectId, String priority, List<String> finalStatuses);
```

In `ProjectServiceTest`, mock the count methods and assert `summary.getTodayDue()` and `summary.getP0Urgent()` are non-zero when repository data is non-zero.

- [ ] **Step 2: Run the focused summary test and verify it fails**

Run: `mvn -q -Dtest=ProjectServiceTest#summaryUsesRealDueAndP0Counts test`  
Expected: FAIL because the service currently returns `0, 0` for both values.

- [ ] **Step 3: Compute the date range in Asia/Shanghai and use final statuses**

In `ProjectService.summary`, derive the current date in `ZoneId.of("Asia/Shanghai")`, create the start and end `Instant` values for that date, and query with `List.of("已完成", "已关闭")`. Preserve the existing count semantics for the four existing status-based fields.

- [ ] **Step 4: Re-run ProjectService tests**

Run: `mvn -q -Dtest=ProjectServiceTest test`  
Expected: PASS.

### Task 5: Build the project-management list and remove sidebar project shortcuts

**Files:**
- Modify: `src/main/resources/static/script.js`
- Modify: `src/main/resources/static/styles.css`
- Test: manual browser acceptance on a local packaged application

**Interfaces:**
- `showProjectDirectory()` renders a searchable, filterable project list for the current user.
- The sidebar renders no `.project-row` project shortcuts; `#projectSwitch` remains the only project-context switcher.
- Project-list actions are rendered from `currentUser.systemRole` and each project membership role.

- [ ] **Step 1: Replace the card directory with a semantic project list**

Replace `.project-directory` cards with a toolbar and table/list structure:

```html
<section class="project-management">
  <div class="project-management-toolbar">
    <input id="projectSearch" type="search" placeholder="搜索项目名称或简称" />
    <select id="projectStatusFilter"><option value="active">进行中</option><option value="archived">已归档</option><option value="all">全部</option></select>
    <select id="projectScopeFilter"><option value="all">我参与</option><option value="managed">我负责</option></select>
    <button data-create-project>新增项目</button>
  </div>
  <div id="projectManagementList"></div>
</section>
```

Render columns for project name, status, project administrators, member count, updated time, and actions. Only system administrators see the creation button and archive/restore actions; only system administrators and project administrators see edit/team actions for their allowed projects.

- [ ] **Step 2: Remove the sidebar project list and preserve project switching**

Delete static `.project-row` markup and the dynamic insertion in `renderProjectNavigation`. Populate the existing `#projectSwitch` with accessible current-project choices; do not introduce a second maintenance path in the sidebar.

- [ ] **Step 3: Add client-side filtering and archive-safe refresh**

Load the user-visible list from `/projects`; for administrators, load `/projects/manage` when the status filter requires archived projects. Filter keyword, status, and “我负责” in the client from the retrieved project and membership data. After archive/restore, refresh both lists; if the archived project is current, clear `currentProjectId`, close the detail drawer, and return to project management with an archive notice.

- [ ] **Step 4: Add explicit loading/error behavior for project management**

Wrap `showProjectDirectory` in `try/catch`; show a retryable error message in `#workItemsTable` when the directory request fails. Disable the initiating archive button while its request is in flight.

- [ ] **Step 5: Validate the rendered flow manually**

Start the package with an H2 test database, create at least seven projects, and verify:

1. sidebar contains no project shortcut list;
2. current-project dropdown changes project context;
3. project management supports keyword, status, and scope filters;
4. archived project is hidden by default, remains visible to system administrators under the archived filter, and can be restored.

### Task 6: Publish P0 API and acceptance alignment

**Files:**
- Modify: `docs/superpowers/specs/2026-07-11-v1-2-unified-product-baseline-design.md`
- Create: `docs/superpowers/specs/2026-07-11-v1-2-p0-api-acceptance.md`

- [ ] **Step 1: Document actual P0 endpoint behavior**

Record permissions, archive behavior, response error code, and user-facing message for every P0 write endpoint. Document that project member mutation accepts system administrators or project administrators, while project content requires membership.

- [ ] **Step 2: Record executable acceptance cases**

Include API request examples for: archived work-item creation rejected; system admin adds first project admin; project member creates a work item in an active project; a guest attempts a write; dashboard summary with due/P0 data; and an archived project disappearing from a member's project list.

- [ ] **Step 3: Validate documentation and package**

Run:

```bash
rg -n "TO""DO|TB""D" docs/superpowers/specs/2026-07-11-v1-2*.md
node --check src/main/resources/static/script.js
mvn -q test package
jar tf target/rnd-workbench-1.0.0-SNAPSHOT.jar | rg 'BOOT-INF/classes/com/rnd/app/(controller/ProjectController|service/ProjectService)\\.class'
```

Expected: no placeholder matches, JavaScript parses, Maven tests pass, and the packaged JAR contains updated classes.

## Plan Self-Review

- **Coverage:** Tasks 1–4 cover every P0 item in the approved V1.2 baseline; Task 5 records the resulting contract and acceptance evidence.
- **Scope:** P1 workflow rules, saved-filter copy, watcher listing, deleted-image placeholders, and P2 audit/experience work are intentionally excluded from this plan.
- **Consistency:** Archive reads and exports remain allowed; only write operations use the active-project guard. System administrators can establish project membership without receiving implicit project-content access.
