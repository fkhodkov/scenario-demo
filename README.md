# Scenario Platform — Communication Automation with Temporal + React Flow

A prototype of a **visual communication scenario editor** backed by **Temporal workflows**.
Non-technical managers draw multi-step communication flows in a browser; the backend executes
them at scale using Kafka events, with PostgreSQL as both the scenario store and Temporal
persistence backend.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│  Browser                                                        │
│  ┌──────────────────────────┐  ┌──────────────────────────┐    │
│  │  React Flow Editor       │  │  Simulator Panel         │    │
│  │  (scenario-editor :3000) │  │  publish events / ACK    │    │
│  └──────────┬───────────────┘  └────────────┬─────────────┘    │
└─────────────┼────────────────────────────────┼──────────────────┘
              │ REST /api                       │ REST /sim
              ▼                                 ▼
┌─────────────────────────┐      ┌──────────────────────────┐
│  scenario-service :8080 │      │  simulator-service :8081 │
│  Spring Boot + Temporal │      │  Spring Boot             │
│  ┌───────────────────┐  │      │  • Publishes events      │
│  │ ScenarioWorkflow  │  │      │  • Intercepts outbound   │
│  │ (graph walker)    │  │      │  • ACK / NACK comms      │
│  └───────────────────┘  │      └──────────┬───────────────┘
│  ┌───────────────────┐  │                 │
│  │ UniversalConsumer │◄─┼─────────────────┤
│  │ (Kafka → signal)  │  │                 │
│  └───────────────────┘  │                 │
└────────┬────────────────┘                 │
         │                                  │
         ▼                   ▼              ▼
┌─────────────────┐  ┌──────────────┐  ┌──────────┐
│  Temporal       │  │  PostgreSQL  │  │  Kafka   │
│  (PostgreSQL)   │  │  (scenarios) │  │  (KRaft) │
└─────────────────┘  └──────────────┘  └──────────┘
```

### Key design decisions

| Concern | Choice | Rationale |
|---------|--------|-----------|
| Workflow engine | **Temporal** | Durable timers, signals, replay |
| Temporal storage | **PostgreSQL** | Same instance as scenario store; simpler ops, no extra infra |
| Scenario storage | **PostgreSQL** | JSONB graph + relational metadata |
| Event bus | **Kafka** | Decoupled delivery; universal consumer bridges to Temporal signals |
| Graph format | **React Flow JSON** | Native serialization — no mapping layer |
| Delivery branching | **sourceHandle** on edges | `delivered`/`failed`/`timeout` handles on `SEND_*` nodes |
| Message correlation | **messageId** | Each outbound comm carries a UUID; delivery events must match it to prevent cross-contamination |
| Duplicate prevention | **DB guard** | One RUNNING execution per user per scenario; enforced in `startExecution` |

---

## Module structure

```
scenario-platform/
├── events-lib/              # Shared event DTO library (sealed interface + records)
│   └── src/…/events/
│       ├── spec/
│       │   ├── KafkaEvent.java       # annotation
│       │   └── EventRegistry.java    # static catalog (record EventSpec)
│       └── dto/
│           ├── BaseEvent.java        # sealed interface
│           └── *Event.java           # one record per event type
│
├── scenario-service/        # Main service  (Spring Boot 3.4 + Temporal 1.32)
│   └── src/…/scenario/
│       ├── domain/           # JPA entities (Scenario, ScenarioExecution)
│       ├── dto/              # ScenarioGraph record (mirrors React Flow JSON)
│       ├── kafka/            # UniversalEventConsumer (topicPattern=".*)
│       ├── repository/       # Spring Data JPA repos
│       ├── service/          # ScenarioService, EventSignalService
│       ├── temporal/
│       │   ├── activities/   # ScenarioActivities + impl (send → Kafka comm.outbound)
│       │   └── workflows/    # ScenarioWorkflow + impl (graph walker)
│       └── web/              # REST controllers + GlobalExceptionHandler
│
├── simulator-service/       # Test harness  (Spring Boot 3.4)
│   └── src/…/simulator/
│       └── web/SimulatorController.java
│
├── scenario-editor/         # React + React Flow frontend  (Vite, Tailwind, Zustand)
│   └── src/
│       ├── api/              # Axios clients + types
│       ├── store/            # Zustand editor state
│       └── components/
│           ├── FlowNodes.tsx          # Custom React Flow node renderers
│           ├── NodePalette.tsx        # Drag-and-drop node palette
│           ├── NodePropertiesPanel.tsx# Properties inspector
│           ├── SimulatorPanel.tsx     # In-browser test panel
│           ├── ScenarioEditor.tsx     # React Flow canvas
│           ├── ScenarioListPage.tsx
│           └── EditorPage.tsx
│
├── docker-compose.yml       # Full stack (no Zookeeper — Kafka runs in KRaft mode)
└── temporal-config/         # Temporal dynamic config (development-sql.yaml)
```

---

## Node types

| Node | Icon | Description |
|------|------|-------------|
| **Trigger** | ⚡ | Entry point — fires when a Kafka event matches `triggerTopic` |
| **Send Email** | ✉️ | Dispatches email; exposes `delivered` / `failed` output handles |
| **Send Push** | 🔔 | Push notification with same delivery handles |
| **Send SMS** | 💬 | SMS with same delivery handles |
| **Wait Event** | 🕐 | Blocks until a Kafka event arrives or timeout fires |
| **Delay** | ⏱ | Fixed-duration sleep (durable — survives worker restarts) |
| **Condition** | ⑂ | Branches on a field value → `true` / `false` handles |
| **End** | ■ | Terminal node |

### Delivery branching

Every `SEND_*` node has three output handles:

```
            ┌──[ delivered ]──► next happy-path node
Send Email ─┤
            ├──[ failed ]─────► fallback (e.g. push, or end)
            │
            └──[ timeout ]────► (implicit: no delivery confirmation within window)
```

The Temporal workflow waits for a `{CHANNEL}_DELIVERED` or `{CHANNEL}_FAILED` Kafka signal.
The signal is matched by **messageId** so that unrelated delivery events for the same user
(from outside this scenario) cannot accidentally advance the workflow.

In the demo, the **Simulator** panel shows all outbound messages and lets you click
**Deliver** or **Fail** to send the corresponding Kafka event.

---

## Startup

### Prerequisites

- Docker + Docker Compose v2
- Java 21 + Maven 3.9 (for building outside Docker)
- Node 20 (for frontend dev mode)

### Full stack (Docker)

```bash
docker compose up --build
```

PostgreSQL starts with a health check; Temporal waits until it is ready before
connecting. Typical cold-start time is under 30 seconds.

| Service | URL |
|---------|-----|
| **Scenario Editor** | http://localhost:3000 |
| **Temporal UI** | http://localhost:8088 |
| **Kafka UI** | http://localhost:8090 |
| **Swagger (scenario-service)** | http://localhost:8080/swagger-ui.html |
| **Swagger (simulator-service)** | http://localhost:8081/swagger-ui.html |

### Local dev (no Docker for services)

```bash
# Start infrastructure only
docker compose up zookeeper kafka postgres temporal temporal-ui kafka-ui

# Build & run scenario-service
cd scenario-service && mvn spring-boot:run

# Build & run simulator-service
cd ../simulator-service && mvn spring-boot:run

# Run editor dev server
cd ../scenario-editor && npm install && npm run dev
```

---

## Demo walkthrough

1. **Open** http://localhost:3000 → click **New Scenario**
2. The editor opens with a blank canvas. Drag a **Trigger** node from the palette, configure
   it for `USER_REGISTERED`, then wire up a **Send Email** → **End** graph.
3. **Save** the scenario, then click **Activate**.
4. Click the **Simulator** button (top-right of the editor) to open the test panel.
5. In Simulator → **Publish**: choose `USER_REGISTERED`, enter `user_001`, click **Publish Event**.
   - `UniversalEventConsumer` picks this up on `user.registered`
   - Because the scenario is ACTIVE and triggered by that topic, a Temporal workflow starts.
   - Only one execution per user per scenario is allowed — a second trigger is a no-op while
     the first is still running.
6. The workflow reaches **Send Email** and publishes to `comm.outbound` with a unique `messageId`.
7. In Simulator → **Outbound**: you'll see a pending email for `user_001`.
   - Click **Deliver** → publishes `EMAIL_DELIVERED` (with the matching `messageId`) → workflow
     takes the `delivered` branch.
   - Click **Fail** → publishes `EMAIL_FAILED` → workflow takes the `failed` branch.
8. Watch the workflow progress in the **Temporal UI** (http://localhost:8088).

---

## Extending the events library

Events are **records implementing a sealed interface**. Add a new event:

```java
// events-lib/src/main/java/com/demo/events/dto/CartAbandonedEvent.java
@KafkaEvent(name = "CART_ABANDONED", topic = "cart.abandoned", description = "User left items in cart")
public record CartAbandonedEvent(
        String  eventId,
        String  userId,
        Instant occurredAt,
        String  cartId,
        double  cartValue
) implements BaseEvent {
    public static CartAbandonedEvent of(String userId, String cartId, double cartValue) {
        return new CartAbandonedEvent(UUID.randomUUID().toString(), userId, Instant.now(), cartId, cartValue);
    }
}
```

Then add it to:
1. The `permits` clause in `BaseEvent.java`
2. The `@JsonSubTypes` list in `BaseEvent.java`
3. The `dtoClasses` list in `EventRegistry.java`

It then automatically appears in the editor's event dropdowns and the simulator's event catalog.

---

## Tech stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Workflow engine | Temporal Java SDK | 1.32.1 |
| Backend framework | Spring Boot | 3.4.13 |
| Persistence | PostgreSQL | 16 |
| Message broker | Kafka (KRaft) | Confluent 7.6 |
| Frontend | React + React Flow | 18 + 11 |
| State management | Zustand | 4.5 |
| Build | Maven 3.9 / Vite 5 | |
| Tests | JUnit 5, Mockito, Testcontainers | managed by Spring Boot BOM |

---

## Production considerations

- **Kafka consumer group isolation**: split `UniversalEventConsumer` into per-scenario consumers
  using dynamic topic subscription to avoid unnecessary cross-scenario signal traffic.
- **Workflow deduplication**: the current guard (one RUNNING execution per user per scenario)
  is in-process; for multi-replica deployments use Temporal's `workflowId` uniqueness guarantee
  by setting `workflowId = scenarioId + "-" + userId` without the UUID suffix, and let Temporal
  reject duplicates with `WorkflowExecutionAlreadyStarted`.
- **Unsubscribe check**: implement as a synchronous activity that queries a suppression list
  *before* dispatching, returning `accepted=false` to immediately branch to the `failed` path.
- **PostgreSQL tuning**: Temporal creates its own schema (`temporal`, `temporal_visibility`) in
  the same PostgreSQL instance. For production, use separate databases or a dedicated instance
  with connection pooling via PgBouncer.
- **KEDA autoscaling**: scale Temporal workers based on `temporal_workflow_task_queue_active_tasks`
  metric exposed via Temporal's Prometheus endpoint.
