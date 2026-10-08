# Pizza Sales API

A RESTful API for importing, storing, and serving sales data from a fictitious pizza place, built with Spring Boot.

## Tech stack

- **Java 25**, **Maven**
- **Spring Boot 4.1.1** (Spring Web, Spring Data JPA, Validation, Actuator)
- **PostgreSQL 18** (via Docker Compose)
- **Flyway** — versioned schema migrations
- **springdoc-openapi** — Swagger UI / OpenAPI docs
- **OpenCSV** — CSV import
- **MapStruct** — entity↔DTO mapping
- **JUnit 5, Mockito, Testcontainers** — testing

## Prerequisites

- JDK 25
- Docker + Docker Compose

No local Maven install needed — this repo includes the Maven Wrapper (`./mvnw`).

## Running locally

```bash
# 1. Start Postgres
docker compose up -d

# 2. Run the app
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`.

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health check**: http://localhost:8080/actuator/health

## Configuration

Database connection is driven by environment variables (see [`application.yml`](src/main/resources/application.yml) and [`docker-compose.yml`](docker-compose.yml) for defaults):

| Variable | Default |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `pizzasales` |
| `DB_USERNAME` | `pizzasalesuser` |
| `DB_PASSWORD` | `pizzasalespw` |

