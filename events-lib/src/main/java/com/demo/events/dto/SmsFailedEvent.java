package com.demo.events.dto;

import com.demo.events.spec.KafkaEvent;
import java.time.Instant;
import java.util.UUID;

@KafkaEvent(name = "SMS_FAILED", topic = "sms.failed", description = "SMS delivery failed")
public record SmsFailedEvent(
        String  eventId,
        String  userId,
        Instant occurredAt,
        String  messageId,
        String  reason
) implements BaseEvent {
    public static SmsFailedEvent of(String userId, String messageId, String reason) {
        return new SmsFailedEvent(UUID.randomUUID().toString(), userId, Instant.now(), messageId, reason);
    }
}
