package com.rnd.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rnd.app.config.RndPrincipal;
import com.rnd.app.controller.UserController;
import com.rnd.app.dto.*;
import com.rnd.app.entity.*;
import com.rnd.app.repository.*;
import com.rnd.app.service.*;
import com.rnd.app.util.BusinessException;
import com.rnd.app.util.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:repair_regression;DB_CLOSE_DELAY=-1",
        "app.storage.path=./target/repair-attachments",
        "app.export.path=./target/repair-exports",
        "app.external-auth.keycloak.enabled=true",
        "app.external-auth.keycloak.authorization-uri=https://example.test/auth",
        "app.external-auth.keycloak.token-uri=https://example.test/token",
        "app.external-auth.keycloak.user-info-uri=https://example.test/userinfo",
        "app.external-auth.keycloak.client-id=repair-test",
        "app.external-auth.keycloak.client-secret=test-only",
        "app.external-auth.feishu.enabled=true",
        "app.external-auth.feishu.authorization-uri=https://example.test/auth",
        "app.external-auth.feishu.token-uri=https://example.test/token",
        "app.external-auth.feishu.app-token-uri=https://example.test/app-token",
        "app.external-auth.feishu.user-info-uri=https://example.test/userinfo",
        "app.external-auth.feishu.client-id=repair-test",
        "app.external-auth.feishu.client-secret=test-only"
})
class RepairRegressionIntegrationTest {
    @Autowired com.rnd.app.config.AppConfig config;
    @Autowired UserRepository users;
    @Autowired ProjectRepository projects;
    @Autowired ProjectMemberRepository memberships;
    @Autowired WorkItemRepository items;
    @Autowired AttachmentRepository attachments;
    @Autowired AuthLoginTransactionRepository transactions;
    @Autowired UserController userController;
    @Autowired ExternalAuthService external;
    @Autowired AuthService auth;
    @Autowired SystemSettingService settings;
    @Autowired ProjectService projectService;
    @Autowired WorkItemService workItems;
    @Autowired AttachmentService attachmentService;
    @Autowired BulkWorkItemService bulk;
    @Autowired ViewPreferenceService views;
    @Autowired ExportService exports;
    @Autowired ObjectMapper mapper;
    @Autowired PlatformTransactionManager transactionManager;
    User owner;
    Project project;
    int sequence;

    @BeforeEach
    void setup() {
        owner = user("USER");
        project = projects.save(Project.builder().code(UUID.randomUUID().toString().substring(0, 20))
                .name("修复回归项目").shortName("回归").createdBy(owner.getId()).build());
        memberships.save(ProjectMember.builder().projectId(project.getId()).userId(owner.getId()).role("MEMBER").build());
    }

    @Test
    void concurrentDisablingAndDemotionKeepAnEnabledAdministrator() throws Exception {
        List<User> originalAdmins = new ArrayList<>();
        for (User user : users.findAll()) if ("ADMIN".equals(user.getSystemRole())) {
            originalAdmins.add(user);
            user.setSystemRole("USER"); users.save(user);
        }
        User first = user("ADMIN"), second = user("ADMIN");
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2), start = new CountDownLatch(1);
        try {
            Future<Boolean> disable = pool.submit(() -> changeAdmin(first, false, ready, start));
            Future<Boolean> demote = pool.submit(() -> changeAdmin(second, true, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS)); start.countDown();
            int succeeded = (disable.get(10, TimeUnit.SECONDS) ? 1 : 0) + (demote.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, succeeded);
            assertEquals(1, users.countBySystemRoleAndStatus("ADMIN", 1));
        } finally {
            start.countDown(); pool.shutdownNow();
            users.deleteById(first.getId()); users.deleteById(second.getId());
            for (User old : originalAdmins) {
                User current = users.findById(old.getId()).orElseThrow();
                current.setSystemRole("ADMIN"); users.save(current);
            }
        }
    }

    private boolean changeAdmin(User user, boolean demote, CountDownLatch ready, CountDownLatch start) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new RndPrincipal(user.getId(), user.getUsername(), "ADMIN"), null, List.of()));
        ready.countDown(); start.await(5, TimeUnit.SECONDS);
        try {
            if (demote) {
                UserController.UpdateUserRequest request = new UserController.UpdateUserRequest();
                request.setSystemRole("USER"); userController.updateUser(user.getId(), request);
            } else {
                UserController.StatusRequest request = new UserController.StatusRequest();
                request.setStatus(0); userController.updateStatus(user.getId(), request);
            }
            return true;
        } catch (BusinessException error) {
            assertEquals(ErrorCode.STATUS_CONFLICT, error.getErrorCode()); return false;
        } finally { SecurityContextHolder.clearContext(); }
    }

    @Test
    void disabledLoginIsRejectedAtStartCallbackExchangeAndBind() {
        settings.updateThirdPartyLoginSettings(new ThirdPartyLoginSettingsDto());
        AuthLoginTransaction tx = transactions.save(AuthLoginTransaction.builder().state(UUID.randomUUID().toString())
                .provider("feishu").redirectUri("https://example.test/callback").expiresAt(Instant.now().plusSeconds(60))
                .externalSubject("test-subject").localUserId(owner.getId()).build());
        forbidden(() -> external.start("feishu"));
        forbidden(() -> external.callback("feishu", null, tx.getState(), "access_denied", "cancelled"));
        forbidden(() -> external.exchange(tx.getState()));
        forbidden(() -> external.bind(tx.getState(), owner.getUsername(), "unused", auth));
        assertFalse(transactions.findById(tx.getState()).orElseThrow().isConsumed());
        assertEquals(0, users.findById(owner.getId()).orElseThrow().getFailCount());
        ThirdPartyLoginSettingsDto enabled = new ThirdPartyLoginSettingsDto();
        enabled.setEnabled(true); enabled.setDingtalkEnabled(true);
        settings.updateThirdPartyLoginSettings(enabled);
        forbidden(() -> external.start("feishu")); // individual platform switch
        enabled.setFeishuEnabled(true); settings.updateThirdPartyLoginSettings(enabled);
        assertNotNull(external.start("feishu"));
        assertNotNull(external.exchange(tx.getState()).getToken());
    }

    private void forbidden(Runnable action) {
        assertEquals(ErrorCode.FORBIDDEN, assertThrows(BusinessException.class, action::run).getErrorCode());
    }

    @Test
    void authorizationUrlEncodesScopeAndCallbackQuery() {
        com.rnd.app.config.AppConfig.ProviderConfig provider = config.getExternalAuth().getKeycloak();
        String original = provider.getRedirectUri();
        String redirect = "https://example.test/callback?return=a&other=x";
        provider.setRedirectUri(redirect);
        try {
            String url = external.start("keycloak");
            assertTrue(url.contains("scope=openid%20profile%20email"));
            Map<String, String> query = new HashMap<>();
            for (String parameter : java.net.URI.create(url).getRawQuery().split("&")) {
                String[] pair = parameter.split("=", 2);
                query.put(pair[0], URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
            }
            assertEquals(redirect, query.get("redirect_uri"));
            assertEquals("S256", query.get("code_challenge_method"));
        } finally { provider.setRedirectUri(original); }
    }

    @Test
    void defaultAndNewViewColumnsSaveAndExportCorrectValues() throws Exception {
        WorkItem task = item("导出任务", owner.getId());
        task.setPlannedStartDate(LocalDate.of(2026, 10, 1));
        task.setActualCompletedAt(Instant.parse("2026-10-01T08:00:00Z")); task.setActualHours(new BigDecimal("2.50"));
        items.save(task);
        ExportWorkItemsRequest request = new ExportWorkItemsRequest();
        request.setColumns(ViewPreferenceService.DEFAULT_COLUMNS);
        assertEquals("READY", exports.create(project.getId(), owner.getId(), request).getStatus());
        List<String> columns = List.of("project", "plannedStartDate", "actualCompletedAt", "actualHours");
        views.save(project.getId(), owner.getId(), new ViewPreferenceDto(columns, "createdAt,desc", null));
        assertEquals(columns, views.get(project.getId(), owner.getId()).getColumns());
        request.setColumns(columns);
        ExportJobDto job = exports.create(project.getId(), owner.getId(), request);
        try (ZipFile zip = new ZipFile(exports.download(job.getId(), owner.getId()).toFile())) {
            String xml = new String(zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(xml.contains("修复回归项目")); assertTrue(xml.contains("2026-10-01"));
            assertTrue(xml.contains("2026-10-01T08:00:00Z")); assertTrue(xml.contains("2.50"));
        }
    }

    @Test
    void boardUsesListFiltersAndKeepsPerColumnLimitAndTotals() {
        for (int i = 0; i < 3; i++) item("匹配任务 " + i, owner.getId());
        item("不应显示", owner.getId()); item("匹配任务 其他人", user("USER").getId());
        ExportWorkItemsRequest filters = new ExportWorkItemsRequest();
        filters.setKeyword("匹配任务"); filters.setOwnerId(owner.getId());
        filters.setStatus("新建"); filters.setView("assigned-to-me");
        List<Map<String, Object>> columns = workItems.board(project.getId(), 2, filters, owner.getId());
        assertEquals(3L, columns.get(0).get("total")); assertEquals(2, columns.get(0).get("shown"));
        assertEquals(true, columns.get(0).get("truncated"));
        for (int i = 1; i < columns.size(); i++) assertEquals(0L, columns.get(i).get("total"));
    }

    @Test
    void explicitNullClearsProjectDatesAndOmittedDatesRemain() throws Exception {
        project.setPlannedStartDate(LocalDate.of(2026, 10, 1)); project.setGoLiveDate(LocalDate.of(2026, 12, 1));
        project = projects.save(project);
        CreateProjectRequest request = mapper.readValue("{\"plannedStartDate\":null}", CreateProjectRequest.class);
        projectService.updateProject(project.getId(), request);
        Project saved = projects.findById(project.getId()).orElseThrow();
        assertNull(saved.getPlannedStartDate()); assertEquals(LocalDate.of(2026, 12, 1), saved.getGoLiveDate());
    }

    @Test
    void singleAndBulkTaskDeletionRemoveAttachmentFiles() throws Exception {
        WorkItem first = item("单条删除", owner.getId()), second = item("批量删除", owner.getId());
        Path firstFile = fileFor(first), secondFile = fileFor(second);
        workItems.delete(first.getId()); assertFalse(Files.exists(firstFile));
        BulkDeleteRequest request = new BulkDeleteRequest(); request.setIds(List.of(second.getId())); request.setConfirmation("确认删除");
        assertEquals(List.of(second.getId()), bulk.delete(project.getId(), request).getSuccesses());
        assertFalse(Files.exists(secondFile));
        assertTrue(attachments.findByWorkItemId(first.getId()).isEmpty());
        assertTrue(attachments.findByWorkItemId(second.getId()).isEmpty());
    }

    @Test
    void rolledBackTaskDeletionRestoresTaskAttachmentAndFile() throws Exception {
        WorkItem task = item("回滚删除", owner.getId()); Path file = fileFor(task);
        try {
            assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactionManager).execute(status -> {
                workItems.delete(task.getId()); throw new IllegalStateException("rollback");
            }));
            assertTrue(items.existsById(task.getId())); assertTrue(Files.exists(file));
            assertEquals(1, attachments.findByWorkItemId(task.getId()).size());
        } finally { workItems.delete(task.getId()); }
    }

    private Path fileFor(WorkItem task) throws Exception {
        Path root = Paths.get("./target/repair-attachments").toAbsolutePath();
        AttachmentDto attachment = attachmentService.upload(task.getId(), "test.txt", 4, "text/plain", owner.getId(), root);
        Path file = root.resolve(attachment.getStoragePath()); Files.writeString(file, "test"); return file;
    }

    private User user(String role) {
        return users.save(User.builder().username(UUID.randomUUID().toString()).nickname("回归用户")
                .passwordHash("unused").systemRole(role).build());
    }

    private WorkItem item(String title, Long ownerId) {
        return items.save(WorkItem.builder().id("R" + UUID.randomUUID().toString().replace("-", "").substring(0, 14))
                .seqNo(++sequence).projectId(project.getId()).type("任务").title(title).ownerId(ownerId)
                .creatorId(owner.getId()).description("回归验证").build());
    }
}
