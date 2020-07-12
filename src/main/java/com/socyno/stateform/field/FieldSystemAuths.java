package com.socyno.stateform.field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.adrianwalker.multilinestring.Multiline;

import com.github.reinert.jjschema.SchemaIgnore;
import com.socyno.base.bscmixutil.StringUtils;
import com.socyno.base.bscsqlutil.AbstractDao;
import com.socyno.webbsc.authority.AuthorityEntity;
import com.socyno.webbsc.ctxutil.ContextUtil;

import lombok.Getter;

public class FieldSystemAuths extends FieldAbstractKeyword<FilterBasicKeyword> {
    
    @Getter
    private static final FieldSystemAuths instance = new FieldSystemAuths();
    
    @Override
    @SchemaIgnore
    public FieldOptionsType getOptionsType() {
        return FieldOptionsType.DYNAMIC;
    }
    
    @SchemaIgnore
    private static AbstractDao getDao() {
        return ContextUtil.getBaseDataSource();
    }
    
    /**
     * SELECT
     *     a.auth,
     *     a.scope,
     *     a.app_name
     * FROM
     *     system_auth a
     * WHERE
     *     a.deleted_at IS NULL
     *     %s
     * ORDER BY
     *    a.scope,
     *    a.auth     
     */
    @Multiline
    private final static String SQL_QUERY_AUTH_OPTIONS = "X";
    
    @Override
    public List<AuthorityEntity> queryDynamicOptions(FilterBasicKeyword filter) throws Exception {
        String placeHolder = "";
        List<String> sqlArgs = new ArrayList<>();
        if (StringUtils.isNotBlank(filter.getKeyword())) {
            sqlArgs.add(filter.getKeyword());
            placeHolder = " AND a.auth LIKE CONCAT('%', ?, '%') ";
        }
        return getDao().queryAsList(AuthorityEntity.class, String.format(SQL_QUERY_AUTH_OPTIONS, placeHolder),
                sqlArgs.toArray());
    }
    
    @Override
    public List<AuthorityEntity> queryDynamicValues(Object[] values) throws Exception {
        if (values == null || values.length <= 0) {
            return Collections.emptyList();
        }
        
        String placeHolder = String.format(" AND a.auth IN (%s) ", StringUtils.join("?", values.length, ","));
        return getDao().queryAsList(AuthorityEntity.class, String.format(SQL_QUERY_AUTH_OPTIONS, placeHolder), values);
    }
}
