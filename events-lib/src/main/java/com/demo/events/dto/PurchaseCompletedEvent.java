package com.demo.events.dto;

import com.demo.events.spec.KafkaEvent;
import java.time.Instant;
import java.util.UUID;

@KafkaEvent(name = "PURCHASE_COMPLETED", topic = "purchase.completed", description = "User completed a purchase")
public record PurchaseCompletedEvent(
        String  eventId,
        String  userId,
        Instant occurredAt,
        String  orderId,
        double  amount
) implements BaseEvent {
    public static PurchaseCompletedEvent of(String userId, String orderId, double amount) {
        return new PurchaseCompletedEvent(UUID.randomUUID().toString(), userId, Instant.now(), orderId, amount);
    }
}
