package com.socyno.stateform.authority;

import java.io.Serializable;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

@Getter
public enum AuthorityScopeType implements Serializable {
    Guest("匿名用户") {
        @Override
        public boolean checkScopeId() {
            return false;
        }
    },
    System("系统全局") {
        @Override
        public boolean checkScopeId() {
            return false;
        }
    },
    Subsystem("业务系统") {
        @Override
        public boolean checkScopeId() {
            return true;
        }
    };
    
    private final String display;
    
    AuthorityScopeType(String display) {
        this.display = display;
    }
    
    public static AuthorityScopeType forName(String name) {
        if (StringUtils.isNotBlank(name)) {
            for (AuthorityScopeType scopeType : AuthorityScopeType.values()) {
                if (scopeType.name().equalsIgnoreCase(name.trim())) {
                    return scopeType;
                }
            }
        }
        return null;
    }
    
    public abstract boolean checkScopeId();
}
