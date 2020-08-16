package com.socyno.stateform.field;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.adrianwalker.multilinestring.Multiline;

import com.socyno.base.bscmixutil.StringUtils;
import com.socyno.base.bscsqlutil.AbstractDao;
import com.socyno.webbsc.service.jdbc.TenantSpecialDataSource;

public abstract class AbstractFieldDynamicStandard<F extends FilterBasicKeyword> extends FieldAbstractKeyword<F> {
    
    private static AbstractDao getDao() {
        return TenantSpecialDataSource.getMain();
    }
    
    /**
     * SELECT
     *     class_path,
     *     category,
     *     option_group,
     *     option_value,
     *     option_display
     * FROM
     *     system_field_option
     * WHERE
     */
    @Multiline
    private final static String SQL_QUERY_CURRENT_OPTIONS = "x";
    
    /**
     * (
     *     option_group LIKE CONCAT('%', ?, '%')
     *   OR 
     *     option_value LIKE CONCAT('%', ?, '%')
     *   OR
     *     option_display LIKE CONCAT('%', ?, '%')
     * )
     */
    @Multiline
    private final static String SQL_QUERY_OPTIONS_LIKE_TMPL = "x";
    
    public boolean categoryRequired() {
        return false;
    }
    
    public String getCategoryFieldValue(Object form) {
        return "";
    }
    
    public String getCategoryFieldValue(F filter) {
        return "";
    }
    
    @Override
    public List<OptionDynamicStandard> queryDynamicOptions(F filter) throws Exception {
        String category = "";
        if (categoryRequired()) {
            if (StringUtils.isBlank(category = getCategoryFieldValue(filter))) {
                return Collections.emptyList();
            }
        }
        StringBuilder sql = new StringBuilder(SQL_QUERY_CURRENT_OPTIONS)
            .append("class_path = ? AND category = ? AND disabled = 0");
        List<Object> args = new ArrayList<>();
        args.add(getClass().getName());
        args.add(category);
        if (filter != null && StringUtils.isNotBlank(filter.getKeyword())) {
            args.add(filter.getKeyword());
            args.add(filter.getKeyword());
            args.add(filter.getKeyword());
            sql.append(" AND ").append(SQL_QUERY_OPTIONS_LIKE_TMPL);
        }
        return getDao().queryAsList(OptionDynamicStandard.class, sql.toString(), args.toArray());
    }
    
    public static List<OptionDynamicStandard> queryDynamicValues(Collection<OptionDynamicStandard> options) throws Exception {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        return queryDynamicValues(options.toArray(new OptionDynamicStandard[0]));
    }
    
    public static List<OptionDynamicStandard> queryDynamicValues(OptionDynamicStandard ...options) throws Exception {
        if (options == null || options.length <= 0) {
            return Collections.emptyList();
        }
        List<Object> args = new LinkedList<>();
        for (OptionDynamicStandard v : options) {
            if (v == null || StringUtils.isBlank(v.getClassPath())) {
                continue;
            }
            args.add(v.getClassPath());
            args.add(v.getCategory());
            args.add(v.getOptionValue());
        }
        return getDao().queryAsList(OptionDynamicStandard.class,
                String.format("%s %s", SQL_QUERY_CURRENT_OPTIONS,
                        StringUtils.join("(class_path = ? AND category = ? AND option_value = ?)",
                                args.size()/3, " OR ")),
                args.toArray());
    }
}