package com.demo.scenario.temporal.workflows;

import java.util.UUID;

public class ScenarioWorkflowInput {
    private UUID scenarioId;
    private String userId;
    private String triggerEventPayload;
    private String executionId;

    public ScenarioWorkflowInput() {}

    public ScenarioWorkflowInput(UUID scenarioId, String userId,
                                  String triggerEventPayload, String executionId) {
        this.scenarioId           = scenarioId;
        this.userId               = userId;
        this.triggerEventPayload  = triggerEventPayload;
        this.executionId          = executionId;
    }

    public UUID getScenarioId()              { return scenarioId; }
    public String getUserId()                { return userId; }
    public String getTriggerEventPayload()   { return triggerEventPayload; }
    public String getExecutionId()           { return executionId; }

    public void setScenarioId(UUID scenarioId)                    { this.scenarioId = scenarioId; }
    public void setUserId(String userId)                          { this.userId = userId; }
    public void setTriggerEventPayload(String triggerEventPayload){ this.triggerEventPayload = triggerEventPayload; }
    public void setExecutionId(String executionId)                { this.executionId = executionId; }
}
