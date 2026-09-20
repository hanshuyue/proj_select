package com.scaffold.workflow.domain;

import java.util.Map;

public final class WorkflowRequests {

    private WorkflowRequests() {
    }

    public record ModelWriteRequest(
            String modelKey,
            String modelName,
            String category,
            String desc,
            String bpmnXml
    ) {
    }

    public record StartInstanceRequest(
            String processDefinitionId,
            String businessKey,
            String businessType,
            String businessTitle,
            Map<String, Object> variables
    ) {
    }

    public record ApproveRequest(
            String result,
            String comment
    ) {
    }
}
