package com.demo.scenario.integration;

import com.demo.scenario.domain.*;
import com.demo.scenario.repository.ScenarioExecutionRepository;
import com.demo.scenario.repository.ScenarioRepository;
import com.demo.scenario.temporal.workflows.ScenarioWorkflow;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.worker.WorkerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full Spring Boot integration test: real JPA (Testcontainers PostgreSQL), real REST layer,
 * embedded Kafka. Temporal is mocked — no Temporal server needed.
 *
 * IT suffix → picked up by maven-failsafe (integration-test phase).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = {"user.registered", "email.delivered", "email.failed",
              "email.opened", "comm.outbound"},
    brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"}
)
@DirtiesContext
class ScenarioApiIT {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ScenarioRepository scenarioRepo;
    @Autowired ScenarioExecutionRepository executionRepo;

    // Mock Temporal so the full context starts without a live Temporal server
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

    // ── Scenario CRUD via REST ────────────────────────────────────────────────

    @Test
    void createScenario_persistsAndReturns() throws Exception {
        mockMvc.perform(post("/api/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name",        "welcome",
                                "description", "onboarding flow",
                                "definition",  GRAPH))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id",          notNullValue()))
                .andExpect(jsonPath("$.name",         is("welcome")))
                .andExpect(jsonPath("$.status",       is("DRAFT")))
                .andExpect(jsonPath("$.triggerTopic", is("user.registered")));

        assertEquals(1, scenarioRepo.count());
    }

    @Test
    void listScenarios_returnsAllPersisted() throws Exception {
        createViaApi("s1");
        createViaApi("s2");

        mockMvc.perform(get("/api/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].name", containsInAnyOrder("s1", "s2")));
    }

    @Test
    void getScenario_returnsById() throws Exception {
        String created = createViaApi("findme");
        String id = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(get("/api/scenarios/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("findme")));
    }

    @Test
    void updateScenario_modifiesName() throws Exception {
        String created = createViaApi("original");
        String id = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(put("/api/scenarios/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name",        "updated",
                                "description", "updated desc",
                                "definition",  GRAPH))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("updated")));
    }

    @Test
    void activateScenario_changesStatusToActive() throws Exception {
        String created = createViaApi("activate-me");
        String id = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/scenarios/" + id + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));

        Scenario inDb = scenarioRepo.findById(java.util.UUID.fromString(id)).orElseThrow();
        assertEquals(ScenarioStatus.ACTIVE, inDb.getStatus());
    }

    @Test
    void pauseScenario_changesStatusToPaused() throws Exception {
        String created = createViaApi("pause-me");
        String id = objectMapper.readTree(created).get("id").asText();

        mockMvc.perform(post("/api/scenarios/" + id + "/activate")).andExpect(status().isOk());

        mockMvc.perform(post("/api/scenarios/" + id + "/pause"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAUSED")));
    }

    // ── Execution via REST ────────────────────────────────────────────────────

    @Test
    void executeScenario_createsExecutionRecord() throws Exception {
        String created = createViaApi("run-me");
        String id = objectMapper.readTree(created).get("id").asText();
        mockMvc.perform(post("/api/scenarios/" + id + "/activate")).andExpect(status().isOk());

        // Use thenAnswer to return a fresh mock per call — avoids Temporal SDK
        // treating the same stub instance as "already started" on repeated calls.
        when(workflowClient.newWorkflowStub(
                org.mockito.ArgumentMatchers.eq(ScenarioWorkflow.class),
                org.mockito.ArgumentMatchers.<WorkflowOptions>any()))
                .thenAnswer(inv -> {
                    ScenarioWorkflow wf = mock(ScenarioWorkflow.class);
                    doNothing().when(wf).execute(any());
                    return wf;
                });

        mockMvc.perform(post("/api/scenarios/" + id + "/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user_77\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId",     is("user_77")))
                .andExpect(jsonPath("$.status",     is("RUNNING")))
                .andExpect(jsonPath("$.workflowId", notNullValue()));

        assertEquals(1, executionRepo.count());
    }

    @Test
    void executeScenario_draftStatus_returns500() throws Exception {
        String created = createViaApi("draft-only");
        String id = objectMapper.readTree(created).get("id").asText();
        // Intentionally NOT activating

        mockMvc.perform(post("/api/scenarios/" + id + "/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"user_1\"}"))
                .andExpect(status().is5xxServerError());

        assertEquals(0, executionRepo.count());
    }

    @Test
    void listExecutions_returnsForScenario() throws Exception {
        String created = createViaApi("multi-exec");
        String id = objectMapper.readTree(created).get("id").asText();
        mockMvc.perform(post("/api/scenarios/" + id + "/activate")).andExpect(status().isOk());

        // Return a fresh mock per call — Temporal's SDK may flag the same stub instance
        // as "already started" on a second WorkflowClient.start() call.
        when(workflowClient.newWorkflowStub(
                org.mockito.ArgumentMatchers.eq(ScenarioWorkflow.class),
                org.mockito.ArgumentMatchers.<WorkflowOptions>any()))
                .thenAnswer(inv -> {
                    ScenarioWorkflow wf = mock(ScenarioWorkflow.class);
                    doNothing().when(wf).execute(any());
                    return wf;
                });

        mockMvc.perform(post("/api/scenarios/" + id + "/execute")
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"u1\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/scenarios/" + id + "/execute")
                .contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"u2\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/scenarios/" + id + "/executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void eventsCatalog_returnsKnownEvents() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].eventType", hasItems(
                        "USER_REGISTERED", "EMAIL_DELIVERED", "EMAIL_FAILED",
                        "EMAIL_OPENED",    "LINK_CLICKED",    "PUSH_DELIVERED")));
    }

    @Test
    void createScenario_invalidJson_returns500() throws Exception {
        mockMvc.perform(post("/api/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name",       "bad",
                                "description","",
                                "definition", "not-valid-json{{"))))
                .andExpect(status().is5xxServerError());

        assertEquals(0, scenarioRepo.count());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String createViaApi(String name) throws Exception {
        return mockMvc.perform(post("/api/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name",        name,
                                "description", "desc",
                                "definition",  GRAPH))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
    private void assertEquals(long expected, long actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
