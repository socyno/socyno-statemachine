package com.socyno.stateform.util;

import com.github.reinert.jjschema.Attributes;
import com.socyno.stateform.abs.AbstractStateFormEventResultView;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Attributes(title = "通用流程单操作视图，事件响应该类型数据则执行打开新窗口至指定表单的特定事件窗口")
public class StateFormEventResultFormEventLink implements AbstractStateFormEventResultView {

    private final String eventResultViewType = "FormEventLink";
    
    private String formName;
    
    private String formEvent;
    
    private long formId;
    
    public StateFormEventResultFormEventLink(String formName, long formId, String formEvent) throws Exception {
        this.formName = formName;
        this.formId = formId;
        this.formEvent = formEvent;
    }
}