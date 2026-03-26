package com.demo.events.dto;

import com.demo.events.spec.KafkaEvent;
import java.time.Instant;
import java.util.UUID;

@KafkaEvent(name = "LINK_CLICKED", topic = "link.clicked", description = "User clicked a tracked link")
public record LinkClickedEvent(
        String  eventId,
        String  userId,
        Instant occurredAt,
        String  messageId,
        String  url
) implements BaseEvent {
    public static LinkClickedEvent of(String userId, String messageId, String url) {
        return new LinkClickedEvent(UUID.randomUUID().toString(), userId, Instant.now(), messageId, url);
    }
}
