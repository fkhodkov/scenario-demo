package com.demo.scenario.temporal.workflows;

import com.demo.scenario.dto.ScenarioGraph;
import com.demo.scenario.dto.ScenarioGraph.ScenarioEdge;
import com.demo.scenario.dto.ScenarioGraph.ScenarioNode;
import com.demo.scenario.temporal.activities.ScenarioActivities;
import com.demo.scenario.temporal.activities.SendResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ScenarioWorkflowImpl implements ScenarioWorkflow {

    private static final Logger log = Workflow.getLogger(ScenarioWorkflowImpl.class);

    private final List<IncomingEvent> eventQueue = new ArrayList<>();

    private final ScenarioActivities activities = Workflow.newActivityStub(
            ScenarioActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .build())
                    .build());

    @Override
    public void onEvent(IncomingEvent event) {
        log.debug("[SIGNAL] received event={} user={}", event.getEventType(), event.getUserId());
        eventQueue.add(event);
    }

    @Override
    public void execute(ScenarioWorkflowInput input) {
        log.info("[WORKFLOW] starting scenarioId={} userId={}", input.getScenarioId(), input.getUserId());

        String definitionJson = activities.loadScenarioDefinition(input.getScenarioId().toString());
        ScenarioGraph graph = parseGraph(definitionJson);

        if (graph == null || graph.getNodes() == null) {
            log.error("Empty graph for scenario {}", input.getScenarioId());
            return;
        }

        Map<String, ScenarioNode> nodeMap = graph.getNodes().stream()
                .collect(Collectors.toMap(ScenarioNode::getId, Function.identity()));

        ScenarioNode current = graph.getNodes().stream()
                .filter(n -> "TRIGGER".equals(n.getType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No TRIGGER node"));

        String executionId = input.getExecutionId();

        while (current != null && !"END".equals(current.getType())) {
            activities.updateExecutionNode(executionId, current.getId(), "RUNNING");
            current = processNode(current, graph, nodeMap, input, executionId);
        }

        String finalNodeId = current != null ? current.getId() : "end";
        activities.updateExecutionNode(executionId, finalNodeId, "COMPLETED");
        log.info("[WORKFLOW] completed scenarioId={} userId={}", input.getScenarioId(), input.getUserId());
    }

    // ── Node processing ───────────────────────────────────────────────────────

    private ScenarioNode processNode(ScenarioNode node, ScenarioGraph graph,
                                     Map<String, ScenarioNode> nodeMap,
                                     ScenarioWorkflowInput input, String executionId) {
        String type = node.getType() == null ? "" : node.getType();
        switch (type) {
            case "TRIGGER":
                return followDefaultEdge(node, graph, nodeMap);

            case "SEND_EMAIL": {
                String templateId = node.getData() != null ? node.getData().getTemplateId() : null;
                SendResult result = activities.sendEmail(input.getUserId(), templateId,
                        input.getTriggerEventPayload());
                return waitForDelivery(result, node, graph, nodeMap, input);
            }
            case "SEND_PUSH": {
                String templateId = node.getData() != null ? node.getData().getTemplateId() : null;
                SendResult result = activities.sendPush(input.getUserId(), templateId,
                        input.getTriggerEventPayload());
                return waitForDelivery(result, node, graph, nodeMap, input);
            }
            case "SEND_SMS": {
                String templateId = node.getData() != null ? node.getData().getTemplateId() : null;
                SendResult result = activities.sendSms(input.getUserId(), templateId,
                        input.getTriggerEventPayload());
                return waitForDelivery(result, node, graph, nodeMap, input);
            }
            case "WAIT_EVENT":
                return processWaitEvent(node, graph, nodeMap);

            case "DELAY": {
                long seconds = node.getData() != null ? node.getData().getDelaySeconds() : 60;
                if (seconds <= 0) seconds = 60;
                Workflow.sleep(Duration.ofSeconds(seconds));
                return followDefaultEdge(node, graph, nodeMap);
            }
            case "CONDITION":
                return processCondition(node, graph, nodeMap);

            case "END":
                return null;

            default:
                log.warn("Unknown node type: {}", type);
                return followDefaultEdge(node, graph, nodeMap);
        }
    }

    private ScenarioNode waitForDelivery(SendResult sent, ScenarioNode sendNode,
                                          ScenarioGraph graph, Map<String, ScenarioNode> nodeMap,
                                          ScenarioWorkflowInput input) {
        if (!sent.isAccepted()) {
            return followEdgeByHandle(sendNode, graph, nodeMap, "failed");
        }

        String channel        = sent.getChannel();
        String deliveredEvent = channel.toUpperCase() + "_DELIVERED";
        String failedEvent    = channel.toUpperCase() + "_FAILED";

        long timeoutSeconds = findDownstreamTimeout(sendNode, graph, nodeMap, 86400L);

        AtomicReference<String> outcome = new AtomicReference<>(null);

        Workflow.await(Duration.ofSeconds(timeoutSeconds), () -> {
            for (IncomingEvent e : eventQueue) {
                if (!input.getUserId().equals(e.getUserId())) continue;
                if (deliveredEvent.equalsIgnoreCase(e.getEventType())) {
                    outcome.set("delivered");
                    eventQueue.remove(e);
                    return true;
                }
                if (failedEvent.equalsIgnoreCase(e.getEventType())) {
                    outcome.set("failed");
                    eventQueue.remove(e);
                    return true;
                }
            }
            return false;
        });

        if (outcome.get() == null) outcome.set("timeout");

        switch (outcome.get()) {
            case "delivered": return followEdgeByHandle(sendNode, graph, nodeMap, "delivered");
            case "failed":    return followEdgeByHandle(sendNode, graph, nodeMap, "failed");
            default:          return followEdgeByHandle(sendNode, graph, nodeMap, "timeout");
        }
    }

    private ScenarioNode processWaitEvent(ScenarioNode node, ScenarioGraph graph,
                                           Map<String, ScenarioNode> nodeMap) {
        String expectedType  = node.getData() != null ? node.getData().getEventType() : null;
        long timeoutSeconds  = node.getData() != null ? node.getData().getTimeoutSeconds() : 3600;
        if (timeoutSeconds <= 0) timeoutSeconds = 3600;

        boolean[] arrived = { false };

        Workflow.await(Duration.ofSeconds(timeoutSeconds), () -> {
            for (IncomingEvent e : eventQueue) {
                if (expectedType == null || expectedType.equalsIgnoreCase(e.getEventType())) {
                    arrived[0] = true;
                    eventQueue.remove(e);
                    return true;
                }
            }
            return false;
        });

        if (arrived[0]) {
            ScenarioNode next = followEdgeByHandle(node, graph, nodeMap, "event");
            return next != null ? next : followDefaultEdge(node, graph, nodeMap);
        } else {
            ScenarioNode next = followEdgeByHandle(node, graph, nodeMap, "timeout");
            return next != null ? next : followDefaultEdge(node, graph, nodeMap);
        }
    }

    private ScenarioNode processCondition(ScenarioNode node, ScenarioGraph graph,
                                           Map<String, ScenarioNode> nodeMap) {
        ScenarioNode trueBranch = followEdgeByHandle(node, graph, nodeMap, "true");
        return trueBranch != null ? trueBranch : followEdgeByHandle(node, graph, nodeMap, "false");
    }

    // ── Edge helpers ─────────────────────────────────────────────────────────

    private ScenarioNode followDefaultEdge(ScenarioNode node, ScenarioGraph graph,
                                            Map<String, ScenarioNode> nodeMap) {
        return outgoing(node, graph).stream()
                .filter(e -> e.getSourceHandle() == null
                          || "default".equalsIgnoreCase(e.getSourceHandle()))
                .findFirst()
                .map(e -> nodeMap.get(e.getTarget()))
                .orElse(null);
    }

    private ScenarioNode followEdgeByHandle(ScenarioNode node, ScenarioGraph graph,
                                             Map<String, ScenarioNode> nodeMap, String handle) {
        Optional<ScenarioNode> explicit = outgoing(node, graph).stream()
                .filter(e -> handle.equalsIgnoreCase(e.getSourceHandle()))
                .findFirst()
                .map(e -> nodeMap.get(e.getTarget()));
        return explicit.orElseGet(() -> followDefaultEdge(node, graph, nodeMap));
    }

    private List<ScenarioEdge> outgoing(ScenarioNode node, ScenarioGraph graph) {
        return graph.getEdges().stream()
                .filter(e -> node.getId().equals(e.getSource()))
                .collect(Collectors.toList());
    }

    private long findDownstreamTimeout(ScenarioNode sendNode, ScenarioGraph graph,
                                        Map<String, ScenarioNode> nodeMap, long defaultValue) {
        return outgoing(sendNode, graph).stream()
                .map(e -> nodeMap.get(e.getTarget()))
                .filter(n -> n != null && "WAIT_EVENT".equals(n.getType()))
                .mapToLong(n -> n.getData() != null && n.getData().getTimeoutSeconds() > 0
                        ? n.getData().getTimeoutSeconds() : defaultValue)
                .findFirst()
                .orElse(defaultValue);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private ScenarioGraph parseGraph(String json) {
        try {
            return new ObjectMapper().readValue(json, ScenarioGraph.class);
        } catch (Exception e) {
            log.error("Failed to parse scenario graph: {}", e.getMessage());
            return null;
        }
    }
}
