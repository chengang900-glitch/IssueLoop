package com.rnd.app.dto;
import lombok.Getter; import lombok.Setter;
import java.util.List;
@Getter @Setter public class BulkUpdateRequest {
 private List<String> ids; private String status; private Long ownerId; private Long sprintId;
 private String priority; private String module; private List<String> addTags; private List<String> removeTags;
}
