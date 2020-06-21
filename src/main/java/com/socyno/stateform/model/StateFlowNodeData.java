package com.socyno.stateform.model;


import com.socyno.base.bscmixutil.CommonUtil;
import com.socyno.base.bscmixutil.StringUtils;
import com.socyno.stateform.abs.AbstractStateAction;
import com.socyno.stateform.abs.AbstractStateChoice;
import com.socyno.stateform.util.StateFormRevision;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class StateFlowNodeData {

    public enum Category {
        STATE,
        ACTION,
        CHOICE,
        YESNO,
        STATE_CURRENT,
        UNCHANGED;
    }

    private final String key;

    private final String text;

    private final String name;

    private final boolean current;

    private Category category;

    public StateFlowNodeData(boolean yesNo, String keyPrefix) {
        category = Category.YESNO;
        key = String.format("%s:%s-%s", category, keyPrefix, yesNo);
        text = yesNo ? "是" : "否";
        name = String.format("%s", yesNo);
        current = false;
    }
    
    public StateFlowNodeData(String state, String display, StateFormRevision stateRevision) {
        if (StringUtils.isBlank(state)) {
            category = Category.UNCHANGED;
            key = category.name();
            text = "状态保持不变";
            name = category.name();
            current = false;
            return;
        }
        name = state;
        text = CommonUtil.ifNull(display, state);
        current = stateRevision != null && stateRevision.getStateFormStatus() != null
                        && stateRevision.getStateFormStatus().equals(state);
        category = current ? Category.STATE_CURRENT : Category.STATE;
        key = String.format("%s:%s", category, state);
    }

    public StateFlowNodeData(AbstractStateChoice choice) {
        category = Category.CHOICE;
        key = String.format("%s:%s", category, choice.getClass().getName());
        text = choice.getDisplay();
        name = choice.getClass().getName();
        current = false;
    }
    
    public StateFlowNodeData(String actionName, AbstractStateAction<?, ?, ?> action) {
        category = Category.ACTION;
        key = String.format("%s:%s", category, actionName);
        text = action.getDisplay();
        name = actionName;
        current = false;
    }
    
    @Override
    public int hashCode() {
        return key == null ? 0 : key.hashCode();
    }

    @Override
    public boolean equals(Object another) {
        if (another == null || !StateFlowNodeData.class.equals(another.getClass())) {
            return false;
        }
        return StringUtils.equals(((StateFlowNodeData) another).getKey(), key);
    }
}
