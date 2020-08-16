package com.socyno.stateform.sugger;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.github.reinert.jjschema.Attributes;

import lombok.Getter;
import lombok.NonNull;

public abstract class AbstractStateFormSugger {
    public static abstract class OptionClass<T> {
        
        protected abstract Class<T> getType();
        
        @SuppressWarnings("unchecked")
        final Collection<T> queryObjectOptions(Collection<?> values) throws Exception {
            if (values == null) {
                return null;
            }
            Set<T> nonNulls = new HashSet<>();
            for (Object v : values) {
                if (v == null) {
                    continue;
                }
                nonNulls.add((T)v);
            }
            return queryOptions(nonNulls);
        }
        
        @SuppressWarnings({ "unchecked" })
        final void fillOriginValue(Object form, Object origin, Definition.OptionWrapper wrapper, Field field, Attributes fieldAttrs) throws Exception {
            fillOriginOption(form, (T)origin, wrapper, field, fieldAttrs);
        }
        
        protected Object parseOriginValue(Object form, Field field, Definition.OptionWrapper wrapper, Attributes fieldAttrs) throws Exception {
            field.setAccessible(true);
            return field.get(form);
        }
        
        protected abstract void fillOriginOption(Object form, T fieldValue, Definition.OptionWrapper wrapper, Field field, Attributes fieldAttrs) throws Exception;
        
        protected abstract Collection<T> queryOptions(Collection<T> values) throws Exception;
        
        @SuppressWarnings("unchecked")
        protected void setMatchedValues(Object form, Field field, Object[] flatValues, Map<Object, Object> mappedFinalValues, Definition.OptionWrapper wrapper, Attributes fieldAttrs) throws Exception {
            Object fetched;
            List<T> matched = new ArrayList<>();
            for (Object v : flatValues) {
                if (v == null || (fetched = mappedFinalValues.get(v)) == null) {
                    continue;
                }
                matched.add((T)fetched);
            }
            field.setAccessible(true);
            field.set(form, wrapper.fromFieldFlatValues(getType(), matched));
        }
    }
    
    public static abstract class Definition {
        
        protected abstract Class<?> getTypeClass();
        
        protected abstract OptionClass<?> getOptionClass();
        
        final Object[] parseFeildFlatValues(@NonNull Object form, @NonNull Field field,
                @NonNull OptionWrapper wrapper, Attributes attributes) throws Exception {
            Object[] rawValues;
            Object fieldValue = getOptionClass().parseOriginValue(form, field, wrapper, attributes);
            if (fieldValue == null || ((rawValues = wrapper.parseFeildFlatValues(fieldValue)) == null)) {
                return null;
            }
            for (Object v : rawValues) {
                if (v == null) {
                    continue;
                }
                getOptionClass().fillOriginValue(form, v, wrapper, field, attributes);
            }
            return rawValues;
        }
        
        final FieldMatched createFieldMatched(@NonNull Field field) {
            Attributes fieldAttr = null;
            if (getTypeClass() != null) {
                if ((fieldAttr = field.getAnnotation(Attributes.class)) == null
                       || !getTypeClass().isAssignableFrom(fieldAttr.type())) {
                    return null;
                }
            }
            Class<?> fieldType = field.getType();
            Class<?> optionType = getOptionClass().getType();
            if (fieldType.equals(optionType)) {
                return new FieldMatched(this, field, OptionWrapper.None, fieldAttr);
            }
            if (fieldType.isArray() && fieldType.getComponentType().equals(optionType)) {
                return new FieldMatched(this, field, OptionWrapper.Array, fieldAttr);
            }
            if (List.class.equals(fieldType) && ((ParameterizedType)field.getGenericType())
                    .getActualTypeArguments()[0].equals(optionType)) {
                return new FieldMatched(this, field, OptionWrapper.List, fieldAttr);
            }
            if (Set.class.equals(fieldType) && ((ParameterizedType)field.getGenericType())
                    .getActualTypeArguments()[0].equals(optionType)) {
                return new FieldMatched(this, field, OptionWrapper.Set, fieldAttr);
            }
            if (Collection.class.equals(fieldType) && ((ParameterizedType)field.getGenericType())
                    .getActualTypeArguments()[0].equals(optionType)) {
                return new FieldMatched(this, field, OptionWrapper.Collection, fieldAttr);
            }
            return null;
        }
        
        public enum OptionWrapper {
            None {
                @Override
                public Object[] parseFeildFlatValues(Object fieldValue) {
                    if (fieldValue == null) {
                        return null;
                    }
                    return new Object[] {fieldValue};
                }

                @Override
                public <T> Object fromFieldFlatValues(Class<T> clazz, Collection<T> flatValues) {
                    if (flatValues == null) {
                        return null;
                    }
                    for (T v : flatValues) {
                        return v;
                    }
                    return null;
                }
            },
            List {
                @Override
                public Object[] parseFeildFlatValues(Object fieldValue) {
                    if (fieldValue == null) {
                        return null;
                    }
                    return ((Collection<?>)fieldValue).toArray();
                }
                
                @Override
                public <T> Object fromFieldFlatValues(Class<T> clazz, Collection<T> flatValues) {
                    if (flatValues == null) {
                        return null;
                    }
                    return new ArrayList<T>(flatValues);
                }
            },
            Set {
                @Override
                public Object[] parseFeildFlatValues(Object fieldValue) {
                    if (fieldValue == null) {
                        return null;
                    }
                    return ((Collection<?>)fieldValue).toArray();
                }
                
                @Override
                public <T> Object fromFieldFlatValues(Class<T> clazz, Collection<T> flatValues) {
                    if (flatValues == null) {
                        return null;
                    }
                    return new HashSet<T>(flatValues);
                }
            },
            Array {
                @Override
                public Object[] parseFeildFlatValues(Object fieldValue) {
                    if (fieldValue == null) {
                        return null;
                    }
                    return (Object[])fieldValue;
                }
                
                @Override
                public <T> Object fromFieldFlatValues(Class<T> clazz, Collection<T> flatValues) throws Exception {
                    if (flatValues == null || flatValues.isEmpty()) {
                        return null;
                    }
                    int i = 0;
                    Object newVal = java.lang.reflect.Array.newInstance(clazz, flatValues.size());
                    for (T v: flatValues) {
                        java.lang.reflect.Array.set(newVal, i++, v);
                    }
                    return newVal;
                }
            },
            Collection {
                @Override
                public Object[] parseFeildFlatValues(Object fieldValue) {
                    if (fieldValue == null) {
                        return null;
                    }
                    return ((Collection<?>)fieldValue).toArray();
                }
                
                @Override
                public <T> Object fromFieldFlatValues(Class<T> clazz, Collection<T> flatValues) {
                    return flatValues;
                }
            };
            public abstract <T> Object fromFieldFlatValues(Class<T> clazz, Collection<T> flatValues) throws Exception;
            public abstract Object[] parseFeildFlatValues(Object fieldValue) throws Exception;
        }
    }
    
    @Getter
    static class FieldMatched {
        
        private final Definition definition;
        
        private final Field field;
        
        private final Definition.OptionWrapper wrapper;
        
        private final Attributes attributes;
        
        FieldMatched(Definition definition, Field field, Definition.OptionWrapper wrapper, Attributes attributes) {
            this.field = field;
            this.wrapper = wrapper;
            this.definition = definition;
            this.attributes = attributes;
        }
        
        private Object[] getFeildFlatValues(Object form) throws Exception {
            if (form == null) {
                return null;
            }
            return definition.parseFeildFlatValues(form, field, wrapper, attributes);
        }

        public void setMatchedValues(Object form, Object[] fieldValues, Map<Object, Object> mappedFinalValues) throws Exception {
            definition.getOptionClass().setMatchedValues(form, field, fieldValues, mappedFinalValues, wrapper, attributes);
        }
    }
    
    @Getter
    static class FieldFiller {
        
        private final Object form;
        
        private final FieldMatched matched;
        
        private final Object[] fieldValues;
        
        private FieldFiller(@NonNull FieldMatched matched, @NonNull Object form, @NonNull Object[] fieldValues) {
            this.form = form;
            this.matched = matched;
            this.fieldValues = fieldValues;
        }
        
        public static FieldFiller create(FieldMatched matched, Object form) throws Exception {
            Object[] fieldValues;
            if ((fieldValues = matched.getFeildFlatValues(form)) == null) {
                return null;
            }
            return new FieldFiller(matched, form, fieldValues);
        }
        
        public OptionClass<?> getOptionClass() {
            return matched.getDefinition().getOptionClass();
        }
        
        public void setMatched(Map<Object, Object> mappedFinalValues) throws Exception {
            if (fieldValues == null || mappedFinalValues == null) {
                return;
            }
            matched.setMatchedValues(form, fieldValues, mappedFinalValues);
        }
    }
    
    protected abstract Collection<Definition> getFieldDefinitions();
    
    protected List<FieldMatched> parseAutoFillFeilds(@NonNull Class<?> clazz, List<FieldMatched> collector)
            throws IllegalAccessException {
        if (Object.class.equals(clazz)) {
            return null;
        }
        if (collector == null) {
            collector = new LinkedList<>();
        }
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            for (Definition definition : getFieldDefinitions()) {
                FieldMatched matched;
                if ((matched = definition.createFieldMatched(field)) != null) {
                    collector.add(matched);
                    break;
                }
            }
        }
        Class<?> superClazz;
        if ((superClazz = clazz.getSuperclass()) != null) {
            parseAutoFillFeilds(superClazz, collector);
        }
        return collector;
    }
    
    protected final static Map<Class<?>, List<FieldMatched>> AUTOFILL_PARSED_CACHED = new ConcurrentHashMap<>();
    
    protected List<FieldMatched> parseAutoFillFeilds(@NonNull Class<?> clazz) throws IllegalAccessException {
        List<FieldMatched> fields;
        if ((fields = AUTOFILL_PARSED_CACHED.get(clazz)) == null) {
            fields = parseAutoFillFeilds(clazz, null);
            AUTOFILL_PARSED_CACHED.put(clazz, fields);
        }
        return fields;
    }
    
    public void apply(Object... forms) throws Exception {
        if (forms == null || forms.length <= 0 || getFieldDefinitions() == null) {
            return;
        }
        List<FieldMatched> fieldsMatched;
        List<FieldFiller> allFeildFillers = new LinkedList<>();
        for (Object form : forms) {
            if (form == null || (fieldsMatched = parseAutoFillFeilds(form.getClass())) == null
                    || fieldsMatched.isEmpty()) {
                continue;
            }
            for (FieldMatched matched : fieldsMatched) {
                FieldFiller filler;
                if ((filler = FieldFiller.create(matched, form)) == null) {
                    continue;
                }
                allFeildFillers.add(filler);
            }
        }
        Set<Object> typedFlatOptons;
        OptionClass<?> flatOptionClass;
        Object[] fieldFlatOptons;
        Map<OptionClass<?> , Set<Object>> mappedFlatOptions = new HashMap<>();
        for (FieldFiller filter : allFeildFillers) {
            if ((typedFlatOptons = mappedFlatOptions.get(flatOptionClass = filter.getOptionClass())) == null) {
                mappedFlatOptions.put(flatOptionClass, typedFlatOptons = new HashSet<>());
            }
            if ((fieldFlatOptons = filter.getFieldValues()) != null) {
                for (Object o : fieldFlatOptons) {
                    typedFlatOptons.add(o);
                }
            }
        }
        Collection<?> typedFinalOptons;
        OptionClass<?> finalOptionClass;
        Map<Object, Object> typedFinalMapepd;
        Map<OptionClass<?> , Map<Object, Object>> mappedFinalOptions = new HashMap<>();
        for (Map.Entry<OptionClass<?> , Set<Object>> typedFlatEntry : mappedFlatOptions.entrySet()) {
            finalOptionClass = typedFlatEntry.getKey();
            typedFinalOptons = finalOptionClass.queryObjectOptions(typedFlatEntry.getValue());
            if (typedFinalOptons == null || typedFinalOptons.isEmpty()) {
                continue;
            }
            typedFinalMapepd = new HashMap<>();
            for (Object option : typedFinalOptons) {
                typedFinalMapepd.put(option, option);
            }
            mappedFinalOptions.put(finalOptionClass, typedFinalMapepd);
        }
        for (FieldFiller filter : allFeildFillers) {
            if ((typedFinalMapepd = mappedFinalOptions.get(flatOptionClass = filter.getOptionClass())) == null) {
                continue;
            }
            filter.setMatched(typedFinalMapepd);
        }
    }
    
    public void apply(Collection<? extends Object> forms) throws Exception {
        if (forms == null || forms.isEmpty()) {
            return;
        }
        apply(forms.toArray(new Object[0]));
    }
}
