package com.demo.scenario.temporal.activities;

/**
 * Result of dispatching an outbound communication.
 * Immutable — Temporal serializes this across activity boundaries.
 */
public record SendResult(
        String messageId,
        boolean accepted,
        String channel
) {}
