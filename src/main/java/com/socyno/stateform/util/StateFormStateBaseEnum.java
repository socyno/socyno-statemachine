package com.socyno.stateform.util;

import com.github.reinert.jjschema.v1.FieldOption;
import com.socyno.base.bscmixutil.StringUtils;
import com.socyno.stateform.service.StateFormService;

public interface StateFormStateBaseEnum extends FieldOption {
    public String getCode();
    
    public String getName();
    
    @Override
    public default String getOptionValue() {
        return getCode();
    }
    
    @Override
    public default void setOptionValue(String var1) {
        
    }
    
    @Override
    public default String getOptionDisplay() {
        String stateDisplay;
        if (StringUtils.isNotBlank(stateDisplay = StateFormService
                .getCustomizedDisplayText(this.getClass().getName().concat(":").concat(getCode())))) {
            return stateDisplay;
        }
        return getName();
    }
}
