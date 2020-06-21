package com.socyno.stateform.util;


import com.socyno.stateform.abs.AbstractStateForm;

import lombok.Data;

@Data
public class StateFormEventContext<F extends AbstractStateForm> {
    private final String message;
    private final F form;
    
    public StateFormEventContext(String message, F form) {
        this.form = form;
        this.message = message;
    }
}
