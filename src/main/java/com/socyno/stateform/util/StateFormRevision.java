package com.socyno.stateform.util;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain=true)
public class StateFormRevision {
    private Long id;
    private String stateFormStatus;
    private long stateFormRevision;
}
