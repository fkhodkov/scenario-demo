package com.demo.scenario.temporal.workflows;

import java.util.UUID;

/**
 * Immutable input passed when starting a ScenarioWorkflow execution.
 */
public record ScenarioWorkflowInput(
        UUID   scenarioId,
        String userId,
        String triggerEventPayload,
        String executionId
) {}
