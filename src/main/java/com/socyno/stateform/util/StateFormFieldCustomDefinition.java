package com.socyno.stateform.util;

import java.util.Map;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain=true)
public class StateFormFieldCustomDefinition {
    private String      field;
    private String      title;
    private String      template;
    private String      description;
    private Integer     position;
    private String      pattern;
    
    private Map<String, String> attributes;
}
