package com.demo.events.dto;

import com.demo.events.spec.KafkaEvent;
import java.time.Instant;
import java.util.UUID;

@KafkaEvent(name = "PUSH_SENT", topic = "push.sent", description = "Push notification sent")
public record PushSentEvent(
        String  eventId,
        String  userId,
        Instant occurredAt,
        String  notificationId
) implements BaseEvent {
    public static PushSentEvent of(String userId, String notificationId) {
        return new PushSentEvent(UUID.randomUUID().toString(), userId, Instant.now(), notificationId);
    }
}
