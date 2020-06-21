package com.socyno.stateform.abs;

import com.github.reinert.jjschema.Attributes;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class BasicStateForm implements AbstractStateForm {

    @Attributes(title = "编号", required = true, readonly = true)
    private Long id;
    
    @Attributes(title = "状态", readonly = true)
    private String state;
    
    @Attributes(title = "版本", required = true, readonly = true)
    private Long revision;
}