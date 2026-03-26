package com.demo.events.dto;

import com.demo.events.spec.KafkaEvent;
import java.time.Instant;
import java.util.UUID;

@KafkaEvent(name = "EMAIL_OPENED", topic = "email.opened", description = "User opened the email")
public record EmailOpenedEvent(
        String  eventId,
        String  userId,
        Instant occurredAt,
        String  messageId
) implements BaseEvent {
    public static EmailOpenedEvent of(String userId, String messageId) {
        return new EmailOpenedEvent(UUID.randomUUID().toString(), userId, Instant.now(), messageId);
    }
}
