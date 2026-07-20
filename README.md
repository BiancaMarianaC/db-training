# TradeFlow — Trade Reconciliation Dashboard (Student Starter)

> Deutsche Bank — TDI 2026 Graduate Technical Training Programme
> **Intermediate Track** | 10-Day Case Study | Version 2.0

This repository is the **starter scaffold** for the TradeFlow case study. Each
day of the programme adds another layer to the system. By Day 10 you and your
team will have built, dockerised, tested, and monitored a full-stack trade
reconciliation platform with Kafka event streaming and a CI/CD pipeline.

---

## What you will build

A mid-complexity trade reconciliation system used (in concept) by an Operations
team to detect and resolve mismatches between internal trade records and
external counterparty/custodian feeds.

```
   ┌──────────┐        ┌────────────────────┐        ┌────────────┐
   │  React   │  HTTP  │  Spring Boot REST  │  JDBC  │ PostgreSQL │
   │ Frontend │ ─────▶ │   recon-service    │ ─────▶ │            │
   └──────────┘        │   + Spring Security│        └────────────┘
                       └──────────┬─────────┘
                                  │  KafkaTemplate / @KafkaListener
                                  ▼
                          ┌───────────────┐
                          │  Apache Kafka │
                          │ trade-events  │
                          └───────┬───────┘
                                  │
                                  ▼
                       ┌────────────────────┐
                       │   Recon Consumer   │ ─▶ writes recon_results
                       │  Audit Consumer    │ ─▶ writes audit_log
                       └────────────────────┘

           /actuator/prometheus  ─▶  Prometheus  ─▶  Grafana dashboards
```

---

## Repository layout

```
tradeflow-studentscopy/
├── db/                       ← Day 1: SQL assets (seed + analytical queries)
│   ├── seed_data.sql         ← Counterparties, instruments, sample trades
│   ├── queries.sql           ← Analytical queries (Window fns, CTEs)
│   └── erd.md                ← ER diagram
│
│   NOTE: Liquibase changelogs live on the JVM classpath at
│         backend/src/main/resources/db/changelog/ — not here.
│
├── backend/                  ← Days 2-6, 9: Java + Spring Boot + Kafka
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/com/dbtraining/tradeflow/
│       ├── TradeflowApplication.java
│       ├── model/            ← Day 2-3: domain POJOs + JPA entities
│       ├── repository/       ← Day 4-5: JDBC DAOs and Spring Data repos
│       ├── service/          ← Day 3-4: business logic + reconciliation
│       ├── controller/       ← Day 6: REST API endpoints
│       ├── dto/              ← request/response objects, TradeEvent
│       ├── exception/        ← Day 3-6: custom exceptions, @RestControllerAdvice
│       ├── config/           ← Day 5-6: Swagger, Security, Kafka config
│       └── kafka/            ← Day 9: producers and consumers
│
├── static-dashboard/         ← Day 7: HTML + CSS markup & styling
│   ├── dashboard.html        ←   Day 8 AM wires these pages with vanilla JS
│   ├── trades.html
│   ├── recon.html
│   ├── add-trade.html
│   ├── css/style.css
│   └── js/*.js               ←   created on Day 8 AM (I093–I098)
│
├── frontend/                 ← Day 8 PM + Day 9: React + Vite recon-ui
│   ├── package.json
│   ├── vite.config.js
│   ├── Dockerfile
│   └── src/
│       ├── App.jsx
│       ├── components/       ← TradeTable, BreakBadge, StatCard, ...
│       ├── hooks/            ← useTradeData, useReconResults
│       ├── services/         ← apiService.js
│       ├── pages/            ← Dashboard, Trades, Recon, AddTrade
│       └── styles/
│
├── monitoring/               ← Day 6 + 10: Prometheus / Grafana
│   ├── prometheus/prometheus.yml
│   └── grafana/provisioning/
│
├── .github/workflows/ci.yml  ← Day 10: GitHub Actions pipeline
├── docker-compose.yml        ← Day 10: full stack: postgres + kafka + app + observability
├── .env.example              ← Sample environment variables
└── .gitignore
```

The full per-day walkthrough lives in [`./student-guides/`](./student-guides/README.md).
**Read [`student-guides/day0/README.md`](./student-guides/day0/README.md) before you start.**

---

## Prerequisites

- Java 17 (Temurin recommended)
- Maven 3.9+
- Node.js 20+ and npm
- Docker Desktop (with at least 4 GB RAM allocated)
- PostgreSQL 15 (or use the bundled Docker container)
- Git
- IDE: IntelliJ IDEA (backend) + VS Code (frontend) recommended

## Quick start (after Day 4)

```bash
# 1. Bring up infrastructure (Postgres + Kafka + Prometheus + Grafana)
docker compose up -d postgres kafka prometheus grafana

# 2. Run the backend (Liquibase runs migrations automatically on startup)
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 3. Run the frontend
cd ../frontend
npm install
npm run dev

# 4. Open
# - Swagger UI:     http://localhost:8080/swagger-ui.html
# - Frontend:       http://localhost:5173
# - Prometheus:     http://localhost:9090
# - Grafana:        http://localhost:3000  (admin/admin)
# - Kafdrop (Kafka): http://localhost:9000
```

---

## Deploy to the demo laptop (Day 10)

The deploy story is **GitHub Actions builds + pushes Docker images to GHCR; the
demo laptop pulls them and runs the full stack via `docker compose up`.** No
cloud hosting, no PaaS — the demo laptop *is* the deploy target.

```bash
# One-time on the demo laptop (uses a GitHub PAT with read:packages scope):
echo "<your-PAT>" | docker login ghcr.io -u <gh-username> --password-stdin

# Each deploy:
docker compose pull        # fetches the latest CI-tested images from GHCR
docker compose up -d       # brings up all 7 services
```

Full walkthrough: [`student-guides/day10/day10-local-cicd.md`](./student-guides/day10/day10-local-cicd.md).

---

## How to read the TODOs in this codebase

Every place you must write code has a comment block that looks like this:

```java
// ============================================================================
// TODO(TICKET-IXXX): <short ticket title>
// WHAT:    <what you must build>
// HOW:     <hint on how to build it>
// WHY:     <why this matters in the bigger system>
// OBSERVE: <what to look for or verify when you're done>
// HINT:    <optional extra hint or link to a related ticket>
// ============================================================================
```

The full ticket text, acceptance criteria, and detailed hints live in the
matching day's README under [`student-guides/`](./student-guides/README.md).

---

## Daily flow

| Day | Theme | New Tickets |
|----:|-------|-------------|
| 0   | Introduction & onboarding | — |
| 1   | PostgreSQL + Liquibase    | I001–I015 |
| 2   | Java OOP fundamentals     | I016–I027 |
| 3   | OOP patterns + SOLID      | I028–I040 |
| 4   | Collections, JDBC, JUnit  | I041–I053 |
| 5   | Spring Boot foundations   | I054–I067 |
| 6   | REST + Security + Monitoring | I068–I085 |
| 7   | HTML + CSS dashboard      | I086–I092 |
| 8   | Vanilla JS (AM) + React (PM) | I093–I111 |
| 9   | React advanced + Kafka    | I112–I124 |
| 10  | Docker + CI/CD + Demo     | I125–I140 |

---

## Branching

Use **GitFlow**:

```
main      ← only release tags (v1.0.0 at end of Day 10)
develop   ← integration branch — your team merges here
feature/* ← one branch per ticket (e.g. feature/I017-trade-class)
```

Open a Pull Request from each `feature/*` branch into `develop`. Get one
team-mate review before merge.

---

## Final demo (Day 10)

A 15-minute end-to-end walkthrough:

| Minutes | Content |
|--------:|---------|
| 3       | Problem statement + architecture diagram |
| 5       | Live demo: post a trade → Kafka event → recon → resolve → Grafana metric |
| 4       | Code walkthrough (one feature you're proud of) |
| 3       | Q&A |

Good luck — and ask your instructors anything! 🏦
