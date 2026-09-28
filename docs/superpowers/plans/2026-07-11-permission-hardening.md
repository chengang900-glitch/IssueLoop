# Permission Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the three confirmed authorization gaps: disabled-login access, unchecked role values, and unauthorized project favorites.

**Architecture:** Keep authorization rules in the existing service/controller boundaries. Login checks account status before password processing; role validation remains close to the existing write paths; the favorite endpoint reuses `ProjectService.ensureProjectMember`.

**Tech Stack:** Spring Boot, Spring Security, Spring Data JPA, JUnit 5, Mockito.

## Global Constraints

- Modify only the confirmed permission gaps and their focused tests.
- Keep existing roles: system `ADMIN`/`USER`; project `PROJECT_ADMIN`/`MEMBER`/`GUEST`.
- Verify with Maven tests and packaging.

---

### Task 1: Prevent disabled users from receiving a JWT

**Files:**
- Modify: `rnd-workbench/src/main/java/com/rnd/app/service/AuthService.java`
- Create: `rnd-workbench/src/test/java/com/rnd/app/service/AuthServiceTest.java`

**Interfaces:**
- Consumes: `User.status` where `1` is active and `0` is disabled.
- Produces: `AuthService.login(String, String)` throws `BusinessException` for disabled accounts.

- [ ] **Step 1: Write the failing test**

```java
when(userRepo.findByUsername("disabled")).thenReturn(Optional.of(User.builder()
        .status(0).passwordHash("hash").build()));
assertThrows(BusinessException.class, () -> service.login("disabled", "password"));
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=AuthServiceTest test`

Expected: the disabled account receives a token before the code change.

- [ ] **Step 3: Write minimal implementation**

```java
if (!Integer.valueOf(1).equals(user.getStatus())) {
    throw new BusinessException(ErrorCode.FORBIDDEN, "账号已停用，请联系管理员");
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=AuthServiceTest test`

Expected: PASS.

### Task 2: Restrict persisted role values to the supported roles

**Files:**
- Modify: `rnd-workbench/src/main/java/com/rnd/app/service/ProjectService.java`
- Modify: `rnd-workbench/src/main/java/com/rnd/app/controller/UserController.java`
- Modify: `rnd-workbench/src/test/java/com/rnd/app/service/ProjectServiceTest.java`

**Interfaces:**
- Consumes: role request strings from member and user administration endpoints.
- Produces: invalid role values throw `BusinessException(BAD_REQUEST, ...)` before persistence.

- [ ] **Step 1: Write the failing test**

```java
assertThrows(BusinessException.class, () -> service.addMember(1L, 10L, "OWNER"));
assertThrows(BusinessException.class, () -> service.updateMemberRole(1L, 10L, "OWNER"));
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ProjectServiceTest test`

Expected: invalid role is accepted before the code change.

- [ ] **Step 3: Write minimal implementation**

```java
private void validateProjectRole(String role) {
    if (!List.of("PROJECT_ADMIN", "MEMBER", "GUEST").contains(role)) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的项目角色");
    }
}
```

Validate `role` after defaulting in `addMember` and before setting it in `updateMemberRole`; validate `ADMIN`/`USER` in the two user-admin write paths.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ProjectServiceTest test`

Expected: PASS.

### Task 3: Require project membership before adding a project favorite

**Files:**
- Modify: `rnd-workbench/src/main/java/com/rnd/app/controller/FavoriteController.java`
- Create: `rnd-workbench/src/test/java/com/rnd/app/controller/FavoriteControllerTest.java`

**Interfaces:**
- Consumes: `ProjectService.ensureProjectMember(Long projectId, Long userId)`.
- Produces: `POST /projects/{id}/favorites` checks membership before accessing `FavoriteRepository`.

- [ ] **Step 1: Write the failing test**

```java
controller.addFavorite(1L);
verify(projectService).ensureProjectMember(1L, 10L);
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=FavoriteControllerTest test`

Expected: verification fails because membership is not checked.

- [ ] **Step 3: Write minimal implementation**

```java
projectService.ensureProjectMember(id, uid);
```

Place it immediately after retrieving the current user ID, before checking or saving the favorite.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=FavoriteControllerTest test`

Expected: PASS.

### Task 4: Verify the complete permission-hardening change

**Files:**
- Verify: `rnd-workbench/pom.xml` test configuration

- [ ] **Step 1: Run the full test suite and package the application**

Run: `mvn test package`

Expected: all tests pass and the Spring Boot jar is built.
