package com.rnd.app.dto;
import lombok.Getter; import lombok.Setter; import java.time.Instant; import java.util.List;
@Getter @Setter public class ExportWorkItemsRequest { private String type,status,priority,severity,module,tag,keyword,view,sort="createdAt,desc"; private Long ownerId,creatorId,sprintId; private Instant dueFrom,dueTo; private List<String> columns; }
