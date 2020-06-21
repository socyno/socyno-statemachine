package com.socyno.stateform.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
@Getter
@Setter
@ToString
public class StateFlowDefinition {
    private Collection<StateFlowNodeData> nodeData;

    private List<StateFlowLinkData> linkData;

    public StateFlowDefinition (Collection<StateFlowNodeData> nodeData, List<StateFlowLinkData> linkData){
        this.nodeData = nodeData == null ? nodeData : new HashSet<>(nodeData);
        this.linkData = linkData == null ? linkData : new ArrayList<>(linkData);
    }
}
