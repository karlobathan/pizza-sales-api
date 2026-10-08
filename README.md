# Pizza Sales API

A RESTful API for importing, storing, and serving sales data from a fictitious pizza place, built with Spring Boot.

## Tech stack

- **Java 25**, **Maven**
- **Spring Boot 4.1.1** (Spring Web, Spring Data JPA, Validation, Actuator)
- **PostgreSQL 18** (via Docker Compose)
- **Flyway**: versioned schema migrations
- **springdoc-openapi**: Swagger UI / OpenAPI docs
- **OpenCSV**: CSV import
- **MapStruct**: entity to DTO mapping
- **JUnit 5, Mockito, Testcontainers**: testing

## Prerequisites

- JDK 25
- Docker + Docker Compose

No local Maven install needed. This repo includes the Maven Wrapper (`./mvnw`).

## Running locally

### 1. Start Postgres

```bash
docker compose up -d
```

### 2. Apply database migrations

```bash
export FLYWAY_URL="jdbc:postgresql://${DB_HOST:-localhost}:${DB_PORT:-5432}/${DB_NAME:-pizzasales}"
export FLYWAY_USER="${DB_USERNAME:-pizzasalesuser}"
export FLYWAY_PASSWORD="${DB_PASSWORD:-pizzasalespw}"
export FLYWAY_LOCATIONS="filesystem:src/main/resources/db/migration"
./mvnw flyway:migrate
```

Migrations are **not** run automatically on app startup (`spring.flyway.enabled: false`). 
See [Database migrations](#database-migrations) for why.

### 3. Run the app

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`.

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Health check**: http://localhost:8080/actuator/health

## Configuration

Database connection is driven by environment variables (see [`application.yml`](src/main/resources/application.yml) 
and [`docker-compose.yml`](docker-compose.yml) for defaults):

| Variable | Default |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `5432` |
| `DB_NAME` | `pizzasales` |
| `DB_USERNAME` | `pizzasalesuser` |
| `DB_PASSWORD` | `pizzasalespw` |

## Database migrations

Flyway migrations live in [`src/main/resources/db/migration`](src/main/resources/db/migration). 
`spring.flyway.enabled` is explicitly set to `false`, so the application never runs migrations itself on startup.

This is deliberate. In a real deployment, schema migrations should run as their own step in the CI/CD pipeline,
before the new app version is rolled out, rather than racing multiple app instances against each other to apply 
DDL on boot. 

