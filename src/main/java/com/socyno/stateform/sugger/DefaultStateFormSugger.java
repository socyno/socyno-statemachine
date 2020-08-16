package com.socyno.stateform.sugger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import lombok.Getter;

public class DefaultStateFormSugger extends AbstractStateFormSugger {
    
    @Getter
    private static final DefaultStateFormSugger instance = new DefaultStateFormSugger();
    
    private static final List<Definition> DEFINITIONS = new LinkedList<Definition>() {
        private static final long serialVersionUID = 1L;
        {
            add(SuggerDefinitionDynamicOption.getInstance());
        }
    };
    
    public static void addFieldDefinitions(Definition... fieldDefinitions) {
        if (fieldDefinitions == null) {
            return;
        }
        for (Definition d : fieldDefinitions) {
            if (d != null) {
                DEFINITIONS.add(d);
            }
        }
    }
    
    @Override
    protected Collection<Definition> getFieldDefinitions() {
        return DEFINITIONS;
    }
}
