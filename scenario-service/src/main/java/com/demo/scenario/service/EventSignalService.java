package com.demo.scenario.service;

import com.demo.scenario.domain.ExecutionStatus;
import com.demo.scenario.domain.ScenarioExecution;
import com.demo.scenario.repository.ScenarioExecutionRepository;
import com.demo.scenario.temporal.workflows.IncomingEvent;
import com.demo.scenario.temporal.workflows.ScenarioWorkflow;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventSignalService {

    private static final Logger log = LoggerFactory.getLogger(EventSignalService.class);

    private final WorkflowClient workflowClient;
    private final ScenarioExecutionRepository executionRepo;
    private final ObjectMapper objectMapper;

    public EventSignalService(WorkflowClient workflowClient,
                               ScenarioExecutionRepository executionRepo,
                               ObjectMapper objectMapper) {
        this.workflowClient = workflowClient;
        this.executionRepo  = executionRepo;
        this.objectMapper   = objectMapper;
    }

    public void routeEventToWorkflows(String userId, String eventType, String topic, String payload) {
        List<ScenarioExecution> running = executionRepo.findByUserId(userId).stream()
                .filter(e -> e.getStatus() == ExecutionStatus.RUNNING)
                .toList();

        if (running.isEmpty()) {
            log.debug("No running executions for userId={}, skipping signal", userId);
            return;
        }

        // Carry messageId from payload so workflows can match delivery events to the
        // specific message they sent — prevents cross-contamination between unrelated comms.
        String messageId = extractField(payload, "messageId");
        IncomingEvent signal = new IncomingEvent(eventType, topic, userId, payload, messageId);

        for (ScenarioExecution exec : running) {
            try {
                ScenarioWorkflow wf = workflowClient.newWorkflowStub(
                        ScenarioWorkflow.class, exec.getWorkflowId());
                wf.onEvent(signal);
                log.info("Signalled workflow {} with event {}", exec.getWorkflowId(), eventType);
            } catch (WorkflowNotFoundException ex) {
                log.warn("Workflow {} not found (already completed?)", exec.getWorkflowId());
            } catch (Exception ex) {
                log.error("Failed to signal workflow {}: {}", exec.getWorkflowId(), ex.getMessage());
            }
        }
    }
    private String extractField(String json, String field) {
        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode val = node.get(field);
            return (val != null && !val.isNull()) ? val.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
