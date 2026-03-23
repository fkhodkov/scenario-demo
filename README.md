# Scenario Platform — Communication Automation with Temporal + React Flow

A prototype of a **visual communication scenario editor** backed by **Temporal workflows**.
Non-technical managers draw multi-step communication flows in a browser; the backend executes
them at scale using Kafka events, with ScyllaDB as the Temporal persistence backend.

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
│  (ScyllaDB)     │  │  (scenarios) │  │          │
└─────────────────┘  └──────────────┘  └──────────┘
```

### Key design decisions

| Concern | Choice | Rationale |
|---------|--------|-----------|
| Workflow engine | **Temporal** | Durable timers, signals, replay |
| Temporal storage | **ScyllaDB** | Cassandra-compatible, high throughput |
| Scenario storage | **PostgreSQL** | JSONB graph + relational metadata |
| Event bus | **Kafka** | Decoupled delivery; universal consumer bridges to Temporal signals |
| Graph format | **React Flow JSON** | Native serialization — no mapping layer |
| Delivery branching | **sourceHandle** on edges | `delivered`/`failed`/`timeout` handles on SEND_* nodes |

---

## Module structure

```
scenario-platform/
├── events-lib/              # Shared event DTO library
│   └── src/…/events/
│       ├── spec/
│       │   ├── KafkaEvent.java       # annotation
│       │   └── EventRegistry.java    # static catalog
│       └── dto/                      # one class per event
│
├── scenario-service/        # Main service
│   └── src/…/scenario/
│       ├── domain/           # JPA entities
│       ├── dto/              # ScenarioGraph (mirrors React Flow)
│       ├── kafka/            # UniversalEventConsumer
│       ├── repository/       # Spring Data JPA repos
│       ├── service/          # ScenarioService, EventSignalService
│       ├── temporal/
│       │   ├── activities/   # ScenarioActivities + impl
│       │   └── workflows/    # ScenarioWorkflow + impl (graph walker)
│       └── web/              # REST controllers
│
├── simulator-service/       # Test harness
│   └── src/…/simulator/
│       └── web/SimulatorController.java
│
├── scenario-editor/         # React + React Flow frontend
│   └── src/
│       ├── api/              # Axios clients + types
│       ├── store/            # Zustand editor state
│       ├── components/
│       │   ├── nodes/        # Custom React Flow node components
│       │   ├── panels/       # Palette, Properties inspector
│       │   ├── simulator/    # SimulatorPanel
│       │   ├── ScenarioEditor.tsx
│       │   ├── ScenarioListPage.tsx
│       │   └── EditorPage.tsx
│       └── App.tsx
│
├── docker-compose.yml       # Full stack
└── temporal-config/         # Dynamic config for Temporal+Scylla
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

The Temporal workflow waits for a `{CHANNEL}_DELIVERED` or `{CHANNEL}_FAILED` event signal.
In the demo the **Simulator** panel shows all outbound messages and lets you click
**Deliver** or **Fail** to send the corresponding Kafka event.

---

## Startup

### Prerequisites

- Docker + Docker Compose v2
- Java 21 + Maven 3.9 (for building outside Docker)
- Node 20 (for frontend dev mode)

### Full stack (Docker)

```bash
# 1. Build events-lib and install to local Maven cache
cd events-lib && mvn install -q && cd ..

# 2. Start everything
docker-compose up --build

# ScyllaDB takes ~60s to become ready before Temporal can start.
# If Temporal exits early, docker-compose up will restart it automatically.
```

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
docker-compose up zookeeper kafka postgres scylla temporal temporal-ui kafka-ui

# Build & run scenario-service
cd scenario-service
mvn spring-boot:run

# Build & run simulator-service
cd ../simulator-service
mvn spring-boot:run

# Run editor dev server
cd ../scenario-editor
npm install
npm run dev
```

---

## Demo walkthrough

1. **Open** http://localhost:3000 → click **New Scenario**
2. The editor opens with a starter graph:
   `USER_REGISTERED → Send Email → [delivered] Wait Email Opened / [failed] End`
3. Customise the graph by dragging nodes from the palette and connecting handles.
4. **Save** the scenario, then click **Activate**.
5. Click the **Simulator** button (top-right of the editor) to open the test panel.
6. In Simulator → **Publish**: choose `USER_REGISTERED`, enter `user_001`, click **Publish Event**.
   - The `UniversalEventConsumer` picks this up on `user.registered`
   - Because the scenario is ACTIVE and triggered by that topic, a Temporal workflow starts.
7. The workflow reaches `Send Email` and publishes to `comm.outbound`.
8. In Simulator → **Outbound**: you'll see a pending email for `user_001`.
   - Click **Deliver** → publishes `EMAIL_DELIVERED` → workflow takes the `delivered` branch.
   - Click **Fail** → publishes `EMAIL_FAILED` → workflow takes the `failed` branch.
9. Watch the workflow progress in the **Temporal UI** (http://localhost:8088).

---

## Extending the events library

Add a new event:

```java
// In events-lib/src/main/java/com/demo/events/dto/
@KafkaEvent(name = "CART_ABANDONED", topic = "cart.abandoned", description = "User left items in cart")
public class CartAbandonedEvent extends BaseEvent {
    public String cartId;
    public double cartValue;
    // …
}
```

Register it in `EventRegistry.java` → it automatically appears in the editor's event
dropdowns and the simulator's event catalog.

---

## Production considerations

- **Kafka consumer group isolation**: split `UniversalEventConsumer` into per-scenario consumers
  using dynamic topic subscription to avoid unnecessary cross-scenario signal traffic.
- **Workflow deduplication**: use `workflowId = scenarioId + userId + triggerEventId` to prevent
  duplicate executions for the same event.
- **Unsubscribe check**: implement as a synchronous activity that queries a suppression list
  *before* dispatching, returning `accepted=false` to immediately branch to the `failed` path.
- **ScyllaDB tuning**: use `NetworkTopologyStrategy` with RF=3 for Temporal's keyspace in prod.
- **KEDA autoscaling**: scale Temporal workers based on `temporalWorkflowTaskQueueActiveTasks` metric.
