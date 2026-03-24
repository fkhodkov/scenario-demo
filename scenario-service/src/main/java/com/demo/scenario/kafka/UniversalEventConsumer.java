package com.demo.scenario.kafka;

import com.demo.events.spec.EventRegistry;
import com.demo.scenario.domain.ScenarioStatus;
import com.demo.scenario.repository.ScenarioRepository;
import com.demo.scenario.service.EventSignalService;
import com.demo.scenario.service.ScenarioService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UniversalEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UniversalEventConsumer.class);

    private final ScenarioService scenarioService;
    private final EventSignalService signalService;
    private final ScenarioRepository scenarioRepo;
    private final ObjectMapper objectMapper;

    public UniversalEventConsumer(ScenarioService scenarioService,
                                   EventSignalService signalService,
                                   ScenarioRepository scenarioRepo,
                                   ObjectMapper objectMapper) {
        this.scenarioService = scenarioService;
        this.signalService   = signalService;
        this.scenarioRepo    = scenarioRepo;
        this.objectMapper    = objectMapper;
    }

    @KafkaListener(
        topicPattern = ".*",
        groupId = "scenario-universal-consumer",
        concurrency = "3"
    )
    public void consume(ConsumerRecord<String, String> record) {
        String topic   = record.topic();
        String payload = record.value();
        if (payload == null || payload.isBlank()) return;

        log.debug("Consumed topic={} key={}", topic, record.key());

        String userId = extractUserId(payload);
        if (userId == null) {
            log.debug("No userId in message on topic={}, skipping", topic);
            return;
        }

        // Derive eventType from the registry (topic -> canonical name)
        String eventType = EventRegistry.findByTopic(topic)
                .map(EventRegistry.EventSpec::eventType)
                .orElse(topic.toUpperCase().replace(".", "_"));

        // Also read eventType from the payload itself — some producers stamp it explicitly.
        // If present it takes precedence for trigger matching (allows one topic to carry
        // multiple logical event types).
        String payloadEventType = extractEventType(payload);
        String effectiveEventType = payloadEventType != null ? payloadEventType : eventType;

        // 1. Start new workflow if this topic triggers an active scenario
        scenarioRepo.findByTriggerTopic(topic).ifPresent(scenario -> {
            if (scenario.getStatus() == ScenarioStatus.ACTIVE) {
                String triggerEventType = scenario.getTriggerEventType();
                if (triggerEventType == null || triggerEventType.equalsIgnoreCase(effectiveEventType)) {
                    try {
                        scenarioService.startExecution(scenario.getId(), userId, payload);
                    } catch (Exception e) {
                        log.error("Failed to start execution for scenario {} user {}: {}",
                                scenario.getId(), userId, e.getMessage());
                    }
                }
            }
        });

        // 2. Signal all running workflows for this user
        signalService.routeEventToWorkflows(userId, eventType, topic, payload);
    }

    private String extractEventType(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode et = node.get("eventType");
            return (et != null && !et.isNull()) ? et.asText().trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractUserId(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode uid = node.get("userId");
            return (uid != null && !uid.isNull()) ? uid.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
