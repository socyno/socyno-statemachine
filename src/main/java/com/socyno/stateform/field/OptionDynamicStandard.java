package com.socyno.stateform.field;

import com.github.reinert.jjschema.v1.FieldSimpleOption;
import com.socyno.base.bscmixutil.StringUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

@Getter
@Setter
@ToString
@Accessors(chain = true)
public class OptionDynamicStandard extends FieldSimpleOption {
    
    public OptionDynamicStandard() {
        this(null);
    }
    
    public OptionDynamicStandard(String value) {
        super(value);
    }
    
    private String classPath;
    
    private String category;
    
    public boolean equals(Object another) {
        if (another == null || !another.getClass().equals(getClass())) {
            return false;
        }
        return StringUtils.equals(getOptionValue(), ((OptionDynamicStandard) another).getOptionValue())
                && StringUtils.equals(getCategory(), ((OptionDynamicStandard) another).getCategory())
                && StringUtils.equals(getClassPath(), ((OptionDynamicStandard) another).getClassPath());
    }
    
    public int hashCode() {
        return String.format("%s-%s-%s", getClassPath(), getCategory(), getOptionValue()).hashCode();
    }
}
