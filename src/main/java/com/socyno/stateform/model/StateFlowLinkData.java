package com.socyno.stateform.model;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class StateFlowLinkData {
    
    private final String to;
    
    private final String from;
    
    private final String linkText;
    
    public StateFlowLinkData(String from, String to) {
        this(from, to, null);
    }
    
    public StateFlowLinkData(String from, String to, String linkText) {
        this.to = to;
        this.from = from;
        this.linkText = linkText;
    }
}
