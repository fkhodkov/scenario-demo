package com.demo.scenario.service;

import com.demo.scenario.domain.ExecutionStatus;
import com.demo.scenario.domain.ScenarioExecution;
import com.demo.scenario.domain.ScenarioStatus;
import com.demo.scenario.repository.ScenarioExecutionRepository;
import com.demo.scenario.temporal.workflows.IncomingEvent;
import com.demo.scenario.temporal.workflows.ScenarioWorkflow;
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventSignalServiceTest {

    @Mock WorkflowClient workflowClient;
    @Mock ScenarioExecutionRepository executionRepo;

    EventSignalService signalService;

    @BeforeEach
    void setUp() {
        signalService = new EventSignalService(workflowClient, executionRepo);
    }

    @Test
    void routeEvent_toRunningWorkflow_sendsSignal() {
        ScenarioExecution exec = runningExecution("user_1", "wf-abc");
        when(executionRepo.findByUserId("user_1")).thenReturn(List.of(exec));

        ScenarioWorkflow mockWf = mock(ScenarioWorkflow.class);
        when(workflowClient.newWorkflowStub(ScenarioWorkflow.class, "wf-abc")).thenReturn(mockWf);

        signalService.routeEventToWorkflows("user_1", "EMAIL_OPENED", "email.opened", "{\"msgId\":\"x\"}");

        ArgumentCaptor<IncomingEvent> captor = ArgumentCaptor.forClass(IncomingEvent.class);
        verify(mockWf).onEvent(captor.capture());

        IncomingEvent signal = captor.getValue();
        assertEquals("EMAIL_OPENED",  signal.getEventType());
        assertEquals("email.opened",  signal.getTopic());
        assertEquals("user_1",        signal.getUserId());
        assertEquals("{\"msgId\":\"x\"}", signal.getPayload());
    }

    @Test
    void routeEvent_noRunningExecutions_doesNotCallWorkflowClient() {
        when(executionRepo.findByUserId("user_1")).thenReturn(List.of());

        signalService.routeEventToWorkflows("user_1", "EMAIL_OPENED", "email.opened", "{}");

        verifyNoInteractions(workflowClient);
    }

    @Test
    void routeEvent_completedExecution_isSkipped() {
        ScenarioExecution completed = buildExecution("user_1", "wf-done", ExecutionStatus.COMPLETED);
        when(executionRepo.findByUserId("user_1")).thenReturn(List.of(completed));

        signalService.routeEventToWorkflows("user_1", "EMAIL_OPENED", "email.opened", "{}");

        verifyNoInteractions(workflowClient);
    }

    @Test
    void routeEvent_mixedExecutions_onlySignalsRunning() {
        ScenarioExecution running   = runningExecution("user_1", "wf-running");
        ScenarioExecution completed = buildExecution("user_1", "wf-done", ExecutionStatus.COMPLETED);
        ScenarioExecution failed    = buildExecution("user_1", "wf-fail", ExecutionStatus.FAILED);
        when(executionRepo.findByUserId("user_1")).thenReturn(List.of(running, completed, failed));

        ScenarioWorkflow mockWf = mock(ScenarioWorkflow.class);
        when(workflowClient.newWorkflowStub(ScenarioWorkflow.class, "wf-running")).thenReturn(mockWf);

        signalService.routeEventToWorkflows("user_1", "LINK_CLICKED", "link.clicked", "{}");

        verify(mockWf, times(1)).onEvent(any());
        verify(workflowClient, times(1)).newWorkflowStub(eq(ScenarioWorkflow.class), anyString());
    }

    @Test
    void routeEvent_workflowNotFound_doesNotThrow() {
        ScenarioExecution exec = runningExecution("user_1", "wf-gone");
        when(executionRepo.findByUserId("user_1")).thenReturn(List.of(exec));

        // WorkflowNotFoundException.getMessage() calls execution.getWorkflowId() —
        // passing null causes NPE.  Build a minimal WorkflowExecution protobuf instead.
        WorkflowExecution protoExec = WorkflowExecution.newBuilder()
                .setWorkflowId("wf-gone")
                .setRunId("run-gone")
                .build();
        ScenarioWorkflow stub = mock(ScenarioWorkflow.class);
        when(workflowClient.newWorkflowStub(ScenarioWorkflow.class, "wf-gone"))
                .thenReturn(stub);
        doThrow(new WorkflowNotFoundException(protoExec, "wf-gone", null))
                .when(stub).onEvent(any());

        // Should not propagate the exception
        assertDoesNotThrow(() ->
                signalService.routeEventToWorkflows("user_1", "EMAIL_OPENED", "email.opened", "{}"));
    }

    @Test
    void routeEvent_signalsMultipleRunningWorkflows() {
        ScenarioExecution exec1 = runningExecution("user_1", "wf-1");
        ScenarioExecution exec2 = runningExecution("user_1", "wf-2");
        when(executionRepo.findByUserId("user_1")).thenReturn(List.of(exec1, exec2));

        ScenarioWorkflow wf1 = mock(ScenarioWorkflow.class);
        ScenarioWorkflow wf2 = mock(ScenarioWorkflow.class);
        when(workflowClient.newWorkflowStub(ScenarioWorkflow.class, "wf-1")).thenReturn(wf1);
        when(workflowClient.newWorkflowStub(ScenarioWorkflow.class, "wf-2")).thenReturn(wf2);

        signalService.routeEventToWorkflows("user_1", "PURCHASE_COMPLETED", "purchase.completed", "{}");

        verify(wf1).onEvent(any());
        verify(wf2).onEvent(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ScenarioExecution runningExecution(String userId, String workflowId) {
        return buildExecution(userId, workflowId, ExecutionStatus.RUNNING);
    }

    private ScenarioExecution buildExecution(String userId, String workflowId,
                                              ExecutionStatus status) {
        ScenarioExecution e = ScenarioExecution.builder()
                .userId(userId).workflowId(workflowId).status(status)
                .currentNodeId("node_1").build();
        e.setId(UUID.randomUUID());
        return e;
    }
}
