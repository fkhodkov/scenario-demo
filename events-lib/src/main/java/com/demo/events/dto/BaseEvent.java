package com.demo.events.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;

/**
 * Sealed interface for all Kafka event DTOs.
 *
 * Implementations are records — immutable, self-describing, exhaustively
 * known to the compiler. The sealed hierarchy replaces the old abstract class
 * so switch expressions over event types are exhaustiveness-checked.
 *
 * Jackson uses @JsonTypeInfo to embed "eventType" in the JSON and
 * @JsonSubTypes to route deserialization to the correct record.
 * Records are deserialized via their canonical constructor (field names
 * must match JSON property names).
 */
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
public sealed interface BaseEvent
        permits UserRegisteredEvent, EmailSentEvent, EmailDeliveredEvent,
                EmailFailedEvent, EmailOpenedEvent, LinkClickedEvent,
                PushSentEvent, PushDeliveredEvent, PushFailedEvent,
                SmsDeliveredEvent, SmsFailedEvent,
                UserUnsubscribedEvent, PurchaseCompletedEvent {

    String  eventId();
    String  userId();
    Instant occurredAt();
}
