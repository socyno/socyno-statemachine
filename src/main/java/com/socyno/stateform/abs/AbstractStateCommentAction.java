package com.socyno.stateform.abs;

public abstract class AbstractStateCommentAction<S extends AbstractStateForm> extends AbstractStateAction<S, BasicStateForm, Void> {
    
    public static final String getFormLogEvent() {
        return "system:comment";
    }
    
    @Override
    public final Boolean messageRequired() {
        return true;
    }
    
    @Override
    public boolean getStateRevisionChangeIgnored() {
        return true;
    }
    
    public AbstractStateCommentAction(String display, String ...sourceStates) {
        super(display, sourceStates, (String)null);
    }
}
