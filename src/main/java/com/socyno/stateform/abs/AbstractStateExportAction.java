package com.socyno.stateform.abs;

public abstract class AbstractStateExportAction<S extends AbstractStateForm, F extends AbstractStateForm, T>
        extends AbstractStateListAction<S, F, T> {
    public AbstractStateExportAction(String display) {
        super(display);
    }
}
