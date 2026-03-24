package com.demo.simulator.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SimulatorController.class)
class SimulatorControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean
    @SuppressWarnings("rawtypes")
    KafkaTemplate kafkaTemplate;

    // ── GET /api/simulator/events ─────────────────────────────────────────────

    @Test
    void getEventCatalog_returnsAllKnownEvents() throws Exception {
        mockMvc.perform(get("/api/simulator/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThan(5))))
                .andExpect(jsonPath("$[*].eventType", hasItem("USER_REGISTERED")))
                .andExpect(jsonPath("$[*].topic",     hasItem("user.registered")))
                .andExpect(jsonPath("$[*].description", everyItem(notNullValue())));
    }

    // ── POST /api/simulator/publish ───────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void publishEvent_validRequest_sendsToKafka() throws Exception {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        Map<String, Object> body = Map.of(
                "eventType", "USER_REGISTERED",
                "userId",    "user_test_1");

        mockMvc.perform(post("/api/simulator/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("published")))
                .andExpect(jsonPath("$.topic",  is("user.registered")))
                .andExpect(jsonPath("$.json",   containsString("user_test_1")));

        verify(kafkaTemplate).send(eq("user.registered"), eq("user_test_1"), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishEvent_withExplicitTopic_usesProvidedTopic() throws Exception {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        Map<String, Object> body = Map.of(
                "eventType", "CUSTOM_EVENT",
                "userId",    "u1",
                "topic",     "custom.topic");

        mockMvc.perform(post("/api/simulator/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic", is("custom.topic")));

        verify(kafkaTemplate).send(eq("custom.topic"), eq("u1"), anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishEvent_withExtraFields_includesInPayload() throws Exception {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        Map<String, Object> body = Map.of(
                "eventType", "PURCHASE_COMPLETED",
                "userId",    "u1",
                "extra",     Map.of("orderId", "ord_123", "amount", 49.99));

        mockMvc.perform(post("/api/simulator/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.json", containsString("ord_123")))
                .andExpect(jsonPath("$.json", containsString("amount")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publishEvent_unknownEventType_derivesTopicFromName() throws Exception {
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));

        Map<String, Object> body = Map.of(
                "eventType", "SOME_CUSTOM_EVENT",
                "userId",    "u1");

        mockMvc.perform(post("/api/simulator/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                // unknown event type → convert SOME_CUSTOM_EVENT → some.custom.event
                .andExpect(jsonPath("$.topic", is("some.custom.event")));
    }

    // ── GET /api/simulator/outbound ───────────────────────────────────────────

    @Test
    void getOutbound_initiallyEmpty() throws Exception {
        mockMvc.perform(get("/api/simulator/outbound"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── GET /api/simulator/log ────────────────────────────────────────────────

    @Test
    void getLog_returnsArray() throws Exception {
        // The log is an in-memory list shared across the test class context;
        // we only assert it is a JSON array, not that it is empty.
        mockMvc.perform(get("/api/simulator/log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── POST /api/simulator/outbound/{id}/deliver ─────────────────────────────

    @Test
    void deliver_messageNotFound_returns500() throws Exception {
        mockMvc.perform(post("/api/simulator/outbound/nonexistent-id/deliver"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void fail_messageNotFound_returns500() throws Exception {
        mockMvc.perform(post("/api/simulator/outbound/nonexistent-id/fail"))
                .andExpect(status().is5xxServerError());
    }
}
