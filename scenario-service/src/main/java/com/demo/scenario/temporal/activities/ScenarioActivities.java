package com.demo.scenario.temporal.activities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ScenarioActivities {

    @ActivityMethod
    SendResult sendEmail(String userId, String templateId, String payload);

    @ActivityMethod
    SendResult sendPush(String userId, String templateId, String payload);

    @ActivityMethod
    SendResult sendSms(String userId, String templateId, String payload);

    @ActivityMethod
    void updateExecutionNode(String executionId, String nodeId, String status);

    @ActivityMethod
    String loadScenarioDefinition(String scenarioId);
}
