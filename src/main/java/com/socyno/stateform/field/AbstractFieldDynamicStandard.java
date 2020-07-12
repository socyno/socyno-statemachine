package com.socyno.stateform.field;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.adrianwalker.multilinestring.Multiline;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.v1.FieldType;
import com.socyno.base.bscmixutil.ClassUtil;
import com.socyno.base.bscmixutil.StringUtils;
import com.socyno.base.bscsqlutil.AbstractDao;
import com.socyno.stateform.abs.AbstractStateForm;
import com.socyno.webbsc.service.jdbc.TenantSpecialDataSource;

import lombok.Getter;
import lombok.NonNull;

public abstract class AbstractFieldDynamicStandard<F extends FilterBasicKeyword> extends FieldAbstractKeyword<F> {
    
    private static AbstractDao getDao() {
        return TenantSpecialDataSource.getMain();
    }
    
    protected boolean searchByCategory() {
        return false;
    }
    
    protected String getFormCategory(AbstractStateForm form) {
        return "";
    }
    
    protected String getFormCategory(FilterBasicKeyword filter) {
        return "";
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
    
    public List<OptionDynamicStandard> queryDynamicOptions(FilterBasicKeyword filter) throws Exception {
        /**
         * 当支持 category 做二级分类，当却未能获取到二级分类的值，则不提供可选数据
         */
        String category = "";
        if (searchByCategory() && StringUtils.isBlank(category = getFormCategory(filter))) {
            Collections.emptyList();
        }
        List<Object> args = new LinkedList<>();
        args.add(getClass().getName());
        args.add(category);
        StringBuilder sql = new StringBuilder(SQL_QUERY_CURRENT_OPTIONS)
                                .append(" class_path = ? AND category = ?");
        if (filter != null && StringUtils.isNotBlank(filter.getKeyword())) {
            args.add(filter.getKeyword());
            args.add(filter.getKeyword());
            args.add(filter.getKeyword());
            sql.append(" AND ").append(SQL_QUERY_OPTIONS_LIKE_TMPL);
            
        }
        return getDao().queryAsList(OptionDynamicStandard.class, sql.toString(), args.toArray());
    }
    
    private static List<OptionDynamicStandard> queryDynamicValues(Collection<OptionDynamicStandard> options) throws Exception {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        return queryDynamicValues(options.toArray(new OptionDynamicStandard[0]));
    }
    
    private static List<OptionDynamicStandard> queryDynamicValues(OptionDynamicStandard ...options) throws Exception {
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
                String.format("%s %s", SQL_QUERY_CURRENT_OPTIONS, StringUtils
                        .join("(class_path = ? AND category = ? AND option_value = ?)", args.size() / 3, " OR ")),
                args.toArray());
    }
    
    @Getter
    private static class FormField {
        private final AbstractStateForm form;
        private final FieldMethod field;
        
        private FormField(AbstractStateForm form, FieldMethod field) {
            this.form = form;
            this.field = field;
        }
    }
    
    @Getter
    private static class FieldMethod {
        private final Field field;
        private final Method setter;
        
        private FieldMethod(Field field, Method setter) {
            this.field = field;
            this.setter = setter;
        }
        
        private FieldMethod(Field field) {
            this.field = field;
            this.setter = null;
        }
    }
    
    private final static Map<Class<?>, List<FieldMethod>> ATTRIBUTES_FIELD_PARSED = new ConcurrentHashMap<>(); 
    private static List<FieldMethod> parseOptionStandardFeild(@NonNull Class<?> clazz, List<FieldMethod> collector)
            throws IllegalAccessException {
        if (Object.class.equals(clazz)) {
            return null;
        }
        List<FieldMethod> fields;
        if ((fields = ATTRIBUTES_FIELD_PARSED.get(clazz)) == null) {
            if (collector == null) {
                collector = new LinkedList<>();
            }
            Attributes annotation;
            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) 
                        || (annotation = field.getAnnotation(Attributes.class)) == null
                        || !AbstractFieldDynamicStandard.class.isAssignableFrom(annotation.type())) {
                    continue;
                }
                try {
                    Method method = clazz.getMethod(String.format("get%s", StringUtils.capitalize(field.getName())),
                            OptionDynamicStandard.class);
                    if (!Modifier.isStatic(method.getModifiers())) {
                        collector.add(new FieldMethod(field, method));
                        continue;
                    }
                } catch (NoSuchMethodException e) {
                    // NOTHING TODO
                }
                if (OptionDynamicStandard.class.equals(field.getType())) {
                    collector.add(new FieldMethod(field));
                }
            }
            Class<?> superClazz;
            if ((superClazz = clazz.getSuperclass()) != null) {
                parseOptionStandardFeild(superClazz, collector);
            }
            fields = collector;
            ATTRIBUTES_FIELD_PARSED.put(clazz, collector);
        }
        return fields;
    }
    
    private static List<FieldMethod> parseOptionStandardFeild(@NonNull Class<?> clazz) throws IllegalAccessException {
        return parseOptionStandardFeild(clazz, null);
    }
    
    public static void replaceOptionStandardField(AbstractStateForm... forms) throws Exception {
        if (forms == null || forms.length <= 0) {
            return;
        }
        List<FieldMethod> fields;
        List<FormField> optionFields;
        OptionDynamicStandard fieldValue;
        Class<? extends FieldType> fieldOptionType;
        AbstractFieldDynamicStandard<?> fieldOptionInstance;
        Map<OptionDynamicStandard, List<FormField>> allOptionFields = new HashMap<>();
        for (AbstractStateForm form : forms) {
            if (form == null || (fields = parseOptionStandardFeild(form.getClass())) == null || fields.isEmpty()) {
                continue;
            }
            for (FieldMethod field : fields) {
                field.getField().setAccessible(true);
                if ((fieldValue = (OptionDynamicStandard) field.getField().get(form)) == null) {
                    continue;
                }
                fieldOptionType = field.getField().getAnnotation(Attributes.class).type();
                fieldOptionInstance = (AbstractFieldDynamicStandard<?>)ClassUtil.getSingltonInstance(fieldOptionType);
                /**
                 * 当支持 category 做二级分类，当却未能获取到二级分类的值，则跳过数据补全
                 */
                String category = "";
                if (fieldOptionInstance.searchByCategory() && StringUtils.isBlank(category = fieldOptionInstance.getFormCategory(form))) {
                    continue;
                }
                OptionDynamicStandard option = new OptionDynamicStandard(fieldValue.getOptionValue())
                        .setClassPath(fieldOptionType.getName())
                        .setCategory(category);
                if ((optionFields = allOptionFields.get(option)) == null) {
                    allOptionFields.put(option, (optionFields = new LinkedList<>()));
                }
                optionFields.add(new FormField(form, field));
            }
        }
        List<OptionDynamicStandard> backendOptions;
        if ((backendOptions = queryDynamicValues(allOptionFields.keySet())) == null || backendOptions.isEmpty()) {
            return;
        }
        for (OptionDynamicStandard option : backendOptions) {
            if ((optionFields = allOptionFields.get(option)) == null) {
                continue;
            }
            for (FormField field : optionFields) {
                if (field.getField().getSetter() != null) {
                    field.getField().getSetter().invoke(field.getForm(), option);
                    continue;
                }
                field.getField().getField().set(field.getForm(), option);
            }
        }
    }
}
