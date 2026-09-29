package com.rnd.app.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Data
public class AnalyticsQuery {
    private String scope = "report";
    private Long projectId;
    private Long ownerId;
    private String type;
    private String status;
    private String keyword;
    private String dateBasis = "completed";
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) private LocalDate to;
    private String groupBy = "project";
    private String period = "month";
    private String lane = "all";
    private String drillDimension;
    private String drillKey;
    private int page = 1;
    private int size = 20;
}
