# Mini-Doodle Backend Challenge

A backend scheduling service: users create free/busy slots and meetings, and a Kafka-backed
async flow automatically marks participant availability.

## Modules

- `scheduler-service` — GraphQL API for slots, meetings, and calendar view
- `consumer-service` — Kafka consumer that applies meeting events to participant slots
- `shared` — common entities and Kafka event DTOs

## Run

```bash
docker compose up
```

Starts Postgres, Kafka (KRaft), `scheduler-service`, and `consumer-service`.

- Scheduler GraphQL: http://localhost:8080/graphiql
- Postgres: `localhost:5432` (`doodle`/`doodle`)
- Kafka (host): `localhost:9092`

## Build

```bash
mvn clean verify          # full build (unit + integration)
mvn -pl <module> -am test # test one module
```
