package com.socyno.stateform.service;

import com.socyno.base.bscmixutil.StringUtils;
import com.socyno.stateform.abs.AbstractStateFormService;

import lombok.Getter;

public class PermissionService extends com.socyno.webbsc.service.jdbc.PermissionService  {
    
    @Getter
    private final static PermissionService instance = new PermissionService();
    
	/**
     * 检查当前用户是否有指定的操作授权
     * 
     */
    public boolean hasFormEventPermission(String authScope, String formName, String eventKey, Long scopeTargetId) throws Exception {
        if (StringUtils.isBlank(formName) || StringUtils.isBlank(eventKey)) {
            return false;
        }
        return hasPermission(AbstractStateFormService.getFormEventKey(formName, eventKey), authScope, scopeTargetId);
    }

    public boolean hasFormEventAnyPermission(String authScope, String formName, String eventKey) throws Exception {
        if (StringUtils.isBlank(formName) || StringUtils.isBlank(eventKey)) {
            return false;
        }
        return hasAnyPermission(AbstractStateFormService.getFormEventKey(formName, eventKey), authScope);
    }
    
    public long[] queryFormEventScopeTargetIds(String authScope, String formName, String eventKey) throws Exception {
        return queryMyScopeTargetIdsByAuthKey(authScope, AbstractStateFormService.getFormEventKey(formName, eventKey));
    }
}
