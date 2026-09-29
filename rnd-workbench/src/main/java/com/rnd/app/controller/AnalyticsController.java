package com.rnd.app.controller;

import com.rnd.app.dto.AnalyticsQuery;
import com.rnd.app.service.AnalyticsService;
import com.rnd.app.util.ApiResponse;
import com.rnd.app.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsService service;

    @GetMapping
    public ApiResponse analyze(@ModelAttribute AnalyticsQuery query) {
        return ApiResponse.ok(service.analyze(query, SecurityUtil.currentUserId()));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@ModelAttribute AnalyticsQuery query, @RequestParam(defaultValue = "summary") String format) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=project-report-" + ("details".equals(format) ? "details" : "summary") + ".csv")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(service.export(query, SecurityUtil.currentUserId(), format));
    }
}
