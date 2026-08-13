# Mini-Doodle Backend Challenge — Task List

## Repository

> **GitHub Repo**: `https://github.com/fu0s/Mini-Doodle-Backend-Challenge`

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

- [x] 1.1 `git init` + first commit containing `README.md` (goal description)
- [x] 1.2 Create parent `pom.xml`: Java 21, Spring Boot 3.x parent, dependencyManagement, shared plugin versions (compiler, checkstyle, surefire/failsafe)
- [x] 1.3 Create modules `scheduler-service`, `consumer-service`, `shared` (common entities + Kafka event DTOs) as child POMs
- [x] 1.4 Draft root `docker-compose.yml` with 4 services: postgres, kafka (KRaft), scheduler-service, consumer-service, with `depends_on` + healthchecks
- [x] 1.5 Validate: `mvn clean verify` passes; `docker-compose up` starts all 4 services healthy

**Validation:** clean build + healthy compose stack. **Commit after phase.**
**Status:** ✅ COMPLETED (Commit: 58a4c28)

---

## Phase 2 — Domain Model, Interfaces & Exceptions

**Goal:** Define domain classes, service contracts, and the exception hierarchy before any persistence.

- [x] 2.1 `SlotStatus` enum (`FREE`, `BUSY`) in a shared constants package
- [x] 2.2 `Slot` domain class: id, username, start, end, status, nullable meetingId (no boolean flags)
- [x] 2.3 `Meeting` domain class: id, title, description, participants (`List<String>`; API layer parses comma-separated input), start, end
- [x] 2.4 `Calendar` aggregate/view object (username + slots + meetings) — not an entity
- [x] 2.5 Constants package: Kafka topic name, event type, slot status, error codes — no inline literals
- [x] 2.6 Service interfaces (signatures only): `SlotService`, `MeetingService`, `CalendarService`, plus `SlotSplitter` (swappable/mockable)
- [x] 2.7 Exception hierarchy in `exceptions` package under abstract unchecked `SchedulingException` (carries error-code enum):
  - [x] `SlotNotFoundException`, `MeetingNotFoundException`
  - [x] `SlotConflictException` (participant not free)
  - [x] `SlotLinkedToMeetingException` (busy slot owned by an active meeting)
  - [x] `InvalidTimeRangeException` (end before/equal start)
  - [x] `InvalidParticipantsException` (empty/malformed participant list)
- [x] 2.8 Error-code enum shared by all exceptions

**Validation:** compiles; interfaces + exceptions documented. **Commit after phase.**
**Status:** ✅ COMPLETED

---

## Phase 3 — Persistence Layer (in `shared` module)

**Goal:** JPA entities, indexed repositories, and entity↔domain mappers — placed in `shared` so both scheduler-service and consumer-service can use them.

- [x] 3.1 `persistence/entities`: `SlotEntity` (meetingId column nullable + indexed), `MeetingEntity`, `MeetingParticipantEntity`, `MeetingParticipantId` (composite key) — in `shared/src/main/java/.../persistence/entity/`
- [x] 3.2 `persistence/repositories`: `SlotRepository`, `MeetingRepository`, `MeetingParticipantRepository` (Spring Data JPA) — indexed queries by username, start, end, meetingId — in `shared/src/main/java/.../persistence/repository/`
- [x] 3.3 DB indexes on `(username, start, end)` and on `meetingId` for scale (hundreds of users, thousands of slots) — defined via `@Index` annotations on `@Table`
- [x] 3.4 Entity↔domain mapper components (`SlotMapper`, `MeetingMapper`) so GraphQL/service layers never see entities directly — in `shared/src/main/java/.../mapper/`

**Validation:** `mvn clean verify` passes; schema auto-generation matches indexes. **Commit after phase.**
**Status:** ✅ COMPLETED

---

## Phase 4 — Service Layer & Business Rules

**Goal:** Implement business rules with the meetingId linkage as the source of truth for busy slots.

- [x] 4.1 `SlotServiceImpl.createSlot(username, start, end, status)` — validate time range (`InvalidTimeRangeException`), no meetingId on manual creation
- [x] 4.2 `SlotServiceImpl.updateSlot` / `deleteSlot` guard check:
  - a. Load slot; if not found → `SlotNotFoundException`
  - b. If `status == BUSY` and `meetingId != null` → `SlotLinkedToMeetingException` (caller must go through the meeting flow, which cascades)
  - c. If `status == BUSY` and `meetingId == null` (manually busy) → allow update/delete
- [x] 4.3 `SlotServiceImpl.findSlotsByUsername(username)` (implemented as `getSlotsByUsername`, matching Phase 2 contract)
- [x] 4.4 `SlotSplitter`: given a FREE slot and meeting `[start, end)` inside it → one BUSY slot (exact duration, tagged meetingId) + zero/one/two FREE remainder slots (meetingId null)
- [x] 4.5 `MeetingServiceImpl.createMeeting`:
  - Validate title/description/time range/participants → `InvalidParticipantsException` / `InvalidTimeRangeException`
  - Verify every participant has a FREE slot fully covering `[start, end)`; if any fails → `SlotConflictException` naming the failing participant(s); reject whole creation, no partial creation
  - On success: persist Meeting + publish `MeetingCreatedEvent` to Kafka *(Kafka publish deferred to Phase 5 — event & topic defined in 5.1; splitting currently runs synchronously via `SlotSplitter`)*
- [x] 4.6 `MeetingServiceImpl.updateMeeting` / `deleteMeeting`:
  - Load meeting; if missing → `MeetingNotFoundException`
  - Revert all slots where `meetingId == meeting.id` to FREE (merge adjacent FREE slots if applicable)
  - Re-apply new time/participants re-running availability check (update), or remove meeting entirely (delete)
- [x] 4.7 `CalendarServiceImpl.getCalendar(username)` — aggregate slots + meetings (optionally within a time frame)

**Validation:** unit tests for 4.2 guard (3) and 4.4 splitter (6) pass; 19 tests total green — `mvn clean verify` ✅. **Commit after phase.**

**Status:** ✅ COMPLETED (except 4.5 Kafka publish, deferred to Phase 5)

---

## Phase 5 — Kafka Integration

**Goal:** Async create-meeting → slot-update flow via one topic, idempotent + dead-lettered.

- [x] 5.1 Define one topic (`meeting-created`) and `MeetingCreatedEvent` (meetingId, participants, start, end) in `shared` module
- [x] 5.2 scheduler-service: publish event via `KafkaTemplate` (explicit JSON serializer) after validation + persistence
- [x] 5.3 consumer-service: `@KafkaListener` on the topic; explicit consumer group id + JSON deserializer (no implicit defaults)
- [x] 5.4 Consumer handler: asynchronously run `SlotSplitter` per participant, tag BUSY slot with event's meetingId
- [x] 5.5 Idempotency: dedupe by meetingId (skip if slot already exists with that meetingId for a participant) for redelivery safety
- [x] 5.6 Consumer-side failures wrapped in a custom exception, routed to a dead-letter topic or logged clearly — never silently dropped

**Validation:** Testcontainers integration test of publish→consume→slot-update passes. **Commit after phase.**

**Status:** ✅ COMPLETED (unit tests green — Testcontainers integration deferred to Phase 9.5)

---

## Phase 6 — GraphQL API

**Goal:** Expose queries/mutations with DataLoaders, validation, and typed error mapping.

- [x] 6.1 `.graphqls` schema:
  - Queries: `slotsByUsername(username)`, `meetingsByUsername(username)`, `calendar(username, from, to)`
  - Mutations: `createSlot`, `updateSlot`, `deleteSlot`, `createMeeting`, `updateMeeting`, `deleteMeeting`
- [x] 6.2 DataLoaders/BatchLoaders for nested/batched resolution (e.g. participant status) to prevent N+1
- [x] 6.3 Resolvers/controllers call services via interfaces only
- [x] 6.4 ArchUnit test asserting no GraphQL class imports a repository class directly
- [x] 6.5 Input DTO validation annotations (`@NotBlank`, `@NotNull`, custom `@ValidTimeRange`) on all mutation inputs, validated before invoking services
- [x] 6.6 `DataFetcherExceptionResolverAdapter` (e.g. `GraphQlExceptionResolver`) mapping each custom exception to a typed `GraphQLError` (`BAD_REQUEST`, `NOT_FOUND`, `FORBIDDEN`) — never leak stack traces

**Validation:** GraphQL integration tests (HttpGraphQlTester) pass for queries/mutations + error types. **Commit after phase.**

---

## Phase 7 — Configuration

**Goal:** Goal: Single-profile, property-driven, no scattered literals, config ownership split cleanly between shared and per-service.

7.1 One application.properties per service:

scheduler-service/src/main/resources/application.properties — datasource (URL, username, password, driver), GraphQL path, server port, Kafka bootstrap servers + producer settings.

consumer-service/src/main/resources/application.properties — datasource (if the consumer reads/writes the same DB), server port, Kafka bootstrap servers + consumer group + consumer settings.

Both files stay on a single profile (no application-dev.properties / application-prod.properties split) — one profile per module, as required.

7.2 One @ConfigurationProperties class per service, owned by that service and reflecting only what that service needs:

scheduler-service: SchedulerProperties — GraphQL path, meeting-creation topic name (producer side), any scheduler-specific tunables.

consumer-service: ConsumerProperties — consumer group id, topic name (consumer side), retry/backoff tunables.

7.3 If a config value is genuinely shared by both services (e.g. the Kafka topic name and event type, which both producer and consumer must agree on), do not duplicate it in two classes — define one shared config class in the shared module (e.g. SharedKafkaProperties with topicName, eventType), bind it via @ConfigurationProperties(prefix = "scheduling.kafka"), and have both scheduler-service and consumer-service import/inject that same shared class instead of redeclaring the properties locally.

7.4 Both services' local @ConfigurationProperties classes (SchedulerProperties, ConsumerProperties) hold only what is genuinely service-specific; anything cross-cutting lives exclusively in the shared config class to avoid drift between producer and consumer configuration.

7.5 No @Value-scattered literals anywhere — every externalized value flows through one of the three config classes (SchedulerProperties, ConsumerProperties, or the shared SharedKafkaProperties).

Validation: both services start against the compose stack using config only (no hardcoded fallbacks in code); the topic name/event type used by the producer and the consumer resolve from the same shared config class, so a single property change propagates to both. Commit after phase.

**Status:** ✅ COMPLETED — `mvn clean verify` green; zero `@Value` in production code; `spring.kafka.*` removed from both application.properties; topic/event-type resolve from shared `SharedKafkaProperties` (`scheduling.kafka.*`) in both services; docker-compose env updated (`SPRING_KAFKA_BOOTSTRAP_SERVERS` → `KAFKA_BOOTSTRAP_SERVERS`). Commit pending.

---

## Phase 8 — Test Web Page

**Goal:** Manual test surface for the running container.

- [x] 8.1 Minimal static page under scheduler-service (GraphiQL/Playground or custom HTML+fetch) to run queries/mutations manually

**Validation:** page loads in browser and executes a basic query against the stack. **Commit after phase.**
**Status:** ✅ COMPLETED (Commit: 94c010a)

---

## Phase 9 — Testing

**Goal:** Comprehensive unit + integration coverage per the validation gate.

- [x] 9.1 Unit: `SlotSplitter` (incl. meetingId tagging), availability-check logic, mappers, event serialization
- [x] 9.2 Unit: each custom exception's trigger condition; validation annotations (e.g. reject end <= start)
- [x] 9.3 Unit: update/delete-slot guard (busy+linked → `SlotLinkedToMeetingException`; busy+no meetingId → allowed; not found → `SlotNotFoundException`)
- [x] 9.4 Integration: `@DataJpaTest` for repositories
- [x] 9.5 Integration: `@SpringBootTest` + Testcontainers (Postgres + Kafka, KRaft) for full create-meeting → publish → consume → slot-update (meetingId set) flow, using Awaitility for async assertions
- [x] 9.6 Integration: meeting delete/update cascading back to FREE slots (meetingId cleared)
- [x] 9.7 GraphQL integration tests via `HttpGraphQlTester` end-to-end, asserting proper GraphQL error types/messages for each custom exception

**Validation:** `mvn clean verify` green (unit + integration). **Commit after phase.**

---

## Phase 10 — Docs & Wrap-up

**Goal:** Finalize documentation; deliver merge-ready history.

- [ ] 10.1 Finalize `README.md`: architecture diagram, `docker-compose up` instructions, example GraphQL queries/mutations example of testing through UI (I will add a screenshot) mention also how to run integration tests
- [ ] 10.2 Document slot-splitting algorithm, meetingId linkage design, async Kafka flow , mention that the slot splitting is supposed to be called only in the consuler-service but since there is a lack of time it was implmeneted just for create meeting and not update and delete, also mention that due to lack of times metrics weren't implmented but mention what metrics could have been added and how 
- [ ] 10.3 Ensure commit history reflects incremental, meaningful progress across all phases
- [ ] 10.4 Final review per AGENTS.md DoD: clean build, tests green, docs factual, independent review done
