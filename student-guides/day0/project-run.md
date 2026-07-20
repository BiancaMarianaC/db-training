# How to Run TradeFlow

Step-by-step setup guide. Run these in order the **first time** you bring the
project up. Once everything is wired, daily use boils down to a single
`docker compose pull && docker compose up -d`.

The deploy model is **Option A**: GitHub Actions builds the images and
pushes them to GHCR; your laptop pulls and runs them. No local Maven /
Node build required.

> All paths are relative to the project root (the directory containing
> `docker-compose.yml`, `backend/`, `frontend/`, `student-guides/`).

---

## Prerequisites

> **Terminal**

```bash
java -version          # 17.x
node -v                # 20.x
docker --version       # 24.x+
docker compose version # v2.x+
gh --version           # optional but handy
```

Install whatever's missing before continuing.

---

## Platform notes (read once, save grief)

Most of this guide assumes a Unix-like shell (bash/zsh on macOS or Linux).
The two places where platform actually matters:

### Apple Silicon Mac (M1 / M2 / M3 / M4) — and Windows-on-ARM

The CI workflow builds **amd64** (Intel/AMD x86_64) images by default. Pulling
those on an arm64 host (Apple Silicon, Windows Surface Pro X, Raspberry Pi)
returns:

```
no matching manifest for linux/arm64/v8 in the manifest list entries
```

**Quick fix — tell Docker to ask for amd64 everywhere in this shell:**

```bash
# macOS / Linux:
export DOCKER_DEFAULT_PLATFORM=linux/amd64

# Windows PowerShell:
$env:DOCKER_DEFAULT_PLATFORM = "linux/amd64"
```

…then run `docker compose pull && docker compose up -d` as normal.
Containers will run via **Rosetta emulation** (or QEMU on Windows). JVM
startup is ~20-30% slower; fine for dev/demo, not for production.

**Make it permanent on macOS:** add the `export` line to `~/.zshrc`.
**On Windows:** Settings → System → About → Advanced system settings → Environment
Variables → add `DOCKER_DEFAULT_PLATFORM = linux/amd64` under User variables.

**Permanent right fix:** have CI build multi-arch images. See the
"Multi-arch builds" section at the bottom of this doc.

### Windows specifics

- **Use PowerShell**, not legacy `cmd`. PowerShell has Unicode + modern syntax.
- **Install Docker Desktop with the WSL 2 backend** (Settings → General →
  Use WSL 2 based engine). The Hyper-V backend is ~10× slower for I/O.
- Java users: use `mvnw.cmd`, not `mvnw`. Both are committed.
- **PowerShell command equivalents** for the bash snippets shown later:

  | Bash (shown elsewhere) | PowerShell equivalent |
  |---|---|
  | `cp .env.example .env` | `Copy-Item .env.example .env` |
  | `export FOO=bar` | `$env:FOO = "bar"` (current session only) |
  | `SPRING_PROFILES_ACTIVE=uat ./mvnw spring-boot:run` | `$env:SPRING_PROFILES_ACTIVE = "uat"; .\mvnw.cmd spring-boot:run` |
  | `./mvnw spring-boot:run` | `.\mvnw.cmd spring-boot:run` |
  | `echo "<PAT>" \| docker login ghcr.io -u sidoncode --password-stdin` | `"<PAT>" \| docker login ghcr.io -u sidoncode --password-stdin` |
  | `grep "Reconciling tradeRef"` | `Select-String "Reconciling tradeRef"` |
  | `docker compose ps`, `docker compose up -d`, `docker pull ...` | Identical |
  | `git rev-parse HEAD` | Identical |

  Most commands look the same in both shells — only the env-var syntax and
  the Maven Wrapper extension (`.cmd` on Windows) really differ.

### Intel Mac, Linux x86_64, Windows x86_64

Nothing extra needed. The default amd64 images run natively.

---

## Step 1 — Find your `your-org`

`your-org` is your GitHub username (or organisation name if the repo lives
under an org). It appears in every GHCR URL.

> **Terminal** — from the project root

```bash
gh repo view --json owner --jq .owner.login
```

No `gh` CLI? Use git:

```bash
git remote get-url origin
# git@github.com:sidoncode/tradeflow.git
#                ^^^^^^^^^ that's your-org
```

**Apply it everywhere.** Open `.env.example` and `docker-compose.yml` and
search-and-replace `your-org` with your real value.

Example: if your GitHub is `sidoncode`, every occurrence of
`ghcr.io/your-org/...` becomes `ghcr.io/sidoncode/...`.

---

## Step 2 — Push code so CI builds the images

GHCR doesn't have any images yet. The first push to `develop`/`main`
triggers `.github/workflows/ci.yml`, which builds and publishes them.

> **Terminal**

```bash
git add .
git commit -m "Wire Option A deploy"
git push origin main      # or develop — both trigger the docker-build job
```

Then in your browser:

1. Open your repo → **Actions** tab.
2. Watch the latest workflow run. First run takes ~5 minutes because
   nothing is cached yet.
3. Wait for all 4 jobs (`lint`, `backend-build-test`, `frontend-build-test`,
   `docker-build`) to go green.

The `docker-build` job logs a **summary** with the exact image refs it
pushed — scroll to the bottom of the job's output.

---

## Step 3 — Verify the images landed in GHCR

After the workflow finishes:

```
https://github.com/<your-org>?tab=packages
```

You should see two packages: `tradeflow-backend` and `tradeflow-frontend`.
Click each → tag list shows at least:

- `latest`
- A 40-char SHA tag (e.g. `8a3f9c2b1e4d5f6a7b8c9d0e1f2a3b4c5d6e7f8a`)

---

## Step 4 — Get a GitHub PAT for `read:packages`

The packages are **private by default**. Your laptop needs a token to pull.

1. Open <https://github.com/settings/tokens> (classic tokens, NOT fine-grained).
2. **Generate new token (classic)**.
3. Scopes: tick **only** `read:packages`.
4. Copy the token (you'll only see it once).

> **Want public images instead?** On the package page → **Package settings**
> → change visibility to public. Then no token is needed for pulls.

---

## Step 5 — Log in to GHCR (one-time on the laptop)

> **Terminal**

```bash
echo "<paste-PAT-here>" | docker login ghcr.io -u <your-github-username> --password-stdin
```

You should see `Login Succeeded`. Credentials are cached in
`~/.docker/config.json` until the token expires.

**Verify the pull works** before continuing:

```bash
docker pull ghcr.io/<your-org>/tradeflow-backend:latest
docker pull ghcr.io/<your-org>/tradeflow-frontend:latest
```

If both succeed, you're set. If you get `pull access denied`, redo Step 4
with the right scope.

---

## Step 6 — Set up `.env`

> **Terminal** — from project root

```bash
cp .env.example .env
```

Open `.env` and fill in two things:

```env
BACKEND_IMAGE=ghcr.io/<your-org>/tradeflow-backend:latest
FRONTEND_IMAGE=ghcr.io/<your-org>/tradeflow-frontend:latest
```

That's the simple form. For **demo day**, pin to a specific SHA so a
teammate's late push can't roll the deploy mid-stage:

```bash
SHA=$(git rev-parse HEAD)
echo "SHA=$SHA"
```

Then in `.env`:

```env
BACKEND_IMAGE=ghcr.io/<your-org>/tradeflow-backend:8a3f9c2b1e4d5f6a7b8c9d0e1f2a3b4c5d6e7f8a
FRONTEND_IMAGE=ghcr.io/<your-org>/tradeflow-frontend:8a3f9c2b1e4d5f6a7b8c9d0e1f2a3b4c5d6e7f8a
```

---

## Step 7 — Pull and run

> **Terminal** — from project root

```bash
docker compose pull        # fetch the CI-tested images
docker compose up -d       # bring up all 8 services
docker compose ps          # confirm Up / Up (healthy)
```

First boot takes ~60 s while Postgres + Kafka come up. Subsequent boots
are ~20 s.

---

## Step 8 — Verify the platform is live

Open in browser:

| URL | What |
|---|---|
| <http://localhost:5173> | React UI |
| <http://localhost:8080/swagger-ui.html> | Swagger |
| <http://localhost:9000> | Kafdrop |
| <http://localhost:9090/targets> | Prometheus targets (`recon-service` UP) |
| <http://localhost:3000> | Grafana (`admin` / `admin`) |

End-to-end smoke test:

1. UI → **Add Trade** → submit.
2. Kafdrop → topic `trade-events` → your event is there.
3. Grafana → API dashboard → request-rate panel ticks up.
4. Audit row landed:
   ```bash
   docker compose exec postgres psql -U tradeflow_user -d tradeflow \
       -c "SELECT * FROM audit_log ORDER BY id DESC LIMIT 1;"
   ```
5. Recon consumer fired:
   ```bash
   docker compose logs backend | grep "Reconciling tradeRef"
   ```

All five green = the platform is healthy.

---

## Login credentials (basic auth)

Hard-coded in `backend/src/main/java/.../config/SecurityConfig.java`:

| User | Password | Roles |
|---|---|---|
| `viewer` | `viewer-pw` | VIEWER — GET only |
| `trader` | `trader-pw` | VIEWER + TRADER — POST/PUT/DELETE |
| `admin`  | `admin-pw`  | VIEWER + TRADER + ADMIN — actuator access |

In Swagger, click **Authorize** and paste the credentials. In Postman, use
the collection at `postman/tradeflow.postman_collection.json`.

---

## Daily flow (after first-time setup)

Whenever you want the latest CI-built version:

```bash
docker compose pull
docker compose up -d
```

Three commands; one of which is optional (`docker compose ps` to verify).
That's the whole CD loop.

---

## Teardown

```bash
docker compose down              # stop services; keep volumes
docker compose down -v           # also wipe postgres-data + grafana-data
docker compose down --rmi local  # also delete any locally-built images
```

Use `docker compose down -v` between demo rehearsals so each run starts
clean (no leftover trades).

---

## Fast dev loop without Docker (Java code changes)

When iterating on backend or frontend code, Docker round-trips are slow.
Use the dev profile + Vite proxy instead:

> **Terminal #1 — backend** (from `backend/`)

```bash
./mvnw spring-boot:run     # H2 in-memory, no Kafka, instant restart
```

> **Terminal #2 — frontend** (from `frontend/`)

```bash
npm install && npm run dev
```

URLs change to:
- React UI: <http://localhost:5173>
- Swagger:  <http://localhost:8080/swagger-ui.html>
- H2 console: <http://localhost:8080/h2-console> · JDBC URL `jdbc:h2:mem:tradeflow`, user `sa`, no password

**Caveat:** Kafka isn't running, so producer logs throw "Connection refused"
warnings. The REST API still works; Kafka events just don't fire.

---

## Fallback — local Docker build (offline / no GHCR access)

When you can't pull from GHCR (offline, no PAT, or CI hasn't run yet):

```bash
docker compose -f docker-compose.yml -f docker-compose.build.yml up -d --build
```

The override file adds `build:` blocks for backend + frontend. First build
takes ~5 min (Maven downloads the dep tree); subsequent builds reuse the
cached layer.

Alias it so it's less verbose:

```bash
alias compose-build='docker compose -f docker-compose.yml -f docker-compose.build.yml'
compose-build up -d --build
```

---

## Common issues

| Symptom | Cause | Fix |
|---|---|---|
| `pull access denied for ghcr.io/...` | Not logged in, or PAT lacks `read:packages` | Redo Steps 4–5. |
| `401 Unauthorized` on every API call | Wrong basic-auth password | Use the table above. |
| Backend exits with `relation "trades" does not exist` | Liquibase didn't run | `docker compose logs backend \| grep -i liquibase` — usually a classpath / changelog path bug. |
| Kafka publish warnings flood the log (dev mode) | No broker reachable | Move to the full Docker stack. |
| `docker compose pull` finishes but `up` shows old code | Image tag pinned to an old SHA in `.env` | Update `.env` to the new SHA → `pull && up -d`. |
| Grafana panels say "No data" | Time range too narrow | Top-right time picker → "Last 15 minutes". |
| Recon page shows no rows | Filter is on `RESOLVED` but nothing has been resolved yet | Click **OPEN** — seed data ships 3 OPEN breaks. |

---

## End-to-end verification — Kafka pipeline + Grafana dashboards

Once `docker compose up -d` shows all 8 services healthy, run this checklist
to confirm the entire pipeline is wired correctly:

```
React UI → Spring Boot → DB commit → Kafka producer → trade-events topic
                                                          │
                                          ┌───────────────┼────────────────┐
                                          ▼               ▼                ▼
                                   trade-log-group   recon-group     audit-group
                                     (logs only)   (calls service)  (writes DB)
                                                                          │
                                                                          ▼
                                                                    audit_log row
                                                                          │
                                                                    Grafana panels
                                                                    + Prom metrics
```

Takes ~5 min. Do it once after first boot, then again as a smoke test before
any demo.

### Step 1 — Open everything in tabs

| URL | What you're watching |
|---|---|
| <http://localhost:5173> | React UI (where you'll post a trade) |
| <http://localhost:8080/swagger-ui.html> | Swagger (or POST via curl instead) |
| <http://localhost:9000> | Kafdrop (topics + consumer groups) |
| <http://localhost:3000> | Grafana (dashboards) |
| <http://localhost:9090/targets> | Prometheus targets — `recon-service` should be `UP` |

### Step 2 — Pin the Grafana dashboards open

In Grafana (`admin` / `admin`):

1. **Dashboards** → **TradeFlow** folder → open **TradeFlow — API**.
2. Top-right time picker → **Last 15 minutes** + refresh interval **5s**.
3. Open **TradeFlow — Kafka** in a second tab, same time range + refresh.

What to watch in each dashboard:

| TradeFlow — API panel | TradeFlow — Kafka panel |
|---|---|
| Request rate (req/s) per URI | Consumer lag by group |
| p95 latency per URI | Records consumed / sec |
| 5xx error rate | Records produced / sec |
| Trades created (counter) | Listener p95 latency |
| Recon run p95 duration | DLT records (last 5 min) |
| Trades-by-status pie chart | Audited total |

### Step 3 — Send a trade event

> **Swagger:** Authorize as `trader` / `trader-pw` → `POST /api/v1/trades` → Try it out → paste the body below → Execute. Expect **201 Created**.

> **Or curl:**

```bash
curl -u trader:trader-pw \
     -X POST http://localhost:8080/api/v1/trades \
     -H "Content-Type: application/json" \
     -d '{
       "tradeRef":      "TRD-VERIFY-001",
       "instrumentId":   1,
       "counterpartyId": 1,
       "quantity":       1000,
       "price":          245.50,
       "tradeDate":      "2026-03-15"
     }'
```

### Step 4 — Verify each piece of the pipeline fired

Within a few seconds:

**1. Kafdrop — producer fired:**
- <http://localhost:9000> → **Topics** → click **trade-events** → **View Messages** → set partition `0`, offset `0`, count `10` → **View**.
- Latest message: key = `TRD-VERIFY-001`, value contains `"action": "CREATED"`.

**2. Backend logs — all 3 consumer groups fired:**

```bash
docker compose logs --tail 50 backend | grep -E "Received TradeEvent|Reconciling|Auditing"
```

Expect three lines per posted trade:
```
... Received TradeEvent[tradeRef=TRD-VERIFY-001, action=CREATED]   ← trade-log-group
... Reconciling tradeRef=TRD-VERIFY-001                            ← recon-group
... Auditing tradeRef=TRD-VERIFY-001 action=CREATED                ← audit-group
```

If only one or two appear: two consumers are sharing a group. Check each `@KafkaListener(groupId = "...")` — all three must be distinct.

**3. Kafdrop — three consumer groups visible:**
- Kafdrop → **Consumers** (left nav). Three groups: `trade-log-group`, `recon-group`, `audit-group`. Each `Lag` = `0`.

**4. Audit row landed in Postgres:**

```bash
docker compose exec postgres psql -U tradeflow_user -d tradeflow -c \
  "SELECT id, table_name, operation, row_pk, changed_by, changed_at FROM audit_log ORDER BY id DESC LIMIT 3;"
```

Top row: a fresh `audit_log` row with the new trade's `id` and `changed_at = now`.

### Step 5 — Watch Grafana light up

**TradeFlow — API dashboard:**

| Panel | Expected change after the POST |
|---|---|
| Request rate (req/s) | Brief spike on `/api/v1/trades` URI |
| p95 latency | Reading appears (was blank if no traffic yet) |
| **Trades created (total)** | Increments by **+1** |
| Trades-by-status pie | Adds a `PENDING` slice (or grows it) |
| JVM heap used | Slight tick — small GC churn from the request |

**TradeFlow — Kafka dashboard:**

| Panel | Expected change |
|---|---|
| Consumer lag by group | Spikes briefly per group, returns to `0` within a second |
| Records consumed / sec | Visible blip across all three `client_id`s |
| Records produced / sec | Visible blip on the backend's `client_id` |
| Listener p95 latency | A new data point |
| Records sent to DLT | Stays at `0` (no failures) |

If the Grafana panels stay flat: hit the time picker → **Refresh**. Auto-refresh
sometimes misses the first data point.

### Step 6 — Stress test (optional but satisfying)

Throw 50 trades and watch Kafdrop + Grafana react:

```bash
for i in $(seq 100 149); do
  curl -s -u trader:trader-pw \
       -X POST http://localhost:8080/api/v1/trades \
       -H "Content-Type: application/json" \
       -d "{\"tradeRef\":\"TRD-LOAD-$i\",\"instrumentId\":1,\"counterpartyId\":1,\"quantity\":100,\"price\":250,\"tradeDate\":\"2026-03-15\"}" \
       > /dev/null
done
echo "Sent 50 trades"
```

What you'll see:
- **Grafana — API request rate** spikes to ~50 req/s for one bucket, then back to 0.
- **Grafana — Kafka consumer lag** spikes per group, settles to 0 within ~3 s.
- **Grafana — Trades created** counter advances by 50.
- **Kafdrop — trade-events** shows 50 new messages.
- **Postgres audit_log**: 50 new rows.

### Step 7 — Test the dead-letter topic (optional)

Send a malformed payload directly to `trade-events` and watch it land in
`trade-events.DLT` after 3 retries:

```bash
docker compose exec -T kafka kafka-console-producer \
  --bootstrap-server kafka:29092 --topic trade-events <<< 'this-is-not-json'
```

Wait ~4 seconds (3 retries × 1s back-off), then in Kafdrop:

- **trade-events.DLT** topic → 1 new message containing `this-is-not-json`.
- **Grafana — Kafka → Records sent to DLT** stat panel turns **red** (threshold = 1).
- Backend logs show the `DeserializationException` and the DLT publish.

### What "all green" looks like

The full verification is healthy when all of these are true:

- [ ] All 8 containers `Up` / `Up (healthy)` in `docker compose ps`.
- [ ] Prometheus `recon-service` target is `UP`.
- [ ] Posting a trade returns 201 with a `Location` header.
- [ ] The `TRD-VERIFY-001` message appears in Kafdrop within 1 second.
- [ ] All three consumer-group log lines appear in backend logs.
- [ ] A matching `audit_log` row exists in Postgres.
- [ ] Grafana API dashboard: request-rate + trades-created counter advanced.
- [ ] Grafana Kafka dashboard: consumer lag spiked then returned to 0.
- [ ] DLT counter is `0` (no rejected messages) — flips to red only when you trigger Step 7.

If any of these fail, the Step 4–5 callouts above point at the most likely
cause. Otherwise — the platform is wired and healthy.

---

## Multi-arch builds (permanent fix for Apple Silicon / arm64)

The CI workflow builds amd64 images by default. To make it build BOTH amd64
and arm64 — so any laptop pulls the native variant — make two changes in
`.github/workflows/ci.yml` inside the `docker-build` job:

1. **Add QEMU setup** after `actions/checkout@v4`:

   ```yaml
   - name: Set up QEMU
     uses: docker/setup-qemu-action@v3
   ```

2. **Add `platforms` to BOTH `docker/build-push-action` steps:**

   ```yaml
   - name: Build + push backend image
     uses: docker/build-push-action@v5
     with:
       context: ./backend
       push: true
       platforms: linux/amd64,linux/arm64        # ← add this
       tags: |
         ghcr.io/${{ github.repository_owner }}/tradeflow-backend:${{ github.sha }}
         ghcr.io/${{ github.repository_owner }}/tradeflow-backend:latest
       cache-from: type=gha
       cache-to: type=gha,mode=max
   ```

CI build time goes from ~3 min → ~7 min (multi-arch builds in series), but
every laptop pulls native bytecode. No `DOCKER_DEFAULT_PLATFORM`, no
Rosetta tax, no per-shell setup.

---

## Where to go from here

- New to the case study? Read [day0/README.md](./README.md).
- Doing Day 1 right now? [day1/README.md](../day1/README.md).
- Day-10 deploy walkthrough in depth: [day10/day10-local-cicd.md](../day10/day10-local-cicd.md).
- All Day-10 reference solutions are in [day10/README.md](../day10/README.md)
  as `▶ Show solution` collapsible blocks.
