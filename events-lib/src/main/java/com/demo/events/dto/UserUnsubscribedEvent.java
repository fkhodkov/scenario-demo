package com.demo.events.dto;

import com.demo.events.spec.KafkaEvent;
import java.time.Instant;
import java.util.UUID;

@KafkaEvent(name = "USER_UNSUBSCRIBED", topic = "user.unsubscribed", description = "User opted out of communications")
public record UserUnsubscribedEvent(
        String  eventId,
        String  userId,
        Instant occurredAt,
        String  channel
) implements BaseEvent {
    public static UserUnsubscribedEvent of(String userId, String channel) {
        return new UserUnsubscribedEvent(UUID.randomUUID().toString(), userId, Instant.now(), channel);
    }
}
