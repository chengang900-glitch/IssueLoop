package com.rnd.app;

import com.rnd.app.dto.AnalyticsQuery;
import com.rnd.app.entity.*;
import com.rnd.app.repository.*;
import com.rnd.app.service.AnalyticsService;
import com.rnd.app.util.JwtUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnalyticsIntegrationTest {
    @Autowired AnalyticsService analytics;
    @Autowired UserRepository users;
    @Autowired ProjectRepository projects;
    @Autowired ProjectMemberRepository members;
    @Autowired WorkItemRepository items;
    @Autowired MockMvc mvc;
    @Autowired JwtUtil jwt;
    User me, other;
    Project project, secret;
    int sequence;

    @BeforeEach void setup() {
        me = users.save(User.builder().username("analytics-me").nickname("报表用户").passwordHash("unused").build());
        other = users.save(User.builder().username("analytics-other").nickname("其他人员").passwordHash("unused").build());
        project = projects.save(Project.builder().code("ANALYTICS").name("测试项目").shortName("测").createdBy(me.getId()).build());
        secret = projects.save(Project.builder().code("PRIVATE").name("不可访问项目").shortName("密").createdBy(other.getId()).build());
        members.save(ProjectMember.builder().projectId(project.getId()).userId(me.getId()).role("MEMBER").build());
        members.save(ProjectMember.builder().projectId(project.getId()).userId(other.getId()).role("MEMBER").build());
    }

    @Test void fullAggregationAndDrillIgnoreListPaginationAndKeepMissingHoursDistinct() {
        for (int i = 0; i < 215; i++) item(project, me, "进行中", new BigDecimal("1.25"), null);
        item(project, other, "新建", null, null);
        item(secret, other, "进行中", new BigDecimal("999"), null);
        AnalyticsQuery q = query("project");
        Map<String,Object> data = analytics.analyze(q, me.getId());
        assertEquals(216, metric(data, "total").intValue());
        assertEquals(216, data.get("total"));
        assertEquals(20, list(data).size());
        assertEquals(new BigDecimal("268.75"), metric(data, "hours"));
        assertEquals(1L, metric(data, "missingHours"));
        q.setDrillDimension("person"); q.setDrillKey(me.getId().toString()); q.setPage(11);
        data = analytics.analyze(q, me.getId());
        assertEquals(215, data.get("total")); assertEquals(15, list(data).size());
        String csv = new String(analytics.export(q, me.getId(), "details"), StandardCharsets.UTF_8);
        assertEquals(218, csv.lines().count()); // metadata + criteria + header + all matching tasks
        assertFalse(csv.contains("不可访问项目"));
    }

    @Test void personalOverviewExcludesOtherOwnersButInboxIncludesAuthorizedReview() {
        item(project, me, "进行中", null, null);
        WorkItem review = item(project, other, "已完成", BigDecimal.ONE, Instant.now());
        review.setCreatorId(me.getId()); items.save(review);
        item(project, other, "已完成", BigDecimal.ONE, Instant.now());
        item(project, other, "新建", null, null);
        AnalyticsQuery q = query("personal"); q.setLane("pending");
        Map<String,Object> data = analytics.analyze(q, me.getId());
        assertEquals(1, metric(data, "total").intValue()); assertEquals(1L, data.get("pendingCount")); assertEquals(1, data.get("total"));
        q.setLane("review");
        assertEquals(1, analytics.analyze(q, me.getId()).get("total"));
        q.setLane("pending");
        ProjectMember member = members.findByProjectIdAndUserId(project.getId(), me.getId()).orElseThrow();
        member.setRole("PROJECT_ADMIN"); members.save(member);
        data = analytics.analyze(q, me.getId());
        assertEquals(1L, data.get("pendingCount")); assertEquals(1, data.get("total"));
        q.setLane("review");
        assertEquals(2, analytics.analyze(q, me.getId()).get("total"));
        q.setLane("pending");
        member.setRole("GUEST"); members.save(member);
        data = analytics.analyze(q, me.getId());
        assertEquals(0L, data.get("pendingCount")); assertEquals(0, data.get("total"));
        assertEquals(1, metric(data, "total").intValue());
    }

    @Test void completionDateUsesShanghaiBoundariesAndExcludesReopenedTasks() {
        item(project, me, "已验收", new BigDecimal("6.50"), Instant.parse("2026-08-31T16:00:00Z"));
        item(project, me, "已完成", new BigDecimal("2.25"), Instant.parse("2026-09-30T15:59:59Z"));
        item(project, me, "已完成", new BigDecimal("50"), Instant.parse("2026-09-30T16:00:00Z"));
        item(project, me, "进行中", new BigDecimal("80"), Instant.parse("2026-09-01T00:00:00Z"));
        item(project, me, "已完成", null, null);
        AnalyticsQuery q = query("report"); q.setFrom(LocalDate.of(2026, 9, 1)); q.setTo(LocalDate.of(2026, 9, 30)); q.setGroupBy("time");
        Map<String,Object> data = analytics.analyze(q, me.getId());
        assertEquals(2, data.get("total")); assertEquals(new BigDecimal("8.75"), metric(data, "completedHours"));
        assertEquals("2026-09", ((List<Map<String,Object>>)data.get("groups")).get(0).get("key"));
        q.setDrillDimension("time"); q.setDrillKey("2026-09");
        assertEquals(2, analytics.analyze(q, me.getId()).get("total"));
    }

    @Test void csvEscapesTextAndSummaryMatchesNumbers() {
        WorkItem w = item(project, me, "已完成", BigDecimal.ZERO, Instant.now());
        w.setTitle("=HYPERLINK(\"bad\")"); items.save(w);
        AnalyticsQuery q = query("report");
        String csv = new String(analytics.export(q, me.getId(), "details"), StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFF")); assertTrue(csv.contains("'=HYPERLINK(\"\"bad\"\")"));
        assertEquals(0L, metric(analytics.analyze(q, me.getId()), "missingHours"));
        String summary = new String(analytics.export(q, me.getId(), "summary"), StandardCharsets.UTF_8);
        assertTrue(summary.contains("\"测试项目\",\"1\",\"0\",\"0\",\"1\""));
    }

    @Test void endpointsEnforceMembershipAndRejectInvalidDates() throws Exception {
        String token = jwt.generate(me.getId(), me.getUsername(), "USER", me.getTokenVersion());
        mvc.perform(get("/api/v1/analytics")).andExpect(status().isUnauthorized());
        for (String path : List.of("/api/v1/analytics", "/api/v1/analytics/export")) {
            mvc.perform(get(path).param("scope", "project").param("projectId", secret.getId().toString()).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/analytics").param("from", "2026-10-01").param("to", "2026-09-01").header("Authorization", "Bearer " + token)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/analytics").param("scope", "personal").header("Authorization", "Bearer " + token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
        project.setArchived(true); projects.save(project);
        mvc.perform(get("/api/v1/analytics/export").param("projectId", project.getId().toString()).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
    }

    @Test void overdueAndUnknownHoursHaveExplicitMeaning() {
        WorkItem overdue = item(project, me, "进行中", null, null); overdue.setDueDate(Instant.now().minusSeconds(3600)); items.save(overdue);
        WorkItem review = item(project, me, "已完成", BigDecimal.ONE, Instant.now()); review.setDueDate(Instant.now().minusSeconds(3600)); items.save(review);
        AnalyticsQuery q = query("project"); q.setLane("overdue");
        Map<String,Object> data = analytics.analyze(q, me.getId());
        assertEquals(1, data.get("total")); assertEquals(1L, metric(data, "overdue")); assertEquals(1L, metric(data, "open"));
    }

    private AnalyticsQuery query(String scope) { AnalyticsQuery q = new AnalyticsQuery(); q.setScope(scope); q.setProjectId(project.getId()); return q; }
    private WorkItem item(Project p, User owner, String status, BigDecimal hours, Instant completed) {
        int seq = ++sequence;
        return items.save(WorkItem.builder().id("AN-" + seq).seqNo(seq).projectId(p.getId()).type("任务").title("报表任务" + seq).ownerId(owner.getId()).creatorId(other.getId()).status(status).actualHours(hours).actualCompletedAt(completed).build());
    }
    private Number metric(Map<String,Object> data, String key) { return (Number)((Map<?,?>)data.get("summary")).get(key); }
    private List<?> list(Map<String,Object> data) { return (List<?>)data.get("list"); }
}
