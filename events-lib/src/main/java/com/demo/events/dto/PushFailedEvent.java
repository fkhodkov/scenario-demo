package com.demo.events.dto;

import com.demo.events.spec.KafkaEvent;
import java.time.Instant;
import java.util.UUID;

@KafkaEvent(name = "PUSH_FAILED", topic = "push.failed", description = "Push delivery failed")
public record PushFailedEvent(
        String  eventId,
        String  userId,
        Instant occurredAt,
        String  notificationId,
        String  reason
) implements BaseEvent {
    public static PushFailedEvent of(String userId, String notificationId, String reason) {
        return new PushFailedEvent(UUID.randomUUID().toString(), userId, Instant.now(), notificationId, reason);
    }
}
