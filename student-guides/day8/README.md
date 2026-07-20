# Day 8 — Vanilla JS (sprints) + React Module 1 + Module 2

> Theme: **AM — Wire the Day-7 dashboard with vanilla JS. PM — Rebuild it in React.**
> Tickets: **15 active** (`I093`, `I095`–`I097`, `I099`–`I104`, `I106`–`I110`) — 4 vanilla JS + 11 React. Tickets `I094`, `I098`, `I105`, `I111` dropped — see "Stretch goals" at the end.
> Modules: React Module 1 — Foundations (AM, first slot) + React Module 2 — State & Events (AM, second slot)

JavaScript Essentials was taught yesterday (Day 7 AM); today you apply it
directly in the AM JS sprint against the static dashboard you shipped.
Then in the PM you rebuild the same dashboard in React. **Two React
modules are now taught back-to-back this morning**: Module 1 (JSX /
Components / Hooks / Router) and Module 2 (useReducer / Context / Forms /
HOC + Render Props) — Module 2 used to live on Day 9 AM, now it lands
here.

> 🔁 **What Module 2 cashes in tomorrow:** today is dense (15 tickets +
> 2 React modules). `Context`, `HOC` and the modal pattern from Module 2
> don't have project tickets today, but they ARE applied on Day 9 via
> `I124A`–`I124D` (BreakContext, ErrorBoundary, ResolveBreakModal,
> withAuditLog HOC). Today: ground the concepts; tomorrow: ship the
> artefacts.

By end of day:

- The 4 Day-7 HTML pages now fetch live data, sort, validate and POST — all in vanilla JS.
- Vite + React project boots on `localhost:5173`.
- Custom hooks fetch trades and recon results.
- All UI is split into reusable components; React Router moves between pages without a full reload.
- React Module 2 concepts (`useReducer`, `Context`, RHF, HOC, Render Props) covered in classroom; the project applies `useReducer` + RHF but not yet Context / HOC.

> **Pacing tip.** AM ends when I093–I098 are green and the static dashboard
> shows live data. After lunch, the React rebuild begins from scratch — same
> dashboard, new architecture. Feel the difference; it's the point of today.

---

## Sprint 7 — Vanilla JavaScript (Day 8 AM)

You shipped 4 styled HTML pages on Day 7. Today AM you wire them to
live data — fetch, sort, validate and POST — in vanilla JS. This
is intentionally pre-React: you'll feel the manual DOM + ad-hoc
state pain that React solves after lunch.

---

### Just-in-time primer — Vanilla JavaScript in 5 minutes

The next six tickets (I093–I098) are pure browser JavaScript — no React yet.
If you've only seen JS in passing, here's the minimum you need before you
open `trades.js`.

**Variables & types.** `const` for things that don't get reassigned, `let`
for things that do. Never `var`. Types are implicit: `"OPEN"`, `42`,
`true`, `null`, arrays `[...]`, objects `{ key: value }`.

**Functions — three shapes you'll see.**

```javascript
function loadTrades() { /* ... */ }            // classic declaration
const loadTrades = () => { /* ... */ };        // arrow function
async function loadTrades() { await fetch(...); } // async = returns a Promise
```

Arrow functions are concise; use them for callbacks. `async`/`await` is how
you read a Promise's value without nesting `.then()`.

**The DOM — finding and changing elements.**

| Goal | API |
|---|---|
| Find ONE element by CSS selector | `document.querySelector('#trades-tbody')` |
| Find MANY elements | `document.querySelectorAll('.badge')` |
| Read / write inner content | `el.textContent = "Loading…"` / `el.innerHTML = "<tr>…</tr>"` |
| Add/remove a CSS class | `el.classList.add('hidden')` / `.remove(...)` / `.toggle(...)` |
| Hide / show a `display:none` element | `el.classList.remove('hidden')` or set `el.hidden = false` |

`textContent` is safe (no HTML parsing). `innerHTML` is faster for table
rows but you MUST escape any user-supplied string first
(`escapeHtml(t.tradeRef)`) — otherwise that's an XSS hole.

**Events — listening for clicks, submits, page-ready.**

```javascript
document.addEventListener("DOMContentLoaded", loadTrades);
button.addEventListener("click", (event) => { event.preventDefault(); /* ... */ });
form.addEventListener("submit", handleSubmit);
```

`event.preventDefault()` stops the browser's default behaviour (e.g. a form
auto-submitting and reloading the page).

**Fetch — talking to the backend.**

```javascript
const res = await fetch(`${API_BASE}/trades?page=0&size=20`, {
  headers: { Authorization: AUTH_HEADER }   // "Basic " + btoa("user:pw")
});
if (!res.ok) throw new Error(`HTTP ${res.status}`);
const page = await res.json();              // { content: [...], totalPages, ... }
const rows = page.content;
```

Three things every fetch needs:
1. **`await`** — `fetch` returns a Promise; await unwraps it.
2. **Check `res.ok`** — fetch only rejects on network failure, not on 4xx/5xx.
   You throw yourself.
3. **Parse the body** — `await res.json()` (or `.text()`).

**The pattern every Day-7 JS file follows.**

```javascript
async function load() {
  showLoading();
  hideError();
  try {
    const res = await fetch(URL, { headers });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    render(data);
  } catch (err) {
    showError(err.message);
  } finally {
    hideLoading();
  }
}
document.addEventListener("DOMContentLoaded", load);
```

`try / catch / finally` gives you loading, error, and "always hide spinner"
in one block. Every fetch in I093–I098 uses this shape.

**Gotchas to expect:**

| Symptom | Cause | Fix |
|---|---|---|
| `Failed to fetch` in console | Backend on `localhost:8080`, page on `:5173` — CORS | Backend already has `@CrossOrigin` from Day 6; if not, add `--cors` proxy |
| `401 Unauthorized` | Missing `Authorization` header or wrong creds | `"Basic " + btoa("viewer:viewer-pw")` — check the seeded users from I076 |
| Table renders `[object Object]` | You forgot `.content` from the `Page<T>` envelope | `page.content` is the array, `page` itself is metadata |
| Form reloads the page on submit | You forgot `event.preventDefault()` | Add it as the first line of the handler |

That's enough JS to ship I093–I098. Code is in the ticket hints.

---

### TICKET-I093 — `trades.js` — fetch trades

**What**
- `static-dashboard/js/trades.js` calls `GET /api/v1/trades` with a Basic-Auth header and renders the `page.content` array into the `<tbody>` of the trades table.

**Why**
- This is the manual fetch-then-render loop that I100's `apiService.js` will centralise and I101's `useTradeData` hook will wrap with a `useEffect` — students need to feel the boilerplate before React abstracts it away.

**Observe**
- Browser shows real trade rows (not "Loading…"); DevTools Network tab logs `GET /api/v1/trades?page=0&size=20` returning 200 with a `{ content: [...], totalPages }` body.

**Acceptance criteria:**
- [ ] `fetch()` with `Authorization: Basic <base64>`.
- [ ] Renders rows into `<tbody>` of the trades table.
- [ ] Loading state shown while pending.
- [ ] Error state shown on 4xx/5xx.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- Hard-code the auth header for now: `"Basic " + btoa("viewer:viewer-pw")`.
  Discuss with your team WHY this is a temporary hack (creds in source = bad).
- Wrap the fetch in `try / catch / finally`: show loading in `try`'s
  prologue, hide it in `finally`. Errors go to the `#trades-error` div.
- The API returns a Spring Data `Page<T>` envelope: `{ content: [...], totalPages, totalElements, ... }`. Pull `page.content` for the rows.
- Use `tbody.innerHTML = items.map(rowHtml).join("")` — fast enough for ~100 rows.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Structure:

1. Module-level constants — `API_BASE`, `AUTH_HEADER`.
2. On `DOMContentLoaded`, call `loadTrades()`.
3. `loadTrades()`:
   - Show `#trades-loading`, hide `#trades-error`.
   - `fetch(url, { headers: { Authorization: AUTH_HEADER } })`.
   - Throw on `!res.ok` — your catch will show the error div.
   - Parse JSON, extract `page.content`.
   - Call `render()` to write rows.
   - `finally` — hide loading.
4. `rowHtml(t)` builds one `<tr>` template-literal per trade.
5. Always escape user-visible strings — `escapeHtml(t.tradeRef)` — habit.
</details>

<details>
<summary>Hint 3 — JS skeleton</summary>

```javascript
const API_BASE = "http://localhost:8080/api/v1";
const AUTH_HEADER = "Basic " + btoa("viewer:viewer-pw");

document.addEventListener("DOMContentLoaded", loadTrades);

async function loadTrades() {
  const loading  = document.getElementById("trades-loading");
  const errorDiv = document.getElementById("trades-error");
  loading.classList.remove("hidden");
  errorDiv.classList.add("hidden");

  try {
    // TODO: fetch /trades with Authorization header
    // TODO: throw on !res.ok
    // TODO: parse JSON; pull page.content
    // TODO: tbody.innerHTML = items.map(rowHtml).join("")
  } catch (e) {
    errorDiv.textContent = "Could not load trades: " + e.message;
    errorDiv.classList.remove("hidden");
  } finally {
    loading.classList.add("hidden");
  }
}

function rowHtml(t) {
  // TODO: return a <tr> string with all 7 columns + badge span on status
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `static-dashboard/js/trades.js`

```javascript
const API_BASE = "http://localhost:8080/api/v1";
const AUTH_HEADER = "Basic " + btoa("viewer:viewer-pw");
const PAGE_SIZE = 20;

let pageContent = [];

document.addEventListener("DOMContentLoaded", loadTrades);

async function loadTrades() {
    const loading  = document.getElementById("trades-loading");
    const errorDiv = document.getElementById("trades-error");
    loading.classList.remove("hidden");
    errorDiv.classList.add("hidden");

    try {
        const url = `${API_BASE}/trades?size=${PAGE_SIZE}`;
        const res = await fetch(url, { headers: { "Authorization": AUTH_HEADER } });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const page = await res.json();
        pageContent = page.content || page;
        render();
    } catch (e) {
        errorDiv.textContent = "Could not load trades: " + e.message;
        errorDiv.classList.remove("hidden");
    } finally {
        loading.classList.add("hidden");
    }
}

function render() {
    const tbody = document.getElementById("trades-tbody");
    tbody.innerHTML = pageContent.map(rowHtml).join("");
}

function rowHtml(t) {
    const badgeClass = "badge-" + (t.status || "pending").toLowerCase();
    return `
        <tr>
            <td>${escapeHtml(t.tradeRef)}</td>
            <td>${t.instrumentId}</td>
            <td>${t.counterpartyId}</td>
            <td>${formatNumber(t.quantity)}</td>
            <td>${formatNumber(t.price)}</td>
            <td>${t.tradeDate}</td>
            <td><span class="badge ${badgeClass}">${t.status ?? "PENDING"}</span></td>
        </tr>
    `;
}

function formatNumber(n) {
    if (n == null) return "";
    const num = Number(n);
    return Number.isFinite(num) ? num.toLocaleString("en-GB", { maximumFractionDigits: 4 }) : String(n);
}

function escapeHtml(s) {
    return String(s ?? "").replace(/[&<>"']/g, c => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;"
    }[c]));
}
```
</details>

**Files to touch:** `static-dashboard/js/trades.js`.

---

### TICKET-I095 — `addTrade.js` — client-side validation

**What**
- `static-dashboard/js/addTrade.js` runs a `validate(data)` pass on submit that flags empty fields, non-positive numerics, and future dates with inline `.field-error` spans.

**Why**
- This hand-rolled `clearErrors / setError / let ok = true` pattern is exactly what I106's React Hook Form `register(name, { required, min, validate })` replaces — students who write it now will see why RHF saves them on the PM rebuild.

**Observe**
- Submitting an empty form fills each `.field-error[data-for="..."]` span with "required"; entering `0` in quantity surfaces "must be > 0" inline; the Network tab stays empty because the POST is gated behind `validate()` returning `false`.

**Acceptance criteria:**
- [ ] All fields required.
- [ ] `quantity > 0`, `price > 0`.
- [ ] `trade_date` must be today or earlier.
- [ ] Inline error messages near each invalid field.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- `evt.preventDefault()` first thing in the submit handler — otherwise the
  browser will GET-navigate away.
- `Object.fromEntries(new FormData(form).entries())` turns the form into a
  plain object keyed by `name`.
- One `validate(data)` function returns `true`/`false`. On `false`, return
  early — don't POST anything.
- Inline errors: `document.querySelector('.field-error[data-for="X"]').textContent = msg`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

The validate function:

1. Call `clearErrors()` first — wipes any previous error spans.
2. Set a local `let ok = true;`.
3. For each field, run the rule. On failure: `setError(name, msg); ok = false;`.
4. Required fields: empty string / missing → `"required"`.
5. Numerics: `Number(data.quantity) > 0` → `"must be > 0"`.
6. Date: `new Date(data.tradeDate) > new Date()` → `"must not be in the future"`.
7. Return `ok`.

Helper trio:
- `clearErrors()` — loops every `.field-error` and sets `textContent = ""`.
- `setError(field, msg)` — finds by `data-for`, sets `textContent`.
- (Later, in I096) `showToast(msg, isError)` — fills `#form-feedback`.
</details>

<details>
<summary>Hint 3 — JS skeleton</summary>

```javascript
document.addEventListener("DOMContentLoaded", () => {
  document.getElementById("trade-form").addEventListener("submit", onSubmit);
});

function onSubmit(evt) {
  evt.preventDefault();
  const data = Object.fromEntries(new FormData(evt.target).entries());
  if (!validate(data)) return;
  // TODO (I096): POST to /api/v1/trades
}

function validate(data) {
  clearErrors();
  let ok = true;
  // TODO: required checks for tradeRef, instrumentId, counterpartyId
  // TODO: quantity > 0, price > 0
  // TODO: tradeDate must not be in the future
  return ok;
}

function clearErrors() {
  document.querySelectorAll(".field-error").forEach(s => s.textContent = "");
}
function setError(field, msg) {
  const el = document.querySelector(`.field-error[data-for="${field}"]`);
  if (el) el.textContent = msg;
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `static-dashboard/js/addTrade.js`

```javascript
const API_BASE = "http://localhost:8080/api/v1";
const AUTH_HEADER = "Basic " + btoa("trader:trader-pw");

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("trade-form").addEventListener("submit", onSubmit);
});

function onSubmit(evt) {
    evt.preventDefault();
    const form = evt.target;
    const data = Object.fromEntries(new FormData(form).entries());
    if (!validate(data)) return;
    // I096 adds the POST here.
    console.log("Validated form data:", data);
}

function validate(data) {
    clearErrors();
    let ok = true;

    if (!data.tradeRef || !data.tradeRef.trim()) {
        setError("tradeRef", "required"); ok = false;
    }
    if (!data.instrumentId)   { setError("instrumentId",   "required"); ok = false; }
    if (!data.counterpartyId) { setError("counterpartyId", "required"); ok = false; }

    if (!data.quantity || Number(data.quantity) <= 0) {
        setError("quantity", "must be > 0"); ok = false;
    }
    if (!data.price || Number(data.price) <= 0) {
        setError("price", "must be > 0"); ok = false;
    }
    if (!data.tradeDate) {
        setError("tradeDate", "required"); ok = false;
    } else if (new Date(data.tradeDate) > new Date()) {
        setError("tradeDate", "must not be in the future"); ok = false;
    }

    return ok;
}

function clearErrors() {
    document.querySelectorAll(".field-error").forEach(s => s.textContent = "");
}

function setError(field, msg) {
    const el = document.querySelector(`.field-error[data-for="${field}"]`);
    if (el) el.textContent = msg;
}
```
</details>

**Files to touch:** `js/addTrade.js`.

---

### TICKET-I096 — `addTrade.js` — POST to API

**What**
- After `validate()` passes, `onSubmit` sends a JSON `POST /api/v1/trades`, parses the GlobalExceptionHandler error envelope on failure, and redirects to `trades.html` on success.

**Why**
- Surfacing `body.details` back into per-field `.field-error` spans is the contract React Hook Form's `setError(field, { message })` will honour in I106 — same backend envelope, different consumer.

**Observe**
- A valid submit shows the green toast in `#form-feedback` and `location.href` flips to `trades.html`; a duplicate `tradeRef` triggers a red toast carrying the backend's `body.message` and the Network tab shows a 400 with the JSON envelope.

**Acceptance criteria:**
- [ ] Submits JSON to `POST /api/v1/trades`.
- [ ] Success: toast notification + redirect to trades.html.
- [ ] Error: parse the error envelope, show the `message`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- `fetch(url, { method: "POST", headers: {...}, body: JSON.stringify(payload) })`.
- Two headers: `Authorization: Basic ...` AND `Content-Type: application/json`.
- On `!res.ok`, parse the error body and look for `body.message` and
  `body.details` (the GlobalExceptionHandler from Day 6 envelope).
- On success, show a toast via `#form-feedback`, then redirect with
  `setTimeout(() => location.href = "trades.html", 800)`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Extend `onSubmit` after the validate check:

1. Disable Submit button to prevent double-submit.
2. `await fetch(...)` — JSON body matches the API DTO: `tradeRef`,
   `instrumentId` (number), `counterpartyId` (number), `quantity`, `price`,
   `tradeDate`.
3. `if (!res.ok)`:
   - `const body = await res.json().catch(() => ({}));`
   - If `body.details` is an object (field-level errors): loop and
     `setError(field, msg)`.
   - Throw with `body.message || HTTP ${res.status}`.
4. On success: `showToast("Trade created — redirecting…")` then redirect.
5. `finally` re-enables the button.

Add a `showToast(msg, isError)` helper — fills `#form-feedback`, switches
border colour based on `isError`.
</details>

<details>
<summary>Hint 3 — JS skeleton</summary>

```javascript
async function onSubmit(evt) {
  evt.preventDefault();
  const form = evt.target;
  const data = Object.fromEntries(new FormData(form).entries());
  if (!validate(data)) return;

  const submitBtn = form.querySelector("button[type=submit]");
  submitBtn.disabled = true;

  try {
    const res = await fetch(`${API_BASE}/trades`, {
      method: "POST",
      headers: {
        "Authorization": AUTH_HEADER,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        tradeRef:       data.tradeRef.trim(),
        instrumentId:   Number(data.instrumentId),
        counterpartyId: Number(data.counterpartyId),
        // TODO: quantity, price, tradeDate
      })
    });

    if (!res.ok) {
      // TODO: parse body, surface body.details + throw body.message
    }

    showToast("Trade created — redirecting…");
    setTimeout(() => location.href = "trades.html", 800);
  } catch (e) {
    showToast("Error: " + e.message, true);
  } finally {
    submitBtn.disabled = false;
  }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `static-dashboard/js/addTrade.js` (replace `onSubmit` and add helpers)

```javascript
async function onSubmit(evt) {
    evt.preventDefault();
    const form = evt.target;
    const data = Object.fromEntries(new FormData(form).entries());

    if (!validate(data)) return;

    const submitBtn = form.querySelector("button[type=submit]");
    submitBtn.disabled = true;

    try {
        const res = await fetch(`${API_BASE}/trades`, {
            method: "POST",
            headers: {
                "Authorization": AUTH_HEADER,
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                tradeRef:        data.tradeRef.trim(),
                instrumentId:    Number(data.instrumentId),
                counterpartyId:  Number(data.counterpartyId),
                quantity:        data.quantity,
                price:           data.price,
                tradeDate:       data.tradeDate
            })
        });

        if (!res.ok) {
            const body = await res.json().catch(() => ({}));
            // Surface field-level errors from the GlobalExceptionHandler envelope.
            if (body.details) {
                Object.entries(body.details).forEach(([f, msg]) => setError(f, msg));
            }
            throw new Error(body.message || `HTTP ${res.status}`);
        }

        showToast("Trade created — redirecting…");
        setTimeout(() => { location.href = "trades.html"; }, 800);
    } catch (e) {
        showToast("Error: " + e.message, true);
    } finally {
        submitBtn.disabled = false;
    }
}

function showToast(msg, isError = false) {
    const t = document.getElementById("form-feedback");
    t.textContent = msg;
    t.style.borderLeftColor = isError ? "#c62828" : "#003366";
    t.classList.remove("hidden");
}
```
</details>

**Files to touch:** `js/addTrade.js`.

---

### TICKET-I097 — `recon.js` — render results

**What**
- `static-dashboard/js/recon.js` fetches `GET /api/v1/recon/results?status=OPEN`, renders a row per break with a status badge, and wires a Resolve button that fires `PUT /api/v1/recon/{id}/resolve` and removes the row on success.

**Why**
- Tearing the row out of the DOM immediately after the PUT is the manual version of I107's optimistic-update map — students who feel this DOM-mutation pain understand why the React rebuild keeps an `optimistic = { [id]: status }` overlay instead.

**Observe**
- Page lists OPEN breaks with red `unmatched` pills; clicking Resolve fires a 200 on the PUT (visible in Network tab) and the row disappears without a full page reload.

**Acceptance criteria:**
- [ ] Fetch `GET /api/v1/recon/results?status=OPEN`.
- [ ] Render rows with badges.
- [ ] "Resolve" button calls `PUT /api/v1/recon/{id}/resolve`, removes row on success.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- Same load/loading/error pattern as `trades.js`.
- Use event delegation on `<tbody>` — ONE click handler that checks
  `evt.target.dataset.action === "resolve"`. Fewer leaks than per-row
  listeners.
- Store the row id via `<tr data-id="${r.id}">`.
- On a successful PUT, `row.remove()` — no full re-fetch needed.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Structure:

1. `loadBreaks()` — show loading, fetch `/recon/results?status=OPEN`,
   parse `page.content` (or array), inject rows, attach delegated click.
2. `rowHtml(r)` — `<tr data-id>` with 5 cells, last one is the Resolve button.
3. `onResolveClick(evt)` — guard on the dataset attr, find the parent
   row, `await fetch PUT /recon/{id}/resolve`, on success `row.remove()`.
4. Disable the button during the PUT so impatient users can't double-fire.

Use `new Date(r.detectedAt).toLocaleString("en-GB")` to format the
timestamp — easier on the eye than ISO 8601.
</details>

<details>
<summary>Hint 3 — JS skeleton</summary>

```javascript
const API_BASE = "http://localhost:8080/api/v1";
const AUTH_HEADER = "Basic " + btoa("trader:trader-pw");

document.addEventListener("DOMContentLoaded", loadBreaks);

async function loadBreaks() {
  const tbody = document.getElementById("recon-tbody");
  const loading = document.getElementById("recon-loading");
  const errorDiv = document.getElementById("recon-error");

  loading.classList.remove("hidden");
  errorDiv.classList.add("hidden");

  try {
    // TODO: fetch /recon/results?status=OPEN
    // TODO: tbody.innerHTML = items.map(rowHtml).join("")
    // TODO: tbody.addEventListener("click", onResolveClick)
  } catch (e) {
    errorDiv.textContent = "Could not load breaks: " + e.message;
    errorDiv.classList.remove("hidden");
  } finally {
    loading.classList.add("hidden");
  }
}

function rowHtml(r) { /* TODO: 5 <td>s, badge + Resolve button */ }
async function onResolveClick(evt) { /* TODO: PUT, then row.remove() */ }
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `static-dashboard/js/recon.js`

```javascript
const API_BASE = "http://localhost:8080/api/v1";
const AUTH_HEADER = "Basic " + btoa("trader:trader-pw");

document.addEventListener("DOMContentLoaded", loadBreaks);

async function loadBreaks() {
    const tbody = document.getElementById("recon-tbody");
    const loading = document.getElementById("recon-loading");
    const errorDiv = document.getElementById("recon-error");

    loading.classList.remove("hidden");
    errorDiv.classList.add("hidden");

    try {
        const res = await fetch(`${API_BASE}/recon/results?status=OPEN`, {
            headers: { "Authorization": AUTH_HEADER }
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        const items = data.content || data;
        tbody.innerHTML = items.map(rowHtml).join("");
        tbody.addEventListener("click", onResolveClick);
    } catch (e) {
        errorDiv.textContent = "Could not load breaks: " + e.message;
        errorDiv.classList.remove("hidden");
    } finally {
        loading.classList.add("hidden");
    }
}

function rowHtml(r) {
    const detected = r.detectedAt ? new Date(r.detectedAt).toLocaleString("en-GB") : "";
    return `
        <tr data-id="${r.id}">
            <td>${r.tradeRef ?? r.tradeId ?? "—"}</td>
            <td>${r.discrepancyType ?? "—"}</td>
            <td><span class="badge badge-open">${r.status}</span></td>
            <td>${detected}</td>
            <td><button data-action="resolve">Resolve</button></td>
        </tr>
    `;
}

async function onResolveClick(evt) {
    const btn = evt.target;
    if (btn.dataset.action !== "resolve") return;
    const row = btn.closest("tr");
    const id = row.dataset.id;

    btn.disabled = true;
    try {
        const res = await fetch(`${API_BASE}/recon/${id}/resolve`, {
            method: "PUT",
            headers: { "Authorization": AUTH_HEADER }
        });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        row.remove();
    } catch (e) {
        alert("Resolve failed: " + e.message);
        btn.disabled = false;
    }
}
```
</details>

**Files to touch:** `js/recon.js`.

---

## Sprint 8 — React Frontend (Day 8 PM)

### TICKET-I099 — Create React app + structure

**What**
- Bootstrap a Vite + React 18 project under `frontend/` with the 5-folder layout (`components/`, `hooks/`, `services/`, `pages/`, `styles/`) and React Router v6 + react-hook-form installed.

**Why**
- Every subsequent PM ticket assumes this folder layout: I100 lives in `services/`, I101–I102 in `hooks/`, I103/I107 in `pages/`, I104/I106/I108 in `components/` — getting the skeleton right now prevents `../../../` import spaghetti later.

**Observe**
- `npm run dev` boots on `http://localhost:5173`, the React placeholder renders without console errors, and `ls src/` shows all 5 directories.

**Acceptance criteria:**
- [ ] `npm run dev` boots on port 5173.
- [ ] Folders: `src/components`, `src/hooks`, `src/services`, `src/pages`, `src/styles`.
- [ ] React Router v6 installed.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Vite is the chosen bundler — faster cold start than CRA, native ESM. The
project layout matters as much as the boot: separating `components/`,
`hooks/`, `services/`, `pages/`, `styles/` keeps cross-imports sane as the
app grows.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. `npm create vite@latest frontend -- --template react`.
2. `cd frontend && npm install` then add `react-router-dom@^6.22`.
3. Create the 5 folders under `src/`: `components/`, `hooks/`, `services/`,
   `pages/`, `styles/`.
4. Move the Vite-generated `index.css` content into `src/styles/global.css`
   and import it from `main.jsx`.
5. `npm run dev` — open `http://localhost:5173`. Should show the React
   placeholder page.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```json
{
  "name": "tradeflow-recon-ui",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1"
    // TODO: add react-router-dom
    // TODO: add react-hook-form (used in I106)
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^4.2.1",
    "vite": "^5.2.7"
  }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit:** `frontend/package.json`, `frontend/src/main.jsx`, `frontend/src/App.jsx`, `frontend/.gitignore`

```json
// frontend/package.json
{
  "name": "tradeflow-recon-ui",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vite build",
    "preview": "vite preview",
    "test": "vitest --passWithNoTests"
  },
  "dependencies": {
    "react": "^18.3.1",
    "react-dom": "^18.3.1",
    "react-hook-form": "^7.51.0",
    "react-router-dom": "^6.22.3"
  },
  "devDependencies": {
    "@vitejs/plugin-react": "^4.2.1",
    "vite": "^5.2.7",
    "vitest": "^1.4.0"
  }
}
```

```jsx
// frontend/src/main.jsx
import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import './styles/global.css';

const root = createRoot(document.getElementById('root'));

root.render(
    <React.StrictMode>
        <BrowserRouter>
            <App />
        </BrowserRouter>
    </React.StrictMode>
);
```

```jsx
// frontend/src/App.jsx — stub until I110 fills in the routes
export default function App() {
    return <h1>TradeFlow — Hello</h1>;
}
```

```gitignore
# frontend/.gitignore
node_modules/
dist/
.vite/
```
</details>

**Files to touch:** `frontend/package.json`, `frontend/src/`.

---

### TICKET-I100 — `apiService.js`

**What**
- `frontend/src/services/apiService.js` exports `getTrades`, `createTrade`, `getReconResults`, `resolveBreak` — every call goes through one fetch wrapper that injects the Basic-Auth header and throws a typed `ApiError({ status, body })` on non-2xx.

**Why**
- This is the React equivalent of the AM's `API_BASE` + `AUTH_HEADER` constants from I093/I096 — centralising it now means I101's hook and I106's form share one error envelope and one place to swap creds when Day 9 lands JWT auth.

**Observe**
- `import { getTrades } from './services/apiService.js'` then calling it from the console returns the paged response; killing the backend and retrying surfaces an `ApiError` with `status: undefined` and a network-failure body, not a raw `TypeError`.

**Acceptance criteria:**
- [ ] Functions: `getTrades(params)`, `createTrade(body)`,
  `getReconResults(params)`, `resolveBreak(id)`.
- [ ] Auth header injected once.
- [ ] On non-2xx, throws an `ApiError` with `status` and `body`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Centralise every backend call here so components never touch `fetch`
directly. One place for the auth header, one place for the base URL, one
place to translate non-2xx responses into a typed error. Use vanilla
`fetch` — don't pull axios in.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. Read the base URL from `import.meta.env.VITE_API_BASE_URL` with a
   sensible fallback (`'/api/v1'`).
2. Hard-code a Basic-Auth header (`trader:trader-pw` works against the
   Phase-2.5 backend — TRADER role is required for POST/PUT/DELETE).
3. A private `request(path, options)` wrapper: merges headers, parses JSON,
   throws `ApiError` on non-2xx.
4. Export thin named functions per endpoint — they all delegate to `request`.
5. Define `ApiError extends Error` with `status` + `body` so callers can
   surface meaningful messages.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```javascript
const BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1';
const AUTH = 'Basic ' + btoa('trader:trader-pw');

export class ApiError extends Error {
    // TODO: status + body on the instance
}

async function request(path, options = {}) {
    // TODO: fetch with merged headers
    // TODO: if !res.ok throw new ApiError(res.status, body)
    // TODO: handle 204 (no content) -> return null
}

export const getTrades       = (params = {}) => /* TODO */;
export const createTrade     = (body)        => /* TODO */;
export const getReconResults = (params = {}) => /* TODO */;
export const resolveBreak    = (id)          => /* TODO */;
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `frontend/src/services/apiService.js`

```javascript
const BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1';

// Hard-coded TRADER credentials for Day 8. A real login flow would replace
// this in a later phase. TRADER role is required for POST/PUT/DELETE.
const AUTH = 'Basic ' + btoa('trader:trader-pw');

export class ApiError extends Error {
    constructor(status, body) {
        super(body?.message || `HTTP ${status}`);
        this.status = status;
        this.body = body;
    }
}

async function request(path, options = {}) {
    const res = await fetch(BASE + path, {
        ...options,
        headers: {
            'Content-Type': 'application/json',
            'Authorization': AUTH,
            ...(options.headers || {})
        }
    });

    if (!res.ok) {
        const body = await res.json().catch(() => ({}));
        throw new ApiError(res.status, body);
    }

    if (res.status === 204) return null;
    return res.json();
}

// ----- Trades --------------------------------------------------------------
export const getTrades       = (params = {}) =>
    request('/trades?' + new URLSearchParams(params).toString());

export const createTrade     = (body) =>
    request('/trades', { method: 'POST', body: JSON.stringify(body) });

// ----- Recon ---------------------------------------------------------------
export const getReconResults = (params = {}) =>
    request('/recon/results?' + new URLSearchParams(params).toString());

export const resolveBreak    = (id) =>
    request(`/recon/${id}/resolve`, { method: 'PUT' });
```
</details>

**Files to touch:** `frontend/src/services/apiService.js`.

---

### TICKET-I101 — `useTradeData(filters)` custom hook

**What**
- `frontend/src/hooks/useTradeData.js` returns `{ trades, loading, error, refetch }`, refetches on `filters` change via a `JSON.stringify` dep key, and uses a `useRef` request counter to drop stale responses.

**Why**
- This hook is the direct replacement for I093's `DOMContentLoaded`-then-fetch loop — students compare the imperative vanilla AM version against the declarative `useEffect` PM version line-for-line; it's the centrepiece of the "what React buys you" lesson.

**Observe**
- React DevTools shows `useTradeData`'s state flipping `loading: true → false` and `trades: []` filling with rows; changing a filter triggers exactly one new `GET /api/v1/trades` in the Network tab (not an infinite loop, which is the I101 gotcha).

**Acceptance criteria:**
- [ ] Returns `{ trades, loading, error, refetch }`.
- [ ] Refetches when `filters` object changes (use `useEffect` deps).
- [ ] Cancels in-flight requests on unmount (`AbortController`).

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

A custom hook is just a function that calls other hooks. Wrap the fetch
lifecycle (loading / data / error) so components stay declarative.
StrictMode mounts components twice in dev — that surfaces race-condition
bugs immediately, which is why `AbortController` + a request-counter
matter.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. `useState` for `trades`, `loading`, `error`.
2. A `useCallback` `refetch` that calls `getTrades(filters)`.
3. `useEffect` that calls `refetch` on mount and whenever the dep changes.
4. The dep can't be the `filters` object directly — a new object on every
   render triggers infinite refetches. Use `JSON.stringify(filters)` as a
   stable key.
5. Track the latest request id with `useRef` — drop the result if a newer
   request was started while this one was in flight.
6. Memoise `filters` in the **caller** with `useMemo` — the request-id
   guard will not protect you if the caller reconstructs the object each
   render.
</details>

<details>
<summary>Hint 3 — Hook skeleton</summary>

```javascript
import { useCallback, useEffect, useRef, useState } from 'react';
import { getTrades } from '../services/apiService.js';

export function useTradeData(filters = {}) {
    const [trades, setTrades] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const filterKey = JSON.stringify(filters);
    const lastRequest = useRef(0);

    const refetch = useCallback(async () => {
        // TODO: bump lastRequest, setLoading(true), null out error
        // TODO: call getTrades(filters)
        // TODO: drop result if a newer request has started
        // TODO: setError on catch, setLoading(false) on finally
    }, [filterKey]);

    useEffect(() => { refetch(); }, [refetch]);

    return { trades, loading, error, refetch };
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `frontend/src/hooks/useTradeData.js`

```javascript
import { useCallback, useEffect, useRef, useState } from 'react';
import { getTrades } from '../services/apiService.js';

export function useTradeData(filters = {}) {
    const [trades, setTrades] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const filterKey = JSON.stringify(filters);
    const lastRequest = useRef(0);
    const mounted = useRef(true);

    const refetch = useCallback(async () => {
        const myReq = ++lastRequest.current;

        setLoading(true);
        setError(null);

        try {
            const page = await getTrades(filters);
            // Drop the result if a newer request has been started or we unmounted.
            if (!mounted.current || myReq !== lastRequest.current) return;
            setTrades(page.content || page);
        } catch (e) {
            if (!mounted.current || myReq !== lastRequest.current) return;
            setError(e);
        } finally {
            if (mounted.current && myReq === lastRequest.current) setLoading(false);
        }
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [filterKey]);

    useEffect(() => {
        mounted.current = true;
        refetch();
        return () => { mounted.current = false; };
    }, [refetch]);

    return { trades, loading, error, refetch };
}
```
</details>

**Files to touch:** `frontend/src/hooks/useTradeData.js`.

---

### TICKET-I102 — `useReconResults(status)` hook

**What**
- `frontend/src/hooks/useReconResults.js` mirrors `useTradeData` but for `/api/v1/recon/results`, takes a single `status` string dep, and returns `{ results, loading, error, refetch }`.

**Why**
- Building the same hook twice (with one different dep shape) cements the custom-hook pattern as the unit of reuse — I103's dashboard then composes both hooks side-by-side without duplicating any fetch logic.

**Observe**
- Dashboard mounts fire two parallel `GET /api/v1/recon/results?status=...` calls (one per hook instance) visible in the Network tab; `results.length` populates within React DevTools without the component re-rendering more than 2-3 times.

**Acceptance criteria:**
- [ ] Mirror of `useTradeData` for `/api/v1/recon/results`.
- [ ] Returns `{ results, loading, error, refetch }`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Same shape as `useTradeData`, simpler dep (just a status string, so no
`JSON.stringify` needed). The point of having two near-identical hooks is
to keep the API surface small — pages call one hook, get the data they
need.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. `useState` for `results`, `loading`, `error`.
2. `useCallback(refetch)` keyed on `status`.
3. `useEffect` runs `refetch` whenever `refetch` changes (which only
   happens when `status` changes).
4. No `useRef` race-counter needed at this depth — single-string dep keeps
   it simple. Add it later if you see stale data.
</details>

<details>
<summary>Hint 3 — Hook skeleton</summary>

```javascript
import { useCallback, useEffect, useState } from 'react';
import { getReconResults } from '../services/apiService.js';

export function useReconResults(status = 'OPEN') {
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const refetch = useCallback(async () => {
        // TODO: setLoading(true), null out error
        // TODO: call getReconResults({ status })
        // TODO: setResults(page.content || page) on success
        // TODO: setError(e) on catch, setLoading(false) on finally
    }, [status]);

    useEffect(() => { refetch(); }, [refetch]);

    return { results, loading, error, refetch };
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `frontend/src/hooks/useReconResults.js`

```javascript
import { useCallback, useEffect, useState } from 'react';
import { getReconResults } from '../services/apiService.js';

export function useReconResults(status = 'OPEN') {
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const refetch = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const page = await getReconResults({ status });
            setResults(page.content || page);
        } catch (e) {
            setError(e);
        } finally {
            setLoading(false);
        }
    }, [status]);

    useEffect(() => { refetch(); }, [refetch]);

    return { results, loading, error, refetch };
}
```
</details>

**Files to touch:** `frontend/src/hooks/useReconResults.js`.

---

### TICKET-I103 — `<TradeDashboard />` page

**What**
- `frontend/src/pages/Dashboard.jsx` composes `useTradeData` + two `useReconResults` calls, computes total/matched%/unmatched/avg-resolution-hours, and renders 4 `<StatCard />`s with a 30-second `setInterval` refresh.

**Why**
- This is where students first compose multiple custom hooks in one component — and where they discover that forgetting `useMemo` on `filters = { size: 500 }` triggers a refetch every render; the `setInterval` cleanup also previews the lifecycle pattern they will lean on Day 10 for WebSocket subscriptions.

**Observe**
- Dashboard renders 4 stat cards with numeric values; the Network tab shows trades + recon requests firing once at mount, then exactly every 30 seconds; navigating away and back does not leak the interval (no doubled-up requests).

**Acceptance criteria:**
- [ ] 4 `<StatCard />`s: Total Trades, Matched %, Unmatched Count, Avg Processing Time.
- [ ] Cards refresh every 30s.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

The dashboard combines both hooks — `useTradeData` for trade totals,
`useReconResults` for break counts. Auto-refresh is a `setInterval` inside
`useEffect` with cleanup. Don't forget the cleanup — without it you leak
intervals on every navigation.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. Pull `trades` from `useTradeData({ size: 500 })` (memoise the filters
   object).
2. Pull `openBreaks` from `useReconResults('OPEN')` and `resolvedBreaks`
   from `useReconResults('RESOLVED')`.
3. Compute totals + matched % + avg-resolution-hours from the arrays.
4. Inside `useEffect`, `setInterval(refetchTrades + refetchBreaks, 30_000)`
   and return a `clearInterval` cleanup.
5. Render 4 `<StatCard caption="..." value={...}/>` inside `<section
   className="cards">`.
</details>

<details>
<summary>Hint 3 — Page skeleton</summary>

```jsx
import { useEffect, useMemo } from 'react';
import StatCard from '../components/StatCard.jsx';
import { useTradeData } from '../hooks/useTradeData.js';
import { useReconResults } from '../hooks/useReconResults.js';

const REFRESH_MS = 30_000;

export default function Dashboard() {
    const filters = useMemo(() => ({ size: 500 }), []);
    const { trades, loading, refetch: refetchTrades } = useTradeData(filters);
    const { results: openBreaks, refetch: refetchBreaks } = useReconResults('OPEN');
    // TODO: also pull resolvedBreaks for the avg-resolution-hours card

    useEffect(() => {
        // TODO: setInterval to call refetchTrades + refetchBreaks every REFRESH_MS
        // TODO: return cleanup that clearInterval()s the id
    }, [refetchTrades, refetchBreaks]);

    // TODO: compute total / matchedPct / avgHours
    return (
        <section className="cards">
            {/* TODO: 4 StatCards */}
        </section>
    );
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `frontend/src/pages/Dashboard.jsx`

```jsx
import { useEffect, useMemo } from 'react';
import StatCard from '../components/StatCard.jsx';
import { useTradeData } from '../hooks/useTradeData.js';
import { useReconResults } from '../hooks/useReconResults.js';

const REFRESH_MS = 30_000;

export default function Dashboard() {
    const filters = useMemo(() => ({ size: 500 }), []);
    const { trades, loading, refetch: refetchTrades } = useTradeData(filters);
    const { results: openBreaks, refetch: refetchBreaks } = useReconResults('OPEN');
    const { results: resolvedBreaks } = useReconResults('RESOLVED');

    useEffect(() => {
        const id = setInterval(() => {
            refetchTrades();
            refetchBreaks();
        }, REFRESH_MS);
        return () => clearInterval(id);
    }, [refetchTrades, refetchBreaks]);

    const total = trades.length;
    const matched = trades.filter(t => t.status === 'MATCHED').length;
    const matchedPct = total ? Math.round((matched / total) * 100) + '%' : '—';
    const avgHours = computeAvgResolutionHours(resolvedBreaks);

    return (
        <>
            <h1>Operations Dashboard</h1>
            <section className="cards">
                <StatCard caption="Total Trades"       value={loading ? '…' : total} />
                <StatCard caption="Matched %"          value={loading ? '…' : matchedPct} />
                <StatCard caption="Unmatched Count"    value={openBreaks.length} />
                <StatCard caption="Avg Resolution Hrs" value={avgHours} />
            </section>
            <p className="footnote">Auto-refresh every 30s.</p>
        </>
    );
}

function computeAvgResolutionHours(resolved) {
    if (!resolved || resolved.length === 0) return '—';
    const valid = resolved.filter(r => r.detectedAt && r.resolvedAt);
    if (valid.length === 0) return '—';
    const totalHrs = valid.reduce((acc, r) => {
        const ms = new Date(r.resolvedAt) - new Date(r.detectedAt);
        return acc + ms / 3_600_000;
    }, 0);
    return (totalHrs / valid.length).toFixed(1) + 'h';
}
```
</details>

**Files to touch:** `frontend/src/pages/Dashboard.jsx`.

---

### TICKET-I104 — `<TradeTable />`

**What**
- `frontend/src/components/TradeTable.jsx` is a presentational component: receives `trades` + sort/filter/page handlers as props, renders rows with `key={trade.id}`, and never calls `fetch` itself.

**Why**
- Splitting "data owner" (page) from "data renderer" (component) is the props-down pattern every following React lesson assumes — I109's reducer state will be lifted into the parent and threaded into this table without any rewrite of the table itself.

**Observe**
- Clicking a column header re-orders rows in the same render cycle (no network call); the React DevTools props panel shows `trades: Array(20)`, `sortField`, `sortDir` updating from the parent; the console has zero "each child in a list should have a unique key" warnings.

**Acceptance criteria:**
- [ ] Receives `trades` as prop.
- [ ] Sortable columns.
- [ ] Filter dropdown for status.
- [ ] Pagination — page size 20.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

A presentational component — props in, JSX out. No `fetch`, no `useState`
for the data itself. Sort + filter + page are driven by props/handlers so
parent pages own the state. Every row needs a stable `key` — missing keys
cause subtle re-render bugs.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. Props: `trades`, `sortField`, `sortDir`, `onSortChange`, `loading`.
2. Early-return for `loading` and empty list.
3. Define a `COLUMNS` array of `{ key, label }` and render `<th>` per
   column — clickable, calls `onSortChange(key)`.
4. Render one `<TradeRow trade={t} />` per row, keyed by `t.id`.
5. Show ▲/▼ next to the active sort column.
</details>

<details>
<summary>Hint 3 — Component skeleton</summary>

```jsx
import TradeRow from './TradeRow.jsx';

export default function TradeTable({
    trades = [],
    sortField,
    sortDir,
    onSortChange,
    loading
}) {
    if (loading) return <div className="loading">Loading trades…</div>;
    if (!trades.length) return <div className="empty">No trades match your filters.</div>;

    return (
        <table className="data-table">
            <thead>
                <tr>
                    {/* TODO: map COLUMNS to clickable <th> */}
                </tr>
            </thead>
            <tbody>
                {/* TODO: map trades to <TradeRow key={t.id} trade={t} /> */}
            </tbody>
        </table>
    );
}

const COLUMNS = [
    // TODO: { key: 'tradeRef', label: 'Trade Ref' }, ... etc
];
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `frontend/src/components/TradeTable.jsx`

```jsx
import StatusBadge from './StatusBadge.jsx';

const PAGE_SIZE = 20;

const COLUMNS = [
    { key: 'tradeRef',       label: 'Trade Ref' },
    { key: 'instrumentId',   label: 'Instrument' },
    { key: 'counterpartyId', label: 'Counterparty' },
    { key: 'quantity',       label: 'Qty' },
    { key: 'price',          label: 'Price' },
    { key: 'tradeDate',      label: 'Date' },
    { key: 'status',         label: 'Status' },
];

export default function TradeTable({
    trades = [],
    sortField,
    sortDir,
    onSortChange,
    loading,
    page = 0,
    pageSize = PAGE_SIZE,
    onPageChange
}) {
    if (loading) return <div className="loading">Loading trades…</div>;
    if (!trades.length) return <div className="empty">No trades match your filters.</div>;

    const start = page * pageSize;
    const pageRows = trades.slice(start, start + pageSize);
    const totalPages = Math.max(1, Math.ceil(trades.length / pageSize));

    return (
        <>
            <table className="data-table">
                <thead>
                    <tr>
                        {COLUMNS.map(col => (
                            <th key={col.key}
                                onClick={() => onSortChange?.(col.key)}
                                style={{ cursor: onSortChange ? 'pointer' : 'default' }}>
                                {col.label}
                                {sortField === col.key && (sortDir === 'asc' ? ' ▲' : ' ▼')}
                            </th>
                        ))}
                    </tr>
                </thead>
                <tbody>
                    {pageRows.map(t => (
                        <tr key={t.id || t.tradeRef}>
                            <td>{t.tradeRef}</td>
                            <td>{t.instrumentId}</td>
                            <td>{t.counterpartyId}</td>
                            <td>{t.quantity}</td>
                            <td>{t.price}</td>
                            <td>{t.tradeDate}</td>
                            <td><StatusBadge status={t.status} /></td>
                        </tr>
                    ))}
                </tbody>
            </table>

            {onPageChange && totalPages > 1 && (
                <div className="pagination">
                    <button disabled={page === 0} onClick={() => onPageChange(page - 1)}>Prev</button>
                    <span>Page {page + 1} of {totalPages}</span>
                    <button disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>Next</button>
                </div>
            )}
        </>
    );
}
```
</details>

**Files to touch:** `frontend/src/components/TradeTable.jsx`.

---

### TICKET-I106 — `<AddTradeForm />` with React Hook Form

**What**
- `frontend/src/components/AddTradeForm.jsx` uses `useForm()` + `register(name, rules)` for every field, calls `apiService.createTrade(data)` on `handleSubmit`, and maps backend `body.details` back into `setError(field, { message })`.

**Why**
- This is the React version of I095/I096 — students see the same validation rules ("must be > 0", "cannot be in the future") expressed declaratively via RHF instead of imperatively via `let ok = true`; the same backend error envelope is consumed by both forms, proving the contract is server-side.

**Observe**
- Submitting an empty form renders `formState.errors.tradeRef.message === 'required'` (visible in React DevTools); a 400 from the backend populates `errors.root.serverError.message` AND per-field `errors.*.message`; navigation to `/trades` happens via `useNavigate()` without a full page reload.

**Acceptance criteria:**
- [ ] All fields use `useForm()` + `register()`.
- [ ] Same validation as the Day-7 static form.
- [ ] On submit calls `apiService.createTrade()` then redirects on success.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

React Hook Form replaces all the controlled-input + manual-validation
boilerplate from Day 7. Wire `register(name, rules)` on each input, hand
`onSubmit` to `handleSubmit(...)`, and read errors from `formState.errors`.
On API failure, map the backend error envelope back into `setError(...)`
so per-field messages surface.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. `const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm();`
2. Each input: `<input {...register('tradeRef', { required: 'required',
   pattern: { value: /^TRD-\d{4}-\d{4}$/, message: '...' } })} />`.
3. `numeric` fields: `valueAsNumber: true` so RHF passes a number to the
   API, not a string.
4. On submit: `await createTrade(data)` then `navigate('/trades')`.
5. On `ApiError` with `body.details`, loop `setError(field, { message })`.
   Also set a `root.serverError` for the top-level message.
6. Backend error envelope shape (from Phase-2.5 GlobalExceptionHandler):
   `{ code, message, details: { field: "msg" }, timestamp, path }`.
</details>

<details>
<summary>Hint 3 — Component skeleton</summary>

```jsx
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { createTrade, ApiError } from '../services/apiService.js';

export default function AddTradeForm() {
    const navigate = useNavigate();
    const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm();

    const onSubmit = async (data) => {
        try {
            await createTrade(data);
            navigate('/trades');
        } catch (e) {
            // TODO: map e.body.details -> setError(field, ...)
            // TODO: setError('root.serverError', { message: e.message })
        }
    };

    return (
        <form onSubmit={handleSubmit(onSubmit)}>
            {/* TODO: tradeRef, instrumentId, counterpartyId, quantity, price, tradeDate */}
            <button type="submit" disabled={isSubmitting}>Submit</button>
        </form>
    );
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `frontend/src/components/AddTradeForm.jsx`

```jsx
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { createTrade, ApiError } from '../services/apiService.js';

export default function AddTradeForm() {
    const navigate = useNavigate();
    const {
        register,
        handleSubmit,
        setError,
        formState: { errors, isSubmitting }
    } = useForm();

    const onSubmit = async (data) => {
        try {
            await createTrade(data);
            navigate('/trades');
        } catch (e) {
            if (e instanceof ApiError && e.body?.details) {
                Object.entries(e.body.details).forEach(([field, msg]) =>
                    setError(field, { message: msg })
                );
            }
            setError('root.serverError', { message: e.message });
        }
    };

    return (
        <form onSubmit={handleSubmit(onSubmit)}>

            <label>Trade Ref
                <input {...register('tradeRef', {
                    required: 'required',
                    pattern: { value: /^TRD-\d{4}-\d{4}$/, message: 'format TRD-YYYY-####' }
                })} />
                {errors.tradeRef && <span className="field-error">{errors.tradeRef.message}</span>}
            </label>

            <label>Instrument ID
                <input type="number" {...register('instrumentId', {
                    required: 'required',
                    valueAsNumber: true,
                    min: { value: 1, message: 'must be > 0' }
                })} />
                {errors.instrumentId && <span className="field-error">{errors.instrumentId.message}</span>}
            </label>

            <label>Counterparty ID
                <input type="number" {...register('counterpartyId', {
                    required: 'required',
                    valueAsNumber: true,
                    min: { value: 1, message: 'must be > 0' }
                })} />
                {errors.counterpartyId && <span className="field-error">{errors.counterpartyId.message}</span>}
            </label>

            <label>Quantity
                <input type="number" step="0.0001" {...register('quantity', {
                    required: 'required',
                    valueAsNumber: true,
                    min: { value: 0.0001, message: 'must be > 0' }
                })} />
                {errors.quantity && <span className="field-error">{errors.quantity.message}</span>}
            </label>

            <label>Price
                <input type="number" step="0.0001" {...register('price', {
                    required: 'required',
                    valueAsNumber: true,
                    min: { value: 0.0001, message: 'must be > 0' }
                })} />
                {errors.price && <span className="field-error">{errors.price.message}</span>}
            </label>

            <label>Trade Date
                <input type="date" {...register('tradeDate', {
                    required: 'required',
                    validate: v => new Date(v) <= new Date() || 'cannot be in the future'
                })} />
                {errors.tradeDate && <span className="field-error">{errors.tradeDate.message}</span>}
            </label>

            {errors.root?.serverError && (
                <div className="error">Server: {errors.root.serverError.message}</div>
            )}

            <button type="submit" disabled={isSubmitting}>
                {isSubmitting ? 'Submitting…' : 'Submit'}
            </button>
        </form>
    );
}
```
</details>

**Files to touch:** `frontend/src/components/AddTradeForm.jsx`.

---

### TICKET-I107 — `<ReconResults />` page

**What**
- `frontend/src/pages/Recon.jsx` renders filter pills (ALL/OPEN/RESOLVED), wires Resolve to call `apiService.resolveBreak(id)`, and keeps an `optimistic = { [id]: 'RESOLVED' }` local map that's overlaid on top of the hook's `results` array.

**Why**
- The optimistic-update / rollback pattern (a controlled input over a status) is exactly what Day 9's `BreakContext` + `withAuditLog` HOC tickets (I124A-D) generalise across the whole app — students need to feel it scoped to one component first.

**Observe**
- Clicking Resolve flips the row's badge to RESOLVED green instantly (before the PUT returns); the Network tab then shows the PUT 200 a moment later; if you stop the backend first and click Resolve, the badge flips back to OPEN red (rollback) and an error toast appears.

**Acceptance criteria:**
- [ ] Filter pills: All / OPEN / RESOLVED.
- [ ] Resolve action calls `apiService.resolveBreak(id)`.
- [ ] Optimistic update — row updates locally before server confirms.
- [ ] Rolls back on server error.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Optimistic UI = update the screen FIRST, then call the API. If the call
fails, roll back. Keep a local `optimistic` map of `{ id: status }`
overrides so a refetch (which is the truth) cleanly takes over once the
server confirms.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. `const { results, refetch } = useReconResults(filter)`.
2. `const [optimistic, setOptimistic] = useState({})`.
3. `doResolve(id)`: set `optimistic[id] = 'RESOLVED'`, await
   `resolveBreak(id)`, then `refetch()`. On catch, delete the optimistic
   entry and show an alert.
4. When rendering each row, use `optimistic[r.id] || r.status` so the
   override wins until the next refetch.
5. Filter pills are buttons that call `setFilter(s)` — `'OPEN'`,
   `'RESOLVED'`, `'SUPPRESSED'`.
</details>

<details>
<summary>Hint 3 — Page skeleton</summary>

```jsx
import { useState } from 'react';
import StatusBadge from '../components/StatusBadge.jsx';
import { useReconResults } from '../hooks/useReconResults.js';
import { resolveBreak } from '../services/apiService.js';

export default function Recon() {
    const [filter, setFilter] = useState('OPEN');
    const { results, loading, error, refetch } = useReconResults(filter);
    const [optimistic, setOptimistic] = useState({});

    const doResolve = async (id) => {
        // TODO: set optimistic[id] = 'RESOLVED'
        // TODO: try resolveBreak(id); then refetch()
        // TODO: catch -> delete optimistic[id], alert the user
    };

    return (
        <>
            <h1>Reconciliation Breaks</h1>
            {/* TODO: filter pills */}
            {/* TODO: table; status = optimistic[r.id] || r.status */}
        </>
    );
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `frontend/src/pages/Recon.jsx`

```jsx
import { useState } from 'react';
import StatusBadge from '../components/StatusBadge.jsx';
import { useReconResults } from '../hooks/useReconResults.js';
import { resolveBreak } from '../services/apiService.js';

export default function Recon() {
    const [filter, setFilter] = useState('OPEN');
    const { results, loading, error, refetch } = useReconResults(filter);
    const [optimistic, setOptimistic] = useState({});

    const doResolve = async (id) => {
        setOptimistic(prev => ({ ...prev, [id]: 'RESOLVED' }));
        try {
            await resolveBreak(id);
            refetch();
        } catch (e) {
            setOptimistic(prev => {
                const next = { ...prev };
                delete next[id];
                return next;
            });
            window.alert('Resolve failed: ' + e.message);
        }
    };

    return (
        <>
            <h1>Reconciliation Breaks</h1>

            <div className="filters">
                {['OPEN', 'RESOLVED', 'SUPPRESSED'].map(s => (
                    <button key={s}
                            className={filter === s ? 'active' : ''}
                            onClick={() => setFilter(s)}>
                        {s}
                    </button>
                ))}
            </div>

            {loading && <div className="loading">Loading…</div>}
            {error   && <div className="error">{error.message}</div>}

            <table className="data-table">
                <thead>
                    <tr>
                        <th>Trade Ref</th>
                        <th>Discrepancy</th>
                        <th>Status</th>
                        <th>Detected</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    {results.map(r => {
                        const status = optimistic[r.id] || r.status;
                        const detected = r.detectedAt
                            ? new Date(r.detectedAt).toLocaleString('en-GB')
                            : '—';
                        return (
                            <tr key={r.id}>
                                <td>{r.tradeRef ?? r.tradeId ?? '—'}</td>
                                <td>{r.discrepancyType ?? '—'}</td>
                                <td><StatusBadge status={status} /></td>
                                <td>{detected}</td>
                                <td>
                                    {status === 'OPEN' && (
                                        <button onClick={() => doResolve(r.id)}>Resolve</button>
                                    )}
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </>
    );
}
```
</details>

**Files to touch:** `frontend/src/pages/Recon.jsx`.

---

### TICKET-I108 — `<StatusBadge />` reusable component

**What**
- `frontend/src/components/StatusBadge.jsx` + `StatusBadge.module.css` — a one-prop pill component with a `COLOURS` lookup mapping all 8 trade + recon statuses (`MATCHED`, `PENDING`, `UNMATCHED`, `DISPUTED`, `CANCELLED`, `OPEN`, `RESOLVED`, `SUPPRESSED`) to scoped CSS classes.

**Why**
- This is the smallest possible reusable component — single prop, no state, no effects — and it's the one students will reach for again in Day 9's `<ResolveBreakModal />` and Day 10's audit log. Getting CSS Modules scoping right here also prevents the global-CSS bleed from I111's stretch ticket.

**Observe**
- Both `<TradeTable />` rows and `<ReconResults />` rows show coloured pills with identical styling; React DevTools shows `<StatusBadge status="MATCHED">` mounted in multiple places without re-renders cascading; an unknown status (`status="FOO"`) falls back to the grey `pending` pill without crashing.

**Acceptance criteria:**
- [ ] Single prop: `status` (string).
- [ ] Renders coloured pill matching Day-7 CSS classes.
- [ ] Has Storybook-style usage examples in a docstring.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

One source of truth for status colour. The status values overlap two
domains — trade statuses (`MATCHED`, `PENDING`, `UNMATCHED`, `DISPUTED`,
`CANCELLED`) and recon statuses (`OPEN`, `RESOLVED`, `SUPPRESSED`) — so
map them all in one `COLOURS` object. Use CSS Modules so colour classes
are scoped.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. Create `StatusBadge.module.css` with `.badge` (pill shape) plus colour
   classes `.matched` `.unmatched` `.disputed` `.pending` `.cancelled`.
2. In the component, import the module: `import styles from
   './StatusBadge.module.css';`.
3. Build a `COLOURS` lookup mapping every status string to a class.
4. Pick a sensible default (e.g. `styles.pending`) for unknown statuses.
5. Return `<span className={`${styles.badge} ${cls}`}>{status}</span>`.
</details>

<details>
<summary>Hint 3 — Component skeleton</summary>

```jsx
// StatusBadge.module.css
// .badge { display: inline-block; padding: 0.15rem 0.6rem;
//   font-size: 0.75rem; font-weight: 600; border-radius: 999px;
//   color: white; }
// .matched   { background: #2e7d32; }
// .unmatched { background: #c62828; }
// ... etc

import styles from './StatusBadge.module.css';

const COLOURS = {
    // TODO: map MATCHED/PENDING/UNMATCHED/DISPUTED/CANCELLED
    // TODO: map OPEN/RESOLVED/SUPPRESSED to the same classes
};

export default function StatusBadge({ status }) {
    const cls = COLOURS[status] || styles.pending;
    return <span className={`${styles.badge} ${cls}`}>{status}</span>;
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit:** `frontend/src/components/StatusBadge.jsx` and `frontend/src/components/StatusBadge.module.css`

```css
/* StatusBadge.module.css */
.badge {
    display: inline-block;
    padding: 0.15rem 0.6rem;
    font-size: 0.75rem;
    font-weight: 600;
    border-radius: 999px;
    color: white;
}

.matched   { background: #2e7d32; }
.unmatched { background: #c62828; }
.disputed  { background: #ef6c00; }
.pending   { background: #757575; }
.cancelled { background: #455a64; }
```

```jsx
/**
 * StatusBadge — reusable coloured pill.
 *
 *  USAGE:
 *    <StatusBadge status="MATCHED" />        ->  green pill
 *    <StatusBadge status="OPEN" />           ->  red pill
 *    <StatusBadge status="DISPUTED" />       ->  amber pill
 */
import styles from './StatusBadge.module.css';

const COLOURS = {
    MATCHED:    styles.matched,
    PENDING:    styles.pending,
    UNMATCHED:  styles.unmatched,
    DISPUTED:   styles.disputed,
    CANCELLED:  styles.cancelled,
    OPEN:       styles.unmatched,
    RESOLVED:   styles.matched,
    SUPPRESSED: styles.pending,
};

export default function StatusBadge({ status }) {
    const cls = COLOURS[status] || styles.pending;
    return <span className={`${styles.badge} ${cls}`}>{status}</span>;
}
```
</details>

**Files to touch:** `frontend/src/components/StatusBadge.jsx`.

---

### Just-in-time primer — `useReducer` in 5 minutes

You've used `useState` all of Day 8 — fine for one or two independent
fields. The next ticket has **five** filter fields (status, date range,
counterparty, sort field, sort direction) that all change together.
That's the point where `useReducer` beats `useState`. Day 9 AM will go
deeper; here's what you need for I109.

**The shape.** A reducer is a pure function: `(state, action) => newState`.
You call `dispatch({ type: "SET_STATUS", payload: "OPEN" })`; React calls
your reducer with the current state + that action; whatever you return
becomes the new state.

```jsx
import { useReducer } from "react";

const initialState = {
  status: "ALL",
  dateRange: { from: null, to: null },
  counterparty: null,
  sortField: "tradeDate",
  sortDir: "desc"
};

function filtersReducer(state, action) {
  switch (action.type) {
    case "SET_STATUS":       return { ...state, status: action.payload };
    case "SET_DATE_RANGE":   return { ...state, dateRange: action.payload };
    case "SET_COUNTERPARTY": return { ...state, counterparty: action.payload };
    case "SET_SORT":         return { ...state, sortField: action.payload.field,
                                               sortDir: action.payload.dir };
    case "RESET":            return initialState;
    default:                 throw new Error(`Unknown action: ${action.type}`);
  }
}

function TradesPage() {
  const [filters, dispatch] = useReducer(filtersReducer, initialState);

  return (
    <>
      <select value={filters.status}
              onChange={(e) => dispatch({ type: "SET_STATUS", payload: e.target.value })}>
        <option>ALL</option><option>OPEN</option><option>RESOLVED</option>
      </select>
      <button onClick={() => dispatch({ type: "RESET" })}>Clear filters</button>
      <TradeTable filters={filters} />
    </>
  );
}
```

**Three rules — break any of them and you'll get bugs:**

| Rule | Why |
|---|---|
| Reducer must be **pure** | No `fetch()`, no `Math.random()`, no DOM access. React may call it twice (Strict Mode) — side-effects will run twice |
| Always **return a new object** (`{ ...state, field: x }`) | Mutating `state.field = x` won't trigger a re-render — React compares object references |
| `dispatch` is **stable** across renders | Safe to pass into `useEffect` deps or memoised children without causing re-renders |

**`useState` vs `useReducer` — when to switch.**

| Use `useState` when… | Use `useReducer` when… |
|---|---|
| 1–2 independent values | 3+ fields that change together |
| Updates are "set to X" | Updates depend on previous state (`{...state, items: [...state.items, x]}`) |
| No invariants between fields | Invariants you want enforced in one place (e.g. "if status=ALL clear counterparty") |
| Children don't need the setter | Children need to dispatch — pass `dispatch` down without wrapper functions |

**Naming convention — copy this, don't invent.**

- Action `type` strings: `SCREAMING_SNAKE_CASE`, verb-led (`SET_STATUS`, `ADD_ROW`, `RESET`).
- Payload key is literally `payload`.
- Reducer name: `<noun>Reducer` (`filtersReducer`, `cartReducer`).

That's enough to ship I109. Hints below have the full file.

---

### TICKET-I109 — `useReducer` for trade filters

**What**
- A `filtersReducer(state, action)` co-located with `<TradesPage />` handles 5 filter fields via `SET_STATUS`, `SET_DATE_RANGE`, `SET_COUNTERPARTY`, `SET_SORT`, `RESET` actions — `dispatch` is passed down to filter UI children.

**Why**
- Five fields that move together is the textbook `useState` → `useReducer` threshold, and pure-reducer discipline (no `fetch` inside, always return a new object) is the same discipline Day 9 AM's deeper `useReducer` lab and Day 10's Redux-Toolkit primer build on.

**Observe**
- Changing the status pill dispatches `{ type: 'SET_STATUS', payload: 'OPEN' }` (visible in React DevTools' hook panel as a new `filters` object); `<TradesPage />` re-renders, `useTradeData(filters)` fires a single new `GET /api/v1/trades?status=OPEN`; `RESET` flips every field back to `initialState` in one render.

**Acceptance criteria:**
- [ ] Reducer state: `{ status, dateRange, counterparty, sortField, sortDir }`.
- [ ] Actions: `SET_STATUS`, `SET_DATE_RANGE`, `SET_COUNTERPARTY`, `SET_SORT`, `RESET`.
- [ ] Reducer used inside `<TradesPage />`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

`useReducer` shines when multiple state fields move together. A filter
panel with 5 fields is exactly that case — one `dispatch` call per user
action beats juggling 5 `useState` setters. Keep the reducer pure (no
side-effects, returns a new state object).
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. Export `initialFilters` with every field defaulted (mostly `null`).
2. Export `tradeFilterReducer(state, action)` switching on `action.type`.
3. Five action types: `SET_STATUS`, `SET_DATE_RANGE`, `SET_COUNTERPARTY`,
   `SET_SORT`, `RESET`.
4. Always return a new object: `{ ...state, status: action.status }`.
5. `default` case throws — typos become loud errors, not silent bugs.
6. Use inside `Trades.jsx` via `const [filters, dispatch] =
   useReducer(tradeFilterReducer, initialFilters);`.
</details>

<details>
<summary>Hint 3 — Hook skeleton</summary>

```javascript
export const initialFilters = {
    status: null,
    from: null,
    to: null,
    counterparty: null,
    sortField: 'tradeDate',
    sortDir: 'desc'
};

export function tradeFilterReducer(state, action) {
    switch (action.type) {
        case 'SET_STATUS':       return { ...state, status: action.status };
        // TODO: SET_DATE_RANGE -> { ...state, from, to }
        // TODO: SET_COUNTERPARTY
        // TODO: SET_SORT -> { ...state, sortField, sortDir }
        case 'RESET':            return initialFilters;
        default:
            throw new Error('Unknown filter action: ' + action.type);
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit:** `frontend/src/hooks/tradeFilterReducer.js` and `frontend/src/pages/Trades.jsx`

```javascript
// frontend/src/hooks/tradeFilterReducer.js
export const initialFilters = {
    status: null,
    from: null,
    to: null,
    counterparty: null,
    sortField: 'tradeDate',
    sortDir: 'desc'
};

export function tradeFilterReducer(state, action) {
    switch (action.type) {
        case 'SET_STATUS':       return { ...state, status: action.status };
        case 'SET_DATE_RANGE':   return { ...state, from: action.from, to: action.to };
        case 'SET_COUNTERPARTY': return { ...state, counterparty: action.counterparty };
        case 'SET_SORT':         return { ...state, sortField: action.field, sortDir: action.dir };
        case 'RESET':            return initialFilters;
        default:
            // Defensive throw — typos in action.type become loud errors.
            throw new Error('Unknown filter action: ' + action.type);
    }
}
```

```jsx
// frontend/src/pages/Trades.jsx
import { useMemo, useReducer } from 'react';
import { initialFilters, tradeFilterReducer } from '../hooks/tradeFilterReducer.js';
import { useTradeData } from '../hooks/useTradeData.js';
import TradeTable from '../components/TradeTable.jsx';

export default function Trades() {
    const [filters, dispatch] = useReducer(tradeFilterReducer, initialFilters);

    const apiParams = useMemo(() => {
        const p = {};
        if (filters.status) p.status = filters.status;
        if (filters.from) p.from = filters.from;
        if (filters.to) p.to = filters.to;
        return p;
    }, [filters.status, filters.from, filters.to]);

    const { trades, loading, error } = useTradeData(apiParams);

    const onSortChange = (field) => {
        const dir = (filters.sortField === field && filters.sortDir === 'asc') ? 'desc' : 'asc';
        dispatch({ type: 'SET_SORT', field, dir });
    };

    return (
        <>
            <h1>Trades</h1>
            <div className="filters">
                {['MATCHED','PENDING','UNMATCHED','DISPUTED','CANCELLED'].map(s => (
                    <button key={s}
                            className={filters.status === s ? 'active' : ''}
                            onClick={() => dispatch({ type: 'SET_STATUS', status: filters.status === s ? null : s })}>
                        {s}
                    </button>
                ))}
                <button className="reset" onClick={() => dispatch({ type: 'RESET' })}>
                    Reset
                </button>
            </div>
            {error && <div className="error">{error.message}</div>}
            <TradeTable
                trades={trades}
                loading={loading}
                sortField={filters.sortField}
                sortDir={filters.sortDir}
                onSortChange={onSortChange}
            />
        </>
    );
}
```
</details>

**Files to touch:** `frontend/src/pages/Trades.jsx`, `frontend/src/hooks/tradeFilterReducer.js`.

---

### TICKET-I110 — React Router setup

**What**
- `frontend/src/App.jsx` owns a sidebar of `<NavLink>`s plus a `<Routes>` block covering `/dashboard`, `/trades`, `/trades/new`, `/recon`, a `/` → `/dashboard` `<Navigate replace />`, and a catch-all 404.

**Why**
- Wiring the SPA shell here is what makes the dashboard a real app instead of a single page — and `<NavLink>`'s `isActive` callback teaches the same pattern Day 9 uses for `<ProtectedRoute>` and Day 10 uses for breadcrumb-active styling.

**Observe**
- Clicking each sidebar link swaps the route without a document fetch in the Network tab (no `index.html` reload); the URL bar updates, the active `<NavLink>` gets the `active` CSS class, and `/nonexistent` renders the 404 component instead of a blank page.

**Acceptance criteria:**
- [ ] Routes: `/dashboard`, `/trades`, `/trades/new`, `/recon`.
- [ ] Default `/` redirects to `/dashboard`.
- [ ] 404 page for anything else.
- [ ] `<Link>` (not `<a>`) used throughout the nav.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

`BrowserRouter` wraps the whole app (in `main.jsx`). `App.jsx` owns the
`<Routes>` block. Use `<NavLink>` (not `<a>`) so the active route gets
styled — the `className` prop on `NavLink` takes a callback that receives
`{ isActive }`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. In `main.jsx`, wrap `<App />` in `<BrowserRouter>`.
2. In `App.jsx`, render a sidebar `<nav>` of `<NavLink>`s.
3. Render a `<Routes>` block with one `<Route>` per page.
4. Use `<Navigate to="/dashboard" replace />` for the `/` redirect.
5. Catch-all: `<Route path="*" element={<NotFound />} />`.
6. `NavLink` className callback: `({ isActive }) => isActive ? 'active' : ''`.
</details>

<details>
<summary>Hint 3 — Component skeleton</summary>

```jsx
import { Navigate, Route, Routes, NavLink } from 'react-router-dom';
import Dashboard from './pages/Dashboard.jsx';
import Trades from './pages/Trades.jsx';
import AddTradeForm from './components/AddTradeForm.jsx';
import Recon from './pages/Recon.jsx';

export default function App() {
    return (
        <div className="layout">
            <nav className="sidebar">
                <ul>
                    {/* TODO: NavLinks: /dashboard, /trades, /trades/new, /recon */}
                </ul>
            </nav>
            <section className="content">
                <Routes>
                    {/* TODO: <Route path="/" element={<Navigate to="/dashboard" replace />} /> */}
                    {/* TODO: per-page routes */}
                    {/* TODO: catch-all 404 */}
                </Routes>
            </section>
        </div>
    );
}

function navClass({ isActive }) {
    return isActive ? 'active' : '';
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `frontend/src/App.jsx`

```jsx
import { Navigate, Route, Routes, NavLink } from 'react-router-dom';
import Dashboard from './pages/Dashboard.jsx';
import Trades from './pages/Trades.jsx';
import AddTradeForm from './components/AddTradeForm.jsx';
import Recon from './pages/Recon.jsx';

export default function App() {
    return (
        <div className="layout">
            <header className="topbar">
                <span className="logo">DB · TradeFlow</span>
                <span className="user">Logged in as <strong>trader</strong></span>
            </header>

            <div className="main">
                <nav className="sidebar" aria-label="Primary">
                    <ul>
                        <li><NavLink to="/dashboard" className={navClass}>Dashboard</NavLink></li>
                        <li><NavLink to="/trades"    className={navClass}>Trades</NavLink></li>
                        <li><NavLink to="/trades/new" className={navClass}>+ New Trade</NavLink></li>
                        <li><NavLink to="/recon"     className={navClass}>Recon Breaks</NavLink></li>
                    </ul>
                </nav>

                <section className="content">
                    <Routes>
                        <Route path="/" element={<Navigate to="/dashboard" replace />} />
                        <Route path="/dashboard" element={<Dashboard />} />
                        <Route path="/trades" element={<Trades />} />
                        <Route path="/trades/new" element={<AddTradeForm />} />
                        <Route path="/recon" element={<Recon />} />
                        <Route path="*" element={<NotFound />} />
                    </Routes>
                </section>
            </div>
        </div>
    );
}

function navClass({ isActive }) {
    return isActive ? 'active' : '';
}

function NotFound() {
    return (
        <div>
            <h2>404 — Not Found</h2>
            <NavLink to="/dashboard">Back to dashboard</NavLink>
        </div>
    );
}
```
</details>

**Files to touch:** `frontend/src/App.jsx`.

---

## Run and Observe — End of Sprint 8 (React Frontend)

You've shipped 13 React tickets (I099–I111) this PM, on top of the 6 vanilla-JS
tickets from the AM (I093–I098). The React app now bootstraps via Vite,
fetches trades through `apiService.js` + custom hooks, renders the dashboard
with filters/reducer, and routes between Trades and Recon pages.
Before the instructor checkpoint, prove it end-to-end.

**Run:**

> **Terminal #1** — backend (from `backend/`)

```bash
./mvnw spring-boot:run
```

> **Terminal #2** — frontend (from `frontend/`)

```bash
npm run dev
```

**Observe — browser:**

Open <http://localhost:5173>. Browser DevTools open (F12).

| Check | Expected after Sprint 8 |
|---|---|
| `/` (Trades dashboard) | `<TradeDashboard />` renders, `<TradeTable />` lists trades, each `<TradeRow />` expands with detail |
| Filter controls (status / counterparty) | `useReducer` updates state, `useTradeData(filters)` refetches, table re-renders |
| `<AddTradeForm />` submit | New trade POSTs, table refreshes, `<StatusBadge />` shows PENDING colour |
| Navigate to `/recon` via Router | `<ReconResults />` page mounts, `useReconResults(status)` populates breaks |
| Resize 1024 / 1440 / 1920 | Layout holds, sidebar collapses below 600px |

**DevTools checks:**

| Tab | What |
|---|---|
| Components (React DevTools) | TradeDashboard → TradeTable → TradeRow tree visible, props/state as expected |
| Network | `GET /api/v1/trades` and `GET /api/v1/recon/results` return 200, no CORS errors |
| Console | no red errors, no React key warnings, no missing-dependency warnings from hooks |

**Negative tests — prove your UI handles failure:**

```
# 1. Submit AddTradeForm with empty required fields
#    Expected: React Hook Form inline validation messages, no POST fires

# 2. Stop the backend (Ctrl+C in Terminal #1), then refresh the dashboard
#    Expected: error state renders gracefully (not a blank white screen);
#    useTradeData surfaces the fetch error

# 3. Filter to a status with no matching trades
#    Expected: empty-state message, not a broken table
```

**If something looks wrong:** check the Vite proxy in `vite.config.js`
(`/api` should forward to `http://localhost:8080`); check React DevTools for
hooks firing on unmounted components; re-read the Reference Solution of the
ticket for the misbehaving component (I101/I102 for hooks, I109 for the
reducer, I110 for router wiring).

---

**Instructor checkpoint:** Before the Zipkin teach-along, demo the running
app (dashboard + filters + add-trade + recon route) to the instructor.

---

## Distributed tracing — Zipkin (Day 8 Sprint 3, 1 hr)

The instructor walks through Zipkin and how a single trade request spans
React → Spring Boot → Postgres.

- Add `spring-cloud-starter-zipkin` to `backend/pom.xml`.
- POST a trade from the React form.
- Open Zipkin UI, find the trace, screenshot the waterfall, paste it into
  your PR description for I106.

<details>
<summary>Reference — full walkthrough</summary>

### Steps

1. **Start Zipkin** (instructor provides the script — `docker run -p 9411:9411 openzipkin/zipkin`).
2. **Add dependencies** to `backend/pom.xml` (Micrometer + Brave tracer).
3. **Configure sampling + endpoint** in `application.yml`.
4. **Send a request** from the React form (POST `/api/v1/trades`).
5. **Open Zipkin UI** → `http://localhost:9411` → click *Run Query*.
6. **Find your trace**, click in, screenshot the waterfall.
7. **Paste screenshot + 1-line explanation** into PR I106.

### Reference solution

**`backend/pom.xml`** — Spring Boot 3 uses Micrometer Tracing (not the
deprecated `spring-cloud-starter-zipkin`). Add these:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
    <groupId>io.zipkin.reporter2</groupId>
    <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
```

**`backend/src/main/resources/application.yml`**:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0          # 100% for the lab — drop to 0.1 in prod
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

> The `%X{traceId},%X{spanId}` in the log pattern is the magic that lets you
> grep the backend log for a Zipkin trace ID and find every line for that
> request.

**Start Zipkin** (instructor provides):

```bash
docker run -d -p 9411:9411 --name zipkin openzipkin/zipkin
```

**Optional extension — frontend → backend trace propagation:**
- Add a `traceparent` header in the React `fetch` calls (W3C trace-context format)
  so the React POST and the Spring Boot insert share one Zipkin trace.
- Pick a screenshot that shows ≥ 2 spans (HTTP request + JDBC query) — that's
  what proves the trace works end-to-end.

### Verify

```bash
# 1. POST a trade via Swagger UI or React form
curl -X POST http://localhost:8080/api/v1/trades -H "Content-Type: application/json" \
  -d '{"tradeRef":"T-DEMO-1","isin":"US0378331005","amount":100}'

# 2. Check the backend log — you should see the traceId in brackets
# Example: INFO  [tradeflow-backend,7e1a...,b3c2...] TradeService : insert T-DEMO-1

# 3. Open Zipkin
open http://localhost:9411

# 4. Click "Run Query" → find your trace → screenshot the waterfall
```

A healthy trace shows: `POST /api/v1/trades` (parent) → `INSERT trades` (JDBC
child). If you only see the parent, the JDBC instrumentation didn't load —
check that `spring-boot-starter-jdbc` (or `data-jpa`) is on the classpath.

</details>

---

## End-of-day checklist

- [ ] 15 tickets merged (`I093`, `I095`–`I097`, `I099`–`I104`, `I106`–`I110`).
- [ ] **AM:** static dashboard fetches live trades, validates, and POSTs new trades.
- [ ] **PM:** React app builds without warnings.
- [ ] All four React pages render live API data.
- [ ] Resolve flow updates optimistically and rolls back on error.

---

## Stretch goals (dropped tickets — finish if time)

These four were ticketed in earlier versions of the timetable and were
dropped to keep Day 8 deliverable. Trainer reference code and student
skeletons are still in place, so any team that finishes the 15 active
tickets can pick these up:

| Dropped | Why it isn't needed | Where the skeleton/reference lives |
|---|---|---|
| `I094` — `trades.js` column sort | React `<TradeTable />` reimplements sort | `static-dashboard/js/trades.js` (skeleton) |
| `I098` — Loading + error UX in vanilla JS | `useTradeData` / `useReconResults` hooks bake it in | `static-dashboard/js/*.js` (skeleton) |
| `I105` — `<TradeRow />` expandable row | Tables render flat; Day-10 demo doesn't expand | `frontend/src/components/TradeRow.jsx` (skeleton) |
| `I111` — Professional styling pass (CSS Modules) | Day-7 brand CSS already styles every page | `frontend/src/styles/` (existing brand CSS holds the line) |

End-goal preserved: the Day-10 demo flow (POST trade → Kafka event →
break detected → resolve → Grafana → CI green) only requires the 15
active tickets.

Next: [Day 9 — React advanced + Kafka](../day9/README.md)
