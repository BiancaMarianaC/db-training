# Day 6 PM — Prometheus + Grafana Hands-on

> **Format:** 1.5 hr guided lab.
> **Goal:** Get a live metrics pipeline from Spring Boot → Prometheus → Grafana
> running on your laptop, then build your first real dashboard panel.
> **Supports tickets:** I078, I079, I080, I081 (no extra ticket; this *is* the
> walkthrough for those four).

By the end of this lab you'll be able to fire a request from Postman, watch the
request-rate panel in Grafana tick up in real time, and explain every hop in
between.

> **Path conventions used in this doc**
> - All paths are relative to the project root: `tradeflow-studentscopy/`.
> - When a code block reads or writes a file, the file's full path is given
>   above it as **File:** `...`.
> - When a code block is a shell command, **Terminal** is given above it with
>   the directory you should run it from.

---

## 1. Why are we doing this?

### The problem

Your app is going to misbehave in production. A query will get slow. A 5xx will
spike on a specific endpoint at 03:00. A memory leak will only show up after
six hours of uptime. **Logs alone won't catch this** — they're text, they're
unstructured, and `grep`-ing logs at 3 AM to find "is the API slow right now?"
is not a real strategy.

### The fix

A **metrics pipeline** gives you numerical, time-series visibility into the
app's behaviour:

- **Spring Boot Actuator** exposes the JVM's internal state (heap, threads,
  HTTP request counts) over an HTTP endpoint.
- **Micrometer** is the metrics facade — you call `Counter.increment()` /
  `Timer.record()` in your code and it doesn't care which backend is listening.
- **Prometheus** scrapes that endpoint every 15 seconds and stores the numbers
  as a time-series database.
- **Grafana** queries Prometheus and draws charts.

The result: a chart of "p95 latency on `POST /api/v1/trades` over the last hour"
that updates live and that an on-call engineer can stare at.

### The mental model (commit this to memory)

```
Spring Boot app
   │
   │ exposes /actuator/prometheus
   │ (Micrometer + micrometer-registry-prometheus formats the metrics)
   ▼
Prometheus  ── scrapes every 15s, stores time-series
   │
   │ PromQL query
   ▼
Grafana  ──── renders charts
```

Four arrows. That's the whole lab.

---

## 2. Pre-lab check (5 min)

Before you start, confirm these pieces already exist in the starter. **None of
this is work for you to do** — it's plumbing that's been pre-wired so you can
focus on the lab. If something is missing, flag the instructor.

| File (relative to project root) | What to look for |
|---|---|
| `backend/pom.xml` | `spring-boot-starter-actuator` and `micrometer-registry-prometheus` dependencies |
| `backend/src/main/resources/application.yml` | `management.endpoints.web.exposure.include: health, info, metrics, prometheus` |
| `monitoring/prometheus/prometheus.yml` | A `scrape_configs` block with `job_name: recon-service` |
| `monitoring/grafana/provisioning/datasources/prometheus.yml` | A `Prometheus` datasource pointing at `http://prometheus:9090` |
| `monitoring/grafana/provisioning/dashboards/dashboard.yml` | A dashboard provider pointing at `/etc/grafana/provisioning/dashboards` |
| `monitoring/grafana/provisioning/dashboards/api.json` | A skeleton dashboard with TODO panels |

If all six are present, you're ready.

---

## 3. Step-by-step (60 min)

### Step 1 — See the metrics endpoint from Spring Boot (5 min)

**What:** Run the backend on your laptop and confirm `/actuator/prometheus`
returns Prometheus-format metrics.

**How:**

> **Terminal #1** — run from project root (`tradeflow-studentscopy/`)

```bash
cd backend
./mvnw spring-boot:run
```

Wait for `Started TradeflowApplication`. Then in a second terminal:

> **Terminal #2** — run from anywhere

```bash
curl -s http://localhost:8080/actuator/prometheus | head -20
```

**Expected output (truncated):**

```
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space"} 1.2582912E7
jvm_memory_used_bytes{area="heap",id="G1 Old Gen"} 8.388608E6
# HELP http_server_requests_seconds  Duration of HTTP server request handling
# TYPE http_server_requests_seconds histogram
http_server_requests_seconds_count{method="GET",status="200",uri="/actuator/health"} 3.0
http_server_requests_seconds_sum{method="GET",status="200",uri="/actuator/health"} 0.012
```

**Why this matters:** Spring Boot is now publishing JVM and HTTP metrics in
Prometheus's text format. **Nothing is scraping yet** — these are just sitting
there waiting to be polled. That's the next step.

**Anatomy of one line:** `metric_name{label1="value1",label2="value2"} number` —
labels are how you slice the data later (e.g. "show me only `status=500`
requests").

---

### Step 2 — Read the Prometheus scrape config (5 min)

**What:** Understand exactly what Prometheus will scrape and from where.

**How:** Open this file and read it top-to-bottom.

> **File:** `monitoring/prometheus/prometheus.yml`

```yaml
global:
  scrape_interval: 15s        # poll every 15 seconds
  evaluation_interval: 15s

scrape_configs:

  - job_name: 'prometheus-self'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'recon-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
```

**What each block does:**

- `global.scrape_interval: 15s` — how often Prometheus polls every target.
  15 s is the standard default.
- `prometheus-self` — Prometheus scrapes its own metrics (it's a monitoring
  tool, it should be monitored too).
- `recon-service` — the important one. Targets `backend:8080` (the Docker
  service name, **not** `localhost`) and asks for `/actuator/prometheus`.

**Why `backend:8080` and not `localhost:8080`?** Inside the Docker compose
network each service is reachable by its compose name. From the Prometheus
container's point of view, `localhost` would be *itself*. We'll add the
`backend` service to compose in Step 5.

---

### Step 3 — Add Prometheus to docker-compose (10 min)

**What:** Wire a Prometheus service into `docker-compose.yml` and bring it up.

**How:** Open the compose file and find the commented Prometheus block
(around line 91). Replace the commented block with the YAML below.

> **File to edit:** `docker-compose.yml`
> **Section:** the `services:` block (replace the commented Prometheus stub)

```yaml
prometheus:
  image: prom/prometheus:latest
  container_name: tradeflow-prometheus
  ports:
    - "9090:9090"
  volumes:
    - ./monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
  command:
    - '--config.file=/etc/prometheus/prometheus.yml'
    - '--storage.tsdb.path=/prometheus'
    - '--web.enable-lifecycle'
  restart: unless-stopped
```

**Line-by-line:**
- `image: prom/prometheus:latest` — official Prometheus image.
- `ports: "9090:9090"` — expose Prometheus's UI to your laptop on
  `http://localhost:9090`.
- `volumes: ... :ro` — mount your `prometheus.yml` read-only into the standard
  config path inside the container. **The `:ro` matters** — you don't want
  Prometheus accidentally rewriting it.
- `--web.enable-lifecycle` — lets you reload config without restarting the
  container (handy when iterating on scrape rules).

**Bring it up:**

> **Terminal** — run from project root (`tradeflow-studentscopy/`)

```bash
docker compose up -d prometheus
```

**Verify:** Open `http://localhost:9090/targets` in your browser.

- `prometheus-self` should be **UP** (green).
- `recon-service` will probably be **DOWN** (red) right now — Prometheus is
  looking for `backend:8080` and the backend isn't in compose yet. We'll
  fix that on Day 10 when the full stack lands; for now run the backend with
  `./mvnw spring-boot:run` and add this **temporary local-dev** target so
  Prometheus scrapes it via your host machine:

> **File to edit:** `monitoring/prometheus/prometheus.yml`
> **Add this block** under `scrape_configs:` (alongside the existing jobs).
> **Mark it `# TEMPORARY — local dev only`** and remove before merge.

```yaml
  # TEMPORARY — local dev only. Remove before merge.
  - job_name: 'recon-service-local'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']
```

Then reload Prometheus config without restarting:

> **Terminal** — run from anywhere

```bash
curl -X POST http://localhost:9090/-/reload
```

`recon-service-local` should flip to **UP**.

**Why `host.docker.internal`:** on Mac and Windows, Docker provides this magic
DNS name so containers can reach the host. On Linux, use
`--add-host=host.docker.internal:host-gateway` or just use your machine's LAN IP.

---

### Step 4 — Add Grafana to docker-compose (10 min)

**What:** Wire Grafana so it auto-connects to Prometheus and auto-loads the
dashboard skeleton.

**How:**

> **File to edit:** `docker-compose.yml`
> **Section:** the `services:` block (replace the commented Grafana stub, ~line 101)

```yaml
grafana:
  image: grafana/grafana:latest
  container_name: tradeflow-grafana
  ports:
    - "3000:3000"
  environment:
    GF_SECURITY_ADMIN_USER: admin
    GF_SECURITY_ADMIN_PASSWORD: admin
    GF_USERS_ALLOW_SIGN_UP: "false"
  volumes:
    - ./monitoring/grafana/provisioning:/etc/grafana/provisioning:ro
    - grafana-data:/var/lib/grafana
  depends_on:
    - prometheus
  restart: unless-stopped
```

And declare the new named volume.

> **Same file:** `docker-compose.yml`
> **Section:** the `volumes:` block at the bottom of the file

```yaml
volumes:
  postgres-data:
  grafana-data:
```

**Line-by-line:**
- `ports: "3000:3000"` — Grafana UI on `http://localhost:3000`.
- `GF_SECURITY_ADMIN_PASSWORD: admin` — first-boot admin password. **Fine for
  dev, never for prod.**
- `volumes: ./monitoring/grafana/provisioning:/etc/grafana/provisioning:ro` —
  this is the magic line. Grafana looks at this path on boot and auto-creates
  the datasource and dashboards you defined as YAML/JSON. No manual UI clicking.
- `grafana-data` — a *named* volume so your changes survive `docker compose
  down`. Without it, every restart wipes Grafana state.
- `depends_on: prometheus` — start order; Grafana boots after Prometheus.

**Bring it up:**

> **Terminal** — run from project root (`tradeflow-studentscopy/`)

```bash
docker compose up -d grafana
```

**Verify:** Open `http://localhost:3000` → log in (`admin` / `admin`, skip the
password change prompt) → click the gear icon → **Connections** → **Data
sources**. You should see **Prometheus** already listed, with URL
`http://prometheus:9090`. Click it → scroll down → click **Save & test**:
"Successfully queried the Prometheus API."

---

### Step 5 — Build your first panel (15 min)

**What:** Replace one of the TODO panels in the auto-loaded dashboard with a
real request-rate chart.

**What you're starting from.** The skeleton dashboard shipped with the
starter is intentionally minimal — three placeholder panels with no queries.
You'll fill them in via the UI, then export the result back to JSON (Step 6).

> **File (read only — do not hand-edit):** `monitoring/grafana/provisioning/dashboards/api.json`

```json
{
  "_comment_what":  "TICKET-I080 + I081 — API metrics dashboard",
  "_comment_how":   "Build this in the Grafana UI, then click 'Share' -> 'Export' -> 'Save to file' to replace this skeleton with the real dashboard JSON.",
  "_todo":          "TICKET-I080: panels = (request rate, p95 latency, error rate). TICKET-I081: pie chart of trades by status.",
  "panels": [
    {
      "title": "TODO — request rate (rate(http_server_requests_seconds_count[1m]))",
      "type": "stat",
      "gridPos": { "x": 0, "y": 0, "w": 12, "h": 4 }
    },
    {
      "title": "TODO — p95 latency (histogram_quantile(0.95, ...))",
      "type": "stat",
      "gridPos": { "x": 12, "y": 0, "w": 12, "h": 4 }
    },
    {
      "title": "TODO — error rate by endpoint",
      "type": "timeseries",
      "gridPos": { "x": 0, "y": 4, "w": 24, "h": 8 }
    }
  ],
  "title": "TradeFlow — API",
  "schemaVersion": 38,
  "version": 1
}
```

The three panel titles tell you what query each should hold once you're done.
Step 6 will overwrite this file with what you build in the UI.

**How (in the Grafana UI at `http://localhost:3000`):**

1. Click the four-square icon → **Dashboards** → open **TradeFlow → API**.
2. You'll see three TODO panels (loaded from `monitoring/grafana/provisioning/dashboards/api.json`).
   Click the ⋮ menu on the first one → **Edit**.
3. In the query editor at the bottom, enter the following PromQL:

   > **Where this goes:** Grafana panel query editor (not a file)

   ```promql
   rate(http_server_requests_seconds_count[1m])
   ```

4. Above the query, set:
   - **Panel title:** `Request rate (req/s)`
   - **Visualization:** `Time series` (right-hand sidebar)
5. Click **Apply** (top right). Then **Save dashboard** (disk icon).
6. **Generate traffic:**

   > **Terminal** — run from anywhere (with the backend still running from Step 1)

   ```bash
   for i in {1..100}; do curl -s http://localhost:8080/actuator/health > /dev/null; sleep 0.2; done
   ```

   Watch the panel tick up live. **This is the win.**

**Anatomy of the PromQL:**
- `http_server_requests_seconds_count` — the underlying counter (total
  requests since boot, monotonically increasing).
- `rate(... [1m])` — convert that to "requests per second, averaged over the
  last 1 minute". Without `rate()` you'd see a line that only ever goes up.

---

### Step 6 — Export the dashboard back to git (5 min)

**What:** Persist your panel as code so the next person who runs `docker
compose up` gets the same dashboard.

**How:**

1. In the dashboard view (Grafana UI), click the **Share** icon (top right) → **Export** tab.
2. Untick "Export for sharing externally" (you want the internal IDs preserved
   for now).
3. Click **Save to file** — your browser downloads `TradeFlow - API-XXX.json`.
4. **Replace** the contents of the skeleton dashboard with the downloaded file:

   > **File to overwrite:** `monitoring/grafana/provisioning/dashboards/api.json`
   > **Source:** the file you just downloaded from Grafana
   > **Terminal** — run from project root (`tradeflow-studentscopy/`)

   ```bash
   mv ~/Downloads/TradeFlow*API*.json monitoring/grafana/provisioning/dashboards/api.json
   ```

5. Commit it.

**Why this matters:** Dashboards-as-code. Anyone who pulls your branch and runs
`docker compose up` gets the same dashboard — no clicking, no "works on my
machine".

---

### Step 7 — Add a custom Counter (TICKET-I079) (10 min)

**What:** Spring Boot's built-in metrics tell you about HTTP and the JVM.
**Custom business metrics** tell you about *your domain* — trades created,
recon runs, breaks resolved.

**How — the pattern (illustrative, not the ticket answer):**

> **Pattern file (example):** `backend/src/main/java/com/dbtraining/tradeflow/service/ExampleService.java`
> Use this *shape* in your real services — do not copy this class verbatim.

```java
package com.dbtraining.tradeflow.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class ExampleService {

    private final Counter requestsCounter;

    public ExampleService(MeterRegistry registry) {
        this.requestsCounter = Counter.builder("tradeflow_example_requests_total")
            .description("Total example requests")
            .tag("component", "example")
            .register(registry);
    }

    public void handle() {
        requestsCounter.increment();
        // ... real work ...
    }
}
```

**The pattern:**
- **Name:** snake_case, ends in `_total` for counters. Prefix with `tradeflow_`
  so your metrics don't collide with Spring's.
- **Tags:** label dimensions you'll want to slice by later (`status`, `region`,
  `instrument_type`). **Don't tag high-cardinality data** like trade IDs — see
  pitfall §5.4.
- **Inject `MeterRegistry`**, build the counter once in the constructor, hold
  the reference. Don't `.builder().register()` on every call — it's wasteful.

**Now you apply it.** Per TICKET-I079:

- Add `tradeflow_trades_created_total` to `TradeService` — incremented on every
  successful POST.

  > **File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/service/TradeService.java`

- Add `tradeflow_recon_run_seconds` (a `Timer`, not a Counter) wrapping
  `ReconciliationService.run()`.

  > **File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/service/ReconciliationService.java`

For the timer, the pattern is:

> **Pattern file (example):** `backend/src/main/java/com/dbtraining/tradeflow/service/ReconciliationService.java`
> Apply this *shape* — your real `run()` body stays your own.

```java
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class ReconciliationService {

    private final Timer reconTimer;

    public ReconciliationService(MeterRegistry registry) {
        this.reconTimer = Timer.builder("tradeflow_recon_run_seconds")
            .description("Recon run duration")
            .register(registry);
    }

    public void run() {
        reconTimer.record(() -> {
            // ... your actual recon work ...
        });
    }
}
```

**Verify:** Restart the backend. Trigger the relevant code path. Then:

> **Terminal** — run from anywhere

```bash
curl -s http://localhost:8080/actuator/prometheus | grep tradeflow_
```

You should see your new metric. Add a panel in Grafana querying it.

---

### Step 8 — PromQL queries you'll need (reference, 5 min)

Step 5 walked you through the *first* panel (request rate). The dashboard
needs three more panels for tickets I080 + I081. Use these queries as your
starting point — paste into the Grafana panel query editor exactly as shown.

> **Where each query goes:** Grafana panel query editor (not a file).
> Change the **Panel title** and **Visualization type** to match the row.

| Panel | Visualization | PromQL |
|---|---|---|
| **Request rate by URI** | Time series | `sum(rate(http_server_requests_seconds_count{uri=~"/api/.*"}[1m])) by (uri)` |
| **p95 latency by URI** | Time series | `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri=~"/api/.*"}[5m])) by (le, uri))` |
| **Error rate (5xx) by URI** | Time series | `sum(rate(http_server_requests_seconds_count{uri=~"/api/.*",status=~"5.."}[1m])) by (uri)` |
| **Trades created (cumulative)** | Stat | `sum(tradeflow_trades_created_total)` |
| **Trades by status** (TICKET-I081) | Pie chart | `sum(tradeflow_trades_created_total) by (status)` (requires the `status` tag — see Pitfall §5.2 first) |
| **Recon run p95 duration** | Stat | `histogram_quantile(0.95, sum(rate(tradeflow_recon_run_seconds_bucket[5m])) by (le))` |
| **JVM heap used** | Time series | `sum(jvm_memory_used_bytes{area="heap"}) by (id)` |

**How to read these:**

- `rate(... [1m])` — convert a cumulative counter into a per-second rate over
  the last 1 minute. Use this on anything ending in `_count` or `_total`.
- `histogram_quantile(0.95, ...)` — read a percentile out of a `_bucket`
  histogram metric. The `sum(... by (le))` shape is mandatory — `le` (less
  than or equal) is the histogram bucket label.
- `sum(... by (uri))` — fold across all status codes / instance IDs, then
  split the result into one line per URI.
- `{uri=~"/api/.*"}` — regex label match (the `=~`). Filters out
  `/actuator/*` noise so the dashboard only shows business endpoints.

**Apply each query the same way as Step 5:**

1. Open dashboard → Edit panel.
2. Paste PromQL → set title + visualization type.
3. **Apply** → confirm you see data → **Save dashboard**.
4. After all panels are built, re-do Step 6 to export back to git.

**Smoke-test trick.** A panel showing "No data" is usually one of three
things: time range too narrow (Pitfall §6.4), regex doesn't match any
labels, or the underlying metric doesn't exist yet. Test the query in
Prometheus directly first:

> **Open in browser:** `http://localhost:9090/graph`

Paste the query. If Prometheus returns nothing, Grafana will too. Fix it
there before bringing it back to the dashboard.

---

## 4. End-of-lab checklist

- [ ] `http://localhost:9090/targets` shows `recon-service` (or `-local`) as **UP**.
- [ ] `http://localhost:3000` opens Grafana, Prometheus datasource is pre-connected.
- [ ] You hit `/actuator/health` 100 times and saw the request-rate panel move.
- [ ] You exported your dashboard JSON to `monitoring/grafana/provisioning/dashboards/api.json`.
- [ ] You can `grep tradeflow_` on `/actuator/prometheus` and see at least one
      custom metric you defined.
- [ ] You can explain the four-arrow mental model from §1 out loud.

---

## 5. Pitfalls (commit to memory)

### Pitfall 1 — `localhost` from inside Docker

The Prometheus container's `localhost` is **itself**, not your laptop or the
backend. Inside compose, use the *service name*: `backend:8080`. From a
container on Mac/Windows, to reach the host: `host.docker.internal`.

### Pitfall 2 — High-cardinality labels

> **Where this would live:** any `@Service` class under `backend/src/main/java/com/dbtraining/tradeflow/service/`
> **Don't write this code:**

```java
// NEVER do this
Counter.builder("trades_processed").tag("trade_id", trade.getId()).register(registry);
```

Every distinct `trade_id` creates a new time series. With 10 M trades, you have
10 M series. Prometheus's RAM explodes; queries time out. **Labels are for
dimensions you'll filter/group by**, not for unique identifiers. If you need
per-trade visibility, that's a *log* line, not a metric.

### Pitfall 3 — Forgetting `[1m]` on counters

> **Where this goes:** Grafana panel query editor (not a file)

```promql
http_server_requests_seconds_count            # WRONG — always-rising line
rate(http_server_requests_seconds_count[1m])  # RIGHT — requests per second
```

Raw counters are cumulative since process start. `rate()` is what turns them
into the "requests per second now" number you actually want.

### Pitfall 4 — Metric exposure off

If `curl /actuator/prometheus` returns 404, check this file:

> **File:** `backend/src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus   # 'prometheus' must be here
```

The default exposure is `health, info` only — Prometheus is opt-in.

### Pitfall 5 — Building a counter inside a hot loop

> **Where this lives:** any `@Service` in `backend/src/main/java/com/dbtraining/tradeflow/service/`

```java
// WRONG — creates a new counter object per call
public void handle() {
    Counter.builder("...").register(registry).increment();
}

// RIGHT — register once, increment many
private final Counter counter = Counter.builder("...").register(registry);
public void handle() { counter.increment(); }
```

Micrometer dedupes by name + tags so you won't get duplicates, but the
construction overhead per request adds up. Always hold the reference.

---

## 6. Common mistakes and errors

A field guide to the failures you'll see at least once.

### 6.1 `recon-service` target shows DOWN with `connection refused`

**Where you'll see it:** Prometheus UI → **Status** → **Targets** at
`http://localhost:9090/targets`.

**Message:**
```
recon-service   DOWN
Last error: Get "http://backend:8080/actuator/prometheus": dial tcp:
  lookup backend on 127.0.0.11:53: no such host
```

**Cause:** Prometheus is looking for a container named `backend` in the compose
network, but the backend isn't running there yet.

**Fix:** Either (a) bring the backend up via `docker compose up -d backend`
once Day 10 wires it (TICKET-I125), or (b) for now, add the
`host.docker.internal:8080` target to `monitoring/prometheus/prometheus.yml`
as shown in Step 3.

---

### 6.2 `recon-service` target shows DOWN with `404 Not Found`

**Where you'll see it:** Prometheus UI → **Status** → **Targets**.

**Message:**
```
server returned HTTP status 404 Not Found
```

**Cause:** The backend is reachable, but `/actuator/prometheus` isn't exposed.

**Fix:** Check `backend/src/main/resources/application.yml` includes
`prometheus` in `management.endpoints.web.exposure.include`. See Pitfall §5.4.

---

### 6.3 `recon-service` target shows DOWN with `403 Forbidden`

**Where you'll see it:** Prometheus UI → **Status** → **Targets**.

**Message:**
```
server returned HTTP status 403 Forbidden
```

**Cause:** After TICKET-I077 (role-based security), `/actuator/**` is locked
down to `ROLE_ADMIN`. Unauthenticated Prometheus can't scrape.

**Fix:** Two options.

- **(a) Permit the endpoint for everyone** in the Spring Security config.

  > **File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/config/SecurityConfig.java`

  ```java
  .requestMatchers("/actuator/prometheus", "/actuator/health").permitAll()
  ```

  Safe-ish in dev; in real prod you'd lock this to Prometheus's source IP or
  use basic auth.

- **(b) Add basic-auth scraping** by extending the scrape config.

  > **File to edit:** `monitoring/prometheus/prometheus.yml`

  ```yaml
  - job_name: 'recon-service'
    metrics_path: '/actuator/prometheus'
    basic_auth:
      username: prometheus
      password: <secret>
    static_configs:
      - targets: ['backend:8080']
  ```

  Heavier; use for prod.

---

### 6.4 Grafana shows "No data" on every panel

**Where you'll see it:** any panel in the Grafana UI shows the text "No data".

**Cause one:** Datasource URL wrong. Inside Docker, Grafana must reach
Prometheus at `http://prometheus:9090` — **not** `http://localhost:9090`.

> **File to check:** `monitoring/grafana/provisioning/datasources/prometheus.yml`
> The `url:` line should read `http://prometheus:9090`.

**Cause two:** Prometheus has no data yet for that query. Open
`http://localhost:9090/graph`, paste your PromQL, and confirm it returns
something. If Prometheus is empty, Grafana will be empty.

**Cause three:** Time range too narrow. Grafana default top-right is "last
6 hours" — if your app has only been up 5 minutes, you'll see a flat line at
the right edge. Switch to "Last 15 minutes".

---

### 6.5 Provisioned dashboard doesn't appear in Grafana

**Where you'll see it:** Grafana boots fine, but the TradeFlow folder is empty
or missing in the Dashboards list.

**Cause:** Either the volume mount is wrong, or the JSON file is malformed.

**Fix:**

> **Terminal** — run from project root (`tradeflow-studentscopy/`)

```bash
docker compose logs grafana | grep -i "provision\|dashboard"
```

Look for a line like `failed to load dashboard from .../api.json`. Then
validate the JSON:

> **Terminal** — run from project root (`tradeflow-studentscopy/`)

```bash
jq . monitoring/grafana/provisioning/dashboards/api.json
```

A trailing comma or missing brace kills the whole provider.

---

### 6.6 Custom metric doesn't appear on `/actuator/prometheus`

**Where you'll see it:** the grep below returns nothing.

> **Terminal** — run from anywhere

```bash
curl -s http://localhost:8080/actuator/prometheus | grep tradeflow_
```

**Cause:** The counter is built but **never called yet** — Micrometer doesn't
publish metrics that have a value of 0 across all tags unless you increment
once, depending on registry version.

**Fix:** Either call `increment()` at least once (e.g. by hitting the endpoint
that triggers it), or use `Counter.builder(...).baseUnit("requests")` and
`.register(...)` — in newer Micrometer, registered counters show up immediately
even with count 0. The fix lives in whichever `@Service` declared the counter,
under `backend/src/main/java/com/dbtraining/tradeflow/service/`.

---

### 6.7 `OutOfMemoryError` in Prometheus after a few hours

**Where you'll see it:** Prometheus container logs.

> **Terminal** — run from project root (`tradeflow-studentscopy/`)

```bash
docker compose logs prometheus | grep -i "out of memory\|OOM"
```

**Cause:** High-cardinality labels (see Pitfall §5.2). Every distinct label
combination is a separate time series; trades, users, request UUIDs as labels
will blow Prometheus's RAM.

**Fix:** Audit your custom metrics' series count.

> **Terminal** — run from anywhere

```bash
curl -s http://localhost:8080/actuator/prometheus | awk -F'{' '{print $1}' | sort -u | wc -l
```

If that number is climbing minute-over-minute, you have a cardinality leak.
Drop the offending tag from the `@Service` that defines it
(under `backend/src/main/java/com/dbtraining/tradeflow/service/`).

---

### 6.8 Mental-model mistakes

- **"Logs and metrics are the same thing."** They're not. Logs are
  per-event text. Metrics are numerical aggregates over time. You want both —
  metrics to know *that* something's wrong, logs to know *why*.
- **"More tags = better."** No. More tags = combinatorial explosion. Add a
  tag only if you'll actually filter or group by it.
- **"I'll just manually fix the dashboard in the UI."** Then it dies the next
  time someone fresh-clones the repo. Export → JSON → git
  (`monitoring/grafana/provisioning/dashboards/api.json`), every time.
- **"Prometheus stores everything forever."** Default retention is 15 days.
  Long-term storage is a separate problem (Thanos, Cortex, remote-write to
  S3) and out of scope for this lab.

---

**Back to:** [Day 6 README](./README.md) · **Next day:** [Day 7 — HTML + CSS dashboard](../day7/README.md)
