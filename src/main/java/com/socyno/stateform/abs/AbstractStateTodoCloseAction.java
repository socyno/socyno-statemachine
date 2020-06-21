package com.socyno.stateform.abs;

import com.socyno.base.bscmixutil.StringUtils;
import com.socyno.stateform.exec.StateFormNoTodoClosedEventFoundException;

/**
 * 代办事项关闭事件.作为内部事件的一种, Leave 事件的子类, 为代办事项专用
 */
public abstract class AbstractStateTodoCloseAction<S extends AbstractStateForm>
        extends AbstractStateLeaveAction<S> {
    /**
     * 构造器
     * 
     * @param display 事件的显示名称
     * @param targetState 触发关闭代办事项的状态
     */
    public AbstractStateTodoCloseAction(String display, String targetState) {
        super(display, targetState);
    }
    
    /**
     * 构建代办事项关闭的原因说明，如果未提供(返回空白)，则不关闭此代办事项。
     */
    protected String getClosedTodoReason(String event, S originForm, AbstractStateForm form) throws Exception {
        StringBuilder reason = new StringBuilder(
                getContextFormService().getExternalFormAction(getContextFormEvent()).getDisplay());
        if (StringUtils.isNotBlank(getContextFormEventMessage())) {
            reason.append(":").append(getContextFormEventMessage());
        }
        return reason.toString();
    }
    
    protected abstract String getClosedTodoEvent(String event, S originForm, AbstractStateForm form) throws Exception;
    
    @SuppressWarnings("unchecked")
    protected final String getClosedTodoTypeKey(String event, S originForm, AbstractStateForm form) throws Exception {
        String createdEvent = getClosedTodoEvent(event, originForm, form);
        AbstractStateAction<S, ?, ?> created = getContextFormService().getInternalFormAction(createdEvent);
        if (created == null || !(created instanceof AbstractStateTodoCreateAction)) {
            throw new StateFormNoTodoClosedEventFoundException(getContextFormService().getFormName(), form.getId(), event);
        }
        return ((AbstractStateTodoCreateAction<S>)created).getTodoTargetKey(createdEvent, originForm, form);
    }
    
    public final Void handle(String event, S originForm, AbstractStateForm form, String message) throws Exception {
        String closeTodoReason = getClosedTodoReason(event, originForm, form);
        if (StringUtils.isBlank(closeTodoReason = getClosedTodoReason(event, originForm, form))) {
            return null;
        }
        String closeTodoTypeKey = getClosedTodoTypeKey(event, originForm, form);
        getContextFormService().getTodoService().closeTodo(closeTodoTypeKey, form.getId(), closeTodoReason);
        return null;
    }
}
