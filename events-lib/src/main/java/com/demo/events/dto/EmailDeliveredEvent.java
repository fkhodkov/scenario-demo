package com.demo.events.dto;

import com.demo.events.spec.KafkaEvent;
import java.time.Instant;
import java.util.UUID;

@KafkaEvent(name = "EMAIL_DELIVERED", topic = "email.delivered", description = "Email confirmed delivered by provider")
public record EmailDeliveredEvent(
        String  eventId,
        String  userId,
        Instant occurredAt,
        String  messageId
) implements BaseEvent {
    public static EmailDeliveredEvent of(String userId, String messageId) {
        return new EmailDeliveredEvent(UUID.randomUUID().toString(), userId, Instant.now(), messageId);
    }
}
