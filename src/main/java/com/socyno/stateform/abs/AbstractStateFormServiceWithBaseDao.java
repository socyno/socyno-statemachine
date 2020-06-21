package com.socyno.stateform.abs;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.NonNull;

import com.github.reinert.jjschema.v1.FieldOption;
import com.socyno.base.bscmixutil.StringUtils;
import com.socyno.base.bscmodel.ObjectMap;
import com.socyno.base.bscmodel.PagedList;
import com.socyno.base.bscmodel.PagedListWithTotal;
import com.socyno.base.bscservice.AbstractLockService;
import com.socyno.base.bscservice.AbstractLogService;
import com.socyno.base.bscservice.AbstractNotifyService;
import com.socyno.base.bscservice.AbstractTodoService;
import com.socyno.base.bscsqlutil.AbstractDao;
import com.socyno.base.bscsqlutil.AbstractDao.ResultSetProcessor;
import com.socyno.base.bscsqlutil.AbstractSqlStatement;
import com.socyno.base.bscsqlutil.SqlQueryUtil;
import com.socyno.base.bsctmplutil.EnjoyUtil;
import com.socyno.stateform.exec.StateFormInvalidStatesException;
import com.socyno.stateform.exec.StateFormNamedQueryNotFoundException;
import com.socyno.stateform.service.SimpleLockService;
import com.socyno.stateform.service.SimpleLogService;
import com.socyno.stateform.util.StateFormNamedQuery;
import com.socyno.stateform.util.StateFormQueryBaseEnum;
import com.socyno.stateform.util.StateFormRevision;
import com.socyno.webbsc.ctxutil.HttpMessageConverter;

public abstract class AbstractStateFormServiceWithBaseDao<F extends AbstractStateForm> extends AbstractStateFormService<F> {
    
    protected abstract String getFormTable();
    
    protected abstract AbstractDao getFormBaseDao();
    
    @Override
    protected AbstractLogService getLogService() {
    	return SimpleLogService.getInstance();
    }
    
    @Override
    protected AbstractLockService getLockService() {
    	return SimpleLockService.getInstance();
    }
    
    @Override
    protected AbstractNotifyService<?> getNotifyService() {
        return null;
    }
    
    @Override
    protected AbstractTodoService getTodoService() {
        return null;
    }
    
    
    /**
     * 获取最终查询的字段和属性的映射关系
     */
    protected Map<String, String> getExtraFieldMapper(Class<?> resultClass, Map<String, String> fieldMapper) {
        return fieldMapper;
    }
    
    protected abstract void fillExtraFormFields(Collection<? extends F> forms) throws Exception;
    
    @SafeVarargs
    protected final void fillExtraFormFields(F ...forms) throws Exception {
        if (forms == null) {
            return;
        }
        fillExtraFormFields(Arrays.asList(forms));
    }
    
    public String getFormIdField() {
        return "id";
    }
    
    public String getFormStateField() {
        return "state_form_status";
    }
    
    public String getFormRevisionField() {
        return "state_form_revision";
    }
    
    @Override
    protected void saveStateRevision(long id, String state) throws Exception {
        saveStateRevision(id, state, new String[0]);
    }
    
    protected void saveStateRevision(long id, String state, String ...stateWhens) throws Exception {
        saveStateRevision(id, state, null, stateWhens);
    }
    
    protected void saveStateRevision(long id, String state, ObjectMap customQueries, String ...stateWhens) throws Exception {
        ObjectMap query = new ObjectMap()
            .put("=" + getFormIdField(), id)
            .put("#" + getFormRevisionField(), getFormRevisionField() + " + 1");
        if (StringUtils.isNotBlank(state)) {
            boolean found = false;
            List<? extends FieldOption> stateOptions = getStates();
            if ((stateOptions = getStates()) == null || stateOptions.isEmpty()) {
                throw new StateFormInvalidStatesException(getFormName(), state);
            }
            for (FieldOption option : stateOptions) {
                if (option == null ) {
                    continue;
                }
                if (StringUtils.equals(state, option.getOptionValue())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new StateFormInvalidStatesException(getFormName(), state);
            }
            query.put(getFormStateField(), state);
        }
        if (customQueries != null) {
            query.addAll(customQueries);
        }
        if (stateWhens != null && stateWhens.length > 0) {
            query.put("=" + getFormStateField(), stateWhens);
        }
        getFormBaseDao().executeUpdate(SqlQueryUtil.prepareUpdateQuery(
            getFormTable(), query
        ));
    }
    
    @Override
    protected Map<Long, StateFormRevision> loadStateRevision(Long[] formIds) throws Exception {
        List<StateFormRevision> data = null;
        if (formIds != null && formIds.length > 0) {
            data = getFormBaseDao().queryAsList(StateFormRevision.class, String.format(
                    "SELECT DISTINCT %s id, %s state_form_status , %s state_form_revision FROM %s WHERE %s IN(%s)",
                    getFormIdField(),
                    getFormStateField(),
                    getFormRevisionField(),
                    getFormTable(),
                    getFormIdField(), StringUtils.join("?", formIds.length, ", ")
                ), formIds);
        }
        if (data == null || data.size() <= 0) {
            return Collections.emptyMap();
        }
        Map<Long, StateFormRevision> result = new HashMap<>();
        for (StateFormRevision l : data) {
            result.put(l.getId(), l);
        }
        return result;
    }

    protected List<F> queryFormWithStateRevision(String sql) throws Exception {
        return queryFormWithStateRevision(sql, null, null);
    }
    
    protected List<F> queryFormWithStateRevision(String sql, Object[] args) throws Exception {
        return queryFormWithStateRevision(sql, args, null);
    }
    
    protected List<F> queryFormWithStateRevision(String sql, Map<String, String> mapper) throws Exception {
        return queryFormWithStateRevision(sql, null, mapper);
    }
    
    protected List<F> queryFormWithStateRevision(String sql, Object[] args, Map<String, String> mapper) throws Exception {
        return queryFormWithStateRevision(getFormClass(), sql, args, mapper);
    }
    
    protected <T> List<T> queryFormWithStateRevision(Class<T> entityClass, String sql) throws Exception {
        return queryFormWithStateRevision(entityClass, sql, null, null);
    }
    
    protected <T> List<T> queryFormWithStateRevision(Class<T> entityClass, String sql, Map<String, String> mapper) throws Exception {
        return queryFormWithStateRevision(entityClass, sql, null, mapper);
    }
    
    protected <T> List<T> queryFormWithStateRevision(Class<T> entityClass, String sql, Object[] args) throws Exception {
        return queryFormWithStateRevision(entityClass, sql, args, null);
    }
    
    protected <T> List<T> queryFormWithStateRevision(@NonNull Class<T> entityClass, String sql, Object[] args, Map<String, String> mapper) throws Exception {
        Map<String, String> mappAll = new HashMap<String, String>();
        if (mapper != null) {
            mappAll.putAll(mapper);
        }
        mappAll.put(getFormIdField(), "id");
        mappAll.put(getFormStateField(), "state");
        mappAll.put(getFormRevisionField(), "revision");
        return getFormBaseDao().queryAsList(entityClass, sql, args, mappAll);
    }
    
    /**
     * 根据预定义的查询名称和条件数据，获取表单结果集的总条目数
     * @param namedQuery 查询名称
     * @param condition  查询条件数据
     * @return
     * @throws Exception
     */
    public long getListFormTotal(@NonNull String namedQuery, @NonNull Object condition) throws Exception {
        return getListFormTotal(getFormNamedQuery(namedQuery), condition);
    }
    
    public long getListFormTotal(@NonNull StateFormNamedQuery<?> namedQuery, @NonNull Object condition) throws Exception {
        return getListFormTotal(HttpMessageConverter.toInstance(namedQuery.getQueryClass(), condition));
    }
    
    public long getListFormTotal(@NonNull AbstractStateFormQuery query) throws Exception {
        AbstractSqlStatement sql = query.prepareSqlTotal();
        return getFormBaseDao().queryAsObject(Long.class, sql.getSql(), sql.getValues());
    }
    
    /**
     * 将 Query 封装为抽象层的 filter 对象
     */
    protected <T extends F> AbstractStateFormFilter<T> queryToFilter(final @NonNull Class<T> clazz, final @NonNull AbstractStateFormQuery query) {
        return new AbstractStateFormFilter<T>() {
            @Override
            public List<T> apply(Class<T> clazz) throws Exception {
                AbstractSqlStatement sql = query.prepareSqlQuery();
                List<T> list = queryFormWithStateRevision(clazz, sql.getSql(), sql.getValues(), getExtraFieldMapper(clazz, query.getFieldMapper()));
                fillExtraFormFields(list);
                return list;
            }
        };
    }
    
    /**
     * 根据预定义的查询名称和条件数据，获取表单结果集
     * @param namedQuery 查询名称
     * @param condition  查询条件数据
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public PagedList<? extends F> listForm(@NonNull StateFormQueryBaseEnum namedQuery, @NonNull Object condition) throws Exception {
        StateFormNamedQuery<?> query;
        if ((query = namedQuery.getNamedQuery()) == null || !getFormClass().isAssignableFrom(query.getResultClass())) {
            throw new StateFormNamedQueryNotFoundException(getFormName(), namedQuery.name());
        }
        return listForm((StateFormNamedQuery<? extends F>)query, condition);
    }
    
    public PagedList<? extends F> listForm(String namedQuery, @NonNull Object condition) throws Exception {
        StateFormNamedQuery<? extends F> query;
        if ((query = getFormNamedQuery(namedQuery)) == null) {
            throw new StateFormNamedQueryNotFoundException(getFormName(), namedQuery);
        }
        return listForm(query, condition);
    }
    
    public PagedList<? extends F> listForm(@NonNull StateFormNamedQuery<? extends F> namedQuery, @NonNull Object condition) throws Exception {
        return listForm(namedQuery.getResultClass(), (AbstractStateFormQuery)HttpMessageConverter.toInstance(namedQuery.getQueryClass(), condition));
    }
    
    /**
     * 根据预定义的查询名称和条件数据，获取表单结果集(同时返回结果集总条数)
     * @param namedQuery 查询名称
     * @param condition  查询条件数据
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public PagedListWithTotal<? extends F> listFormWithTotal(@NonNull StateFormQueryBaseEnum namedQuery, @NonNull Object condition) throws Exception {
        StateFormNamedQuery<?> query;
        if ((query = namedQuery.getNamedQuery()) == null || !getFormClass().isAssignableFrom(query.getResultClass())) {
            throw new StateFormNamedQueryNotFoundException(getFormName(), namedQuery.name());
        }
        return listFormWithTotal((StateFormNamedQuery<? extends F>)query, condition);
    }
    
    public PagedListWithTotal<? extends F> listFormWithTotal(String namedQuery, @NonNull Object condition) throws Exception {
        StateFormNamedQuery<? extends F> query;
        if ((query = getFormNamedQuery(namedQuery)) == null) {
            throw new StateFormNamedQueryNotFoundException(getFormName(), namedQuery);
        }
        return listFormWithTotal(query, condition);
    }
    
    public PagedListWithTotal<? extends F> listFormWithTotal(@NonNull StateFormNamedQuery<? extends F> namedQuery, @NonNull Object condition) throws Exception {
        return listFormWithTotal(namedQuery.getResultClass(), (AbstractStateFormQuery)HttpMessageConverter.toInstance(namedQuery.getQueryClass(), condition));
    }
    
    @Override
    public <T extends F> List<T> listForm(@NonNull Class<T> clazz, @NonNull AbstractStateFormFilter<T> filter) throws Exception {
        return filter.apply(clazz);
    }
    
    public <T extends F> PagedList<T> listForm(@NonNull Class<T> resultClazz, @NonNull AbstractStateFormQuery query)
                        throws Exception {
        List<T> resutSet = listForm(resultClazz, queryToFilter(resultClazz, query));
        return new PagedList<T>().setPage(query.getPage()).setLimit(query.getLimit())
                            .setList(resutSet);
    }
    
    public <T extends F> PagedListWithTotal<T> listFormWithTotal(@NonNull Class<T> resultClazz,
                        @NonNull AbstractStateFormQuery query) throws Exception {
        List<T> resutSet = listForm(resultClazz, queryToFilter(resultClazz, query));
        long total = (resutSet == null || resutSet.size() <= 0 || resutSet.size() >= query.getLimit())
                                    ? getListFormTotal(query) : (query.getOffset() + resutSet.size());
       return new PagedListWithTotal<T>().setPage(query.getPage()).setLimit(query.getLimit()).setTotal(total)
                               .setList(resutSet);
    }
    
    /**
     * 获取表单详情数据的 SQL 模板。
     * 格式适用 jFinal 的 enjoy 模板，可用参数分别为 :
     * <pre>
     *       formTable   : 表名称
     *       formIdField : ID 字段名
     *       formIdValue : ID 字段值
     * 默认模板语句 ： SELECT * FROM #(formTable) WHERE #(formIdField)=#(formIdValue)
     * </pre>
     */
    protected String loadFormSqlTmpl() {
        return "SELECT * FROM #(formTable) WHERE #(formIdField)=#(formIdValue)";
    }
    
    @Override
    protected <T extends F> T loadFormNoStateRevision(Class<T> clazz, long formId) throws Exception {
        Map<String, String> mapper = getExtraFieldMapper(clazz, null);
        if (mapper == null ) {
            mapper = new HashMap<>();
        }
        mapper.put(getFormIdField(), "id");
        mapper.put(getFormStateField(), "state");
        mapper.put(getFormRevisionField(), "revision");
        return getFormBaseDao().queryAsObject(clazz, EnjoyUtil.format(
                loadFormSqlTmpl(), new ObjectMap()
                        .put("formTable", getFormTable())
                        .put("formIdField", getFormIdField())
                        .put("formIdValue", formId)
                        .asMap()),
                new Object[0],
                mapper
        );
    }
    
    @Override
    public <T extends F> T getForm(Class<T> clazz, long formId) throws Exception {
        T form = loadFormNoStateRevision(clazz, formId);
        fillExtraFormFields(form);
        return form;
    }
    
    @Override
    public F getForm(long formId) throws Exception {
        return getForm(getFormClass(), formId);
    }
    
    private <T> T superTriggerActionWithoutLock(String event, AbstractStateForm form, String message, Class<T> clazz) throws Exception  {
        return super.triggerActionWithoutLock(event, form, message, clazz);
    }
    
    @SuppressWarnings("unchecked")
    @Override
    protected <T> T triggerActionWithoutLock(final String event, final AbstractStateForm form, final String message, final Class<T> clazz) throws Exception {
        final Object[] result = new Object[] {null};
        getFormBaseDao().executeTransaction(new ResultSetProcessor() {
            @Override
            public void process(ResultSet r, Connection c) throws Exception {
                result[0] = superTriggerActionWithoutLock(event, form, message, clazz);
            }
        });
        return (T)result[0];
    }
}
