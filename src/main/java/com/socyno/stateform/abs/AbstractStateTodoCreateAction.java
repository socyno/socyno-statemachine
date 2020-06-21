package com.socyno.stateform.abs;

import com.socyno.base.bscmixutil.StringUtils;
import com.socyno.base.bscmodel.ObjectMap;
import com.socyno.base.bscmodel.SessionContext;
import com.socyno.base.bscmodel.SimpleLog;
import com.socyno.base.bsctmplutil.EnjoyUtil;
import com.socyno.stateform.exec.StateFormNoTodoAssigneeFoundException;
import com.socyno.webbsc.ctxutil.ContextUtil;
import com.socyno.webbsc.exception.AbstractMethodUnimplimentedException;

/**
 * 代办事项创建事件.作为内部事件的一种, Enter事件的子类,为代办事项专用
 */
public abstract class AbstractStateTodoCreateAction<S extends AbstractStateForm>
        extends AbstractStateEnterAction<S> {
    /**
     * 构造器
     * 
     * @param display 事件的显示名称
     * @param targetState 触发创建代办事项的状态
     */
    public AbstractStateTodoCreateAction(String display, String targetState) {
        super(display, targetState);
    }
    
    protected String getTodoCategory(String event, S originForm, AbstractStateForm form) {
        return String.format("system.state.form:normal?formName=%s", getContextFormService().getFormName());
    }
    
    /**
     * 构建代办事项的标题，如果未提供(返回空白)，则不创建此代办事项。
     */
    public final String getTodoTargetKey(String event, S originForm, AbstractStateForm form) {
        return String.format("system.state.form:%s::%s", getContextFormService().getFormName(), event);
    }
    
    /**
     * 获取代办事项的发起人. 默认通过查询首条操作日志的创建者,或者是当前操作者, 如此无法满足需求,请自行重写该方法.
     */
    protected long getTodoApplier(String event, S originForm, AbstractStateForm form) throws Exception {
        AbstractStateFormService<S> formService;
        if (!((formService = getContextFormService()) instanceof AbstractStateFormServiceWithBaseDao)) {
            throw new AbstractMethodUnimplimentedException();
        }
        
        SimpleLog firstLog;
        if ((firstLog = ((AbstractStateFormServiceWithBaseDao<?>) formService)
                .queryFirstLog(form.getId())) == null) {
            return SessionContext.getTokenUserId();
        }
        return firstLog.getOperateUserId();
    }
    
    protected abstract String getTodoTitle(String event, S originForm, AbstractStateForm form) throws Exception;
    
    protected abstract long[] getTodoAssignees(String event, S originForm, AbstractStateForm form) throws Exception;
    
    public final Void handle(String event, S originForm, AbstractStateForm form, String message) throws Exception {
        String todoTitle;
        /* 当代办事项的标题为空白,视为不创建该代办事项 */
        if (StringUtils.isBlank(todoTitle = getTodoTitle(event, originForm, form))) {
            return null;
        }
        long[] todoAssignees;
        String fromName = getContextFormService().getFormName();
        if ((todoAssignees = getTodoAssignees(event, originForm, form)) == null || todoAssignees.length <= 0) {
            throw new StateFormNoTodoAssigneeFoundException(fromName, form.getId(), event);
        }
        long todoApplier = getTodoApplier(event, originForm, form);
        String todoCategory = getTodoCategory(event, originForm, form);
        String todoTypeKey = getTodoTargetKey(event, originForm, form);
        String todoTargetPage = EnjoyUtil.format(ContextUtil.getConfigTrimed("system.todo.target.page.form"),
                new ObjectMap().put("formName", fromName).put("formId", form.getId()).asMap());
        getContextFormService().getTodoService().createTodo(todoTypeKey, form.getId(), todoApplier, todoTargetPage,
                todoTitle, todoCategory, todoAssignees);
        return null;
    }
}
