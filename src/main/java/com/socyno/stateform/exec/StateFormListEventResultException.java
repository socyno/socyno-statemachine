package com.socyno.stateform.exec;

import com.socyno.base.bscexec.MessageException;
import com.socyno.base.bscmixutil.CommonUtil;

public class StateFormListEventResultException extends MessageException {
    private static final long serialVersionUID = 1L;

    private final String form;
    private final String event;
    
    public StateFormListEventResultException(String form, String event) {
        this.form = form;
        this.event = event;
    }
    
    @Override
    public String getMessage() {
        return String.format("表单(%s)的创建操作(%s)结果不是有效的表单ID(整形数字)。", 
                CommonUtil.ifNull(form, ""), CommonUtil.ifNull(event, ""));
    }
}
