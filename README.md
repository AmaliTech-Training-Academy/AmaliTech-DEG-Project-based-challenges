# AmaliTech DEG Backend Challenges

This repository contains my backend solutions for the AmaliTech DEG project-based challenges.

## Projects

### 1. Idempotency Gateway
Ensures that payment requests are processed only once, even if retried multiple times.

Location: backend/idempotency-gateway

---

### 2. Pulse-Check API
Monitors remote devices using heartbeat signals and triggers alerts when devices go offline.

Location: backend/pulse-check-api

---

## Tech Stack
- Java
- Spring Boot
- REST APIs

---

## How to Run

Each project is independent:

```bash
cd backend/idempotency-gateway
mvn spring-boot:run