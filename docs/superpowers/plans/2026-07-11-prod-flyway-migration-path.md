# Production Flyway Migration Path Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent the PostgreSQL production profile from scanning H2 migrations with duplicate Flyway versions.

**Architecture:** Separate PostgreSQL migration files into `db/migration/postgresql`; retain H2 files in their current test-only path. Configure the production profile to scan only the PostgreSQL location so each Flyway version is unique per active database.

**Tech Stack:** Spring Boot 2.7, Flyway 8, PostgreSQL, Maven.

## Global Constraints

- Preserve migration contents and version ordering V1 through V4.
- Keep the H2 default and test migration location unchanged.
- Deliver a repackaged Spring Boot executable JAR after tests pass.

---

### Task 1: Isolate PostgreSQL Flyway migrations

**Files:**
- Modify: `rnd-workbench/src/main/resources/application-prod.yml:9-10`
- Move: `rnd-workbench/src/main/resources/db/migration/V1__init_schema.sql`
- Move: `rnd-workbench/src/main/resources/db/migration/V2__seed_admin.sql`
- Move: `rnd-workbench/src/main/resources/db/migration/V3__global_work_item_counters.sql`
- Move: `rnd-workbench/src/main/resources/db/migration/V4__v1_1_collaboration.sql`

**Interfaces:**
- Consumes: production profile `spring.flyway.locations`.
- Produces: `classpath:db/migration/postgresql` containing exactly V1-V4 PostgreSQL migrations.

- [ ] **Step 1: Verify the pre-fix collision**

Run: `find src/main/resources/db/migration -type f | sort`

Expected: root migrations and `h2/` migrations both contain V1 through V4.

- [ ] **Step 2: Apply the minimal configuration and path change**

```yaml
spring:
  flyway:
    locations: classpath:db/migration/postgresql
```

Move the four PostgreSQL SQL files, unchanged, to `db/migration/postgresql/`.

- [ ] **Step 3: Verify the resource layout**

Run: `find src/main/resources/db/migration -type f | sort`

Expected: separate `h2/` and `postgresql/` directories, each containing V1-V4.

### Task 2: Build and inspect the deployable artifact

**Files:**
- Verify: `rnd-workbench/target/rnd-workbench-1.0.0-SNAPSHOT.jar`

- [ ] **Step 1: Run tests and package**

Run: `mvn clean package`

Expected: all tests pass and Maven reports `BUILD SUCCESS`.

- [ ] **Step 2: Inspect packaged migration resources**

Run: `jar tf target/rnd-workbench-1.0.0-SNAPSHOT.jar | rg 'db/migration/(h2|postgresql)/V[1-4]'`

Expected: eight files: four H2 and four PostgreSQL migrations, each in its isolated path.

- [ ] **Step 3: Deliver the new JAR and PowerShell command**

```powershell
java -jar .\rnd-workbench-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```
