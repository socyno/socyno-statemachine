package com.socyno.stateform.util;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class StateFormDynamicViewField {
    private final String field;
    private final Class<?> type;
    private final Map<String, Object> attributes;
    
    public StateFormDynamicViewField(String field, Class<?> clazz) {
        this(field, clazz, null);
    }
    
    public StateFormDynamicViewField(String field, Class<?> type, Map<String, Object> attributes) {
        this.field = field;
        this.type = type;
        this.attributes = attributes;
    }
}
