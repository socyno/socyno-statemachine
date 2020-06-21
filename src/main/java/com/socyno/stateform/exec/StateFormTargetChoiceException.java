package com.socyno.stateform.exec;

import com.socyno.base.bscexec.MessageException;

public class StateFormTargetChoiceException extends MessageException {

    private static final long serialVersionUID = 1L;
 
    public StateFormTargetChoiceException() {
        super("无效的表单操作目标状态值。");
    }
    
    @Override
    public String getMessage() {
        return "表单操作异常，请联系系统管理员。";
    }
}
