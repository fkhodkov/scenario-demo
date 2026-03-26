package com.demo.events.dto;

import com.demo.events.spec.KafkaEvent;
import java.time.Instant;
import java.util.UUID;

@KafkaEvent(name = "USER_REGISTERED", topic = "user.registered", description = "New user signed up")
public record UserRegisteredEvent(
        String  eventId,
        String  userId,
        Instant occurredAt,
        String  email,
        String  channel   // email | push | sms
) implements BaseEvent {
    public static UserRegisteredEvent of(String userId, String email, String channel) {
        return new UserRegisteredEvent(UUID.randomUUID().toString(), userId, Instant.now(), email, channel);
    }
}
