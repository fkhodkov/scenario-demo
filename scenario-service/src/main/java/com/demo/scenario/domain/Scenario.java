package com.demo.scenario.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scenarios")
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScenarioStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String definition;

    private String triggerTopic;
    private String triggerEventType;
    private Instant createdAt;
    private Instant updatedAt;

    public Scenario() {}

    @PrePersist
    void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    // ── Getters ──
    public UUID getId()                  { return id; }
    public String getName()              { return name; }
    public String getDescription()       { return description; }
    public ScenarioStatus getStatus()    { return status; }
    public String getDefinition()        { return definition; }
    public String getTriggerTopic()      { return triggerTopic; }
    public String getTriggerEventType()  { return triggerEventType; }
    public Instant getCreatedAt()        { return createdAt; }
    public Instant getUpdatedAt()        { return updatedAt; }

    // ── Setters ──
    public void setId(UUID id)                         { this.id = id; }
    public void setName(String name)                   { this.name = name; }
    public void setDescription(String description)     { this.description = description; }
    public void setStatus(ScenarioStatus status)       { this.status = status; }
    public void setDefinition(String definition)       { this.definition = definition; }
    public void setTriggerTopic(String triggerTopic)   { this.triggerTopic = triggerTopic; }
    public void setTriggerEventType(String t)          { this.triggerEventType = t; }
    public void setCreatedAt(Instant createdAt)        { this.createdAt = createdAt; }
    public void setUpdatedAt(Instant updatedAt)        { this.updatedAt = updatedAt; }

    // ── Builder ──
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Scenario s = new Scenario();
        public Builder name(String v)               { s.name = v; return this; }
        public Builder description(String v)        { s.description = v; return this; }
        public Builder status(ScenarioStatus v)     { s.status = v; return this; }
        public Builder definition(String v)         { s.definition = v; return this; }
        public Builder triggerTopic(String v)       { s.triggerTopic = v; return this; }
        public Builder triggerEventType(String v)   { s.triggerEventType = v; return this; }
        public Scenario build()                     { return s; }
    }
}
