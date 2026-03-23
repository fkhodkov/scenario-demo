package com.demo.events.dto;
import com.demo.events.spec.KafkaEvent;
@KafkaEvent(name="EMAIL_DELIVERED", topic="email.delivered", description="Email confirmed delivered by provider")
public class EmailDeliveredEvent extends BaseEvent {
    public String messageId;
    public EmailDeliveredEvent() {}
    public EmailDeliveredEvent(String userId, String messageId) { super(userId); this.messageId = messageId; }
}
