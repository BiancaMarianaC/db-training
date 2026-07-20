# Day 10 PM — Local CI/CD with Kafka, Prometheus & Grafana

> **Format:** ~1.5 hr lab.
> **Goal:** Wire a real CI/CD pipeline where GitHub Actions builds + tests + publishes
> the backend and frontend Docker images, and the demo laptop pulls them and runs the
> full stack (Spring Boot + Postgres + Kafka + Prometheus + Grafana) via
> `docker compose up`.
> **Supports tickets:** I125, I126, I127, I132, I133.

> **Path conventions used in this doc**
> - All paths are relative to the project root: `tradeflow-studentscopy/`.
> - When a code block reads or writes a file, the file's full path is given
>   above it as **File:** `...`.
> - When a code block is a shell command, **Terminal** is given above it with
>   the directory you should run it from.

---

## 1. What "CI/CD" actually means for this demo

You will **not** be hosting TradeFlow on a public URL. The demo runs on the
team's laptop, projected on a screen. So "deployment" doesn't mean "push to
the cloud" — it means "the latest tested code runs on the demo laptop on
demo day, end-to-end, with all 7 services healthy."

A real CI/CD pipeline in that world looks like:

```
Developer pushes to GitHub
   │
   ▼
GitHub Actions (CI)
   ├── lint
   ├── backend build + test + Liquibase validate    ← TICKET-I133/I134
   ├── frontend build + test
   └── build Docker images + push to GHCR           ← what this lab adds
       (GitHub Container Registry — comes free with the repo)
   │
   ▼
Demo laptop (the "deployment target")
   docker compose pull && docker compose up -d      ← pulls latest tested images
```

The pipeline **ends at the registry**. The demo laptop is the consumer. This
is the same pattern small teams use to deploy to a single VM — only difference
is the VM is your MacBook.

**Why this is the right shape:**

- The full Kafka + Prometheus + Grafana stack lives in `docker-compose.yml`.
  No free PaaS hosts all of that — but `docker compose up` does, identically,
  every time.
- GitHub Actions does the slow work (build, test, image push) on its
  infrastructure, not your laptop, so the demo laptop only does a fast
  `docker pull`.
- Every team member's "what's deployed?" answer is unambiguous: *"the image
  tagged with the SHA of the last green build on `main`."*

---

## 2. The pieces of the wiring

| # | Piece | Lives in | Job |
|---|---|---|---|
| 1 | `docker login ghcr.io` | demo laptop (one-time) | Authenticate the laptop with GitHub Container Registry. |
| 2 | Backend Dockerfile | `backend/Dockerfile` | TICKET-I125 — multi-stage build of the Spring Boot JAR. |
| 3 | Frontend Dockerfile | `frontend/Dockerfile` | TICKET-I126 — multi-stage build of the Vite + nginx bundle. |
| 4 | Build-and-push job | `.github/workflows/ci.yml` (extend the existing `docker-build` job) | On every push to `main`/`develop`, push images to `ghcr.io/<org>/tradeflow-backend` and `tradeflow-frontend`. |
| 5 | Image refs in compose | `docker-compose.yml` | Switch backend + frontend from `build:` to `image:` pointing at GHCR. |
| 6 | Pinned tag in `.env` | `.env` (untracked) | Single source of truth for "what version is deployed". |

Six things. We're not changing Kafka / Prometheus / Grafana / Postgres at all
— they use upstream public images straight from Docker Hub.

---

## 3. Step-by-step (60 min)

### Step 1 — Verify the Dockerfiles exist (5 min)

This lab assumes tickets I125 and I126 are done. Quick sanity check:

> **Terminal** — run from project root (`tradeflow-studentscopy/`)

```bash
test -f backend/Dockerfile && test -f frontend/Dockerfile && echo "OK"
```

If you see `OK`, you're ready. If not, finish I125 / I126 first — those tickets
own the multi-stage build details.

---

### Step 2 — Extend the CI workflow to push images to GHCR (15 min)

**What:** Modify the existing `docker-build` job so it logs into GitHub
Container Registry and pushes the built images, tagged with both the git SHA
and `latest`.

**Why:** Without `--push`, the images you build in CI vanish when the runner
shuts down. They have to live somewhere your laptop can `docker pull` from.
GHCR is the obvious choice — free, scoped to your repo, no separate signup.

**How:**

> **File to edit:** `.github/workflows/ci.yml`
> **Section:** the existing `docker-build:` job at the bottom.

Replace the entire `docker-build:` job with the version below:

```yaml
  docker-build:
    needs: [backend-build-test, frontend-build-test]
    if: github.event_name == 'push' && (github.ref == 'refs/heads/develop' || github.ref == 'refs/heads/main')
    runs-on: ubuntu-latest

    # GHCR needs these scopes — otherwise the push step 403s.
    permissions:
      contents: read
      packages: write

    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build + push backend image
        uses: docker/build-push-action@v5
        with:
          context: ./backend
          push: true
          tags: |
            ghcr.io/${{ github.repository_owner }}/tradeflow-backend:${{ github.sha }}
            ghcr.io/${{ github.repository_owner }}/tradeflow-backend:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Build + push frontend image
        uses: docker/build-push-action@v5
        with:
          context: ./frontend
          push: true
          tags: |
            ghcr.io/${{ github.repository_owner }}/tradeflow-frontend:${{ github.sha }}
            ghcr.io/${{ github.repository_owner }}/tradeflow-frontend:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

**Anatomy:**

- **`permissions: packages: write`** — the workflow token (`GITHUB_TOKEN`) is
  read-only by default. You have to explicitly grant `packages: write` for
  the GHCR push to work.
- **`secrets.GITHUB_TOKEN`** — auto-injected, no setup needed. Don't create a
  Personal Access Token unless you have a reason to.
- **Two tags per image** — `:<git-sha>` (immutable, audit trail) and `:latest`
  (convenience for the demo laptop). Pull the SHA when you want
  reproducibility, pull `:latest` when you want "the freshest tested build".
- **`cache-from / cache-to: type=gha`** — caches Docker layers between CI
  runs in GitHub's free Actions cache. First build is slow; subsequent builds
  are 5-10x faster.
- **`if:` guard kept from the original** — images only get pushed on
  `develop`/`main` pushes, not on every feature-branch CI run. Your registry
  doesn't bloat with experimental tags.

**Push and verify:**

> **Terminal** — run from project root (`tradeflow-studentscopy/`)

```bash
git add .github/workflows/ci.yml
git commit -m "CI: push backend + frontend images to GHCR [TICKET-I133]"
git push origin main
```

Watch the Actions tab. When the `docker-build` job goes green, open:

```
https://github.com/<your-org>/<your-repo>/pkgs/container/tradeflow-backend
https://github.com/<your-org>/<your-repo>/pkgs/container/tradeflow-frontend
```

Both should list two tags: the long SHA and `latest`.

---

### Step 3 — Switch docker-compose to consume the registry images (10 min)

**What:** Change the backend and frontend services in `docker-compose.yml`
from `build: ./backend` to `image: ghcr.io/...:latest`. Keep Kafka, Postgres,
Prometheus, Grafana exactly as they are.

**Why:** `build:` makes the *demo laptop* compile the JAR and bundle the React
app — slow, depends on JDK/Node being installed, easy to get a different
result from CI. `image:` makes the laptop pull a pre-built, CI-tested artifact.
That's the whole point of having a registry.

**How:**

> **File to edit:** `docker-compose.yml`
> **Section:** the `backend:` service (replace the commented stub around line 70)

```yaml
backend:
  image: ${BACKEND_IMAGE:-ghcr.io/your-org/tradeflow-backend:latest}
  container_name: tradeflow-backend
  ports:
    - "8080:8080"
  environment:
    SPRING_PROFILES_ACTIVE: docker
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/${POSTGRES_DB:-tradeflow}
    SPRING_DATASOURCE_USERNAME: ${POSTGRES_USER:-tradeflow_user}
    SPRING_DATASOURCE_PASSWORD: ${POSTGRES_PASSWORD:-changeme}
    SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
  depends_on:
    postgres:
      condition: service_healthy
    kafka:
      condition: service_started
  restart: unless-stopped
```

> **Same file:** `docker-compose.yml`
> **Section:** the `frontend:` service (replace the commented stub around line 80)

```yaml
frontend:
  image: ${FRONTEND_IMAGE:-ghcr.io/your-org/tradeflow-frontend:latest}
  container_name: tradeflow-frontend
  ports:
    - "5173:80"
  depends_on:
    - backend
  restart: unless-stopped
```

**Why the `${BACKEND_IMAGE:-...}` syntax?** This is Bash-style default
substitution. If the env var is set (in `.env`, or exported in the shell),
that value wins. If it's not, the fallback after `:-` is used. Lets you pin
to a specific SHA for the demo without editing the compose file:

```bash
BACKEND_IMAGE=ghcr.io/your-org/tradeflow-backend:abc123def docker compose up -d
```

---

### Step 4 — Authenticate the demo laptop with GHCR (5 min)

**What:** Tell Docker on the demo laptop how to log in to `ghcr.io` so
`docker pull` works for your (potentially private) images.

**Why:** GHCR packages default to **private** when first published — they're
visible only to people with read access to the source repo. Even if you make
them public later, you'll want to do this anyway for any future private repo.

**How — one-time setup on the demo laptop:**

1. In GitHub, go to your profile → **Settings** → **Developer settings** →
   **Personal access tokens** → **Tokens (classic)** → **Generate new token (classic)**.
2. Scopes: tick **`read:packages`**. Nothing else.
3. Copy the token (you'll only see it once).

4. Log in:

> **Terminal** — run from anywhere on the demo laptop

```bash
echo "<paste-token-here>" | docker login ghcr.io -u <your-github-username> --password-stdin
```

You should see `Login Succeeded`. Docker stores credentials in
`~/.docker/config.json` and won't ask again until the token expires.

**Test the pull works:**

> **Terminal** — run from anywhere

```bash
docker pull ghcr.io/your-org/tradeflow-backend:latest
```

If it pulls, you're done. If it 403s, you either fat-fingered the token or
your token doesn't have `read:packages`.

---

### Step 5 — Lock in the image versions for the demo (5 min)

**What:** Create a `.env` file that pins the *exact* image SHAs you want to
demo. **Not committed** — every demo target laptop has its own.

**Why:** `:latest` is fine for "I want the freshest build". For the actual
15-min demo you want **immutability** — pin to a specific SHA so a teammate's
late push doesn't surprise you mid-demo.

**How:**

> **File to create:** `.env`  (project root)
> **Do not commit** — it's already in `.gitignore`.

```env
# Pinned image versions for the demo run.
# Copy the SHA from the green CI build you want to demo.
BACKEND_IMAGE=ghcr.io/your-org/tradeflow-backend:8a3f9c2b1e4d5f6a7b8c9d0e1f2a3b4c5d6e7f8a
FRONTEND_IMAGE=ghcr.io/your-org/tradeflow-frontend:8a3f9c2b1e4d5f6a7b8c9d0e1f2a3b4c5d6e7f8a

# Postgres credentials — pin these too so the seed data path is reproducible.
POSTGRES_DB=tradeflow
POSTGRES_USER=tradeflow_user
POSTGRES_PASSWORD=demo-only-password
```

To find the SHA you want: open the GitHub Actions run for the commit you want
to demo, copy the `${{ github.sha }}` value (visible in the workflow logs as
"Building with SHA abc123..."), and paste it here.

> **Pro tip:** also tag a *release tag* in git for the demo SHA
> (`git tag v1.0.0-demo && git push --tags`) so you can find this build in 6
> months without spelunking through workflow runs.

---

### Step 6 — Deploy to the demo laptop (10 min)

**What:** A two-command deploy that pulls the pinned images and brings the
whole stack up.

**How:**

> **Terminal** — run from project root (`tradeflow-studentscopy/`) on the demo laptop

```bash
# Pull whatever's pinned in .env (or :latest if .env is missing).
docker compose pull

# Bring the whole stack up in the background.
docker compose up -d
```

That's the deploy. Two commands. The demo laptop now runs:

- Postgres (with your Liquibase schema applied on backend startup)
- Zookeeper + Kafka + Kafdrop
- Backend (the CI-tested image)
- Frontend (the CI-tested image, served by nginx)
- Prometheus (scraping the backend)
- Grafana (with provisioned datasource + dashboards)

---

### Step 7 — Smoke test the full stack (10 min)

**What:** Confirm all 7 services are healthy and the integrated flow works
end-to-end. This is TICKET-I132 in scripted form.

**How — run through these in order:**

> **Terminal** — run from project root (`tradeflow-studentscopy/`)

```bash
# 1. All 7 containers up?
docker compose ps
# Expect: postgres, zookeeper, kafka, kafdrop, backend, frontend, prometheus, grafana
# Status: 'Up' (or 'Up (healthy)' for those with healthchecks)

# 2. Backend healthy?
curl -s http://localhost:8080/actuator/health | jq .
# Expect: {"status":"UP", ...}

# 3. Frontend serving?
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:5173
# Expect: 200

# 4. Prometheus scraping the backend?
curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job, health}'
# Expect: recon-service health = "up"

# 5. Grafana up?
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:3000
# Expect: 200 (redirects to /login)

# 6. Kafka up? (uses kafdrop's REST proxy if you wired it; otherwise skip)
curl -s http://localhost:9000/topic | head -20
```

Then the **end-to-end flow** (manual, browser-based):

1. Open `http://localhost:5173`.
2. Post a trade through the UI (or via Postman).
3. Open Kafdrop at `http://localhost:9000` → confirm the trade event landed
   on the `trade-events` topic.
4. Open Grafana at `http://localhost:3000` → API dashboard → request-rate
   panel should have ticked up.
5. `docker compose logs backend | grep -i "audit"` → audit row written.

All five checks green = the demo is shippable.

---

## 4. Optional — self-hosted runner for fully-automated CD (15 min)

The pattern above is **manual pull + up** on the demo laptop. If you want to
go one step further — "push to main and the demo laptop auto-redeploys" — use
a **self-hosted Actions runner** on the demo laptop:

1. GitHub repo → **Settings** → **Actions** → **Runners** → **New
   self-hosted runner**. Follow the OS-specific install instructions
   (downloads a small Go binary, runs it under your user).
2. Add a new job to `.github/workflows/ci.yml` that runs on the self-hosted
   runner *after* `docker-build` succeeds:

> **File to edit:** `.github/workflows/ci.yml`
> **Add this job at the end:**

```yaml
  deploy-to-demo-laptop:
    needs: docker-build
    if: github.ref == 'refs/heads/main'
    runs-on: self-hosted          # NOT ubuntu-latest — your laptop
    steps:
      - uses: actions/checkout@v4
      - name: Pull + restart
        run: |
          cd ${{ github.workspace }}
          docker compose pull
          docker compose up -d
```

**Trade-offs:**

- **Pro:** Push → 5 min later, demo laptop is on the new build. Zero manual steps.
- **Con:** Your laptop needs to be on, online, and running the runner agent
  for jobs to pick up. For the actual 15-min demo, **stop the runner** — you
  don't want a teammate's accidental push to roll the deploy mid-demo.

For most teams the manual `docker compose pull && up` from Step 6 is the
safer and clearer choice. Use the self-hosted runner only if "always-fresh
demo" is genuinely valuable to you.

---

## 5. Pitfalls (commit to memory)

### Pitfall 1 — `403 denied` on `docker pull` from GHCR

The image is **private** by default and your laptop's `docker login` either
hasn't happened or used a token without `read:packages`. Fix: redo Step 4.
If you genuinely want the images public, go to the package page →
**Package settings** → change visibility to public.

### Pitfall 2 — `build:` and `image:` both set on the same service

If a compose service has *both* `build:` and `image:`, docker-compose builds
the image locally and tags it with your `image:` value — **ignoring the
registry**. Either delete the `build:` line or use `docker compose pull`
instead of `up --build`. For the deploy laptop, you want `image:` only.

### Pitfall 3 — `kafka` service name confusion

Inside the compose network, Spring Boot reaches Kafka at `kafka:29092` (the
*internal* listener). From your laptop / Postman it'd be `localhost:9092`
(the *external* listener). Mixing them up gives confusing
`UnknownTopicOrPartitionException` errors. Set
`SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092` in `docker-compose.yml`, never
`localhost:9092`.

### Pitfall 4 — Demo laptop has a stale image

You pushed a fix, CI is green, but the demo laptop still runs the bug. Either
(a) you forgot `docker compose pull` before `up`, or (b) you pinned a SHA in
`.env` that predates the fix. `docker compose pull && docker compose up -d`
always — never just `up`.

### Pitfall 5 — `:latest` tag bites you mid-demo

If you demo with `:latest` and a teammate pushes to `main` right before your
slot, the next `docker compose pull` could surprise you. **Always pin to a
SHA in `.env` for the actual demo window.** Use `:latest` for development
iteration only.

---

## 6. End-of-lab checklist

- [ ] CI workflow pushes both images to GHCR on every push to `main`/`develop`.
- [ ] `https://github.com/<org>/<repo>/pkgs/container/tradeflow-backend` and
      `tradeflow-frontend` both list at least one SHA tag + `latest`.
- [ ] `docker login ghcr.io` succeeded on the demo laptop.
- [ ] `docker compose pull` from a clean clone successfully pulls all 7 services'
      images.
- [ ] `docker compose up -d` brings all 7 services to `Up` (or `Up (healthy)`).
- [ ] `.env` is pinned to a specific SHA for the demo (and is gitignored).
- [ ] All 5 smoke checks from Step 7 pass.
- [ ] You can explain why the backend image lives in GHCR but Kafka/Postgres
      images come straight from Docker Hub.

---

## 7. Common mistakes and errors

### 7.1 CI fails at `docker/login-action` with `403`

**Where you'll see it:** Actions tab → `docker-build` job → "Log in to GHCR" step.

**Message:**
```
Error: buildx failed with: ERROR: failed to push: unexpected status: 403 Forbidden
```

**Cause:** Workflow is missing `permissions: packages: write`. The default
`GITHUB_TOKEN` is read-only.

**Fix:**

> **File to edit:** `.github/workflows/ci.yml`
> **Section:** the `docker-build:` job, top-level.

```yaml
permissions:
  contents: read
  packages: write
```

---

### 7.2 `docker compose up` fails with `pull access denied`

**Where you'll see it:** demo laptop terminal:
```
Error response from daemon: pull access denied for ghcr.io/.../tradeflow-backend,
repository does not exist or may require 'docker login'
```

**Cause:** Either you skipped `docker login ghcr.io` (Step 4), or your token
lacks `read:packages`, or the repo name in `docker-compose.yml` doesn't match
the actual GHCR path.

**Fix:** Verify the package URL exists:
```
https://github.com/<org>/<repo>/pkgs/container/tradeflow-backend
```
If it does, redo `docker login` with a valid `read:packages` token. If it
doesn't, the CI push step never succeeded — check Actions logs.

---

### 7.3 Backend container starts then immediately exits

**Where you'll see it:**

> **Terminal** — run from project root (`tradeflow-studentscopy/`)

```bash
docker compose ps
# backend status: 'Exited (1)' or 'Restarting'
```

**Cause:** Almost always a config issue — bad DB URL, wrong profile, or
Liquibase validation failure.

**Fix:**

> **Terminal** — run from project root (`tradeflow-studentscopy/`)

```bash
docker compose logs backend | tail -50
```

Look for:
- `Connection refused` to `postgres:5432` → `depends_on` is missing
  `condition: service_healthy` on postgres.
- `Validation Failed: ... changesets have changed since they were ran` →
  someone edited an applied Liquibase changeset. See the Day-2 Liquibase
  tutorial §8.4.
- `liquibase.exception.ChangeLogParseException` → bad path in
  `spring.liquibase.change-log`. See Day-2 §6.1.

---

### 7.4 Frontend loads but API calls fail

**Where you'll see it:** browser DevTools → Network tab on the frontend at
`http://localhost:5173`. Requests to `/api/...` return 404 or never resolve.

**Cause:** The frontend was built without `VITE_API_BASE_URL` set, or with
the wrong value (e.g. it points at the dev proxy `/api/v1` but nginx in the
container isn't proxying).

**Fix one — proxy-based (recommended):** Have nginx in the frontend
container proxy `/api/` to `http://backend:8080`. Then the frontend can
fetch `/api/v1/...` relative URLs and they reach the backend.

> **File to edit:** `frontend/nginx.conf`

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    location /api/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
    }

    location /actuator/ {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

**Fix two — env-var-baked:** Set `VITE_API_BASE_URL` at *build time* in CI:

> **File to edit:** `.github/workflows/ci.yml`
> **Section:** the frontend build-and-push step.

```yaml
- name: Build + push frontend image
  uses: docker/build-push-action@v5
  with:
    context: ./frontend
    push: true
    build-args: |
      VITE_API_BASE_URL=http://localhost:8080/api/v1
    tags: ghcr.io/${{ github.repository_owner }}/tradeflow-frontend:latest
```

The proxy approach is more flexible — same image works in dev, staging,
demo without rebuild.

---

### 7.5 Prometheus targets show `recon-service` as DOWN

**Where you'll see it:** `http://localhost:9090/targets`.

**Cause:** The scrape target in `prometheus.yml` points at `localhost:8080`
instead of `backend:8080`.

**Fix:**

> **File to edit:** `monitoring/prometheus/prometheus.yml`

```yaml
  - job_name: 'recon-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']    # NOT localhost:8080
```

Then reload Prometheus:

> **Terminal** — run from project root (`tradeflow-studentscopy/`)

```bash
curl -X POST http://localhost:9090/-/reload
```

See the Day 6 Prometheus tutorial §5.1 for the full "localhost vs service
name" explanation.

---

### 7.6 Grafana dashboard is empty after `docker compose down -v`

**Where you'll see it:** the TradeFlow folder still appears, but custom
panels you added in the UI are gone.

**Cause:** `-v` deletes named volumes, including `grafana-data` which holds
manual UI changes. The dashboards in
`monitoring/grafana/provisioning/dashboards/` are restored from disk on next
boot — but any UI tweaks you didn't export are gone.

**Fix:** Lessons learned. **Always export UI changes** back to
`monitoring/grafana/provisioning/dashboards/api.json` before
`docker compose down -v`. Day 6 tutorial Step 6 covers the export workflow.

---

### 7.7 Image push works but the SHA tag is wrong

**Where you'll see it:** GHCR package page shows a tag like `:refs-heads-main`
instead of the git SHA.

**Cause:** Wrong template variable in the workflow. `github.ref` is the
*branch ref*, not the commit SHA. Use `github.sha`.

**Fix:**

> **File to edit:** `.github/workflows/ci.yml`

```yaml
tags: |
  ghcr.io/${{ github.repository_owner }}/tradeflow-backend:${{ github.sha }}   # not github.ref
  ghcr.io/${{ github.repository_owner }}/tradeflow-backend:latest
```

---

### 7.8 Mental-model mistakes

- **"`docker compose up` IS the CI/CD pipeline."** No — `docker compose up`
  is the *deploy* step. The pipeline is **everything before it**: lint,
  test, image build, image push. The deploy is the tip of the iceberg.
- **"I'll just `build:` locally on the demo laptop."** Then you're building
  on potentially-different infra from CI. The whole point of registry-based
  deploys is "what CI tested is exactly what the laptop runs".
- **"Self-hosted runner = production CD."** It's CD onto a laptop, which is
  fine for this bootcamp's demo. Don't extrapolate the pattern to a real
  product without thinking about: laptop sleeps, network drops, runner
  agent crashes, who owns the laptop on day 91.
- **"I'll worry about pinning the SHA later."** Pin it the moment you set up
  for the demo. The instant before demo is the worst time to discover that
  `:latest` rolled forward in the last 10 minutes.

---

**Back to:** [Day 10 README](./README.md)
