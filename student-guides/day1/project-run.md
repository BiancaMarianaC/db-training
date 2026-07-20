# Day 1 — How to run the project

> Step-by-step setup for **Day 1 only**. Day 1 is database work — you need the
> backend running against H2 in-memory so Liquibase applies your changesets on
> every restart. The frontend is optional today.
>
> For the full Docker Compose / Kafka / Grafana / GHCR pull-and-deploy story,
> see [`day0/project-run.md`](../day0/project-run.md) — that one is for Day 10.

All paths in this doc are relative to the project root
(`tradeflow-studentscopy/`, the folder containing `backend/`, `frontend/`,
`docker-compose.yml`, and `student-guides/`).

---

## 1. Pre-flight (10 min, once per laptop)

### 1.1 Toolchain check

> **Terminal** — anywhere

```bash
java -version          # must report 17.x (Temurin recommended)
mvn -version           # 3.9+ — OR skip, the repo ships ./mvnw
git --version
```

For the optional frontend later today:

```bash
node -v                # 20.x
npm -v
```

You do **not** need Docker on Day 1 if you stick with the H2 in-memory option
(recommended). Docker only matters if you want to run real Postgres locally.

**Common Day-1 toolchain mistake:** `java -version` reports 21 or 24 because
the laptop has multiple JDKs. Spring Boot 3.2 will compile against newer JDKs
but our pom pins `<java.version>17</java.version>`. Fix it:

```bash
# macOS — list installed JDKs, then point JAVA_HOME at 17
/usr/libexec/java_home -V
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
java -version          # should now say 17

# Linux — usually:
sudo update-alternatives --config java

# Windows PowerShell — set persistently for this user:
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Path\To\jdk-17", "User")
```

Add the `export JAVA_HOME=...` line to `~/.zshrc` (macOS) or `~/.bashrc`
(Linux) so it sticks across reboots.

### 1.2 Get the project

If your team has already created the GitHub repo (that's TICKET-I001):

```bash
git clone <your-team-repo-url>
cd tradeflow-studentscopy
```

If not yet — work from the trainer-provided starter on your laptop. You'll
push it to GitHub when you do TICKET-I001 later this morning.

### 1.3 Open in your IDE

- **Backend** (Java) — IntelliJ IDEA. Open the `backend/` folder as a Maven
  project. Wait for the bottom-right progress bar to finish indexing
  (~30 seconds first time).
- **Frontend** (optional today) — VS Code. Open the `frontend/` folder.

---

## 2. Boot the backend (3 min)

This is the only thing you strictly need on Day 1.

> **Terminal #1** — from `tradeflow-studentscopy/backend/`

```bash
./mvnw spring-boot:run
```

(Windows PowerShell: `.\mvnw.cmd spring-boot:run`)

First run downloads Maven dependencies — takes ~60 seconds. Subsequent runs
are ~10 seconds.

### What you should see at boot

Watch for these specific lines in the log (last ~15 lines of output):

```
INFO  liquibase.lockservice : Successfully acquired change log lock
INFO  liquibase.changelog   : Reading from PUBLIC.DATABASECHANGELOG
INFO  liquibase.changelog   : ChangeSet db/changelog/changes/001-init.xml::001-init::tradeflow-team ran successfully in 4ms
INFO  liquibase.lockservice : Successfully released change log lock
INFO  o.s.b.w.embedded.tomcat.TomcatWebServer : Tomcat started on port(s): 8080 (http)
INFO  c.d.t.TradeflowApplication : Started TradeflowApplication in 6.2 seconds
```

If you see `Started TradeflowApplication`, you're good. The `dev` profile is
active by default → H2 in-memory DB → backend on `http://localhost:8080`.

### If the app crashed at boot

95% of Day-1 boot failures are one of these three. Match the message:

| Symptom in log | Cause | Fix |
|---|---|---|
| `FileNotFoundException: classpath:db/changelog/...` | Stale `classpath:` prefix on `spring.liquibase.change-log` in `application.yml`. | Remove the `classpath:` prefix. Details in [`day2/day02-liquibase.md`](../day2/day02-liquibase.md) §5 Pitfall 1. |
| `script statement is empty` on `data.sql` | Someone deleted the `SELECT 1;` placeholder in `data.sql`. | Restore the `SELECT 1;` line. The placeholder stays until Day 5 / TICKET-I066. |
| `Port 8080 was already in use` | Another process is on 8080 — often a forgotten previous boot. | `lsof -i :8080` to find the PID, `kill <PID>`. Or override: `./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081`. |

For everything else, search the stack trace in
[`day2/day02-liquibase.md`](../day2/day02-liquibase.md) §8 — it lists nine
common errors with exact fix steps.

---

## 3. Verify the backend is actually alive (2 min)

Open a **second terminal** (leave the backend running in Terminal #1).

### 3.1 Health check

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP"}
```

### 3.2 Trades endpoint returns an empty list

```bash
curl http://localhost:8080/api/v1/trades
# Expected: []
```

Empty array is correct — the `trades` table doesn't exist yet because YOU
are about to write the changeset that creates it (TICKET-I003).

### 3.3 Swagger UI lists every endpoint

Open in a browser:

```
http://localhost:8080/swagger-ui.html
```

You should see all REST endpoints listed (most will throw
`UnsupportedOperationException: TICKET-IXXX` if you call them — those are
stubs you'll implement on Days 5/6).

### 3.4 H2 console — your main verification tool today

This is the single most important tool for Day 1. Open in a browser:

```
http://localhost:8080/h2-console
```

Connect using these exact values (copy/paste them — typos are the #1 issue):

| Field | Value |
|---|---|
| Saved Settings | `Generic H2 (Embedded)` |
| Driver Class | `org.h2.Driver` |
| **JDBC URL** | `jdbc:h2:mem:tradeflow` |
| User Name | `sa` |
| Password | *(leave blank)* |

Click **Connect**. You should see a SQL editor + a left-hand schema tree.

Run this query to confirm the starting state:

```sql
SHOW TABLES;
```

You should see exactly two rows:

```
TABLE_NAME
------------------------
DATABASECHANGELOG
DATABASECHANGELOGLOCK
```

That's correct — those are Liquibase's bookkeeping tables. The five real
TradeFlow tables (`trades`, `instruments`, `counterparties`, `settlements`,
`recon_breaks`) are tickets I003–I007 — you'll write them today.

---

## 4. (Optional) Boot the frontend (5 min)

Day 1 is database-heavy and you can skip the frontend entirely. But it's a
quick win to confirm the wiring works.

> **Terminal #3** — from `tradeflow-studentscopy/frontend/`

```bash
npm install       # first time only, ~30 seconds
npm run dev
```

You should see:

```
  VITE v5.x  ready in 800 ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
```

Open `http://localhost:5173`. You'll see the TradeFlow React shell with
**zero rows** in every table — because every `GET /api/...` returns `[]`
right now. That's expected. The UI starts showing data once your Day-1
schema + seed land.

---

## 5. The Day-1 inner loop (rest of the day)

Open `student/student-guides/day1/README.md` and work tickets I001 → I015
in order. From TICKET-I003 onwards, every ticket follows this loop:

```
                ┌────────────────────────────────────────────┐
                │ 1. Write a Liquibase changeset under       │
                │    backend/src/main/resources/             │
                │      db/changelog/changes/0XX-yourfile.xml │
                └─────────────────┬──────────────────────────┘
                                  │
                                  ▼
                ┌────────────────────────────────────────────┐
                │ 2. Add an <include file="..."/> line in    │
                │    db.changelog-master.xml                 │
                └─────────────────┬──────────────────────────┘
                                  │
                                  ▼
                ┌────────────────────────────────────────────┐
                │ 3. RESTART backend in Terminal #1:         │
                │    - Ctrl+C                                │
                │    - ./mvnw spring-boot:run                │
                │    Liquibase applies your changeset.       │
                └─────────────────┬──────────────────────────┘
                                  │
                                  ▼
                ┌────────────────────────────────────────────┐
                │ 4. Open H2 console → verify the table:     │
                │    SHOW TABLES;                            │
                │    SELECT * FROM information_schema.columns│
                │      WHERE table_name = 'TRADES';          │
                └─────────────────┬──────────────────────────┘
                                  │
                                  ▼
                ┌────────────────────────────────────────────┐
                │ 5. Commit, open a PR into develop.         │
                │    Move to the next ticket.                │
                └────────────────────────────────────────────┘
```

H2 in-memory means every restart starts with an empty DB and re-applies all
your changesets. Fast iteration — and a broken changeset crashes the app
loudly at boot, so you can never accidentally ship one.

### Stuck on a ticket? Use the progressive hints

Every ticket in the Day 1 README has a progressive `<details>` block:

- **Hint 1 (basic nudge)** — open after ~10 minutes stuck.
- **Hint 2 (more guided)** — open after ~20 minutes.
- **Hint 3 (code skeleton)** — open after ~30 minutes.
- **Reference Solution (complete code)** — open after ~40 minutes, or if
  you're behind schedule. This is the safety net — not cheating. Copy the
  code, adapt to your file, and keep moving.

The TOC budgets ~45 minutes per sprint. If a ticket is eating all of it,
jump to the Reference Solution.

---

## 6. End-of-day shutdown

In Terminal #1: `Ctrl+C`. H2 lives in JVM memory — everything is wiped.

That's fine because your *changesets* live in `git`. Tomorrow's
`./mvnw spring-boot:run` replays them all from scratch in about 6 seconds.
That's the whole point of the Liquibase setup: the database is a function
of the code, not the other way around.

Push your branch before you close the laptop:

```bash
git status                        # confirm you know what's staged
git add backend/src/main/resources/db/changelog/
git commit -m "I00X: <short title>"
git push origin feature/I00X-<short-name>
```

Open a PR into `develop`. Ask a team-mate for a review. Merge once it's
green.

---

## 7. Day-1 FAQ

**Q. The Day 1 README mentions running against Postgres in Docker. Do I need to?**
No, not on Day 1. H2 in-memory is faster to iterate against (no Docker
overhead, no container startup time) and supports Postgres-style SQL via
`MODE=PostgreSQL`. Postgres comes into play on Day 5 (`uat` profile) and
Day 10 (Docker Compose).

**Q. The Day 1 README mentions Liquibase running from the command line. Do I need to install Liquibase separately?**
No. Spring Boot's auto-config runs Liquibase **inside the app at boot**. The
`./mvnw spring-boot:run` command is your "run Liquibase" command. You never
need the standalone Liquibase CLI on Day 1.

**Q. Do I need to start Docker today?**
No, unless you're running the optional Postgres path. H2 mode needs zero
Docker.

**Q. My changeset failed — how do I undo it without restarting from scratch?**
On H2 (default dev profile): just restart the app. H2 is in-memory; the DB
is wiped. Liquibase re-applies all your changesets from a clean state on
boot. There's no "undo a single changeset" needed.

**Q. I edited a changeset that already ran and now the app refuses to boot with `Validation Failed`.**
On H2: just restart. The checksum is recomputed against the new file
content because the DB was wiped.
On Postgres: roll back first with
`./mvnw liquibase:rollback -Dliquibase.rollbackCount=1`, then re-apply.
Better habit: don't edit applied changesets — write a *new* changeset that
patches the previous one. Applied changesets are immutable.

**Q. The H2 console keeps saying "Wrong user name or password".**
Check the JDBC URL exactly — typos like `jdbc:h2:mem:tradflow` (missing 'e')
silently create a *new* in-memory DB with no tables and no `sa` user, then
fail the connect. Copy the URL from §3.4 character-for-character.

**Q. How do I view the SQL Liquibase actually ran?**
In the H2 console:
```sql
SELECT id, author, filename, exectype, dateexecuted, md5sum
  FROM databasechangelog
 ORDER BY orderexecuted;
```
Each row is a changeset Liquibase has applied to this DB.

**Q. Can I write SQL directly in `data.sql` instead of a Liquibase changeset?**
On Day 1, no. The whole point is to learn Liquibase. `data.sql` is for the
optional dev-profile seed on Day 5 / TICKET-I066. Stick with `<insert>` /
`<sql>` inside changesets for Day 1's I011–I013 seed tickets.

**Q. Do I need IntelliJ specifically, or can I use VS Code for the backend?**
VS Code works. IntelliJ has better Java tooling out of the box (Maven
integration, run config, debugger) but VS Code with the Microsoft Java pack
is fine. Pick whichever you're already fast in.

---

## 8. Quick-reference URLs

While the backend is running on the `dev` profile:

| URL | Purpose |
|---|---|
| <http://localhost:8080/actuator/health> | `{"status":"UP"}` — confirms backend is alive |
| <http://localhost:8080/swagger-ui.html> | API endpoint catalog |
| <http://localhost:8080/h2-console> | Verify your schema (JDBC URL `jdbc:h2:mem:tradeflow`, user `sa`, blank password) |
| <http://localhost:8080/api/v1/trades> | Returns `[]` until you wire it up on Day 5 |
| <http://localhost:5173> | React UI (only if `npm run dev` is running) |

---

## 9. Where to go next

- New to the case study? Read [`day0/README.md`](../day0/README.md) before
  Day 1.
- The full ticket list with 4-tier hints: [`day1/README.md`](./README.md).
- Liquibase deep-dive (Day 2 lab): [`day2/day02-liquibase.md`](../day2/day02-liquibase.md).
- Full-stack run with Docker Compose + Kafka + Grafana (Day 10 deploy):
  [`day0/project-run.md`](../day0/project-run.md).

---

**Back to:** [Day 1 README](./README.md) · **Day 2:** [Java OOP fundamentals](../day2/README.md)
