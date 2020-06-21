package com.socyno.stateform.authority;


public interface AuthoritySpecialRejecter {
    public boolean check(Object scopeSource) throws Exception;
}
