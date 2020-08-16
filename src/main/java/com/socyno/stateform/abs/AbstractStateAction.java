package com.socyno.stateform.abs;

import com.socyno.base.bscmixutil.ClassUtil;
import com.socyno.base.bscmixutil.ClassUtil.FieldAttribute;
import com.socyno.stateform.service.StateFormService;
import com.socyno.stateform.util.StateFormSimpleChoice;
import com.socyno.webbsc.authority.Authority;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Slf4j
@ToString
public abstract class AbstractStateAction<S extends AbstractStateForm, F extends AbstractStateForm, T> {
    
    @Getter
    @Setter
    @Accessors(chain = true)
    private static class EventContext {
        
        private String event;
        
        private String message;

        private NameValuePair[] params;
        
        private AbstractStateFormService<?> service;
        
        EventContext(AbstractStateFormService<?> service, String event, String message, NameValuePair ...params) {
            this.event = event;
            this.message = message;
            this.service = service;
            this.params = params;
        }
    }
    
    private final static ThreadLocal<EventContext> CONTEXT_FORM_SERVICE = new ThreadLocal<>();
    
    public static enum EventFormType {
        NULL, CREATE, VIEW, EDIT, DELETE;
    }
    
    private static final String[] InternalFields = new String[] {"id", "state", "revision"};
    
    @Getter(AccessLevel.NONE)
    private final String display;
    
    @Getter(AccessLevel.NONE)
    private final String[] sourceStates;
    
    private final AbstractStateChoice targetState;
    
    private void setEventContext(AbstractStateFormService<S> service, String event, String message,
            NameValuePair... params) {
        if (CONTEXT_FORM_SERVICE.get() == null) {
            CONTEXT_FORM_SERVICE.set(new EventContext(service, event, message, params));
            return;
        }
        CONTEXT_FORM_SERVICE.get().setService(service).setEvent(event).setMessage(message).setParams(params);
    }
    
    private void clearEventContext() {
        setEventContext(null, null, null);
    }
    
    protected String getContextFormEvent() {
        return CONTEXT_FORM_SERVICE.get().getEvent();
    }
    
    protected String getContextFormEventMessage() {
        return CONTEXT_FORM_SERVICE.get().getMessage();
    }
    
    protected NameValuePair[] getContextFormEventParams() {
        return CONTEXT_FORM_SERVICE.get().getParams();
    }
    
    @SuppressWarnings("unchecked")
    protected AbstractStateFormService<S> getContextFormService() {
        return (AbstractStateFormService<S>) CONTEXT_FORM_SERVICE.get().getService();
    }
    
    public final String getDisplay() {
        String stateDisplay;
        if (StringUtils
                .isNotBlank(stateDisplay = StateFormService.getCustomizedDisplayText(this.getClass().getName()))) {
            return stateDisplay;
        }
        return this.display;
    }
    
    public AbstractStateAction(String display, String sourceState, String targetState) {
        this(display, new String[] {sourceState}, targetState);
    }
    
    public AbstractStateAction(String display, String[] sourceStates, String targetState) {
        this(display, sourceStates, StateFormSimpleChoice.getInstance(targetState));
    }
    
    public AbstractStateAction(String display, String sourceState, AbstractStateChoice targetState) {
        this(display, new String[] {sourceState}, targetState);
    }
    
    public AbstractStateAction(String display, String[] sourceStates, AbstractStateChoice targetState) {
        this.display = display;
        this.sourceStates = ArrayUtils.clone(sourceStates);
        this.targetState = targetState;
    }
    
    public boolean isListEvent() {
        return isCreateEvent() || (this instanceof AbstractStateListAction);
    }
    
    public boolean isCreateEvent() {
        return (this instanceof AbstractStateCreateAction);
    }
    
    public boolean isDeleteEvent() {
        return (this instanceof AbstractStateDeleteAction);
    }
    
    public boolean isCommentEvent() {
        return (this instanceof AbstractStateCommentAction);
    }
    
    public static String[] getInternalFields() {
        return ArrayUtils.clone(InternalFields);
    }
    
    /**
     * 是否为异步事件
     * @return
     */
    public boolean isAsyncEvent() {
        return AbstractStateAsyncEeventView.class.isAssignableFrom(getReturnTypeClass());
    }
    
    /**
     * 是否为动态表单事件
     * @return
     */
    public boolean isDynamicEvent() {
        return DynamicStateForm.class.isAssignableFrom(getFormTypeClass());
    }
    
    /**
     * 获取允许的操作状态列表。
     * 
     * @return
     */
    public final String[] getSourceStates() {
        Set<String> sources = new HashSet<>();
        if (sourceStates != null && sourceStates.length > 0) {
            for(String s : sourceStates) {
                if (StringUtils.isNotBlank(s)) {
                    sources.add(s);
                }
            }
        }
        return sources.toArray(new String[0]);
    }
    
    /**
     * 确认表单事件, 绝对无目标状态的变化
     */
    public boolean ensureNoStateChange() {
        AbstractStateChoice target;
        if ((target = getTargetState()) == null ||
                (target.isSimple() && StringUtils.isBlank(target.getTargetState()))) {
            return true;
        }
        return false;
    }
    
    /**
     * 确认允许的操作状态定义中是否包含指定的状态。
     * 
     * @return
     */
    public final boolean sourceContains(String source) {
        String[] sources = null;
        if (StringUtils.isBlank(source) || (sources = getSourceStates()).length <= 0) {
            return false;
        }
        boolean found = false;
        for (String s : sources) {
            if (source.equals(s)) {
                found = true;
                break;
            }
        }
        log.info("Source state {} included by {} : {}", source, sources, found);
        return found;
    }
    
    /**
     * 获取输入表单的可编辑字段
     */
    public List<FieldAttribute> getEditableFields() throws Exception {
        Collection<FieldAttribute> fields;
        if ((fields = ClassUtil.parseClassFields(getFormTypeClass())) == null) {
            return null;
        }
        List<FieldAttribute> editFields = new ArrayList<>();
        for (FieldAttribute field :fields) {
            if (ArrayUtils.contains(InternalFields, field.getField()) 
                    || !(field.isRequired() || field.isEditable())) {
                continue;
            }
            editFields.add(field);
        }
        return editFields;
    }
    
    /**
     * 获取输入表单的字段信息
     */
    public Collection<FieldAttribute> getFormFields() throws Exception {
        return ClassUtil.parseClassFields(getFormTypeClass());
    }
    
    /**
     * 是否要求操作上下文的说明
     * 
     * @return ture - 必填, false - 选填， null - 不填
     */
    public Boolean messageRequired() {
        return false;
    }
    
    /**
     * 是否操作执行前是否要求执行确认。
     * 
     * @return ture - 要求, false - 不要求
     */
    public boolean confirmRequired() {
        return true;
    }
    
    /**
     * 是否执行请求数据准备类型定义。如果在操作界面展示前，需要准备表单详情外的特殊数据时，
     * 可重写该方法返回 true， 并重事件的 prepare 方法生成前端所需数据即可。
     */
    public boolean prepareRequired() {
        return false;
    }
    
    /**
     * 事件界面的展示形式, 分为:<pre>
     * 
     * 不显示界面 ：当无可编辑字段，也无需操作备注的情况下，默认不显示界面；
     * 
     * 可编辑界面 ：有可编辑字段，或需要填写操作说明的情况下，默认为可编辑界面；
     * 
     * 仅展示界面 ：仅作内容显示，不执行任何操作（在部分特定场景使用，按需配置即可）；
     * 
     * </pre>
     */
    public EventFormType getEventFormType() throws Exception {
        List<FieldAttribute> fields;
        if (((fields = getEditableFields()) == null || fields.isEmpty())
                    && messageRequired() == null) {
            return EventFormType.NULL;
        }
        return EventFormType.EDIT;
    }
    
    /**
     * 确认操作是否可执行. 必须在实现中通过 Authority 注解来配置.
     * 
     */
    public abstract void check(String event, S originForm, String sourceState);
    
    /**
     * 获取操作准备数据
     * 
     * @return
     */
    public final AbstractStatePrepare prepareForm(AbstractStateFormService<S> service, String event, S originForm, NameValuePair... params) throws Exception {
        if (!prepareRequired()) {
            return null;
        }
        setEventContext(service, event, null);
        try {
            return prepare(event, originForm);
        } finally {
            clearEventContext();
        }
    }
    
    /**
     * 准备数据
     * 
     * @return
     */
    public AbstractStatePrepare prepare(String event, S originForm) throws Exception {
        return null;
    }
    
    /**
     * 操作实现
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public final T handleForm(AbstractStateFormService<S> service, String contextEvent, String event, S originForm, AbstractStateForm form, String message) throws Exception {
        setEventContext(service, contextEvent, message);
        try {
            return handle(event, originForm, (F)form, message);
        } finally {
            clearEventContext();
        }
    }
    
    /**
     * 操作实现
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public final T handleForm(AbstractStateFormService<S> service, String event, S originForm, AbstractStateForm form, String message) throws Exception {
        return handleForm(service, event, event, originForm, (F)form, message);
    }
    
    /**
     * 操作实现
     * 
     * @return
     */
    public T handle(String event, S originForm, F form, String message) throws Exception {
        return (T)null;
    }
    
    /**
     * 操作完成的后期处理，可用于操作通知。
     * 
     * 注意：调用失败，将不终止该操作，而是继续执行。
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public final void postForm(AbstractStateFormService<S> service, String event, S originForm, AbstractStateForm form, String message) {
        setEventContext(service, event, message);
        try {
            post(event, originForm, (F)form, message);
        } finally {
            clearEventContext();
        }
    }
    
    /**
     * 操作完成的后期处理，可用于操作通知。
     * 
     * 注意：调用失败，将不终止该操作，而是继续执行。
     * 
     * @return
     */
    public void post(String event, S originForm, F form, String message) {
        
    }
    
    /**
     * 获取动态表单操作事件输入表单定义
     */
    public String getDynamicFormDefinition(String event, S form) throws Exception {
        return ClassUtil.classToJson(BasicStateForm.class).toString();
    }
    
    /**
     * 是否忽略流程表单的版本自动升级。
     * 默认根据事件是否导致状态变化，或是否有编辑字段定义的情况自动判定。
     * 如存在特定场景，确认无需或必须进行流程表单的版本升级时，可重写该方法。
     * @return
     */
    public boolean getStateRevisionChangeIgnored() throws Exception {
        List<FieldAttribute> fields;
        return (fields = getEditableFields()) == null || fields.isEmpty();
    }
    
    /**
     * 获取当前详情表单类型
     */
    @SuppressWarnings("unchecked")
    public Class<AbstractStateForm> getOriginTypeClass() {
        return (Class<AbstractStateForm>)ClassUtil.getActualParameterizedType(this.getClass(), AbstractStateAction.class, 0);
    }

    /**
     * 获取事件编辑表单类型
     */
    @SuppressWarnings("unchecked")
    public Class<AbstractStateForm> getFormTypeClass() {
        return (Class<AbstractStateForm>)ClassUtil.getActualParameterizedType(this.getClass(), AbstractStateAction.class, 1);
    }
    
    /**
     * 获取事件响应数据类型
     */
    public Class<?> getReturnTypeClass() {
        return (Class<?>)ClassUtil.getActualParameterizedType(this.getClass(), AbstractStateAction.class, 2);
    }
    
    /**
     * 获取事件授权配置信息
     */
    public Authority getAuthority () throws Exception {
        Method method = this.getClass().getMethod("check", String.class,
                    this.getOriginTypeClass(), String.class);
        return method.getAnnotation(Authority.class);
    }
    
    /**
     * 获取事件目标状态显示
     */
    public String getTargetStateForDisplay () throws Exception {
        String state = "";
        AbstractStateChoice target;
        if ((target = getTargetState()) != null) {
            if (target.isSimple()) {
                state = target.getTargetState();
            } else {
                state = String.format("selector:%s", target.getDisplay());
            }
        }
        return state;
    }
    
    /**
     * 标识当前事件是否与给定流程单相关
     * @param originForm
     * @return
     */
    public boolean flowMatched(S originForm) {
        return true;
    }
    
    /**
     * 是否允许 Hanlde 返回 NULL 值
     */
    public boolean allowHandleReturnNull() {
        return true;
    }
}
