package com.socyno.stateform.sugger;

import java.lang.reflect.Field;
import java.util.Collection;
import com.github.reinert.jjschema.Attributes;
import com.socyno.base.bscmixutil.ClassUtil;
import com.socyno.base.bscmixutil.StringUtils;
import com.socyno.stateform.field.AbstractFieldDynamicStandard;
import com.socyno.stateform.field.OptionDynamicStandard;
import com.socyno.stateform.sugger.AbstractStateFormSugger.Definition;
import com.socyno.stateform.sugger.AbstractStateFormSugger.OptionClass;

import lombok.Getter;

public class SuggerDefinitionDynamicOption extends Definition {
    
    @Getter
    private static final SuggerDefinitionDynamicOption instance = new SuggerDefinitionDynamicOption();
    
    @Override
    public Class<?> getTypeClass() {
        return AbstractFieldDynamicStandard.class;
    }
    
    private final OptionClass<?> optionClass = new OptionClass<OptionDynamicStandard>() {
        @Override
        protected Class<OptionDynamicStandard> getType() {
            return OptionDynamicStandard.class;
        }
        
        @Override
        protected Collection<OptionDynamicStandard> queryOptions(Collection<OptionDynamicStandard> values) throws Exception {
            return AbstractFieldDynamicStandard.queryDynamicValues(values);
        }
        
        @Override
        protected void fillOriginOption(Object form, OptionDynamicStandard fieldValue, OptionWrapper wrapper, Field field,
                Attributes fieldAttrs) throws Exception {
            if (fieldValue == null || fieldAttrs == null || !(fieldValue instanceof OptionDynamicStandard)) {
                return;
            }
            String category = "";
            AbstractFieldDynamicStandard<?> fieldTypeInstance = (AbstractFieldDynamicStandard<?>) ClassUtil
                    .getSingltonInstance(fieldAttrs.type());
            if (fieldTypeInstance.categoryRequired()
                    && StringUtils.isBlank(category = fieldTypeInstance.getCategoryFieldValue(form))) {
                return;
            }
            ((OptionDynamicStandard) fieldValue).setCategory(category).setClassPath(fieldAttrs.type().getName());
        }
    };
    
    @Override
    public OptionClass<?> getOptionClass() {
        return optionClass;
    }
}
