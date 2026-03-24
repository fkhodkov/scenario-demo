package com.demo.scenario.web;

import com.demo.scenario.domain.*;
import com.demo.scenario.service.ScenarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScenarioController.class)
class ScenarioControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ScenarioService scenarioService;

    // ── GET /api/events ───────────────────────────────────────────────────────

    @Test
    void getEvents_returnsEventCatalog() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(5))))
                .andExpect(jsonPath("$[*].eventType", hasItem("USER_REGISTERED")))
                .andExpect(jsonPath("$[*].topic",     hasItem("user.registered")));
    }

    // ── GET /api/scenarios ────────────────────────────────────────────────────

    @Test
    void listScenarios_returnsAll() throws Exception {
        UUID id = UUID.randomUUID();
        Scenario s = buildScenario(id, "onboarding", ScenarioStatus.ACTIVE);
        when(scenarioService.listAll()).thenReturn(List.of(s));

        mockMvc.perform(get("/api/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("onboarding")))
                .andExpect(jsonPath("$[0].status", is("ACTIVE")));
    }

    @Test
    void listScenarios_empty_returnsEmptyArray() throws Exception {
        when(scenarioService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/scenarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── GET /api/scenarios/{id} ───────────────────────────────────────────────

    @Test
    void getScenario_existing_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(scenarioService.findById(id)).thenReturn(buildScenario(id, "test", ScenarioStatus.DRAFT));

        mockMvc.perform(get("/api/scenarios/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("test")));
    }

    @Test
    void getScenario_notFound_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        when(scenarioService.findById(id)).thenThrow(new RuntimeException("not found"));

        mockMvc.perform(get("/api/scenarios/" + id))
                .andExpect(status().is5xxServerError());
    }

    // ── POST /api/scenarios ───────────────────────────────────────────────────

    @Test
    void createScenario_validRequest_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(scenarioService.create(eq("flow"), eq("desc"), anyString()))
                .thenReturn(buildScenario(id, "flow", ScenarioStatus.DRAFT));

        Map<String, String> body = Map.of(
                "name",       "flow",
                "description","desc",
                "definition", "{\"nodes\":[],\"edges\":[]}");

        mockMvc.perform(post("/api/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("flow")));
    }

    // ── POST /api/scenarios/{id}/activate ────────────────────────────────────

    @Test
    void activateScenario_returns200WithActiveStatus() throws Exception {
        UUID id = UUID.randomUUID();
        Scenario active = buildScenario(id, "s", ScenarioStatus.ACTIVE);
        when(scenarioService.activate(id)).thenReturn(active);

        mockMvc.perform(post("/api/scenarios/" + id + "/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ACTIVE")));
    }

    // ── POST /api/scenarios/{id}/pause ───────────────────────────────────────

    @Test
    void pauseScenario_returns200WithPausedStatus() throws Exception {
        UUID id = UUID.randomUUID();
        Scenario paused = buildScenario(id, "s", ScenarioStatus.PAUSED);
        when(scenarioService.pause(id)).thenReturn(paused);

        mockMvc.perform(post("/api/scenarios/" + id + "/pause"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PAUSED")));
    }

    // ── POST /api/scenarios/{id}/execute ─────────────────────────────────────

    @Test
    void executeScenario_returns200WithExecution() throws Exception {
        UUID id    = UUID.randomUUID();
        UUID execId = UUID.randomUUID();

        Scenario s = buildScenario(id, "s", ScenarioStatus.ACTIVE);
        ScenarioExecution exec = ScenarioExecution.builder()
                .scenario(s).userId("user_1").workflowId("wf-x")
                .status(ExecutionStatus.RUNNING).currentNodeId("start").build();
        exec.setId(execId);

        when(scenarioService.startExecution(eq(id), eq("user_1"), any())).thenReturn(exec);

        Map<String, String> body = Map.of("userId", "user_1");

        mockMvc.perform(post("/api/scenarios/" + id + "/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("user_1")))
                .andExpect(jsonPath("$.status", is("RUNNING")));
    }

    @Test
    void executeScenario_inactiveScenario_returns500() throws Exception {
        UUID id = UUID.randomUUID();
        when(scenarioService.startExecution(eq(id), any(), any()))
                .thenThrow(new IllegalStateException("Scenario is not ACTIVE"));

        Map<String, String> body = Map.of("userId", "user_1");

        mockMvc.perform(post("/api/scenarios/" + id + "/execute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is5xxServerError());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Scenario buildScenario(UUID id, String name, ScenarioStatus status) {
        Scenario s = Scenario.builder()
                .name(name).description("desc")
                .definition("{\"nodes\":[],\"edges\":[]}")
                .status(status)
                .triggerTopic("user.registered")
                .triggerEventType("USER_REGISTERED")
                .build();
        s.setId(id);
        return s;
    }
}
