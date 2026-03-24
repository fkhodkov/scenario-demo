package com.demo.scenario.temporal.workflows;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IncomingEventTest {

    @Test
    void allArgsConstructor_setsFields() {
        IncomingEvent e = new IncomingEvent("EMAIL_OPENED", "email.opened", "user_1", "{\"msgId\":\"x\"}");
        assertEquals("EMAIL_OPENED",      e.getEventType());
        assertEquals("email.opened",      e.getTopic());
        assertEquals("user_1",            e.getUserId());
        assertEquals("{\"msgId\":\"x\"}", e.getPayload());
    }

    @Test
    void noArgsConstructor_thenSetters() {
        IncomingEvent e = new IncomingEvent();
        e.setEventType("LINK_CLICKED");
        e.setUserId("user_2");
        assertEquals("LINK_CLICKED", e.getEventType());
        assertEquals("user_2",       e.getUserId());
    }
}
