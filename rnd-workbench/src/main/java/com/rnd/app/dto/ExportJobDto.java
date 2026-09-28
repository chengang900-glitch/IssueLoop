package com.rnd.app.dto; import lombok.AllArgsConstructor; import lombok.Getter; import java.time.Instant;
@Getter @AllArgsConstructor public class ExportJobDto { private Long id; private String status; private String fileName; private Instant expiresAt; }
