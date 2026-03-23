package com.demo.events.dto;
import com.demo.events.spec.KafkaEvent;
@KafkaEvent(name="PUSH_FAILED", topic="push.failed", description="Push delivery failed")
public class PushFailedEvent extends BaseEvent {
    public String notificationId; public String reason;
    public PushFailedEvent() {}
    public PushFailedEvent(String userId, String notificationId, String reason) {
        super(userId); this.notificationId = notificationId; this.reason = reason;
    }
}
