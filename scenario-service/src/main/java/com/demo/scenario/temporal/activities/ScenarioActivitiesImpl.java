package com.demo.scenario.temporal.activities;

import com.demo.scenario.domain.ExecutionStatus;
import com.demo.scenario.repository.ScenarioExecutionRepository;
import com.demo.scenario.repository.ScenarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class ScenarioActivitiesImpl implements ScenarioActivities {

    private static final Logger log = LoggerFactory.getLogger(ScenarioActivitiesImpl.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ScenarioRepository scenarioRepo;
    private final ScenarioExecutionRepository executionRepo;

    public ScenarioActivitiesImpl(KafkaTemplate<String, String> kafkaTemplate,
                                   ScenarioRepository scenarioRepo,
                                   ScenarioExecutionRepository executionRepo) {
        this.kafkaTemplate = kafkaTemplate;
        this.scenarioRepo  = scenarioRepo;
        this.executionRepo = executionRepo;
    }

    @Override
    public SendResult sendEmail(String userId, String templateId, String payload) {
        log.info("[ACTIVITY] sendEmail userId={} template={}", userId, templateId);
        String messageId = UUID.randomUUID().toString();
        String json = "{\"channel\":\"email\",\"userId\":\"" + userId +
                "\",\"messageId\":\"" + messageId +
                "\",\"templateId\":\"" + templateId +
                "\",\"payload\":" + (payload == null ? "{}" : payload) + "}";
        try {
            kafkaTemplate.send("comm.outbound", userId, json).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish email to comm.outbound: " + e.getMessage(), e);
        }
        return new SendResult(messageId, true, "email");
    }

    @Override
    public SendResult sendPush(String userId, String templateId, String payload) {
        log.info("[ACTIVITY] sendPush userId={} template={}", userId, templateId);
        String messageId = UUID.randomUUID().toString();
        String json = "{\"channel\":\"push\",\"userId\":\"" + userId +
                "\",\"messageId\":\"" + messageId +
                "\",\"templateId\":\"" + templateId +
                "\",\"payload\":" + (payload == null ? "{}" : payload) + "}";
        try {
            kafkaTemplate.send("comm.outbound", userId, json).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish push to comm.outbound: " + e.getMessage(), e);
        }
        return new SendResult(messageId, true, "push");
    }

    @Override
    public SendResult sendSms(String userId, String templateId, String payload) {
        log.info("[ACTIVITY] sendSms userId={} template={}", userId, templateId);
        String messageId = UUID.randomUUID().toString();
        String json = "{\"channel\":\"sms\",\"userId\":\"" + userId +
                "\",\"messageId\":\"" + messageId +
                "\",\"templateId\":\"" + templateId +
                "\",\"payload\":" + (payload == null ? "{}" : payload) + "}";
        try {
            kafkaTemplate.send("comm.outbound", userId, json).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to publish sms to comm.outbound: " + e.getMessage(), e);
        }
        return new SendResult(messageId, true, "sms");
    }

    @Override
    public void updateExecutionNode(String executionId, String nodeId, String status) {
        executionRepo.findById(UUID.fromString(executionId)).ifPresent(exec -> {
            exec.setCurrentNodeId(nodeId);
            if ("COMPLETED".equals(status)) {
                exec.setStatus(ExecutionStatus.COMPLETED);
                exec.setFinishedAt(Instant.now());
            } else if ("FAILED".equals(status)) {
                exec.setStatus(ExecutionStatus.FAILED);
                exec.setFinishedAt(Instant.now());
            }
            executionRepo.save(exec);
        });
    }

    @Override
    public String loadScenarioDefinition(String scenarioId) {
        return scenarioRepo.findById(UUID.fromString(scenarioId))
                .map(s -> s.getDefinition())
                .orElseThrow(() -> new RuntimeException("Scenario not found: " + scenarioId));
    }
}
