package com.demo.scenario.service;

import com.demo.scenario.domain.*;
import com.demo.scenario.repository.ScenarioExecutionRepository;
import com.demo.scenario.repository.ScenarioRepository;
import com.demo.scenario.temporal.workflows.ScenarioWorkflow;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScenarioServiceTest {

    @Mock ScenarioRepository scenarioRepo;
    @Mock ScenarioExecutionRepository executionRepo;
    @Mock WorkflowClient workflowClient;

    ScenarioService service;
    ObjectMapper mapper = new ObjectMapper();

    // Valid minimal graph JSON
    static final String VALID_GRAPH = """
            {"nodes":[{"id":"n1","type":"TRIGGER","data":{"label":"T","eventType":"USER_REGISTERED","topic":"user.registered"},"position":{"x":0,"y":0}}],"edges":[]}
            """;

    @BeforeEach
    void setUp() {
        service = new ScenarioService(scenarioRepo, executionRepo, workflowClient, mapper);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_persistsScenarioWithDraftStatus() throws Exception {
        when(scenarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Scenario result = service.create("onboarding", "welcome flow", VALID_GRAPH);

        assertEquals("onboarding",        result.getName());
        assertEquals("welcome flow",      result.getDescription());
        assertEquals(ScenarioStatus.DRAFT, result.getStatus());
        assertEquals("user.registered",   result.getTriggerTopic());
        assertEquals("USER_REGISTERED",   result.getTriggerEventType());
        verify(scenarioRepo).save(any(Scenario.class));
    }

    @Test
    void create_invalidJson_throws() {
        assertThrows(Exception.class,
                () -> service.create("bad", "desc", "{not valid json}"));
        verify(scenarioRepo, never()).save(any());
    }

    @Test
    void create_noTriggerNode_stillSavesWithNullTopic() throws Exception {
        String noTrigger = "{\"nodes\":[{\"id\":\"n1\",\"type\":\"END\",\"data\":{\"label\":\"end\"},\"position\":{\"x\":0,\"y\":0}}],\"edges\":[]}";
        when(scenarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Scenario result = service.create("x", "y", noTrigger);

        assertNull(result.getTriggerTopic());
        assertNull(result.getTriggerEventType());
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Test
    void update_modifiesExistingScenario() throws Exception {
        UUID id = UUID.randomUUID();
        Scenario existing = buildScenario(id, ScenarioStatus.DRAFT);
        when(scenarioRepo.findById(id)).thenReturn(Optional.of(existing));
        when(scenarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String newGraph = "{\"nodes\":[{\"id\":\"n1\",\"type\":\"TRIGGER\",\"data\":{\"label\":\"T\",\"topic\":\"purchase.completed\",\"eventType\":\"PURCHASE_COMPLETED\"},\"position\":{\"x\":0,\"y\":0}}],\"edges\":[]}";
        Scenario result = service.update(id, "renamed", "new desc", newGraph);

        assertEquals("renamed",              result.getName());
        assertEquals("new desc",             result.getDescription());
        assertEquals("purchase.completed",   result.getTriggerTopic());
        assertEquals("PURCHASE_COMPLETED",   result.getTriggerEventType());
    }

    @Test
    void update_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(scenarioRepo.findById(id)).thenReturn(Optional.empty());

        assertThrows(Exception.class,
                () -> service.update(id, "x", "y", VALID_GRAPH));
    }

    // ── activate / pause ──────────────────────────────────────────────────────

    @Test
    void activate_setsStatusActive() {
        UUID id = UUID.randomUUID();
        Scenario s = buildScenario(id, ScenarioStatus.DRAFT);
        when(scenarioRepo.findById(id)).thenReturn(Optional.of(s));
        when(scenarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Scenario result = service.activate(id);
        assertEquals(ScenarioStatus.ACTIVE, result.getStatus());
    }

    @Test
    void pause_setsStatusPaused() {
        UUID id = UUID.randomUUID();
        Scenario s = buildScenario(id, ScenarioStatus.ACTIVE);
        when(scenarioRepo.findById(id)).thenReturn(Optional.of(s));
        when(scenarioRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Scenario result = service.pause(id);
        assertEquals(ScenarioStatus.PAUSED, result.getStatus());
    }

    // ── startExecution ────────────────────────────────────────────────────────

    @Test
    void startExecution_createsExecutionAndStartsWorkflow() {
        UUID scenarioId = UUID.randomUUID();
        Scenario scenario = buildScenario(scenarioId, ScenarioStatus.ACTIVE);
        when(scenarioRepo.findById(scenarioId)).thenReturn(Optional.of(scenario));

        ScenarioExecution savedExec = ScenarioExecution.builder()
                .scenario(scenario).userId("user_1").workflowId("wf-x")
                .status(ExecutionStatus.RUNNING).currentNodeId("start").build();
        savedExec.setId(UUID.randomUUID());
        when(executionRepo.save(any())).thenReturn(savedExec);

        ScenarioWorkflow mockWf = mock(ScenarioWorkflow.class);
        when(workflowClient.newWorkflowStub(eq(ScenarioWorkflow.class), any(WorkflowOptions.class)))
                .thenReturn(mockWf);

        ScenarioExecution result = service.startExecution(scenarioId, "user_1", "{}");

        assertNotNull(result);
        assertEquals("user_1", result.getUserId());
        assertEquals(ExecutionStatus.RUNNING, result.getStatus());
        verify(executionRepo).save(any(ScenarioExecution.class));
    }

    @Test
    void startExecution_inactiveScenario_throws() {
        UUID scenarioId = UUID.randomUUID();
        Scenario scenario = buildScenario(scenarioId, ScenarioStatus.DRAFT);
        when(scenarioRepo.findById(scenarioId)).thenReturn(Optional.of(scenario));

        assertThrows(IllegalStateException.class,
                () -> service.startExecution(scenarioId, "user_1", "{}"));
        verify(executionRepo, never()).save(any());
    }

    @Test
    void startExecution_workflowIdIsUnique() {
        UUID scenarioId = UUID.randomUUID();
        Scenario scenario = buildScenario(scenarioId, ScenarioStatus.ACTIVE);
        when(scenarioRepo.findById(scenarioId)).thenReturn(Optional.of(scenario));

        ArgumentCaptor<ScenarioExecution> execCaptor = ArgumentCaptor.forClass(ScenarioExecution.class);
        when(executionRepo.save(execCaptor.capture())).thenAnswer(inv -> {
            ScenarioExecution e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(workflowClient.newWorkflowStub(eq(ScenarioWorkflow.class), any(WorkflowOptions.class)))
                .thenReturn(mock(ScenarioWorkflow.class));

        service.startExecution(scenarioId, "user_1", "{}");
        service.startExecution(scenarioId, "user_1", "{}");

        List<ScenarioExecution> allCaptured = execCaptor.getAllValues();
        assertEquals(2, allCaptured.size());
        assertNotEquals(allCaptured.get(0).getWorkflowId(), allCaptured.get(1).getWorkflowId());
    }

    // ── listAll / findById ─────────────────────────────────────────────────────

    @Test
    void listAll_delegatesToRepo() {
        UUID id = UUID.randomUUID();
        when(scenarioRepo.findAll()).thenReturn(List.of(buildScenario(id, ScenarioStatus.DRAFT)));

        List<Scenario> result = service.listAll();

        assertEquals(1, result.size());
        verify(scenarioRepo).findAll();
    }

    @Test
    void findById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(scenarioRepo.findById(id)).thenReturn(Optional.empty());

        assertThrows(Exception.class, () -> service.findById(id));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Scenario buildScenario(UUID id, ScenarioStatus status) {
        Scenario s = Scenario.builder()
                .name("test-scenario")
                .definition(VALID_GRAPH)
                .status(status)
                .triggerTopic("user.registered")
                .triggerEventType("USER_REGISTERED")
                .build();
        s.setId(id);
        return s;
    }
}
