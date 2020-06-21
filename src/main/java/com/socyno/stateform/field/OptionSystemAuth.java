package com.socyno.stateform.field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.v1.FieldOption;
import com.github.reinert.jjschema.v1.FieldSimpleOption;
import com.github.reinert.jjschema.v1.FieldType;

import lombok.Data;
import lombok.Getter;

@Data
public class OptionSystemAuth implements FieldOption {
    
    @Getter
    public static enum AuthType {
        Interfaze("interface"),
        FormEvent("form_event");
        
        private final String code;
        
        AuthType(String code) {
            this.code = code;
        }
    }
    
    public static class FieldOptionsAuthType extends FieldType {
    
    	private final static FieldOptionsAuthType INSTANCE = new FieldOptionsAuthType();
    	
    	public static FieldOptionsAuthType getInstance() {
    		return INSTANCE;
    	}
    	
        @SuppressWarnings("serial")
        private final List<FieldSimpleOption> options = new ArrayList<FieldSimpleOption>() {{
            add(FieldSimpleOption.create(AuthType.Interfaze.getCode(), "系统接口"));
            add(FieldSimpleOption.create(AuthType.FormEvent.getCode(), "表单事件"));
        }};
        
        public FieldOptionsType getOptionsType() {
            return FieldOptionsType.STATIC;
        }
        
        public List<FieldSimpleOption> getStaticOptions() {
            return Collections.unmodifiableList(options);
        }
    }
    
    @Attributes(title="授权范围", position = 10)
    private String scopeType;
    
    @Attributes(title="授权标识", position = 20, type = FieldOptionsAuthType.class)
    private String type;
    
    @Attributes(title="授权标识", position = 30)
    private String auth;
    
    @Attributes(title="请求方式")
    private String requestMethods;
    
    @Attributes(title="请求函数")
    private String controllerMethod;
    
    @Override
    public String getOptionValue() {
        return getAuth();
    }
    
    @Override
    public String getOptionDisplay() {
        return String.format("%s:%s", getScopeType(), getAuth());
    }

    @Override
    public String getOptionGroup() {
        return type;
    }
    
    @Override
    public void setOptionValue(String value) {
        setAuth(value);
    }
}
