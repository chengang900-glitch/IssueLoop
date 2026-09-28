# V1.2 P1/P2 Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the approved P1 collaboration rules and P2 operational experience without changing the V1.2 project-management baseline.

**Architecture:** Add minimal work-item closure metadata and explicit list endpoints; keep activity records as the audit source; use Flyway V7 in H2 and PostgreSQL; keep the native frontend and centralize reusable UI error/empty states in `script.js`.

**Tech Stack:** Spring Boot 2.7, Spring Data JPA, Flyway, H2, PostgreSQL, vanilla JavaScript.

## Global Constraints

- All work-item, attachment, comment, watcher, and audit queries must enforce project membership.
- Do not add third-party dependencies or alter existing API success shapes.
- Validate with `node --check`, `mvn -q test package`, a local UI smoke flow, and JAR inspection.

---

### Task 1: Enforce administrator safety and recorded closure reasons

**Files:**
- Modify: `UserController.java`, `ProjectService.java`, `WorkItemService.java`, `WorkItemController.java`, `UpdateStatusRequest.java`, `WorkItem.java`
- Create: `db/migration/h2/V7__work_item_closure_reason.sql`, `db/migration/postgresql/V7__work_item_closure_reason.sql`
- Test: `ProjectServiceTest.java`, `WorkItemServiceTest.java`, `UserControllerTest.java`

- [ ] Prevent disabling/demoting the final system administrator and removing/demoting the final project administrator.
- [ ] Require a reason when closing or reopening a work item; persist it and include it in the activity record.
- [ ] Add failing tests for both guards and legal reasoned transitions, then run focused tests.

### Task 2: Complete saved filters, watchers, mentions, and image references

**Files:**
- Modify: `SavedFilterController.java`, `SavedFilterService.java`, `WorkItemController.java`, `WorkItemService.java`, `WorkItemWatcherRepository.java`, `CommentController.java`, `CommentService.java`, `MiscController.java`, `script.js`
- Test: `SavedFilterServiceTest.java`, `WorkItemServiceTest.java`, `AttachmentServiceTest.java`

- [ ] Add private saved-filter copy and watcher-list/current-user watched-item endpoints with project checks.
- [ ] Validate comment mentions against project membership before notifying mentioned users.
- [ ] When an attachment that is embedded in a description is removed, replace its image marker with a deterministic deleted-image placeholder.

### Task 3: Deliver P2 page states, report links, and operational visibility

**Files:**
- Modify: `script.js`, `index.html`, `styles.css`, `MiscController.java`
- Create: `docs/operations/backup-and-production-checklist.md`
- Test: local browser smoke flow

- [ ] Add reusable loading/error/empty rendering to project management, work-item list, and project report views.
- [ ] Add links from project brief risk/due/activity widgets to their underlying list/detail context.
- [ ] Expose a project activity audit list to project administrators and document backup, restore, production checks, and expected data-retention responsibilities.

### Task 4: Contract, regression, and package verification

**Files:**
- Modify: `docs/superpowers/specs/2026-07-11-v1-2-p0-api-acceptance.md`
- Output: `target/rnd-workbench-1.0.0-SNAPSHOT.jar`

- [ ] Record all new request fields, permissions, and error cases.
- [ ] Run `node --check src/main/resources/static/script.js` and `mvn -q test package`.
- [ ] Run the local login → project management → create → watcher/filter → archive-readonly smoke path and inspect the packaged classes.
