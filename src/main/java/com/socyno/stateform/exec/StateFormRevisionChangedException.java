package com.socyno.stateform.exec;

import com.socyno.base.bscexec.MessageException;
import com.socyno.stateform.abs.AbstractStateForm;

import lombok.Getter;

@Getter
public class StateFormRevisionChangedException extends MessageException {
    private static final long serialVersionUID = 1L;
    private final Long oldRevision;
    private final Long newRevision;
    private final AbstractStateForm form;
    
    public StateFormRevisionChangedException(AbstractStateForm form, Long oldRevision, Long newRevision) {
        this.form = form;
        this.oldRevision = oldRevision;
        this.newRevision = newRevision;
    }
    
    @Override
    public String getMessage() {
        return "表单实体已被变更，请刷新页面(或重新获取)。";
    }
}
