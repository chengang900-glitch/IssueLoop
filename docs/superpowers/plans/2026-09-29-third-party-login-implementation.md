# Third Party Login Configuration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use inline execution with task-by-task checkpoints.

**Goal:** Add administrator-controlled third-party collaboration app login settings for Feishu, DingTalk, and WeCom, expose only enabled providers on the login page, and rename the enterprise entry to “企业统一认证登录”.

**Architecture:** Reuse the existing `system_settings` key/value table and admin-only settings API. Store one global enable flag and three provider flags as validated settings, expose a public read-only enabled-provider endpoint for the login page, and keep provider secrets out of the database and browser. Real provider authorization adapters remain disabled until deployment credentials and callback URLs are supplied.

**Tech Stack:** Spring Boot 2.7.18, Java 11, JPA, Flyway, H2/PostgreSQL migrations, vanilla JavaScript/CSS, Maven tests.

## Global Constraints

- Preserve the existing local username/password login and JWT permission model.
- Use exact product copy: “企业统一认证登录”, “集成第三方协同 APP 登录”, “飞书登录”, “钉钉登录”, “企微登录”.
- The master switch defaults to disabled; disabled or unconfigured providers must not appear on the public login page.
- Only administrators may read or update management settings; public login configuration returns enabled provider names only.
- Do not store App Secret, Client Secret, access token, or refresh token in repository, database settings, browser storage, URL, or logs.
- Do not modify unrelated user, project, task, or existing uncommitted files.

---

### Task 1: Settings service and migration

**Files:**
- Create: `rnd-workbench/src/main/resources/db/migration/h2/V16__third_party_login_settings.sql`
- Create: `rnd-workbench/src/main/resources/db/migration/postgresql/V16__third_party_login_settings.sql`
- Modify: `rnd-workbench/src/main/java/com/rnd/app/service/SystemSettingService.java`
- Create: `rnd-workbench/src/main/java/com/rnd/app/dto/ThirdPartyLoginSettingsDto.java`
- Test: `rnd-workbench/src/test/java/com/rnd/app/service/SystemSettingServiceTest.java`

**Interfaces:**
- `SystemSettingService.getThirdPartyLoginSettings()` returns `ThirdPartyLoginSettingsDto` with `enabled`, `feishuEnabled`, `dingtalkEnabled`, `wecomEnabled`.
- `SystemSettingService.updateThirdPartyLoginSettings(ThirdPartyLoginSettingsDto)` validates the master/provider relationship and persists four keys.

- [ ] Add four settings keys with disabled defaults to both migration trees.
- [ ] Add DTO fields and getters/setters following existing Lombok DTO style.
- [ ] Add service constants, defaults, read, and transactional update methods; reject an enabled master switch with no provider selected using `BusinessException(BAD_REQUEST, ...)`.
- [ ] Add service tests covering missing defaults, valid update, disabled master, and no-provider rejection.
- [ ] Run `mvn -o -q -Dtest=SystemSettingServiceTest test` and expect PASS.

### Task 2: Admin and public settings endpoints

**Files:**
- Modify: `rnd-workbench/src/main/java/com/rnd/app/controller/SystemSettingController.java`
- Create: `rnd-workbench/src/main/java/com/rnd/app/dto/ThirdPartyLoginProvidersDto.java`
- Create or modify: `rnd-workbench/src/test/java/com/rnd/app/controller/SystemSettingControllerTest.java`

**Interfaces:**
- `GET /api/v1/settings/third-party-login` is admin-only and returns all four editable flags.
- `PUT /api/v1/settings/third-party-login` is admin-only and updates the flags.
- `GET /api/v1/auth/login-providers` is public and returns `{enterprise: true, thirdParty: false, providers: []}` or enabled provider keys.

- [ ] Add admin GET/PUT endpoints using the existing `requireAdmin()` boundary.
- [ ] Add public provider endpoint that returns only display-safe provider keys and never settings secrets.
- [ ] Add controller tests for admin authorization, update response, disabled response, and enabled provider response.
- [ ] Run the focused controller tests and expect PASS.

### Task 3: Login page dynamic provider buttons

**Files:**
- Modify: `rnd-workbench/src/main/resources/static/script.js`
- Modify: `rnd-workbench/src/main/resources/static/styles.css`
- Test: `node --check /tmp/extracted-login-script.js` and browser smoke check against the running app.

- [ ] Change the visible enterprise button text to “企业统一认证登录”.
- [ ] Add a public provider-config fetch before showing the login form; render Feishu, DingTalk, and WeCom buttons only when returned by the server.
- [ ] Keep the existing local login form usable if the public config request fails.
- [ ] Add accessible labels and provider-specific error text; do not put provider tokens in browser storage.
- [ ] Run JavaScript syntax check and verify all provider combinations by toggling returned configuration in a local test response.

### Task 4: Admin settings page

**Files:**
- Modify: `rnd-workbench/src/main/resources/static/script.js`
- Modify: `rnd-workbench/src/main/resources/static/styles.css`

- [ ] Extend `showSystemSettings()` with a “集成第三方协同 APP 登录” card.
- [ ] Render the three provider checkboxes only when the master switch is enabled; disable them when it is off.
- [ ] Submit the four flags to the admin endpoint and show server validation errors inline/toast.
- [ ] Preserve the existing project-code settings card and navigation.

### Task 5: Verification and Git checkpoint

**Files:**
- Modify only files listed above.

- [ ] Run `node --check` for the frontend script.
- [ ] Run `mvn -o -q -Dtest=SystemSettingServiceTest,SystemSettingControllerTest test`.
- [ ] Run the complete Maven test suite and inspect failure count.
- [ ] Review `git diff --check` and `git status --short`; ensure unrelated pre-existing changes are not staged.
- [ ] Commit only this implementation with message `feat: add configurable collaboration app login settings`.
- [ ] Report that real Keycloak/Feishu/DingTalk/WeCom callback authentication remains deployment-config dependent until credentials and callback URLs are supplied.
