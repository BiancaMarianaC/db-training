# Day 7 — Frontend Foundations: HTML, CSS + JavaScript Essentials

> Theme: **Build the dashboard's structure and styling. Cover JavaScript Essentials in classroom.**
> Tickets: **I086 – I092 + I092A** (8 tickets — 7 HTML/CSS + 1 JS bridge)
> Modules: HTML (AM, first slot) + CSS (PM) + JavaScript Essentials (AM, second slot)

Sprint work today is HTML + CSS plus a small JS bridge ticket. JavaScript
Essentials is taught this morning (const/let, DOM, fetch contract,
try/catch/finally) and **immediately applied** in `I092A` (JS bootstrap
skeletons). The bigger JS sprints (`I093`–`I098`) land tomorrow on
Day 8 AM and build directly on today's skeletons.

By end of day:

- Four hand-authored HTML pages (dashboard, trades, recon, add-trade).
- Brand CSS (navy + gold), card layouts, status badges.
- 4 JS skeleton files loaded with `DOMContentLoaded` + TODO markers for Day 8.
- JS Essentials covered in classroom *and* applied via `I092A`.

---

## Sprint 6 — Frontend — HTML & CSS

### TICKET-I086 — `dashboard.html`

**What**
- New `static-dashboard/dashboard.html`: semantic shell with `<header>`, `<nav>`, `<main>`, and 4 `<article class="card">` blocks whose `id`s match the constants in tomorrow's `cards.js`.

**Why**
- Today's static dashboard markup is wired to Day 8's vanilla JS in I098 then ported to React in I099 — the `id` and class names you choose now are the contract both consumers honour, so a typo here causes 2 days of cascading rework.

**Observe**
- Chrome DevTools Elements tab shows exactly one `<header>`, one `<nav>`, one `<main>`, and four `<article class="card">` nodes; W3C HTML validator (`https://validator.w3.org/`) returns zero errors on the file.

**Acceptance criteria:**
- [ ] `<header>` with bank logo placeholder + user name.
- [ ] `<nav>` sidebar: links to Trades / Recon / Add Trade.
- [ ] `<main>` with 4 summary cards: Total Trades, Matched %, Unmatched Count, Avg Processing Time.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- Semantic HTML5: use `<header>`, `<nav>`, `<main>` — not a sea of `<div>`s.
- The 4 cards each need a stable `id` so JS (`cards.js`) can write values into them.
- Link `css/style.css` from `<head>`; load `js/cards.js` at end of `<body>` with `defer`.
- No data yet — cards show `—` until JS lands in I091/I093.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Page skeleton:

1. `<head>` — meta charset/viewport, title, `<link rel="stylesheet" href="css/style.css">`.
2. `<header class="topbar">` — logo span + user span.
3. `.layout` wrapper containing `<nav class="sidebar">` (4 links) and `<main class="content">`.
4. Inside `<main>`: `<h1>` + a `.cards` section with 4 `<article class="card">` elements.
5. Each card: `<h2 class="card-caption">` + `<p class="card-value" id="card-...">—</p>`.
6. Load `<script src="js/cards.js" defer></script>` just before `</body>`.
</details>

<details>
<summary>Hint 3 — HTML skeleton</summary>

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <title>TradeFlow — Dashboard</title>
  <link rel="stylesheet" href="css/style.css"/>
</head>
<body>
  <header class="topbar">
    <!-- TODO: logo span + user span -->
  </header>

  <div class="layout">
    <nav class="sidebar">
      <!-- TODO: <ul> with 4 nav links -->
    </nav>
    <main class="content">
      <h1>Operations Dashboard</h1>
      <section class="cards">
        <!-- TODO: 4 <article class="card"> blocks -->
      </section>
    </main>
  </div>

  <script src="js/cards.js" defer></script>
</body>
</html>
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `static-dashboard/dashboard.html`

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>TradeFlow — Dashboard</title>
    <link rel="stylesheet" href="css/style.css"/>
</head>
<body>

<header class="topbar">
    <span class="logo">DB · TradeFlow</span>
    <span class="user">Logged in as <strong>viewer</strong></span>
</header>

<div class="layout">

    <nav class="sidebar" aria-label="Primary">
        <ul>
            <li><a href="dashboard.html" class="active">Dashboard</a></li>
            <li><a href="trades.html">Trades</a></li>
            <li><a href="recon.html">Recon Breaks</a></li>
            <li><a href="add-trade.html">+ New Trade</a></li>
        </ul>
    </nav>

    <main class="content">
        <h1>Operations Dashboard</h1>

        <section class="cards">
            <article class="card">
                <h2 class="card-caption">Total Trades</h2>
                <p class="card-value" id="card-total-trades">—</p>
            </article>
            <article class="card">
                <h2 class="card-caption">Matched %</h2>
                <p class="card-value" id="card-matched-pct">—</p>
            </article>
            <article class="card">
                <h2 class="card-caption">Unmatched Count</h2>
                <p class="card-value" id="card-unmatched">—</p>
            </article>
            <article class="card">
                <h2 class="card-caption">Avg Processing Time</h2>
                <p class="card-value" id="card-avg-time">—</p>
            </article>
        </section>
    </main>

</div>

<script src="js/cards.js" defer></script>

</body>
</html>
```
</details>

**Files to touch:** `static-dashboard/dashboard.html`, `css/style.css`.

---

### TICKET-I087 — `trades.html`

**What**
- New `static-dashboard/trades.html`: 7-column `<table class="data-table">` with `<th data-key="...">` headers, empty `<tbody id="trades-tbody">`, plus Prev/page-info/Next pagination shell.

**Why**
- The `data-key` attributes are the bridge between today's static `<th>` and Day 8's I093 sort handler — `trades.js` reads `event.target.dataset.key` to know which column to sort by, so the attribute names must match the JSON field keys the backend returns (`tradeRef`, `quantity`, ...).

**Observe**
- DevTools Elements panel shows 7 `<th>` nodes each with a non-empty `data-key`; clicking a header today does nothing (no JS), but `$0.dataset.key` in the console returns the expected camelCase string.

**Acceptance criteria:**
- [ ] Data table with columns: Trade Ref, Instrument, Counterparty, Qty, Price, Date, Status.
- [ ] Sortable column headers.
- [ ] Pagination controls at the bottom.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- `<thead>` holds the column headers, `<tbody>` is empty for now —
  rows arrive via `trades.js` in I093.
- Each `<th>` gets a `data-key="..."` attribute matching the JSON field
  name from the API (`tradeRef`, `quantity`, ...). That's how the sort
  handler will know which column to sort by.
- Reuse the topbar + sidebar structure from `dashboard.html` (mark Trades
  as `active`).
- Add empty `<div>`s for loading + error placeholders.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Structure:

1. Same `<head>` + topbar + sidebar as `dashboard.html`.
2. `<main>` → `<h1>Trades</h1>` + `<table class="data-table">`.
3. `<thead>` with one `<th data-key="...">` per column (7 columns total).
4. `<tbody id="trades-tbody">` — leave empty; JS fills it.
5. Below the table: `<div id="trades-loading" class="loading hidden">` and
   `<div id="trades-error" class="error hidden">`.
6. Pagination `<nav class="pagination">` with Prev / page-info / Next buttons.
7. `<script src="js/trades.js" defer></script>` before `</body>`.
</details>

<details>
<summary>Hint 3 — HTML skeleton</summary>

```html
<main class="content">
  <h1>Trades</h1>

  <table class="data-table">
    <thead>
      <tr>
        <th data-key="tradeRef">Trade Ref</th>
        <!-- TODO: instrumentId, counterpartyId, quantity, price, tradeDate, status -->
      </tr>
    </thead>
    <tbody id="trades-tbody"></tbody>
  </table>

  <div id="trades-loading" class="loading hidden">Loading trades…</div>
  <div id="trades-error" class="error hidden"></div>

  <!-- TODO: pagination nav with prev, info, next -->
</main>
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `static-dashboard/trades.html`

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>TradeFlow — Trades</title>
    <link rel="stylesheet" href="css/style.css"/>
</head>
<body>

<header class="topbar">
    <span class="logo">DB · TradeFlow</span>
    <span class="user">Logged in as <strong>viewer</strong></span>
</header>

<div class="layout">

    <nav class="sidebar" aria-label="Primary">
        <ul>
            <li><a href="dashboard.html">Dashboard</a></li>
            <li><a href="trades.html" class="active">Trades</a></li>
            <li><a href="recon.html">Recon Breaks</a></li>
            <li><a href="add-trade.html">+ New Trade</a></li>
        </ul>
    </nav>

    <main class="content">

        <h1>Trades</h1>

        <table class="data-table">
            <thead>
            <tr>
                <th data-key="tradeRef">Trade Ref</th>
                <th data-key="instrumentId">Instrument</th>
                <th data-key="counterpartyId">Counterparty</th>
                <th data-key="quantity">Qty</th>
                <th data-key="price">Price</th>
                <th data-key="tradeDate">Date</th>
                <th data-key="status">Status</th>
            </tr>
            </thead>
            <tbody id="trades-tbody">
            </tbody>
        </table>

        <div id="trades-loading" class="loading hidden">Loading trades…</div>
        <div id="trades-error" class="error hidden"></div>

        <nav class="pagination" aria-label="Trades pagination">
            <button id="trades-prev" type="button">‹ Prev</button>
            <span id="trades-pagination-info">Page 1</span>
            <button id="trades-next" type="button">Next ›</button>
        </nav>

    </main>
</div>

<script src="js/trades.js" defer></script>
</body>
</html>
```
</details>

**Files to touch:** `static-dashboard/trades.html`.

---

### TICKET-I088 — `recon.html`

**What**
- New `static-dashboard/recon.html`: 5-column break-list shell (`Trade Ref`, `Discrepancy`, `Status`, `Opened`, `Action`) with empty `<tbody id="recon-tbody">` plus loading + error placeholder divs.

**Why**
- The empty `<tbody>` is what Day 8's I097 `recon.js` targets with `innerHTML` to inject per-row badges and Resolve buttons via event-delegation — if the `id` drifts, the JS silently writes nowhere and the page stays blank with no console error.

**Observe**
- DevTools shows `document.getElementById("recon-tbody")` returns the empty `<tbody>` element (not `null`); both loading and error divs carry the `hidden` class so nothing visible is between the table and the next section.

**Acceptance criteria:**
- [ ] List of reconciliation results.
- [ ] Status badge per row.
- [ ] "Resolve" button on OPEN rows.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- Same outer layout as the other pages — reuse topbar + sidebar (mark
  Recon as `active`).
- The page itself is a static shell — `recon.js` (I097) populates rows.
- Columns: Trade Ref, Discrepancy, Status, Opened, Action.
- The Resolve button lives inside an `Action` column on each row;
  recon.js will inject it via `innerHTML`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Build the shell:

1. `<head>` + topbar + sidebar (Recon link gets `class="active"`).
2. `<main>` → `<h1>Reconciliation Breaks</h1>`.
3. `<table class="data-table">` with 5 `<th>` columns.
4. `<tbody id="recon-tbody">` — empty; `recon.js` injects rows.
5. Loading + error `<div>`s with the `hidden` class.
6. `<script src="js/recon.js" defer></script>`.

You don't write per-row badges or buttons here — those are generated by
`recon.js` because the data is dynamic.
</details>

<details>
<summary>Hint 3 — HTML skeleton</summary>

```html
<main class="content">
  <h1>Reconciliation Breaks</h1>

  <table class="data-table">
    <thead>
      <tr>
        <th>Trade Ref</th>
        <!-- TODO: Discrepancy, Status, Opened, Action -->
      </tr>
    </thead>
    <tbody id="recon-tbody"></tbody>
  </table>

  <div id="recon-loading" class="loading hidden">Loading breaks…</div>
  <div id="recon-error" class="error hidden"></div>
</main>
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `static-dashboard/recon.html`

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>TradeFlow — Recon</title>
    <link rel="stylesheet" href="css/style.css"/>
</head>
<body>

<header class="topbar">
    <span class="logo">DB · TradeFlow</span>
    <span class="user">Logged in as <strong>trader</strong></span>
</header>

<div class="layout">

    <nav class="sidebar" aria-label="Primary">
        <ul>
            <li><a href="dashboard.html">Dashboard</a></li>
            <li><a href="trades.html">Trades</a></li>
            <li><a href="recon.html" class="active">Recon Breaks</a></li>
            <li><a href="add-trade.html">+ New Trade</a></li>
        </ul>
    </nav>

    <main class="content">

        <h1>Reconciliation Breaks</h1>

        <table class="data-table">
            <thead>
            <tr>
                <th>Trade Ref</th>
                <th>Discrepancy</th>
                <th>Status</th>
                <th>Opened</th>
                <th>Action</th>
            </tr>
            </thead>
            <tbody id="recon-tbody">
            </tbody>
        </table>

        <div id="recon-loading" class="loading hidden">Loading breaks…</div>
        <div id="recon-error" class="error hidden"></div>

    </main>
</div>

<script src="js/recon.js" defer></script>
</body>
</html>
```
</details>

**Files to touch:** `static-dashboard/recon.html`.

---

### TICKET-I089 — `add-trade.html`

**What**
- New `static-dashboard/add-trade.html`: `<form id="trade-form" novalidate>` with 6 named inputs (Trade Ref, Instrument, Counterparty, Quantity, Price, Trade Date), each paired with a `<span class="field-error" data-for="X">` sibling for inline errors.

**Why**
- The `name` attribute on every input is the contract Day 8's I095 `addTrade.js` reads via `new FormData(form)` — and `novalidate` keeps the browser's native tooltip out of the way so your I096 client-side validation can surface error-envelope messages into the `data-for="X"` spans.

**Observe**
- DevTools Elements panel shows every input has a `name=` attribute matching the camelCase API field; in the console, `Object.fromEntries(new FormData(document.getElementById('trade-form')))` returns 6 keys when the form is filled.

**Acceptance criteria:**
- [ ] Form fields: instrument (dropdown), counterparty (dropdown), quantity, price, trade date.
- [ ] Submit + Cancel buttons.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- One `<form id="trade-form" novalidate>` — `novalidate` so YOUR JS
  validation runs instead of the browser's tooltip default.
- Each field: `<label>` wrapping the `<input>`/`<select>` plus a
  `<span class="field-error" data-for="fieldName"></span>` for inline errors.
- Add a hidden `<div id="form-feedback" class="toast hidden">` for success
  / failure messages.
- Cancel is a link styled as a button (`<a class="btn-secondary">`),
  not a `<button>`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Six fields in order:

1. Trade Ref — `<input type="text" required pattern="TRD-\d{4}-\d{4}">`.
2. Instrument — `<select required>` with one `<option value="">— select —</option>`; addTrade.js will fill the rest.
3. Counterparty — same shape as Instrument.
4. Quantity — `<input type="number" step="0.0001" min="0.0001" required>`.
5. Price — same shape as Quantity.
6. Trade Date — `<input type="date" required>`.

Each field gets a sibling `<span class="field-error" data-for="X">`. JS
finds it by `data-for` and writes the inline error message.

The `name="..."` attribute on each input is critical — `addTrade.js` reads
the form with `new FormData(form)` and uses `name` as the key.
</details>

<details>
<summary>Hint 3 — HTML skeleton</summary>

```html
<form id="trade-form" novalidate>
  <label>Trade Ref
    <input type="text" name="tradeRef" required pattern="TRD-\d{4}-\d{4}"
           placeholder="TRD-2026-0001"/>
    <span class="field-error" data-for="tradeRef"></span>
  </label>

  <!-- TODO: Instrument <select name="instrumentId"> -->
  <!-- TODO: Counterparty <select name="counterpartyId"> -->
  <!-- TODO: Quantity (number, step=0.0001, min=0.0001) -->
  <!-- TODO: Price (same) -->
  <!-- TODO: Trade Date (type="date") -->

  <div class="form-actions">
    <button type="submit">Submit</button>
    <a href="trades.html" class="btn-secondary">Cancel</a>
  </div>
</form>

<div id="form-feedback" class="toast hidden"></div>
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `static-dashboard/add-trade.html`

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <title>TradeFlow — Add Trade</title>
    <link rel="stylesheet" href="css/style.css"/>
</head>
<body>

<header class="topbar">
    <span class="logo">DB · TradeFlow</span>
    <span class="user">Logged in as <strong>trader</strong></span>
</header>

<div class="layout">

    <nav class="sidebar" aria-label="Primary">
        <ul>
            <li><a href="dashboard.html">Dashboard</a></li>
            <li><a href="trades.html">Trades</a></li>
            <li><a href="recon.html">Recon Breaks</a></li>
            <li><a href="add-trade.html" class="active">+ New Trade</a></li>
        </ul>
    </nav>

    <main class="content">

        <h1>New Trade</h1>

        <form id="trade-form" novalidate autocomplete="off">

            <label>Trade Ref
                <input type="text" name="tradeRef" required pattern="TRD-\d{4}-\d{4}"
                       placeholder="TRD-2026-0001"/>
                <span class="field-error" data-for="tradeRef"></span>
            </label>

            <label>Instrument
                <select name="instrumentId" required>
                    <option value="">— select —</option>
                </select>
                <span class="field-error" data-for="instrumentId"></span>
            </label>

            <label>Counterparty
                <select name="counterpartyId" required>
                    <option value="">— select —</option>
                </select>
                <span class="field-error" data-for="counterpartyId"></span>
            </label>

            <label>Quantity
                <input type="number" name="quantity" step="0.0001" min="0.0001" required/>
                <span class="field-error" data-for="quantity"></span>
            </label>

            <label>Price
                <input type="number" name="price" step="0.0001" min="0.0001" required/>
                <span class="field-error" data-for="price"></span>
            </label>

            <label>Trade Date
                <input type="date" name="tradeDate" required/>
                <span class="field-error" data-for="tradeDate"></span>
            </label>

            <div class="form-actions">
                <button type="submit">Submit</button>
                <a href="trades.html" class="btn-secondary">Cancel</a>
            </div>
        </form>

        <div id="form-feedback" class="toast hidden"></div>

    </main>
</div>

<script src="js/addTrade.js" defer></script>
</body>
</html>
```
</details>

**Files to touch:** `static-dashboard/add-trade.html`.

---

### TICKET-I090 — `style.css` — brand styling

**What**
- New `static-dashboard/css/style.css`: DB brand palette as `:root` custom properties (`--db-navy: #003366`, `--db-gold: #FFC72C`), 220px-fixed sidebar grid, navy topbar, professional `.data-table` (zebra rows + hover).

**Why**
- Defining the palette as CSS custom properties is the only way Day 8's React port (I099) can reuse the same tokens via CSS Modules without forking — and the layout `display: grid` rule is what makes the sidebar collapse cleanly under the I091 `@media (max-width: 600px)` breakpoint instead of fighting it.

**Observe**
- DevTools Elements → Computed tab on `.topbar` shows `background-color: rgb(0, 51, 102)`; hovering a `.sidebar li a` shows the gold-left-border transition fire; the `<th>` rows show navy background, white text via the cascade.

**Acceptance criteria:**
- [ ] Deutsche Bank palette: navy `#003366`, gold `#FFC72C`, white background.
- [ ] Dark sidebar nav.
- [ ] Sans-serif font stack.
- [ ] Professional data table style (zebra rows, hover state).

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- Use CSS custom properties (`--db-navy`, `--db-gold`, etc.) in `:root`
  so colours are changed in one place.
- The layout is a CSS Grid: `220px 1fr` — sidebar fixed width, content takes the rest.
- Zebra rows: `tbody tr:nth-child(even) { background: ...; }`.
- Topbar uses flex with `justify-content: space-between` — logo left, user right.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Top-down approach:

1. `* { box-sizing: border-box; }` plus a `body` reset (margin 0, font stack, bg colour).
2. `.topbar` — flex, navy background, white text, `space-between`.
3. `.layout` — `display: grid; grid-template-columns: 220px 1fr;`.
4. `.sidebar` — dark navy bg, `<ul>` reset, `<a>` block-level links with
   a transparent left border that turns gold on hover/`.active`.
5. `.content` — padding for the main area, `h1` colour = navy.
6. `.data-table` — full width, collapsed borders, white bg, rounded corners
   via `overflow: hidden`. `<th>` in navy with white text. Zebra + hover.
</details>

<details>
<summary>Hint 3 — CSS skeleton</summary>

```css
:root {
  --db-navy: #003366;
  --db-gold: #FFC72C;
  --db-bg:   #f4f6f9;
  /* TODO: card bg, text colours, status colours */
}

* { box-sizing: border-box; }
body {
  margin: 0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
  background: var(--db-bg);
}

.topbar { /* TODO: flex, navy bg, white text, padding, space-between */ }

.layout  { display: grid; grid-template-columns: 220px 1fr; }
.sidebar { /* TODO: dark bg, padding */ }

.data-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
}
/* TODO: th styling, zebra rows, hover */
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `static-dashboard/css/style.css`

```css
:root {
    --db-navy: #003366;
    --db-gold: #FFC72C;
    --db-bg: #f4f6f9;
    --db-card: #ffffff;
    --db-text: #1a1f2c;
    --db-text-light: #5b6473;
    --status-matched: #2e7d32;
    --status-unmatched: #c62828;
    --status-disputed: #ef6c00;
    --status-pending: #757575;
}

* { box-sizing: border-box; }
body {
    margin: 0;
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
    background: var(--db-bg);
    color: var(--db-text);
}

.topbar {
    background: var(--db-navy);
    color: white;
    padding: 0.75rem 1.5rem;
    display: flex;
    justify-content: space-between;
    align-items: center;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
}
.topbar .logo { font-weight: 700; letter-spacing: 0.5px; }
.topbar .user { font-size: 0.875rem; opacity: 0.85; }

.layout {
    display: grid;
    grid-template-columns: 220px 1fr;
    min-height: calc(100vh - 56px);
}
.sidebar {
    background: #001a33;
    color: #cfd6df;
    padding-top: 1rem;
}
.sidebar ul { list-style: none; padding: 0; margin: 0; }
.sidebar li a {
    display: block;
    color: inherit;
    text-decoration: none;
    padding: 0.75rem 1.25rem;
    border-left: 3px solid transparent;
}
.sidebar li a:hover,
.sidebar li a.active {
    background: rgba(255, 199, 44, 0.06);
    border-left-color: var(--db-gold);
    color: white;
}

.content { padding: 1.5rem 2rem; }
.content h1 { margin-top: 0; color: var(--db-navy); }

.data-table {
    width: 100%;
    border-collapse: collapse;
    background: var(--db-card);
    border-radius: 8px;
    overflow: hidden;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.data-table th,
.data-table td { padding: 0.75rem 1rem; text-align: left; }
.data-table th {
    background: var(--db-navy);
    color: white;
    cursor: pointer;
    user-select: none;
    font-weight: 500;
}
.data-table tbody tr:nth-child(even) { background: rgba(0, 51, 102, 0.03); }
.data-table tbody tr:hover { background: rgba(0, 51, 102, 0.08); }
```
</details>

**Files to touch:** `static-dashboard/css/style.css`.

---

### TICKET-I091 — Summary card styling

**What**
- Append `.cards` + `.card` + `.card-caption` + `.card-value` rules to `css/style.css`: 4-column `grid-template-columns: repeat(4, 1fr)`, 1rem gap, rounded white cards with soft shadow, plus a `@media (max-width: 600px)` block that collapses to 1 column.

**Why**
- The 4-column grid is the visual shape Day 8's I098 `cards.js` populates with `Promise.allSettled` results — the responsive collapse rule means Day 9's accessibility audit and demo-laptop projector demos behave identically on cohort laptops down to mobile.

**Observe**
- DevTools Elements → Computed tab on `.cards` shows `display: grid` with `grid-template-columns: repeat(4, 1fr)`; Chrome responsive-mode at 599px shows the cards stacked in 1 column and the sidebar hidden.

**Acceptance criteria:**
- [ ] 4 cards in a CSS grid, equal width, 1rem gap.
- [ ] Each card: big number, small caption, optional trend indicator.
- [ ] Responsive: 1-column on screens < 600px.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- Use `display: grid; grid-template-columns: repeat(4, 1fr);` on `.cards` —
  cleaner than flexbox for equal-width columns.
- Big number = `.card-value` ~2rem, semibold, navy.
- Small caption = `.card-caption` 0.8rem, uppercase, light grey.
- A `@media (max-width: 600px)` block collapses to one column.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Three rule blocks:

1. `.cards` — grid container, 4 equal columns, `gap: 1rem`, top margin.
2. `.card` — white background, rounded corners, soft shadow, padding.
3. `.card-caption` (small label) and `.card-value` (big number) — typography.

Then a media query at `max-width: 600px` that switches `.cards` to a single
column AND hides the sidebar (the `.layout` becomes 1-column too).
</details>

<details>
<summary>Hint 3 — CSS skeleton</summary>

```css
.cards {
  display: grid;
  /* TODO: 4 equal columns, 1rem gap */
  margin-top: 1rem;
}
.card {
  background: white;
  border-radius: 8px;
  padding: 1rem 1.25rem;
  /* TODO: subtle box-shadow */
}
.card-caption {
  font-size: 0.8rem;
  text-transform: uppercase;
  /* TODO: letter-spacing, light colour */
}
.card-value {
  /* TODO: 2rem, 600 weight, navy */
}

@media (max-width: 600px) {
  /* TODO: collapse .cards + .layout to one column, hide sidebar */
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `static-dashboard/css/style.css` (append)

```css
/* ---------- Summary cards (TICKET-I091) ---------- */
.cards {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 1rem;
    margin-top: 1rem;
}
.card {
    background: var(--db-card);
    border-radius: 8px;
    padding: 1rem 1.25rem;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.card-caption {
    font-size: 0.8rem;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    color: var(--db-text-light);
    margin: 0 0 0.5rem 0;
}
.card-value {
    font-size: 2rem;
    font-weight: 600;
    margin: 0;
    color: var(--db-navy);
}

/* ---------- Responsive (TICKET-I091) ---------- */
@media (max-width: 600px) {
    .layout { grid-template-columns: 1fr; }
    .sidebar { display: none; }
    .cards  { grid-template-columns: 1fr; }
}
```
</details>

**Files to touch:** `css/style.css`.

---

### TICKET-I092 — Status badges

**What**
- Append `.badge` + per-status modifier classes (`.badge-matched`, `.badge-unmatched`, `.badge-disputed`, `.badge-pending`, plus `.badge-resolved` / `.badge-open` for the recon page) to `css/style.css`: shared pill base (`border-radius: 999px`, 0.75rem font, white text) and one `background-color` per status.

**Why**
- The base + modifier split is the CSS specificity demo students will encounter all week — JS in I093/I097 writes `<span class="badge badge-matched">` joining two classes, and the lesson is that adding `.badge-resolved` later doesn't touch the shape, only the colour.

**Observe**
- Drop `<span class="badge badge-matched">MATCHED</span>` into `dashboard.html`, reload, and the pill renders green with white text; DevTools Computed tab on the span shows `border-radius: 999px` resolving from `.badge` and `background-color: rgb(46, 125, 50)` resolving from `.badge-matched`.

**Acceptance criteria:**
- [ ] CSS classes: `.badge-matched` (green), `.badge-unmatched` (red),
  `.badge-disputed` (amber), `.badge-pending` (grey).
- [ ] Rounded pill shape, ~10px font.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 2 — More guided</summary>

Two layers:

1. A base `.badge` class with the shared shape (inline-block, padding,
   tiny font, white text, `border-radius: 999px` for a perfect pill).
2. Per-status modifier classes (`.badge-matched`, ...) that only set
   `background-color`.

You'll also want `.badge-resolved` (= green) and `.badge-open` (= red) for
the recon page. Reuse the existing colour variables.

JS will write `<span class="badge badge-matched">MATCHED</span>` — both
classes together.
</details>

<details>
<summary>Hint 1 — Basic nudge</summary>

- `border-radius: 999px` is the trick for a pill shape (any value larger
  than half the height works).
- All badges share a base `.badge` class; only the colour differs.
- Use the colour custom properties you already defined
  (`--status-matched`, `--status-unmatched`, ...).
- Pill text is short (`MATCHED`, `UNMATCHED`) — keep font weight 600.
</details>

<details>
<summary>Hint 3 — CSS skeleton</summary>

```css
.badge {
  display: inline-block;
  padding: 0.15rem 0.6rem;
  font-size: 0.75rem;
  font-weight: 600;
  border-radius: 999px;
  color: white;
}
.badge-matched   { background: var(--status-matched); }
.badge-unmatched { background: var(--status-unmatched); }
/* TODO: badge-disputed, badge-pending, badge-resolved, badge-open */
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `static-dashboard/css/style.css` (append)

```css
/* ---------- Status badges (TICKET-I092) ---------- */
.badge {
    display: inline-block;
    padding: 0.15rem 0.6rem;
    font-size: 0.75rem;
    font-weight: 600;
    border-radius: 999px;
    color: white;
}
.badge-matched   { background: var(--status-matched); }
.badge-unmatched { background: var(--status-unmatched); }
.badge-disputed  { background: var(--status-disputed); }
.badge-pending   { background: var(--status-pending); }
.badge-resolved  { background: var(--status-matched); }
.badge-open      { background: var(--status-unmatched); }
.badge:hover     { filter: brightness(1.1); }
```
</details>

**Files to touch:** `css/style.css`.

---

### TICKET-I092A — JS bootstrap skeletons (Day-7 PM bridge)

**Bridge ticket** that applies this morning's JS Essentials lesson on the
project — `const`/`let`, `DOMContentLoaded`, `querySelector`. You write
~10 lines per file; tomorrow (Day 8 AM) you fill them in with real fetch
+ render logic for I093–I098.

**What**
- Four new files under `static-dashboard/js/`: `cards.js`, `trades.js`, `recon.js`, `addTrade.js`. Each ~10 lines: a `DOMContentLoaded` listener, `const` references to the page's key elements, a single `console.log("<name>.js: ready")`, and a `// TICKET-I09X — TODO` marker for the Day-8 ticket that fills it in.

**Why**
- I092A's JS modules are the bridge between today's static HTML and tomorrow's reactive UI — once the listener and selectors compile cleanly, Day 8's I093/I095/I097/I098 tickets only have to add `fetch()` and render logic, which means three students can pick a JS file each on Day 8 AM with no merge conflicts.

**Observe**
- Open each HTML page in Chrome — DevTools Console shows exactly one `<name>.js: ready` line per page reload, zero errors, and `document.querySelector("#card-total-trades")` from the console returns the element (not `null`), proving `defer` ran the script after DOM parse.

**Acceptance criteria:**
- [ ] Create `static-dashboard/js/cards.js` — `DOMContentLoaded` listener
  that selects the 4 card value `<span>`s and logs `"cards.js: ready"`.
- [ ] Create skeletons for `trades.js`, `recon.js`, `addTrade.js` (if not
  present) — each with `DOMContentLoaded` + a single `console.log` proving
  the script loaded.
- [ ] Each file ends with a `// TICKET-I09X — TODO:` marker for the Day-8
  ticket that will fill it in (`I093` for trades, `I095` for addTrade,
  `I097` for recon, `I091/I098` for cards).
- [ ] Each HTML page loads its matching JS with `<script defer>`.
- [ ] Open the page in Chrome → console shows the `ready` line. No errors.

<details>
<summary>Hint 1 — Why this exists</summary>

JS Essentials was the second AM teach block. This ticket is the smallest
honest application of what you learned: pick elements out of the DOM,
listen for `DOMContentLoaded`, write a `const`. Tomorrow's tickets
(`I093`–`I098`) plug `fetch()` + rendering into this skeleton.
</details>

<details>
<summary>Hint 2 — `cards.js` skeleton (copy-paste, then fill in tomorrow)</summary>

```javascript
// static-dashboard/js/cards.js
// TICKET-I092A — skeleton; TICKET-I091/I098 fill this in on Day 8.

document.addEventListener("DOMContentLoaded", () => {
  const totalEl     = document.querySelector("#card-total-trades");
  const matchedEl   = document.querySelector("#card-matched-pct");
  const unmatchedEl = document.querySelector("#card-unmatched");
  const lagEl       = document.querySelector("#card-avg-lag");

  console.log("cards.js: ready");
  // TICKET-I091 — TODO: fetch /api/v1/trades and /api/v1/recon/run
  // TICKET-I098 — TODO: loading + error UX on the 4 cards
});
```

`trades.js`, `recon.js`, `addTrade.js` follow the same shape with their
own `TICKET-IXXX — TODO` markers.
</details>

<details>
<summary>Hint 3 — Loading the script</summary>

```html
<!-- bottom of dashboard.html, just before </body> -->
<script src="js/cards.js" defer></script>
```

`defer` waits until the DOM has parsed; without it `querySelector` would
return `null` if the script ran before the cards exist. Same pattern for
`trades.html` → `js/trades.js`, etc.
</details>

**Files to touch:** `static-dashboard/js/cards.js` (new), `js/trades.js`,
`js/recon.js`, `js/addTrade.js`, plus the four HTML files' `<script>`
tags.

---

## Run and Observe — End of Sprint 6 (HTML & CSS Dashboard + JS skeletons)

You've shipped 8 tickets (I086–I092 + I092A): 4 HTML pages, brand CSS,
summary cards, status badges, and JS bootstrap skeletons. The pages are
still static — no `fetch()` yet (that's Day 8 AM). Before the instructor
checkpoint, prove the markup, styling, and skeleton scripts all load.

**Run:**

The backend doesn't matter today — there's no JS to hit it yet. Just open
the HTML pages directly or via a quick local server.

```bash
# Option A — open files directly in the browser:
open dashboard.html        # macOS
xdg-open dashboard.html    # Linux
start dashboard.html       # Windows

# Option B — serve via Python (closer to what Day-8 JS will need):
cd tradeflow-studentscopy/static-dashboard
python3 -m http.server 5500
# Then open http://localhost:5500/dashboard.html
```

**Observe — markup & styling checks:**

| Page | What to check |
|---|---|
| `dashboard.html` | header, sidebar nav, 4 summary cards laid out correctly; sample numbers visible (placeholder text is fine — no live data today) |
| `trades.html` | table headers render, dummy rows show status badges with correct colour, pagination control is visible |
| `recon.html` | results table structure renders; status pills display |
| `add-trade.html` | every input has a `<label>`; required fields marked; form submits to nowhere (no JS) — that's expected |

**Observe — visual specifics:**

| Check | Expected after Sprint 6 |
|---|---|
| Brand colors | navy header + gold accents match the spec in `style.css` |
| Status badges | `MATCHED` green, `PENDING` amber, `BREAK`/`FAILED` red |
| Responsive | shrink the window to ~600px width — sidebar collapses, cards stack |
| Accessibility | tab order is logical; every form field has a programmatic label |
| Empty state | placeholder empty-state markup is in place (will get used when JS arrives) |

**If the styling looks wrong:** open DevTools — the **Elements** tab shows
the computed CSS for each element. Most Day-7 bugs are: forgot to link
`style.css` in `<head>`, used the wrong class name on a badge, or the
sidebar grid template-columns spec is off.

---

**Instructor checkpoint:** Before you move on to the AI-Assisted Frontend
teach-along, get the instructor to walk through your 4 pages live.

---

## AI-Assisted Frontend (Day 7 PM, ~1 hr)

- Prompt: *"Build a responsive nav bar in HTML+CSS for a banking ops
  dashboard. Brand colors: navy and gold. Must be accessible (aria-labels,
  keyboard navigable)."*
- Paste output, run an axe-DevTools audit, fix at least 1 accessibility issue.
- Capture the prompt + the fix in your PR description.

<details>
<summary>Reference — full walkthrough</summary>

### Steps

1. **Generate** the nav bar with the prompt above. Save raw output verbatim
   into your PR description.
2. **Drop it into** `frontend/index.html` (or your nav partial) + `nav.css`.
3. **Install axe-DevTools** browser extension. Run a scan on the page.
4. **Pick at least one finding** that's an actual a11y bug, not a style nit.
5. **Fix it** — usually 1-3 lines.
6. **Re-run axe**. Confirm the finding is gone. Screenshot before + after.

### Reference solution

**`frontend/index.html`** (or `partials/nav.html`):

```html
<header class="topbar" role="banner">
  <a href="/" class="brand" aria-label="TradeFlow home">
    <span class="logo">TF</span>
    <span class="title">TradeFlow Ops</span>
  </a>

  <nav class="primary-nav" aria-label="Primary">
    <ul>
      <li><a href="/trades.html">Trades</a></li>
      <li><a href="/breaks.html" aria-current="page">Breaks</a></li>
      <li><a href="/audit.html">Audit</a></li>
      <li><a href="/settings.html">Settings</a></li>
    </ul>
  </nav>

  <button class="nav-toggle" aria-expanded="false" aria-controls="primary-nav">
    <!-- TODO: add an accessible label, e.g. aria-label="Open menu" -->
    ☰
  </button>
</header>
```

**`frontend/css/nav.css`**:

```css
:root {
  --navy: #0a1f44;
  --gold: #c9a227;
  --ink:  #1b1b1b;
}

.topbar {
  display: flex;
  align-items: center;
  gap: 1.5rem;
  padding: 0.75rem 1.25rem;
  background: var(--navy);
  color: #fff;
}

.brand { display: flex; align-items: center; gap: 0.5rem; color: #fff; text-decoration: none; }
.logo  { background: var(--gold); color: var(--navy); font-weight: 700; padding: 0.25rem 0.5rem; border-radius: 4px; }

.primary-nav ul { display: flex; gap: 1rem; list-style: none; margin: 0; padding: 0; }
.primary-nav a {
  color: #fff;
  text-decoration: none;
  padding: 0.5rem 0.75rem;
  border-radius: 4px;
}
.primary-nav a:hover,
.primary-nav a:focus-visible {     /* keyboard-focus visibility */
  outline: 2px solid var(--gold);
  outline-offset: 2px;
}
.primary-nav [aria-current="page"] { background: rgba(255,255,255,0.12); }

.nav-toggle { display: none; }

@media (max-width: 720px) {
  .primary-nav { display: none; }
  .nav-toggle  { display: inline-block; background: transparent; color: #fff; border: 1px solid var(--gold); padding: 0.4rem 0.7rem; border-radius: 4px; }
}
```

**Common axe findings to watch for:**
- **Contrast** — light gold-on-white text fails 4.5:1. Use the gold only on dark backgrounds.
- **Missing button label** — the `☰` button has no accessible name; add `aria-label="Open menu"` and flip `aria-expanded` when toggled.
- **Skip-to-content link** — many AI outputs omit it. Add `<a href="#main" class="skip-link">Skip to content</a>`.

### Verify

- axe-DevTools shows 0 Critical, 0 Serious.
- Tab through the page — focus ring is visible on every link.
- Resize to 600px — hamburger appears, links collapse.

</details>

---

## End-of-day checklist

- [ ] 7 tickets merged (`I086`–`I092`).
- [ ] All 4 HTML pages render with the navy + gold brand palette.
- [ ] Summary cards are a 4-column grid; collapse to 1 column under 600px.
- [ ] Status badges colour correctly per status enum.
- [ ] Sidebar `.active` state is correct on each page.
- [ ] axe-DevTools shows 0 Critical / 0 Serious after the AI lab fix.

Next: [Day 8 — Vanilla JS (AM) + React foundations (PM)](../day8/README.md)
