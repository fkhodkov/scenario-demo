package com.demo.scenario.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scenario_executions")
public class ScenarioExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String workflowId;

    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;

    private String currentNodeId;
    private Instant startedAt;
    private Instant finishedAt;

    public ScenarioExecution() {}

    @PrePersist
    void prePersist() { startedAt = Instant.now(); }

    // ── Getters ──
    public UUID getId()                  { return id; }
    public Scenario getScenario()        { return scenario; }
    public String getUserId()            { return userId; }
    public String getWorkflowId()        { return workflowId; }
    public ExecutionStatus getStatus()   { return status; }
    public String getCurrentNodeId()     { return currentNodeId; }
    public Instant getStartedAt()        { return startedAt; }
    public Instant getFinishedAt()       { return finishedAt; }

    // ── Setters ──
    public void setId(UUID id)                       { this.id = id; }
    public void setScenario(Scenario scenario)       { this.scenario = scenario; }
    public void setUserId(String userId)             { this.userId = userId; }
    public void setWorkflowId(String workflowId)     { this.workflowId = workflowId; }
    public void setStatus(ExecutionStatus status)    { this.status = status; }
    public void setCurrentNodeId(String nodeId)      { this.currentNodeId = nodeId; }
    public void setStartedAt(Instant startedAt)      { this.startedAt = startedAt; }
    public void setFinishedAt(Instant finishedAt)    { this.finishedAt = finishedAt; }

    // ── Builder ──
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ScenarioExecution e = new ScenarioExecution();
        public Builder scenario(Scenario v)        { e.scenario = v; return this; }
        public Builder userId(String v)            { e.userId = v; return this; }
        public Builder workflowId(String v)        { e.workflowId = v; return this; }
        public Builder status(ExecutionStatus v)   { e.status = v; return this; }
        public Builder currentNodeId(String v)     { e.currentNodeId = v; return this; }
        public ScenarioExecution build()           { return e; }
    }
}
