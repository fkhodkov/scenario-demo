package com.demo.scenario.temporal.workflows;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IncomingEventTest {

    @Test
    void allArgsConstructor_setsFields() {
        IncomingEvent e = new IncomingEvent("EMAIL_OPENED", "email.opened", "user_1", "{\"msgId\":\"x\"}");
        assertEquals("EMAIL_OPENED",       e.eventType());
        assertEquals("email.opened",       e.topic());
        assertEquals("user_1",             e.userId());
        assertEquals("{\"msgId\":\"x\"}", e.payload());
        assertNull(e.messageId());
    }

    @Test
    void fiveArgConstructor_includesMessageId() {
        IncomingEvent e = new IncomingEvent("EMAIL_DELIVERED", "email.delivered", "user_1", "{}", "msg-abc");
        assertEquals("msg-abc", e.messageId());
    }

    @Test
    void recordEquality() {
        IncomingEvent a = new IncomingEvent("T", "t", "u", "p", "mid");
        IncomingEvent b = new IncomingEvent("T", "t", "u", "p", "mid");
        assertEquals(a, b);
    }
}
