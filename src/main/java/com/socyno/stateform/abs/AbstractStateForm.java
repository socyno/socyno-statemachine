package com.socyno.stateform.abs;

import com.socyno.base.bscmixutil.StringUtils;

public interface AbstractStateForm {
    
    public Long getId();
    
    public void setId(Long formId);
    
    public Long getRevision();
    
    public void setRevision(Long revision);
    
    public String getState();
    
    public void setState(String state);
    
    public default String getSummary() {
        return StringUtils.leftPad("" + getId(), 8, "0");
    }
}
