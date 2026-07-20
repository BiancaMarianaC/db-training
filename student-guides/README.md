# Student Guides — TradeFlow Trade Reconciliation Dashboard

> Deutsche Bank — TDI 2026 Graduate Technical Training Programme
> **Intermediate Track** | 10-Day Case Study | March 2026

This is your day-by-day companion through the TradeFlow case study. Each day
has its own folder with a `README.md` containing **every ticket spelled out** —
description, acceptance criteria, and focused hints.

## How to use this guide

1. **Start with Day 0** — it sets the case-study context, terminology, and end goal.
2. **Each morning, open that day's `README.md`** — it lists every ticket
   assigned that day, the acceptance criteria, and hints.
3. **In the code**, search the `tradeflow-studentscopy/` repo for `TICKET-IXXX`
   to jump directly to the place you have to write code.

## Index

| Day | Folder | Theme | Tickets |
|----:|--------|-------|---------|
| 0   | [day0/](./day0/README.md)   | Onboarding, terminology, end goal     | —          |
| 1   | [day1/](./day1/README.md)   | PostgreSQL + Schema Design            | 13 active: I001–I008, I011–I015 |
| 2   | [day2/](./day2/README.md)   | Java OOP fundamentals + Liquibase (taught + applied) | 14 active: I016–I027, I009, I010 |
| 3   | [day3/](./day3/README.md)   | OOP patterns + SOLID                  | I028–I040  |
| 4   | [day4/](./day4/README.md)   | Collections, JDBC, JUnit              | I041–I053  |
| 5   | [day5/](./day5/README.md)   | Spring Boot foundations               | I054–I067  |
| 6   | [day6/](./day6/README.md)   | REST + Security + Monitoring          | I068–I085  |
| 7   | [day7/](./day7/README.md)   | HTML + CSS dashboard + JS Essentials (taught & applied) | I086–I092, I092A |
| 8   | [day8/](./day8/README.md)   | Vanilla JS sprints (AM) + React Modules 1 & 2 (PM) | 15 active: I093, I095–I097, I099–I104, I106–I110 |
| 9   | [day9/](./day9/README.md)   | Apache Kafka + React advanced polish | I112–I124, I124A–D |
| 10  | [day10/](./day10/README.md) | Docker + CI/CD + Demo                 | I125–I140  |

## Conventions in every day's README

Every ticket entry follows this layout:

> **TICKET-IXXX — Short title**
> *What:* the thing you have to deliver.
> *Acceptance criteria:* a checklist your work must satisfy.
> *Hints:* shortcuts and gotchas — read these BEFORE diving in.
> *Files to touch:* paths in `tradeflow-studentscopy/`.

## Ground rules

- **Pair / mob** on tickets when possible.
- **One branch per ticket** (`feature/I017-trade-class`) → PR → review → merge to `develop`.
- **Write the test first** when the ticket involves business logic.
- **Don't move on without a green build** — ask the instructor if blocked for >20 minutes.
- **All AI-assisted code MUST be reviewed by you** before commit. Cite the prompt in the PR description.

## Day-1 quick-start

The starter ships with a working Day-1 bootstrap. Boot it before touching any tickets:

```bash
# Terminal 1 — backend (port 8080)
cd tradeflow-studentscopy/backend
./mvnw spring-boot:run

# Terminal 2 — frontend (port 5173)
cd tradeflow-studentscopy/frontend
npm install && npm run dev
```

Open:
- React UI: <http://localhost:5173>
- Swagger:  <http://localhost:8080/swagger-ui.html>
- H2 console: <http://localhost:8080/h2-console> (JDBC URL `jdbc:h2:mem:tradeflow`, user `sa`)

You should see 10 trades + 3 OPEN breaks already in the UI. That's your starting baseline.
