package com.demo.scenario.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/**
 * Parsed representation of the React Flow graph stored in scenario.definition.
 * All types are records — they are deserialized once and never mutated.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScenarioGraph(
        List<ScenarioNode> nodes,
        List<ScenarioEdge> edges
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScenarioNode(
            String   id,
            String   type,
            NodeData data,
            Position position
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NodeData(
            String              label,
            String              eventType,
            String              topic,
            long                timeoutSeconds,
            String              templateId,
            String              channel,
            String              conditionField,
            String              conditionValue,
            long                delaySeconds,
            Map<String, Object> extra
    ) {}

    public record Position(double x, double y) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScenarioEdge(
            String   id,
            String   source,
            String   target,
            String   sourceHandle,
            EdgeData data
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EdgeData(
            String label,
            String edgeType,
            String eventType
    ) {}
}
