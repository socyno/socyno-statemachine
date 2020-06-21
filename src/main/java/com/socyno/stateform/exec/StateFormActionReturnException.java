package com.socyno.stateform.exec;

import com.socyno.base.bscexec.MessageException;
import com.socyno.base.bscmixutil.CommonUtil;

public class StateFormActionReturnException extends MessageException {
    private static final long serialVersionUID = 1L;

    private final String form;
    private final String event;
    
    public StateFormActionReturnException(String form, String event) {
        this.form = form;
        this.event = event;
    }
    
    @Override
    public String getMessage() {
        return String.format("表单操作被拒绝（form=%s, event=%s），返回类型与定义不匹配。", 
                CommonUtil.ifNull(form, ""), CommonUtil.ifNull(event, ""));
    }
}
