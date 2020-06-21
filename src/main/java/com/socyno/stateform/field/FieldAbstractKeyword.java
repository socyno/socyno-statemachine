package com.socyno.stateform.field;

import java.util.Collections;
import java.util.List;

import com.github.reinert.jjschema.SchemaIgnore;
import com.github.reinert.jjschema.v1.FieldOption;
import com.github.reinert.jjschema.v1.FieldOptionsFilter;
import com.socyno.base.bscfield.FieldTableView;
import com.socyno.base.bscmixutil.ClassUtil;

public abstract class FieldAbstractKeyword<F extends FilterBasicKeyword> extends FieldTableView {
    
    @Override
    @SchemaIgnore
    public FieldOptionsType getOptionsType() {
        return FieldOptionsType.DYNAMIC;
    }
    
    @Override
    @SchemaIgnore
    public Class<? extends FieldOptionsFilter> getDynamicFilterFormClass() {
        return null;
    }
    
    @SuppressWarnings("unchecked")
    @SchemaIgnore
    public Class<F> getFilterFormClass() {
        return (Class<F>) ClassUtil.getActualParameterizedType(this.getClass(), FieldAbstractKeyword.class, 0);
    }
    
    @Override
    @SchemaIgnore
    @SuppressWarnings("unchecked")
    public final List<? extends FieldOption> queryDynamicOptions(FieldOptionsFilter filter) throws Exception {
        if (filter == null || !getFilterFormClass().isAssignableFrom(filter.getClass())) {
            return Collections.emptyList();
        }
        return queryDynamicOptions((F) filter);
    }
    
    public abstract List<? extends FieldOption> queryDynamicOptions(F filter) throws Exception;
}
