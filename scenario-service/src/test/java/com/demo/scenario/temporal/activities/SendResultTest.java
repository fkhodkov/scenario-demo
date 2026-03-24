package com.demo.scenario.temporal.activities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SendResultTest {

    @Test
    void allArgsConstructor_setsFields() {
        SendResult r = new SendResult("msg-123", true, "email");
        assertEquals("msg-123", r.getMessageId());
        assertTrue(r.isAccepted());
        assertEquals("email", r.getChannel());
    }

    @Test
    void noArgsConstructor_thenSetters() {
        SendResult r = new SendResult();
        r.setMessageId("msg-456");
        r.setAccepted(false);
        r.setChannel("push");

        assertEquals("msg-456", r.getMessageId());
        assertFalse(r.isAccepted());
        assertEquals("push", r.getChannel());
    }

    @Test
    void notAccepted_representsUnsubscribed() {
        SendResult r = new SendResult("msg-789", false, "sms");
        assertFalse(r.isAccepted(), "accepted=false means user is unsubscribed or blocked");
    }
}
