package com.demo.scenario.kafka;

import com.demo.scenario.domain.Scenario;
import com.demo.scenario.domain.ScenarioStatus;
import com.demo.scenario.repository.ScenarioRepository;
import com.demo.scenario.service.EventSignalService;
import com.demo.scenario.service.ScenarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UniversalEventConsumerTest {

    @Mock ScenarioService scenarioService;
    @Mock EventSignalService signalService;
    @Mock ScenarioRepository scenarioRepo;

    UniversalEventConsumer consumer;
    ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        consumer = new UniversalEventConsumer(scenarioService, signalService, scenarioRepo, mapper);
    }

    // ── Trigger matching ──────────────────────────────────────────────────────

    @Test
    void consume_activeScenarioOnTopic_startsExecution() throws Exception {
        UUID scenarioId = UUID.randomUUID();
        Scenario scenario = buildScenario(scenarioId, "user.registered",
                "USER_REGISTERED", ScenarioStatus.ACTIVE);
        when(scenarioRepo.findByTriggerTopicAndStatus("user.registered", ScenarioStatus.ACTIVE))
                .thenReturn(List.of(scenario));

        String payload = "{\"userId\":\"user_1\",\"eventType\":\"USER_REGISTERED\"}";
        consumer.consume(record("user.registered", "user_1", payload));

        verify(scenarioService).startExecution(eq(scenarioId), eq("user_1"), eq(payload));
    }

    @Test
    void consume_noActiveScenarioForTopic_doesNotStartExecution() throws Exception {
        when(scenarioRepo.findByTriggerTopicAndStatus("user.registered", ScenarioStatus.ACTIVE))
                .thenReturn(List.of());

        String payload = "{\"userId\":\"user_1\",\"eventType\":\"USER_REGISTERED\"}";
        consumer.consume(record("user.registered", "user_1", payload));

        verify(scenarioService, never()).startExecution(any(), any(), any());
    }

    @Test
    void consume_noScenarioForTopic_onlySignals() throws Exception {
        when(scenarioRepo.findByTriggerTopicAndStatus("email.opened", ScenarioStatus.ACTIVE))
                .thenReturn(List.of());

        String payload = "{\"userId\":\"user_1\",\"messageId\":\"msg_1\"}";
        consumer.consume(record("email.opened", "user_1", payload));

        verify(scenarioService, never()).startExecution(any(), any(), any());
        verify(signalService).routeEventToWorkflows(eq("user_1"), eq("EMAIL_OPENED"),
                eq("email.opened"), eq(payload));
    }

    @Test
    void consume_eventTypeMismatch_doesNotStartExecution() throws Exception {
        UUID scenarioId = UUID.randomUUID();
        Scenario scenario = buildScenario(scenarioId, "user.registered",
                "USER_REGISTERED", ScenarioStatus.ACTIVE);
        when(scenarioRepo.findByTriggerTopicAndStatus("user.registered", ScenarioStatus.ACTIVE))
                .thenReturn(List.of(scenario));

        // Payload has a different eventType than the scenario expects
        String payload = "{\"userId\":\"user_1\",\"eventType\":\"USER_UPDATED\"}";
        consumer.consume(record("user.registered", "user_1", payload));

        verify(scenarioService, never()).startExecution(any(), any(), any());
    }

    @Test
    void consume_multipleActiveScenarios_startsAll() throws Exception {
        UUID id1 = UUID.randomUUID(), id2 = UUID.randomUUID();
        Scenario s1 = buildScenario(id1, "user.registered", "USER_REGISTERED", ScenarioStatus.ACTIVE);
        Scenario s2 = buildScenario(id2, "user.registered", "USER_REGISTERED", ScenarioStatus.ACTIVE);
        when(scenarioRepo.findByTriggerTopicAndStatus("user.registered", ScenarioStatus.ACTIVE))
                .thenReturn(List.of(s1, s2));

        String payload = "{\"userId\":\"user_1\",\"eventType\":\"USER_REGISTERED\"}";
        consumer.consume(record("user.registered", "user_1", payload));

        verify(scenarioService).startExecution(eq(id1), eq("user_1"), eq(payload));
        verify(scenarioService).startExecution(eq(id2), eq("user_1"), eq(payload));
    }

    @Test
    void consume_nullPayload_isIgnored() {
        consumer.consume(record("user.registered", null, null));
        verifyNoInteractions(scenarioService, signalService, scenarioRepo);
    }

    @Test
    void consume_blankPayload_isIgnored() {
        consumer.consume(record("user.registered", null, "   "));
        verifyNoInteractions(scenarioService, signalService, scenarioRepo);
    }

    @Test
    void consume_noUserIdInPayload_isIgnored() throws Exception {
        String payload = "{\"eventType\":\"USER_REGISTERED\"}"; // no userId
        consumer.consume(record("user.registered", null, payload));
        verifyNoInteractions(scenarioService, signalService);
    }

    @Test
    void consume_malformedJson_isIgnored() {
        consumer.consume(record("user.registered", null, "not-json{{{"));
        verifyNoInteractions(scenarioService, signalService);
    }

    @Test
    void consume_alwaysSignalsRunningWorkflows() throws Exception {
        when(scenarioRepo.findByTriggerTopicAndStatus(any(), eq(ScenarioStatus.ACTIVE)))
                .thenReturn(List.of());

        String payload = "{\"userId\":\"user_99\",\"messageId\":\"m1\"}";
        consumer.consume(record("email.delivered", "user_99", payload));

        verify(signalService).routeEventToWorkflows(
                eq("user_99"), eq("EMAIL_DELIVERED"), eq("email.delivered"), eq(payload));
    }

    @Test
    void consume_startExecution_throws_doesNotPropagateException() throws Exception {
        UUID scenarioId = UUID.randomUUID();
        Scenario scenario = buildScenario(scenarioId, "user.registered",
                "USER_REGISTERED", ScenarioStatus.ACTIVE);
        when(scenarioRepo.findByTriggerTopicAndStatus("user.registered", ScenarioStatus.ACTIVE))
                .thenReturn(List.of(scenario));
        when(scenarioService.startExecution(any(), any(), any()))
                .thenThrow(new RuntimeException("Temporal unavailable"));

        String payload = "{\"userId\":\"user_1\",\"eventType\":\"USER_REGISTERED\"}";
        // Should not throw — consumer must not fail and cause Kafka offset not to commit
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> consumer.consume(record("user.registered", "user_1", payload)));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ConsumerRecord<String, String> record(String topic, String key, String value) {
        return new ConsumerRecord<>(topic, 0, 0L, key, value);
    }

    private Scenario buildScenario(UUID id, String topic, String eventType,
                                    ScenarioStatus status) {
        Scenario s = Scenario.builder()
                .name("test").definition("{}")
                .status(status)
                .triggerTopic(topic)
                .triggerEventType(eventType)
                .build();
        s.setId(id);
        return s;
    }
}
