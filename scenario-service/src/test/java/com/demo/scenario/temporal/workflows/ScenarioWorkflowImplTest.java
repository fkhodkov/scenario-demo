package com.demo.scenario.temporal.workflows;

import com.demo.scenario.temporal.activities.ScenarioActivities;
import com.demo.scenario.temporal.activities.SendResult;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ScenarioWorkflowImpl using Temporal's in-process test environment.
 *
 * KEY TRICK — Temporal's activity registration scanner rejects Mockito dynamic
 * subclasses because they inherit the @ActivityMethod annotations from the interface.
 * The fix is to register a plain hand-written ADAPTER that holds a reference to the
 * Mockito mock and delegates every call.  Temporal sees a clean concrete class;
 * the mock still captures every invocation for verification.
 */
class ScenarioWorkflowImplTest {

    static final String TASK_QUEUE = "TEST_QUEUE";

    TestWorkflowEnvironment testEnv;
    WorkflowClient client;
    ScenarioActivities mockActivities;

    // ── Activity adapter — plain class, no @ActivityInterface, no @ActivityMethod ──

    /**
     * Plain delegating wrapper.  Temporal registers this; assertions go on the mock.
     */
    static class ActivitiesAdapter implements ScenarioActivities {
        private final ScenarioActivities delegate;
        ActivitiesAdapter(ScenarioActivities delegate) { this.delegate = delegate; }

        @Override public SendResult sendEmail(String u, String t, String p)   { return delegate.sendEmail(u, t, p); }
        @Override public SendResult sendPush(String u, String t, String p)    { return delegate.sendPush(u, t, p); }
        @Override public SendResult sendSms(String u, String t, String p)     { return delegate.sendSms(u, t, p); }
        @Override public void updateExecutionNode(String eid, String nid, String status) {
            delegate.updateExecutionNode(eid, nid, status);
        }
        @Override public String loadScenarioDefinition(String sid)            { return delegate.loadScenarioDefinition(sid); }
    }

    // ── Graph fixtures ────────────────────────────────────────────────────────

    /** TRIGGER → SEND_EMAIL (delivered→END, failed→END) */
    static final String GRAPH_EMAIL = """
            {"nodes":[
              {"id":"n1","type":"TRIGGER","data":{"label":"T","eventType":"USER_REGISTERED","topic":"user.registered"},"position":{"x":0,"y":0}},
              {"id":"n2","type":"SEND_EMAIL","data":{"label":"Email","templateId":"welcome","channel":"email"},"position":{"x":200,"y":0}},
              {"id":"n3","type":"END","data":{"label":"End"},"position":{"x":400,"y":0}}
            ],"edges":[
              {"id":"e1","source":"n1","target":"n2","sourceHandle":null},
              {"id":"e2","source":"n2","target":"n3","sourceHandle":"delivered"},
              {"id":"e3","source":"n2","target":"n3","sourceHandle":"failed"}
            ]}""";

    /** TRIGGER → WAIT_EVENT(EMAIL_OPENED, 60s) → [event→END_A] [timeout→END_B] */
    static final String GRAPH_WAIT = """
            {"nodes":[
              {"id":"n1","type":"TRIGGER","data":{"label":"T","topic":"user.registered"},"position":{"x":0,"y":0}},
              {"id":"n2","type":"WAIT_EVENT","data":{"label":"W","eventType":"EMAIL_OPENED","timeoutSeconds":60},"position":{"x":200,"y":0}},
              {"id":"n3","type":"END","data":{"label":"EndEvent"},"position":{"x":400,"y":0}},
              {"id":"n4","type":"END","data":{"label":"EndTimeout"},"position":{"x":400,"y":100}}
            ],"edges":[
              {"id":"e1","source":"n1","target":"n2","sourceHandle":null},
              {"id":"e2","source":"n2","target":"n3","sourceHandle":"event"},
              {"id":"e3","source":"n2","target":"n4","sourceHandle":"timeout"}
            ]}""";

    /** TRIGGER → DELAY(10s) → END */
    static final String GRAPH_DELAY = """
            {"nodes":[
              {"id":"n1","type":"TRIGGER","data":{"label":"T","topic":"t"},"position":{"x":0,"y":0}},
              {"id":"n2","type":"DELAY","data":{"label":"Wait","delaySeconds":10},"position":{"x":200,"y":0}},
              {"id":"n3","type":"END","data":{"label":"Done"},"position":{"x":400,"y":0}}
            ],"edges":[
              {"id":"e1","source":"n1","target":"n2","sourceHandle":null},
              {"id":"e2","source":"n2","target":"n3","sourceHandle":null}
            ]}""";

    // ── Setup / teardown ──────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        testEnv       = TestWorkflowEnvironment.newInstance();
        client        = testEnv.getWorkflowClient();
        mockActivities = Mockito.mock(ScenarioActivities.class);

        Worker worker = testEnv.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(ScenarioWorkflowImpl.class);
        // Register the ADAPTER, not the mock directly.
        worker.registerActivitiesImplementations(new ActivitiesAdapter(mockActivities));
        testEnv.start();
    }

    @AfterEach
    void tearDown() {
        testEnv.close();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ScenarioWorkflow newStub(String[] workflowIdOut) {
        String wfId = "test-wf-" + UUID.randomUUID();
        if (workflowIdOut != null) workflowIdOut[0] = wfId;
        return client.newWorkflowStub(ScenarioWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(wfId)
                        .build());
    }

    private void awaitCompletion(String workflowId) throws Exception {
        WorkflowStub stub = client.newUntypedWorkflowStub(workflowId);
        stub.getResult(5, TimeUnit.SECONDS, Void.class);
    }

    private ScenarioWorkflowInput inputFor(String graph) {
        UUID scenarioId  = UUID.randomUUID();
        UUID executionId = UUID.randomUUID();
        when(mockActivities.loadScenarioDefinition(scenarioId.toString())).thenReturn(graph);
        return new ScenarioWorkflowInput(scenarioId, "user_001", "{}", executionId.toString());
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void happyPath_emailDelivered_workflowCompletes() throws Exception {
        when(mockActivities.sendEmail(eq("user_001"), eq("welcome"), any()))
                .thenReturn(new SendResult("msg-1", true, "email"));

        String[] wfId = new String[1];
        ScenarioWorkflow wf = newStub(wfId);
        ScenarioWorkflowInput input = inputFor(GRAPH_EMAIL);

        WorkflowClient.start(wf::execute, input);
        Thread.sleep(300);

        wf.onEvent(new IncomingEvent("EMAIL_DELIVERED", "email.delivered", "user_001", "{}"));
        awaitCompletion(wfId[0]);

        verify(mockActivities).sendEmail("user_001", "welcome", "{}");
        verify(mockActivities, atLeastOnce()).updateExecutionNode(any(), any(), any());
    }

    @Test
    void emailFailed_signal_workflowCompletes() throws Exception {
        when(mockActivities.sendEmail(any(), any(), any()))
                .thenReturn(new SendResult("msg-1", true, "email"));

        String[] wfId = new String[1];
        ScenarioWorkflow wf = newStub(wfId);
        WorkflowClient.start(wf::execute, inputFor(GRAPH_EMAIL));
        Thread.sleep(300);

        wf.onEvent(new IncomingEvent("EMAIL_FAILED", "email.failed", "user_001", "{}"));
        awaitCompletion(wfId[0]);

        verify(mockActivities).sendEmail(any(), any(), any());
    }

    @Test
    void emailNotAccepted_unsubscribed_immediatelyCompletes() throws Exception {
        when(mockActivities.sendEmail(any(), any(), any()))
                .thenReturn(new SendResult("msg-1", false, "email"));

        String[] wfId = new String[1];
        ScenarioWorkflow wf = newStub(wfId);
        WorkflowClient.start(wf::execute, inputFor(GRAPH_EMAIL));

        awaitCompletion(wfId[0]);
        verify(mockActivities).sendEmail(any(), any(), any());
    }

    @Test
    void waitEvent_eventArrivesBeforeTimeout_completesViaEventBranch() throws Exception {
        String[] wfId = new String[1];
        ScenarioWorkflow wf = newStub(wfId);
        WorkflowClient.start(wf::execute, inputFor(GRAPH_WAIT));
        Thread.sleep(300);

        wf.onEvent(new IncomingEvent("EMAIL_OPENED", "email.opened", "user_001", "{}"));
        awaitCompletion(wfId[0]);
    }

    @Test
    void waitEvent_timeout_completesViaTimeoutBranch() throws Exception {
        String[] wfId = new String[1];
        ScenarioWorkflow wf = newStub(wfId);
        WorkflowClient.start(wf::execute, inputFor(GRAPH_WAIT));

        testEnv.sleep(Duration.ofSeconds(61));
        awaitCompletion(wfId[0]);
    }

    @Test
    void delayNode_timeSkipped_completesAfterDelay() throws Exception {
        String[] wfId = new String[1];
        ScenarioWorkflow wf = newStub(wfId);
        WorkflowClient.start(wf::execute, inputFor(GRAPH_DELAY));

        testEnv.sleep(Duration.ofSeconds(11));
        awaitCompletion(wfId[0]);
    }

    @Test
    void deliveryTimeout_noSignalReceived_completesViaTimeoutBranch() throws Exception {
        when(mockActivities.sendEmail(any(), any(), any()))
                .thenReturn(new SendResult("msg-1", true, "email"));

        String[] wfId = new String[1];
        ScenarioWorkflow wf = newStub(wfId);
        WorkflowClient.start(wf::execute, inputFor(GRAPH_EMAIL));

        testEnv.sleep(Duration.ofHours(25));
        awaitCompletion(wfId[0]);
    }

    @Test
    void updateExecutionNode_calledForEachStep() throws Exception {
        when(mockActivities.sendEmail(any(), any(), any()))
                .thenReturn(new SendResult("msg-1", true, "email"));

        String[] wfId = new String[1];
        ScenarioWorkflow wf = newStub(wfId);
        ScenarioWorkflowInput input = inputFor(GRAPH_EMAIL);
        WorkflowClient.start(wf::execute, input);
        Thread.sleep(300);

        wf.onEvent(new IncomingEvent("EMAIL_DELIVERED", "email.delivered", "user_001", "{}"));
        awaitCompletion(wfId[0]);

        verify(mockActivities, atLeast(2))
                .updateExecutionNode(eq(input.getExecutionId()), any(), any());
    }
}
