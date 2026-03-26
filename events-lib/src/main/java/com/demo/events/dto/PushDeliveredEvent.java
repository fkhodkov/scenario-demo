package com.demo.events.dto;

import com.demo.events.spec.KafkaEvent;
import java.time.Instant;
import java.util.UUID;

@KafkaEvent(name = "PUSH_DELIVERED", topic = "push.delivered", description = "Push confirmed delivered")
public record PushDeliveredEvent(
        String  eventId,
        String  userId,
        Instant occurredAt,
        String  notificationId
) implements BaseEvent {
    public static PushDeliveredEvent of(String userId, String notificationId) {
        return new PushDeliveredEvent(UUID.randomUUID().toString(), userId, Instant.now(), notificationId);
    }
}
