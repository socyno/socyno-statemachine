package com.socyno.stateform.exec;

import com.github.reinert.jjschema.v1.FieldOption;
import com.socyno.base.bscexec.MessageException;
import com.socyno.base.bscmixutil.CommonUtil;

import lombok.Getter;
import lombok.NonNull;

@Getter
public class StateFormFieldInvalidOptionException extends MessageException {

    private static final long serialVersionUID = 1L;

    private String value;
    private Class<? extends FieldOption> clazz;
    
    public StateFormFieldInvalidOptionException(@NonNull Class<? extends FieldOption> clazz, String value) {
        this.value = value;
        this.clazz = clazz;
    }
    
    @Override
    public String getMessage() {
        return String.format("表单字段的输入选项值不正确（option class=%s, option value=%s）.", 
                            clazz.getName(), CommonUtil.ifNull(value, ""));
    }
}
