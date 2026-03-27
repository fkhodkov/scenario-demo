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
        log.debug("[SIGNAL] received event={} user={}", event.eventType(), event.userId());
        eventQueue.add(event);
    }

    @Override
    public void execute(ScenarioWorkflowInput input) {
        log.info("[WORKFLOW] starting scenarioId={} userId={}", input.scenarioId(), input.userId());

        String definitionJson = activities.loadScenarioDefinition(input.scenarioId().toString());
        ScenarioGraph graph = parseGraph(definitionJson);

        if (graph == null || graph.nodes() == null) {
            log.error("Empty graph for scenario {}", input.scenarioId());
            return;
        }

        Map<String, ScenarioNode> nodeMap = graph.nodes().stream()
                .collect(Collectors.toMap(ScenarioNode::id, Function.identity()));

        ScenarioNode current = graph.nodes().stream()
                .filter(n -> "TRIGGER".equals(n.type()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No TRIGGER node"));

        String executionId = input.executionId();

        while (current != null && !"END".equals(current.type())) {
            activities.updateExecutionNode(executionId, current.id(), "RUNNING");
            current = processNode(current, graph, nodeMap, input, executionId);
        }

        String finalNodeId = current != null ? current.id() : "end";
        activities.updateExecutionNode(executionId, finalNodeId, "COMPLETED");
        log.info("[WORKFLOW] completed scenarioId={} userId={}", input.scenarioId(), input.userId());
    }

    // ── Node processing ───────────────────────────────────────────────────────

    private ScenarioNode processNode(ScenarioNode node, ScenarioGraph graph,
                                     Map<String, ScenarioNode> nodeMap,
                                     ScenarioWorkflowInput input, String executionId) {
        String type = node.type() == null ? "" : node.type();
        switch (type) {
            case "TRIGGER":
                return followDefaultEdge(node, graph, nodeMap);

            case "SEND_EMAIL": {
                String templateId = node.data() != null ? node.data().templateId() : null;
                SendResult result = activities.sendEmail(input.userId(), templateId,
                        input.triggerEventPayload());
                return waitForDelivery(result, node, graph, nodeMap, input);
            }
            case "SEND_PUSH": {
                String templateId = node.data() != null ? node.data().templateId() : null;
                SendResult result = activities.sendPush(input.userId(), templateId,
                        input.triggerEventPayload());
                return waitForDelivery(result, node, graph, nodeMap, input);
            }
            case "SEND_SMS": {
                String templateId = node.data() != null ? node.data().templateId() : null;
                SendResult result = activities.sendSms(input.userId(), templateId,
                        input.triggerEventPayload());
                return waitForDelivery(result, node, graph, nodeMap, input);
            }
            case "WAIT_EVENT":
                return processWaitEvent(node, graph, nodeMap);

            case "DELAY": {
                long seconds = node.data() != null ? node.data().delaySeconds() : 60;
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
        if (!sent.accepted()) {
            return followEdgeByHandle(sendNode, graph, nodeMap, "failed");
        }

        String channel        = sent.channel();
        String deliveredEvent = channel.toUpperCase() + "_DELIVERED";
        String failedEvent    = channel.toUpperCase() + "_FAILED";
        // The messageId we sent — only delivery events carrying this exact ID are relevant.
        // This prevents cross-contamination when multiple emails are in flight for the same user.
        String sentMessageId  = sent.messageId();

        long timeoutSeconds = findDownstreamTimeout(sendNode, graph, nodeMap, 86400L);

        AtomicReference<String> outcome = new AtomicReference<>(null);

        Workflow.await(Duration.ofSeconds(timeoutSeconds), () -> {
            for (IncomingEvent e : eventQueue) {
                if (!input.userId().equals(e.userId())) continue;
                // If the event carries a messageId, it must match the one we sent.
                // Events with no messageId are accepted for backwards compatibility.
                if (sentMessageId != null && e.messageId() != null
                        && !sentMessageId.equals(e.messageId())) {
                    continue;  // belongs to a different message — skip
                }
                if (deliveredEvent.equalsIgnoreCase(e.eventType())) {
                    outcome.set("delivered");
                    eventQueue.remove(e);
                    return true;
                }
                if (failedEvent.equalsIgnoreCase(e.eventType())) {
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
        String expectedType  = node.data() != null ? node.data().eventType() : null;
        long timeoutSeconds  = node.data() != null ? node.data().timeoutSeconds() : 3600;
        if (timeoutSeconds <= 0) timeoutSeconds = 3600;

        boolean[] arrived = { false };

        Workflow.await(Duration.ofSeconds(timeoutSeconds), () -> {
            for (IncomingEvent e : eventQueue) {
                if (expectedType == null || expectedType.equalsIgnoreCase(e.eventType())) {
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
        // React Flow names the generic output handle "out" (from OutRight component).
        // Also accept null and "default" for backwards compatibility.
        return outgoing(node, graph).stream()
                .filter(e -> e.sourceHandle() == null
                          || "default".equalsIgnoreCase(e.sourceHandle())
                          || "out".equalsIgnoreCase(e.sourceHandle()))
                .findFirst()
                .map(e -> nodeMap.get(e.target()))
                .orElse(null);
    }

    private ScenarioNode followEdgeByHandle(ScenarioNode node, ScenarioGraph graph,
                                             Map<String, ScenarioNode> nodeMap, String handle) {
        Optional<ScenarioNode> explicit = outgoing(node, graph).stream()
                .filter(e -> handle.equalsIgnoreCase(e.sourceHandle()))
                .findFirst()
                .map(e -> nodeMap.get(e.target()));
        return explicit.orElseGet(() -> followDefaultEdge(node, graph, nodeMap));
    }

    private List<ScenarioEdge> outgoing(ScenarioNode node, ScenarioGraph graph) {
        return graph.edges().stream()
                .filter(e -> node.id().equals(e.source()))
                .collect(Collectors.toList());
    }

    private long findDownstreamTimeout(ScenarioNode sendNode, ScenarioGraph graph,
                                        Map<String, ScenarioNode> nodeMap, long defaultValue) {
        return outgoing(sendNode, graph).stream()
                .map(e -> nodeMap.get(e.target()))
                .filter(n -> n != null && "WAIT_EVENT".equals(n.type()))
                .mapToLong(n -> n.data() != null && n.data().timeoutSeconds() > 0
                        ? n.data().timeoutSeconds() : defaultValue)
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
