package com.socyno.stateform.field;

import com.github.reinert.jjschema.Attributes;

import lombok.Data;

@Data
public class FilterBasicKeyword implements FilterAbstractFrom {
    @Attributes(title = "表单名称")
    private String formName;
    
    @Attributes(title = "表单编号")
    private Long formId;
    
    @Attributes(title = "关键字", position = 100)
    private String keyword;

    @Attributes(title = "表单数据")
    private String formJson;
    
    public FilterBasicKeyword(String keyword, String formName, Long formId) {
        setFormId(formId);
        setKeyword(keyword);
        setFormName(formName);
    }
}
