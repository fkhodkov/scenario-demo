package com.demo.scenario.temporal.workflows;

/**
 * A Kafka event signal delivered to a running workflow.
 * messageId correlates delivery/failure events back to the specific
 * outbound message that triggered the wait — prevents cross-contamination
 * when multiple messages are in flight for the same user.
 */
public record IncomingEvent(
        String eventType,
        String topic,
        String userId,
        String payload,
        String messageId   // nullable — absent for non-delivery events
) {
    /** Convenience constructor for events that don't carry a messageId. */
    public IncomingEvent(String eventType, String topic, String userId, String payload) {
        this(eventType, topic, userId, payload, null);
    }
}
