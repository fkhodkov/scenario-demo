package com.demo.events.dto;
import com.demo.events.spec.KafkaEvent;
@KafkaEvent(name="SMS_FAILED", topic="sms.failed", description="SMS delivery failed")
public class SmsFailedEvent extends BaseEvent {
    public String messageId; public String reason;
    public SmsFailedEvent() {}
    public SmsFailedEvent(String userId, String messageId, String reason) {
        super(userId); this.messageId = messageId; this.reason = reason;
    }
}
