package com.socyno.stateform.exec;

import com.socyno.base.bscexec.MessageException;
import com.socyno.base.bscmixutil.CommonUtil;

import lombok.Getter;

@Getter
public class StateFormNotFoundException extends MessageException {
    
    private static final long serialVersionUID = 1L;
    
    private Long formId;
    private String formName;
    
    public StateFormNotFoundException(String name, Long formId) {
        this.formName = name;
        this.formId = formId;
    }
    
    @Override
    public String getMessage() {
        return String.format("流程表单记录（%s/%s）不存在.", CommonUtil.ifNull(formName, ""), CommonUtil.ifNull(formId, ""));
    }
}
