package com.socyno.stateform.abs;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.NameValuePair;

import java.util.Set;

import com.github.reinert.jjschema.v1.FieldOption;
import com.socyno.base.bscexec.MessageException;
import com.socyno.base.bscmixutil.ArrayUtils;
import com.socyno.base.bscmixutil.ClassUtil;
import com.socyno.base.bscmixutil.StringUtils;
import com.socyno.base.bscmodel.ObjectMap;
import com.socyno.base.bscmodel.RunableWithSessionContext;
import com.socyno.base.bscmodel.SimpleLog;
import com.socyno.base.bscservice.AbstractLockService;
import com.socyno.base.bscservice.AbstractLogService;
import com.socyno.base.bscservice.AbstractNotifyService;
import com.socyno.base.bscservice.AbstractTodoService;
import com.socyno.base.bscservice.AbstractLockService.CommonLockExecutor;
import com.socyno.stateform.exec.StateFormActionDeclinedException;
import com.socyno.stateform.exec.StateFormActionMessageRequiredException;
import com.socyno.stateform.exec.StateFormActionNotFoundException;
import com.socyno.stateform.exec.StateFormActionReturnException;
import com.socyno.stateform.exec.StateFormEventResultNullException;
import com.socyno.stateform.exec.StateFormNotFoundException;
import com.socyno.stateform.exec.StateFormRevisionChangedException;
import com.socyno.stateform.exec.StateFormRevisionNotFoundException;
import com.socyno.stateform.model.StateFlowNodeData;
import com.socyno.stateform.service.PermissionService;
import com.socyno.stateform.exec.StateFormListEventDefinedException;
import com.socyno.stateform.exec.StateFormListEventResultException;
import com.socyno.stateform.exec.StateFormListEventTargetException;
import com.socyno.stateform.exec.StateFormNamedQueryNotFoundException;
import com.socyno.stateform.util.StateFormActionDefinition;
import com.socyno.stateform.util.StateFormEventClassEnum;
import com.socyno.stateform.util.StateFormNamedQuery;
import com.socyno.stateform.util.StateFormQueryBaseEnum;
import com.socyno.stateform.util.StateFormQueryDefinition;
import com.socyno.stateform.util.StateFormRevision;
import com.socyno.stateform.util.StateFormStateBaseEnum;
import com.socyno.stateform.util.StateFormWithAction;
import com.socyno.webbsc.authority.Authority;
import com.socyno.webbsc.authority.AuthorityScopeIdNoopMultipleCleaner;
import com.socyno.webbsc.authority.AuthorityScopeIdNoopMultipleParser;
import com.socyno.webbsc.authority.AuthorityScopeIdNoopParser;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractStateFormService<S extends AbstractStateForm> {
    
    private final Map<String, StateFormStateBaseEnum> states = new HashMap<>();
    
    private final Map<String, StateFormNamedQuery<? extends S>> queries = new HashMap<>();
    
    private final Map<String, AbstractStateAction<S, ?, ?>> actions = new HashMap<>();
    
    public abstract String getFormName();
    
    protected abstract void saveStateRevision(long formId, String state) throws Exception;
    
    protected abstract Map<Long, StateFormRevision> loadStateRevision(Long[] formIds) throws Exception;
    
    protected abstract <T extends S> T loadFormNoStateRevision(Class<T> clazz, long formId) throws Exception;
    
    protected abstract AbstractLogService getLogService();
    
    protected abstract AbstractLockService getLockService();
    
    protected abstract AbstractTodoService getTodoService();
    
    public abstract PermissionService getPermissionService();
    
    protected abstract AbstractNotifyService<?> getNotifyService();
    
    @SuppressWarnings("unchecked")
    public Class<S> getFormClass() {
        return (Class<S>)ClassUtil.getActualParameterizedType(
                             getClass(), AbstractStateFormService.class, 0);
    }
    
    @SuppressWarnings("unchecked")
    public Class<? extends S> getSingleFormClass() throws Exception {
        Class<? extends S> singleFormClass;
        Class<S> basicFormClass = getFormClass();
        if (basicFormClass.isAssignableFrom(singleFormClass = (Class<? extends S>) getClass()
                    .getMethod("getForm", long.class).getReturnType()) ) {
            return singleFormClass;
        }
        return basicFormClass;
    }
    
    /**
     * 注册查询。当查询的名称（name）冲突时，将覆盖之前的数据。
     */
    @SuppressWarnings("unchecked")
    protected void setQueries(StateFormQueryBaseEnum... queries) {
        if (queries == null || queries.length <= 0) {
            return;
        }
        for (StateFormQueryBaseEnum q : queries) {
            if (q == null) {
                continue;
            }
            if (q.getNamedQuery() == null || !getFormClass().isAssignableFrom(q.getNamedQuery().getResultClass())) {
                throw new MessageException(String.format("Named query result class must be extended from class %s .",
                        getFormClass().getName()));
            }
            setQuery(q.name(), (StateFormNamedQuery<? extends S>) q.getNamedQuery());
        }
    }
    
    private void setQuery(String name, StateFormNamedQuery<? extends S> query) {
        if (query == null) {
            queries.remove(name);
            return;
        }
        queries.put(name, query);
    }
    
    /**
     * 注册状态。当态的名称（name）冲突时，将覆盖之前的数据。
     */
    protected void setStates(StateFormStateBaseEnum ...states) {
        if (states ==  null || states.length <= 0) {
            return;
        }
        for (StateFormStateBaseEnum s : states) {
            setState(s);
        }
    }
    
    private void setState(StateFormStateBaseEnum state) {
        if (state == null) {
            return;
        }
        states.put(state.getCode(), state);
    }
    
    /**
     * 注册事件。当态的事件（name）冲突时，将覆盖之前的数据。
     */
    @SuppressWarnings("unchecked")
    protected void setActions(StateFormEventClassEnum ...events) {
        if (events == null || events.length <= 0) {
            return;
        }
        for (StateFormEventClassEnum e : events) {
            if (e == null) {
                continue;
            }
            if (e.getEventClass() == null) {
                setAction(e.getName(), null);
            } else {
                try {
                    setAction(e.getName(), (AbstractStateAction<S, ?, ?>) createInstance(e.getEventClass()));
                } catch (RuntimeException x) {
                    throw (RuntimeException) x;
                } catch (Exception x) {
                    throw new RuntimeException(x);
                }
            }
        }
    }
    
    private void setAction(String event, AbstractStateAction<S, ?, ?> action) {
        if (action == null) {
            actions.remove(event);
            return;
        }
        actions.put(event, action);
    }
    
    /**
     * 获取状态可选项清单
     */
    public List<? extends FieldOption> getStates() {
        return new ArrayList<>(states.values());
    }
    
    /**
     * 获取给定状态枚举对应的状态码清单
     */
    protected String[] getStateCodes(StateFormStateBaseEnum... states) {
        if (states == null || states.length <= 0) {
            return new String[0];
        }
        List<String> result = new ArrayList<>(states.length);
        for (StateFormStateBaseEnum s : states) {
            if (s == null) {
                continue;
            }
            result.add(s.getCode());
        }
        return result.toArray(new String[0]);
    }
    
    /**
     * 获取除给定状态枚举以外的状态码清单
     */
    protected String[] getStateCodesEx(StateFormStateBaseEnum... exclusions) {
        if (exclusions == null) {
            exclusions = new StateFormStateBaseEnum[0];
        }
        List<String> excodes = new ArrayList<>();
        for (StateFormStateBaseEnum e : exclusions) {
            if (e == null) {
                continue;
            }
            excodes.add(e.getCode());
        }
        List<String> result = new ArrayList<>(states.size());
        for (String s : states.keySet()) {
            if (s == null || excodes.contains(s)) {
                continue;
            }
            result.add(s);
        }
        return result.toArray(new String[0]);
    }
    
    /**
     * 获取定义的事件清单
     */
    protected Map<String, AbstractStateAction<S, ?, ?>> getFormActions() {
        return Collections.unmodifiableMap(actions);
    }
    
    /**
     * 获取定义的查询清单
     */
    public Map<String, StateFormNamedQuery<? extends S>> getFormQueries() {
        return Collections.unmodifiableMap(queries);
    }
    
    protected <T> T createInstance(@NonNull Class<T> clazz) throws InvocationTargetException, IllegalAccessException,
                                    InstantiationException, IllegalArgumentException, NoSuchMethodException {
        Constructor<T> eventConstructor;
        try {
            eventConstructor = clazz.getDeclaredConstructor(this.getClass());
            return eventConstructor.newInstance(this);
        } catch (NoSuchMethodException x) {
            eventConstructor = clazz.getDeclaredConstructor();
                return eventConstructor.newInstance();
        }
    }
    
    /**
     * 获取完成的表单数据，包括状态、版本及创建信息
     * 
     */
    public S getForm(long formId) throws Exception {
        return getForm(getFormClass(), formId);
    }
    
    public <T extends S> T getForm(Class<T> clazz, long formId) throws Exception {
       T form;
        if ((form = loadFormNoStateRevision(clazz, formId)) == null) {
            throw new StateFormNotFoundException(getFormName(), formId);
        }
        StateFormRevision s;
        if ((s = loadStateRevision(formId)) == null) {
            throw new StateFormRevisionNotFoundException(getFormName(), formId);
        }
        form.setState(s.getStateFormStatus());
        form.setRevision(s.getStateFormRevision());
        return form;
    }
    
    public S getForm(String namedQuery, long formId) throws Exception {
        StateFormNamedQuery<? extends S> formQuery;
        if ((formQuery = getFormNamedQuery(namedQuery)) == null) {
            throw new StateFormNamedQueryNotFoundException(getFormName(), namedQuery);
        }
        return getForm(formQuery.getResultClass(), formId);
    }
    
    /**
     * 获取表单及其结构类名称
     * 
     */
    public StateFormWithAction<S> getFormNoActions(long formId) throws Exception {
        return getFormWithActions(getForm(formId), null);
    }
    
    /**
     * 获取表单的可执行操作
     * 
     */
    public StateFormWithAction<S> getFormActions(long formId) throws Exception {
        return getFormWithActions(getForm(formId), getFormActions(true, null), true);
    }
    
    /**
     * 获取表单的可执行操作
     * 
     */
    public Map<String, String> getFormActionNames(long formId) throws Exception {
        final Map<String, String> formActions = new HashMap<>();
        checkFormWithActions(getForm(formId), getFormActions(true, null), new FormActionsProcessor() {
            @Override
            public void enabled(S form, String action, AbstractStateAction<S, ?, ?> definition) throws Exception {
                formActions.put(action, definition.getDisplay());
            }
        });
        return formActions;
    }
    
    /**
     * 获取表单的查询定义
     * 
     */
    public final List<StateFormQueryDefinition> getFormQueryDefinition() throws Exception {
        Map<String, StateFormNamedQuery<? extends S>> queries;
        List<StateFormQueryDefinition> definitions = new ArrayList<>();
        if ((queries = getFormQueries()) != null) {
            for (Map.Entry<String, StateFormNamedQuery<? extends S>> query : queries.entrySet()) {
                if (query == null) {
                    continue;
                }
                definitions
                        .add(StateFormQueryDefinition.fromStateQuery(getFormName(), query.getKey(), query.getValue()));
            }
        }
        return definitions;
    }
    
    /**
     * 组装表单实体以及可执行操作
     * 
     */
    public StateFormWithAction<S> getFormWithActions(long formId) throws Exception {
        return getFormWithActions(getForm(formId), getFormActions(true, null));
    }
    
    /**
     * 组装表单及其对应的可执行操作
     * 
     */
    protected StateFormWithAction<S> getFormWithActions(S form, Map<String, AbstractStateAction<S, ?, ?>> actions)
                throws Exception {
        return getFormWithActions(form, actions, false);
    }
    
    protected abstract class FormActionsProcessor {
        
        public abstract void enabled (S form, String action, AbstractStateAction<S, ?, ?> definition) throws Exception;
        
        public void disabled (S form, String action, AbstractStateAction<S, ?, ?> definition) throws Exception {
            
        }
    }

    /**
     * 获取状态的显示名称
     */
    public String getStateDisplay(String state) {
        for (FieldOption option : getStates()) {
            if (StringUtils.equals(option.getOptionValue(), state)) {
                return option.getOptionDisplay();
            }
        }
        return null;
    }
    
    
    protected void checkFormWithActions(S form, Map<String, AbstractStateAction<S, ?, ?>> actions,
            @NonNull FormActionsProcessor processor) throws Exception {
        if (form == null) {
            return;
        }
        if (actions != null && actions.size() > 0) {
            for (Entry<String, AbstractStateAction<S, ?, ?>> a : actions.entrySet()) {
                String actionName = a.getKey();
                AbstractStateAction<S, ?, ?> actionDef = a.getValue();
                if (actionDef.sourceContains(form.getState()) && checkFromAction(actionName, form)) {
                    processor.enabled(form, actionName, actionDef);
                } else {
                    processor.disabled(form, actionName, actionDef);
                }
            }
        }
    }
    
    protected StateFormWithAction<S> getFormWithActions(final S form, Map<String, AbstractStateAction<S, ?, ?>> actions,
                        boolean actionsOnly) throws Exception {
        final List<StateFormActionDefinition> formActions = new ArrayList<>();
        checkFormWithActions(form, actions, new FormActionsProcessor() {
            @Override
            public void enabled(S form, String action, AbstractStateAction<S, ?, ?> definition) throws Exception {
                StateFormActionDefinition fromAction = StateFormActionDefinition.fromStateAction(action, getFormName(), definition);
                if (definition.isDynamicEvent()) {
                    fromAction.setFormClass(definition.getDynamicFormDefinition(action, form));
                }
                formActions.add(fromAction);
            }
        });
        return new StateFormWithAction<S>(actionsOnly ? null : form, formActions);
    }
    
    /**
     * 筛选表单。在完成筛选后自动填充状态及版本等元数据。
     */
    public final List<S> listForm(@NonNull AbstractStateFormFilter<S> filter) throws Exception {
        return listForm(getFormClass(), filter);
    }
    
    /**
     * 筛选表单。在完成筛选后自动填充状态及版本等元数据。
     */
    public <T extends S> List<T> listForm(@NonNull Class<T> clazz, @NonNull AbstractStateFormFilter<T> filter) throws Exception {
        List<T> list = filter.apply(clazz);
        if (list != null && list.size() > 0) {
            Set<Long> formIds = new HashSet<>();
            for (S l : list) {
                formIds.add(l.getId());
            }
            Map<Long, StateFormRevision> srs = loadStateRevision(formIds.toArray(new Long[0]));
            for (S f : list) {
                StateFormRevision sr;
                if ((sr = srs.get(f.getId())) != null) {
                    f.setState(sr.getStateFormStatus());
                    f.setRevision(sr.getStateFormRevision());
                }
            }
        }
        return list;
    }
    

    /**
     * 获取指定的事件
     * 
     */
    protected AbstractStateAction<S, ?, ?> getFormAction(String name, boolean external) throws Exception {
        Map<String, AbstractStateAction<S, ?, ?>> actions;
        if (StringUtils.isBlank(name) || (actions = getFormActions(external, null)).isEmpty()) {
            return null;
        }
        return actions.get(name);
    }
    
    /**
     * 获取指定的外部事件
     * 
     */
    protected AbstractStateAction<S, ?, ?> getExternalFormAction(String name) throws Exception {
        return getFormAction(name, true);
    }
    
    /**
     * 获取指定的外部事件
     * 
     */
    protected AbstractStateAction<S, ?, ?> getExternalFormAction(@NonNull StateFormEventClassEnum event) throws Exception {
        return getExternalFormAction(event.getName());
    }
    
    /**
     * 获取指定的内部事件
     * 
     */
    protected AbstractStateAction<S, ?, ?> getInternalFormAction(String name) throws Exception {
        return getFormAction(name, false);
    }
    
    /**
     * 获取指定的内部事件
     * 
     */
    protected AbstractStateAction<S, ?, ?> getInternalFormAction(@NonNull StateFormEventClassEnum event) throws Exception {
        return getInternalFormAction(event.getName());
    }
    
    /**
     * 是否支持注释功能
     * 
     */
    public boolean supportCommentFormAction()  throws Exception {
        Map<String, AbstractStateAction<S, ?, ?>> actions;
        if ((actions = getFormActions(true, null)).isEmpty()) {
            return false;
        }
        for (AbstractStateAction<S, ?, ?> action : actions.values()) {
            if (action instanceof AbstractStateCommentAction) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 获取流程表单的所有外部事件定义
     * 
     */
    public final List<StateFormActionDefinition> getExternalFormActionDefinition()  throws Exception {
        String event;
        AbstractStateAction<S, ?, ?> action;
        List<StateFormActionDefinition> actions = new ArrayList<>();
        for (Map.Entry<String,AbstractStateAction<S,?,?>> e : getFormActions(true, null).entrySet()) {
            event = e.getKey();
            action = e.getValue();
            if (action == null) {
                continue;
            }
            actions.add(StateFormActionDefinition.fromStateAction(event, getFormName(), action));
        }
        return actions;
    }
    
    /**
     * 获取流程表单指定的外部事件定义
     */
    public final StateFormActionDefinition getExternalFormActionDefinition(String event)  throws Exception {
        AbstractStateAction<S, ?, ?> action;
        if ((action = getExternalFormAction(event)) == null) {
            return null;
        }
        return StateFormActionDefinition.fromStateAction(event, getFormName(), action);
    }
    
    /**
     * 解析状态机的事件定义，生成流程数据，用于绘制流程图
     */
    @SuppressWarnings("unchecked")
    public final Map<StateFlowNodeData, Set<StateFlowNodeData>> parseFormFlowChartDefinition(
            boolean keepUnChanged, AbstractStateForm form) throws Exception {
        /**
         * 加载当前的状态信息
         */
        StateFormRevision stateRevision = null;
        if (form != null) {
            stateRevision = new StateFormRevision().setId(form.getId())
                .setStateFormRevision(form.getRevision())
                .setStateFormStatus(form.getState());
        }
        /**
         * 将事件及状态的流转关系转化为基础流程数据
         */
        AbstractStateAction<S, ?, ?> action;
        Map<StateFlowNodeData, Set<StateFlowNodeData>> baseFlowData = new HashMap<>();
        for (Map.Entry<String,AbstractStateAction<S,?,?>> e : getFormActions(true, null).entrySet()) {
            action = e.getValue();
            if (action == null) {
                continue;
            }
            if (form != null && !action.flowMatched((S)form)) {
                continue;
            }
            StateFlowNodeData actionNode = new StateFlowNodeData(e.getKey(), action);
            if (!baseFlowData.containsKey(actionNode)) {
                baseFlowData.put(actionNode, new HashSet<>());
            }
            for (String source : action.getSourceStates()) {
                StateFlowNodeData sourceNode = new StateFlowNodeData(source, getStateDisplay(source), stateRevision);
                if (!baseFlowData.containsKey(sourceNode)) {
                    baseFlowData.put(sourceNode, new HashSet<>());
                }
                baseFlowData.get(actionNode).add(sourceNode);
            }
            parseStateChoice(action.getTargetState(), actionNode, baseFlowData, stateRevision, (S)form);
        }
        
        /**
         * 构造流程图所需的数据结构。结果集中的 key 流程节点数据， values 为其下游节点列表。
         */
        StateFlowNodeData stateUnchangedNode = null;
        Set<StateFlowNodeData> noParentNonActionNodes = new HashSet<>(); 
        Map<StateFlowNodeData, Set<StateFlowNodeData>> resultFlowData = new HashMap<>();
        for (Map.Entry<StateFlowNodeData, Set<StateFlowNodeData>> flowEntry : baseFlowData.entrySet()) {
            StateFlowNodeData childNode = flowEntry.getKey();
            Set<StateFlowNodeData> parentNodes = flowEntry.getValue();
            if (!StateFlowNodeData.Category.ACTION.equals(flowEntry.getKey().getCategory())
                    && parentNodes.isEmpty()) {
                noParentNonActionNodes.add(childNode);
                continue;
            }
            if (!resultFlowData.containsKey(childNode)) {
                resultFlowData.put(childNode, new HashSet<>());
            }
            for (StateFlowNodeData parentNode : parentNodes) {
                if (!resultFlowData.containsKey(parentNode)) {
                    resultFlowData.put(parentNode, new HashSet<>());
                }
                resultFlowData.get(parentNode).add(childNode);
                if (stateUnchangedNode == null) {
                    if (StateFlowNodeData.Category.UNCHANGED.equals(parentNode.getCategory())) {
                        stateUnchangedNode = parentNode;
                    } else if (StateFlowNodeData.Category.UNCHANGED.equals(childNode.getCategory())) {
                        stateUnchangedNode = childNode;
                    }
                }
            }
        }
        /**
         * 移除所有无父节点的非事件节点及其子节点信息。
         * 
         * 在流程解析期间，会根据流程实列选择性的丢弃部分流程分支，
         * 从而导致解析的流程数据中存在部分无父节点的状态存在，而
         * 从流程设计的角度出发，通常都一定是由某个事件创建流程单
         * 从而形成流程实例，因此非事件却无父节点的情况均视为未清
         * 理干净或无效的流程节点。
         */
        for (StateFlowNodeData node : noParentNonActionNodes) {
            resultFlowData.remove(node);
        }
        if (keepUnChanged) {
            return resultFlowData;
        }
        /**
         * 移除非状态流转的流程节点，如表单编辑或相关数据查询事件等节点;
         * 首先移除 "状态保持不变" 的流程节点，然后在移除所有非状态类且子节点为空的节点
         */
        resultFlowData.remove(stateUnchangedNode);
        for (Set<StateFlowNodeData> children: resultFlowData.values()) {
            children.remove(stateUnchangedNode);
        }
        while (true) {
            boolean hasNodeDeleted = false;
            for (StateFlowNodeData parentNode : resultFlowData.keySet().toArray(new StateFlowNodeData[0])) {
                Set<StateFlowNodeData> childrenNodes = resultFlowData.get(parentNode);
                if (childrenNodes.isEmpty() && !StateFlowNodeData.Category.STATE.equals(parentNode.getCategory())
                        && !StateFlowNodeData.Category.STATE_CURRENT.equals(parentNode.getCategory())) {
                    hasNodeDeleted = true;
                    resultFlowData.remove(parentNode);
                    for (Set<StateFlowNodeData> children : resultFlowData.values()) {
                        children.remove(parentNode);
                    }
                }
            }
            if (!hasNodeDeleted) {
                break;
            }
        }
        
        return resultFlowData;
    }
    
    /**
     * 递归解析事件的状态选择器，生成流程节点数据
     */
    private void parseStateChoice(final AbstractStateChoice targetChoice, final StateFlowNodeData parentNode,
                                  final Map<StateFlowNodeData, Set<StateFlowNodeData>> flowData,
                                  final StateFormRevision stateRevision, final S form) {
        /**
         * 最终的简单状态值处理
         */
        if (targetChoice == null || targetChoice.isSimple()) {
            String stateValue = targetChoice == null ? null : targetChoice.getTargetState();
            StateFlowNodeData stateNode = new StateFlowNodeData(stateValue, getStateDisplay(stateValue), 
                                                                        stateRevision);
            if (!flowData.containsKey(stateNode)) {
                flowData.put(stateNode, new HashSet<>());
            }
            flowData.get(stateNode).add(parentNode);
            return;
        }
        /**
         * 状态选择器的递归，遍历其 true 和 false 的目标
         */
        StateFlowNodeData choiceNode = new StateFlowNodeData(targetChoice);
        if (!flowData.containsKey(choiceNode)) {
            flowData.put(choiceNode, new HashSet<>());
        }
        flowData.get(choiceNode).add(parentNode);
        for (boolean yesNo : new boolean[]{true, false}) {
            /**
             * 目标指向为 null， 意味着不会发生状态变化，此时做丢弃处理
             */
            AbstractStateChoice yesNoChoice = yesNo ? targetChoice.getTrueState() : targetChoice.getFalseState();
            if (yesNoChoice == null) {
                continue;
            }
            /**
             * 当明确声明，当前选择器分支与流程实列无关时，做丢弃处理
             */
            if (form != null && !targetChoice.flowMatched(form, yesNo)) {
                continue;
            }
            /**
             * 对于简单选择器，为确保 YesNo 节点的唯一性，随机生成唯一键
             */
            StateFlowNodeData yesNoNode = yesNoChoice.isSimple()
                    ? new StateFlowNodeData(yesNo, StringUtils.randomGuid())
                    : new StateFlowNodeData(yesNo, yesNoChoice.getClass().getName());
            if (!flowData.containsKey(yesNoNode)) {
                flowData.put(yesNoNode, new HashSet<>());
            }
            flowData.get(yesNoNode).add(choiceNode);
            
            /**
             * 对于简单选择器，直接创建其对应的状态节点
             */
            StateFlowNodeData targetNode = yesNoChoice.isSimple()
                    ? new StateFlowNodeData(yesNoChoice.getTargetState(), 
                            getStateDisplay(yesNoChoice.getTargetState()), stateRevision)
                    : new StateFlowNodeData(yesNoChoice);
            if (!flowData.containsKey(targetNode)) {
                flowData.put(targetNode, new HashSet<>());
            }
            flowData.get(targetNode).add(yesNoNode);
            
            /**
             * 非简单选择器，则需要继续递归，生成流程节点数据
             */
            if (!yesNoChoice.isSimple()) {
                parseStateChoice(yesNoChoice, yesNoNode, flowData, stateRevision, form);
            }
        }
    }
    
    /**
     * 定义流程的预定义特殊事件注册信息
     */
    @SuppressWarnings("serial")
    public Map<String, String> getFormPredefinedExtraEvents() {
        return new HashMap<String, String>() {{
            put(getFormAccessEventName(), "访问");
        }};
    }
    
    /**
     * 获取特定类型（外部或内部）的操作
     * 
     * @param external
     *            true  表示外部 : 创建事件或有source states的事件; 
     *            false 表示内部 : 非创建事件, 且无 source states定义
     * @param targetState
     *            限制仅获取目标状态为该值操作
     * @return
     */
    private Map<String, AbstractStateAction<S, ?, ?>> getFormActions(boolean external, String targetState) throws Exception {
        Map<String, AbstractStateAction<S, ?, ?>> actions;
        if ((actions = getFormActions()) == null || actions.isEmpty()) {
            return Collections.emptyMap();
        }
        
        String name;
        String[] sourceStates;
        boolean isExternalEvent;
        AbstractStateChoice targetStateChoice;
        AbstractStateAction<S, ?, ?> definition;
        boolean targetStateReuired = StringUtils.isNotBlank(targetState);
        
        /**
         * false 返回内部事件，true 返回外部事件。
         * 如果指定了目标状态的情况下，只返回明确为该目标状态的事件。
         */
        Map<String, AbstractStateAction<S, ?, ?>> targetActions = new HashMap<>();
        for (Entry<String, AbstractStateAction<S, ?, ?>> action : actions.entrySet()) {
            name = action.getKey();
            definition = action.getValue();
            if (StringUtils.isBlank(name) || definition == null) {
                continue;
            }
            sourceStates = definition.getSourceStates();
            targetStateChoice = definition.getTargetState();
            isExternalEvent = sourceStates.length > 0;
            
            // 如果指定目标状态的选择，不符合的一律忽略
            if (targetStateReuired) {
                if (targetStateChoice == null || !targetStateChoice.isSimple()
                              || !targetState.equals(targetStateChoice.getTargetState())) {
                    continue;
                }
            }
            if (external) {
                // 表单列表事件无 source states, 但属于外部事件 
                if (definition.isListEvent()) {
                    targetActions.put(name, definition);
                    if (definition.getSourceStates().length > 0) {
                      throw new StateFormListEventDefinedException(getFormName(), name);
                    }
                } else if (isExternalEvent) {
                    targetActions.put(name, definition);
                }
            } else if (!isExternalEvent) {
                targetActions.put(name, definition);
            }
        }
        return targetActions;
    }
    
    /**
     * 获取特定目标状态的内部操作
     * 
     * @param targetState
     *            限制仅获取目标状态为该值操作
     * @return
     */
    protected Map<String, AbstractStateAction<S, ?, ?>> getInternalFromActions(String targetState)  throws Exception {
        if (StringUtils.isBlank(targetState)) {
            return Collections.emptyMap();
        }
        return getFormActions(false, targetState);
    }
    
    /**
     * 确认给定的操作是否允许执行
     */
    public boolean checkAction(String event, long formId) throws Exception {
        return checkFromAction(event, getForm(formId));
    }
    
    /**
     * 确认给定的操作是否允许执行
     */
    public boolean checkFromAction(String event, S form) throws Exception {
        AbstractStateAction<S, ?, ?> action = null;
        if ((action = getExternalFormAction(event)) == null || action.isListEvent()) {
            log.warn("表单({})未定义此外部操作({})。", getFormName(), event);
            return false;
        }
        String sourceState;
        if (!action.sourceContains(sourceState = form.getState())) {
            log.warn("表单({})的外部操作({})不可从指定的状态({})上执行。", getFormName(), event, sourceState);
            return false;
        }
        try {
            Authority authority;
            if ((authority = action.getAuthority()) == null) {
                log.warn("表单({})操作({})未声明授权信息。", getFormName(), event);
                return false;
            }
            if (createInstance(authority.rejecter()).check(form)) {
                return false;
            }
            if (createInstance(authority.checker()).check(form)) {
                return true;
            }
            /**
             * 全局授权验证
             */
            long[] allScopeTargetIds;
            if ((allScopeTargetIds = parseAuthorityScopeTargetIds(authority, form)) == null) {
                return getPermissionService().hasFormEventPermission(authority.value(), getFormName(), event, null);
            }
            /**
             * 针对 multipleChoiceEnabled 的场景，只要当前用户有任一授权标的的授权即可，
             * 否则必须具有所有授权标的的授权才允许执行操作。
             */
            if (authority.multipleChoiceEnabled()) {
                for (Long targetId : allScopeTargetIds) {
                    if (getPermissionService().hasFormEventPermission(authority.value(), getFormName(), event, targetId)) {
                        return true;
                    } 
                }
                return false;
            }
            for (Long targetId : allScopeTargetIds) {
                if (!getPermissionService().hasFormEventPermission(authority.value(), getFormName(), event, targetId)) {
                   return false;
                } 
            }
            return true;
        } catch(Exception e) {
            log.error(String.format("表单(%s)的外部操作(%s)可执行检测程序异常。", getFormName(), event), e);
            return false;
        }
    }

    /**
     * 针对表单的创建事件。
     * 
     */
    public boolean checkListAction(String event, AbstractStateForm form) throws Exception {
        AbstractStateAction<S, ?, ?> action = null;
        if ((action = getExternalFormAction(event)) == null || !action.isListEvent()) {
            log.warn("表单({})未定义此创建操作({})。", getFormName(), event);
            return false;
        }
        
        try {
            Authority authority;
            if ((authority = action.getAuthority()) == null) {
                log.warn("表单({})操作({})未声明授权信息。", getFormName(), event);
                return false;
            }
            /** 
             * form 为 null 值，视为事前评估，只要在授权范围(SopeType)上发现
             * 有任一标的(ScopeId)被授予该权限，即为允许执行。
             * 
             * TODO: 针对非 Create 事件，后续需根据设计调整对应的收取按验证机制。
             **/
            if (form == null) {
                return getPermissionService().hasFormEventAnyPermission(authority.value(), getFormName(), event);
            }
            if (createInstance(authority.rejecter()).check(form)) {
                return false;
            }
            if (createInstance(authority.checker()).check(form)) {
                return true;
            }
            /**
             * 全局授权验证
             */
            long[] allScopeTargetIds;
            if ((allScopeTargetIds = parseAuthorityScopeTargetIds(authority, form)) == null) {
                return getPermissionService().hasFormEventPermission(authority.value(), getFormName(), event, null);
            }
            /**
             * 针对 List 事件（包括 Create 事件），不适用 multipleChoiceEnabled 的场景，
             * 因此，必须具有所有授权标的的授权才允许执行操作。
             */
            for (Long targetId : allScopeTargetIds) {
                if (!getPermissionService().hasFormEventPermission(authority.value(), getFormName(), event, targetId)) {
                   return false;
                } 
            }
            return true;
        } catch(Exception e) {
            log.error(String.format("表单(%s)的创建操作(%s)可执行检测程序异常。", getFormName(), event), e);
            return false;
        }
    }
    
    /**
     * 解析流程实列中，事件的授权标的清单。
     * 
     * 如果返回 null，即代表为全局授权，无授权标的存在；
     * 否则，在解析失败或未解析到标的时将抛出异常。
     * 
     * @param authority
     * @param form
     * @return
     * @throws Exception
     */
    protected long[] parseAuthorityScopeTargetIds(@NonNull Authority authority, AbstractStateForm form) throws Exception {
        if (!getPermissionService().getEnsuredAuthorityScope(authority.value()).isCheckTargetId()) {
           return null;
        }
        long[] allScopeTargetIds = null;
        if (!AuthorityScopeIdNoopParser.class.equals(authority.parser())) {
            Long scopeTargetId = null;
            if ((scopeTargetId = authority.parser().newInstance().getAuthorityScopeId(form)) == null) {
                throw new MessageException(String.format("解析授权标的失败（formId = %s, parser = %s）",
                        form.getId(), authority.parser().getName()));
            }
            allScopeTargetIds = new long[] {scopeTargetId};
        } else if (!AuthorityScopeIdNoopMultipleParser.class.equals(authority.multipleParser())){
            if ((allScopeTargetIds = authority.multipleParser().newInstance().getAuthorityScopeIds(form)) == null) {
                throw new MessageException(String.format("解析授权标的失败（formId = %s, parser = %s）",
                        form.getId(), authority.multipleParser().getName()));
            }
        }
        if (allScopeTargetIds == null || allScopeTargetIds.length <= 0) {
            throw new MessageException(String.format("未解析到授权标的信息（formId = %s, parser = %s）",
                    form.getId(), authority.multipleParser().getName()));
        }
        return allScopeTargetIds;
    }
    
    /**
     * 获取当前表单指定事件上的授权人员清单
     */
    public long[] getActionUserIds(String event, S form) throws Exception {
        AbstractStateAction<S, ?, ?> action = null;
        if ((action = getExternalFormAction(event)) == null || action.isListEvent()) {
            throw new MessageException(String.format("表单(%s)未定义此外部操作(%s)", getFormName(), event));
        }
        Authority authority;
        if ((authority = action.getAuthority()) == null) {
            throw new MessageException(String.format("表单(%s)操作(%s)未声明授权信息。", getFormName(), event));
        }
        long[] allScopeTargetIds = parseAuthorityScopeTargetIds(authority, form);
        return getPermissionService().queryAuthedUserIds(AbstractStateFormService.getFormEventKey(getFormName(), event),
                authority.value(), allScopeTargetIds);
    }
    
    /**
     * 操作执行前的检查。
     * 
     * @return 如检查失败，抛出异常；否则返回当前的表单实体。
     */
    protected S triggerPreHandle(String event, AbstractStateForm form) throws Exception {
        S originForm = null;
        AbstractStateAction<S, ?, ?> action;
        if ((action = getExternalFormAction(event)) == null) {
            throw new StateFormActionNotFoundException(getFormName(), event);
        }
        log.info("Try to check form action trigger : event = {}, class = {}, listEvent = {}"
                                + ", ensureNoStateChange = {}, stateRevisionChangeIgnored = {}", 
                        event, action.getClass().getName(), action.isListEvent(), 
                        action.ensureNoStateChange(), action.getStateRevisionChangeIgnored());
        if (action.isListEvent()) {
            if (!checkListAction(event, form)) {
                throw new StateFormActionDeclinedException(getFormName(), event);
            }
            return null;
        }
        originForm = getForm(form.getId());
        if (originForm.getRevision() == null) {
            throw new StateFormRevisionNotFoundException(getFormName(), originForm.getId(), true);
        }
        if (!(action.ensureNoStateChange() && action.getStateRevisionChangeIgnored())) {
            if (form.getRevision() == null) {
                throw new StateFormRevisionNotFoundException(getFormName(), form.getId());
            }
            if (form.getRevision().longValue() != originForm.getRevision().longValue()) {
                throw new StateFormRevisionChangedException(form, form.getRevision(), originForm.getRevision());
            }
        }
        if (!checkFromAction(event, originForm)) {
            throw new StateFormActionDeclinedException(getFormName(), event);
        }
        return originForm;
    }
    
    /**
     * 获取指定的表单事件数据。
     */
    public AbstractStatePrepare triggerPrepare(String event, long formId, NameValuePair... params) throws Exception {
        AbstractStateAction<S, ?, ?> action;
        if ((action = getExternalFormAction(event)) == null) {
            throw new StateFormActionNotFoundException(getFormName(), event);
        }
        log.info("Try to prepare form action trigger : event = {}, class = {}, listEvent = {}"
                                + ", ensureNoStateChange = {}, stateRevisionChangeIgnored = {}"
                                + ", prepareRequired = {}", 
                        event, action.getClass().getName(), action.isListEvent(), 
                        action.ensureNoStateChange(), action.getStateRevisionChangeIgnored(),
                        action.prepareRequired());
        return action.prepareForm(this, event, formId > 0 ? getForm(formId) : null, params);
    }
    
    /**
     * 执行指定的操作，忽略返回值。
     */
    public void triggerAction(String event, AbstractStateForm form) throws Exception {
        triggerAction(event, form, "");
    }
    
    /**
     * 执行指定的操作，忽略返回值。
     */
    public void triggerAction(String event, AbstractStateForm form, String message) throws Exception {
        triggerAction(event, form, message, void.class);
    }
    
    /**
     * 执行指定操作，必须确保事件定义的返回值与给定的返回值类型匹配。
     */
    public <T> T triggerAction(String event, AbstractStateForm form, Class<T> clazz) throws Exception {
        return triggerAction(event, form, "", clazz);
    }
    
    /**
     * 执行指定操作，必须确保事件定义的返回值与给定的返回值类型匹配。
     */
    @SuppressWarnings("unchecked")
    public <T> T triggerAction(final String event, final AbstractStateForm form, final String message, final Class<T> clazz) throws Exception {
        
        AbstractStateAction<S, ?, ?> action;
        if ((action = getExternalFormAction(event)) == null) {
            throw new StateFormActionNotFoundException(getFormName(), event);
        }
        
        /* 确认无内容变更的操作, 不添加分布锁 */
        if ((action.isListEvent() || action.ensureNoStateChange())
                    && action.getStateRevisionChangeIgnored()) {
            return triggerActionWithoutLock(event, form, message, clazz);
        }
        
        /* 加锁且事务处理, 表单创建使用表单全局锁 */
        Long lockObjectId = form.getId();
        if (action.isListEvent()) {
            lockObjectId = -1000000L;
        }
        final Object[] result = new Object[] {null};
        getLockService().lockAndRun(getFormName(), lockObjectId, event,
                new CommonLockExecutor() {
                    @Override
                    public void execute() throws Exception {
                        result[0] = triggerActionWithoutLock(event, form, message, clazz);
                    }
                });
        return (T)result[0];
    }
    
    /**
     * 执行指定操作，必须确保事件定义的返回值与给定的返回值类型匹配。
     */
    @SuppressWarnings("unchecked")
    protected <T> T triggerActionWithoutLock(final String event, final AbstractStateForm form, final String message, final Class<T> clazz) throws Exception {
        
        AbstractStateAction<S, ?, ?> action;
        if ((action = getExternalFormAction(event)) == null) {
            throw new StateFormActionNotFoundException(getFormName(), event);
        }
        
        /**
         * 前置校验，确保可执行操作
         */
        S originForm = triggerPreHandle(event, form);
        try {
            /**
             * 校验 message 是否设置
             */
            if (action.messageRequired() != null && action.messageRequired() && StringUtils.isBlank(message)) {
                throw new StateFormActionMessageRequiredException(getFormName(), event);
            }
            
            /**
             * 校验要求字段确保值不为空，且值在预期的选项范围内
             */
            if (form != null) {
                ClassUtil.checkFormRequiredAndOpValue(form, true /* 忽略只读字段 */,
                        AbstractStateAction.getInternalFields() /* 忽略内部字段 */);
            }
            
            /**
             * 校验返回数据类型 
             */
            Class<?> returnTypeClass = action.getReturnTypeClass();
            if (clazz != null && !void.class.equals(clazz) && !clazz.isAssignableFrom(returnTypeClass)) {
                log.error("Defined return class of action {} is {}, but require {}",
                                            event, returnTypeClass, clazz);
                throw new StateFormActionReturnException(getFormName(), event);
            }
            
            /**
             * 执行预定义处理方法 
             */
            final Object result = action.handleForm(this, event, originForm, form, message);
            
            /**
             * 异步任务处理
             */
            if (AbstractStateAsyncEeventView.class.isAssignableFrom(returnTypeClass)) {
                new Thread(new RunableWithSessionContext() {
                    @Override
                    public void exec() {
                        try {
                            /* 重要：此处的休眠时为确保操作结束后，线程才开始运行，
                             * 否则该创建异步任务的事务还未提交，导致等待时的异常 */
                            Thread.sleep(1000);
                            if (((AbstractStateAsyncEeventView)result).waitingForFinished(event, message, originForm, form)) {
                                psotHandleForm(event, message, result, originForm, form);
                            }
                        } catch (Exception e) {
                            log.error(e.toString(), e);
                        }
                    }
                }).start();
            } else {
                psotHandleForm(event, message, result, originForm, form);
            }
            if (clazz == null || void.class.equals(clazz)) {
                return (T)null;
            }
            return (T)result;
        } catch (Throwable ex) {
            triggerExceptionHandle(event, originForm, form, ex);
            throw ex;
        }
    }
    
    protected abstract void addMultiChoiceCoveredTargetScopeIds(String event, AbstractStateForm form, String scopeType, long ...coverdTargetScopeIds) throws Exception;
    
    protected abstract long[] queryMultipleChoiceCoveredTargetScopeIds(String event, AbstractStateForm form)  throws Exception;;
    
    protected abstract void cleanMultipleChoiceTargetScopeIds(String[] clearEvents, AbstractStateForm form) throws Exception;;
    
    private void psotHandleForm(String event, String message, Object result, S originForm, AbstractStateForm form) throws Exception {
        /**
         * 首先，验证事件的响应结果
         */
        AbstractStateAction<S, ?, ?> action = getExternalFormAction(event);
        if (result == null && !action.allowHandleReturnNull()) {
            throw new StateFormEventResultNullException(getFormName(), event);
        }
        boolean isCreateEvent = action.isCreateEvent();
        /**
         * 针对单一表单事件，需要记录操作日志，启动事件触发器（内部事件）
         * 
         * TODO: 针对 List（不包括 Create） 事件，后续需根据设计，实现对应的能力。
         */
        if (isCreateEvent || !action.isListEvent()) {
            String newState = null;
            AbstractStateChoice targetState;
            if ((targetState = action.getTargetState()) != null) {
                newState = targetState.getTargetState(form);
            }
            if (isCreateEvent) {
                if (StringUtils.isBlank(newState)) {
                    throw new StateFormListEventTargetException(getFormName(), event);
                }
                if (!getFormClass().isAssignableFrom(result.getClass())) {
                    throw new StateFormListEventResultException(getFormName(), event);
                }
                form.setRevision(1L);
                form.setState(newState);
                form.setId(((AbstractStateForm)result).getId());
            } else {
                Authority authority = action.getAuthority();
                form.setState(StringUtils.ifBlank(newState, originForm.getState()));
                /**
                 * 当清除和 multipleChoiceEnabled 同时存在时，优先执行清除操作，
                 * 当然在此场景下，将很可能导致流程始终无法正常走向下一状态。
                 */
                if (!AuthorityScopeIdNoopMultipleCleaner.class.equals(authority.multipleCleaner())) {
                    String[] clearEvents = authority.multipleCleaner().newInstance().getEventsToClean();
                    cleanMultipleChoiceTargetScopeIds(clearEvents, form);
                }
                /**
                 * 针对 multipleChoiceEnabled 场景，记录下当前用户操作覆盖的收取按标的，
                 * 并根据是否已全部覆盖当前流程事件实例所有可查询到的要求，来决定是否走
                 * 向流程实例的下一个状态。
                 */
                if (!AuthorityScopeIdNoopMultipleParser.class.equals(authority.multipleParser())) {
                    if (authority.multipleChoiceEnabled()) {
                        /**
                         * 获取当前用户操作覆盖的授权标的，并记录下来
                         */ 
                        long[] coveredTargetScopeIds = getPermissionService()
                                .queryFormEventScopeTargetIds(authority.value(), getFormName(), event);
                        /* 当前流程实例涉及的所有授权标的清单 */ 
                        long[] multipleTargetScopeIds = authority.multipleParser().newInstance()
                                .getAuthorityScopeIds(originForm == null ? form : originForm);
                        if (coveredTargetScopeIds == null) {
                            coveredTargetScopeIds = multipleTargetScopeIds;
                        }
                        addMultiChoiceCoveredTargetScopeIds(event, form, authority.value(), coveredTargetScopeIds);
                        /**
                         * 查询并确认是否已完全覆盖，以决定流程走向
                         */
                        coveredTargetScopeIds = queryMultipleChoiceCoveredTargetScopeIds(event, form);
                        if (coveredTargetScopeIds != null && coveredTargetScopeIds.length > 0) {
                            if (!ArrayUtils.containsAll(coveredTargetScopeIds, multipleTargetScopeIds)) {
                                form.setState(originForm.getState());
                            }
                        }
                    }
                }
            }
            /**
             * 补充事件输出的变更描述，以便记录到操作日志中
             */
            String eventAppendMessage;
            if (result != null && (result instanceof AbstractStateFormEventMessageAppender)
                    && StringUtils.isNotBlank(eventAppendMessage = 
                        ((AbstractStateFormEventMessageAppender)result).getEventAppendMessage())) {
                if (StringUtils.isBlank(message)) {
                    message = eventAppendMessage;
                } else {
                    message = String.format("%s\r\n%s", message, eventAppendMessage);
                }
            }
            
            log.info("Form action has been handled : form = {}, id = {}, event = {}, target = {}",
                                getFormName(), form.getId(), event, newState);
            /**
             * 处理内部事件，即状态到达或离开时的自动触发的事件
             */
            if ((isCreateEvent && form.getId() != null) || originForm != null) {
                AbstractStateAction<S, ?, ?> eventAction;
                Map<String, AbstractStateAction<S, ?, ?>> stateActions;
                /**
                 * 处理状态离开事件
                 */
                if (originForm != null && (stateActions = getInternalFromActions(originForm.getState())).size() > 0) {
                    for (Entry<String, AbstractStateAction<S, ?, ?>> stateAction :stateActions.entrySet()) {
                        if (((eventAction = stateAction.getValue()) instanceof AbstractStateLeaveAction)
                                && (!StringUtils.equals(originForm.getState(), form.getState())
                                        || ((AbstractStateLeaveAction<?>)eventAction).executeWhenNoStateChanged())) {
                            eventAction.handleForm(this, event, stateAction.getKey(), originForm, form, message);
                        }
                    }
                }
                /**
                 * 处理状态进入事件
                 */
                if ((stateActions = getInternalFromActions(form.getState())).size() > 0) {
                    for (Entry<String, AbstractStateAction<S, ?, ?>> stateAction :stateActions.entrySet()) {
                        if (((eventAction = stateAction.getValue()) instanceof AbstractStateEnterAction)
                                && (isCreateEvent || !StringUtils.equals(originForm.getState(), form.getState())
                                        || ((AbstractStateEnterAction<?>)eventAction).executeWhenNoStateChanged())) {
                            eventAction.handleForm(this, event, stateAction.getKey(), originForm, form, message);
                        }
                    }
                }
            }
        }
        /**
         * 保存状态并回调预定义后处理方法
         */
        triggerPostHandle(event, originForm, form, message);
    }
    
    protected void triggerPostHandle(String event, S originForm, AbstractStateForm form, String message) throws Exception {
        /**
         * 当表单状态发生变化，或未声明忽略版本升级时，将对表单版本进行升级。
         */
        AbstractStateAction<S, ?, ?> absAction = getExternalFormAction(event);
        String originState = absAction.isListEvent() ? null : originForm.getState();
        if (!StringUtils.equals(originState, form.getState())
                || !absAction.getStateRevisionChangeIgnored()) {
            if (!absAction.isDeleteEvent() && form.getId() != null) {
                saveStateRevision(form.getId(), StringUtils.equals(originState, form.getState()) ? "" : form.getState());
            }
        }
        /**
         * 调用 POST 回调。注意：POST 回调失败时，事件操作继续。
         */
        try {
            absAction.postForm(this, event, originForm, form, message);
        } catch (Exception e) {
            log.error(e.toString() ,e);
        }
        
        if (form.getId() == null) {
            return;
        }
        /**
         * 发送系统通知，并记录事件日志。
         */
        String logEventName = event;
        AbstractStateAction<S, ?, ?> eventAction;
        if ((eventAction = getExternalFormAction(event)) != null 
                    && eventAction.isCommentEvent() ) {
            logEventName  = AbstractStateCommentAction.getFormLogEvent();
        }
        AbstractStateForm changedForm = form;
        if (eventAction != null && !eventAction.isDeleteEvent()) {
            changedForm = getForm(form.getId());
        }
        AbstractNotifyService<?> notifier;
        /* 异步通知 */
        if ((notifier = getNotifyService()) != null) {
            notifier.sendAsync(
                String.format("system.state.form:%s:%s", getFormName(), event), 
                new ObjectMap().put("formEvent", event)
                    .put("formName", getFormName())
                    .put("formService", this)
                    .put("originForm", originForm)
                    .put("changedForm", changedForm)
                    .put("eventMessage", message),
                    AbstractNotifyService.NOEXCEPTION_TMPL_NOTFOUD);
        }
        /* 当有未提供操作说明，且未发生版本升级时，忽略日志记录 */
        if (StringUtils.isBlank(message) && originForm != null && changedForm != null 
                            && changedForm.getRevision().equals(originForm.getRevision())) {
            return;
        }
        getLogService().createLog(logEventName, getFormName(), form.getId(), message,
                originForm, changedForm);
    }
    
    protected void triggerExceptionHandle(String event, S originForm, AbstractStateForm form, Throwable ex) throws Exception {
        
    }
    
    protected StateFormRevision loadStateRevision(long formId) throws Exception {
        Map<Long, StateFormRevision> sr = loadStateRevision(new Long[] {formId});
        if (sr == null || sr.size() != 1) {
            return null;
        }
        return sr.values().iterator().next();
    }

    public final Class<AbstractStateForm> getActionFormTypeClass(String event) throws Exception {
        AbstractStateAction<S, ?, ?> action;
        if((action = getFormActions().get(event)) == null) {
            throw new StateFormActionNotFoundException(getFormName(), event);
        }
        return action.getFormTypeClass();
    }
    
    public final Class<AbstractStateForm> getActionOriginTypeClass(String event) throws Exception {
        AbstractStateAction<S, ?, ?> action;
        if((action = getFormActions().get(event)) == null) {
            throw new StateFormActionNotFoundException(getFormName(), event);
        }
        return action.getOriginTypeClass();
    }

    public final Class<?> getActionReturnTypeClass(String event) throws Exception {
        AbstractStateAction<S, ?, ?> action;
        if((action = getFormActions().get(event)) == null) {
            throw new StateFormActionNotFoundException(getFormName(), event);
        }
        return action.getReturnTypeClass();
    }
    
    public String getFormEventKey(String event) {
        return getFormEventKey(getFormName(), event);
    }
    
    public static String getFormEventKeyPrefix(String formName) {
        return String.format("%s::", formName);
    }
    
    public static String getFormEventKey(String formName, String formEvent) {
        return String.format("%s%s", getFormEventKeyPrefix(formName), formEvent);
    }
    
    public static String getFormAccessEventName() {
        return "_access";
    }
    
    public final String getFormAccessEventKey() {
        return getFormEventKey(getFormName(), getFormAccessEventName());
    }
    
    public static final String getFormAccessEventKey(String formName) {
        return getFormEventKey(formName, getFormAccessEventName());
    }
    
    /**
     * 检索表单的日志记录
     * @param formId
     * @param fromLogIndex
     */
    public List<SimpleLog> queryLogs (long formId, Long fromLogIndex) throws Exception {
        return getLogService().queryLogExcludeOperationTypes(
                getFormName(), formId, new String[] {AbstractStateCommentAction.getFormLogEvent()}, fromLogIndex);
    }
    
    /**
     * 检索表单的首条日志记录
     * @param formId
     */
    public SimpleLog queryFirstLog (long formId) throws Exception {
        return getLogService().getFirstLog(getFormName(), formId);
    }
    
    /**
     * 检索表单的最新的日志记录
     * @param formId
     */
    public SimpleLog queryLatestLog (long formId) throws Exception {
        return getLogService().getLatestLog(getFormName(), formId);
    }
    
    /**
     * 检索表单的讨论列表
     * @param formId
     * @param fromLogIndex
     */
    public List<SimpleLog> queryComments (long formId, Long fromLogIndex) throws Exception {
        return getLogService().queryLogIncludeOperationTypes(
                getFormName(), formId, new String[] {AbstractStateCommentAction.getFormLogEvent()}, fromLogIndex);
    }
    
    /**
     * 获取默认的预定义查询
     */
    public StateFormNamedQuery<? extends S> getFormDefaultQuery() {
        Map<String, StateFormNamedQuery<? extends S>> queries;
        if ((queries = getFormQueries()) == null || queries.isEmpty()) {
            return null;
        }
        for (String name : queries.keySet()) {
            if ("default".equalsIgnoreCase(name)) {
                return queries.get(name);
            }
        }
        for (StateFormNamedQuery<? extends S>query : queries.values()) {
            if (query != null) {
                 return query;
            }
        }
        return null;
    }
    
    /**
     * 获取自定名称的查询
     * @param name
     */
    public StateFormNamedQuery<? extends S> getFormNamedQuery(String name) {
        if (name == null) {
            return getFormDefaultQuery();
        }
        Map<String, StateFormNamedQuery<? extends S>> queries;
        if ((queries = getFormQueries()) == null || queries.isEmpty()) {
            return null;
        }
        return queries.get(name);
    }
}
