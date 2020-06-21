package com.socyno.stateform.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class StateFlowLinkData {
    
    private String to;
    
    private String from;
    
    private String linkText;
    
    public StateFlowLinkData(String from, String to) {
        this.from = from;
        this.to = to;
    }
    
}
