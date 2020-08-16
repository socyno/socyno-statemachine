package com.socyno.stateform.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.Expose;
import com.socyno.base.bscmixutil.ClassUtil;
import com.socyno.stateform.abs.AbstractStateForm;

public class StateFormSerializer implements JsonSerializer<AbstractStateForm> {
    @Override
    public JsonElement serialize(AbstractStateForm obj, Type typeOfT, JsonSerializationContext context) {
        if (obj == null) {
            return null;
        }
        try {
            Expose expose;
            Map<String, Object> mapped = new HashMap<>();
            for (Field field : ClassUtil.parseAllFields(obj.getClass())) {
                if (field.isSynthetic() || Modifier.isStatic(field.getModifiers())
                        || Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                if ((expose = field.getAnnotation(Expose.class)) != null && expose.serialize()) {
                    continue;
                }
                field.setAccessible(true);
                mapped.put(field.getName(), field.get(obj));
            }
            JsonElement jsoned = context.serialize(mapped);
            if (jsoned != null && jsoned.isJsonObject()) {
                ((JsonObject) jsoned).addProperty("summary", ((AbstractStateForm) obj).getSummary());
            }
            return jsoned;
        } catch (RuntimeException e) {
            throw (RuntimeException) e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
