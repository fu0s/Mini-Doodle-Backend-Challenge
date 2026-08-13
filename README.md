# Mini-Doodle Backend Challenge

A backend scheduling service: users create free/busy slots and meetings, and a Kafka-backed
async flow automatically marks participant availability.

## Architecture


### Key Design Decisions

- **GraphQL via Spring for GraphQL** — DataLoader/BatchLoader for every one-to-many/many-to-many resolution to avoid N+1
- **Kafka with Testcontainers (KRaft mode)** — production-realistic integration tests
- **Single docker-compose.yml** — orchestrates 4 services (db, kafka, scheduler-service, consumer-service)
- **Calendar as a domain concept** — derived from slots + meetings, not a persisted table
- **Validation layer** — Jakarta Bean Validation on input DTOs + explicit business-rule validation in services
- **MeetingId linkage** — source of truth for busy slots created by meetings

## Modules

| Module | Description |
|--------|-------------|
| `scheduler-service` | GraphQL API for slots, meetings, and calendar view |
| `consumer-service` | Kafka consumer that applies meeting events to participant slots |
| `shared` | Common entities, Kafka event DTOs, mappers, constants, and exception hierarchy |

## Quick Start

```bash
# Start all services (Postgres, Kafka, scheduler, consumer)
docker compose up
```

**Services:**
- Scheduler GraphQL: http://localhost:8080/graphql
- Manual test surface: http://localhost:8080/
- Postgres: `localhost:5432` (user: `doodle`, password: `doodle`, db: `doodle`)
- Kafka (host): `localhost:9092`

## Build & Test

```bash
# Full build (unit + integration tests)
mvn clean verify

# Test one module
mvn -pl scheduler-service -am test
mvn -pl consumer-service -am test
mvn -pl shared -am test

# Integration tests only (requires Docker for Testcontainers)
mvn -pl scheduler-service -am test -Dgroups=integration
mvn -pl consumer-service -am test -Dgroups=integration

# Checkstyle
mvn validate
```

## GraphQL API

### Queries

```graphql
# Get all slots for a user
query slotsByUsername($username: String!) {
  slotsByUsername(username: $username) {
    id
    username
    start
    end
    status
    meetingId
  }
}

# Get all meetings for a user
query meetingsByUsername($username: String!) {
  meetingsByUsername(username: $username) {
    id
    title
    description
    participants
    start
    end
  }
}

# Get calendar view (slots + meetings) for a user within timeframe
query calendar($username: String!, $from: OffsetDateTime!, $to: OffsetDateTime!) {
  calendar(username: $username, from: $from, to: $to) {
    username
    slots {
      id
      username
      start
      end
      status
      meetingId
    }
    meetings {
      id
      title
      description
      participants
      start
      end
    }
  }
}
```

### Mutations

```graphql
# Create a free/busy slot
mutation createSlot($input: CreateSlotInput!) {
  createSlot(input: $input) {
    id
    username
    start
    end
    status
    meetingId
  }
}

# Update a slot (guarded: busy slots linked to meetings cannot be modified directly)
mutation updateSlot($id: ID!, $input: UpdateSlotInput!) {
  updateSlot(id: $id, input: $input) {
    id
    username
    start
    end
    status
    meetingId
  }
}

# Delete a slot (guarded: busy slots linked to meetings cannot be deleted directly)
mutation deleteSlot($id: ID!) {
  deleteSlot(id: $id)
}

# Create a meeting (async: publishes Kafka event, consumer splits participant slots)
mutation createMeeting($input: CreateMeetingInput!) {
  createMeeting(input: $input) {
    id
    title
    description
    participants
    start
    end
  }
}

# Update a meeting (cascades: reverts old slots, applies new)
mutation updateMeeting($id: ID!, $input: UpdateMeetingInput!) {
  updateMeeting(id: $id, input: $input) {
    id
    title
    description
    participants
    start
    end
  }
}

# Delete a meeting (cascades: reverts all slots to FREE)
mutation deleteMeeting($id: ID!) {
  deleteMeeting(id: $id)
}
```

### Example Variables

```json
# Create slot
{
  "input": {
    "username": "alice",
    "start": "2025-01-15T09:00:00Z",
    "end": "2025-01-15T17:00:00Z",
    "status": "FREE"
  }
}

# Create meeting
{
  "input": {
    "title": "Team Sync",
    "description": "Weekly team sync",
    "participants": "alice,bob,charlie",
    "start": "2025-01-15T10:00:00Z",
    "end": "2025-01-15T11:00:00Z"
  }
}
```

## Manual Testing (Browser)

1. Start stack: `docker compose up`
2. Open http://localhost:8080/ — minimal GraphiQL-like test page
3. Run queries/mutations against the live GraphQL endpoint

## Running Integration Tests

Integration tests use Testcontainers (Postgres + Kafka KRaft) and require Docker:

```bash
# Full integration test suite
mvn clean verify

# Or run only integration tests for a module
mvn -pl scheduler-service -am test -Dgroups=integration
mvn -pl consumer-service -am test -Dgroups=integration
```

Test coverage includes:
- `@DataJpaTest` for repositories
- `@SpringBootTest` + Testcontainers for full create-meeting → publish → consume → slot-update flow
- Meeting delete/update cascading back to FREE slots
- GraphQL e2e tests via `HttpGraphQlTester` with proper error type assertions

## Design Documentation

### Slot Splitting Algorithm

When a meeting is created within a participant's FREE slot, the `SlotSplitter` divides the slot:

```
Input: FREE slot [09:00–17:00), Meeting [10:00–11:00)

Output:
- FREE remainder [09:00–10:00)  (meetingId = null)
- BUSY slot     [10:00–11:00)  (meetingId = <meeting-id>)
- FREE remainder [11:00–17:00)  (meetingId = null)
```

**Rules:**
- Only FREE slots can be split
- BUSY slot receives exact meeting duration and is tagged with `meetingId`
- FREE remainder slots have `meetingId = null` (manually created)
- Adjacent FREE slots are merged on meeting delete/update

### MeetingId Linkage Design

- **Source of truth**: `meetingId` on `Slot` entity marks slots created by meetings
- **Manual slots**: `meetingId = null` → user can update/delete freely
- **Meeting-linked slots**: `meetingId != null` → guarded by `SlotLinkedToMeetingException`
- **Cascading**: Meeting delete/update reverts linked slots to FREE and clears `meetingId`

### Async Kafka Flow

```
createMeeting() (Scheduler)
    │
    ├─�� Validate participants have covering FREE slots
    ├─�� Persist Meeting + MeetingParticipants
    ├─�� Publish MeetingCreatedEvent to Kafka (topic: meeting-created)
    │
    �� (async)
Consumer Service (@KafkaListener)
    │
    ├─�� Deserialize event
    ├─�� Idempotency check: skip if slot with meetingId already exists for participant
    ├─�� Run SlotSplitter per participant
    └─�� Persist split slots (BUSY + FREE remainders)
```

**Idempotency**: Consumer dedupes by `(meetingId, participant)` — safe for redelivery.

**Error handling**: Consumer failures wrap in custom exception, routed to dead-letter topic / logged — never silently dropped.

### Known Limitations (Time Constraints)

1. **Slot splitting only on create**: The `SlotSplitter` is invoked in the consumer for `createMeeting` only. For `updateMeeting` and `deleteMeeting`, slot reversion happens synchronously in the scheduler-service (not via Kafka consumer). In a fully async design, all mutations would publish events and the consumer would handle splitting/reverting uniformly.

2. **Metrics not implemented**: Due to time constraints, no metrics were added. Recommended metrics:
   - **Latency**: Meeting creation end-to-end (scheduler → Kafka → consumer → DB)
   - **Throughput**: Meetings created/sec, slots split/sec
   - **Errors**: Kafka consumer failures, dead-letter queue size, validation rejections
   - **Business**: Slot conflict rate, meeting cancellation rate, calendar query latency
   - **Implementation**: Micrometer + Prometheus (Grafana dashboards), Spring Actuator endpoints
