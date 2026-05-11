# Idempotency Gateway — Pay-Once Payment Protocol

A Spring Boot REST API that guarantees every payment is processed **exactly once**, no matter how many times the request is retried. Built for FinSafe Transactions Ltd. to eliminate double-charging caused by network timeouts and client retries.

---

## Table of Contents

1. [Architecture Diagram](#architecture-diagram)
2. [How It Works](#how-it-works)
3. [Tech Stack](#tech-stack)
4. [Setup Instructions](#setup-instructions)
5. [API Documentation](#api-documentation)
6. [Design Decisions](#design-decisions)
7. [Developer's Choice — Key Expiry (TTL)](#developers-choice--key-expiry-ttl)

---

## Architecture Diagram

![Architecture Diagram](./docs/architecture-diagram.svg)

The diagram above shows the complete request flow from the moment a client sends a payment request to the moment a response is returned. Every incoming request passes through four stages: client request intake, key lookup, validation and processing, and finally the response.

---

## How It Works

When a client sends a payment request, they must include a unique `Idempotency-Key` header. The server uses this key to track whether the request has been seen before.

- **First request** — the key is new. The server processes the payment, stores the result against the key, and returns `201 Created`.
- **Duplicate request** — the key already exists and the payload matches. The server skips processing entirely and returns the original stored response with a `200 OK` and an `X-Cache-Hit: true` header.
- **Tampered request** — the key exists but the payload is different. The server rejects it with `422 Unprocessable Entity`.
- **Concurrent duplicate** — two identical requests arrive at the same time. The second request waits for the first to finish, then returns the same result without processing twice.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.0 |
| Database | SQLite |
| ORM | Spring Data JPA + Hibernate |
| Documentation | SpringDoc OpenAPI (Swagger UI) |
| Build Tool | Maven |

---

## Setup Instructions

### Prerequisites

Make sure you have the following installed:

- Java 21 or higher
- Maven 3.8 or higher

You can verify with:
```bash
java -version
mvn -version
```

### Steps

**1. Clone the repository**
```bash
git clone https://github.com/edenlisk/AmaliTech-idempotency-gateway-assessment.git
cd AmaliTech-Idempotency-gateway-assessment/Backend/idempotency-gateway
```

**2. Build the project**
```bash
mvn clean install
```

**3. Run the application**
```bash
mvn spring-boot:run
```

The server starts on `http://localhost:8080`.

The SQLite database file `idempotency.db` is created automatically in the project root on first startup. No database setup is required.

**4. Open Swagger UI**

Visit `http://localhost:8080/swagger-ui/index.html` to explore and test the API interactively.

---

## API Documentation

### Base URL
```
http://localhost:8080/api/v1
```

---

### Endpoints

#### `POST /process-payment`

Processes a payment request. Uses the `Idempotency-Key` header to guarantee the payment is only charged once.

**Headers**

| Header | Required | Description |
|---|---|---|
| `Content-Type` | Yes | `application/json` |
| `Idempotency-Key` | Yes | A unique string identifying this request (e.g. a UUID) |

**Request Body**

```json
{
  "amount": 100.0,
  "currency": "GHS"
}
```

| Field | Type | Required | Description                               |
|---|---|---|-------------------------------------------|
| `amount` | `Double` | Yes | Payment amount, must be greater than zero |
| `currency` | `String` | Yes | Currency code (e.g. RWF, GHS, USD, EUR)   |

---

### Response Scenarios

#### Scenario 1 — New Payment (First Request)

```bash
curl -X POST http://localhost:8080/api/v1/process-payment \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: e4d2af05-0b99-4440-9db3-42997392d80d" \
  -d '{"amount": 100.0, "currency": "GHS"}'
```

**Response — `201 Created`** *(after ~2 second processing delay)*
```json
{
  "status": "SUCCESS",
  "message": "Charged 100.0 GHS",
  "amount": 100.0,
  "currency": "GHS",
  "idempotencyKey": "e4d2af05-0b99-4440-9db3-42997392d80d"
}
```

---

#### Scenario 2 — Duplicate Request (Same Key, Same Payload)

```bash
curl -X POST http://localhost:8080/api/v1/process-payment \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: e4d2af05-0b99-4440-9db3-42997392d80d" \
  -d '{"amount": 100.0, "currency": "GHS"}'
```

**Response — `200 OK`** *(returned instantly, no processing delay)*

Response headers include:
```
X-Cache-Hit: true
```

```json
{
  "status": "SUCCESS",
  "message": "Charged 100.0 GHS",
  "amount": 100.0,
  "currency": "GHS",
  "idempotencyKey": "e4d2af05-0b99-4440-9db3-42997392d80d"
}
```

---

#### Scenario 3 — Key Reused With Different Payload

```bash
curl -X POST http://localhost:8080/api/v1/process-payment \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: e4d2af05-0b99-4440-9db3-42997392d80d" \
  -d '{"amount": 500.0, "currency": "GHS"}'
```

**Response — `422 Unprocessable Entity`**
```json
{
  "timestamp": "2026-05-11T10:30:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Idempotency key already used for a different request body."
}
```

---

#### Scenario 4 — Missing Idempotency-Key Header

```bash
curl -X POST http://localhost:8080/api/v1/process-payment \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.0, "currency": "GHS"}'
```

**Response — `400 Bad Request`**
```json
{
  "timestamp": "2026-05-11T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Missing required header: Idempotency-Key"
}
```

---

#### Scenario 5 — Invalid Request Body

```bash
curl -X POST http://localhost:8080/api/v1/process-payment \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: e4d2af05-0b99-4440-9db3-42997392d80d" \
  -d '{"amount": -50.0, "currency": ""}'
```

**Response — `400 Bad Request`**
```json
{
  "timestamp": "2026-05-11T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "amount: Amount must be greater than zero"
}
```

---

### Response Summary

| Scenario | Status Code | `X-Cache-Hit` Header |
|---|---|---|
| New payment processed | `201 Created` | Not present |
| Duplicate request (same payload) | `200 OK` | `true` |
| Same key, different payload | `422 Unprocessable Entity` | Not present |
| Missing `Idempotency-Key` header | `400 Bad Request` | Not present |
| Invalid request body | `400 Bad Request` | Not present |

---

## Design Decisions

### SQLite as the Database
SQLite was chosen to keep the setup as simple as possible — no database server installation, no configuration, no credentials. The database file is created automatically on startup. For a production deployment, this can be swapped for PostgreSQL or MySQL by updating the datasource configuration in `application.yml` and replacing the SQLite driver.

### SHA-256 for Payload Hashing
Incoming payloads are hashed using SHA-256 before being stored. This means the raw payment data is never stored twice and the hash comparison in fraud detection is fast and reliable. SHA-256 was chosen specifically because it is collision-resistant (no two different payloads can produce the same hash), one-way (the hash cannot be reversed to expose payment data), and is the industry standard used by payment processors like Stripe for exactly this purpose.

### Interface + Implementation Pattern
The service layer is split into a `PaymentService` interface and a `PaymentServiceImpl` class. This keeps the controller decoupled from the implementation details and makes the codebase easier to test and extend. The same pattern is applied to `KeyExpiryService`.

### Per-Key Locking for Race Conditions
A `ConcurrentHashMap<String, ReentrantLock>` is used to manage one lock per idempotency key. When two identical requests arrive simultaneously, the second request blocks on the lock until the first finishes processing, then returns the stored response without reprocessing. This prevents duplicate charges under high concurrency circumstances without rejecting correct retries.

### Utility Classes
Hashing (`HashUtil`) and serialization (`SerializationUtil`) logic are extracted into dedicated utility classes rather than living inside the service. This keeps the service focused purely on business logic and makes the utilities independently reusable and testable.

---

## Developer's Choice — Idempotency Key Expiry (TTL)

### What was added
A scheduled background job (cron job) that automatically deletes idempotency records older than **36 hours** from the database. It runs every hour and ensures that the `idempotency_records` table does not grow indefinitely with no longer entries.

### Why it was added
Without expiry, the `idempotency_records` table grows indefinitely. In a payment processor handling thousands of transactions per day this causes three real problems:

- **Performance degrades** — queries slow down as the table grows into millions of rows
- **Storage costs increase** — unnecessarily storing records that will never be needed again
- **Keys can never be reused** — a client who retries the same key weeks later for a new legitimate payment would incorrectly receive a cached response from months ago

A 36-hour TTL solves all three. The window is long enough to safely cover all legitimate retries within a business cycle.

The TTL value is also configured in `application.yml` under `app.idempotency.ttl-hours` so it can be adjusted per environment without changing the code.

---

*Built by NSANZIMFURA Enock NKUMBUYEDENI as part of the AmaliTech DEG Backend Engineering Assessment.*
