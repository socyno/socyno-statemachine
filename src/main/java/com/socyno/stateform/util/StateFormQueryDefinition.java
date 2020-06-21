package com.socyno.stateform.util;

import com.socyno.base.bscmixutil.ClassUtil;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain=true)
public class StateFormQueryDefinition {
    
    private StateFormQueryDefinition() {
        
    }
    
    private String name;
    
    private String display;

    private String formName;
    
    private String formClass;
    
    private String resultClass;
    
    public static StateFormQueryDefinition fromStateQuery(String formName, String name, StateFormNamedQuery<?> query)
        throws Exception {
        return  new StateFormQueryDefinition()
                .setName(name)
                .setDisplay(query.getName())
                .setFormName(formName)
                .setFormClass(ClassUtil.classToJson(query.getQueryClass()).toString())
                .setResultClass(ClassUtil.classToJson(query.getResultClass()).toString());
    }
}
