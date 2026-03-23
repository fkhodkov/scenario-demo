package com.demo.events.spec;

import java.util.*;

/**
 * Static registry of all known event specifications.
 * In a real system this would scan the classpath; here we register explicitly
 * so the demo remains self-contained without a full classpath scan dependency.
 */
public class EventRegistry {

    public record EventSpec(String eventType, String topic, String description, Class<?> dtoClass) {}

    private static final List<EventSpec> REGISTRY = new ArrayList<>();

    static {
        // Auto-register every annotated DTO listed here
        List<Class<?>> dtoClasses = List.of(
            com.demo.events.dto.UserRegisteredEvent.class,
            com.demo.events.dto.EmailSentEvent.class,
            com.demo.events.dto.EmailDeliveredEvent.class,
            com.demo.events.dto.EmailFailedEvent.class,
            com.demo.events.dto.EmailOpenedEvent.class,
            com.demo.events.dto.LinkClickedEvent.class,
            com.demo.events.dto.PushSentEvent.class,
            com.demo.events.dto.PushDeliveredEvent.class,
            com.demo.events.dto.PushFailedEvent.class,
            com.demo.events.dto.SmsDeliveredEvent.class,
            com.demo.events.dto.SmsFailedEvent.class,
            com.demo.events.dto.UserUnsubscribedEvent.class,
            com.demo.events.dto.PurchaseCompletedEvent.class
        );
        for (Class<?> cls : dtoClasses) {
            KafkaEvent ann = cls.getAnnotation(KafkaEvent.class);
            if (ann != null) {
                REGISTRY.add(new EventSpec(ann.name(), ann.topic(), ann.description(), cls));
            }
        }
    }

    public static List<EventSpec> all() {
        return Collections.unmodifiableList(REGISTRY);
    }

    public static Optional<EventSpec> findByTopic(String topic) {
        return REGISTRY.stream().filter(e -> e.topic().equals(topic)).findFirst();
    }

    public static Optional<EventSpec> findByName(String name) {
        return REGISTRY.stream().filter(e -> e.eventType().equals(name)).findFirst();
    }
}
