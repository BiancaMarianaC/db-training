# Day 1 — PostgreSQL & Schema Design

> Theme: **Design the reconciliation schema.**
> Tickets: **13 active** — `I001`–`I008`, `I011`–`I015`. Tickets `I009` + `I010` are now **Day 2 Sprint 3** (wire master changelog + run + verify), where the Excel curriculum runs the *advanced* Liquibase teach block. Today's Day-1 AM ends with a ~20-min "Liquibase Foundations" primer (added to the Excel) so authoring `<changeSet>` XML in I003–I015 is taught-then-applied within Day 1.
> Module: Database & SQL — Modules 1 & 2

By end of day your team must have:

1. A GitHub repository with GitFlow branches.
2. An ER diagram of the trade-reconciliation domain.
3. Six Liquibase changeset XML files authored (one per table + FKs/indexes).
4. Seed data + views + AI `audit_log` all written as changeset files.
5. *(Tomorrow Day 2 Sprint 3:* wire the master changelog + run + verify them with Liquibase taught proper.*)*

---

## What you start with

The starter project is intentionally **minimal** — every ticket below is
yours to implement. The app boots out of the box but the schema is empty
and the API endpoints return empty responses.

---

## How to run the project on Day 1

> **What "running" means today:** boot Spring Boot in the `dev` profile
> against an in-memory H2 database. Liquibase applies the no-op smoke
> changeset, the app comes up, but the schema is empty and GET endpoints
> return `[]`. **Day 1 is database work** — most of your time is in
> Liquibase XML and SQL, not in the running app. The frontend is optional
> on Day 1.

> **Full step-by-step run guide:** [`day1/project-run.md`](./project-run.md)
> — toolchain checks, troubleshooting, H2-console verification, the inner
> loop, and a Day-1 FAQ. Open it the first time you run the project; the
> quick version below is enough once you've done it once.

### Prerequisites

- Java 17 (`java -version` → `17.x`)
- Maven 3.9+ (or use the bundled `./mvnw`)
- Docker Desktop — **only** if you want real Postgres instead of H2 (optional today)

### Option A — H2 in-memory (recommended for Day 1)

Fastest iteration loop. No Docker needed. The DB is wiped on every
shutdown — which is exactly what you want while writing Liquibase
changesets and re-running them dozens of times.

> **Terminal #1 — backend** (run from `tradeflow-studentscopy/backend/`)

```bash
./mvnw spring-boot:run
```

Wait for `Started TradeflowApplication`. The `dev` profile is active by
default → H2 in-memory DB → backend listens on `http://localhost:8080`.

> **Terminal #2 — frontend** (optional on Day 1, run from `tradeflow-studentscopy/frontend/`)

```bash
npm install     # first time only
npm run dev
```

Vite dev server listens on `http://localhost:5173`.

### Option B — Real Postgres in Docker

Closer to the rest of the programme. Slower to iterate (containers take a
few seconds to come up).

> **Terminal — bring up Postgres** (run from project root `tradeflow-studentscopy/`)

```bash
docker compose up -d postgres
docker compose ps         # postgres should be 'Up (healthy)'
```

> **Terminal — backend** (run from `backend/`)

```bash
SPRING_PROFILES_ACTIVE=uat ./mvnw spring-boot:run
```

The `uat` profile points the backend at the Dockerised Postgres at
`postgres:5432` (mapped to your host's `localhost:5432`).

### What you should see on Day 1

| URL | What's there |
|---|---|
| `http://localhost:8080/actuator/health` | `{"status":"UP"}` |
| `GET http://localhost:8080/api/v1/trades` | `[]` (empty list) |
| `GET http://localhost:8080/api/v1/recon/results` | `[]` (empty list) |
| `POST http://localhost:8080/api/v1/trades` | 500 — `TICKET-IXXX` not yet implemented |
| `http://localhost:8080/swagger-ui.html` | Lists every endpoint (most still throw) |
| `http://localhost:8080/h2-console` | JDBC URL `jdbc:h2:mem:tradeflow`, user `sa`, no password. **Only `DATABASECHANGELOG` + lock table exist** until you write the schema. |
| `http://localhost:5173` | React UI loads, shows zero data (since GET returns `[]`) |

### The Day-1 inner loop

1. Write a Liquibase changeset under
   `backend/src/main/resources/db/changelog/changes/` (tickets I003–I010).
2. Add a `<include file="..."/>` line for it in
   `db/changelog/db.changelog-master.xml`.
3. **Restart the backend** (`Ctrl+C` → `./mvnw spring-boot:run`). Liquibase
   applies your new changeset on boot.
4. Open `http://localhost:8080/h2-console` → confirm the table you just
   created shows up.
5. Repeat. By end of Day 1: 5 tables, seed data, 2 views.

### Common issues on Day 1

| Symptom | Cause | Fix |
|---|---|---|
| `FileNotFoundException: classpath:db/changelog/db.changelog-master.xml` | `classpath:` prefix on `spring.liquibase.change-log`. | Drop the prefix — see [day02-liquibase.md §5 Pitfall 1](../day2/day02-liquibase.md). |
| `script statement is empty` on `data.sql` | You deleted the placeholder `SELECT 1;` line. | Put it back. `data.sql` isn't for Day 1 anyway — leave it alone until Day 5. |
| H2 console rejects login | You changed the JDBC URL. | Use exactly: `jdbc:h2:mem:tradeflow`, user `sa`, blank password. |
| `Validation Failed: 1 changesets have changed since they were ran` | You edited an applied changeset. | On H2 just restart — it's in-memory, the checksum resets. On Postgres, write a *new* changeset instead. |

This is your starting line. As you complete the tickets, the schema fills
in, the seed loads, and (by Day 4–5) the React UI starts showing real data.

---

## Sprint 0 — Setup

### TICKET-I001 — Create GitHub repo with GitFlow branching

**What**
- A GitHub repo with `main` + `develop` branches, branch protection on `main`, and one smoke-test PR opened from a `feature/I001-repo-setup` branch.

**Why**
- Every other ticket this fortnight ships as a PR through this workflow — if the repo isn't set up today, Day 2's first commit has nowhere to go.

**Observe**
- GitHub Settings → Branches lists a protection rule for `main` (PR required, 1 review). Direct `git push origin main` from a feature branch is rejected with "Branch protection rule violations".

**Acceptance criteria:**
- [ ] Repo exists on GitHub, all team-mates are collaborators.
- [ ] `main` and `develop` branches exist.
- [ ] `main` is protected: PR required, 1 review, no force-push.
- [ ] Every team member has cloned the repo locally.
- [ ] A `feature/I001-repo-setup` branch is opened as the first PR (smoke test).

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

GitFlow = three branch types: `main` (always releasable), `develop`
(integration), `feature/<ticket-id>-<short>` (per ticket). Protect `main` so
nobody can force-push or merge without a review.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. Create a fresh empty repo on GitHub. Add every team-mate as a collaborator.
2. Clone the starter code locally, push to the new remote.
3. From `main`, create `develop`: `git switch -c develop && git push -u origin develop`.
4. In GitHub → Settings → Branches → "Add branch protection rule" for `main`:
   require a PR, 1 approving review, block force-pushes.
5. Open a throwaway `feature/I001-repo-setup` branch and raise a PR into
   `develop` as a smoke test that the workflow works.
</details>

<details>
<summary>Hint 3 — Code / steps</summary>

```bash
# After cloning the starter into a new GitHub remote
git switch main && git pull
git switch -c develop
git push -u origin develop

# Smoke-test branch
git switch -c feature/I001-repo-setup
echo "Repo bootstrapped" >> docs/CHANGELOG.md
git add docs/CHANGELOG.md
git commit -m "I001: bootstrap repo with GitFlow"
git push -u origin feature/I001-repo-setup
# → open PR feature/I001-repo-setup → develop
```

Branch-protection settings to enable for `main`:
- ☑ Require a pull request before merging
- ☑ Require 1 approving review
- ☑ Do not allow bypassing the above settings
- ☑ Restrict who can push (admins only)
- ☐ Allow force pushes — leave OFF
</details>

<details>
<summary>Reference — full walkthrough</summary>

**Files to touch:** GitHub repo settings (UI clicks) + one smoke-test commit.

### Step 1 — Create the GitHub repo
- New repo (private), name: `tradeflow-<team-name>`.
- **Do NOT** initialise with README/`.gitignore`/licence — the starter has them.
- On the empty-repo page, copy the `git@github.com:<org>/<repo>.git` URL.

### Step 2 — Push the starter code
```bash
cd tradeflow-studentscopy/
git remote remove origin                  # if a stale remote exists
git remote add origin git@github.com:<org>/<repo>.git
git push -u origin main
```

### Step 3 — Create `develop`
```bash
git switch -c develop
git push -u origin develop
```

### Step 4 — Branch protection on `main`
Settings → Branches → Add classic branch protection rule:
- Branch name pattern: `main`
- ☑ Require a pull request before merging → Required approvals: **1**
- ☑ Require status checks to pass before merging (leave empty for now — Day 10 wires CI)
- ☑ Do not allow bypassing the above settings
- Save.

Repeat for `develop` (same rules, 0 approvals if your team prefers fast iteration).

### Step 5 — Smoke-test branch + PR
```bash
git switch -c feature/I001-repo-setup
mkdir -p docs
printf "# TradeFlow Team\n\nTeam: <team-name>\nMembers: <name1>, <name2>, <name3>, <name4>\n" > docs/TEAM.md
git add docs/TEAM.md
git commit -m "I001: team bootstrap"
git push -u origin feature/I001-repo-setup
```

Open the PR in GitHub: base `develop`, compare `feature/I001-repo-setup`.

### Step 6 — Add collaborators
Settings → Collaborators → invite every team-mate by GitHub username. Each
member accepts the invite from their email.
</details>

**Files to touch:** Repo settings (no code files in the starter).

---

### TICKET-I002 — Design ER diagram

**What**
- An ER diagram at `db/erd.md` (Mermaid) covering `trades`, `instruments`, `counterparties`, `settlements`, `recon_breaks` with columns, types, PK/FK, and cardinalities.

**Why**
- Tickets I003–I008 each lift one box from this diagram into a Liquibase changeset — if the diagram is wrong now, the schema is wrong for the rest of the week.

**Observe**
- The Mermaid block renders on GitHub when you view `db/erd.md`. All four FK arrows (`COUNTERPARTIES→TRADES`, `INSTRUMENTS→TRADES`, `TRADES→SETTLEMENTS`, `TRADES→RECON_BREAKS`) point in the correct direction with `||--o{` cardinality.

**Acceptance criteria:**
- [ ] Diagram shows: `trades`, `instruments`, `counterparties`, `settlements`, `recon_breaks`.
- [ ] All columns visible, with types.
- [ ] PK / FK relationships drawn and labelled.
- [ ] Cardinalities marked (1:N, N:N).
- [ ] Committed to `db/erd.md` (Mermaid) or `db/erd.png` and linked from the project README.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Discuss as a team **before** drawing — schema design is the architectural
decision Day 1 turns on. The 5 tables form a clear hub-and-spoke around
`trades`: instruments and counterparties are reference data; settlements and
recon_breaks both hang off `trades`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Mermaid renders directly on GitHub — fastest path. No tools to install.
- Show columns + types + PK/FK on every entity.
- Mark cardinality on each relationship: one trade → many settlements?
  many recon_breaks? (Decide as a team — affects FKs.)
- `recon_breaks` and `settlements` each carry a `trade_id` FK back to
  `trades`. `instruments` and `counterparties` don't reference anything —
  they're reference tables.
</details>

<details>
<summary>Hint 3 — Mermaid skeleton</summary>

```markdown
# TradeFlow — Entity Relationship Diagram

```mermaid
erDiagram
    COUNTERPARTIES ||--o{ TRADES : "originates"
    INSTRUMENTS    ||--o{ TRADES : "references"
    TRADES         ||--o{ SETTLEMENTS  : "settles"
    TRADES         ||--o{ RECON_BREAKS : "may produce"

    TRADES {
        BIGINT id PK
        VARCHAR trade_ref UK
        BIGINT instrument_id FK
        BIGINT counterparty_id FK
        NUMERIC quantity
        NUMERIC price
        DATE trade_date
        VARCHAR status
    }
    %% TODO: define COUNTERPARTIES, INSTRUMENTS, SETTLEMENTS, RECON_BREAKS
``\`
```

Use the schema spec in tickets I003–I007 as the source of truth for each
entity's columns + types.
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `db/erd.md`

```markdown
# TradeFlow — Entity Relationship Diagram

```mermaid
erDiagram
    COUNTERPARTIES ||--o{ TRADES        : "originates"
    INSTRUMENTS    ||--o{ TRADES        : "references"
    TRADES         ||--o{ SETTLEMENTS   : "settles"
    TRADES         ||--o{ RECON_BREAKS  : "may produce"

    COUNTERPARTIES {
        BIGINT id PK
        VARCHAR name
        CHAR(20) lei_code UK
        VARCHAR region "APAC|EMEA|NAMR|LATAM"
    }
    INSTRUMENTS {
        BIGINT id PK
        VARCHAR symbol UK
        VARCHAR name
        VARCHAR asset_class "EQUITY|FIXED_INCOME|FX|COMMODITY|DERIVATIVE"
        CHAR(3) currency
        CHAR(12) isin UK
    }
    TRADES {
        BIGINT id PK
        VARCHAR trade_ref UK
        BIGINT instrument_id FK
        BIGINT counterparty_id FK
        NUMERIC quantity "(18,4) > 0"
        NUMERIC price "(18,4) >= 0"
        DATE trade_date
        VARCHAR status "PENDING|MATCHED|UNMATCHED|DISPUTED|CANCELLED"
        TIMESTAMPTZ created_at
    }
    SETTLEMENTS {
        BIGINT id PK
        BIGINT trade_id FK
        DATE settlement_date
        NUMERIC amount "(18,4) >= 0"
        VARCHAR status "PENDING|SETTLED|FAILED|CANCELLED"
    }
    RECON_BREAKS {
        BIGINT id PK
        BIGINT trade_id FK
        VARCHAR discrepancy_type "PRICE|QUANTITY|MISSING|DUPLICATE|STATUS"
        VARCHAR status "OPEN|INVESTIGATING|RESOLVED|IGNORED"
        TIMESTAMPTZ resolved_at
    }
``\`

## Design decisions worth flagging
| Decision | Why |
|---|---|
| `NUMERIC(18,4)` for quantity + price | IEEE-754 doubles cause silent rounding errors on money. |
| FKs everywhere | DB enforces referential integrity cheaper + safer than app-layer checks. |
| `recon_breaks` (not `recon_results`) | A break is a *negative* finding worth tracking. |
| `CHAR(20)` for `lei_code` | LEI is exactly 20 alphanumerics — fixed width is cheaper + signals intent. |
```
</details>

**Files to touch:** `db/erd.md`.

---

## Sprint 1 — Schema Design  [45 min, groups of 4–5]

Design the DB schema together. Author your changesets under
`backend/src/main/resources/db/changelog/changes/`. Wire each new changeset
into `db.changelog-master.xml`. Use Liquibase's `<createTable>` + `<sql>`
elements — never plain `.sql` files for schema migrations.

**Instructor reviews the schema before you move to Sprint 2.**

### Just-in-time primer — Liquibase in 5 minutes

You'll spend the rest of Day 1 writing schema as Liquibase XML. If you've
never used it before, the mental model fits on one page.

**What it is.** A schema-migration tool. Instead of dropping and recreating
the DB each time, you describe schema *changes* as small ordered scripts
("changesets") and Liquibase applies the ones that haven't run yet.

**Two key files / concepts.**

| Concept | What it is | Where it lives |
|---|---|---|
| **Changeset** | One atomic schema change (`<createTable>`, `<addColumn>`, `<sql>`). Has an `id` + `author` — the pair is its identity. | One per file under `db/changelog/changes/`. |
| **Master changelog** | The ordered include-list of changesets. Liquibase reads it top-to-bottom. | `db/changelog/db.changelog-master.xml`. |
| **DATABASECHANGELOG** | Bookkeeping table Liquibase creates in your DB. Logs every changeset that ran (`id` + `author` + checksum). | Auto-created in the target DB. |

**The contract you sign by using Liquibase.**

1. **Changesets are immutable once shipped.** Edit a changeset that's already
   run in any environment and Liquibase will refuse to start (checksum
   mismatch). To change something later, write a *new* changeset.
2. **Order matters.** Parent tables before children, FKs after both tables
   exist. The master changelog's `<include>` order is your dependency order.
3. **Every changeset needs a `<rollback>` block.** Even a single-line
   `<dropTable>` — so `liquibase rollback` can undo it cleanly.
4. **No `.sql` files for schema.** Use Liquibase's XML elements
   (`<createTable>`, `<addColumn>`, `<addForeignKeyConstraint>`).
   Drop to a `<sql>` block only for CHECK constraints or DB-specific bits.

**Day-1 flow.** Sprint 1 → you author one changeset per table (I003–I007),
then FKs/indexes (I008). Sprint 2 → seed data + views as changesets
(I011–I014). Sprint 3 → AI-assisted `audit_log` changeset (I015). **You
do NOT wire or run them today** — that's Day 2 Sprint 3 where Liquibase
is taught proper (I009 wires the master changelog, I010 boots + verifies).

**Gotchas you will hit (and the fix):**

| Symptom | Why | Fix |
|---|---|---|
| `Validation Failed: ... checksum has changed` | You edited an already-applied changeset | Revert the edit; add a new changeset instead |
| `Cannot find file: classpath:db/changelog/...` | Liquibase 4.x doesn't want a `classpath:` prefix | Drop the prefix — paths are relative to `src/main/resources/` |
| `relation "trades" does not exist` when adding an FK | Wrong include order — child loaded before parent | Reorder `<include>`s so parents come first |
| Tables vanish on restart | You're on H2 in-memory; that's normal | Switch to file-mode H2 or Postgres if you want persistence |

That's the whole tool for today. The code is in the ticket hints below.

---

### TICKET-I003 — Create `trades` table

**What**
- A Liquibase changeset `002-create-trades.xml` that creates the `trades` table with PK on `id`, UNIQUE `trade_ref`, status/quantity CHECKs, and a `<rollback>` block.

**Why**
- `trades` is the hub table every other entity FKs into. Day 4's JPA entities and Day 5's REST endpoints both assume this schema is in place.

**Observe**
- The XML validates against `dbchangelog-4.20.xsd` (your IDE stops underlining it red). The file lives under `backend/src/main/resources/db/changelog/changes/` — it doesn't run today; verification happens Day 2 Sprint 3.

**Acceptance criteria:**
- [ ] New file `backend/src/main/resources/db/changelog/changes/002-create-trades.xml`.
- [ ] Columns: `id`, `trade_ref`, `instrument_id`, `counterparty_id`,
  `quantity`, `price`, `trade_date`, `status`, `created_at`.
- [ ] Primary key on `id`.
- [ ] `trade_ref` UNIQUE NOT NULL.
- [ ] CHECK constraint: `status IN ('PENDING','MATCHED','UNMATCHED','DISPUTED','CANCELLED')`.
- [ ] CHECK: `quantity > 0`, `price > 0`.
- [ ] `<rollback>` block included.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- Money columns use `NUMERIC(18,4)` (or `DECIMAL(18,4)`), **never** `FLOAT`.
- Status / discrepancy enums are enforced with a `CHECK` constraint, not a DB
  type. Use a `<sql>` block inside the `<changeSet>` for that.
- FKs to `instruments` and `counterparties` are NOT added here — they land in
  I008 once both parent tables exist.
- `created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Structure of a Liquibase changeset for a table:

1. Wrap a `<changeSet id="..." author="...">` inside `<databaseChangeLog>`.
2. `<createTable tableName="trades">` with one `<column>` per field.
3. Each column declares its type; constraints go inside `<constraints .../>`.
4. CHECK constraints (range, enum) go in a `<sql>` block AFTER the table
   creation — Liquibase doesn't have a portable `<check>` element.
5. Indexes are separate `<createIndex>` elements (also inside the same
   changeset) — one per index.
6. Add a `<rollback>` block at the end so `liquibase rollback` works.

The `instrument_id` and `counterparty_id` columns are plain `BIGINT` here —
NO `<constraints foreignKeyName=...>` yet. I008 adds those.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

  <changeSet id="002-create-trades" author="your-name">
    <createTable tableName="trades">
      <column name="id" type="BIGINT" autoIncrement="true">
        <constraints primaryKey="true" nullable="false"/>
      </column>
      <!-- TODO: trade_ref (UNIQUE NOT NULL) -->
      <!-- TODO: instrument_id, counterparty_id (BIGINT, NOT NULL — no FK yet) -->
      <!-- TODO: quantity, price (NUMERIC(18,4), NOT NULL) -->
      <!-- TODO: trade_date (DATE, NOT NULL) -->
      <!-- TODO: status (VARCHAR(20), NOT NULL, default 'PENDING') -->
      <!-- TODO: created_at (TIMESTAMP WITH TIME ZONE, default CURRENT_TIMESTAMP) -->
    </createTable>

    <!-- TODO: <sql> with CHECK quantity > 0, price > 0, status IN (...) -->
    <!-- TODO: <createIndex> on trade_date and on status -->

    <rollback>
      <dropTable tableName="trades"/>
    </rollback>
  </changeSet>
</databaseChangeLog>
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/resources/db/changelog/changes/002-create-trades.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

  <changeSet id="002-create-trades" author="your-name">
    <comment>Trades — the central fact table for the recon pipeline.</comment>
    <createTable tableName="trades">
      <column name="id" type="BIGINT" autoIncrement="true">
        <constraints primaryKey="true" nullable="false"/>
      </column>
      <column name="trade_ref" type="VARCHAR(30)">
        <constraints nullable="false" unique="true"
                     uniqueConstraintName="uq_trades_trade_ref"/>
      </column>
      <column name="instrument_id" type="BIGINT">
        <constraints nullable="false"/>
      </column>
      <column name="counterparty_id" type="BIGINT">
        <constraints nullable="false"/>
      </column>
      <column name="quantity" type="NUMERIC(18,4)">
        <constraints nullable="false"/>
      </column>
      <column name="price" type="NUMERIC(18,4)">
        <constraints nullable="false"/>
      </column>
      <column name="trade_date" type="DATE">
        <constraints nullable="false"/>
      </column>
      <column name="status" type="VARCHAR(20)" defaultValue="PENDING">
        <constraints nullable="false"/>
      </column>
      <column name="created_at" type="TIMESTAMP WITH TIME ZONE"
              defaultValueComputed="CURRENT_TIMESTAMP">
        <constraints nullable="false"/>
      </column>
    </createTable>

    <sql>
      ALTER TABLE trades ADD CONSTRAINT chk_trades_quantity_positive
          CHECK (quantity > 0);
      ALTER TABLE trades ADD CONSTRAINT chk_trades_price_non_negative
          CHECK (price >= 0);
      ALTER TABLE trades ADD CONSTRAINT chk_trades_status
          CHECK (status IN ('PENDING','MATCHED','UNMATCHED','DISPUTED','CANCELLED'));
    </sql>

    <createIndex tableName="trades" indexName="idx_trades_trade_date">
      <column name="trade_date"/>
    </createIndex>
    <createIndex tableName="trades" indexName="idx_trades_status">
      <column name="status"/>
    </createIndex>

    <rollback>
      <dropTable tableName="trades"/>
    </rollback>
  </changeSet>
</databaseChangeLog>
```
</details>

**Files to touch:** `backend/src/main/resources/db/changelog/changes/002-create-trades.xml`.

---

### TICKET-I004 — Create `instruments` table

**What**
- A changeset `003-create-instruments.xml` for the `instruments` reference table with UNIQUE `symbol`, `CHAR(3)` ISO-4217 `currency`, and a CHECK on `asset_class`.

**Why**
- `instruments` is the parent table for the FK on `trades.instrument_id` (added in I008). Day 5's instrument-filtered REST queries also lean on this CHECK to keep asset_class values clean.

**Observe**
- The CHECK lives in a `<sql>` block AFTER `<createTable>` — Liquibase's portable XML has no `<check>` element. The `currency` column type is exactly `CHAR(3)`, not `VARCHAR(3)`.

**Acceptance criteria:**
- [ ] New file `003-create-instruments.xml`.
- [ ] Columns: `id`, `symbol`, `name`, `asset_class`, `currency`.
- [ ] `symbol` UNIQUE NOT NULL.
- [ ] CHECK: `asset_class IN ('EQUITY','FIXED_INCOME','FX','COMMODITY','DERIVATIVE')`.
- [ ] `currency` is `CHAR(3)` (ISO 4217).

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- Symbols are short strings like `SAP.DE`, `NVDA`, `EURUSD` — keep them
  `VARCHAR(20)` and UNIQUE.
- Currency is fixed-width — ISO 4217 is always 3 letters → `CHAR(3)`.
- CHECK on `asset_class` is a `<sql>` block, not a column attribute.
- Always include a `<rollback>` that drops the table.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Same pattern as I003:
1. New file under `db/changelog/changes/`.
2. `<changeSet>` → `<createTable tableName="instruments">` → one `<column>`
   per field.
3. CHECK constraint via `<sql> ALTER TABLE ... ADD CONSTRAINT ...</sql>`
   below the `</createTable>`.
4. `<rollback>` block at the end.

Differences vs trades: no money columns, no status, no created_at (instruments
are reference data — they don't churn).
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```xml
<changeSet id="003-create-instruments" author="your-name">
  <createTable tableName="instruments">
    <column name="id" type="BIGINT" autoIncrement="true">
      <constraints primaryKey="true" nullable="false"/>
    </column>
    <!-- TODO: symbol (VARCHAR(20), UNIQUE NOT NULL) -->
    <!-- TODO: name (VARCHAR(200), NOT NULL) -->
    <!-- TODO: asset_class (VARCHAR(20), NOT NULL) -->
    <!-- TODO: currency (CHAR(3), NOT NULL) -->
  </createTable>

  <!-- TODO: <sql> CHECK asset_class IN (...) -->

  <rollback>
    <dropTable tableName="instruments"/>
  </rollback>
</changeSet>
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/resources/db/changelog/changes/003-create-instruments.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

  <changeSet id="003-create-instruments" author="your-name">
    <createTable tableName="instruments">
      <column name="id" type="BIGINT" autoIncrement="true">
        <constraints primaryKey="true" nullable="false"/>
      </column>
      <column name="symbol" type="VARCHAR(20)">
        <constraints nullable="false" unique="true"
                     uniqueConstraintName="uq_instruments_symbol"/>
      </column>
      <column name="name" type="VARCHAR(200)">
        <constraints nullable="false"/>
      </column>
      <column name="asset_class" type="VARCHAR(20)">
        <constraints nullable="false"/>
      </column>
      <column name="currency" type="CHAR(3)">
        <constraints nullable="false"/>
      </column>
    </createTable>

    <sql>
      ALTER TABLE instruments ADD CONSTRAINT chk_instruments_asset_class
          CHECK (asset_class IN ('EQUITY','FIXED_INCOME','FX','COMMODITY','DERIVATIVE'));
    </sql>

    <rollback>
      <dropTable tableName="instruments"/>
    </rollback>
  </changeSet>
</databaseChangeLog>
```
</details>

---

### TICKET-I005 — Create `counterparties` table

**What**
- A changeset `004-create-counterparties.xml` for `counterparties` with `CHAR(20)` UNIQUE NOT NULL `lei_code` and a CHECK constraining `region` to the 4 allowed values.

**Why**
- LEI is an ISO 17442 identifier — exactly 20 chars, no variability. Locking the type at the DB now prevents the "why is one LEI 21 chars?" support ticket later. I012's seed data lands here.

**Observe**
- `INSERT INTO counterparties (...) VALUES ('X', 'SHORTLEI', 'EMEA');` will be rejected (Day 2 once Liquibase runs) — `lei_code` is exactly `CHAR(20)`, no padding shortcut.
- [ ] Columns: `id`, `name`, `lei_code`, `region`.
- [ ] `lei_code` is `CHAR(20)` UNIQUE NOT NULL (Legal Entity Identifier).
- [ ] CHECK: `region IN ('APAC','EMEA','NAMR','LATAM')`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

LEI = Legal Entity Identifier, the ISO 17442 standard. Always exactly 20
characters (letters + digits, no spaces). Example: `W22LROWP2IHZNBB6K528`
(Goldman Sachs International). Use `CHAR(20)` — fixed-width tells the DB
nothing varies.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Same shape as I003 / I004:
- `<changeSet>` → `<createTable tableName="counterparties">` → `<column>` per field.
- `lei_code` UNIQUE NOT NULL, type `CHAR(20)`.
- `region` is a small enum — CHECK constraint via `<sql>` block.
- `<rollback><dropTable .../></rollback>`.

Counterparties are reference data — no `created_at`, no status. They rarely
change; if one does, it's an event for ops, not a row update at scale.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```xml
<changeSet id="004-create-counterparties" author="your-name">
  <createTable tableName="counterparties">
    <column name="id" type="BIGINT" autoIncrement="true">
      <constraints primaryKey="true" nullable="false"/>
    </column>
    <!-- TODO: name (VARCHAR(100), NOT NULL) -->
    <!-- TODO: lei_code (CHAR(20), UNIQUE NOT NULL) -->
    <!-- TODO: region (VARCHAR(10), NOT NULL) -->
  </createTable>

  <!-- TODO: <sql> CHECK region IN ('APAC','EMEA','NAMR','LATAM') -->

  <rollback>
    <dropTable tableName="counterparties"/>
  </rollback>
</changeSet>
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/resources/db/changelog/changes/004-create-counterparties.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

  <changeSet id="004-create-counterparties" author="your-name">
    <createTable tableName="counterparties">
      <column name="id" type="BIGINT" autoIncrement="true">
        <constraints primaryKey="true" nullable="false"/>
      </column>
      <column name="name" type="VARCHAR(100)">
        <constraints nullable="false"/>
      </column>
      <column name="lei_code" type="CHAR(20)">
        <constraints nullable="false" unique="true"
                     uniqueConstraintName="uq_counterparties_lei"/>
      </column>
      <column name="region" type="VARCHAR(10)">
        <constraints nullable="false"/>
      </column>
    </createTable>

    <sql>
      ALTER TABLE counterparties ADD CONSTRAINT chk_counterparties_region
          CHECK (region IN ('APAC','EMEA','NAMR','LATAM'));
    </sql>

    <rollback>
      <dropTable tableName="counterparties"/>
    </rollback>
  </changeSet>
</databaseChangeLog>
```
</details>

---

### TICKET-I006 — Create `settlements` table

**What**
- A changeset `005-create-settlements.xml` with inline FK `trade_id → trades(id)`, planned `settlement_date` (DATE), nullable `settled_at` (TIMESTAMP), and a status CHECK.

**Why**
- `settled_at` minus `trade_date` is exactly what view `v_settlement_lag` (I014) computes. Day 4's settlement entity maps onto this table.

**Observe**
- The FK is declared inline on `trade_id` (parent `trades` already exists since I003); attempting to insert a `settlement` with a non-existent `trade_id` will fail with a referential integrity error once Liquibase runs Day 2.

**Acceptance criteria:**
- [ ] New file `005-create-settlements.xml`.
- [ ] Columns: `id`, `trade_id`, `settlement_date`, `settled_at` (nullable
  TIMESTAMP — null while pending), `status`, `created_at`.
- [ ] FK: `trade_id → trades(id)`.
- [ ] CHECK: `status IN ('PENDING','SETTLED','FAILED')`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- One trade → at most one settlement row (1:1 in the simple case; consider
  1:N if your team decides partial settlements are in scope).
- `settled_at` is what `v_settlement_lag` will compare against in I014 — it's
  nullable while the settlement is still `PENDING`.
- `trade_id` FK is added inline here (trades already exists from I003).
</details>

<details>
<summary>Hint 2 — More guided</summary>

Settlements hang off trades. Because trades was created in I003, you can
declare the FK inline on `trade_id` using `<constraints foreignKeyName="..."
references="trades(id)"/>`.

Two timestamps to distinguish:
- `settlement_date` (DATE): the *planned* settlement date — set on insert.
- `settled_at` (TIMESTAMP, nullable): the actual settlement time — populated
  later when the settlement clears.

Index `trade_id` — almost every query joins back to trades.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```xml
<changeSet id="005-create-settlements" author="your-name">
  <createTable tableName="settlements">
    <column name="id" type="BIGINT" autoIncrement="true">
      <constraints primaryKey="true" nullable="false"/>
    </column>
    <column name="trade_id" type="BIGINT">
      <constraints nullable="false"
                   foreignKeyName="fk_settlements_trade"
                   references="trades(id)"/>
    </column>
    <!-- TODO: settlement_date (DATE, NOT NULL) -->
    <!-- TODO: settled_at (TIMESTAMP WITH TIME ZONE, nullable) -->
    <!-- TODO: status (VARCHAR(20), NOT NULL, default 'PENDING') -->
    <!-- TODO: created_at (TIMESTAMP WITH TIME ZONE, default CURRENT_TIMESTAMP) -->
  </createTable>

  <!-- TODO: <sql> CHECK status IN ('PENDING','SETTLED','FAILED') -->
  <!-- TODO: <createIndex> on trade_id -->

  <rollback>
    <dropTable tableName="settlements"/>
  </rollback>
</changeSet>
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/resources/db/changelog/changes/005-create-settlements.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

  <changeSet id="005-create-settlements" author="your-name">
    <createTable tableName="settlements">
      <column name="id" type="BIGINT" autoIncrement="true">
        <constraints primaryKey="true" nullable="false"/>
      </column>
      <column name="trade_id" type="BIGINT">
        <constraints nullable="false"
                     foreignKeyName="fk_settlements_trade"
                     references="trades(id)"/>
      </column>
      <column name="settlement_date" type="DATE">
        <constraints nullable="false"/>
      </column>
      <column name="settled_at" type="TIMESTAMP WITH TIME ZONE">
      </column>
      <column name="status" type="VARCHAR(20)" defaultValue="PENDING">
        <constraints nullable="false"/>
      </column>
      <column name="created_at" type="TIMESTAMP WITH TIME ZONE"
              defaultValueComputed="CURRENT_TIMESTAMP">
        <constraints nullable="false"/>
      </column>
    </createTable>

    <sql>
      ALTER TABLE settlements ADD CONSTRAINT chk_settlements_status
          CHECK (status IN ('PENDING','SETTLED','FAILED'));
    </sql>

    <createIndex tableName="settlements" indexName="idx_settlements_trade">
      <column name="trade_id"/>
    </createIndex>

    <rollback>
      <dropTable tableName="settlements"/>
    </rollback>
  </changeSet>
</databaseChangeLog>
```
</details>

---

### TICKET-I007 — Create `recon_breaks` table

**What**
- A changeset `006-create-recon-breaks.xml` for `recon_breaks` with inline FK to `trades`, two CHECKs (`status`, nullable `discrepancy_type`), and indexes on `status` + `trade_id`.

**Why**
- `recon_breaks` is the table the dashboard reads most — the "OPEN breaks" widget on Day 6 hits this table on every refresh. The `status` index is what stops that query going full-scan.

**Observe**
- The `discrepancy_type` CHECK is wrapped in `IS NULL OR ...` so the column can stay null on `IGNORED` rows without breaking the constraint.

**Acceptance criteria:**
- [ ] New file `006-create-recon-breaks.xml`.
- [ ] Columns: `id`, `trade_id`, `status`, `discrepancy_type`,
  `resolved_at`, `resolved_by`, `created_at`.
- [ ] FK: `trade_id → trades(id)`.
- [ ] CHECK: `status IN ('OPEN','RESOLVED','IGNORED')`.
- [ ] CHECK: `discrepancy_type IS NULL OR discrepancy_type IN ('PRICE_MISMATCH','QUANTITY_MISMATCH','DATE_MISMATCH','MISSING_TRADE')`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- `resolved_at` is `NULL` while the break is `OPEN` — populated when an ops
  user closes it.
- `discrepancy_type` is nullable (an `IGNORED` break may not have a type yet).
- Index `status` heavily — the dashboard's main query is "give me all OPEN breaks".
- `trade_id` FK to `trades(id)` — trades exists since I003.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Two CHECK constraints to add:
1. `status IN ('OPEN','RESOLVED','IGNORED')`
2. `discrepancy_type IS NULL OR discrepancy_type IN ('PRICE_MISMATCH','QUANTITY_MISMATCH','DATE_MISMATCH','MISSING_TRADE')`
   — note the `IS NULL OR` clause so nullable still passes the CHECK.

Indexes you'll want:
- One on `status` — the "OPEN" filter is the most common query.
- One on `trade_id` — the FK join is hot.

`resolved_by` (VARCHAR) tracks the username who closed the break. Set it
when the ops user resolves; null while OPEN.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```xml
<changeSet id="006-create-recon-breaks" author="your-name">
  <createTable tableName="recon_breaks">
    <column name="id" type="BIGINT" autoIncrement="true">
      <constraints primaryKey="true" nullable="false"/>
    </column>
    <column name="trade_id" type="BIGINT">
      <constraints nullable="false"
                   foreignKeyName="fk_recon_breaks_trade"
                   references="trades(id)"/>
    </column>
    <!-- TODO: status (VARCHAR(20), NOT NULL, default 'OPEN') -->
    <!-- TODO: discrepancy_type (VARCHAR(30), nullable) -->
    <!-- TODO: resolved_at (TIMESTAMP WITH TIME ZONE, nullable) -->
    <!-- TODO: resolved_by (VARCHAR(50), nullable) -->
    <!-- TODO: created_at (TIMESTAMP WITH TIME ZONE, default CURRENT_TIMESTAMP) -->
  </createTable>

  <!-- TODO: <sql> CHECK status IN ('OPEN','RESOLVED','IGNORED') -->
  <!-- TODO: <sql> CHECK discrepancy_type IS NULL OR discrepancy_type IN (...) -->
  <!-- TODO: <createIndex> on status -->
  <!-- TODO: <createIndex> on trade_id -->

  <rollback>
    <dropTable tableName="recon_breaks"/>
  </rollback>
</changeSet>
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/resources/db/changelog/changes/006-create-recon-breaks.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

  <changeSet id="006-create-recon-breaks" author="your-name">
    <createTable tableName="recon_breaks">
      <column name="id" type="BIGINT" autoIncrement="true">
        <constraints primaryKey="true" nullable="false"/>
      </column>
      <column name="trade_id" type="BIGINT">
        <constraints nullable="false"
                     foreignKeyName="fk_recon_breaks_trade"
                     references="trades(id)"/>
      </column>
      <column name="status" type="VARCHAR(20)" defaultValue="OPEN">
        <constraints nullable="false"/>
      </column>
      <column name="discrepancy_type" type="VARCHAR(30)">
      </column>
      <column name="resolved_at" type="TIMESTAMP WITH TIME ZONE">
      </column>
      <column name="resolved_by" type="VARCHAR(50)">
      </column>
      <column name="created_at" type="TIMESTAMP WITH TIME ZONE"
              defaultValueComputed="CURRENT_TIMESTAMP">
        <constraints nullable="false"/>
      </column>
    </createTable>

    <sql>
      ALTER TABLE recon_breaks ADD CONSTRAINT chk_recon_breaks_status
          CHECK (status IN ('OPEN','RESOLVED','IGNORED'));
      ALTER TABLE recon_breaks ADD CONSTRAINT chk_recon_breaks_type
          CHECK (discrepancy_type IS NULL OR discrepancy_type IN
                 ('PRICE_MISMATCH','QUANTITY_MISMATCH','DATE_MISMATCH','MISSING_TRADE'));
    </sql>

    <createIndex tableName="recon_breaks" indexName="idx_recon_breaks_status">
      <column name="status"/>
    </createIndex>
    <createIndex tableName="recon_breaks" indexName="idx_recon_breaks_trade">
      <column name="trade_id"/>
    </createIndex>

    <rollback>
      <dropTable tableName="recon_breaks"/>
    </rollback>
  </changeSet>
</databaseChangeLog>
```
</details>

---

### TICKET-I008 — Foreign keys + indexes

**What**
- A changeset `007-fks-and-indexes.xml` adding the two forward-reference FKs (`trades.instrument_id`, `trades.counterparty_id`) and the indexes `idx_trades_trade_date`, `idx_trades_status`, `idx_recon_open`, `idx_settlements_status`.

**Why**
- These FKs couldn't be inline in I003 because `instruments` and `counterparties` didn't exist yet. Splitting them out is the canonical Liquibase pattern for forward references; Day 2 Sprint 3's "FK order failure" demo lands on this changeset.

**Observe**
- After Day 2's Liquibase run, `SELECT * FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS WHERE TABLE_NAME='TRADES' AND CONSTRAINT_TYPE='REFERENTIAL';` lists both `fk_trades_instrument` and `fk_trades_counterparty`.

**Acceptance criteria:**
- [ ] New file `007-fks-and-indexes.xml`.
- [ ] FKs on `trades(instrument_id → instruments.id)` and
  `trades(counterparty_id → counterparties.id)`.
- [ ] Indexes: `idx_trades_trade_date`, `idx_trades_status`,
  `idx_recon_open` (on `recon_breaks.status`), `idx_settlements_status`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- FKs from `trades` → `instruments` and `trades` → `counterparties` could NOT
  be inline in I003 (those parent tables didn't exist yet). Add them here
  with `<addForeignKeyConstraint>`.
- FKs from `settlements` and `recon_breaks` to `trades` were already inline in
  I006/I007 — don't duplicate them.
- `EXPLAIN ANALYZE` your hot queries before and after adding the index. Paste
  both plans into the PR. That's how you prove the index does something.
</details>

<details>
<summary>Hint 2 — More guided</summary>

The changeset has two kinds of `<sql>`-less Liquibase elements:

**FKs** — for each, name + parent column → child table + child column:
```xml
<addForeignKeyConstraint
    constraintName="fk_trades_instrument"
    baseTableName="trades"          baseColumnNames="instrument_id"
    referencedTableName="instruments" referencedColumnNames="id"/>
```

**Indexes** — one `<createIndex>` per index:
```xml
<createIndex tableName="trades" indexName="idx_trades_status">
  <column name="status"/>
</createIndex>
```

**Heads-up on duplicates:** if you already added `idx_trades_trade_date` in
I003 (the reference solution did), don't add it again here — Liquibase will
fail with "index already exists". Add ONLY the indexes the I008 acceptance
criteria lists that you haven't created yet.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```xml
<changeSet id="007-fks-and-indexes" author="your-name">
  <!-- TODO: addForeignKeyConstraint fk_trades_instrument -->
  <!-- TODO: addForeignKeyConstraint fk_trades_counterparty -->

  <!-- TODO: createIndex idx_trades_status (if not added in I003) -->
  <!-- TODO: createIndex idx_settlements_status -->
  <!-- TODO: createIndex idx_recon_open on recon_breaks(status) (if not added in I007) -->

  <rollback>
    <!-- TODO: dropAllForeignKeyConstraints + dropIndex for each above -->
  </rollback>
</changeSet>
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/resources/db/changelog/changes/007-fks-and-indexes.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

  <changeSet id="007-fks-and-indexes" author="your-name">

    <addForeignKeyConstraint
        constraintName="fk_trades_instrument"
        baseTableName="trades"             baseColumnNames="instrument_id"
        referencedTableName="instruments"  referencedColumnNames="id"/>

    <addForeignKeyConstraint
        constraintName="fk_trades_counterparty"
        baseTableName="trades"               baseColumnNames="counterparty_id"
        referencedTableName="counterparties" referencedColumnNames="id"/>

    <createIndex tableName="trades" indexName="idx_trades_status">
      <column name="status"/>
    </createIndex>

    <createIndex tableName="settlements" indexName="idx_settlements_status">
      <column name="status"/>
    </createIndex>

    <rollback>
      <dropIndex tableName="settlements"  indexName="idx_settlements_status"/>
      <dropIndex tableName="trades"       indexName="idx_trades_status"/>
      <dropForeignKeyConstraint baseTableName="trades"
                                constraintName="fk_trades_counterparty"/>
      <dropForeignKeyConstraint baseTableName="trades"
                                constraintName="fk_trades_instrument"/>
    </rollback>
  </changeSet>
</databaseChangeLog>
```
</details>

---

> **Note — Liquibase wiring + run moved to Day 2 Sprint 3.** You author
> changesets today (`I003`–`I008` produce the 6 XML files). Tomorrow on
> Day 2 Sprint 3 you'll be taught Liquibase properly, then `I009` wires
> the master changelog and `I010` boots the migration. Today the
> instructor checkpoint is **XML file review**, not a green Liquibase
> run.

**🛑 Instructor checkpoint:** Before you move to Sprint 2, get the
instructor to review your schema (ER diagram + the 6 changeset XML
files in `db/changelog/changes/`). The XML doesn't *run* until tomorrow.

---

## Sprint 2 — Seed Data + Views  [45 min]

### TICKET-I011 — INSERT 5 instruments

**What**
- A changeset `008-seed-instruments.xml` with 5 `<insert>` rows covering at least 3 asset classes (e.g. EQUITY, FIXED_INCOME, FX, COMMODITY, DERIVATIVE).

**Why**
- Without seed data Day 5's React dashboard shows empty tables and you can't tell whether the bug is the API or the empty DB. The 5 instruments are also what I013's 10 trades reference via `instrument_id`.

**Observe**
- After Day 2's run, `SELECT count(*) FROM instruments;` returns 5 and `SELECT DISTINCT asset_class FROM instruments;` lists at least 3 distinct values.

**Acceptance criteria:**
- [ ] 5 rows covering at least 3 asset classes (e.g. an equity, an FX pair,
  a bond, a commodity).
- [ ] Recognisable symbols (e.g. `SAP.DE`, `NVDA`, `EURUSD`, `BUND_10Y`, `GOLD`).
- [ ] Inserts run via Liquibase `<loadData>` OR `<sql>` changeset
  (preferred), OR plain INSERTs in `data.sql` (acceptable for dev only).

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- Liquibase `<insert>` is the production-shaped choice. `data.sql` runs only
  on the `dev` profile and silently skips on uat/prod.
- Cover at least 3 asset classes: e.g. EQUITY + FIXED_INCOME + FX.
- Symbols are short, recognisable: `SAP.DE`, `NVDA`, `EURUSD`, `XAU`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Three options for seeding via Liquibase, ranked by clarity:

1. **`<insert>` elements** (recommended for ≤ 10 rows): one per row, explicit,
   reviewable in a diff.
2. **`<sql>` block** (acceptable): one or more `INSERT INTO ...` statements.
3. **`<loadData>` + CSV** (overkill for 5 rows; better for >100).

Whichever you pick, do NOT mix with raw `data.sql` — that runs on a different
profile and the row counts won't match between dev and uat.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```xml
<changeSet id="008-seed-instruments" author="your-name">
  <insert tableName="instruments">
    <column name="symbol"      value="SAP.DE"/>
    <column name="name"        value="SAP SE"/>
    <column name="asset_class" value="EQUITY"/>
    <column name="currency"    value="EUR"/>
  </insert>
  <!-- TODO: 4 more inserts — at least 3 distinct asset_class values across them -->

  <rollback>
    <delete tableName="instruments"/>
  </rollback>
</changeSet>
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/resources/db/changelog/changes/008-seed-instruments.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

  <changeSet id="008-seed-instruments" author="your-name">
    <insert tableName="instruments">
      <column name="symbol"      value="SAP.DE"/>
      <column name="name"        value="SAP SE"/>
      <column name="asset_class" value="EQUITY"/>
      <column name="currency"    value="EUR"/>
    </insert>
    <insert tableName="instruments">
      <column name="symbol"      value="US10Y"/>
      <column name="name"        value="US 10-Year Treasury"/>
      <column name="asset_class" value="FIXED_INCOME"/>
      <column name="currency"    value="USD"/>
    </insert>
    <insert tableName="instruments">
      <column name="symbol"      value="EURUSD"/>
      <column name="name"        value="Euro / US Dollar"/>
      <column name="asset_class" value="FX"/>
      <column name="currency"    value="USD"/>
    </insert>
    <insert tableName="instruments">
      <column name="symbol"      value="XAU"/>
      <column name="name"        value="Spot Gold"/>
      <column name="asset_class" value="COMMODITY"/>
      <column name="currency"    value="USD"/>
    </insert>
    <insert tableName="instruments">
      <column name="symbol"      value="ESM6"/>
      <column name="name"        value="S&amp;P 500 E-mini Futures (Jun 2026)"/>
      <column name="asset_class" value="DERIVATIVE"/>
      <column name="currency"    value="USD"/>
    </insert>

    <rollback>
      <delete tableName="instruments"/>
    </rollback>
  </changeSet>
</databaseChangeLog>
```
</details>

**Files to touch:** `backend/src/main/resources/db/changelog/changes/008-seed-instruments.xml`.

---

### TICKET-I012 — INSERT 4 counterparties

**What**
- A changeset `009-seed-counterparties.xml` with 4 `<insert>` rows using real public LEI codes (Goldman, Morgan Stanley, Citi, Nomura) spanning at least 2 regions.

**Why**
- Real LEIs (not `AAAA00000000000000000`) are what students will actually see on Day 7's API contracts and Day 8's recon engine fixtures. Wrong-shape data here means re-seeding mid-fortnight.

**Observe**
- `SELECT region, count(*) FROM counterparties GROUP BY region;` returns at least 2 distinct regions. XML ampersand "Morgan Stanley & Co." is escaped as `&amp;` — if not, Liquibase fails parsing with `The reference to entity "Co" must end with the ';' delimiter`.

**Acceptance criteria:**
- [ ] 4 rows with real LEI codes (public reference data, see below).
- [ ] Region distribution covers at least 2 regions.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Use the 4 real public LEIs in the table below. At least 2 regions must be
covered (EMEA + NAMR + APAC = easy).

| Counterparty                        | LEI                    | Region |
|-------------------------------------|------------------------|--------|
| Goldman Sachs International         | `W22LROWP2IHZNBB6K528` | EMEA   |
| Morgan Stanley & Co.                | `I7331LVKQX1L8TUI8447` | NAMR   |
| Citigroup Global Markets Limited    | `XKZZ2JZF41MRHTR1V493` | EMEA   |
| Nomura International PLC            | `DGQCSV2PHVF7I2743539` | APAC   |
</details>

<details>
<summary>Hint 2 — More guided</summary>

Same shape as the instruments seed (I011). Use `<insert>` elements (not
`<sql>` — the values you're inserting are static, not derived).

Four counterparties, two regions minimum — you can pick any 4 from the LEI
table above. The region CHECK from I005 fires on insert, so if your CHECK
isn't right, this changeset will fail loudly.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```xml
<changeSet id="009-seed-counterparties" author="your-name">
  <insert tableName="counterparties">
    <column name="name"     value="Goldman Sachs International"/>
    <column name="lei_code" value="W22LROWP2IHZNBB6K528"/>
    <column name="region"   value="EMEA"/>
  </insert>
  <!-- TODO: 3 more counterparties from the LEI table -->

  <rollback>
    <delete tableName="counterparties"/>
  </rollback>
</changeSet>
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/resources/db/changelog/changes/009-seed-counterparties.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

  <changeSet id="009-seed-counterparties" author="your-name">
    <insert tableName="counterparties">
      <column name="name"     value="Goldman Sachs International"/>
      <column name="lei_code" value="W22LROWP2IHZNBB6K528"/>
      <column name="region"   value="EMEA"/>
    </insert>
    <insert tableName="counterparties">
      <column name="name"     value="Morgan Stanley &amp; Co."/>
      <column name="lei_code" value="I7331LVKQX1L8TUI8447"/>
      <column name="region"   value="NAMR"/>
    </insert>
    <insert tableName="counterparties">
      <column name="name"     value="Nomura International PLC"/>
      <column name="lei_code" value="DGQCSV2PHVF7I2743539"/>
      <column name="region"   value="APAC"/>
    </insert>
    <insert tableName="counterparties">
      <column name="name"     value="Citigroup Global Markets Limited"/>
      <column name="lei_code" value="XKZZ2JZF41MRHTR1V493"/>
      <column name="region"   value="EMEA"/>
    </insert>

    <rollback>
      <delete tableName="counterparties"/>
    </rollback>
  </changeSet>
</databaseChangeLog>
```
</details>

---

### TICKET-I013 — INSERT 10 trades (mixed statuses)

**What**
- A changeset `010-seed-trades.xml` with 10 `<insert>` rows hitting each of the 5 status values (`PENDING`, `MATCHED`, `UNMATCHED`, `DISPUTED`, `CANCELLED`) at least once, spread across multiple `trade_date` values.

**Why**
- The Day-8 recon engine and Day-6 dashboard both filter by `status`; if every seeded trade is `PENDING` you can't tell those filters work. Mixed dates are what makes `v_settlement_lag` produce non-zero output later.

**Observe**
- After Day 2's run, `SELECT status, count(*) FROM trades GROUP BY status;` returns 5 rows. Liquibase `<column>` uses `valueNumeric="..."` for `instrument_id`/`counterparty_id`/`quantity`/`price` and `valueDate="..."` for `trade_date` — `value=` on these will silently insert nulls or fail the NOT NULL constraint.

**Acceptance criteria:**
- [ ] 10 trades referencing the seeded instruments + counterparties.
- [ ] At least one trade per `status` value: `PENDING`, `MATCHED`,
  `UNMATCHED`, `DISPUTED`, `CANCELLED`.
- [ ] `trade_date` spread across the last 30 days.
- [ ] Reasonable `quantity` + `price` (no `1e-9` test data).

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- Pre-generate `trade_ref` values like `TRD-2026-0001` … `TRD-2026-0010`.
- Verify with `SELECT status, count(*) FROM trades GROUP BY status;` —
  every status should appear at least once.
- `valueNumeric=` for `instrument_id`, `counterparty_id`, `quantity`, `price`.
- `valueDate=` for `trade_date` (Liquibase wants ISO `YYYY-MM-DD`).
</details>

<details>
<summary>Hint 2 — More guided</summary>

Distribute across the matrix:
- 5 distinct status values × 10 trades → at least 1 per status, with 5 left to
  vary across instruments / counterparties.
- 10 trades / 5 instruments = avg 2 trades per instrument.
- 10 trades / 4 counterparties = uneven — that's realistic.
- Spread `trade_date` across multiple days so the v_settlement_lag view
  produces interesting output.

`instrument_id` and `counterparty_id` are the IDs auto-generated by I011/I012.
With `BIGINT autoIncrement`, the first instrument is `id=1`, second is `id=2`,
etc. So referencing `instrument_id=1` means "the first row in your seed".
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```xml
<changeSet id="010-seed-trades" author="your-name">
  <insert tableName="trades">
    <column name="trade_ref"      value="TRD-2026-0001"/>
    <column name="instrument_id"  valueNumeric="1"/>
    <column name="counterparty_id" valueNumeric="1"/>
    <column name="quantity"       valueNumeric="1000.0000"/>
    <column name="price"          valueNumeric="245.5000"/>
    <column name="trade_date"     valueDate="2026-03-01"/>
    <column name="status"         value="MATCHED"/>
  </insert>
  <!-- TODO: 9 more trades. Cover all 5 statuses across them.
       Vary instrument_id (1-5) and counterparty_id (1-4). -->

  <rollback>
    <delete tableName="trades"/>
  </rollback>
</changeSet>
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/resources/db/changelog/changes/010-seed-trades.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

  <changeSet id="010-seed-trades" author="your-name">
    <insert tableName="trades"><column name="trade_ref" value="TRD-2026-0001"/><column name="instrument_id" valueNumeric="1"/><column name="counterparty_id" valueNumeric="1"/><column name="quantity" valueNumeric="1000.0000"/><column name="price" valueNumeric="245.5000"/><column name="trade_date" valueDate="2026-03-01"/><column name="status" value="MATCHED"/></insert>
    <insert tableName="trades"><column name="trade_ref" value="TRD-2026-0002"/><column name="instrument_id" valueNumeric="1"/><column name="counterparty_id" valueNumeric="2"/><column name="quantity" valueNumeric="500.0000"/><column name="price" valueNumeric="246.0000"/><column name="trade_date" valueDate="2026-03-01"/><column name="status" value="UNMATCHED"/></insert>
    <insert tableName="trades"><column name="trade_ref" value="TRD-2026-0003"/><column name="instrument_id" valueNumeric="2"/><column name="counterparty_id" valueNumeric="1"/><column name="quantity" valueNumeric="100000.0000"/><column name="price" valueNumeric="99.5000"/><column name="trade_date" valueDate="2026-03-02"/><column name="status" value="MATCHED"/></insert>
    <insert tableName="trades"><column name="trade_ref" value="TRD-2026-0004"/><column name="instrument_id" valueNumeric="3"/><column name="counterparty_id" valueNumeric="3"/><column name="quantity" valueNumeric="50000.0000"/><column name="price" valueNumeric="1.0850"/><column name="trade_date" valueDate="2026-03-02"/><column name="status" value="PENDING"/></insert>
    <insert tableName="trades"><column name="trade_ref" value="TRD-2026-0005"/><column name="instrument_id" valueNumeric="4"/><column name="counterparty_id" valueNumeric="4"/><column name="quantity" valueNumeric="10.0000"/><column name="price" valueNumeric="2125.7500"/><column name="trade_date" valueDate="2026-03-03"/><column name="status" value="DISPUTED"/></insert>
    <insert tableName="trades"><column name="trade_ref" value="TRD-2026-0006"/><column name="instrument_id" valueNumeric="5"/><column name="counterparty_id" valueNumeric="1"/><column name="quantity" valueNumeric="25.0000"/><column name="price" valueNumeric="78.4000"/><column name="trade_date" valueDate="2026-03-03"/><column name="status" value="MATCHED"/></insert>
    <insert tableName="trades"><column name="trade_ref" value="TRD-2026-0007"/><column name="instrument_id" valueNumeric="1"/><column name="counterparty_id" valueNumeric="2"/><column name="quantity" valueNumeric="750.0000"/><column name="price" valueNumeric="245.7500"/><column name="trade_date" valueDate="2026-03-04"/><column name="status" value="PENDING"/></insert>
    <insert tableName="trades"><column name="trade_ref" value="TRD-2026-0008"/><column name="instrument_id" valueNumeric="2"/><column name="counterparty_id" valueNumeric="3"/><column name="quantity" valueNumeric="50000.0000"/><column name="price" valueNumeric="99.7500"/><column name="trade_date" valueDate="2026-03-04"/><column name="status" value="CANCELLED"/></insert>
    <insert tableName="trades"><column name="trade_ref" value="TRD-2026-0009"/><column name="instrument_id" valueNumeric="3"/><column name="counterparty_id" valueNumeric="4"/><column name="quantity" valueNumeric="25000.0000"/><column name="price" valueNumeric="1.0860"/><column name="trade_date" valueDate="2026-03-05"/><column name="status" value="UNMATCHED"/></insert>
    <insert tableName="trades"><column name="trade_ref" value="TRD-2026-0010"/><column name="instrument_id" valueNumeric="4"/><column name="counterparty_id" valueNumeric="2"/><column name="quantity" valueNumeric="5.0000"/><column name="price" valueNumeric="2130.0000"/><column name="trade_date" valueDate="2026-03-05"/><column name="status" value="DISPUTED"/></insert>

    <rollback>
      <delete tableName="trades"/>
    </rollback>
  </changeSet>
</databaseChangeLog>
```
</details>

---

### TICKET-I014 — Two SQL views

**What**
- A changeset `011-create-views.xml` with two `<createView replaceIfExists="true">` blocks — `v_open_breaks` (3-way JOIN, filter `status='OPEN'`) and `v_settlement_lag` (2-way JOIN, date arithmetic, filter `status='SETTLED'`).

**Why**
- Views give the Day-5 controllers a stable read shape independent of underlying schema churn — if a column gets renamed, the view absorbs the change. The Day-6 React dashboard reads from `v_open_breaks` directly.

**Observe**
- After Day 2's run, `SELECT * FROM v_open_breaks;` and `SELECT * FROM v_settlement_lag;` both execute without error (row counts may be 0 if `recon_breaks` / `settlements` aren't seeded — empty is fine, exception is not).

**Acceptance criteria:**
- [ ] `v_open_breaks`: every `recon_breaks` row where `status='OPEN'`,
  JOINed onto `trades` to surface `trade_ref`, and the counterparty name.
- [ ] `v_settlement_lag`: for every `SETTLED` settlement, exposes
  `trade_id`, `trade_date`, `settled_at`, and lag (settled_at − trade_date
  expressed in hours OR days).
- [ ] Both created via Liquibase changeset (`009-create-views.xml`), with
  rollback dropping the views.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Two views, two `<createView>` elements, one changeset (or one per view —
either way works).

- `v_open_breaks` = "which breaks need attention right now". 3-way JOIN.
- `v_settlement_lag` = "how long do settlements take". 2-way JOIN + a date
  arithmetic expression.

`<createView replaceIfExists="true">` is the safe pattern — Liquibase will
re-create on every run instead of erroring on conflict.
</details>

<details>
<summary>Hint 2 — More guided</summary>

**v_open_breaks** needs to expose enough for an ops user to glance and act:
- `break_id` (rb.id) — primary key for the resolve action
- `discrepancy_type` — what kind of break
- `trade_ref` — find the trade fast
- `counterparty` name — who's on the other side
- (Stretch) `instrument` symbol — what was traded

Filter: `WHERE rb.status = 'OPEN'`.

**v_settlement_lag** — only meaningful for SETTLED rows:
- `trade_id`, `trade_ref`, `trade_date`, `settled_at`
- `lag_hours` or `lag_days` — derive from `(settled_at - trade_date)`.

Filter: `WHERE s.status = 'SETTLED'`.

On H2 (dev profile) and Postgres (uat) both,
`EXTRACT(EPOCH FROM (s.settled_at - t.trade_date)) / 3600.0` gives lag in
hours. H2 needs PostgreSQL compatibility mode set in the JDBC URL — the
starter already does that.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```xml
<changeSet id="011-create-view-open-breaks" author="your-name">
  <createView viewName="v_open_breaks" replaceIfExists="true">
    SELECT
        rb.id              AS break_id,
        rb.discrepancy_type,
        t.trade_ref,
        cp.name            AS counterparty
    FROM recon_breaks rb
    JOIN trades t          ON t.id = rb.trade_id
    JOIN counterparties cp ON cp.id = t.counterparty_id
    WHERE rb.status = 'OPEN'
  </createView>
  <rollback>
    <dropView viewName="v_open_breaks"/>
  </rollback>
</changeSet>

<changeSet id="011-create-view-settlement-lag" author="your-name">
  <createView viewName="v_settlement_lag" replaceIfExists="true">
    <!-- TODO: SELECT trade_ref, trade_date, settled_at,
                       (settled_at - trade_date) AS lag_…
                FROM settlements JOIN trades …
                WHERE settlements.status='SETTLED' -->
  </createView>
  <rollback>
    <dropView viewName="v_settlement_lag"/>
  </rollback>
</changeSet>
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/resources/db/changelog/changes/011-create-views.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

  <changeSet id="011-create-view-open-breaks" author="your-name">
    <createView viewName="v_open_breaks" replaceIfExists="true">
      SELECT
          rb.id              AS break_id,
          rb.discrepancy_type,
          rb.created_at      AS detected_at,
          t.trade_ref,
          t.quantity,
          t.price,
          i.symbol           AS instrument,
          cp.name            AS counterparty,
          cp.region
      FROM recon_breaks rb
      JOIN trades t          ON t.id = rb.trade_id
      JOIN instruments i     ON i.id = t.instrument_id
      JOIN counterparties cp ON cp.id = t.counterparty_id
      WHERE rb.status = 'OPEN'
    </createView>
    <rollback>
      <dropView viewName="v_open_breaks"/>
    </rollback>
  </changeSet>

  <changeSet id="011-create-view-settlement-lag" author="your-name">
    <createView viewName="v_settlement_lag" replaceIfExists="true">
      SELECT
          t.trade_ref,
          t.trade_date,
          s.settled_at,
          (CAST(s.settled_at AS DATE) - t.trade_date) AS lag_days,
          s.status AS settlement_status
      FROM settlements s
      JOIN trades t ON t.id = s.trade_id
      WHERE s.status = 'SETTLED'
    </createView>
    <rollback>
      <dropView viewName="v_settlement_lag"/>
    </rollback>
  </changeSet>
</databaseChangeLog>
```
</details>

---

## Run and Observe — End of Sprint 2 (Seed + Views)

The tables now have data, and two views surface joined reads of that data.
Restart the backend and confirm what's changed since end of Sprint 1.

**Run:**

> **Terminal #1** — from `tradeflow-studentscopy/backend/`

```bash
# Ctrl+C then:
./mvnw spring-boot:run
```

Boot log should show 4 new `ChangeSet` lines for your seed (instruments,
counterparties, trades) and views changesets.

**Observe — H2 console:**

| Check | Expected after Sprint 2 |
|---|---|
| `SELECT count(*) FROM instruments;` | `5` |
| `SELECT count(*) FROM counterparties;` | `4` |
| `SELECT count(*) FROM trades;` | `10` |
| `SELECT status, count(*) FROM trades GROUP BY status;` | 5 rows — one per status value (PENDING/MATCHED/UNMATCHED/DISPUTED/CANCELLED) |
| `SELECT region, count(*) FROM counterparties GROUP BY region;` | At least 2 distinct regions |
| `SELECT * FROM v_open_breaks;` | View executes without error (may be 0 rows if you didn't seed `recon_breaks`) |
| `SELECT * FROM v_settlement_lag;` | View executes without error (likely 0 rows — needs `settlements` data) |

**Browser checks:**

- `http://localhost:8080/api/v1/trades` — still returns `[]` (the REST
  endpoint is a Day-5 ticket; data is in the DB but no controller wires it
  to JSON yet).

**Useful diagnostic query** — what Liquibase has applied:

```sql
SELECT id, filename, exectype
  FROM databasechangelog
 ORDER BY orderexecuted;
```

The row count should equal the number of `<include>` lines in your
`db.changelog-master.xml`. If it doesn't, an include is missing or
mis-pointed.

**If something looks wrong:** confirm each seed changeset is referenced
from the master changelog. The most common Sprint-2 mistake is forgetting
to add the `<include>` line for a new file.

---

## Sprint 3 — AI-Assisted SQL  [30 min lab]

### TICKET-I015 — AI-generated `audit_log` DDL

**What**
- A changeset `010-create-audit-log.xml` containing AI-generated DDL for `audit_log` that you have manually reviewed and fixed (at least one issue found), plus the prompt, raw AI output, issue, and fix in the PR description.

**Why**
- This is the programme's first AI-assisted ticket — the point isn't "have AI write the SQL", it's "learn to prompt + review". The Day-2 trigger lab and Day-9 audit-trail story both lean on this table.

**Observe**
- The PR description has four named sections: prompt, raw output, issue found, fix. A PR with only the final XML (no AI artefacts) fails the acceptance check. `chk_audit_log_operation` rejects `INSERT INTO audit_log (..., operation, ...) VALUES (..., 'X', ...);`.

**Acceptance criteria:**
- [ ] New file `010-create-audit-log.xml` containing the *reviewed* DDL.
- [ ] Table has columns at minimum: `id`, `entity`, `entity_id`,
  `action` ('INSERT'/'UPDATE'/'DELETE'), `old_value`, `new_value`,
  `event_time`, `user_name`.
- [ ] PR description includes:
  - The exact prompt you used.
  - The raw AI output (paste verbatim).
  - The issue you found (e.g. wrong column type, missing CHECK,
    `timestamp` as a column name, etc.).
  - Your fix.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

"audit_log table" is intentionally generic — push the AI to be specific.
Always paste the AI's raw output verbatim into the PR before you touch
anything; that's the "exhibit A" the instructor reviews. Then diff it
against what you would have written.

Column naming: the acceptance criteria uses generic names (`entity`, `action`,
`old_value`, `new_value`, `event_time`, `user_name`). The reference solution
uses bank-style names (`table_name`, `operation`, `row_pk`, `before_data`,
`after_data`, `changed_by`, `changed_at`). Either is fine — pick one and use
it consistently. The reference names let the Day-2 trigger lab work without
edits.
</details>

<details>
<summary>Hint 2 — More guided</summary>

**Use this AI prompt (or close):**

> *"Design an append-only audit_log table for a Postgres-backed trade
> reconciliation system. Track INSERT/UPDATE/DELETE on any entity. Store
> before/after payload as JSON. Include who and when. Output Liquibase
> XML, not plain SQL."*

**Common AI bugs to look for:**
- Uses `timestamp` as a column name (reserved word in some DBs).
- Uses `TEXT` instead of `JSONB` for old/new value (loses query-ability).
- Forgets to mark old/new value columns nullable (a fresh INSERT has no
  `old_value`; a DELETE has no `new_value`).
- Omits a default for `event_time` / `changed_at`.
- No CHECK on the `operation` / `action` column (any string would be allowed).
- No index on `(table_name, row_pk)` — lookups for "show me the history of
  trade #123" become full scans.

**The deliverable matters as much as the schema** — your PR must show the AI
prompt, the AI's raw output, the issue you found, and your fix. This is the
graded artefact.
</details>

<details>
<summary>Hint 3 — Workflow + code skeleton</summary>

### Workflow
1. Open Claude / ChatGPT / Gemini. Fresh chat.
2. Paste the prompt from Hint 2.
3. Copy the raw output into your PR draft *immediately* (before editing).
4. Diff the AI output against the skeleton below — find at least one issue.
5. Author your corrected XML in `db/changelog/changes/012-create-audit-log.xml`.
6. Wire into `db.changelog-master.xml`.
7. Restart backend, verify the table.

### Code skeleton

```xml
<changeSet id="012-create-audit-log" author="your-name">
  <createTable tableName="audit_log">
    <column name="id" type="BIGINT" autoIncrement="true">
      <constraints primaryKey="true" nullable="false"/>
    </column>
    <!-- TODO: table_name VARCHAR(64) NOT NULL -->
    <!-- TODO: operation CHAR(1) NOT NULL          (+CHECK 'I'/'U'/'D') -->
    <!-- TODO: row_pk BIGINT NOT NULL -->
    <!-- TODO: before_data JSONB     (nullable — INSERT has no before) -->
    <!-- TODO: after_data  JSONB     (nullable — DELETE has no after)  -->
    <!-- TODO: changed_by  VARCHAR(50) NOT NULL default CURRENT_USER -->
    <!-- TODO: changed_at  TIMESTAMP WITH TIME ZONE NOT NULL default CURRENT_TIMESTAMP -->
  </createTable>
  <!-- TODO: CHECK on operation -->
  <!-- TODO: index on (table_name, row_pk) -->
  <!-- TODO: index on (changed_at)         -->
</changeSet>
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/resources/db/changelog/changes/012-create-audit-log.xml`

Column naming matches the reference implementation (so the Day-2 trigger lab
works out of the box). If you adopted the generic naming from the acceptance
criteria, rename accordingly.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

  <changeSet id="012-create-audit-log" author="your-name">
    <createTable tableName="audit_log">
      <column name="id" type="BIGINT" autoIncrement="true">
        <constraints primaryKey="true" nullable="false"/>
      </column>
      <column name="table_name" type="VARCHAR(64)">
        <constraints nullable="false"/>
      </column>
      <column name="operation" type="CHAR(1)">
        <constraints nullable="false"/>
      </column>
      <column name="row_pk" type="BIGINT">
        <constraints nullable="false"/>
      </column>
      <column name="before_data" type="JSONB"/>
      <column name="after_data"  type="JSONB"/>
      <column name="changed_by" type="VARCHAR(50)"
              defaultValueComputed="CURRENT_USER">
        <constraints nullable="false"/>
      </column>
      <column name="changed_at" type="TIMESTAMP WITH TIME ZONE"
              defaultValueComputed="CURRENT_TIMESTAMP">
        <constraints nullable="false"/>
      </column>
    </createTable>

    <sql>
      ALTER TABLE audit_log ADD CONSTRAINT chk_audit_log_operation
          CHECK (operation IN ('I','U','D'));
    </sql>

    <createIndex tableName="audit_log" indexName="idx_audit_log_table_pk">
      <column name="table_name"/>
      <column name="row_pk"/>
    </createIndex>

    <createIndex tableName="audit_log" indexName="idx_audit_log_changed_at">
      <column name="changed_at"/>
    </createIndex>

    <rollback>
      <dropTable tableName="audit_log"/>
    </rollback>
  </changeSet>
</databaseChangeLog>
```
</details>

---

## Run and Observe — End of Sprint 3 (AI-assisted `audit_log`)

The `audit_log` table now exists. Restart and confirm.

**Run:**

> **Terminal #1** — from `tradeflow-studentscopy/backend/`

```bash
# Ctrl+C then:
./mvnw spring-boot:run
```

Boot log should show one new `ChangeSet` line for your audit_log changeset.

**Observe — H2 console:**

| Check | Expected after Sprint 3 |
|---|---|
| `SHOW TABLES;` | 6 business tables now (added: AUDIT_LOG) |
| `SELECT column_name, data_type FROM information_schema.columns WHERE table_name='AUDIT_LOG' ORDER BY ordinal_position;` | 8 columns matching your changeset spec |
| `SELECT * FROM information_schema.table_constraints WHERE table_name='AUDIT_LOG' AND constraint_type='CHECK';` | The `chk_audit_log_operation` CHECK is registered |
| `SELECT * FROM information_schema.indexes WHERE table_name='AUDIT_LOG';` | Both indexes you wrote (`idx_audit_log_table_pk`, `idx_audit_log_changed_at`) |

**Negative test — prove your operation CHECK fires:**

```sql
INSERT INTO audit_log (table_name, operation, row_pk)
VALUES ('trades', 'X', 1);
-- Expected: Check constraint violation: "CHK_AUDIT_LOG_OPERATION"
```

**(Stretch — Postgres only) Test the trigger if you shipped it:**

Switch to the `uat` profile (`SPRING_PROFILES_ACTIVE=uat ./mvnw
spring-boot:run` against Docker Postgres) and insert a row into `trades`
manually. A new row should land in `audit_log` automatically.

**End-of-day cross-check:**

```sql
-- Total changesets applied
SELECT count(*) FROM databasechangelog;
-- Should equal: 1 (smoke) + 5 (tables) + 1 (FKs/indexes) + 3 (seed) + 1 (views) + 1 (audit_log) = 12
```

If that number is lower, an `<include>` is missing from your master
changelog — see end-of-day checklist below.

---

## Optional / Stretch

- **Stretch goal:** Add a trigger on `trades` that auto-inserts into
  `audit_log` for every `INSERT`/`UPDATE`/`DELETE`.
- **Mini-challenge:** Write a recursive CTE that traces a trade through
  `trades → settlements → recon_breaks → resolution`.

---

## End-of-day checklist

- [ ] 13 tickets merged into `develop` (`I001`–`I008`, `I011`–`I015`).
- [ ] All Liquibase **changeset XML files** authored and committed
  — 5 tables + FKs/indexes + seed + views + `audit_log`.
- [ ] ER diagram + AI-lab artefacts (prompt + fix) committed.
- [ ] Instructor signed off on the XML files (no boot/run today).
- [ ] *(Tomorrow Day 2 Sprint 3:* `I009` + `I010` wire + run + verify the changesets against H2.*)*

Next: [Day 2 — Java OOP fundamentals](../day2/README.md)
