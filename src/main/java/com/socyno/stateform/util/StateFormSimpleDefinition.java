package com.socyno.stateform.util;

import java.util.List;

import com.github.reinert.jjschema.v1.FieldOption;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain=true)
public class StateFormSimpleDefinition {
    private String name;
    private String title;
    private String formClass;
    private String[] visibleActions;
    private List<? extends FieldOption>    states;
    private List<StateFormQueryDefinition>  queries;
    private List<StateFormActionDefinition> actions;
    private List<StateFormActionDefinition> otherActions;
}
