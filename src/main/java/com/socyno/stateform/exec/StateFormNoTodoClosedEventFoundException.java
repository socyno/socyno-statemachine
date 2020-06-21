package com.socyno.stateform.exec;

import com.socyno.base.bscexec.MessageException;
import com.socyno.base.bscmixutil.CommonUtil;

public class StateFormNoTodoClosedEventFoundException extends MessageException {
    
    private static final long serialVersionUID = 1L;
    
    private Long formId;
    private String formName;
    private String formEvent;
    
    public StateFormNoTodoClosedEventFoundException(String name, Long formId, String event) {
        this.formName = name;
        this.formId = formId;
        this.formEvent = event;
    }
    
    @Override
    public String getMessage() {
        return String.format("未提供要关闭的待办事项创建事件, 无法关闭通用表单（formName=%s, formId=%s, formEvent=%s)待办事项",
                CommonUtil.ifNull(formName, ""), CommonUtil.ifNull(formId, ""), CommonUtil.ifNull(formEvent, ""));
    }
}
