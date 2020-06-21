package com.socyno.stateform.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import com.github.reinert.jjschema.Attributes;

import lombok.NonNull;

public class StateFormDynamicViewModelUtil {
    
    @Attributes()
    public String get$$templated$$() {
        return null;
    }
    
    public static Object getFieldValue(@NonNull Object object, String field)
            throws IllegalAccessException, NoSuchFieldException {
        Field feildDef = object.getClass().getDeclaredField(field);
        feildDef.setAccessible(true);
        return feildDef.get(object);
    }
    
    @SuppressWarnings("unchecked")
    public static void setAnnotationAttr(Annotation annotation, String attr, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        InvocationHandler hanlder = Proxy.getInvocationHandler(annotation);
        Field valuesFeld = hanlder.getClass().getDeclaredField("memberValues");
        valuesFeld.setAccessible(true);
        ((Map<String, Object>) valuesFeld.get(hanlder)).put(attr, value);
    }
    
    public static Method createAttributesMethod(@NonNull String method, @NonNull Class<?> type, Map<String, Object> attributes)
            throws Exception {
        if (!method.startsWith("get") && !method.startsWith("is")) {
            method = String.format("get%s", method);
        }
        Method tmpl = StateFormDynamicViewModelUtil.class.getDeclaredMethod("get$$templated$$");
        Constructor<Method> copier = Method.class.getDeclaredConstructor(Class.class, String.class, Class[].class,
                Class.class, Class[].class, int.class, int.class, String.class, byte[].class, byte[].class,
                byte[].class);
        copier.setAccessible(true);
        Method copied = copier.newInstance(
                getFieldValue(tmpl, "clazz"),
                method,
                getFieldValue(tmpl, "parameterTypes"),
                type,
                getFieldValue(tmpl, "exceptionTypes"), 
                getFieldValue(tmpl, "modifiers"),
                getFieldValue(tmpl, "slot"),
                getFieldValue(tmpl, "signature"),
                getFieldValue(tmpl, "annotations"),
                getFieldValue(tmpl, "parameterAnnotations"),
                getFieldValue(tmpl, "annotationDefault"));
        Attributes annotation = copied.getAnnotation(Attributes.class);
        if (attributes != null && attributes.size() > 0) {
            for (Map.Entry<String,Object> attr : attributes.entrySet()) {
                setAnnotationAttr(annotation, attr.getKey(), attr.getValue());
            }
        }
        return copied;
    }
}
