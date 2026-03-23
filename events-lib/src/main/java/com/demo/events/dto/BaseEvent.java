package com.demo.events.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = UserRegisteredEvent.class,    name = "USER_REGISTERED"),
    @JsonSubTypes.Type(value = EmailSentEvent.class,         name = "EMAIL_SENT"),
    @JsonSubTypes.Type(value = EmailDeliveredEvent.class,    name = "EMAIL_DELIVERED"),
    @JsonSubTypes.Type(value = EmailFailedEvent.class,       name = "EMAIL_FAILED"),
    @JsonSubTypes.Type(value = EmailOpenedEvent.class,       name = "EMAIL_OPENED"),
    @JsonSubTypes.Type(value = LinkClickedEvent.class,       name = "LINK_CLICKED"),
    @JsonSubTypes.Type(value = PushSentEvent.class,          name = "PUSH_SENT"),
    @JsonSubTypes.Type(value = PushDeliveredEvent.class,     name = "PUSH_DELIVERED"),
    @JsonSubTypes.Type(value = PushFailedEvent.class,        name = "PUSH_FAILED"),
    @JsonSubTypes.Type(value = SmsDeliveredEvent.class,      name = "SMS_DELIVERED"),
    @JsonSubTypes.Type(value = SmsFailedEvent.class,         name = "SMS_FAILED"),
    @JsonSubTypes.Type(value = UserUnsubscribedEvent.class,  name = "USER_UNSUBSCRIBED"),
    @JsonSubTypes.Type(value = PurchaseCompletedEvent.class, name = "PURCHASE_COMPLETED")
})
public abstract class BaseEvent {
    public String eventId = UUID.randomUUID().toString();
    public String userId;
    public Instant occurredAt = Instant.now();

    protected BaseEvent() {}
    protected BaseEvent(String userId) { this.userId = userId; }
}
