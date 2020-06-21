package com.socyno.stateform.util;

import com.github.reinert.jjschema.Attributes;
import com.socyno.stateform.abs.AbstractStateFormEventResultView;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Attributes(title = "通用流程单创建图，事件响应该类型数据则执行打开新窗口至指定流程单的创建窗口")
public class StateFormEventResultFormCreateLink implements AbstractStateFormEventResultView {

    private final String eventResultViewType = "FormCreateLink";
    
    private String formName;
    
    private String formEvent;
    
    public StateFormEventResultFormCreateLink(String formName, String formEvent) throws Exception {
        this.formName = formName;
        this.formEvent = formEvent;
    }
}