package com.demo.scenario.temporal.activities;

import com.demo.scenario.domain.ExecutionStatus;
import com.demo.scenario.domain.Scenario;
import com.demo.scenario.domain.ScenarioExecution;
import com.demo.scenario.domain.ScenarioStatus;
import com.demo.scenario.repository.ScenarioExecutionRepository;
import com.demo.scenario.repository.ScenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScenarioActivitiesImplTest {

    @Mock KafkaTemplate<String, String> kafkaTemplate;
    @Mock ScenarioRepository scenarioRepo;
    @Mock ScenarioExecutionRepository executionRepo;

    ScenarioActivitiesImpl activities;

    @BeforeEach
    void setUp() {
        activities = new ScenarioActivitiesImpl(kafkaTemplate, scenarioRepo, executionRepo);
    }

    // ── sendEmail ─────────────────────────────────────────────────────────────

    @Test
    void sendEmail_publishesToOutboundTopic() {
        SendResult result = activities.sendEmail("user_1", "tmpl_welcome", "{\"key\":\"val\"}");

        verify(kafkaTemplate).send(eq("comm.outbound"), eq("user_1"), contains("\"channel\":\"email\""));
        assertTrue(result.isAccepted());
        assertEquals("email", result.getChannel());
        assertNotNull(result.getMessageId());
    }

    @Test
    void sendEmail_nullPayload_usesEmptyObject() {
        activities.sendEmail("user_1", "tmpl", null);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("comm.outbound"), eq("user_1"), captor.capture());
        assertTrue(captor.getValue().contains("\"payload\":{}"));
    }

    @Test
    void sendEmail_generatesUniqueMessageIds() {
        SendResult r1 = activities.sendEmail("u1", "t", null);
        SendResult r2 = activities.sendEmail("u1", "t", null);
        assertNotEquals(r1.getMessageId(), r2.getMessageId());
    }

    // ── sendPush ──────────────────────────────────────────────────────────────

    @Test
    void sendPush_publishesToOutboundTopic() {
        SendResult result = activities.sendPush("user_2", "tmpl_push", null);

        verify(kafkaTemplate).send(eq("comm.outbound"), eq("user_2"), contains("\"channel\":\"push\""));
        assertTrue(result.isAccepted());
        assertEquals("push", result.getChannel());
    }

    // ── sendSms ───────────────────────────────────────────────────────────────

    @Test
    void sendSms_publishesToOutboundTopic() {
        SendResult result = activities.sendSms("user_3", "tmpl_sms", null);

        verify(kafkaTemplate).send(eq("comm.outbound"), eq("user_3"), contains("\"channel\":\"sms\""));
        assertTrue(result.isAccepted());
        assertEquals("sms", result.getChannel());
    }

    // ── updateExecutionNode ───────────────────────────────────────────────────

    @Test
    void updateExecutionNode_updatesCurrentNodeId() {
        UUID execId = UUID.randomUUID();
        ScenarioExecution exec = buildExecution(execId, ExecutionStatus.RUNNING);
        when(executionRepo.findById(execId)).thenReturn(Optional.of(exec));

        activities.updateExecutionNode(execId.toString(), "node_5", "RUNNING");

        assertEquals("node_5", exec.getCurrentNodeId());
        verify(executionRepo).save(exec);
    }

    @Test
    void updateExecutionNode_completed_setsStatusAndFinishedAt() {
        UUID execId = UUID.randomUUID();
        ScenarioExecution exec = buildExecution(execId, ExecutionStatus.RUNNING);
        when(executionRepo.findById(execId)).thenReturn(Optional.of(exec));

        activities.updateExecutionNode(execId.toString(), "end", "COMPLETED");

        assertEquals(ExecutionStatus.COMPLETED, exec.getStatus());
        assertNotNull(exec.getFinishedAt());
        verify(executionRepo).save(exec);
    }

    @Test
    void updateExecutionNode_failed_setsStatusAndFinishedAt() {
        UUID execId = UUID.randomUUID();
        ScenarioExecution exec = buildExecution(execId, ExecutionStatus.RUNNING);
        when(executionRepo.findById(execId)).thenReturn(Optional.of(exec));

        activities.updateExecutionNode(execId.toString(), "node_err", "FAILED");

        assertEquals(ExecutionStatus.FAILED, exec.getStatus());
        assertNotNull(exec.getFinishedAt());
    }

    @Test
    void updateExecutionNode_notFound_doesNothing() {
        UUID execId = UUID.randomUUID();
        when(executionRepo.findById(execId)).thenReturn(Optional.empty());

        assertDoesNotThrow(() ->
                activities.updateExecutionNode(execId.toString(), "node_1", "RUNNING"));
        verify(executionRepo, never()).save(any());
    }

    // ── loadScenarioDefinition ────────────────────────────────────────────────

    @Test
    void loadScenarioDefinition_returnsDefinitionJson() {
        UUID id = UUID.randomUUID();
        Scenario s = Scenario.builder()
                .definition("{\"nodes\":[]}")
                .name("test")
                .status(ScenarioStatus.ACTIVE)
                .build();
        when(scenarioRepo.findById(id)).thenReturn(Optional.of(s));

        String result = activities.loadScenarioDefinition(id.toString());

        assertEquals("{\"nodes\":[]}", result);
    }

    @Test
    void loadScenarioDefinition_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(scenarioRepo.findById(id)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> activities.loadScenarioDefinition(id.toString()));
        assertTrue(ex.getMessage().contains(id.toString()));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ScenarioExecution buildExecution(UUID id, ExecutionStatus status) {
        ScenarioExecution e = ScenarioExecution.builder()
                .userId("user_x")
                .workflowId("wf-" + id)
                .status(status)
                .currentNodeId("node_1")
                .build();
        // simulate JPA-assigned id
        e.setId(id);
        return e;
    }
}
