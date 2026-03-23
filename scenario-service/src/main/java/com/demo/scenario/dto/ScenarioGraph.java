package com.demo.scenario.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScenarioGraph {

    private List<ScenarioNode> nodes;
    private List<ScenarioEdge> edges;

    public List<ScenarioNode> getNodes() { return nodes; }
    public List<ScenarioEdge> getEdges() { return edges; }
    public void setNodes(List<ScenarioNode> nodes) { this.nodes = nodes; }
    public void setEdges(List<ScenarioEdge> edges) { this.edges = edges; }

    // ── Node ────────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScenarioNode {
        private String id;
        private String type;
        private NodeData data;
        private Position position;

        public String getId()       { return id; }
        public String getType()     { return type; }
        public NodeData getData()   { return data; }
        public Position getPosition() { return position; }

        public void setId(String id)           { this.id = id; }
        public void setType(String type)       { this.type = type; }
        public void setData(NodeData data)     { this.data = data; }
        public void setPosition(Position p)    { this.position = p; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NodeData {
        private String label;
        private String eventType;
        private String topic;
        private long timeoutSeconds;
        private String templateId;
        private String channel;
        private String conditionField;
        private String conditionValue;
        private long delaySeconds;
        private Map<String, Object> extra;

        public String getLabel()           { return label; }
        public String getEventType()       { return eventType; }
        public String getTopic()           { return topic; }
        public long getTimeoutSeconds()    { return timeoutSeconds; }
        public String getTemplateId()      { return templateId; }
        public String getChannel()         { return channel; }
        public String getConditionField()  { return conditionField; }
        public String getConditionValue()  { return conditionValue; }
        public long getDelaySeconds()      { return delaySeconds; }
        public Map<String, Object> getExtra() { return extra; }

        public void setLabel(String label)                   { this.label = label; }
        public void setEventType(String eventType)           { this.eventType = eventType; }
        public void setTopic(String topic)                   { this.topic = topic; }
        public void setTimeoutSeconds(long timeoutSeconds)   { this.timeoutSeconds = timeoutSeconds; }
        public void setTemplateId(String templateId)         { this.templateId = templateId; }
        public void setChannel(String channel)               { this.channel = channel; }
        public void setConditionField(String conditionField) { this.conditionField = conditionField; }
        public void setConditionValue(String conditionValue) { this.conditionValue = conditionValue; }
        public void setDelaySeconds(long delaySeconds)       { this.delaySeconds = delaySeconds; }
        public void setExtra(Map<String, Object> extra)      { this.extra = extra; }
    }

    public static class Position {
        private double x;
        private double y;
        public double getX() { return x; }
        public double getY() { return y; }
        public void setX(double x) { this.x = x; }
        public void setY(double y) { this.y = y; }
    }

    // ── Edge ────────────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScenarioEdge {
        private String id;
        private String source;
        private String target;
        private String sourceHandle;
        private EdgeData data;

        public String getId()             { return id; }
        public String getSource()         { return source; }
        public String getTarget()         { return target; }
        public String getSourceHandle()   { return sourceHandle; }
        public EdgeData getData()         { return data; }

        public void setId(String id)                   { this.id = id; }
        public void setSource(String source)           { this.source = source; }
        public void setTarget(String target)           { this.target = target; }
        public void setSourceHandle(String h)          { this.sourceHandle = h; }
        public void setData(EdgeData data)             { this.data = data; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EdgeData {
        private String label;
        private String edgeType;
        private String eventType;

        public String getLabel()     { return label; }
        public String getEdgeType()  { return edgeType; }
        public String getEventType() { return eventType; }

        public void setLabel(String label)       { this.label = label; }
        public void setEdgeType(String edgeType) { this.edgeType = edgeType; }
        public void setEventType(String e)       { this.eventType = e; }
    }
}
