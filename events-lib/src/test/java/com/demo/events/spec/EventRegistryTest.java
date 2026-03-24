package com.demo.events.spec;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EventRegistry — the static catalog of all known Kafka events.
 * No Spring context needed.
 */
class EventRegistryTest {

    @Test
    void all_returnsNonEmptyList() {
        List<EventRegistry.EventSpec> specs = EventRegistry.all();
        assertNotNull(specs);
        assertFalse(specs.isEmpty());
    }

    @Test
    void all_containsExpectedEventTypes() {
        Set<String> types = EventRegistry.all().stream()
                .map(EventRegistry.EventSpec::eventType)
                .collect(Collectors.toSet());

        assertTrue(types.contains("USER_REGISTERED"));
        assertTrue(types.contains("EMAIL_SENT"));
        assertTrue(types.contains("EMAIL_DELIVERED"));
        assertTrue(types.contains("EMAIL_FAILED"));
        assertTrue(types.contains("EMAIL_OPENED"));
        assertTrue(types.contains("LINK_CLICKED"));
        assertTrue(types.contains("PUSH_SENT"));
        assertTrue(types.contains("PUSH_DELIVERED"));
        assertTrue(types.contains("PUSH_FAILED"));
        assertTrue(types.contains("SMS_DELIVERED"));
        assertTrue(types.contains("SMS_FAILED"));
        assertTrue(types.contains("USER_UNSUBSCRIBED"));
        assertTrue(types.contains("PURCHASE_COMPLETED"));
    }

    @Test
    void all_containsExpectedTopics() {
        Set<String> topics = EventRegistry.all().stream()
                .map(EventRegistry.EventSpec::topic)
                .collect(Collectors.toSet());

        assertTrue(topics.contains("user.registered"));
        assertTrue(topics.contains("email.delivered"));
        assertTrue(topics.contains("email.failed"));
        assertTrue(topics.contains("email.opened"));
        assertTrue(topics.contains("link.clicked"));
        assertTrue(topics.contains("push.delivered"));
        assertTrue(topics.contains("purchase.completed"));
    }

    @Test
    void all_returnsUnmodifiableList() {
        List<EventRegistry.EventSpec> specs = EventRegistry.all();
        assertThrows(UnsupportedOperationException.class, () -> specs.add(null));
    }

    @Test
    void all_eachSpecHasNonNullFields() {
        for (EventRegistry.EventSpec spec : EventRegistry.all()) {
            assertNotNull(spec.eventType(),   "eventType must not be null for " + spec);
            assertNotNull(spec.topic(),       "topic must not be null for " + spec);
            assertNotNull(spec.description(), "description must not be null for " + spec);
            assertNotNull(spec.dtoClass(),    "dtoClass must not be null for " + spec);
        }
    }

    @Test
    void findByTopic_knownTopic_returnsSpec() {
        Optional<EventRegistry.EventSpec> spec = EventRegistry.findByTopic("user.registered");

        assertTrue(spec.isPresent());
        assertEquals("USER_REGISTERED", spec.get().eventType());
        assertEquals("user.registered", spec.get().topic());
    }

    @Test
    void findByTopic_unknownTopic_returnsEmpty() {
        Optional<EventRegistry.EventSpec> spec = EventRegistry.findByTopic("nonexistent.topic");
        assertTrue(spec.isEmpty());
    }

    @Test
    void findByName_knownName_returnsSpec() {
        Optional<EventRegistry.EventSpec> spec = EventRegistry.findByName("EMAIL_DELIVERED");

        assertTrue(spec.isPresent());
        assertEquals("email.delivered", spec.get().topic());
    }

    @Test
    void findByName_unknownName_returnsEmpty() {
        Optional<EventRegistry.EventSpec> spec = EventRegistry.findByName("TOTALLY_UNKNOWN_EVENT");
        assertTrue(spec.isEmpty());
    }

    @Test
    void findByTopic_and_findByName_areConsistent() {
        // For every spec, findByTopic and findByName must return the same spec
        for (EventRegistry.EventSpec spec : EventRegistry.all()) {
            EventRegistry.EventSpec byTopic = EventRegistry.findByTopic(spec.topic()).orElseThrow();
            EventRegistry.EventSpec byName  = EventRegistry.findByName(spec.eventType()).orElseThrow();

            assertEquals(spec.eventType(), byTopic.eventType());
            assertEquals(spec.topic(),     byName.topic());
        }
    }

    @Test
    void deliveredAndFailedEvents_existAsSymmetricPairs() {
        Set<String> types = EventRegistry.all().stream()
                .map(EventRegistry.EventSpec::eventType)
                .collect(Collectors.toSet());

        // Every channel that has a DELIVERED event must also have a FAILED event
        for (String channel : List.of("EMAIL", "PUSH", "SMS")) {
            assertTrue(types.contains(channel + "_DELIVERED"),
                    channel + "_DELIVERED must be registered");
            assertTrue(types.contains(channel + "_FAILED"),
                    channel + "_FAILED must be registered");
        }
    }

    @Test
    void dtoClasses_areAnnotatedWithKafkaEvent() {
        for (EventRegistry.EventSpec spec : EventRegistry.all()) {
            KafkaEvent ann = spec.dtoClass().getAnnotation(KafkaEvent.class);
            assertNotNull(ann,
                    spec.dtoClass().getSimpleName() + " must be annotated with @KafkaEvent");
            assertEquals(spec.eventType(), ann.name());
            assertEquals(spec.topic(),     ann.topic());
        }
    }
}
