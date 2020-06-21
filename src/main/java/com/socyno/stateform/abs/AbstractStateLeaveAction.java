package com.socyno.stateform.abs;

import org.apache.commons.lang3.StringUtils;

import com.socyno.stateform.exec.StateFormEmptyTargetStateException;

public abstract class AbstractStateLeaveAction<S extends AbstractStateForm> extends AbstractStateAction<S, AbstractStateForm, Void> {
    public AbstractStateLeaveAction(String display, String sourceState) {
        super(display, (String)null, sourceState);
        if (StringUtils.isBlank(sourceState)) {
            throw new StateFormEmptyTargetStateException();
        }
    }
    
    @Override
    public void check(String event, S originForm, String sourceState) {
        
    }
    
    protected boolean executeWhenNoStateChanged() {
        return false;
    }
}
