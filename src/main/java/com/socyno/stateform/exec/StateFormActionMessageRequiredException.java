package com.socyno.stateform.exec;

import com.socyno.base.bscexec.MessageException;
import com.socyno.base.bscmixutil.CommonUtil;

public class StateFormActionMessageRequiredException extends MessageException {
    private static final long serialVersionUID = 1L;

    private final String form;
    private final String event;
    
    public StateFormActionMessageRequiredException(String form, String event) {
        this.form = form;
        this.event = event;
    }
    
    @Override
    public String getMessage() {
        return String.format("表单操作被拒绝（form=%s, event=%s），操作说明要求必须填写", 
                CommonUtil.ifNull(form, ""), CommonUtil.ifNull(event, ""));
    }
}
