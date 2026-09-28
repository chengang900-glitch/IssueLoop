# Project Navigation and Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep project navigation compact while providing searchable project lists and administrator project/member management.

**Architecture:** Extend project listing with an administrator full-list branch while retaining membership restrictions for normal users. Keep recent-project ordering in localStorage, and add list/management modals to the static frontend.

**Tech Stack:** Spring Boot, Spring Data JPA, vanilla JavaScript, JUnit 5.

## Global Constraints

- Sidebar shows at most five recent projects.
- Only ADMIN sees full project management and the create button.
- Do not add project archival behavior in this scope.
- Validate locally and do not package automatically.

---

### Task 1: Add controlled project listing for administrators

**Files:**
- Modify: `rnd-workbench/src/main/java/com/rnd/app/controller/ProjectController.java`
- Modify: `rnd-workbench/src/main/java/com/rnd/app/service/ProjectService.java`
- Test: `rnd-workbench/src/test/java/com/rnd/app/service/ProjectServiceTest.java`

- [ ] Add an admin-only full project listing branch and retain member-only results for normal users.
- [ ] Verify a non-admin remains limited to project memberships.

### Task 2: Build compact sidebar navigation and project views

**Files:**
- Modify: `rnd-workbench/src/main/resources/static/index.html`
- Modify: `rnd-workbench/src/main/resources/static/script.js`
- Modify: `rnd-workbench/src/main/resources/static/styles.css`

- [ ] Store and render at most five recent project IDs in localStorage.
- [ ] Add project switcher, my-projects view, admin project-management view, project edit form, and member-management form.
- [ ] Wire project creation, update, add/change/remove member APIs and preserve existing project selection.

### Task 3: Verify no-project and multi-project flows

**Files:**
- Verify: `rnd-workbench/src/main/resources/static/script.js`

- [ ] Run `node --check src/main/resources/static/script.js`.
- [ ] Run `mvn test`.
- [ ] Smoke test login, create projects, sidebar truncation, switching, and member management locally.
