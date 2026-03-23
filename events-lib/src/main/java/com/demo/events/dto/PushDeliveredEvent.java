package com.demo.events.dto;
import com.demo.events.spec.KafkaEvent;
@KafkaEvent(name="PUSH_DELIVERED", topic="push.delivered", description="Push confirmed delivered")
public class PushDeliveredEvent extends BaseEvent {
    public String notificationId;
    public PushDeliveredEvent() {}
    public PushDeliveredEvent(String userId, String notificationId) { super(userId); this.notificationId = notificationId; }
}
