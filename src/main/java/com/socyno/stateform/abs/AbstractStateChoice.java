package com.socyno.stateform.abs;

import lombok.Data;
import lombok.NonNull;

import com.socyno.base.bscmixutil.StringUtils;
import com.socyno.stateform.exec.StateFormChoiceTooDeepException;
import com.socyno.stateform.exec.StateFormTargetChoiceException;
import com.socyno.stateform.util.StateFormSimpleChoice;

@Data
public abstract class AbstractStateChoice {

    private final String display;
    private final String targetState;
    private final AbstractStateChoice trueState;
    private final AbstractStateChoice falseState;
    
    protected AbstractStateChoice(String targetState) {
        this(null, targetState, null ,null);
    }
    
    protected AbstractStateChoice(AbstractStateChoice targetState) {
        this(null, targetState.getTargetState(), null ,null);
    }
    
    public AbstractStateChoice(String display, String trueState, String falseState) {
        this(display, null, StateFormSimpleChoice.getInstance(trueState), StateFormSimpleChoice.getInstance(falseState));
    }
    
    public AbstractStateChoice(String display, String trueState, AbstractStateChoice falseState) {
        this(display, null, StateFormSimpleChoice.getInstance(trueState), falseState);
    }
    
    public AbstractStateChoice(String display, AbstractStateChoice trueState, String falseState) {
        this(display, null, trueState, StateFormSimpleChoice.getInstance(falseState));
    }
    
    public AbstractStateChoice(String display, AbstractStateChoice trueState, AbstractStateChoice falseState) {
        this(display, null, trueState, falseState);
    }
    
    private AbstractStateChoice(String display, String targetState, AbstractStateChoice trueState, AbstractStateChoice falseState) {
        boolean simpleIsBlank = StringUtils.isBlank(targetState);
        if ((simpleIsBlank && trueState == null) || (!simpleIsBlank && (trueState != null || falseState != null))) {
            throw new StateFormTargetChoiceException();
        }
        this.trueState = trueState;
        this.falseState = falseState;
        this.targetState = targetState;
        this.display = StringUtils.ifBlank(display, targetState);
    }
    
    public boolean isSimple() {
        return (this instanceof StateFormSimpleChoice)
                || (getTrueState() == null && getFalseState() == null);
    }
    
    public String getTargetState(@NonNull AbstractStateForm form) {
        int maxDeepth = 15;
    	AbstractStateChoice t = this;
        while (t != null && !t.isSimple()) {
        	if (maxDeepth-- <= 0) {
        		throw new StateFormChoiceTooDeepException(this);
        	}
        	try {
        	    t = t.select(form) ? t.getTrueState() : t.getFalseState();
        	} catch(Exception e) {
        	    if (e instanceof RuntimeException) {
        	        throw (RuntimeException)e;
        	    }
        	    throw new RuntimeException(e);
        	}
        }
        if (t == null) {
            return null;
        }
        return t.getTargetState();
    }
    
    /**
     * 检测当给定表单是否与选择器的分支相关。
     * 其目的在于绘制实列流程图时，是否需要保留该选择器的分支。
     * @param form
     * @param yesNo
     * @return
     */
    public boolean flowMatched(AbstractStateForm form, boolean yesNo) {
        return true;
    }
    
    /**
     * 选择器的走向选择逻辑，当该方法返回 true 则接下来走向 tureState, 否则走向 falseState。
     * @param form
     * @return
     */
    protected abstract boolean select(AbstractStateForm form) throws Exception;
}
