package com.demo.simulator.web;

import com.demo.events.spec.EventRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/simulator")
@CrossOrigin(origins = "*")
public class SimulatorController {

    private static final Logger log = LoggerFactory.getLogger(SimulatorController.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;

    private final CopyOnWriteArrayList<OutboundMessage> outboundLog = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<String> eventLog = new CopyOnWriteArrayList<>();

    public SimulatorController(KafkaTemplate<String, String> kafka, ObjectMapper objectMapper) {
        this.kafka        = kafka;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/events")
    public List<Map<String, String>> catalog() {
        return EventRegistry.all().stream()
                .map(e -> Map.of(
                        "eventType",   e.eventType(),
                        "topic",       e.topic(),
                        "description", e.description()))
                .toList();
    }

    @PostMapping("/publish")
    public Map<String, String> publish(@RequestBody PublishRequest req) throws Exception {
        String topic = req.getTopic();
        if (topic == null || topic.isBlank()) {
            topic = EventRegistry.findByName(req.getEventType())
                    .map(EventRegistry.EventSpec::topic)
                    .orElse(req.getEventType().toLowerCase().replace("_", "."));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId",    UUID.randomUUID().toString());
        payload.put("eventType",  req.getEventType());
        payload.put("userId",     req.getUserId());
        payload.put("occurredAt", Instant.now().toString());
        if (req.getExtra() != null) payload.putAll(req.getExtra());

        String json = objectMapper.writeValueAsString(payload);
        kafka.send(topic, req.getUserId(), json);

        String entry = "[" + Instant.now() + "] PUBLISHED " + req.getEventType()
                + " → topic:" + topic + " userId:" + req.getUserId();
        addLog(entry);

        log.info("Simulator published {} to {} for user {}", req.getEventType(), topic, req.getUserId());
        return Map.of("status", "published", "topic", topic, "json", json);
    }

    @KafkaListener(topics = "comm.outbound", groupId = "simulator-outbound")
    public void interceptOutbound(ConsumerRecord<String, String> record) throws Exception {
        Map<?, ?> msg = objectMapper.readValue(record.value(), Map.class);
        OutboundMessage om = new OutboundMessage(
                UUID.randomUUID().toString(),
                str(msg, "channel"),
                str(msg, "userId"),
                str(msg, "messageId"),
                str(msg, "templateId"),
                Instant.now().toString(),
                "PENDING",
                record.value()
        );
        outboundLog.add(0, om);
        if (outboundLog.size() > 500) outboundLog.subList(500, outboundLog.size()).clear();
        log.info("Simulator intercepted {} for user {}", om.channel(), om.userId());
    }

    @GetMapping("/outbound")
    public List<OutboundMessage> outbound() { return outboundLog; }

    @GetMapping("/log")
    public List<String> log() { return eventLog; }

    @PostMapping("/outbound/{messageId}/deliver")
    public Map<String, String> deliver(@PathVariable String messageId) throws Exception {
        return ackMessage(messageId, true);
    }

    @PostMapping("/outbound/{messageId}/fail")
    public Map<String, String> fail(@PathVariable String messageId) throws Exception {
        return ackMessage(messageId, false);
    }

    private Map<String, String> ackMessage(String messageId, boolean success) throws Exception {
        OutboundMessage om = outboundLog.stream()
                .filter(m -> m.messageId().equals(messageId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Message not found: " + messageId));

        // Replace with updated status
        int idx = outboundLog.indexOf(om);
        OutboundMessage updated = new OutboundMessage(
                om.id(), om.channel(), om.userId(), om.messageId(),
                om.templateId(), om.timestamp(),
                success ? "DELIVERED" : "FAILED",
                om.rawPayload());
        if (idx >= 0) outboundLog.set(idx, updated);

        String channel   = om.channel();
        String eventType = success ? channel.toUpperCase() + "_DELIVERED"
                                   : channel.toUpperCase() + "_FAILED";
        String topic     = success ? channel + ".delivered" : channel + ".failed";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId",    UUID.randomUUID().toString());
        payload.put("eventType",  eventType);
        payload.put("userId",     om.userId());
        payload.put("messageId",  om.messageId());
        payload.put("occurredAt", Instant.now().toString());
        if (!success) payload.put("reason", "simulated_failure");

        String json = objectMapper.writeValueAsString(payload);
        kafka.send(topic, om.userId(), json);
        addLog("[" + Instant.now() + "] " + eventType + " → " + topic + " messageId:" + messageId);

        return Map.of("status", eventType, "topic", topic);
    }

    private void addLog(String entry) {
        eventLog.add(0, entry);
        if (eventLog.size() > 200) eventLog.subList(200, eventLog.size()).clear();
    }

    private String str(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : "";
    }

    // ── DTOs as Java records (no Lombok needed) ────────────────────────────────

    public record OutboundMessage(
            String id, String channel, String userId,
            String messageId, String templateId,
            String timestamp, String status, String rawPayload) {}

    public static class PublishRequest {
        private String eventType;
        private String userId;
        private String topic;
        private Map<String, Object> extra;

        public String getEventType()         { return eventType; }
        public String getUserId()            { return userId; }
        public String getTopic()             { return topic; }
        public Map<String, Object> getExtra(){ return extra; }
        public void setEventType(String v)   { this.eventType = v; }
        public void setUserId(String v)      { this.userId = v; }
        public void setTopic(String v)       { this.topic = v; }
        public void setExtra(Map<String, Object> v) { this.extra = v; }
    }
}
