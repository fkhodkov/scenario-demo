package com.demo.scenario.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that the ScenarioGraph POJO correctly round-trips through Jackson.
 * This is critical because the graph is stored as JSONB and deserialized
 * by the workflow at execution time.
 */
class ScenarioGraphTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    private static final String FULL_GRAPH_JSON = """
            {
              "nodes": [
                {
                  "id": "node_1",
                  "type": "TRIGGER",
                  "position": {"x": 100, "y": 200},
                  "data": {
                    "label": "User Registered",
                    "eventType": "USER_REGISTERED",
                    "topic": "user.registered"
                  }
                },
                {
                  "id": "node_2",
                  "type": "SEND_EMAIL",
                  "position": {"x": 350, "y": 200},
                  "data": {
                    "label": "Welcome Email",
                    "templateId": "welcome_v1",
                    "channel": "email"
                  }
                },
                {
                  "id": "node_3",
                  "type": "WAIT_EVENT",
                  "position": {"x": 600, "y": 150},
                  "data": {
                    "label": "Wait opened",
                    "eventType": "EMAIL_OPENED",
                    "timeoutSeconds": 86400
                  }
                },
                {
                  "id": "node_4",
                  "type": "DELAY",
                  "position": {"x": 600, "y": 300},
                  "data": {
                    "label": "Wait 1h",
                    "delaySeconds": 3600
                  }
                },
                {
                  "id": "node_5",
                  "type": "CONDITION",
                  "position": {"x": 800, "y": 200},
                  "data": {
                    "label": "Check channel",
                    "conditionField": "channel",
                    "conditionValue": "email"
                  }
                },
                {
                  "id": "node_6",
                  "type": "END",
                  "position": {"x": 1000, "y": 200},
                  "data": {"label": "Done"}
                }
              ],
              "edges": [
                {
                  "id": "e1",
                  "source": "node_1",
                  "target": "node_2",
                  "sourceHandle": null,
                  "data": {"edgeType": "DEFAULT"}
                },
                {
                  "id": "e2",
                  "source": "node_2",
                  "target": "node_3",
                  "sourceHandle": "delivered",
                  "data": {"edgeType": "ON_DELIVERED", "label": "delivered"}
                },
                {
                  "id": "e3",
                  "source": "node_2",
                  "target": "node_6",
                  "sourceHandle": "failed",
                  "data": {"edgeType": "ON_FAILED", "label": "failed"}
                },
                {
                  "id": "e4",
                  "source": "node_3",
                  "target": "node_5",
                  "sourceHandle": "event",
                  "data": {"edgeType": "ON_EVENT"}
                },
                {
                  "id": "e5",
                  "source": "node_3",
                  "target": "node_4",
                  "sourceHandle": "timeout",
                  "data": {"edgeType": "ON_TIMEOUT"}
                }
              ]
            }
            """;

    @Test
    void deserialize_nodesAndEdges() throws Exception {
        ScenarioGraph graph = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);

        assertNotNull(graph.getNodes());
        assertNotNull(graph.getEdges());
        assertEquals(6, graph.getNodes().size());
        assertEquals(5, graph.getEdges().size());
    }

    @Test
    void triggerNode_hasCorrectData() throws Exception {
        ScenarioGraph graph = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);

        ScenarioGraph.ScenarioNode trigger = graph.getNodes().stream()
                .filter(n -> "TRIGGER".equals(n.getType()))
                .findFirst()
                .orElseThrow();

        assertEquals("node_1",         trigger.getId());
        assertEquals("USER_REGISTERED", trigger.getData().getEventType());
        assertEquals("user.registered", trigger.getData().getTopic());
        assertEquals("User Registered", trigger.getData().getLabel());
    }

    @Test
    void sendEmailNode_hasTemplateId() throws Exception {
        ScenarioGraph graph = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);

        ScenarioGraph.ScenarioNode sendEmail = graph.getNodes().stream()
                .filter(n -> "SEND_EMAIL".equals(n.getType()))
                .findFirst().orElseThrow();

        assertEquals("welcome_v1", sendEmail.getData().getTemplateId());
        assertEquals("email",      sendEmail.getData().getChannel());
    }

    @Test
    void waitEventNode_hasTimeoutSeconds() throws Exception {
        ScenarioGraph graph = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);

        ScenarioGraph.ScenarioNode wait = graph.getNodes().stream()
                .filter(n -> "WAIT_EVENT".equals(n.getType()))
                .findFirst().orElseThrow();

        assertEquals("EMAIL_OPENED", wait.getData().getEventType());
        assertEquals(86400,          wait.getData().getTimeoutSeconds());
    }

    @Test
    void delayNode_hasDelaySeconds() throws Exception {
        ScenarioGraph graph = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);

        ScenarioGraph.ScenarioNode delay = graph.getNodes().stream()
                .filter(n -> "DELAY".equals(n.getType()))
                .findFirst().orElseThrow();

        assertEquals(3600, delay.getData().getDelaySeconds());
    }

    @Test
    void conditionNode_hasConditionFields() throws Exception {
        ScenarioGraph graph = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);

        ScenarioGraph.ScenarioNode cond = graph.getNodes().stream()
                .filter(n -> "CONDITION".equals(n.getType()))
                .findFirst().orElseThrow();

        assertEquals("channel", cond.getData().getConditionField());
        assertEquals("email",   cond.getData().getConditionValue());
    }

    @Test
    void edges_deliveredHandle_isMapped() throws Exception {
        ScenarioGraph graph = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);

        ScenarioGraph.ScenarioEdge deliveredEdge = graph.getEdges().stream()
                .filter(e -> "delivered".equals(e.getSourceHandle()))
                .findFirst().orElseThrow();

        assertEquals("node_2", deliveredEdge.getSource());
        assertEquals("node_3", deliveredEdge.getTarget());
        assertEquals("ON_DELIVERED", deliveredEdge.getData().getEdgeType());
    }

    @Test
    void edges_failedHandle_isMapped() throws Exception {
        ScenarioGraph graph = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);

        ScenarioGraph.ScenarioEdge failedEdge = graph.getEdges().stream()
                .filter(e -> "failed".equals(e.getSourceHandle()))
                .findFirst().orElseThrow();

        assertEquals("node_2", failedEdge.getSource());
        assertEquals("node_6", failedEdge.getTarget());
    }

    @Test
    void edges_timeoutHandle_isMapped() throws Exception {
        ScenarioGraph graph = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);

        ScenarioGraph.ScenarioEdge timeoutEdge = graph.getEdges().stream()
                .filter(e -> "timeout".equals(e.getSourceHandle()))
                .findFirst().orElseThrow();

        assertEquals("node_3", timeoutEdge.getSource());
        assertEquals("node_4", timeoutEdge.getTarget());
    }

    @Test
    void unknownFields_areIgnored() throws Exception {
        String jsonWithExtra = """
                {"nodes":[{"id":"n1","type":"END","data":{"label":"x","unknownField":"ignored"}}],"edges":[]}
                """;
        ScenarioGraph graph = mapper.readValue(jsonWithExtra, ScenarioGraph.class);
        assertEquals(1, graph.getNodes().size());
        assertEquals("n1", graph.getNodes().get(0).getId());
    }

    @Test
    void emptyGraph_deserializesCleanly() throws Exception {
        ScenarioGraph graph = mapper.readValue("{\"nodes\":[],\"edges\":[]}", ScenarioGraph.class);
        assertNotNull(graph.getNodes());
        assertNotNull(graph.getEdges());
        assertTrue(graph.getNodes().isEmpty());
        assertTrue(graph.getEdges().isEmpty());
    }

    @Test
    void roundTrip_serializeThenDeserialize() throws Exception {
        ScenarioGraph original = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);
        String serialized      = mapper.writeValueAsString(original);
        ScenarioGraph restored = mapper.readValue(serialized, ScenarioGraph.class);

        assertEquals(original.getNodes().size(), restored.getNodes().size());
        assertEquals(original.getEdges().size(), restored.getEdges().size());
        assertEquals(original.getNodes().get(0).getId(), restored.getNodes().get(0).getId());
    }
}
