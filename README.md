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

## Importing data

Sales data is loaded from CSV files by two one-off import jobs. Each job is the same application started with a
Spring profile: it runs the import, then exits (no web server is started). The exit code is `0` on success and `1`
on failure, so the jobs can be scripted or run in a pipeline.

| Job | Profile | Imports (in this order) |
|---|---|---|
| Pizza import | `import-pizzas` | pizza types (with categories and ingredients), then pizzas |
| Order import | `import-orders` | orders, then order details |

**Run the pizza import first.** Order details reference pizzas, so the order import fails with
`references unknown pizza` if the pizzas are not in the database yet.

Both jobs need Postgres running with migrations applied (steps 1 and 2 of [Running locally](#running-locally)).
Run them as two separate commands. Don't activate both profiles in one run: each job exits the application as
soon as it finishes, so the second job would never start.

### 1. Import pizzas

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=import-pizzas
```

| File | Property | Environment variable | Default | Columns |
|---|---|---|---|---|
| Pizza types | `app.import.pizzas.types-file` | `APP_IMPORT_PIZZAS_TYPESFILE` | `classpath:data/pizza_types.csv` | `pizza_type_id,name,category,ingredients` |
| Pizzas | `app.import.pizzas.file` | `APP_IMPORT_PIZZAS_FILE` | `classpath:data/pizzas.csv` | `pizza_id,pizza_type_id,size,price` |

- `ingredients` is a quoted, comma-separated list, e.g. `"Chicken, Tomatoes, Garlic"`.
- Categories and ingredients are created on first use and matched case-insensitively, so `Chicken` and `chicken` are
  the same category.
- `size` must be one of `S`, `M`, `L`, `XL`, `XXL`.
- Every `pizza_type_id` in the pizzas file must exist in the pizza types file (or already be in the database).

### 2. Import orders

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=import-orders
```

| File | Property | Environment variable | Default | Columns |
|---|---|---|---|---|
| Orders | `app.import.orders.file` | `APP_IMPORT_ORDERS_FILE` | `classpath:data/orders.csv` | `order_id,date,time` |
| Order details | `app.import.orders.details-file` | `APP_IMPORT_ORDERS_DETAILSFILE` | `classpath:data/order_details.csv` | `order_details_id,order_id,pizza_id,quantity` |

- `date` is `yyyy-MM-dd` and `time` is `HH:mm:ss`, e.g. `2015-01-01,11:38:36`.
- Every `order_id` in the order details file must exist in the orders file (or already be in the database), and every
  `pizza_id` must already have been imported by the pizza import.
- These files are large (about 21k orders and 49k order details), so rows are saved in chunks, one transaction per
  chunk. See [Chunk size](#chunk-size).

### Providing your own files

Each file defaults to the copy bundled in [`src/main/resources/data`](src/main/resources/data), configured in
[`application.yml`](src/main/resources/application.yml):

```yaml
app:
  import:
    chunk-size: 1000
    pizzas:
      types-file: classpath:data/pizza_types.csv
      file: classpath:data/pizzas.csv
    orders:
      file: classpath:data/orders.csv
      details-file: classpath:data/order_details.csv
```

To import a different file, set its property (see the tables above) to a Spring resource location:

- `file:/absolute/path/orders.csv`: a file on disk (`file:relative/path.csv` resolves against the working directory)
- `classpath:data/orders.csv`: a file on the classpath

Only the files you override change; the rest keep their defaults. Files must be UTF-8 with a header row matching the
columns above. Pass the override in whichever way suits you:

**Command-line arguments with the Maven Wrapper:**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=import-pizzas \
  -Dspring-boot.run.arguments="--app.import.pizzas.types-file=file:/data/pizza_types.csv --app.import.pizzas.file=file:/data/pizzas.csv"
```

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=import-orders \
  -Dspring-boot.run.arguments="--app.import.orders.file=file:/data/orders.csv --app.import.orders.details-file=file:/data/order_details.csv"
```

**Environment variables** (the property name in upper case, with dots as underscores and dashes removed):

```bash
APP_IMPORT_ORDERS_FILE=file:/data/orders.csv \
APP_IMPORT_ORDERS_DETAILSFILE=file:/data/order_details.csv \
./mvnw spring-boot:run -Dspring-boot.run.profiles=import-orders
```

**The packaged jar** (build it once with `./mvnw package`):

```bash
java -jar target/pizzasalesapi-0.0.1-SNAPSHOT.jar --spring.profiles.active=import-pizzas \
  --app.import.pizzas.types-file=file:/data/pizza_types.csv \
  --app.import.pizzas.file=file:/data/pizzas.csv
```

```bash
java -jar target/pizzasalesapi-0.0.1-SNAPSHOT.jar --spring.profiles.active=import-orders \
  --app.import.orders.file=file:/data/orders.csv \
  --app.import.orders.details-file=file:/data/order_details.csv
```

If a file can't be found, the job logs `Failed to read CSV file at <location>` and exits with code `1`.

### Chunk size

`app.import.chunk-size` (environment variable `APP_IMPORT_CHUNKSIZE`, default `1000`) sets how many orders or order
details are saved per transaction. It must be a positive number. Lower it to use less memory per transaction, or raise
it for fewer round-trips to the database.

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=import-orders -Dspring-boot.run.arguments="--app.import.chunk-size=500"
```

### Re-running an import

Imports are safe to re-run. Rows whose source id (`pizza_type_id`, `pizza_id`, `order_id` or `order_details_id`)
is already in the database are skipped and left unchanged, and each job logs how many rows it imported and skipped:

```
Imported 21350 new orders (0 already existed)
Imported 0 new orders (21350 already existed)
```

Because orders and order details are saved one chunk at a time, a failure partway through keeps the chunks saved
before it. Fix the cause, then run the same job again; it skips what was already saved and carries on from there.

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

