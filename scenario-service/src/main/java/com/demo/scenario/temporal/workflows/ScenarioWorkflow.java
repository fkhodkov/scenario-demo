package com.demo.scenario.temporal.workflows;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ScenarioWorkflow {

    @WorkflowMethod
    void execute(ScenarioWorkflowInput input);

    /**
     * Signal sent by the Kafka consumer when a relevant event arrives for this user.
     * The workflow may be waiting in a WAIT_EVENT node.
     */
    @SignalMethod
    void onEvent(IncomingEvent event);
}
