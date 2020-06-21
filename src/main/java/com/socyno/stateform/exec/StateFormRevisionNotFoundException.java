package com.socyno.stateform.exec;

import com.socyno.base.bscexec.MessageException;

import lombok.Getter;

@Getter
public class StateFormRevisionNotFoundException extends MessageException {
    private static final long serialVersionUID = 1L;

    private Long formId;
    private String formName;
    private boolean forOriginForm;
    
    public StateFormRevisionNotFoundException(String form, Long formId, boolean forOriginForm) {
        this.formId = formId;
        this.formName = form;
        this.forOriginForm = forOriginForm;
    }
    
    public StateFormRevisionNotFoundException(String form, Long formId) {
        this(form, formId, false);
    }
    
    @Override
    public String getMessage() {
        return String.format("无法从%s的表单实体中获取版本信息，确认表单内容是否完整。",
                                forOriginForm ? "存储" : "提供");
    }
}
