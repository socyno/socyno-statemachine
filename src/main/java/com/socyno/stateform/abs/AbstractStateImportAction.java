package com.socyno.stateform.abs;

public abstract class AbstractStateImportAction<S extends AbstractStateForm, F extends AbstractStateForm, T>
        extends AbstractStateListAction<S, F, T> {
    public AbstractStateImportAction(String display) {
        super(display);
    }
}
