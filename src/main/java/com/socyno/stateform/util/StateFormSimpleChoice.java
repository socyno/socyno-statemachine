package com.socyno.stateform.util;

import org.apache.commons.lang3.StringUtils;

import com.socyno.stateform.abs.AbstractStateChoice;
import com.socyno.stateform.abs.AbstractStateForm;

public class StateFormSimpleChoice extends AbstractStateChoice {
    
    protected StateFormSimpleChoice(String trueState) {
        super(trueState);
    }
    
    public static StateFormSimpleChoice getInstance(String trueState) {
        if (StringUtils.isBlank(trueState)) {
            return null;
        }
        return new StateFormSimpleChoice(trueState);
    }
    
    @Override
    public boolean isSimple() {
        return true;
    }
    
    @Override
    protected boolean select(AbstractStateForm form) {
        return true;
    }
}
