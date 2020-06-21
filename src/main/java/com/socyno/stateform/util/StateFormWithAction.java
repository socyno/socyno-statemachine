package com.socyno.stateform.util;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

import com.socyno.base.bscmixutil.ClassUtil;
import com.socyno.stateform.abs.AbstractStateForm;

@Data
public class StateFormWithAction<F extends AbstractStateForm> {
    
    private final F form;
    private final String formClass;
    private final List<StateFormActionDefinition> actions = new ArrayList<>();
    
    public StateFormWithAction(F form, List<StateFormActionDefinition> actions) throws Exception {
        this.form = form;
        this.formClass = form == null ? null : ClassUtil.classToJson(form.getClass()).toString();
        if (actions != null) {
            this.actions.addAll(actions);
        }
    }
}
