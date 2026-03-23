package com.demo.events.dto;
import com.demo.events.spec.KafkaEvent;
@KafkaEvent(name="EMAIL_SENT", topic="email.sent", description="Email dispatched to provider")
public class EmailSentEvent extends BaseEvent {
    public String messageId; public String templateId;
    public EmailSentEvent() {}
    public EmailSentEvent(String userId, String messageId, String templateId) {
        super(userId); this.messageId = messageId; this.templateId = templateId;
    }
}
