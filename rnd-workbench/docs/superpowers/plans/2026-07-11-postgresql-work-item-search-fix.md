# PostgreSQL Work Item Search Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ensure a newly created project can load its empty work-item list on PostgreSQL when no filters are provided.

**Architecture:** Replace the static JPQL query that binds nullable parameters with a dynamic Spring Data JPA `Specification`. Only supplied filters produce predicates, so PostgreSQL never receives an untyped `NULL` bind parameter. Keep the existing endpoint, pagination, sorting, and personal-view semantics.

**Tech Stack:** Spring Boot 2.7, Spring Data JPA Specification, H2 integration tests, PostgreSQL production.

## Global Constraints

- Preserve the `/api/v1/projects/{id}/work-items` request and response contract.
- Do not alter project creation or database migrations.

---

### Task 1: Replace static nullable JPQL with dynamic predicates

**Files:**
- Modify: `src/main/java/com/rnd/app/repository/WorkItemRepository.java`
- Modify: `src/main/java/com/rnd/app/service/WorkItemService.java`

- [ ] Extend the repository with `JpaSpecificationExecutor<WorkItem>` and remove the static `search` query.
- [ ] Build a `Specification<WorkItem>` from only non-null filters and express each personal view as a predicate/subquery.
- [ ] Execute `findAll(specification, pageable)` and map the page to `WorkItemDto`.

### Task 2: Lock in the empty-filter regression case

**Files:**
- Modify: `src/test/java/com/rnd/app/repository/WorkItemPersonalViewTest.java`

- [ ] Invoke `WorkItemService.search` from the integration test so the tested route matches production behavior.
- [ ] Add an assertion that an `all` search with every optional filter omitted returns the project items.

### Task 3: Validate and package

**Files:**
- Output: `target/rnd-workbench-1.0.0-SNAPSHOT.jar`

- [ ] Run the targeted personal-view integration test.
- [ ] Run the full Maven test suite and package the executable JAR.
- [ ] Inspect the JAR to confirm it contains the updated work-item classes.
