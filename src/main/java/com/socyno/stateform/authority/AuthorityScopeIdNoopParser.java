package com.socyno.stateform.authority;


public class AuthorityScopeIdNoopParser implements AuthorityScopeIdParser {
    public Long getAuthorityScopeId(Object scopeSource) {
        return null;
    }
}
