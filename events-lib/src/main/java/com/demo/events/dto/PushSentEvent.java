package com.demo.events.dto;
import com.demo.events.spec.KafkaEvent;
@KafkaEvent(name="PUSH_SENT", topic="push.sent", description="Push notification sent")
public class PushSentEvent extends BaseEvent {
    public String notificationId;
    public PushSentEvent() {}
    public PushSentEvent(String userId, String notificationId) { super(userId); this.notificationId = notificationId; }
}
