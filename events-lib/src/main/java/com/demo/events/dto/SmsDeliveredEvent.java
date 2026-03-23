package com.demo.events.dto;
import com.demo.events.spec.KafkaEvent;
@KafkaEvent(name="SMS_DELIVERED", topic="sms.delivered", description="SMS confirmed delivered")
public class SmsDeliveredEvent extends BaseEvent {
    public String messageId;
    public SmsDeliveredEvent() {}
    public SmsDeliveredEvent(String userId, String messageId) { super(userId); this.messageId = messageId; }
}
