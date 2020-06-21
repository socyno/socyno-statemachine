package com.socyno.stateform.exec;

import com.socyno.base.bscexec.MessageException;
import com.socyno.base.bscmixutil.CommonUtil;

import lombok.Getter;

@Getter
public class StateFormNotDefinedException extends MessageException {
    
    private static final long serialVersionUID = 1L;
    
    private String formName;
    
    public StateFormNotDefinedException(String name) {
        this.formName = name;
    }
    
    @Override
    public String getMessage() {
        return String.format("流程表单（%s）未定义.", CommonUtil.ifNull(formName, ""));
    }
}
