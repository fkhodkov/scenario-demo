package com.demo.scenario.integration;

import com.demo.scenario.domain.*;
import com.demo.scenario.repository.ScenarioExecutionRepository;
import com.demo.scenario.repository.ScenarioRepository;
import com.demo.scenario.service.ScenarioService;
import com.demo.scenario.temporal.workflows.ScenarioWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.worker.WorkerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for ScenarioService using a real PostgreSQL container
 * (via the tc: JDBC URL in application.yml) and real JPA/Hibernate.
 * Temporal and Kafka producers are mocked.
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1,
    brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"})
@DirtiesContext
class ScenarioServiceIntegrationTest {

    @Autowired ScenarioService scenarioService;
    @Autowired ScenarioRepository scenarioRepo;
    @Autowired ScenarioExecutionRepository executionRepo;

    @MockBean WorkflowClient workflowClient;
    @MockBean WorkerFactory workerFactory;

    static final String GRAPH = """
            {"nodes":[
              {"id":"n1","type":"TRIGGER","data":{"label":"T","eventType":"USER_REGISTERED","topic":"user.registered"},"position":{"x":0,"y":0}},
              {"id":"n2","type":"END","data":{"label":"Done"},"position":{"x":300,"y":0}}
            ],"edges":[
              {"id":"e1","source":"n1","target":"n2","sourceHandle":null}
            ]}""";

    @BeforeEach
    void clean() {
        executionRepo.deleteAll();
        scenarioRepo.deleteAll();
    }

    // Helper: set up workflowClient stub with explicit types to avoid ambiguity
    private void stubWorkflowClient() {
        when(workflowClient.newWorkflowStub(
                org.mockito.ArgumentMatchers.<Class<ScenarioWorkflow>>any(),
                org.mockito.ArgumentMatchers.<WorkflowOptions>any()))
                .thenAnswer(inv -> {
                    ScenarioWorkflow wf = mock(ScenarioWorkflow.class);
                    doNothing().when(wf).execute(any());
                    return wf;
                });
    }

    // ── Scenario CRUD ─────────────────────────────────────────────────────────

    @Test
    void create_persistsToDatabase() throws Exception {
        Scenario s = scenarioService.create("welcome", "onboarding", GRAPH);

        assertNotNull(s.getId());
        assertEquals("welcome", s.getName());
        assertEquals(ScenarioStatus.DRAFT, s.getStatus());
        assertEquals("user.registered", s.getTriggerTopic());
        assertEquals(1, scenarioRepo.count());
    }

    @Test
    void create_twoScenarios_bothPersisted() throws Exception {
        String g2 = GRAPH.replace("user.registered", "purchase.completed")
                         .replace("USER_REGISTERED", "PURCHASE_COMPLETED");
        scenarioService.create("s1", "d1", GRAPH);
        scenarioService.create("s2", "d2", g2);

        assertEquals(2, scenarioRepo.count());
    }

    @Test
    void update_modifiesPersistedRecord() throws Exception {
        Scenario s = scenarioService.create("old", "old desc", GRAPH);
        String g2 = GRAPH.replace("user.registered", "email.opened")
                         .replace("USER_REGISTERED", "EMAIL_OPENED");

        Scenario updated = scenarioService.update(s.getId(), "new", "new desc", g2);

        assertEquals("new", updated.getName());
        assertEquals("email.opened", updated.getTriggerTopic());
        Scenario inDb = scenarioRepo.findById(s.getId()).orElseThrow();
        assertEquals("new", inDb.getName());
    }

    @Test
    void activate_changesStatusToActive() throws Exception {
        Scenario s = scenarioService.create("s", "d", GRAPH);
        Scenario activated = scenarioService.activate(s.getId());

        assertEquals(ScenarioStatus.ACTIVE, activated.getStatus());
        assertEquals(ScenarioStatus.ACTIVE,
                scenarioRepo.findById(s.getId()).orElseThrow().getStatus());
    }

    @Test
    void pause_changesStatusToPaused() throws Exception {
        Scenario s = scenarioService.create("s", "d", GRAPH);
        scenarioService.activate(s.getId());
        Scenario paused = scenarioService.pause(s.getId());

        assertEquals(ScenarioStatus.PAUSED, paused.getStatus());
    }

    @Test
    void listAll_returnsAllScenarios() throws Exception {
        scenarioService.create("a", "d", GRAPH);
        scenarioService.create("b", "d", GRAPH);

        List<Scenario> all = scenarioService.listAll();
        assertEquals(2, all.size());
    }

    // ── Execution persistence ─────────────────────────────────────────────────

    @Test
    void startExecution_persistsExecutionRecord() throws Exception {
        Scenario s = scenarioService.create("s", "d", GRAPH);
        scenarioService.activate(s.getId());
        stubWorkflowClient();

        ScenarioExecution exec = scenarioService.startExecution(s.getId(), "user_42", "{}");

        assertNotNull(exec.getId());
        assertEquals("user_42", exec.getUserId());
        assertEquals(ExecutionStatus.RUNNING, exec.getStatus());
        assertEquals(1, executionRepo.count());
    }

    @Test
    void startExecution_workflowIdContainsScenarioAndUserId() throws Exception {
        Scenario s = scenarioService.create("s", "d", GRAPH);
        scenarioService.activate(s.getId());
        stubWorkflowClient();

        ScenarioExecution exec = scenarioService.startExecution(s.getId(), "alice", "{}");

        assertTrue(exec.getWorkflowId().contains(s.getId().toString()));
        assertTrue(exec.getWorkflowId().contains("alice"));
    }

    @Test
    void listExecutions_returnsForScenario() throws Exception {
        Scenario s = scenarioService.create("s", "d", GRAPH);
        scenarioService.activate(s.getId());
        stubWorkflowClient();

        scenarioService.startExecution(s.getId(), "u1", "{}");
        scenarioService.startExecution(s.getId(), "u2", "{}");

        List<ScenarioExecution> execs = scenarioService.listExecutions(s.getId());
        assertEquals(2, execs.size());
    }

    @Test
    void findByTriggerTopic_returnsMatchingScenario() throws Exception {
        scenarioService.create("s", "d", GRAPH);
        assertTrue(scenarioRepo.findByTriggerTopic("user.registered").isPresent());
    }

    @Test
    void findByUserId_returnsAllExecutionsForUser() throws Exception {
        // Bob runs two DIFFERENT scenarios — each gets its own execution
        String g2 = GRAPH.replace("user.registered", "purchase.completed")
                         .replace("USER_REGISTERED", "PURCHASE_COMPLETED");
        Scenario s1 = scenarioService.create("s1", "d", GRAPH);
        Scenario s2 = scenarioService.create("s2", "d", g2);
        scenarioService.activate(s1.getId());
        scenarioService.activate(s2.getId());
        stubWorkflowClient();

        scenarioService.startExecution(s1.getId(), "bob", "{}");
        scenarioService.startExecution(s2.getId(), "bob", "{}");
        scenarioService.startExecution(s1.getId(), "carol", "{}");

        List<ScenarioExecution> bobExecs = executionRepo.findByUserId("bob");
        assertEquals(2, bobExecs.size());
    }
}
