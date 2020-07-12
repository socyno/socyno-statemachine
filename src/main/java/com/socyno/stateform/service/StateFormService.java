package com.socyno.stateform.service;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.http.NameValuePair;

import com.github.reinert.jjschema.v1.FieldOption;
import com.github.reinert.jjschema.v1.FieldOptionsFilter;
import com.github.reinert.jjschema.v1.FieldType;
import com.github.reinert.jjschema.v1.FieldType.FieldOptionsType;
import com.google.gson.JsonElement;
import com.socyno.base.bscexec.MessageException;
import com.socyno.base.bscmixutil.ClassUtil;
import com.socyno.base.bscmixutil.CommonUtil;
import com.socyno.base.bscmixutil.JsonUtil;
import com.socyno.base.bscmixutil.StringUtils;
import com.socyno.base.bscmodel.ObjectMap;
import com.socyno.base.bscmodel.PagedList;
import com.socyno.base.bscmodel.PagedListWithTotal;
import com.socyno.base.bscmodel.SimpleLog;
import com.socyno.base.bscsqlutil.AbstractDao;
import com.socyno.base.bscsqlutil.AbstractDao.ResultSetProcessor;
import com.socyno.stateform.abs.AbstractStateForm;
import com.socyno.stateform.abs.AbstractStateFormService;
import com.socyno.stateform.abs.AbstractStateFormServiceWithBaseDao;
import com.socyno.stateform.abs.AbstractStatePrepare;
import com.socyno.stateform.exec.StateFormActionNotFoundException;
import com.socyno.stateform.exec.StateFormCustomFieldFormNotFoundException;
import com.socyno.stateform.exec.StateFormNotDefinedException;
import com.socyno.stateform.field.FilterAbstractFrom;
import com.socyno.stateform.field.FilterBasicKeyword;
import com.socyno.stateform.model.StateFlowDefinition;
import com.socyno.stateform.model.StateFlowLinkData;
import com.socyno.stateform.model.StateFlowNodeData;
import com.socyno.stateform.util.StateFormActionDefinition;
import com.socyno.stateform.util.StateFormSimpleDefinition;
import com.socyno.stateform.util.StateFormFieldCustomAttribute;
import com.socyno.stateform.util.StateFormWithAction;
import com.socyno.base.bscsqlutil.SqlQueryUtil;
import com.socyno.webbsc.authority.Authority;
import com.socyno.webbsc.authority.AuthorityEntity;
import com.socyno.webbsc.authority.AuthorityScope;
import com.socyno.webbsc.authority.AuthorityScopeIdNoopMultipleCleaner;
import com.socyno.webbsc.authority.AuthorityScopeIdNoopMultipleParser;
import com.socyno.webbsc.authority.AuthorityScopeIdNoopParser;
import com.socyno.webbsc.ctxutil.ContextUtil;
import com.socyno.webbsc.ctxutil.HttpMessageConverter;
import com.socyno.webbsc.service.jdbc.SimpleLogService;

@Slf4j
public class StateFormService {
    
    @Data
    @Accessors(chain=true)
    private static class CommonStateFormInstance {
        private String formName;
        private String formDisplay;
        private AbstractStateFormServiceWithBaseDao<?> serviceInstance;
    }
    
    private static final Map<String, CommonStateFormInstance> STATE_FORM_INSTANCES
                            = new ConcurrentHashMap<>();
    
    private static class FormCustomDataCacher implements Runnable {
        @Override
        public void run() {
            refresh();
        }
        
        @Getter
        @Setter
        @ToString
        public static class FormCustomView {
            private String formName;
            private String formView;
        }
        
        /**
         * SELECT DISTINCT
         *     class_path form_name,
         *     form_attrs form_view
         * FROM
         *     system_form_viewattrs
         */
        @Multiline
        private static final String SQL_QUERY_FORM_VIEW = "x";
        
        public void refresh(String ...forms) {
            try {
                Object[] args = null;
                StringBuilder sql = new StringBuilder(SQL_QUERY_FORM_VIEW);
                if (forms != null && forms.length > 0) {
                    sql.append(" WHERE class_path in ")
                        .append(StringUtils.join("?", forms.length, ", ", "(", ")"));
                    args = (Object[])forms;
                }
                saveToCache(getDao().queryAsList(FormCustomView.class, sql.toString(), args), 0);
            } catch (Exception e) {
                log.error("Failed to load configs.", e);
            }
        }
        
        private void saveToCache(List<FormCustomView> list, int offset) {
            int listSize;
            if (list == null || (listSize = list.size()) <= 0 || offset >= listSize) {
                return;
            }
            String currentForm = null;
            String currentAttrs = null;
            try {
                while (offset < listSize) {
                    currentForm = (String) list.get(offset).getFormName();
                    currentAttrs = (String) list.get(offset).getFormView();
                    ClassUtil.AttributesProccessor.setCustomFormAttributes(currentForm,
                            ClassUtil.AttributesProccessor.parseFormCustomizedProperties(currentForm, currentAttrs));
                    offset++;
                }
            } catch (Exception ex) {
                log.error(String.format("Failed to load form custom attributes, form = %s", currentForm), ex);
                saveToCache(list, ++offset);
            }
        }
    }
    
    private final static FormCustomDataCacher FORM_DATA_CACHER = new FormCustomDataCacher();
    
    static {
        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(FORM_DATA_CACHER, 20, 30,
                TimeUnit.SECONDS);
    }
    
    public static AbstractDao getDao() {
        return ContextUtil.getBaseDataSource();
    }
    
    @Data
    public static class CommonStateFormRegister {
        private Long id;
        private String formName;
        private String formService;
        private String formDisplay;
        private String formBackend;
        private Integer disabled;
        
        public boolean isEnabled() {
            return disabled == null || disabled == 0;
        }
    }
    
    /**
     * 注册新的表单
     * @param register
     * @throws Exception
     */
    public static void registerForm(CommonStateFormRegister register) throws Exception {
        if (register == null || StringUtils.isBlank(register.getFormName())
                || StringUtils.isBlank(register.getFormDisplay())
                || StringUtils.isBlank(register.getFormService())
                || StringUtils.isBlank(register.getFormBackend())) {
            throw new MessageException("通用流程表单定义数据不规范。");
        }
        getDao().executeUpdate(SqlQueryUtil.prepareInsertQuery(
            "system_form_defined", new ObjectMap()
                    .put("form_name", register.getFormName().trim())
                    .put("form_backend", register.getFormBackend().trim())
                    .put("form_display", register.getFormDisplay().trim())
                    .put("form_service", register.getFormService().trim())
        ));
    }
    
    /**
     * 更新已注册的表单
     * @param register
     * @throws Exception
     */
    public static void updateForm(CommonStateFormRegister register) throws Exception {
        if (register == null || StringUtils.isBlank(register.getFormName())
                || StringUtils.isBlank(register.getFormDisplay())
                || StringUtils.isBlank(register.getFormService())
                || StringUtils.isBlank(register.getFormBackend())) {
            throw new MessageException("通用流程表单定义数据不规范。");
        }
        getDao().executeUpdate(SqlQueryUtil.prepareUpdateQuery(
            "system_form_defined", new ObjectMap()
                    .put("=form_name", register.getFormName())
                    .put("form_backend", register.getFormBackend().trim())
                    .put("form_display", register.getFormDisplay().trim())
                    .put("form_service", register.getFormService().trim())
                    .put("disabled", CommonUtil.ifNull(register.getDisabled(), 0) == 0 ? 0 : 1)
        ));
        /* 完成更新后需要清除表单的缓存实例，确保访问时可重新加载 */
        STATE_FORM_INSTANCES.remove(register.getFormName());
    }
    
    /**
     * 删除已注册的表单
     * @param register
     * @throws Exception
     */
    public static void removeForm(String formName) throws Exception {
        getDao().executeTransaction(new ResultSetProcessor() {
            @Override
            public void process(ResultSet result, Connection conn) throws Exception {
                getDao().executeUpdate(SqlQueryUtil.prepareDeleteQuery("system_form_defined",
                        new ObjectMap().put("=form_name", formName)));
                getDao().executeUpdate(SqlQueryUtil.prepareDeleteQuery("system_form_actions",
                        new ObjectMap().put("=form_name", formName)));
            }
        });
        /* 完成更新后需要清除表单的缓存实例 */
        STATE_FORM_INSTANCES.remove(formName);
    }
    
    /**
     * 启动或禁用已注册的表单
     * @param register
     * @throws Exception
     */
    public static void toggleForm(String formName) throws Exception {
        if (getDao().executeUpdate(SqlQueryUtil.prepareUpdateQuery("system_form_defined",
                    new ObjectMap().put("disabled", 0).put("=form_name", formName).put("!=disabled", 0)
                )) == 0) {
            getDao().executeUpdate(SqlQueryUtil.prepareUpdateQuery("system_form_defined",
                new ObjectMap().put("disabled", 1).put("=form_name", formName).put("=disabled", 0)
            ));
        }
    }
    
    /**
     * 获取注册的表单信息
     */
    public static CommonStateFormRegister getFormRegister(String formName) throws Exception {
        if (StringUtils.isBlank(formName)) {
            return null;
        }
        return getDao().queryAsObject(CommonStateFormRegister.class,
                    "SELECT * FROM system_form_defined WHERE form_name = ?",
                     new Object[ ] {formName});
    }
        
    /**
     * SELECT
     *     f.*
     * FROM
     *     system_form_defined f
     */
    @Multiline
    private final static String SQL_QUERY_DEFINED_FORM = "X";
    
    /**
     * WHERE
     *     f.form_name LIKE CONCAT('%', ? , '%')
     * OR
     *     f.form_display LIKE CONCAT('%', ? , '%')
     */
    @Multiline
    private final static String SQL_QUERY_DEFINED_LIKE = "X";
    
    /**
     * 列举所有已注册的通用表单
     */
    public static List<CommonStateFormRegister> listStateFormRegister() throws Exception {
         return listStateFormRegister(null);
    }
    
    /**
     * 根据给定的关键字检索已注册的通用表单
     */
    public static List<CommonStateFormRegister> listStateFormRegister(String namelike) throws Exception {
        if (StringUtils.isBlank(namelike)) {
            return getDao().queryAsList(CommonStateFormRegister.class, SQL_QUERY_DEFINED_FORM, null);
        }
        return getDao().queryAsList(CommonStateFormRegister.class,
                String.format("%s %s", SQL_QUERY_DEFINED_FORM, SQL_QUERY_DEFINED_LIKE),
                new Object[] { namelike, namelike });
    }
    
    /**
     * 扫描并解析所有已注册的通用表单
     */
    public static void parseStateFormRegister() throws Exception {
         for (CommonStateFormRegister form : listStateFormRegister()) {
             if (StringUtils.equals(ContextUtil.getAppName(), form.getFormBackend())) {
                 parseStateFormRegister(form);
             }
         }
    }
    
    private static void parseStateFormRegister(CommonStateFormRegister form) throws Exception {
        if (form == null || !form.isEnabled()) {
            log.warn("通用流程单服务({})被禁用, 忽略。", form);
            return;
        }
        AbstractStateFormServiceWithBaseDao<?> serviceObject;
        try {
            serviceObject = ClassUtil.getSingltonInstance(form.getFormService(),
                    AbstractStateFormServiceWithBaseDao.class);
        } catch (ClassNotFoundException e) {
            log.warn("通用流程单服务类({})不存在, 忽略。", form);
            return;
        }
        if (!StringUtils.equals(form.getFormName(), serviceObject.getFormName())) {
            throw new MessageException(String.format("注册表单名称（%s）与类中定义的名称(%s/%s)不一致.", form.getFormName(),
                    form.getFormService(), serviceObject.getFormName()));
        }
        /**
         * 解析和校验通用流程事件定义，并转化为授权标识存储以供系统功能的配置使用。
         * 
         */
        log.info("开始解析并验证通用流程定义: {}", form);
        List<AuthorityEntity> authorities = new LinkedList<>();
        for (StateFormActionDefinition formAction : serviceObject.getExternalFormActionDefinition()) {
            Authority authority;
            AuthorityScope authorityScope;
            if ((authority = formAction.getAuthority()) == null || (authorityScope = serviceObject
                    .getPermissionService().getProvidedAuthorityScope(authority.value())) == null) {
                throw new MessageException(String.format("通用流程表单事件(%s/%s)未声明授权(Authority)信息",
                        formAction.getFormName(), formAction.getName()));
            }
            if (authorityScope.isSubsystem()) {
                if (AuthorityScopeIdNoopParser.class.equals(authority.parser())
                        && AuthorityScopeIdNoopMultipleParser.class.equals(authority.multipleParser())) {
                    throw new MessageException(
                            String.format("通用流程表单事件(%s/%s)未实现授权标的解析器（parser or multipleParser）",
                                    formAction.getFormName(), formAction.getName()));
                }
            }
            if (!AuthorityScopeIdNoopParser.class.equals(authority.parser())
                    && !AuthorityScopeIdNoopMultipleParser.class.equals(authority.multipleParser())) {
                throw new MessageException(String.format("通用流程表单事件(%s/%s)授权标的解析器（parser or multipleParser）只能使用其一",
                        formAction.getFormName(), formAction.getName()));
            }
            if (formAction.isCreateEvent() && (authority.multipleChoiceEnabled()
                    || !AuthorityScopeIdNoopMultipleCleaner.class.equals(authority.multipleCleaner()))) {
                throw new MessageException(
                        String.format("通用流程表单创建事件(%s/%s)中不支持 multipleChoiceEnabled 和 multipelCleaner 属性",
                                formAction.getFormName(), formAction.getName()));
            }
            authorities.add(new AuthorityEntity(authority.value(),
                    AbstractStateFormService.getFormEventKey(form.getFormName(), formAction.getName())));
        }
        Map<String, String> extraEvents;
        if ((extraEvents = serviceObject.getFormPredefinedExtraEvents()) != null) {
            for (Map.Entry<String, String> exEvent : extraEvents.entrySet()) {
                if (StringUtils.isAnyBlank(exEvent.getKey(), exEvent.getValue())) {
                    continue;
                }
                authorities.add(new AuthorityEntity("Predefined",
                        AbstractStateFormService.getFormEventKey(form.getFormName(), exEvent.getKey())));
            }
        }
        serviceObject.getPermissionService().saveAuthorityEntitisForConfig(form.getFormName(), authorities);
        log.info("完成解析并验证通用流程定义: {}", form);
        
        
        STATE_FORM_INSTANCES.put(form.getFormName(), new CommonStateFormInstance()
                            .setFormName(form.getFormName())
                            .setFormDisplay(form.getFormDisplay())
                            .setServiceInstance(serviceObject));
    }
    
    private static CommonStateFormInstance getStateFormInstance(String formName) throws Exception {
        if (!STATE_FORM_INSTANCES.containsKey(formName)) {
            parseStateFormRegister(getFormRegister(formName));
        }
        CommonStateFormInstance instance;
        if ((instance = STATE_FORM_INSTANCES.get(formName)) == null) {
            throw new StateFormNotDefinedException(formName);
        }
        return instance;
    }
    
    /**
     * 检查表单是否注册
     */
    public static boolean checkFormDefined(String formName) throws Exception {
        return getFormRegister(formName) != null;
    }
    
    /**
     * 获取表单完整定义
     */
    public static StateFormSimpleDefinition getFormDefinition(String formName) throws Exception {
        CommonStateFormInstance instance = getStateFormInstance(formName);
        AbstractStateFormServiceWithBaseDao<?> service = instance.getServiceInstance();
        List<StateFormActionDefinition> actions = new ArrayList<>();
        List<StateFormActionDefinition> otherActions = new ArrayList<>();
        for (StateFormActionDefinition action : service.getExternalFormActionDefinition()) {
            if (service.checkListAction(action.getName(), null)) {
                actions.add(action);
            } else {
                otherActions.add(action);
            }
        }
        return new StateFormSimpleDefinition()
                    .setStates(service.getStates())
                    .setName(instance.getFormName())
                    .setTitle(instance.getFormDisplay())
                    .setQueries(service.getFormQueryDefinition())
                    .setActions(actions)
                    .setOtherActions(otherActions)
                    .setFormClass(ClassUtil.classToJson(service.getSingleFormClass()).toString());
    }
    
    /**
     * 获取流程表单外部事件
     */
    public static StateFormActionDefinition getFormExtenalDefinition(String formName, String formEvent) throws Exception {
        CommonStateFormInstance instance = getStateFormInstance(formName);
        AbstractStateFormServiceWithBaseDao<?> service = instance.getServiceInstance();
        StateFormActionDefinition action;
        if ((action = service.getExternalFormActionDefinition(formEvent)) == null) {
            throw new StateFormActionNotFoundException(formName, formEvent);
        }
        return action;
    }
    
    /**
     SELECT
         view_name
     FROM 
         system_form_extraviews
     WHERE
         form_name = ?
     */
    @Multiline
    private final static String SQL_QUERY_FORM_EXTRA_VIEWS = "X";
    
    /**
     * 获取额外的关联表单清单
     */
    public static List<String> queryFormExtraViews(String formName) throws Exception {
        if (StringUtils.isBlank(formName)) {
            return Collections.emptyList();
        }
        return getDao().queryAsList(String.class, SQL_QUERY_FORM_EXTRA_VIEWS, new Object[] {formName});
    }
    
    /**
     * 保存额外的关联表单清单
     */
    public static void saveFormExtraViews(final String formName, final List<String> views) throws Exception {
        if (views == null) {
            return;
        }
        getDao().executeTransaction(new ResultSetProcessor() {
            @Override
            public void process(ResultSet result, Connection conn) throws Exception {
                getDao().executeUpdate(SqlQueryUtil.prepareDeleteQuery(
                        "system_form_extraviews", new ObjectMap()
                                .put("=form_name", formName)
                    ));
                for (String view : views) {
                    if (StringUtils.isBlank(view)) {
                        continue;
                    }
                    getDao().executeUpdate(SqlQueryUtil.prepareInsertQuery(
                        "system_form_extraviews", new ObjectMap()
                                .put("form_name", formName)
                                .put("view_name", view)
                    ));
                }
                
            }});
    }
    
    /**
     * 获取表单事件的请求数据类型
     */
    public static Class<AbstractStateForm> getActionFormTypeClass(String formName, String event) throws Exception {
        CommonStateFormInstance instance = getStateFormInstance(formName);
        return instance.getServiceInstance().getActionFormTypeClass(event);
    }
    
    /**
     * 获取表单事件的原始数据类型
     */
    public static Class<AbstractStateForm> getActionOriginTypeClass(String formName, String event) throws Exception {
        CommonStateFormInstance instance = getStateFormInstance(formName);
        return instance.getServiceInstance().getActionOriginTypeClass(event);
    }
    
    /**
     * 获取表单事件的操作响应类型
     */
    public static Class<?> getActionReturnTypeClass(String formName, String event) throws Exception {
        CommonStateFormInstance instance = getStateFormInstance(formName);
        return instance.getServiceInstance().getActionReturnTypeClass(event);
    }
    
    /**
     * 检索表单
     */
    public static PagedList<?> listForm(String formName, JsonElement data, String namedQuery) throws Exception {
        return getStateFormInstance(formName).getServiceInstance().listForm(namedQuery, data);
    }
    
    /**
     * 检索表单（包括分页用的总条数）
     */
    public static PagedListWithTotal<?> listFormWithTotal(String formName, JsonElement data, String namedQuery)
            throws Exception {
        return (PagedListWithTotal<?>) getStateFormInstance(formName).getServiceInstance().listFormWithTotal(namedQuery,
                data);
    }
    
    /**
     * 获取指定表单的详情
     */
    public static AbstractStateForm getForm(String formName, long formId) throws Exception {
        return getStateFormInstance(formName).getServiceInstance().getForm(formId);
    }
    
    /**
     * 获取指定查询定义的单条表单数据
     */
    public static AbstractStateForm getForm(String formName, String namedQuery, long formId) throws Exception {
        return getStateFormInstance(formName).getServiceInstance().getForm(namedQuery, formId);
    }
    
    /**
     * 获取指定表单当前可执行操作
     */
    public static StateFormWithAction<?> getFormActions(String formName, long formId) throws Exception {
        return getStateFormInstance(formName).getServiceInstance().getFormActions(formId);
    }
    
    /**
     * 获取指定表单当前可执行操作
     */
    public static Map<String,String> getFormActionNames(String formName, long formId) throws Exception {
        return getStateFormInstance(formName).getServiceInstance().getFormActionNames(formId);
    }
    
    /**
     * 获取指定表单的详情
     */
    public static StateFormWithAction<?> getFormNoActions(String formName, long formId) throws Exception {
        return getStateFormInstance(formName).getServiceInstance().getFormNoActions(formId);
    }
    
    /**
     * 获取指定表单的详情以及当前可执行操作
     */
    public static StateFormWithAction<?> getFormWithActions(String formName, long formId) throws Exception {
        return getStateFormInstance(formName).getServiceInstance().getFormWithActions(formId);
    }
    
    /**
     * 执行指定的表单操作
     */
    public static Object triggerAction(String formName, String formAction,
                                    AbstractStateForm formData, String message, Class<?> actionResult) throws Exception  {
        return getStateFormInstance(formName).getServiceInstance().triggerAction(formAction, formData, message, actionResult);
    }
    
    /**
     * 获取指定的操作准备数据。
     */
    public static AbstractStatePrepare triggerPrepare(String formName, String formAction, long formId, NameValuePair... params) throws Exception  {
        return getStateFormInstance(formName).getServiceInstance().triggerPrepare(formAction, formId, params);
    }
    
    /**
     * 动态可选项搜索
     */
    public static List<? extends FieldOption> queryFieldTypeOptions(String fieldTypeKey, String keyword,
            String formName, Long formId) throws Exception {
        return queryFieldTypeOptions(formName, fieldTypeKey,
                JsonUtil.toJson(new FilterBasicKeyword(keyword, formName, formId)));
    }
    
    /**
     * 动态可选项搜索
     */
    public static List<? extends FieldOption> queryFieldTypeOptions(String formName, String fieldTypeKey,
            String filterJson) throws Exception {
        FieldType fieldType = null;
        try {
            fieldType = ClassUtil.getSingltonInstance(fieldTypeKey, FieldType.class);
        } catch (Exception ex) {
            log.warn("The field type key ({}) not found", ex);
            return null;
        }
        FieldOptionsType fieldOptionsType = fieldType.getOptionsType();
        if (!FieldOptionsType.DYNAMIC.equals(fieldOptionsType)) {
            return null;
        }
        Class<? extends FieldOptionsFilter> filterClass = null;
        if ((filterClass = fieldType.getDynamicFilterFormClass()) == null) {
            filterClass = FilterBasicKeyword.class;
        }
        FieldOptionsFilter filterData = HttpMessageConverter.toInstance(filterClass,
                StringUtils.ifBlank(filterJson, "{}"));
        if (FilterBasicKeyword.class.isAssignableFrom(filterClass)) {
            ((FilterBasicKeyword)filterData).setFormJson(filterJson);
        }
        if (filterData != null && filterData instanceof FilterAbstractFrom) {
            ((FilterAbstractFrom) filterData).setFormName(formName);
        }
        return (List<? extends FieldOption>) fieldType.queryDynamicOptions(filterData);
    }
    
    /**
     * 根据动态可选项的值获取选项列表
     */
    public static List<? extends FieldOption> queryFieldTypeValues(String fieldTypeKey, String[] values)
            throws Exception {
        FieldType fieldType = null;
        try {
            fieldType = ClassUtil.getSingltonInstance(fieldTypeKey, FieldType.class);
        } catch (Exception ex) {
            log.warn("The field type key ({}) not found", ex);
            return null;
        }
        FieldOptionsType fieldOptionsType = fieldType.getOptionsType();
        if (FieldOptionsType.DYNAMIC.equals(fieldOptionsType)) {
            return (List<? extends FieldOption>) fieldType.queryDynamicValues((Object[]) values);
        }
        return null;
    }
    
    /**
     * 动态获取操作表单的定义
     */
    public static String queryFormTypeDefinition(String formTypeKey) throws Exception {
        Class<?> formClass = null;
        try {
            formClass = ClassUtil.loadClass(formTypeKey);
        } catch (Exception ex) {
            log.warn("The form type key ({}) not found", ex);
            return null;
        }
        FORM_DATA_CACHER.refresh(formTypeKey);
        return ClassUtil.classToJson(formClass).toString();
    }
    
    /**
     * 获取制定表单的状态可选值
     */
    public static List<? extends FieldOption> getStates(String formName) throws Exception  {
        CommonStateFormInstance instance = getStateFormInstance(formName);
        AbstractStateFormServiceWithBaseDao<?> service = instance.getServiceInstance();
        return service.getStates();
    }
    
    /**
     * 查询指定表单界面的排版数据
     */
    public static String getFieldCustomDefinition(String formClassPath) throws Exception {
        if (StringUtils.isBlank(formClassPath)) {
            throw new StateFormCustomFieldFormNotFoundException(formClassPath);
        }
        return getDao().queryAsObject(String.class,
                "SELECT form_attrs FROM system_form_viewattrs WHERE class_path = ?", 
                new Object[] {formClassPath});
    }
    
    /**
     * 预览指定表单界面的排版信息变更
     */
    public static String previewFieldCustomDefinition(String form, String definition) throws Exception {
        if (StringUtils.isBlank(form)) {
            throw new StateFormCustomFieldFormNotFoundException(form);
        }
        Class<?> type = null;
        try {
            type = ClassUtil.loadClass(form);
        } catch (Exception e) {
            throw new StateFormCustomFieldFormNotFoundException(form);
        }
        String origin = ClassUtil.AttributesProccessor.setContextPreviewAttributes(form, definition);
        try {
            return ClassUtil.classToJson(type).toString();
        } finally {
            ClassUtil.AttributesProccessor.resetContextPreviewAttributes(origin);
        }
    }
    
    /**
     * 保存指定表单界面的排版信息
     */
    public static void saveFieldCustomDefinition(String formClassPath, String definition) throws Exception {
         previewFieldCustomDefinition(formClassPath, definition);
         getDao().executeUpdate(SqlQueryUtil.prepareInsertQuery(
             "system_form_viewattrs", new ObjectMap()
                 .put("class_path", formClassPath)
                 .put("=form_attrs", definition)
         ));
         SimpleLogService.getInstance().createLog("state.form.custom.definition", "edit", 
                         formClassPath, null, getFieldCustomDefinition(formClassPath), definition);
    }
    
    /**
     * 添加或更新表单的自定义属性
     */
    public static void saveFieldCustomAttribute(@NonNull StateFormFieldCustomAttribute attr) throws Exception {
        if (StringUtils.isAnyBlank(attr.getName(), attr.getDisplay(), attr.getDescription())) {
            throw new MessageException("自定义的通用流程表单或字段属性的名称、显示及描述字段均不可为空值");
        }
        if (attr.getId() == null) {
            if (getDao().queryAsObject(Long.class, "SELECT id FROM name = ?", 
                                            new Object[] {attr.getName()}) != null) {
                throw new MessageException(String.format(
                    "自定义的通用流程表单或字段属性(%s)已存在",
                    CommonUtil.ifNull(attr.getName(), "")
                ));
            }
            getDao().executeUpdate(SqlQueryUtil.prepareInsertQuery(
                    "system_form_fieldattrs", new ObjectMap()
                        .put("name", attr.getName())
                        .put("display", attr.getDisplay())
                        .put("description", attr.getDescription())));
            return;
        }
        getDao().executeUpdate(SqlQueryUtil.prepareUpdateQuery(
                "system_form_fieldattrs", new ObjectMap()
                    .put("id", attr.getId())
                    .put("=name", attr.getName())
                    .put("=display", attr.getDisplay())
                    .put("=description", attr.getDescription())));
    }
    
    /**
     * 删除表单的自定义属性
     */
    public static void removeFieldCustomAttribute(long id) throws Exception {
        getDao().executeUpdate(SqlQueryUtil.prepareDeleteQuery(
                "system_form_fieldattrs", new ObjectMap().put("=id", id)));
    }
    
    /**
     * 列举表单的自定义属性
     */
    public static List<StateFormFieldCustomAttribute> listFieldCustomAttribute() throws Exception {
        return getDao().queryAsList(StateFormFieldCustomAttribute.class,
                "SELECT * FROM system_form_fieldattrs ORDER BY name DESC");
    }
    
    /**
     * 获取表单的流程定义数据
     */
    public static StateFlowDefinition parseFormFlowDefinition(String formName, boolean unchanged,
            Long formId) throws Exception {
        CommonStateFormInstance instance = getStateFormInstance(formName);
        AbstractStateForm currentForm = instance.getServiceInstance().getForm(formId);
        AbstractStateFormServiceWithBaseDao<?> service = instance.getServiceInstance();
        Map<StateFlowNodeData, Set<StateFlowNodeData>> flowData = service
                .parseFormFlowChartDefinition(unchanged, currentForm);
        List<StateFlowLinkData> flowLinkData = new ArrayList<>();
        for (Map.Entry<StateFlowNodeData, Set<StateFlowNodeData>> nodeEntry : flowData.entrySet()) {
            StateFlowNodeData parentNode = nodeEntry.getKey();
            Set<StateFlowNodeData> nodeChildren = nodeEntry.getValue();
            if (nodeChildren == null || nodeChildren.size() <= 0) {
                continue;
            }
            for (StateFlowNodeData c : nodeChildren) {
                flowLinkData.add(new StateFlowLinkData(parentNode.getKey(), c.getKey()));
            }
        }
        return new StateFlowDefinition(flowData.keySet(), flowLinkData);
    }
    
    /**
     * 检索表单的变更日志
     */
    public static List<SimpleLog> queryLogs(String formName, long formId, Long fromLogIndex)  throws Exception {
        return getStateFormInstance(formName).getServiceInstance().queryLogs(formId, fromLogIndex);
    }
    
    /**
     * 检索表单的讨论列表
     */
    public static List<SimpleLog> queryComments(String formName, long formId, Long fromLogIndex)  throws Exception {
        return getStateFormInstance(formName).getServiceInstance().queryComments(formId, fromLogIndex);
    }
    
}
