package com.demo.events.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that all event DTOs serialize/deserialize correctly through Jackson.
 * The polymorphic @JsonTypeInfo + @JsonSubTypes on BaseEvent is critical for
 * the Kafka message envelope pattern used by the simulator.
 */
class EventDtoSerializationTest {

    ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.findAndRegisterModules(); // picks up JavaTimeModule for Instant
    }

    // ── Individual DTO tests ──────────────────────────────────────────────────

    @Test
    void userRegisteredEvent_roundTrips() throws Exception {
        UserRegisteredEvent original = new UserRegisteredEvent("user_1", "test@example.com", "email");
        String json     = mapper.writeValueAsString(original);
        BaseEvent restored = mapper.readValue(json, BaseEvent.class);

        assertInstanceOf(UserRegisteredEvent.class, restored);
        UserRegisteredEvent typed = (UserRegisteredEvent) restored;
        assertEquals("user_1",            typed.userId);
        assertEquals("test@example.com",  typed.email);
        assertEquals("email",             typed.channel);
        assertNotNull(typed.eventId);
        assertNotNull(typed.occurredAt);
    }

    @Test
    void emailDeliveredEvent_roundTrips() throws Exception {
        EmailDeliveredEvent ev = new EmailDeliveredEvent("user_2", "msg_123");
        String json = mapper.writeValueAsString(ev);
        BaseEvent restored = mapper.readValue(json, BaseEvent.class);

        assertInstanceOf(EmailDeliveredEvent.class, restored);
        assertEquals("msg_123", ((EmailDeliveredEvent) restored).messageId);
    }

    @Test
    void emailFailedEvent_roundTrips() throws Exception {
        EmailFailedEvent ev = new EmailFailedEvent("user_3", "msg_456", "bounce");
        String json = mapper.writeValueAsString(ev);
        BaseEvent restored = mapper.readValue(json, BaseEvent.class);

        assertInstanceOf(EmailFailedEvent.class, restored);
        EmailFailedEvent typed = (EmailFailedEvent) restored;
        assertEquals("msg_456", typed.messageId);
        assertEquals("bounce",  typed.reason);
    }

    @Test
    void linkClickedEvent_roundTrips() throws Exception {
        LinkClickedEvent ev = new LinkClickedEvent("user_4", "msg_789", "https://example.com/promo");
        String json = mapper.writeValueAsString(ev);
        BaseEvent restored = mapper.readValue(json, BaseEvent.class);

        assertInstanceOf(LinkClickedEvent.class, restored);
        assertEquals("https://example.com/promo", ((LinkClickedEvent) restored).url);
    }

    @Test
    void pushDeliveredEvent_roundTrips() throws Exception {
        PushDeliveredEvent ev = new PushDeliveredEvent("user_5", "notif_001");
        String json = mapper.writeValueAsString(ev);
        BaseEvent restored = mapper.readValue(json, BaseEvent.class);

        assertInstanceOf(PushDeliveredEvent.class, restored);
        assertEquals("notif_001", ((PushDeliveredEvent) restored).notificationId);
    }

    @Test
    void purchaseCompletedEvent_roundTrips() throws Exception {
        PurchaseCompletedEvent ev = new PurchaseCompletedEvent("user_6", "ord_001", 99.99);
        String json = mapper.writeValueAsString(ev);
        BaseEvent restored = mapper.readValue(json, BaseEvent.class);

        assertInstanceOf(PurchaseCompletedEvent.class, restored);
        PurchaseCompletedEvent typed = (PurchaseCompletedEvent) restored;
        assertEquals("ord_001", typed.orderId);
        assertEquals(99.99,     typed.amount, 0.001);
    }

    @Test
    void userUnsubscribedEvent_roundTrips() throws Exception {
        UserUnsubscribedEvent ev = new UserUnsubscribedEvent("user_7", "email");
        String json = mapper.writeValueAsString(ev);
        BaseEvent restored = mapper.readValue(json, BaseEvent.class);

        assertInstanceOf(UserUnsubscribedEvent.class, restored);
        assertEquals("email", ((UserUnsubscribedEvent) restored).channel);
    }

    // ── Shared contract tests ────────────────────────────────────────────────

    @Test
    void baseEvent_hasEventIdAndOccurredAt() {
        UserRegisteredEvent ev = new UserRegisteredEvent("u", "e@e.com", "email");
        assertNotNull(ev.eventId,    "eventId must be auto-generated");
        assertNotNull(ev.occurredAt, "occurredAt must be auto-set");
        assertFalse(ev.eventId.isBlank());
    }

    @Test
    void twoEvents_haveUniqueEventIds() {
        EmailDeliveredEvent e1 = new EmailDeliveredEvent("u", "m1");
        EmailDeliveredEvent e2 = new EmailDeliveredEvent("u", "m2");
        assertNotEquals(e1.eventId, e2.eventId);
    }

    @Test
    void eventTypeDiscriminator_isPresentInJson() throws Exception {
        EmailOpenedEvent ev = new EmailOpenedEvent("user_1", "msg_1");
        String json = mapper.writeValueAsString(ev);
        assertTrue(json.contains("\"eventType\""),
                "JSON must contain eventType discriminator field, got: " + json);
        assertTrue(json.contains("EMAIL_OPENED"),
                "JSON must contain the event type value, got: " + json);
    }

    @Test
    void noArgsConstructors_allowDeserialization() throws Exception {
        // Simulate what Kafka's StringDeserializer hands to the consumer
        String raw = """
                {"eventType":"EMAIL_DELIVERED","eventId":"abc","userId":"u1",
                 "occurredAt":"2024-01-01T12:00:00Z","messageId":"m1"}
                """;
        BaseEvent ev = mapper.readValue(raw, BaseEvent.class);
        assertInstanceOf(EmailDeliveredEvent.class, ev);
        assertEquals("u1", ev.userId);
    }

    // ── Parameterized: all delivered/failed variants ─────────────────────────

    static Stream<BaseEvent> allEventInstances() {
        return Stream.of(
            new UserRegisteredEvent("u", "e@e.com", "email"),
            new EmailSentEvent("u", "m1", "t1"),
            new EmailDeliveredEvent("u", "m1"),
            new EmailFailedEvent("u", "m1", "bounce"),
            new EmailOpenedEvent("u", "m1"),
            new LinkClickedEvent("u", "m1", "https://x.com"),
            new PushSentEvent("u", "n1"),
            new PushDeliveredEvent("u", "n1"),
            new PushFailedEvent("u", "n1", "token_expired"),
            new SmsDeliveredEvent("u", "s1"),
            new SmsFailedEvent("u", "s1", "invalid_number"),
            new UserUnsubscribedEvent("u", "email"),
            new PurchaseCompletedEvent("u", "o1", 42.0)
        );
    }

    @ParameterizedTest
    @MethodSource("allEventInstances")
    void allEvents_serializeAndDeserializeWithoutLoss(BaseEvent original) throws Exception {
        String json    = mapper.writeValueAsString(original);
        BaseEvent back = mapper.readValue(json, BaseEvent.class);

        assertEquals(original.getClass(), back.getClass(),
                "Deserialized type must match original for " + original.getClass().getSimpleName());
        assertEquals(original.userId,     back.userId);
        assertEquals(original.eventId,    back.eventId);
    }

    @ParameterizedTest
    @MethodSource("allEventInstances")
    void allEvents_haveNonNullEventId(BaseEvent ev) {
        assertNotNull(ev.eventId);
        assertFalse(ev.eventId.isBlank());
    }

    @ParameterizedTest
    @MethodSource("allEventInstances")
    void allEvents_haveNonNullOccurredAt(BaseEvent ev) {
        assertNotNull(ev.occurredAt);
        // occurredAt must be in the recent past (within last minute)
        assertTrue(ev.occurredAt.isAfter(Instant.now().minusSeconds(60)));
    }
}
