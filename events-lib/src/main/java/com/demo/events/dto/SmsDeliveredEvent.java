package com.demo.events.dto;

import com.demo.events.spec.KafkaEvent;
import java.time.Instant;
import java.util.UUID;

@KafkaEvent(name = "SMS_DELIVERED", topic = "sms.delivered", description = "SMS confirmed delivered")
public record SmsDeliveredEvent(
        String  eventId,
        String  userId,
        Instant occurredAt,
        String  messageId
) implements BaseEvent {
    public static SmsDeliveredEvent of(String userId, String messageId) {
        return new SmsDeliveredEvent(UUID.randomUUID().toString(), userId, Instant.now(), messageId);
    }
}
