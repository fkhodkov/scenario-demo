package com.demo.events.dto;

import com.demo.events.spec.KafkaEvent;
import java.time.Instant;
import java.util.UUID;

@KafkaEvent(name = "EMAIL_SENT", topic = "email.sent", description = "Email dispatched to provider")
public record EmailSentEvent(
        String  eventId,
        String  userId,
        Instant occurredAt,
        String  messageId,
        String  templateId
) implements BaseEvent {
    public static EmailSentEvent of(String userId, String messageId, String templateId) {
        return new EmailSentEvent(UUID.randomUUID().toString(), userId, Instant.now(), messageId, templateId);
    }
}
