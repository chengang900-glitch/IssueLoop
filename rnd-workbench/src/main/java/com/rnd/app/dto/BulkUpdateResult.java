package com.rnd.app.dto;
import lombok.AllArgsConstructor; import lombok.Getter;
import java.util.List;
@Getter @AllArgsConstructor public class BulkUpdateResult {
 private List<String> successes; private List<Failure> failures;
 @Getter @AllArgsConstructor public static class Failure { private String id; private int code; private String reason; }
}
