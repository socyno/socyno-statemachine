package com.socyno.stateform.abs;

public interface AbstractStateAsyncEeventView {
    public boolean waitingForFinished(String event, String message, AbstractStateForm originForm, AbstractStateForm form)
                throws Exception;
}
