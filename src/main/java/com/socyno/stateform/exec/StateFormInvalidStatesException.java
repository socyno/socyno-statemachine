package com.socyno.stateform.exec;

import com.socyno.base.bscexec.MessageException;
import com.socyno.base.bscmixutil.CommonUtil;

import lombok.Getter;

@Getter
public class StateFormInvalidStatesException extends MessageException {
    
    private static final long serialVersionUID = 1L;
    
    private String form;
    private String state;
    
    public StateFormInvalidStatesException(String form, String state) {
        this.form = form;
        this.state = state;
    }
    
    @Override
    public String getMessage() {
        return String.format("流程单（%s）为定义合适状态信息，或者给定的状态（%s）不在定义范围内", CommonUtil.ifNull(form, ""),
                CommonUtil.ifNull(state, ""));
    }
}
