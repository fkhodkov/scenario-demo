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

        assertNotNull(graph.nodes());
        assertNotNull(graph.edges());
        assertEquals(6, graph.nodes().size());
        assertEquals(5, graph.edges().size());
    }

    @Test
    void triggerNode_hasCorrectData() throws Exception {
        ScenarioGraph graph = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);

        ScenarioGraph.ScenarioNode trigger = graph.nodes().stream()
                .filter(n -> "TRIGGER".equals(n.type()))
                .findFirst()
                .orElseThrow();

        assertEquals("node_1",         trigger.id());
        assertEquals("USER_REGISTERED", trigger.data().eventType());
        assertEquals("user.registered", trigger.data().topic());
        assertEquals("User Registered", trigger.data().label());
    }

    @Test
    void sendEmailNode_hasTemplateId() throws Exception {
        ScenarioGraph graph = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);

        ScenarioGraph.ScenarioNode sendEmail = graph.nodes().stream()
                .filter(n -> "SEND_EMAIL".equals(n.type()))
                .findFirst().orElseThrow();

        assertEquals("welcome_v1", sendEmail.data().templateId());
        assertEquals("email",      sendEmail.data().channel());
    }

    @Test
    void waitEventNode_hasTimeoutSeconds() throws Exception {
        ScenarioGraph graph = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);

        ScenarioGraph.ScenarioNode wait = graph.nodes().stream()
                .filter(n -> "WAIT_EVENT".equals(n.type()))
                .findFirst().orElseThrow();

        assertEquals("EMAIL_OPENED", wait.data().eventType());
        assertEquals(86400,          wait.data().timeoutSeconds());
    }

    @Test
    void delayNode_hasDelaySeconds() throws Exception {
        ScenarioGraph graph = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);

        ScenarioGraph.ScenarioNode delay = graph.nodes().stream()
                .filter(n -> "DELAY".equals(n.type()))
                .findFirst().orElseThrow();

        assertEquals(3600, delay.data().delaySeconds());
    }

    @Test
    void conditionNode_hasConditionFields() throws Exception {
        ScenarioGraph graph = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);

        ScenarioGraph.ScenarioNode cond = graph.nodes().stream()
                .filter(n -> "CONDITION".equals(n.type()))
                .findFirst().orElseThrow();

        assertEquals("channel", cond.data().conditionField());
        assertEquals("email",   cond.data().conditionValue());
    }

    @Test
    void edges_deliveredHandle_isMapped() throws Exception {
        ScenarioGraph graph = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);

        ScenarioGraph.ScenarioEdge deliveredEdge = graph.edges().stream()
                .filter(e -> "delivered".equals(e.sourceHandle()))
                .findFirst().orElseThrow();

        assertEquals("node_2", deliveredEdge.source());
        assertEquals("node_3", deliveredEdge.target());
        assertEquals("ON_DELIVERED", deliveredEdge.data().edgeType());
    }

    @Test
    void edges_failedHandle_isMapped() throws Exception {
        ScenarioGraph graph = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);

        ScenarioGraph.ScenarioEdge failedEdge = graph.edges().stream()
                .filter(e -> "failed".equals(e.sourceHandle()))
                .findFirst().orElseThrow();

        assertEquals("node_2", failedEdge.source());
        assertEquals("node_6", failedEdge.target());
    }

    @Test
    void edges_timeoutHandle_isMapped() throws Exception {
        ScenarioGraph graph = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);

        ScenarioGraph.ScenarioEdge timeoutEdge = graph.edges().stream()
                .filter(e -> "timeout".equals(e.sourceHandle()))
                .findFirst().orElseThrow();

        assertEquals("node_3", timeoutEdge.source());
        assertEquals("node_4", timeoutEdge.target());
    }

    @Test
    void unknownFields_areIgnored() throws Exception {
        String jsonWithExtra = """
                {"nodes":[{"id":"n1","type":"END","data":{"label":"x","unknownField":"ignored"}}],"edges":[]}
                """;
        ScenarioGraph graph = mapper.readValue(jsonWithExtra, ScenarioGraph.class);
        assertEquals(1, graph.nodes().size());
        assertEquals("n1", graph.nodes().get(0).id());
    }

    @Test
    void emptyGraph_deserializesCleanly() throws Exception {
        ScenarioGraph graph = mapper.readValue("{\"nodes\":[],\"edges\":[]}", ScenarioGraph.class);
        assertNotNull(graph.nodes());
        assertNotNull(graph.edges());
        assertTrue(graph.nodes().isEmpty());
        assertTrue(graph.edges().isEmpty());
    }

    @Test
    void roundTrip_serializeThenDeserialize() throws Exception {
        ScenarioGraph original = mapper.readValue(FULL_GRAPH_JSON, ScenarioGraph.class);
        String serialized      = mapper.writeValueAsString(original);
        ScenarioGraph restored = mapper.readValue(serialized, ScenarioGraph.class);

        assertEquals(original.nodes().size(), restored.nodes().size());
        assertEquals(original.edges().size(), restored.edges().size());
        assertEquals(original.nodes().get(0).id(), restored.nodes().get(0).id());
    }
}
