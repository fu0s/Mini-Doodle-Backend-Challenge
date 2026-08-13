# Mini-Doodle Backend Challenge — Task List

## Repository

> **GitHub Repo**: `<!-- ADD YOUR GITHUB REPO URL HERE -->`

## Architecture Decisions

- GraphQL via Spring for GraphQL; DataLoader/BatchLoader for every one-to-many/many-to-many resolution to avoid N+1
- Kafka tests via Testcontainers (KRaft mode), not embedded Kafka, for production-realistic integration tests
- One root `docker-compose.yml` orchestrating 4 services (db, kafka, scheduler-service, consumer-service), started with a single `docker-compose up`
- Calendar exists only as a domain concept (username + derived slots/meetings) — not its own DB table
- No magic strings/booleans: centralize topic names, event types, and slot status in enums/constants classes
- GraphQL resolvers/controllers call services only — never repositories directly
- Program to interfaces: every service exposes an interface (`SlotService`, `MeetingService`, `CalendarService`) with an `Impl` class injected via Spring; mappers get an interface too if there is more than one implementation strategy, otherwise a concrete `@Component` mapper with a stable, documented contract
- Validation layer: Jakarta Bean Validation (`@NotBlank`, `@NotNull`, custom `@ValidTimeRange`) on input DTOs, plus explicit business-rule validation in services
- Custom exceptions: one hierarchy under a base `SchedulingException` (unchecked), mapped to GraphQL errors via a `DataFetcherExceptionResolverAdapter`

## Maven Build

Multi-module Maven project: parent POM + `scheduler-service`, `consumer-service`, `shared` modules.

| Task | Command |
|------|---------|
| Full build | `mvn clean verify` |
| Test one module | `mvn -pl scheduler-service -am test` |
| Integration tests only | `mvn -pl scheduler-service -am test -Dgroups=integration` |
| Checkstyle | `mvn validate` (bound to checkstyle) |

## Domain Entities

- **Calendar**: username, `List<Slot>`, `List<Meeting>` (derived, not persisted as its own row)
- **Slot**: id, username, start, end, status (`FREE`/`BUSY`), meetingId (nullable — set when this slot's BUSY period was created by a meeting; null for manually-created busy/free slots)
- **Meeting**: id, title, description, participants (`List<username>`), start, end

---

## Phase 1 — Repo & Module Skeleton

**Goal:** Scaffold the Maven multi-module repo and one-command Docker orchestration.

- [ ] 1.1 `git init` + first commit containing `README.md` (goal description)
- [ ] 1.2 Create parent `pom.xml`: Java 21, Spring Boot 3.x parent, dependencyManagement, shared plugin versions (compiler, checkstyle, surefire/failsafe)
- [ ] 1.3 Create modules `scheduler-service`, `consumer-service`, `shared` (common entities + Kafka event DTOs) as child POMs
- [ ] 1.4 Draft root `docker-compose.yml` with 4 services: postgres, kafka (KRaft), scheduler-service, consumer-service, with `depends_on` + healthchecks
- [ ] 1.5 Validate: `mvn clean verify` passes; `docker-compose up` starts all 4 services healthy

**Validation:** clean build + healthy compose stack. **Commit after phase.**

---

## Phase 2 — Domain Model, Interfaces & Exceptions

**Goal:** Define domain classes, service contracts, and the exception hierarchy before any persistence.

- [ ] 2.1 `SlotStatus` enum (`FREE`, `BUSY`) in a shared constants package
- [ ] 2.2 `Slot` domain class: id, username, start, end, status, nullable meetingId (no boolean flags)
- [ ] 2.3 `Meeting` domain class: id, title, description, participants (`List<String>`; API layer parses comma-separated input), start, end
- [ ] 2.4 `Calendar` aggregate/view object (username + slots + meetings) — not an entity
- [ ] 2.5 Constants package: Kafka topic name, event type, slot status, error codes — no inline literals
- [ ] 2.6 Service interfaces (signatures only): `SlotService`, `MeetingService`, `CalendarService`, plus `SlotSplitter` (swappable/mockable)
- [ ] 2.7 Exception hierarchy in `exceptions` package under abstract unchecked `SchedulingException` (carries error-code enum):
  - [ ] `SlotNotFoundException`, `MeetingNotFoundException`
  - [ ] `SlotConflictException` (participant not free)
  - [ ] `SlotLinkedToMeetingException` (busy slot owned by an active meeting)
  - [ ] `InvalidTimeRangeException` (end before/equal start)
  - [ ] `InvalidParticipantsException` (empty/malformed participant list)
- [ ] 2.8 Error-code enum shared by all exceptions

**Validation:** compiles; interfaces + exceptions documented. **Commit after phase.**

---

## Phase 3 — Persistence Layer

**Goal:** JPA entities, indexed repositories, and entity↔domain mappers.

- [ ] 3.1 `persistence/entities`: `SlotEntity` (meetingId column nullable + indexed), `MeetingEntity`, `MeetingParticipantEntity`
- [ ] 3.2 `persistence/repositories`: `SlotRepository`, `MeetingRepository`, `MeetingParticipantRepository` (Spring Data JPA) — indexed queries by username, start, end, meetingId
- [ ] 3.3 DB indexes on `(username, start, end)` and on `meetingId` for scale (hundreds of users, thousands of slots)
- [ ] 3.4 Entity↔domain mapper interface(s)/components so GraphQL/service layers never see entities directly

**Validation:** schema auto-generation matches indexes; `@DataJpaTest` compiles. **Commit after phase.**

---

## Phase 4 — Service Layer & Business Rules

**Goal:** Implement business rules with the meetingId linkage as the source of truth for busy slots.

- [ ] 4.1 `SlotServiceImpl.createSlot(username, start, end, status)` — validate time range (`InvalidTimeRangeException`), no meetingId on manual creation
- [ ] 4.2 `SlotServiceImpl.updateSlot` / `deleteSlot` guard check:
  - a. Load slot; if not found → `SlotNotFoundException`
  - b. If `status == BUSY` and `meetingId != null` → `SlotLinkedToMeetingException` (caller must go through the meeting flow, which cascades)
  - c. If `status == BUSY` and `meetingId == null` (manually busy) → allow update/delete
- [ ] 4.3 `SlotServiceImpl.findSlotsByUsername(username)`
- [ ] 4.4 `SlotSplitter` (pure function): given a FREE slot and meeting `[start, end)` inside it → one BUSY slot (exact duration, tagged meetingId) + zero/one/two FREE remainder slots (meetingId null)
- [ ] 4.5 `MeetingServiceImpl.createMeeting`:
  - Validate title/description/time range/participants → `InvalidParticipantsException` / `InvalidTimeRangeException`
  - Verify every participant has a FREE slot fully covering `[start, end)`; if any fails → `SlotConflictException` naming the failing participant(s); reject whole creation, no partial creation
  - On success: persist Meeting + publish `MeetingCreatedEvent` to Kafka
- [ ] 4.6 `MeetingServiceImpl.updateMeeting` / `deleteMeeting`:
  - Load meeting; if missing → `MeetingNotFoundException`
  - Revert all slots where `meetingId == meeting.id` to FREE (merge adjacent FREE slots if applicable)
  - Re-apply new time/participants re-running availability check (update), or remove meeting entirely (delete)
- [ ] 4.7 `CalendarServiceImpl.getCalendar(username)` — aggregate slots + meetings (optionally within a time frame)

**Validation:** unit tests for 4.2 guard and 4.4 splitter pass. **Commit after phase.**

---

## Phase 5 — Kafka Integration

**Goal:** Async create-meeting → slot-update flow via one topic, idempotent + dead-lettered.

- [ ] 5.1 Define one topic (`meeting-created`) and `MeetingCreatedEvent` (meetingId, participants, start, end) in `shared` module
- [ ] 5.2 scheduler-service: publish event via `KafkaTemplate` (explicit JSON serializer) after validation + persistence
- [ ] 5.3 consumer-service: `@KafkaListener` on the topic; explicit consumer group id + JSON deserializer (no implicit defaults)
- [ ] 5.4 Consumer handler: asynchronously run `SlotSplitter` per participant, tag BUSY slot with event's meetingId
- [ ] 5.5 Idempotency: dedupe by meetingId (skip if slot already exists with that meetingId for a participant) for redelivery safety
- [ ] 5.6 Consumer-side failures wrapped in a custom exception, routed to a dead-letter topic or logged clearly — never silently dropped

**Validation:** Testcontainers integration test of publish→consume→slot-update passes. **Commit after phase.**

---

## Phase 6 — GraphQL API

**Goal:** Expose queries/mutations with DataLoaders, validation, and typed error mapping.

- [ ] 6.1 `.graphqls` schema:
  - Queries: `slotsByUsername(username)`, `meetingsByUsername(username)`, `calendar(username, from, to)`
  - Mutations: `createSlot`, `updateSlot`, `deleteSlot`, `createMeeting`, `updateMeeting`, `deleteMeeting`
- [ ] 6.2 DataLoaders/BatchLoaders for nested/batched resolution (e.g. participant status) to prevent N+1
- [ ] 6.3 Resolvers/controllers call services via interfaces only
- [ ] 6.4 ArchUnit test asserting no GraphQL class imports a repository class directly
- [ ] 6.5 Input DTO validation annotations (`@NotBlank`, `@NotNull`, custom `@ValidTimeRange`) on all mutation inputs, validated before invoking services
- [ ] 6.6 `DataFetcherExceptionResolverAdapter` (e.g. `GraphQlExceptionResolver`) mapping each custom exception to a typed `GraphQLError` (`BAD_REQUEST`, `NOT_FOUND`, `FORBIDDEN`) — never leak stack traces

**Validation:** GraphQL integration tests (HttpGraphQlTester) pass for queries/mutations + error types. **Commit after phase.**

---

## Phase 7 — Configuration

**Goal:** Single-profile, property-driven, no scattered literals.

- [ ] 7.1 One `application.properties` per module (datasource, Kafka bootstrap servers, GraphQL path, server port)
- [ ] 7.2 `@ConfigurationProperties` config class (e.g. `SchedulerProperties`) for topic name, consumer group, tunables — injected instead of scattered `@Value`

**Validation:** both services start against compose stack using config only. **Commit after phase.**

---

## Phase 8 — Test Web Page

**Goal:** Manual test surface for the running container.

- [ ] 8.1 Minimal static page under scheduler-service (GraphiQL/Playground or custom HTML+fetch) to run queries/mutations manually

**Validation:** page loads in browser and executes a basic query against the stack. **Commit after phase.**

---

## Phase 9 — Testing

**Goal:** Comprehensive unit + integration coverage per the validation gate.

- [ ] 9.1 Unit: `SlotSplitter` (incl. meetingId tagging), availability-check logic, mappers, event serialization
- [ ] 9.2 Unit: each custom exception's trigger condition; validation annotations (e.g. reject end <= start)
- [ ] 9.3 Unit: update/delete-slot guard (busy+linked → `SlotLinkedToMeetingException`; busy+no meetingId → allowed; not found → `SlotNotFoundException`)
- [ ] 9.4 Integration: `@DataJpaTest` for repositories
- [ ] 9.5 Integration: `@SpringBootTest` + Testcontainers (Postgres + Kafka, KRaft) for full create-meeting → publish → consume → slot-update (meetingId set) flow, using Awaitility for async assertions
- [ ] 9.6 Integration: meeting delete/update cascading back to FREE slots (meetingId cleared)
- [ ] 9.7 GraphQL integration tests via `HttpGraphQlTester` end-to-end, asserting proper GraphQL error types/messages for each custom exception

**Validation:** `mvn clean verify` green (unit + integration). **Commit after phase.**

---

## Phase 10 — Docs & Wrap-up

**Goal:** Finalize documentation; deliver merge-ready history.

- [ ] 10.1 Finalize `README.md`: architecture diagram, `docker-compose up` instructions, example GraphQL queries/mutations
- [ ] 10.2 Document slot-splitting algorithm, meetingId linkage design, async Kafka flow
- [ ] 10.3 Ensure commit history reflects incremental, meaningful progress across all phases
- [ ] 10.4 Final review per AGENTS.md DoD: clean build, tests green, docs factual, independent review done
