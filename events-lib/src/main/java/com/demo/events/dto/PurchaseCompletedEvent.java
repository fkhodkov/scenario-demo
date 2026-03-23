package com.demo.events.dto;
import com.demo.events.spec.KafkaEvent;
@KafkaEvent(name="PURCHASE_COMPLETED", topic="purchase.completed", description="User completed a purchase")
public class PurchaseCompletedEvent extends BaseEvent {
    public String orderId; public double amount;
    public PurchaseCompletedEvent() {}
    public PurchaseCompletedEvent(String userId, String orderId, double amount) {
        super(userId); this.orderId = orderId; this.amount = amount;
    }
}
