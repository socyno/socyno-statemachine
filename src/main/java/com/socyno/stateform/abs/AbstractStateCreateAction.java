package com.socyno.stateform.abs;

public abstract class AbstractStateCreateAction<S extends AbstractStateForm, F extends AbstractStateForm,
                        T extends AbstractStateForm> extends AbstractStateAction<S, F, T> {
    @Override
    public final Boolean messageRequired() {
        return null;
    }
    
    @Override
    public final EventFormType getEventFormType() throws Exception {
        return EventFormType.CREATE;
    }
    
    
    @Override
    public boolean getStateRevisionChangeIgnored() throws Exception {
        return false;
    }
    
    public AbstractStateCreateAction(String display, String targetState) {
        super(display, (String[]) null, targetState);
    }
    
    @Override
    public boolean allowHandleReturnNull() {
        return false;
    }
}
