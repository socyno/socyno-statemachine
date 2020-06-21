package com.socyno.stateform.util;

import com.github.reinert.jjschema.Attributes;
import com.socyno.stateform.abs.AbstractStateFormEventResultView;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@Attributes(title = "常规超链接视图，事件响应该类型数据则执行打开新窗口")
public class StateFormEventResultSimpleLink implements AbstractStateFormEventResultView {

    private final String eventResultViewType = "SimpleLink";
    
    private final String url;
    
    public StateFormEventResultSimpleLink(String url) throws Exception {
        this.url = url;
    }
}
