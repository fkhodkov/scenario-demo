package com.demo.events.spec;

import java.lang.annotation.*;

/**
 * Marks a class as a Kafka-event DTO and declares its metadata.
 * The events-lib acts as the "external library" that downstream services
 * import to discover available event types.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KafkaEvent {
    /** Human-readable event name shown in the scenario editor. */
    String name();
    /** Kafka topic this event is produced/consumed on. */
    String topic();
    /** Short description for the UI. */
    String description() default "";
}
