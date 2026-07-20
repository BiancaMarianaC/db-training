# Day 0 — Welcome to TradeFlow

> Read this **before Day 1** — it sets the business context, vocabulary,
> architecture, and what "done" looks like at the end of the programme.

---

## 1. The business problem

When a bank executes a trade — say, buying 1,000 shares of SAP for a client — that
single trade is recorded in **at least two places**:

1. **Inside the bank** — by the front office order management system.
2. **Outside the bank** — by the counterparty (broker, exchange, or
   custodian) that the trade was executed against.

At the end of the day, these two records **must agree** on every detail: the
trade reference, the instrument, the quantity, the price, the settlement
date, and the counterparty. When they don't agree, that disagreement is
called a **break** (or a *reconciliation break*). Breaks can cost the bank
money (failed settlement, regulatory fines) or signal something more
serious (a bug, an attempted fraud, or operational risk).

**Trade Reconciliation** is the operational process of:

1. Pulling internal and external trade feeds.
2. Matching them.
3. Detecting and classifying mismatches.
4. Routing breaks to Ops staff for resolution.
5. Reporting and auditing every action.

TradeFlow is a **simplified** version of that platform — small enough that a
team of 3–4 grads can build it in 10 days, but realistic enough that the
patterns and decisions are the same ones you will see on a real DB platform.

---

## 2. End goal — what you will have on Day 10

By the end of Day 10 your team will be able to demonstrate:

A **PostgreSQL** database with the schema defined as Liquibase changelogs
   (version-controlled migrations, no manual `psql` scripts).

A **Spring Boot** service (`recon-service`) exposing a REST API with:
   - CRUD endpoints for trades.
   - Reconciliation trigger + results endpoint.
   - Spring Security with **role-based access** (TRADER vs VIEWER).
   - **Swagger/OpenAPI** documentation at `/swagger-ui.html`.
   - **Prometheus metrics** at `/actuator/prometheus`.

A **React** frontend (`recon-ui`) with:
   - Trade dashboard with summary cards.
   - Trade entry form with client-side validation.
   - Reconciliation results page with status badges.
   - Resolve-break flow with optimistic UI.

**Apache Kafka** event streaming:
   - `trade-events` topic produced on every trade create/update.
   - Consumers for auto-reconciliation and audit logging.

**Prometheus + Grafana** observability:
   - API request rate, p95 latency, error rate dashboards.
   - Kafka consumer lag panel.

**Docker Compose** that brings the whole stack up with one command.

A **GitHub Actions** CI/CD pipeline that builds, tests, validates the
   Liquibase migration on every push, and **publishes the backend + frontend
   Docker images to GitHub Container Registry (GHCR)**. The demo laptop is
   the deploy target — `docker compose pull && docker compose up -d` brings
   the latest CI-tested build up locally with all 7 services (no cloud host).

A **15-minute final demo** for instructors and peers.

---

## 3. Terminology cheat sheet

Mark this page — you will hear these terms every day.

### Trading & operations

| Term | Definition |
|------|------------|
| **Trade** | An agreement to buy or sell a financial instrument. Has a unique trade reference, quantity, price, date, counterparty. |
| **Trade reference (`trade_ref`)** | The natural key for a trade — typically a string like `TRD-2026-0001`. Globally unique. |
| **Instrument** | What is being traded. E.g. an equity (SAP.DE), a bond, an FX pair. Identified by an `ISIN` or internal symbol. |
| **ISIN** | International Securities Identification Number — 12-char globally-unique instrument identifier (e.g. `DE0007164600` is SAP). |
| **Counterparty** | The other side of the trade — usually a broker, exchange, or another bank. Identified by an **LEI** (Legal Entity Identifier). |
| **LEI** | 20-character globally-unique legal entity ID. |
| **Asset class** | Category of instrument: EQUITY, FIXED_INCOME, FX, COMMODITY, DERIVATIVE. |
| **Settlement date** | The date money + ownership actually change hands (usually trade date + 2 business days = "T+2"). |
| **Reconciliation** | The process of comparing internal records with external records and flagging mismatches. |
| **Break** | A mismatch — internal says X, external says Y. Must be investigated and resolved. |
| **Discrepancy type** | Category of a break: PRICE_MISMATCH, QUANTITY_MISMATCH, DATE_MISMATCH, MISSING_TRADE. |
| **Audit log** | An append-only history of every change to every trade — required for compliance. |

### Tech

| Term | Definition |
|------|------------|
| **Liquibase** | A tool that version-controls your database schema. Instead of running `CREATE TABLE` manually, you write a **changelog** (XML/YAML), and Liquibase applies it as **changesets** in order. |
| **Spring Boot** | A Java framework that auto-configures sensible defaults so you can stand up a REST service in minutes. |
| **JPA / Hibernate** | Object-relational mapping — write `@Entity` Java classes and Spring Data writes the SQL for you. |
| **JDBC** | The lower-level Java DB API. You'll write JDBC by hand on Day 4 and JPA on Day 5. |
| **DTO** | Data Transfer Object — a plain class used to pass data between layers or across the wire. Decouples your API from your DB entities. |
| **Mockito** | Java mocking library. Lets you fake a `TradeRepository` so you can unit-test `TradeService` without a real DB. |
| **Actuator** | A Spring Boot module that exposes `/health`, `/metrics`, `/prometheus`, `/info` endpoints. |
| **Prometheus** | A time-series database that **pulls** metrics from your app every N seconds. |
| **Grafana** | A dashboard tool that **reads** from Prometheus and renders charts. |
| **Kafka** | A distributed event log. Producers write events to **topics**; consumers read them. Decouples the producer from the consumer. |
| **Topic** | A named channel in Kafka — e.g. `trade-events`. |
| **Consumer group** | A label that lets Kafka distribute messages across multiple consumer instances. |
| **Docker** | Bundles your app + its dependencies into a portable container image. |
| **Docker Compose** | Lets you define and run multi-container applications with one YAML file + `docker compose up`. |
| **GitHub Actions** | CI/CD that runs on every push — checks out code, runs tests, builds Docker images, deploys. |
| **GitFlow** | A branching strategy: `main` for releases, `develop` for integration, `feature/*` per ticket. |

### React-specific

| Term | Definition |
|------|------------|
| **JSX** | HTML-in-JavaScript syntax used by React components. |
| **Hook** | A function like `useState` / `useEffect` that lets a function component manage state and side effects. |
| **Custom hook** | A function you write that composes other hooks (e.g. `useTradeData`). |
| **Prop drilling** | Passing data through several layers of components — annoying but sometimes correct. |
| **Context** | React's built-in solution for sharing state without prop drilling. |
| **React Hook Form** | A popular library for handling forms with validation. |

---

## 4. Architecture at a glance

```
                                    Operations User
                                          │
                                  ┌───────▼──────┐
                                  │  React UI    │ port 5173 (dev) / 80 (prod)
                                  │  (Vite +     │
                                  │   nginx)     │
                                  └───────┬──────┘
                                          │ HTTPS / Basic Auth
                                          │ JSON
                                  ┌───────▼─────────────────────┐
                                  │ Spring Boot recon-service   │ port 8080
                                  │ ┌─────────────────────────┐ │
                                  │ │ TradeController         │ │
                                  │ │ ReconController         │ │
                                  │ │ ─────────────────────── │ │
                                  │ │ TradeService            │ │
                                  │ │ ReconciliationService   │ │
                                  │ │ ─────────────────────── │ │
                                  │ │ TradeRepository (JPA)   │ │
                                  │ └─────────────────────────┘ │
                                  └────┬──────────────────┬─────┘
                                       │ JDBC             │ KafkaTemplate
                                       │                  │ @KafkaListener
                              ┌────────▼─────┐       ┌────▼───────────┐
                              │ PostgreSQL   │       │  Apache Kafka  │
                              │ (Liquibase)  │       │  trade-events  │
                              └──────────────┘       └────┬───────────┘
                                                          │
                                              ┌───────────▼────────────┐
                                              │ Recon + Audit consumers│
                                              │ inside recon-service   │
                                              └────────────────────────┘

         /actuator/prometheus  ─ Prometheus ─Grafana dashboards
```

---

## 5. How the days build on each other

| Day | Layer added | What "done" looks like |
|----:|-------------|------------------------|
| 1   | **DB**            | Schema in Liquibase, seed data in, analytical queries work in psql. |
| 2   | **Domain (Java)** | Plain Java classes model the schema. Console main prints a trade list. |
| 3   | **Business logic** | `ReconciliationService.matchTrades()` flags mismatches. CSV export works. |
| 4   | **JDBC + Tests**  | Real PostgreSQL connection via JDBC DAOs. JUnit + Mockito > 70% coverage. |
| 5   | **Spring Boot**   | Service boots, JPA entities replace DAOs, Swagger UI lists endpoints. |
| 6   | **REST + Security + Metrics** | API is live, Postman flows green, Prometheus shows app metrics, Grafana shows charts. |
| 7   | **HTML + CSS**    | 4 hand-authored, brand-styled, responsive pages. No JS yet. |
| 8   | **JS (AM) + React (PM)** | AM: vanilla JS wires the Day-7 pages to the API. PM: Vite + React rebuilds the same dashboard with hooks + router. |
| 9   | **Kafka**         | Posting a trade fires an event → consumer auto-runs recon → audit log writes. |
| 10  | **Ops**           | Whole stack via `docker compose up`. GitHub Actions runs build + tests + Liquibase validate **and pushes images to GHCR** on every push to `develop`/`main`. Demo laptop pulls + runs. Final demo. |

---

## 6. Team norms (read this with your team on Day 0)

1. **Daily stand-up (5 min, 9:00 AM)** — each person says: what I did yesterday,
   what I'll do today, anything blocking me.
2. **Pair on hard tickets**, work solo on easy ones. Switch pairs daily.
3. **One PR per ticket.** Title pattern: `[TICKET-I017] Add Trade class`.
4. **Get one review** before merging to `develop`.
5. **Never push directly to `main`.** Only release tags from `develop`.
6. **End-of-day demo (15 min, 5:00 PM)** — one team-member walks the
   instructor through the day's work and any blockers.
7. **Ask early.** If you're stuck for more than 20 minutes, raise a hand. The
   instructor would rather unblock 5 people for 5 minutes than have 5 people
   silently lose an hour.

---

## 7. Setup checklist (do this before Day 1)

```bash
# Confirm versions:
java -version          # 17.x.x
mvn -v                 # 3.9.x
node -v                # 20.x
npm -v                 # 10.x
docker --version       # 24.x+
docker compose version # v2.x+
git --version          # 2.x

# Clone the starter:
cd ~/Desktop
git clone <your-team's-repo-url> tradeflow
cd tradeflow

# Copy .env:
cp .env.example .env

# Bring up an empty Postgres to confirm Docker works:
docker compose up -d postgres
docker compose ps      # postgres should be 'healthy'
docker compose down
```

If anything in that checklist fails, raise a hand before Day 1 starts.

---

## 8. Where to find help

| Question type | Where to look |
|---------------|---------------|
| What does this ticket want? | The day's README under `student-guides/` |
| Where do I write the code? | Search the repo for the ticket ID (e.g. `TICKET-I017`) |
| How do I do X in Spring Boot? | Spring docs first → then Stack Overflow → then Claude/Copilot |
| My build is broken | Read the FULL stack trace, then ask your pair, then the instructor |
| Conceptual / domain question | Ask the instructor — domain questions are the highest-value to discuss live |

---

## 9. AI use policy

- **Allowed:** Using Claude / ChatGPT / Copilot to **explain** concepts,
  **scaffold** boilerplate, **review** your code, and **debug**.
- **Required:** Read every line of AI-generated code before committing.
  Understand it. Be able to defend it in code review.
- **Required:** Note in the PR description if AI helped — e.g. "Used Claude to
  generate the initial @ControllerAdvice; I refactored the error envelope shape."
- **Forbidden:** Pasting AI output blindly. We will ask you to explain
  random lines from your PRs.

The tickets explicitly marked "AI-assisted" (Days 1, 3, 7, 9) are exercises
in **prompting + reviewing** — not just code generation.

---

## 10. Ready?

Head to [Day 1](../day1/README.md) when Day 1 kicks off.
Good luck.

---

**Running the project:** see [project-run.md](./project-run.md) — practical
reference for booting the platform at any point in the programme (Day-1
quick run, Postgres-backed, full Docker stack, login credentials, smoke
test, common gotchas).
