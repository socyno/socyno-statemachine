package com.socyno.stateform.abs;

import com.socyno.base.bscmodel.ObjectMap;
import com.socyno.base.bscservice.HttpUtil;

/**
 * 审批类代办事项创建事件,区别于普通的表单代办事项,可接入外部标准化审批
 */
public abstract class AbstractStateTodoApprovalAction<S extends AbstractStateForm>
        extends AbstractStateTodoCreateAction<S> {
    /**
     * 构造器
     * 
     * @param display 事件的显示名称
     * @param targetState 触发创建代办事项的状态
     */
    public AbstractStateTodoApprovalAction(String display, String targetState) {
        super(display, targetState);
    }
    
    protected final String getTodoCategory(String event, S originForm, AbstractStateForm form) {
        return String.format("system.state.form:approval?%s",
                HttpUtil.toQueryString(new ObjectMap()
                    .put("formName", getContextFormService().getFormName())
                    .put("rejectEvent", getNextRejectEvent(event, originForm, form))
                    .put("approveEvent", getNextApproveEvent(event, originForm, form))
                    .asMap()
                ));
    }
    
    protected abstract String getNextRejectEvent(String event, S originForm, AbstractStateForm form);
    
    protected abstract String getNextApproveEvent(String event, S originForm, AbstractStateForm form);
}
