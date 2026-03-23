package com.demo.scenario.web;

import com.demo.events.spec.EventRegistry;
import com.demo.scenario.domain.Scenario;
import com.demo.scenario.domain.ScenarioExecution;
import com.demo.scenario.service.ScenarioService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ScenarioController {

    private final ScenarioService service;

    public ScenarioController(ScenarioService service) {
        this.service = service;
    }

    @GetMapping("/events")
    public List<Map<String, String>> listEvents() {
        return EventRegistry.all().stream()
                .map(e -> Map.of(
                        "eventType",   e.eventType(),
                        "topic",       e.topic(),
                        "description", e.description()))
                .collect(Collectors.toList());
    }

    @GetMapping("/scenarios")
    public List<Scenario> list() { return service.listAll(); }

    @GetMapping("/scenarios/{id}")
    public Scenario get(@PathVariable UUID id) { return service.findById(id); }

    @PostMapping("/scenarios")
    public Scenario create(@RequestBody ScenarioRequest req) throws Exception {
        return service.create(req.getName(), req.getDescription(), req.getDefinition());
    }

    @PutMapping("/scenarios/{id}")
    public Scenario update(@PathVariable UUID id, @RequestBody ScenarioRequest req) throws Exception {
        return service.update(id, req.getName(), req.getDescription(), req.getDefinition());
    }

    @PostMapping("/scenarios/{id}/activate")
    public Scenario activate(@PathVariable UUID id) { return service.activate(id); }

    @PostMapping("/scenarios/{id}/pause")
    public Scenario pause(@PathVariable UUID id)    { return service.pause(id); }

    @GetMapping("/scenarios/{id}/executions")
    public List<ScenarioExecution> executions(@PathVariable UUID id) {
        return service.listExecutions(id);
    }

    @PostMapping("/scenarios/{id}/execute")
    public ScenarioExecution manualStart(@PathVariable UUID id,
                                          @RequestBody ManualStartRequest req) {
        return service.startExecution(id, req.getUserId(),
                req.getPayload() != null ? req.getPayload() : "{}");
    }

    // ── Request bodies ────────────────────────────────────────────────────────

    public static class ScenarioRequest {
        private String name;
        private String description;
        private String definition;
        public String getName()        { return name; }
        public String getDescription() { return description; }
        public String getDefinition()  { return definition; }
        public void setName(String name)               { this.name = name; }
        public void setDescription(String description) { this.description = description; }
        public void setDefinition(String definition)   { this.definition = definition; }
    }

    public static class ManualStartRequest {
        private String userId;
        private String payload;
        public String getUserId()  { return userId; }
        public String getPayload() { return payload; }
        public void setUserId(String userId)   { this.userId = userId; }
        public void setPayload(String payload) { this.payload = payload; }
    }
}
