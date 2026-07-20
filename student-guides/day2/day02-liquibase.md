# Day 2 — Additional Topic: Liquibase in Spring Boot

> **Format:** 30 min teach + 30 min lab.
> **Goal:** Understand *how* Liquibase plugs into Spring Boot so that when Day 4
> lights up the full backend, the schema management story is already familiar.
> **No ticket to ship today** — this is a guided walkthrough, not a graded sprint.

---

## 1. Why are we doing this?

### The problem

On Day 1 you ran Liquibase **from the command line** via `liquibase update`. That
works for a solo developer poking at a local DB, but in a real Spring Boot
service the schema needs to be in sync with the running application code at
*every* boot — on your laptop, on UAT, on prod, on a teammate's machine, inside
a Docker container, inside CI.

If you forget to run `liquibase update` before starting the app, your code expects
a `settlements` table that doesn't exist yet and the first query blows up.

### The fix

Spring Boot can run Liquibase **automatically** as part of application startup —
*before* the JPA layer comes up, *before* the first HTTP request lands. Same
changelogs, same XML, but now you can never forget to apply them.

The startup sequence becomes:

```
Spring Boot starts
  ↓
DataSource is constructed (talks to Postgres / H2)
  ↓
Liquibase auto-configuration sees the DataSource + a changelog on the classpath
  ↓
Liquibase runs every un-applied changeset
  ↓
JPA / Hibernate boots on top of the now-current schema
  ↓
Web layer comes up — app is ready
```

If any changeset fails, the application **refuses to start**. That's the point —
you want a broken migration to surface at boot, not at 03:00 when a query hits
a missing column.

---

## 2. The four pieces of the wiring

Liquibase-in-Spring-Boot has exactly four moving parts. Everything else is detail.

| # | Piece | Lives in | Job |
|---|---|---|---|
| 1 | Liquibase dependency | `backend/pom.xml` | Put the Liquibase JARs + Spring Boot auto-config on the classpath. |
| 2 | DataSource | `application-dev.yml` (or `-uat`, `-prod`) | Tell Spring Boot which DB to connect to. Liquibase reuses it. |
| 3 | Liquibase config | `application.yml` | Tell Liquibase **where the master changelog is** and that it's enabled. |
| 4 | Changelogs | `backend/src/main/resources/db/changelog/...` | The actual schema changes, in XML/YAML/SQL. |

Memorise that table. The rest of this doc is just zooming in on each one.

---

## 3. Step-by-step walkthrough

Open each file as you read each step. Don't paste — read what's already there
and understand *why* it's there.

### Step 1 — Confirm the dependency is on the classpath

**File:** `backend/pom.xml`

Look for this block (around line 82):

```xml
<!-- Day 2/4 — Liquibase -->
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

**What this does:** Pulls the Liquibase library onto the classpath. Spring Boot's
auto-configuration ships a class called `LiquibaseAutoConfiguration` that
**activates automatically** whenever it sees `liquibase-core` *and* a configured
`DataSource`. No `@Configuration` class, no `@Bean` definitions — that's the
auto-config doing the work for you.

> **`liquibase-core` vs `spring-boot-starter-liquibase`** — you'll see both in
> tutorials. `spring-boot-starter-liquibase` is a "starter" artifact whose entire
> job is to pull in `liquibase-core`. In Spring Boot 3.x they behave identically.
> Our pom uses `liquibase-core` directly — that's fine, **don't change it.**

**No version tag?** Notice there's no `<version>`. The Spring Boot parent pom
(declared at the top of `pom.xml`) pins a Liquibase version that's known to be
compatible with this Boot version. Override it only if you have a very good reason.

---

### Step 2 — Confirm a DataSource exists

**File:** `backend/src/main/resources/application-dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:tradeflow;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
```

**What this does:** Sets up an in-memory **H2** database for the `dev` profile.
H2 is a Java DB that lives entirely in JVM memory — fast to boot, wiped on shutdown,
perfect for dev. `MODE=PostgreSQL` makes H2 accept Postgres-style SQL so your
changelogs don't need to fork between dev and prod.

**Why Liquibase cares:** Liquibase needs a `DataSource` to run anything. Spring
Boot constructs the `DataSource` from this config and hands it to Liquibase
auto-magically. The same wiring works in `application-uat.yml` and
`application-prod.yml` — only the URL/credentials change.

> **Note — for Day 1 + Day 2 right now**, the app isn't running yet (you're
> still writing Java POJOs). This config is here so that when Day 4/5 actually
> starts the Spring Boot app, the plumbing already works.

---

### Step 3 — Point Liquibase at the master changelog

**File:** `backend/src/main/resources/application.yml`

```yaml
spring:
  liquibase:
    enabled: true
    # Liquibase 4.24+ rejects the `classpath:` prefix here.
    # File is loaded from backend/src/main/resources/db/changelog/.
    change-log: db/changelog/db.changelog-master.xml
```

**What each line does:**

- `enabled: true` — explicit on/off switch. Leaving this out also defaults to
  `true`, but being explicit avoids the "where did Liquibase go?" hunt.
- `change-log: db/changelog/db.changelog-master.xml` — the **classpath-relative**
  path to the master changelog. Everything under `src/main/resources/` ends up on
  the classpath, so this resolves to that file.

> **Common bug to avoid.** Older guides will tell you to write
> `change-log: classpath:db/changelog/db.changelog-master.xml`. **Don't.**
> Liquibase 4.24+ (which Boot 3.2.4 pulls in) treats `classpath:` as part of the
> literal filename and crashes with `FileNotFoundException`. Drop the prefix —
> the path is classpath-resolved automatically here.

---

### Step 4 — Read the master changelog

**File:** `backend/src/main/resources/db/changelog/db.changelog-master.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
                            http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <!-- Day-1 bootstrap: all 5 tables + indexes -->
    <include file="db/changelog/changes/001-init.xml"/>

</databaseChangeLog>
```

**What this does:** A *master* changelog is a table of contents. It uses
`<include file="..."/>` lines to chain together the *real* changeset files in
the order they should be applied. Liquibase applies them top-to-bottom on first
run, then skips already-applied ones on subsequent runs (it remembers via the
`DATABASECHANGELOG` table it creates in your DB).

**Why split it up?** One huge changelog file is unreadable and merge-conflict-prone.
By splitting `001-init.xml`, `002-add-checks.xml`, `003-audit-log.xml`, ... each
PR touches a *new* file rather than fighting over one growing file. The master
just chains them.

**Naming convention** (already declared in the existing comment block):
- `001-init.xml`, `002-<your-change>.xml`, `003-...` — zero-padded numbers keep
  alphabetical order = chronological order.
- Each file should have one logical change.

---

### Step 5 — Look at an actual changeset

**File:** `backend/src/main/resources/db/changelog/changes/001-init.xml`

This is the Day-1 bootstrap. A `<changeSet>` is the atomic unit Liquibase tracks:

```xml
<changeSet id="001-init-smoke" author="tradeflow">
    <tagDatabase tag="day1-bootstrap"/>
</changeSet>
```

**What this does:** Right now, `001-init.xml` is deliberately a **no-op smoke
test** — it just tags the database state. That's because Day 1 of the case study
asks **you** to write the real CREATE TABLE changesets. The wiring is already
proven (Liquibase runs and succeeds), and your Day-1 sprint tickets fill in the
actual schema.

**Anatomy of a `<changeSet>`:**
- `id` — unique within the file. Liquibase keys "have I applied this?" on
  `(id, author, filename)`.
- `author` — your handle. Useful in `git blame`-style situations.
- The body — one or more Liquibase change types (`createTable`, `addColumn`,
  `sql`, `tagDatabase`, ...).

---

## 4. Run it and watch it work

You won't have a runnable Spring Boot app until Day 4/5, so today the
**instructor demos** this part live. Watch the boot log when they run
`./mvnw spring-boot:run` — you should see lines like:

```
INFO  liquibase.lockservice : Successfully acquired change log lock
INFO  liquibase.changelog   : Reading from PUBLIC.DATABASECHANGELOG
INFO  liquibase.changelog   : ChangeSet db/changelog/changes/001-init.xml::001-init-smoke::tradeflow ran successfully in 4ms
INFO  liquibase.lockservice : Successfully released change log lock
```

That's the four pieces wired together: classpath dependency → datasource →
master changelog → first changeset applied.

**If the app crashes at boot**, 95% of the time it's one of the three pitfalls
below.

---

## 5. The three pitfalls (commit these to memory)

### Pitfall 1 — `classpath:` prefix on `change-log`

```yaml
# WRONG (Liquibase 4.24+):
spring:
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.xml

# RIGHT:
spring:
  liquibase:
    change-log: db/changelog/db.changelog-master.xml
```

The path is already classpath-relative in this property. The literal string
`classpath:` becomes part of the filename and Liquibase 404s.

### Pitfall 2 — Empty `data.sql` crashes Spring Boot's SQL initializer

If you create `src/main/resources/data.sql` and leave it empty (or comment out
all its lines), Spring Boot's SQL script runner throws on the empty resource.
That's why the starter ships `data.sql` containing the single placeholder line:

```sql
SELECT 1;
```

Don't delete that line until you actually have seed data to put there
(Day 5 / TICKET-I066).

### Pitfall 3 — Circular bean dependency

If you ever see this in the boot log:

```
The dependencies of some of the beans in the application context form a cycle:
liquibase ↔ entityManagerFactory
```

…it usually means someone added `spring.jpa.defer-datasource-initialization: true`
to make `data.sql` run after Liquibase. **Don't add that property.** Spring Boot
3.x already runs `data.sql` after Liquibase by default, and the `defer-` flag
creates the cycle. The note in `application-dev.yml` calls this out — leave it
as-is.

---

## 6. Lab exercise (30 min, no ticket)

Work in pairs. The first two tasks are read-only — they build the mental model.
Task 3 is the hands-on: you write a complete changeset, boot the app, verify
it in the H2 console, then clean up. Tasks 4 + 5 lock in the learning.

### Task 1 — Trace the four pieces (5 min, read-only)

Open all four files from §2 in split panes: `pom.xml`, `application.yml`,
`application-dev.yml`, `db.changelog-master.xml`. Find the four pieces from §2
and screenshot them for your team's notes.

### Task 2 — Predict the boot sequence (5 min, whiteboard)

On a whiteboard or piece of paper, draw the startup arrow: which of the four
pieces is read first, second, third? Where does the `DATABASECHANGELOG` table
get created? Compare with the diagram in §1 — did you get it right?

### Task 3 — Write a real changeset, boot, verify, roll back (15 min, hands-on)

This is the rep that locks the workflow in. You'll create a throwaway
`lab_demo` table — it doesn't collide with the Day-1 schema tickets, and you
delete it at the end so the repo stays clean.

**Step A — write the changeset.**

> **File to create:** `backend/src/main/resources/db/changelog/changes/002-lab-demo.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

  <changeSet id="002-lab-demo" author="your-name">
    <createTable tableName="lab_demo">
      <column name="id" type="BIGINT" autoIncrement="true">
        <constraints primaryKey="true" nullable="false"/>
      </column>
      <column name="note" type="VARCHAR(100)">
        <constraints nullable="false"/>
      </column>
      <column name="created_at" type="TIMESTAMP"
              defaultValueComputed="CURRENT_TIMESTAMP">
        <constraints nullable="false"/>
      </column>
    </createTable>

    <insert tableName="lab_demo">
      <column name="note" value="Liquibase lab — hello from your-name"/>
    </insert>

    <rollback>
      <dropTable tableName="lab_demo"/>
    </rollback>
  </changeSet>
</databaseChangeLog>
```

**Step B — wire it into the master.**

> **File to edit:** `backend/src/main/resources/db/changelog/db.changelog-master.xml`

Add this ONE line below the existing `<include file="...001-init.xml"/>`:

```xml
<include file="db/changelog/changes/002-lab-demo.xml"/>
```

**Step C — boot the backend.**

> **Terminal** — from `tradeflow-studentscopy/backend/`

```bash
./mvnw spring-boot:run
```

In the startup log, look for this line:

```
liquibase.changelog : ChangeSet db/changelog/changes/002-lab-demo.xml::002-lab-demo::your-name ran successfully in NNms
```

If you don't see it — or the app crashed at boot — jump to §8. Your error
message will match one of the nine common failures listed there.

**Step D — verify in the H2 console.**

Open `http://localhost:8080/h2-console` in a browser. Connect with:

- JDBC URL: `jdbc:h2:mem:tradeflow`
- User: `sa`
- Password: (blank)

Run these two queries:

```sql
-- 1. Your table exists and has the seeded row.
SELECT * FROM lab_demo;
-- → 1 row: id=1, note='Liquibase lab — hello from your-name', created_at=now

-- 2. Liquibase recorded it.
SELECT id, author, exectype, filename
  FROM databasechangelog
 WHERE filename LIKE '%lab-demo%';
-- → 1 row: id='002-lab-demo', author='your-name', exectype='EXECUTED'
```

Screenshot both results for your team's notes. The `EXECUTED` row in
`databasechangelog` is the muscle memory you want — that row is how Liquibase
remembers it already ran your changeset and won't try to apply it again on
the next boot.

**Step E — clean up.**

Two paths depending on which DB you targeted:

- **Default (H2 in-memory, `dev` profile):** just stop the app (`Ctrl+C` in
  Terminal). H2 lives in JVM memory — table + `databasechangelog` row both
  vanish. Then delete `002-lab-demo.xml` and remove the `<include>` line you
  added in Step B. Confirm with `git status` that only those two changes
  remain, then `git checkout` them to clean up:

  ```bash
  rm backend/src/main/resources/db/changelog/changes/002-lab-demo.xml
  git checkout backend/src/main/resources/db/changelog/db.changelog-master.xml
  ```

- **If you ran against Postgres (`uat` profile):** you must roll back FIRST,
  then delete the file. Deleting first and re-booting will hit error §8.4
  (the `databasechangelog` row references a file that no longer exists).

  ```bash
  cd backend
  ./mvnw liquibase:rollback -Dliquibase.rollbackCount=1
  ```

  Watch the log for `Rolled back changeSet:001-...`. Then delete the file
  and revert the include line as above. Verify in pgAdmin: `lab_demo` is
  gone and `databasechangelog` no longer references it.

**What you should be able to explain after Task 3:**

- Why the `<rollback>` block matters (the Postgres path doesn't work without it).
- Why deleting `002-lab-demo.xml` *without* rolling back on Postgres would
  later trigger error §8.4 on the next boot.
- What's stored in `databasechangelog` and why Liquibase trusts it on every boot.

### Task 4 — Pitfall scavenger hunt (3 min)

Run these and explain what you find:

```bash
# Confirm zero classpath: prefixes on Liquibase paths.
grep -rn "classpath:" backend/src/main/resources/application*.yml

# Find the defer-datasource-initialization warning in application-dev.yml.
grep -n "defer-datasource" backend/src/main/resources/application-dev.yml
```

Read the surrounding comment — be ready to explain *why* `defer-datasource-
initialization: true` is a footgun (see §5 Pitfall 3).

### Task 5 — Out-loud check (2 min, pairs)

Take turns explaining: "When `./mvnw spring-boot:run` starts in the `dev`
profile, here's exactly what happens to the database, in order…". Aim for
~90 seconds, no notes.

---

## 7. What you should be able to answer at end of lab

- Where does Liquibase get its `DataSource` from?
- What does the master changelog *contain* — and what does it *not* contain?
- Why is `001-init.xml` a no-op `<tagDatabase>` right now?
- Why must you avoid the `classpath:` prefix on `spring.liquibase.change-log`?
- What does Spring Boot do if a changeset fails at startup?

If you can answer all five, you're set for the full Spring Boot bootstrap on
Day 4. If any are fuzzy, re-read the relevant section before tomorrow.

---

## 8. Common mistakes and errors

A field guide to the failures you (or a teammate) will hit at least once.
When the boot log lights up red, match the error against this list **before**
asking the instructor.

### 8.1 `FileNotFoundException: db/changelog/db.changelog-master.xml`

**You'll see:**
```
liquibase.exception.ChangeLogParseException:
  java.io.FileNotFoundException: classpath:db/changelog/db.changelog-master.xml
```

**Cause:** You wrote `change-log: classpath:db/...` in `application.yml`.
Liquibase 4.24+ treats `classpath:` as part of the filename.

**Fix:** Drop the `classpath:` prefix. See §5, Pitfall 1.

---

### 8.2 `The dependencies of some of the beans … form a cycle: liquibase ↔ entityManagerFactory`

**You'll see:**
```
APPLICATION FAILED TO START
The dependencies of some of the beans in the application context form a cycle:
   liquibase
      ↓
   entityManagerFactory
      ↑
   dataSourceScriptDatabaseInitializer
```

**Cause:** Someone added `spring.jpa.defer-datasource-initialization: true`
or `spring.sql.init.mode: always` without understanding the ordering.

**Fix:** Remove `defer-datasource-initialization`. Spring Boot 3 runs `data.sql`
*after* Liquibase by default — you don't need the flag. See §5, Pitfall 3.

---

### 8.3 `script statement is empty` on `data.sql`

**You'll see:**
```
org.springframework.jdbc.datasource.init.ScriptStatementFailedException:
  Failed to execute SQL script statement #1 of class path resource [data.sql]:
  ; nested exception: ... script statement is empty
```

**Cause:** `src/main/resources/data.sql` exists but is empty (or fully
commented out). Spring Boot's SQL initializer can't parse "nothing".

**Fix:** Leave the placeholder `SELECT 1;` line in `data.sql` until you have
real seed SQL to put there. See §5, Pitfall 2.

---

### 8.4 `ValidationFailedException: Validation Failed: 1 changesets have changed since they were ran`

**You'll see:**
```
liquibase.exception.ValidationFailedException:
  Validation Failed:
    1 changesets have changed since they were ran against the database
```

**Cause:** You edited a changeset *after* it had already been applied. Liquibase
stores a checksum of every applied changeset in `DATABASECHANGELOG` and refuses
to start if the file no longer matches.

**Fix (in dev, H2 in-memory):** Just restart — H2 is wiped between boots, so the
checksum is recomputed.

**Fix (in dev, Postgres):** Either (a) revert your edit and write a *new*
changeset instead, or (b) run `liquibase clear-checksums` and let it
re-checksum on next boot. **Option (a) is the right habit** — applied
changesets are immutable; changes go in new files.

**Never do this in UAT/prod.** If you need to fix an applied changeset there,
you write a corrective new changeset.

---

### 8.5 `Table "DATABASECHANGELOG" not found` / Liquibase silently doesn't run

**You'll see:** Boot succeeds, but tables you expect are missing. No
`liquibase.changelog: ChangeSet … ran successfully` lines in the boot log.

**Cause:** Usually one of:
- `spring.liquibase.enabled: false` somewhere (check all profile files).
- `liquibase-core` got commented out of `pom.xml`.
- `change-log:` path is wrong — typo or pointing at a file that doesn't exist.

**Fix:** Grep:
```bash
grep -rn "liquibase" backend/src/main/resources/
grep -n "liquibase-core" backend/pom.xml
```
Confirm the dependency is in, `enabled: true`, and the path resolves.

---

### 8.6 `Cannot find changelog file: db/changelog/changes/00X-yourfile.xml`

**You'll see:**
```
liquibase.exception.ChangeLogParseException:
  Cannot find changelog file: db/changelog/changes/002-add-checks.xml
```

**Cause:** The master changelog `<include>`s a file that doesn't exist on the
classpath. Often a typo, or the file was created outside
`src/main/resources/db/changelog/changes/`.

**Fix:** Check the path is exactly relative to `src/main/resources/`. The
`<include file="...">` value is classpath-relative, **not** filesystem-relative
to the master changelog.

---

### 8.7 Duplicate changeset id

**You'll see:**
```
liquibase.exception.DatabaseException:
  ChangeSet 002-add-checks::add-trade-checks::tradeflow ran in a different way
```
or
```
Duplicate id+author+file: 002-add-checks::add-checks::tradeflow
```

**Cause:** Copy-pasted a `<changeSet>` block and forgot to change the `id`, or
two team-mates pushed the same `id` in different PRs.

**Fix:** Every `<changeSet id="...">` must be unique within its file. Easiest
convention: prefix the id with the file number, e.g. `id="002-add-trade-checks"`
inside `002-add-trade-checks.xml`.

---

### 8.8 Schema is right but seed data is missing

**You'll see:** Tables exist, but `SELECT * FROM trades` returns 0 rows in dev
even though `data.sql` has `INSERT` statements.

**Cause:** `spring.sql.init.mode` defaults to `embedded` — H2 yes, Postgres no.
If you're on the `uat` profile with Postgres, `data.sql` won't run unless you
set `spring.sql.init.mode: always`.

**Fix:** Set the mode explicitly per profile. Note: in real environments seed
data usually belongs in a *Liquibase* changeset (so it's tracked and ordered),
not in `data.sql`. `data.sql` is a dev-convenience shortcut only.

---

### 8.9 Mental-model mistakes (not errors, but bite you later)

- **"I'll just edit `001-init.xml`."** No — once it's been applied (even on your
  laptop), edit it and you'll hit §8.4 forever. New changes go in new files.
- **"Liquibase is just for the schema."** It's for any reproducible DB state
  change — tables, indexes, triggers, *and* reference data (currency codes,
  region codes). Use it for anything that must be the same on every environment.
- **"I'll run Liquibase manually before starting the app."** Defeats the whole
  point of this lab. The app *is* the runner now. If you find yourself doing
  this, something's wrong with your config — fix that instead.

---

**Back to:** [Day 2 README](./README.md) · **Next day:** [Day 3 — OOP patterns + SOLID](../day3/README.md)
