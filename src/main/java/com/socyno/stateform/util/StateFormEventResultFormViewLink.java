package com.socyno.stateform.util;

import com.github.reinert.jjschema.Attributes;
import com.socyno.stateform.abs.AbstractStateFormEventResultView;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Attributes(title = "通用流程单连接视图，事件响应该类型数据则打开新窗口至指定表单的详情页面")
public class StateFormEventResultFormViewLink implements AbstractStateFormEventResultView {

    private final String eventResultViewType = "FormViewLink";
    
    private final String formName;
    
    private final long formId;
    
    private final boolean withActions;
    
    public StateFormEventResultFormViewLink(String formName, long formId, boolean withActions) throws Exception {
        this.formName = formName;
        this.formId = formId;
        this.withActions = withActions;
    }
}