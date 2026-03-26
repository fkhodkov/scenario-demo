package com.demo.events.dto;

import com.demo.events.spec.KafkaEvent;
import java.time.Instant;
import java.util.UUID;

@KafkaEvent(name = "EMAIL_FAILED", topic = "email.failed", description = "Email delivery failed (bounce / unsubscribed)")
public record EmailFailedEvent(
        String  eventId,
        String  userId,
        Instant occurredAt,
        String  messageId,
        String  reason
) implements BaseEvent {
    public static EmailFailedEvent of(String userId, String messageId, String reason) {
        return new EmailFailedEvent(UUID.randomUUID().toString(), userId, Instant.now(), messageId, reason);
    }
}
