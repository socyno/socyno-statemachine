package com.socyno.stateform.abs;

public interface AbstractStateFormTotalFilter<F extends AbstractStateForm> extends AbstractStateFormFilter<F>{
    public int getTotal() throws Exception ;
}
