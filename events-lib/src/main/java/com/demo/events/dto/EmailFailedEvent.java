package com.demo.events.dto;
import com.demo.events.spec.KafkaEvent;
@KafkaEvent(name="EMAIL_FAILED", topic="email.failed", description="Email delivery failed (bounce / unsubscribed)")
public class EmailFailedEvent extends BaseEvent {
    public String messageId; public String reason;
    public EmailFailedEvent() {}
    public EmailFailedEvent(String userId, String messageId, String reason) {
        super(userId); this.messageId = messageId; this.reason = reason;
    }
}
