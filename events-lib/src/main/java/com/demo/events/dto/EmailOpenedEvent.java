package com.demo.events.dto;
import com.demo.events.spec.KafkaEvent;
@KafkaEvent(name="EMAIL_OPENED", topic="email.opened", description="User opened the email")
public class EmailOpenedEvent extends BaseEvent {
    public String messageId;
    public EmailOpenedEvent() {}
    public EmailOpenedEvent(String userId, String messageId) { super(userId); this.messageId = messageId; }
}
