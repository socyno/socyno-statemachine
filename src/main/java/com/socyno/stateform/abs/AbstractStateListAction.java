package com.socyno.stateform.abs;

public abstract class AbstractStateListAction<S extends AbstractStateForm, F extends AbstractStateForm, T>
        extends AbstractStateAction<S, F, T> {
    public AbstractStateListAction(String display) {
        super(display, (String) null, (String) null);
    }
}
