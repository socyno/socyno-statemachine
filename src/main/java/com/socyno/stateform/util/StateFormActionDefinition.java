package com.socyno.stateform.util;

import com.google.gson.annotations.Expose;
import com.socyno.base.bscmixutil.ClassUtil;
import com.socyno.stateform.abs.AbstractStateAction;
import com.socyno.stateform.abs.AbstractStateAction.EventFormType;
import com.socyno.webbsc.authority.Authority;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;

@Data
@Accessors(chain=true)
public class StateFormActionDefinition {
    
    private StateFormActionDefinition() {
        
    }
    
    private String name;
    
    private String formName;
    
    private String display;
    
    private String targetState;
    
    private String[] sourceStates;
    
    @Expose(serialize = false, deserialize = false)
    private Authority authority;
    
    private boolean asyncEvent;
    
    private boolean createEvent;
    
    private boolean listEvent;
    
    private boolean dynamicEvent;
      
    private Boolean messageRequired;
    
    private boolean confirmRequired;
    
    private boolean prepareRequired;
    
    private boolean stateRevisionChangeIgnored;
    
    private EventFormType eventFormType;
    
    private String formClass;
    
    private String resultClass;
    
    public static StateFormActionDefinition fromStateAction(String name, String formName, @NonNull AbstractStateAction<?, ?, ?> action)
        throws Exception {
        return  new StateFormActionDefinition()
                .setName(name)
                .setFormName(formName)
                .setDisplay(action.getDisplay())
                .setListEvent(action.isListEvent())
                .setEventFormType(action.getEventFormType())
                .setTargetState(action.getTargetStateForDisplay())
                .setSourceStates(action.getSourceStates())
                .setAsyncEvent(action.isAsyncEvent())
                .setCreateEvent(action.isCreateEvent())
                .setPrepareRequired(action.prepareRequired())
                .setConfirmRequired(action.confirmRequired())
                .setMessageRequired(action.messageRequired())
                .setStateRevisionChangeIgnored(action.getStateRevisionChangeIgnored())
                .setFormClass(ClassUtil.classToJson(action.getFormTypeClass()).toString())
                .setResultClass(ClassUtil.classToJson(action.getReturnTypeClass()).toString())
                .setAuthority(action.getAuthority())
                .setDynamicEvent(action.isDynamicEvent())
                ;
    }
}
