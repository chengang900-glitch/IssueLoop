package com.rnd.app;

import com.rnd.app.entity.Attachment;
import com.rnd.app.entity.Project;
import com.rnd.app.entity.ProjectMember;
import com.rnd.app.entity.User;
import com.rnd.app.entity.WorkItem;
import com.rnd.app.repository.AttachmentRepository;
import com.rnd.app.repository.ProjectMemberRepository;
import com.rnd.app.repository.ProjectRepository;
import com.rnd.app.repository.UserRepository;
import com.rnd.app.repository.WorkItemRepository;
import com.rnd.app.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 附件上传/下载的端到端用例。
 *
 * <p>修复前 upload 用的是相对路径 + {@code MultipartFile.transferTo}（容器按 multipart
 * 临时目录解析），而下载按进程工作目录解析 —— 两个基准不一致，文件可能根本没落到
 * DB 记录指向的位置。此前仓库里只有附件删除的测试，没有覆盖真实上传链路。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class AttachmentUploadIntegrationTest {

    private static final Path STORAGE_ROOT = createStorageRoot();

    private static Path createStorageRoot() {
        try {
            return Files.createTempDirectory("issueloop-attachment-upload").toAbsolutePath().normalize();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.path", STORAGE_ROOT::toString);
    }

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ProjectRepository projects;
    @Autowired ProjectMemberRepository members;
    @Autowired WorkItemRepository items;
    @Autowired AttachmentRepository attachments;
    @Autowired JwtUtil jwt;

    @Test
    void uploadWritesFileWhereTheRecordPointsAndDownloadServesIt() throws Exception {
        User user = users.save(User.builder()
                .username(UUID.randomUUID() + "@example.test").nickname("附件用户")
                .passwordHash("unused").systemRole("ADMIN").build());
        Project project = projects.save(Project.builder()
                .name("附件项目").shortName("附件")
                .code("ATT" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                .createdBy(user.getId()).build());
        members.save(ProjectMember.builder()
                .projectId(project.getId()).userId(user.getId()).role("PROJECT_ADMIN").build());
        String workItemId = "ATT-" + UUID.randomUUID().toString().substring(0, 8);
        items.save(WorkItem.builder().id(workItemId).seqNo(1001).projectId(project.getId())
                .type("任务").title("附件用例").status("新建").priority("P2").severity("普通")
                .creatorId(user.getId()).build());
        String token = jwt.generate(user.getId(), user.getUsername(), user.getSystemRole(), user.getTokenVersion());

        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain",
                "hello".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/v1/work-items/" + workItemId + "/attachments").file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storagePath").isString());

        Attachment stored = attachments.findByWorkItemId(workItemId).get(0);
        Path onDisk = STORAGE_ROOT.resolve(stored.getStoragePath()).normalize();
        assertTrue(onDisk.startsWith(STORAGE_ROOT), "附件必须落在配置的存储根目录内：" + onDisk);
        assertTrue(Files.exists(onDisk), "附件文件必须存在于记录指向的路径：" + onDisk);
        assertEquals("hello", Files.readString(onDisk));

        mvc.perform(get("/api/v1/attachments/" + stored.getId() + "/download")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
                .andExpect(content().bytes("hello".getBytes(StandardCharsets.UTF_8)));
    }
}
