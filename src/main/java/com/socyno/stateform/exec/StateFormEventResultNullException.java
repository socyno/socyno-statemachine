package com.socyno.stateform.exec;

import com.socyno.base.bscexec.MessageException;
import com.socyno.base.bscmixutil.CommonUtil;

public class StateFormEventResultNullException extends MessageException {
    private static final long serialVersionUID = 1L;

    private final String form;
    private final String event;
    
    public StateFormEventResultNullException(String form, String event) {
        this.form = form;
        this.event = event;
    }
    
    @Override
    public String getMessage() {
        return String.format("表单(%s)的事件(%s)响应数据不可为NULL值", 
                CommonUtil.ifNull(form, ""), CommonUtil.ifNull(event, ""));
    }
}
