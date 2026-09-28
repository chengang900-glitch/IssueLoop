# User Management and First Project Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make a newly deployed workspace usable by allowing administrators to create a first project and manage users, while allowing users to maintain their own department and password.

**Architecture:** Extend the `users` table with department, job role, and first-password-change state. Keep system authority, job role, and project membership role separate. Add focused user-controller endpoints and upgrade the static frontend with account, project, and administrator management modals.

**Tech Stack:** Spring Boot 2.7, Spring Data JPA, Flyway, vanilla JavaScript, JUnit 5, Mockito.

## Global Constraints

- System roles: `ADMIN`, `USER`.
- Job roles: 项目经理、实施顾问、开发顾问、测试顾问.
- Project roles remain `PROJECT_ADMIN`, `MEMBER`, `GUEST`.
- User creation and reset use password `123456` and set `mustChangePassword=true`.
- Verify locally only; do not run Maven packaging.

---

### Task 1: Add user profile and first-password-change persistence

**Files:**
- Modify: `rnd-workbench/src/main/java/com/rnd/app/entity/User.java`
- Create: `rnd-workbench/src/main/resources/db/migration/h2/V5__user_profile_fields.sql`
- Create: `rnd-workbench/src/main/resources/db/migration/postgresql/V5__user_profile_fields.sql`

**Interfaces:**
- Produces: `User.department`, `User.jobRole`, `User.mustChangePassword` persisted in both databases.

- [ ] Add entity fields mapped to `department`, `job_role`, and `must_change_password`.
- [ ] Add database-specific V5 migrations with nullable department/job role and non-null boolean defaulting to false.
- [ ] Run `mvn -q -Dtest=ApplicationContextTest test` and confirm Flyway applies V1-V5 on H2.

### Task 2: Complete secure user-management and self-service APIs

**Files:**
- Modify: `rnd-workbench/src/main/java/com/rnd/app/controller/UserController.java`
- Modify: `rnd-workbench/src/test/java/com/rnd/app/controller/UserControllerTest.java`

**Interfaces:**
- Produces: admin-only create, edit, status, and reset-password operations; self-service department/password updates; enriched `/auth/me` response.

- [ ] Return department, job role, status, and must-change-password state from `/auth/me`.
- [ ] Default newly created user passwords to encoded `123456`; validate email username and job role; set the first-password-change flag.
- [ ] Add `PUT /users/{id}/status` and `POST /users/{id}/reset-password`; reset lock state and failed attempts.
- [ ] Make `PUT /users/me` update department and clear the flag after a successful password replacement.
- [ ] Test admin denial, job-role validation, reset behavior, and self-service password flag clearing.

### Task 3: Make first-project and no-project states explicit

**Files:**
- Modify: `rnd-workbench/src/main/resources/static/index.html`
- Modify: `rnd-workbench/src/main/resources/static/script.js`
- Modify: `rnd-workbench/src/main/resources/static/styles.css`

**Interfaces:**
- Consumes: `POST /api/v1/projects`.
- Produces: an administrator can create a project from the empty state; no project-specific request uses a null ID.

- [ ] Replace static placeholder project rows with dynamic project navigation.
- [ ] Add the first-project/project form and bind `#addProjectBtn`.
- [ ] Render the empty state with a create-project action for administrators and a waiting-for-assignment message for other users.
- [ ] Guard project-dependent navigation, create, export, and filter actions with a visible “请先选择或创建项目” notification.

### Task 4: Add administrator user management and personal account UI

**Files:**
- Modify: `rnd-workbench/src/main/resources/static/index.html`
- Modify: `rnd-workbench/src/main/resources/static/script.js`
- Modify: `rnd-workbench/src/main/resources/static/styles.css`

**Interfaces:**
- Consumes: `/users`, `/users/{id}`, `/users/{id}/status`, `/users/{id}/reset-password`, and `/users/me`.
- Produces: an ADMIN-only user-management screen and user account dialog.

- [ ] Render an ADMIN-only “用户管理” navigation entry with paginated user table.
- [ ] Add create/edit user dialog with email, name, department, job role, system role, and status fields.
- [ ] Add enable/disable and reset-password actions; show the fixed initial password only in success feedback.
- [ ] Bind the lower-left account block to a self-service dialog for department and password updates.
- [ ] When `mustChangePassword=true`, show a required password dialog before normal workspace use.

### Task 5: Validate behavior without packaging

**Files:**
- Verify: `rnd-workbench/src/main/resources/static/script.js`
- Verify: `rnd-workbench/src/test/java/com/rnd/app/controller/UserControllerTest.java`

- [ ] Run `node --check src/main/resources/static/script.js`.
- [ ] Run `mvn test` and confirm all tests pass.
- [ ] Use a local browser smoke test to create a project, open navigation, create a user, and open the account dialog.
