package com.rnd.app.entity;

import lombok.*;
import java.io.Serializable;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class ProjectCodeCounterId implements Serializable {
    private String prefix;
    private String period;
}
