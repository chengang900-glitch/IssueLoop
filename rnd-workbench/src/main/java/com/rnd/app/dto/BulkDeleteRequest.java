package com.rnd.app.dto;
import lombok.Getter; import lombok.Setter; import java.util.List;
@Getter @Setter public class BulkDeleteRequest { private List<String> ids; private String confirmation; }
