package com.demo.scenario.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for the Scenario domain object — no Spring context.
 */
class ScenarioTest {

    @Test
    void builder_setsAllFields() {
        Scenario s = Scenario.builder()
                .name("onboarding")
                .description("welcome flow")
                .status(ScenarioStatus.DRAFT)
                .definition("{\"nodes\":[]}")
                .triggerTopic("user.registered")
                .triggerEventType("USER_REGISTERED")
                .build();

        assertEquals("onboarding",       s.getName());
        assertEquals("welcome flow",     s.getDescription());
        assertEquals(ScenarioStatus.DRAFT, s.getStatus());
        assertEquals("{\"nodes\":[]}", s.getDefinition());
        assertEquals("user.registered",  s.getTriggerTopic());
        assertEquals("USER_REGISTERED",  s.getTriggerEventType());
        assertNull(s.getId(), "id not set until persisted");
    }

    @Test
    void setters_mutateFields() {
        Scenario s = new Scenario();
        s.setName("test");
        s.setStatus(ScenarioStatus.ACTIVE);
        s.setDefinition("{\"nodes\":[]}");

        assertEquals("test",             s.getName());
        assertEquals(ScenarioStatus.ACTIVE, s.getStatus());
    }

    @Test
    void defaultConstructor_producesEmptyObject() {
        Scenario s = new Scenario();
        assertNull(s.getId());
        assertNull(s.getName());
        assertNull(s.getStatus());
    }

    @Test
    void statusTransitions_areJustFields() {
        Scenario s = Scenario.builder().name("x").status(ScenarioStatus.DRAFT)
                .definition("{}").build();
        assertEquals(ScenarioStatus.DRAFT, s.getStatus());

        s.setStatus(ScenarioStatus.ACTIVE);
        assertEquals(ScenarioStatus.ACTIVE, s.getStatus());

        s.setStatus(ScenarioStatus.PAUSED);
        assertEquals(ScenarioStatus.PAUSED, s.getStatus());
    }
}
