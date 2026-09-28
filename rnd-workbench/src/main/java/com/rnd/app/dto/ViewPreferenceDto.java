package com.rnd.app.dto;
import lombok.*;
import java.util.List;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ViewPreferenceDto {
    private List<String> columns;
    private String sort;
    private String groupBy;
}
