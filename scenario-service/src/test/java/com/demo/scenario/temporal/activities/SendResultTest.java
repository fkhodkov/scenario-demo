package com.demo.scenario.temporal.activities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SendResultTest {

    @Test
    void constructor_setsFields() {
        SendResult r = new SendResult("msg-123", true, "email");
        assertEquals("msg-123", r.messageId());
        assertTrue(r.accepted());
        assertEquals("email", r.channel());
    }

    @Test
    void notAccepted_representsUnsubscribed() {
        SendResult r = new SendResult("msg-789", false, "sms");
        assertFalse(r.accepted(), "accepted=false means user is unsubscribed or blocked");
    }

    @Test
    void recordEquality() {
        assertEquals(new SendResult("x", true, "email"), new SendResult("x", true, "email"));
        assertNotEquals(new SendResult("x", true, "email"), new SendResult("x", false, "email"));
    }
}
