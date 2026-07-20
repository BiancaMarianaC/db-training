# Day 2 — Java OOP Fundamentals + Liquibase (taught + applied)

> Theme: **Translate the DB schema into clean Java objects (Sprint 1+2). Properly teach & run Liquibase (Sprint 3).**
> Tickets: **14 active** — `I016`–`I027` (Java OOP) + `I009`, `I010` (Liquibase — moved here from Day 1)
> Module: Java — Modules 1 & 2 + Liquibase Sprint 3

By end of day:

- All your DB tables are reflected as Java classes with proper encapsulation.
- Trade objects are built via the **Builder pattern**.
- Three enums replace the magic strings.
- A console `main` prints a formatted list of trades.

---

## Sprint 1 — Java Domain Model

### TICKET-I016 — Create package structure

**What**
- Scaffold the standard package layout under `com.dbtraining.tradeflow`: `model`, `service`, `dao`, `util`, `exception`, plus the root `TradeflowApplication`.

**Why**
- Every class you write Days 2–5 lands in one of these folders; locking the layout now means imports stop moving once teams hit Day 3's services and Day 4's DAOs. Empty packages also flush out the "package vs folder" misconception before any real class shows it the painful way.

**Observe**
- `tree backend/src/main/java/com/dbtraining/tradeflow` shows five sub-folders + `TradeflowApplication.java`; `./mvnw compile` from `backend/` ends with `BUILD SUCCESS` and zero warnings.

**Acceptance criteria:**
- [ ] Packages exist: `model`, `service`, `dao`, `util`, `exception`.
- [ ] `TradeflowApplication.java` is in the root package and `main()` prints a banner.
- [ ] `./mvnw compile` runs clean.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Most of the scaffold is already in place from Day 1. Your job is just to
confirm the five sub-packages exist and that the root `TradeflowApplication`
compiles. A package in Java is just a folder under `src/main/java/` that
matches a `package` declaration at the top of each `.java` file.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Run `tree backend/src/main/java/com/dbtraining/tradeflow` (or use the
  IDE's project view) — you should see `model/`, `service/`, `dao/`,
  `util/`, `exception/` folders.
- Empty folders are silently ignored by `git` — drop a placeholder
  `package-info.java` (or any `.java` stub) in each so the folder commits.
- The root `TradeflowApplication.java` keeps the banner you wrote on Day 1.
- `./mvnw compile` from `backend/` must finish with `BUILD SUCCESS`.
</details>

<details>
<summary>Hint 3 — Layout skeleton</summary>

```
backend/src/main/java/com/dbtraining/tradeflow/
├── TradeflowApplication.java        // main() with banner
├── model/                           // POJOs land here (I017–I025)
│   └── package-info.java            // optional placeholder
├── service/                         // business logic (Day 3+)
│   └── package-info.java
├── dao/                             // JDBC / JPA layer (Day 4+)
│   └── package-info.java
├── util/                            // helpers (formatters, validators)
│   └── package-info.java
└── exception/                       // custom checked / runtime exceptions
    └── package-info.java
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/TradeflowApplication.java`

```java
// backend/src/main/java/com/dbtraining/tradeflow/TradeflowApplication.java
package com.dbtraining.tradeflow;

public class TradeflowApplication {

    public static void main(String[] args) {
        printBanner();
        // printDay2Demo() will be wired in by TICKET-I026
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("  Deutsche Bank — TDI 2026 Graduate Technical Training");
        System.out.println("  Intermediate Track — Case Study: Trade Reconciliation");
        System.out.println();
    }
}
```
</details>

**Files to touch:** `backend/src/main/java/com/dbtraining/tradeflow/`.

---

### TICKET-I017 — Build the `Trade` class

**What**
- Java POJO mirroring the `trades` table: private fields for `tradeRef`, `instrumentId`, `counterpartyId`, `quantity`, `price`, `tradeDate`, `status`, `createdAt`, with getters and no public setters.

**Why**
- This is the first translation from yesterday's schema into Java. The getters you author here become the row-mapping targets for Day 4's JDBC DAO and the `@Column` fields for Day 5's JPA entity — once `Trade` is right, the next three days stand on it.

**Observe**
- `./mvnw compile` ends with `BUILD SUCCESS` once `TradeStatus` (I019) is on the classpath; `grep -n 'public void set' backend/src/main/java/com/dbtraining/tradeflow/model/Trade.java` returns zero hits (no setters).

**Acceptance criteria:**
- [ ] Fields: `tradeRef` (String), `instrumentId` (Long), `counterpartyId`
  (Long), `quantity` (BigDecimal), `price` (BigDecimal), `tradeDate`
  (LocalDate), `status` (TradeStatus — see I019), `createdAt` (Instant).
- [ ] All fields `private`, with getters (no setters — favour immutability
  via the Builder, see I018).

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

`BigDecimal`, not `double`, for `quantity` and `price` — same reason the DB
used `NUMERIC(18,4)`: floats lose pennies on rounding. Use `LocalDate` for
`tradeDate` and `Instant` for `createdAt` (avoid `java.util.Date` — it's the
legacy API). Encapsulation: every field `private`, every accessor `public`,
no setters yet (the Builder in I018 is the only construction path).
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Package: `com.dbtraining.tradeflow.model`.
- Imports: `java.math.BigDecimal`, `java.time.Instant`, `java.time.LocalDate`.
- Make the public constructor `private` — callers must go through the
  Builder you'll add in I018. For now you can leave the private constructor
  empty (the Builder will populate the fields when wired).
- Add one getter per field. No setters.
- A computed `getNotional()` helper (`quantity * price`) is a nice touch and
  pre-saves you work for Day 3.
</details>

<details>
<summary>Hint 3 — Class skeleton</summary>

```java
package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class Trade {

    private String tradeRef;
    private Long instrumentId;
    private Long counterpartyId;
    private BigDecimal quantity;
    private BigDecimal price;
    private LocalDate tradeDate;
    private TradeStatus status;        // enum from I019
    private Instant createdAt;

    private Trade() { /* Builder fills these in — see I018 */ }

    // TODO: one getter per field
    // TODO: getNotional() = quantity.multiply(price)
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/model/Trade.java`

```java
// backend/src/main/java/com/dbtraining/tradeflow/model/Trade.java
package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Trade — POJO mirroring the trades table.
 * Immutable from the outside: construction goes through the Builder (I018).
 */
public class Trade {

    private String tradeRef;
    private Long instrumentId;
    private Long counterpartyId;
    private BigDecimal quantity;
    private BigDecimal price;
    private LocalDate tradeDate;
    private TradeStatus status;
    private Instant createdAt;

    // Package-private no-arg constructor — Builder is the public path.
    Trade() {}

    public String getTradeRef()         { return tradeRef; }
    public Long getInstrumentId()       { return instrumentId; }
    public Long getCounterpartyId()     { return counterpartyId; }
    public BigDecimal getQuantity()     { return quantity; }
    public BigDecimal getPrice()        { return price; }
    public LocalDate getTradeDate()     { return tradeDate; }
    public TradeStatus getStatus()      { return status; }
    public Instant getCreatedAt()       { return createdAt; }

    /** Notional = quantity * price. Computed; not stored. */
    public BigDecimal getNotional() {
        return quantity == null || price == null ? null : quantity.multiply(price);
    }

    // Builder (I018) and equals/hashCode (I025) are added in later tickets.
}
```
</details>

**Files to touch:** `backend/src/main/java/com/dbtraining/tradeflow/model/Trade.java`.

---

### TICKET-I018 — `Trade.Builder` (Builder pattern)

**What**
- Add a fluent `public static final Builder` nested class with one chainable method per field and a `build()` that validates required fields and sign constraints.

**Why**
- Builders are the construction pattern the rest of the codebase will lean on — Day 3's services build Trades for tests, Day 5's JPA-to-domain mappers use them, Day 8's WireMock fixtures use them. Fail-fast validation in `build()` also catches missing fields at construction instead of as a `NullPointerException` deep in Day 4's DAO.

**Observe**
- `Trade.builder().build()` throws `NullPointerException` with the field name (e.g. `tradeRef required`); a fully populated builder produces a non-null `Trade` whose getters return what you set.

**Acceptance criteria:**
- [ ] `Trade.builder().tradeRef("TRD-1").quantity(...)....build()` works.
- [ ] Required fields are validated in `build()`; missing → `IllegalStateException`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

The Builder is a `public static final` nested class inside `Trade` that
mirrors every field, exposes one `setter`-style method per field (each
returning `this` so calls chain), and produces a `Trade` from a final
`build()` method. Keep the public `Trade` constructor `private` so callers
have no choice but to go through the Builder.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Add a `public static Builder builder()` factory on `Trade` returning
  `new Builder()`.
- Each Builder method looks like:
  ```java
  public Builder tradeRef(String v) { this.tradeRef = v; return this; }
  ```
- The `build()` method calls a private `Trade(Builder b)` constructor that
  copies fields across. Use `Objects.requireNonNull(...)` for each required
  field — if any is null, you get a clean `NullPointerException` with the
  field name.
- Defensive defaults: if caller didn't pass `status`, default to
  `TradeStatus.PENDING`; if no `createdAt`, default to `Instant.now()`.
- Sign checks: `quantity > 0`, `price >= 0`. Throw `IllegalStateException`
  if not.
</details>

<details>
<summary>Hint 3 — Builder skeleton</summary>

```java
// Inside Trade.java, after the getters:

public static Builder builder() { return new Builder(); }

private Trade(Builder b) {
    this.tradeRef       = b.tradeRef;
    this.instrumentId   = b.instrumentId;
    this.counterpartyId = b.counterpartyId;
    this.quantity       = b.quantity;
    this.price          = b.price;
    this.tradeDate      = b.tradeDate;
    this.status         = b.status != null ? b.status : TradeStatus.PENDING;
    this.createdAt      = b.createdAt != null ? b.createdAt : Instant.now();
}

public static final class Builder {
    private String tradeRef;
    private Long instrumentId;
    // TODO: rest of the fields

    public Builder tradeRef(String v) { this.tradeRef = v; return this; }
    // TODO: one fluent setter per field

    public Trade build() {
        // TODO: Objects.requireNonNull on every required field
        // TODO: sign checks on quantity / price
        return new Trade(this);
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/model/Trade.java`

```java
// backend/src/main/java/com/dbtraining/tradeflow/model/Trade.java
package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class Trade {

    private String tradeRef;
    private Long instrumentId;
    private Long counterpartyId;
    private BigDecimal quantity;
    private BigDecimal price;
    private LocalDate tradeDate;
    private TradeStatus status;
    private Instant createdAt;

    Trade() {}

    private Trade(Builder b) {
        this.tradeRef       = b.tradeRef;
        this.instrumentId   = b.instrumentId;
        this.counterpartyId = b.counterpartyId;
        this.quantity       = b.quantity;
        this.price          = b.price;
        this.tradeDate      = b.tradeDate;
        this.status         = b.status != null ? b.status : TradeStatus.PENDING;
        this.createdAt      = b.createdAt != null ? b.createdAt : Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    // (getters from I017 — omitted for brevity)

    public static final class Builder {
        private String tradeRef;
        private Long instrumentId;
        private Long counterpartyId;
        private BigDecimal quantity;
        private BigDecimal price;
        private LocalDate tradeDate;
        private TradeStatus status;
        private Instant createdAt;

        public Builder tradeRef(String v)        { this.tradeRef = v;       return this; }
        public Builder instrumentId(Long v)      { this.instrumentId = v;   return this; }
        public Builder counterpartyId(Long v)    { this.counterpartyId = v; return this; }
        public Builder quantity(BigDecimal v)    { this.quantity = v;       return this; }
        public Builder price(BigDecimal v)       { this.price = v;          return this; }
        public Builder tradeDate(LocalDate v)    { this.tradeDate = v;      return this; }
        public Builder status(TradeStatus v)     { this.status = v;         return this; }
        public Builder createdAt(Instant v)      { this.createdAt = v;      return this; }

        public Trade build() {
            Objects.requireNonNull(tradeRef,       "tradeRef required");
            Objects.requireNonNull(instrumentId,   "instrumentId required");
            Objects.requireNonNull(counterpartyId, "counterpartyId required");
            Objects.requireNonNull(quantity,       "quantity required");
            Objects.requireNonNull(price,          "price required");
            Objects.requireNonNull(tradeDate,      "tradeDate required");
            if (quantity.signum() <= 0) throw new IllegalStateException("quantity must be > 0");
            if (price.signum() < 0)    throw new IllegalStateException("price must be >= 0");
            return new Trade(this);
        }
    }
}
```
</details>

**Files to touch:** `Trade.java` (same file).

---

### TICKET-I019 — `TradeStatus` enum

**What**
- Replace the magic strings with a `TradeStatus` enum carrying values `PENDING, MATCHED, UNMATCHED, DISPUTED, CANCELLED` plus a `boolean isTerminal()` helper.

**Why**
- The values must match the DB CHECK constraint in `002-create-trades.xml` exactly so Day 5's `@Enumerated(EnumType.STRING)` mapping just works. `isTerminal()` is the first method on an enum students see — it's how they learn enums are full classes, not just labels.

**Observe**
- `System.out.println(TradeStatus.MATCHED.isTerminal())` prints `true`; `System.out.println(TradeStatus.PENDING.isTerminal())` prints `false`; `./mvnw compile` is clean.

**Acceptance criteria:**
- [ ] Enum has values: `PENDING, MATCHED, UNMATCHED, DISPUTED, CANCELLED`.
- [ ] Adds a `boolean isTerminal()` returning true for `MATCHED` / `CANCELLED`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Java enums are full classes — they can have methods, constructors, even
fields. Use that. The values must match the DB's CHECK constraint on
`trades.status` exactly (case included) so JPA's `@Enumerated(EnumType.STRING)`
mapping just works later. "Terminal" means no further transitions are
allowed.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Package: `com.dbtraining.tradeflow.model`.
- Five values, comma-separated, semicolon at the end.
- Add `public boolean isTerminal()` that returns true only for `MATCHED`
  and `CANCELLED`.
- Keep this in sync with `db/changelog/changes/002-create-trades.xml` —
  if either drifts, JPA will throw at load time on Day 5.
</details>

<details>
<summary>Hint 3 — Enum skeleton</summary>

```java
package com.dbtraining.tradeflow.model;

public enum TradeStatus {

    PENDING,
    MATCHED,
    UNMATCHED,
    DISPUTED,
    CANCELLED;

    public boolean isTerminal() {
        // TODO: return true for MATCHED or CANCELLED, false otherwise
        return false;
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/model/TradeStatus.java`

```java
// backend/src/main/java/com/dbtraining/tradeflow/model/TradeStatus.java
package com.dbtraining.tradeflow.model;

/**
 * TradeStatus — TICKET-I019.
 * Values match the DB CHECK constraint on trades.status. Replaces magic
 * strings so the compiler catches typos.
 */
public enum TradeStatus {

    PENDING,
    MATCHED,
    UNMATCHED,
    DISPUTED,
    CANCELLED;

    /**
     * A "terminal" status means no further transitions are allowed.
     * MATCHED and CANCELLED are terminal; PENDING / UNMATCHED / DISPUTED
     * are still in-flight.
     */
    public boolean isTerminal() {
        return this == MATCHED || this == CANCELLED;
    }
}
```
</details>

**Files to touch:** `backend/src/main/java/com/dbtraining/tradeflow/model/TradeStatus.java`.

---

### TICKET-I020 — `AssetClass` enum

**What**
- `AssetClass` enum with `EQUITY, FIXED_INCOME, FX, COMMODITY, DERIVATIVE` plus a `boolean isCash()` helper using an exhaustive switch expression.

**Why**
- `AssetClass` is consumed by `Instrument` (I023) and routed on by Day 3's recon validators (cash products dedup differently from derivatives). Exhaustive switch is the small payoff that turns "added a new asset class" from a hunt-and-replace into a compiler error.

**Observe**
- `AssetClass.FX.isCash()` returns `true`; `AssetClass.COMMODITY.isCash()` returns `false`; remove the `COMMODITY, DERIVATIVE` branch and `javac` complains "switch expression does not cover all possible input values".

**Acceptance criteria:**
- [ ] Values: `EQUITY, FIXED_INCOME, FX, COMMODITY, DERIVATIVE`.
- [ ] Helper `boolean isCash()` returns true for `EQUITY`, `FIXED_INCOME`, `FX`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Same shape as `TradeStatus` — five values plus one helper. `isCash()` is a
classification helper: cash products (EQUITY, FIXED_INCOME, FX) settle
T+2 through a single CSD; COMMODITY and DERIVATIVE follow a different
settlement pipeline.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Same package as the other enums.
- Use a switch expression (Java 14+) for `isCash()` — exhaustive and
  the compiler catches a missed case.
- This enum is consumed by `Instrument` (I023) and routed on by Day 3's
  validators.
</details>

<details>
<summary>Hint 3 — Enum skeleton</summary>

```java
package com.dbtraining.tradeflow.model;

public enum AssetClass {

    EQUITY,
    FIXED_INCOME,
    FX,
    COMMODITY,
    DERIVATIVE;

    public boolean isCash() {
        return switch (this) {
            case EQUITY, FIXED_INCOME, FX -> true;
            // TODO: handle COMMODITY and DERIVATIVE
        };
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/model/AssetClass.java`

```java
// backend/src/main/java/com/dbtraining/tradeflow/model/AssetClass.java
package com.dbtraining.tradeflow.model;

/**
 * AssetClass — TICKET-I020.
 * Drives subclass choice (EquityTrade vs FXTrade vs BondTrade on Day 3)
 * and downstream routing (FX goes to a different settlement system).
 */
public enum AssetClass {

    EQUITY,
    FIXED_INCOME,
    FX,
    COMMODITY,
    DERIVATIVE;

    /** Cash products = EQUITY, FIXED_INCOME, FX. COMMODITY + DERIVATIVE settle differently. */
    public boolean isCash() {
        return switch (this) {
            case EQUITY, FIXED_INCOME, FX -> true;
            case COMMODITY, DERIVATIVE   -> false;
        };
    }
}
```
</details>

**Files to touch:** `model/AssetClass.java`.

---

### TICKET-I021 — `DiscrepancyType` enum

**What**
- `DiscrepancyType` enum with `PRICE_MISMATCH, QUANTITY_MISMATCH, DATE_MISMATCH, MISSING_TRADE` plus a `String describe()` returning short human-readable reason strings.

**Why**
- These reason codes are attached to every recon break Day 3 produces and shown to ops in the Day 7 UI; locking the four values now keeps Day 3's `ReconciliationService.matchTrades()` from inventing strings. `describe()` is also where students see enums can carry data, not just behaviour.

**Observe**
- `DiscrepancyType.PRICE_MISMATCH.describe()` prints the full sentence; calling `.describe()` on every value in `DiscrepancyType.values()` returns four non-null distinct strings.

**Acceptance criteria:**
- [ ] Values: `PRICE_MISMATCH, QUANTITY_MISMATCH, DATE_MISMATCH, MISSING_TRADE`.
- [ ] Each has a human-readable `String describe()` method.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

These are reason codes ops users see in the recon report. The `describe()`
helper returns plain English for the UI / CSV export — keep it short, no
trailing period needed.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Same package, same file pattern.
- Use a switch expression on `this` to return the description.
- These values feed Day 3's `ReconciliationService.matchTrades()` — when a
  comparison fails, pick the matching enum and attach it to the recon break.
</details>

<details>
<summary>Hint 3 — Enum skeleton</summary>

```java
package com.dbtraining.tradeflow.model;

public enum DiscrepancyType {

    PRICE_MISMATCH,
    QUANTITY_MISMATCH,
    DATE_MISMATCH,
    MISSING_TRADE;

    public String describe() {
        return switch (this) {
            case PRICE_MISMATCH    -> "Price does not match counterparty record";
            // TODO: the other three cases
        };
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/model/DiscrepancyType.java`

```java
// backend/src/main/java/com/dbtraining/tradeflow/model/DiscrepancyType.java
package com.dbtraining.tradeflow.model;

/**
 * DiscrepancyType — TICKET-I021.
 * Reason code for a reconciliation break. Ops users see the describe()
 * output in the recon UI and CSV export.
 */
public enum DiscrepancyType {

    PRICE_MISMATCH,
    QUANTITY_MISMATCH,
    DATE_MISMATCH,
    MISSING_TRADE;

    /** Short human-readable string for UI / CSV export. */
    public String describe() {
        return switch (this) {
            case PRICE_MISMATCH    -> "Price does not match counterparty record";
            case QUANTITY_MISMATCH -> "Quantity does not match counterparty record";
            case DATE_MISMATCH     -> "Trade or settlement date mismatch";
            case MISSING_TRADE     -> "Trade exists on one side only";
        };
    }
}
```
</details>

**Files to touch:** `model/DiscrepancyType.java`.

---

### TICKET-I022 — `Counterparty` class

**What**
- POJO mirroring the `counterparties` table with fields `id`, `name`, `leiCode`, `region` plus a fluent Builder that validates the 20-char LEI and the `APAC|EMEA|NAMR|LATAM` region values.

**Why**
- Equality on `leiCode` (globally-unique Legal Entity Identifier) is what Day 3's reconciliation will use to join internal trades to counterparty feeds — two records with the same LEI are the same firm. Validating LEI length now means Day 4's DAO never has to.

**Observe**
- A builder with `region("MARS")` throws `IllegalStateException`; `region("EMEA")` succeeds; two `Counterparty` instances with the same `leiCode` but different `name` return `true` from `.equals()` and have identical `hashCode()`.

**Acceptance criteria:**
- [ ] Fields: `id, name, leiCode, region`.
- [ ] `region` is a `String` enum (APAC/EMEA/NAMR/LATAM) — or its own enum if you prefer.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Same shape as `Trade`: POJO + Builder, private fields, getters only, no
public constructor. Equality is on `leiCode` (a Legal Entity Identifier is
globally unique — same LEI means same firm, even across feeds). `region`
can stay a `String` for now if you validate the four allowed values in the
Builder.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Four fields: `Long id`, `String name`, `String leiCode`, `String region`.
- `leiCode` is exactly 20 characters (LEI standard) — validate in `build()`.
- `region` must match `APAC|EMEA|NAMR|LATAM` — also validated in `build()`.
- Mirror the `Trade.Builder` pattern: nested static class, fluent setters,
  `build()` runs `Objects.requireNonNull` and length / regex checks.
- equals / hashCode use only `leiCode` (Day 3 reconciliation joins on it).
</details>

<details>
<summary>Hint 3 — Class skeleton</summary>

```java
package com.dbtraining.tradeflow.model;

import java.util.Objects;

public class Counterparty {

    private Long id;
    private String name;
    private String leiCode;
    private String region;

    Counterparty() {}

    private Counterparty(Builder b) {
        // TODO: copy fields
    }

    public static Builder builder() { return new Builder(); }

    // TODO: getters
    // TODO: equals + hashCode on leiCode

    public static final class Builder {
        // TODO: mirror fields + fluent setters + build() with length / regex checks
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/model/Counterparty.java`

```java
// backend/src/main/java/com/dbtraining/tradeflow/model/Counterparty.java
package com.dbtraining.tradeflow.model;

import java.util.Objects;

/**
 * Counterparty — POJO mirroring the counterparties table.
 * Equality on leiCode (globally-unique Legal Entity Identifier).
 */
public class Counterparty {

    private Long id;
    private String name;
    private String leiCode;
    private String region;

    Counterparty() {}

    private Counterparty(Builder b) {
        this.name    = b.name;
        this.leiCode = b.leiCode;
        this.region  = b.region;
    }

    public static Builder builder() { return new Builder(); }

    public Long getId()        { return id; }
    public String getName()    { return name; }
    public String getLeiCode() { return leiCode; }
    public String getRegion()  { return region; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Counterparty other)) return false;
        return Objects.equals(leiCode, other.leiCode);
    }

    @Override public int hashCode() { return Objects.hash(leiCode); }

    @Override public String toString() {
        return "Counterparty[" + leiCode + " | " + name + " | " + region + "]";
    }

    public static final class Builder {
        private String name;
        private String leiCode;
        private String region;

        public Builder name(String v)    { this.name = v;    return this; }
        public Builder leiCode(String v) { this.leiCode = v; return this; }
        public Builder region(String v)  { this.region = v;  return this; }

        public Counterparty build() {
            Objects.requireNonNull(name,    "name required");
            Objects.requireNonNull(leiCode, "leiCode required");
            Objects.requireNonNull(region,  "region required");
            if (leiCode.length() != 20)
                throw new IllegalStateException("leiCode must be exactly 20 chars (LEI standard)");
            if (!region.matches("APAC|EMEA|NAMR|LATAM"))
                throw new IllegalStateException("region must be one of APAC|EMEA|NAMR|LATAM");
            return new Counterparty(this);
        }
    }
}
```
</details>

**Files to touch:** `model/Counterparty.java`.

---

### TICKET-I023 — `Instrument` class

**What**
- POJO mirroring the `instruments` table with fields `id`, `symbol`, `name`, `assetClass` (the I020 enum), `currency` (ISO 4217, 3 chars) and an optional `isin`; Builder validates currency length and ISIN length when set.

**Why**
- Reuses the same Builder shape as I017 / I022 so students see the pattern is mechanical, not one-off. Equality on `symbol` lets Day 3's recon dedup instruments across feeds (`SAP.DE` from Murex equals `SAP.DE` from Reuters even when the names differ).

**Observe**
- `Instrument.builder().symbol("EURUSD").name("EUR/USD").assetClass(AssetClass.FX).currency("EU").build()` throws `IllegalStateException` ("currency must be ISO-4217 3-letter code"); `currency("USD")` succeeds.

**Acceptance criteria:**
- [ ] Fields: `id, symbol, name, assetClass (AssetClass), currency`.
- [ ] `currency` is `String` of length 3 — validate in setter or builder.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Same shape as `Counterparty`. `currency` is fixed-width (ISO 4217 = always
3 letters: `USD`, `EUR`, `GBP`). Validate length in the Builder's `build()`.
Equality is on `symbol` — `SAP.DE` is `SAP.DE` regardless of feed.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Five fields per AC: `Long id`, `String symbol`, `String name`,
  `AssetClass assetClass`, `String currency`.
- Add an optional `String isin` (12 chars when present, nullable for FX /
  commodities) — handy for Day 4's DAO lookups, costs nothing now.
- Builder validations: `currency.length() == 3`; if `isin` is non-null,
  `isin.length() == 12`.
- equals / hashCode on `symbol` only.
</details>

<details>
<summary>Hint 3 — Class skeleton</summary>

```java
package com.dbtraining.tradeflow.model;

import java.util.Objects;

public class Instrument {

    private Long id;
    private String symbol;
    private String name;
    private AssetClass assetClass;
    private String currency;
    private String isin;   // optional

    Instrument() {}

    private Instrument(Builder b) {
        // TODO: copy fields
    }

    public static Builder builder() { return new Builder(); }

    // TODO: getters, equals/hashCode on symbol, toString

    public static final class Builder {
        // TODO: fluent setters + build() with currency length check
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/model/Instrument.java`

```java
// backend/src/main/java/com/dbtraining/tradeflow/model/Instrument.java
package com.dbtraining.tradeflow.model;

import java.util.Objects;

/**
 * Instrument — POJO mirroring the instruments table.
 * Equality on symbol (e.g. "SAP.DE", "EURUSD"). ISIN may be null for FX
 * and commodities.
 */
public class Instrument {

    private Long id;
    private String symbol;
    private String name;
    private AssetClass assetClass;
    private String currency;
    private String isin;

    Instrument() {}

    private Instrument(Builder b) {
        this.symbol     = b.symbol;
        this.name       = b.name;
        this.assetClass = b.assetClass;
        this.currency   = b.currency;
        this.isin       = b.isin;
    }

    public static Builder builder() { return new Builder(); }

    public Long getId()              { return id; }
    public String getSymbol()        { return symbol; }
    public String getName()          { return name; }
    public AssetClass getAssetClass(){ return assetClass; }
    public String getCurrency()      { return currency; }
    public String getIsin()          { return isin; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Instrument other)) return false;
        return Objects.equals(symbol, other.symbol);
    }

    @Override public int hashCode() { return Objects.hash(symbol); }

    @Override public String toString() {
        return "Instrument[" + symbol + " | " + name + " | " + assetClass + " " + currency + "]";
    }

    public static final class Builder {
        private String symbol;
        private String name;
        private AssetClass assetClass;
        private String currency;
        private String isin;

        public Builder symbol(String v)         { this.symbol = v;     return this; }
        public Builder name(String v)           { this.name = v;       return this; }
        public Builder assetClass(AssetClass v) { this.assetClass = v; return this; }
        public Builder currency(String v)       { this.currency = v == null ? null : v.toUpperCase(); return this; }
        public Builder isin(String v)           { this.isin = v;       return this; }

        public Instrument build() {
            Objects.requireNonNull(symbol,     "symbol required");
            Objects.requireNonNull(name,       "name required");
            Objects.requireNonNull(assetClass, "assetClass required");
            Objects.requireNonNull(currency,   "currency required");
            if (currency.length() != 3)
                throw new IllegalStateException("currency must be ISO-4217 3-letter code");
            if (isin != null && isin.length() != 12)
                throw new IllegalStateException("ISIN must be exactly 12 chars when set");
            return new Instrument(this);
        }
    }
}
```
</details>

**Files to touch:** `model/Instrument.java`.

---

### TICKET-I024 — `ReconResult` class

**What**
- POJO + Builder recording one reconciliation break: `id`, `tradeId`, `status` (`"OPEN"|"RESOLVED"|"SUPPRESSED"` as plain String for Day 2), `discrepancyType` (the I021 enum), `detectedAt`, nullable `resolvedAt`, plus an idempotent `resolve()` method.

**Why**
- This is the row Day 3's `ReconciliationService` will write every time `matchTrades()` finds a discrepancy; locking the shape now means Day 3 just calls `.builder()...build()`. `status` stays a String today on purpose — Day 5 promotes it to a `ReconStatus` enum and students see the migration cost first-hand.

**Observe**
- Build a `ReconResult` and `isOpen()` is `true`; call `resolve()` and `isOpen()` is `false`, `getResolvedAt()` is non-null, calling `resolve()` again is a no-op (idempotent).

**Acceptance criteria:**
- [ ] Fields: `id, tradeId, status, discrepancyType, resolvedAt`.
- [ ] `status` is `String` for now (we'll add a `ReconStatus` enum on Day 5).
- [ ] `resolvedAt` can be `null` while break is open.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

A `ReconResult` records one reconciliation break against one `Trade`. Keep
the link by `tradeId` (Long) for now — we'll swap to a `Trade` reference on
Day 5 when JPA enters. `resolvedAt` is `null` while the break is open and
gets stamped when ops marks it resolved.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Five fields: `Long id`, `Long tradeId`, `String status`,
  `DiscrepancyType discrepancyType`, `Instant resolvedAt`.
- `status` allowed values: `"OPEN"`, `"RESOLVED"`, `"SUPPRESSED"` (Day 5
  promotes these to an enum).
- Add a `detectedAt` (Instant) too — useful for the audit trail; defaults
  to `Instant.now()` when not set.
- Add a `resolve()` method that sets `status="RESOLVED"` and stamps
  `resolvedAt = Instant.now()` — idempotent (no-op if already RESOLVED).
- Builder pattern same as the rest; `discrepancyType` is required,
  `tradeId` is required.
</details>

<details>
<summary>Hint 3 — Class skeleton</summary>

```java
package com.dbtraining.tradeflow.model;

import java.time.Instant;
import java.util.Objects;

public class ReconResult {

    private Long id;
    private Long tradeId;
    private String status;                 // "OPEN" | "RESOLVED" | "SUPPRESSED"
    private DiscrepancyType discrepancyType;
    private Instant detectedAt;
    private Instant resolvedAt;            // null while open

    ReconResult() {}

    private ReconResult(Builder b) {
        // TODO: copy fields, default status to "OPEN", detectedAt to now()
    }

    public static Builder builder() { return new Builder(); }

    // TODO: getters
    // TODO: resolve() — sets status="RESOLVED", stamps resolvedAt
    // TODO: isOpen() helper

    public static final class Builder {
        // TODO: fluent setters + build() (discrepancyType required, tradeId required)
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/model/ReconResult.java`

```java
// backend/src/main/java/com/dbtraining/tradeflow/model/ReconResult.java
package com.dbtraining.tradeflow.model;

import java.time.Instant;
import java.util.Objects;

/**
 * ReconResult — POJO recording one reconciliation break against one Trade.
 * status is a String for Day 2; Day 5 promotes it to a ReconStatus enum.
 */
public class ReconResult {

    private Long id;
    private Long tradeId;
    private String status;
    private DiscrepancyType discrepancyType;
    private Instant detectedAt;
    private Instant resolvedAt;

    ReconResult() {}

    private ReconResult(Builder b) {
        this.tradeId         = b.tradeId;
        this.discrepancyType = b.discrepancyType;
        this.status          = b.status != null ? b.status : "OPEN";
        this.detectedAt      = b.detectedAt != null ? b.detectedAt : Instant.now();
        this.resolvedAt      = b.resolvedAt;
    }

    public static Builder builder() { return new Builder(); }

    public Long getId()                        { return id; }
    public Long getTradeId()                   { return tradeId; }
    public String getStatus()                  { return status; }
    public DiscrepancyType getDiscrepancyType(){ return discrepancyType; }
    public Instant getDetectedAt()             { return detectedAt; }
    public Instant getResolvedAt()             { return resolvedAt; }

    /** Mark this break resolved; sets resolvedAt = now. Idempotent. */
    public void resolve() {
        if ("RESOLVED".equals(this.status)) return;
        this.status = "RESOLVED";
        this.resolvedAt = Instant.now();
    }

    public boolean isOpen() { return "OPEN".equals(status); }

    @Override public String toString() {
        return "ReconResult[trade=" + tradeId + " | " + discrepancyType + " | " + status + "]";
    }

    public static final class Builder {
        private Long tradeId;
        private String status;
        private DiscrepancyType discrepancyType;
        private Instant detectedAt;
        private Instant resolvedAt;

        public Builder tradeId(Long v)                      { this.tradeId = v;         return this; }
        public Builder status(String v)                     { this.status = v;          return this; }
        public Builder discrepancyType(DiscrepancyType v)   { this.discrepancyType = v; return this; }
        public Builder detectedAt(Instant v)                { this.detectedAt = v;      return this; }
        public Builder resolvedAt(Instant v)                { this.resolvedAt = v;      return this; }

        public ReconResult build() {
            Objects.requireNonNull(tradeId,         "tradeId required");
            Objects.requireNonNull(discrepancyType, "discrepancyType required");
            return new ReconResult(this);
        }
    }
}
```
</details>

**Files to touch:** `model/ReconResult.java`.

---

### TICKET-I025 — Override `equals()` and `hashCode()` on `Trade`

**What**
- Override `equals(Object)` and `hashCode()` on `Trade` using only `tradeRef`, plus a manual two-`Trade` assertion in `main` to prove the contract.

**Why**
- `tradeRef` is the natural / business key — two feeds publishing `TRD-2026-0001` are the same trade even if their timestamps differ. Day 3's reconciliation hashes Trades into a `HashMap<String, Trade>` keyed on this; break the contract and dedup silently misbehaves.

**Observe**
- Two trades built with the same `tradeRef` but different `price` and `quantity` satisfy `a.equals(b) == true` and `a.hashCode() == b.hashCode()`; two trades with different `tradeRef` return `false` from `.equals()`.

**Acceptance criteria:**
- [ ] `equals()` uses only `tradeRef`.
- [ ] `hashCode()` consistent with `equals()`.
- [ ] Unit test (manual `assertEquals` in a `main` is fine for today).

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

The contract: if `a.equals(b)` then `a.hashCode() == b.hashCode()`. Break
that and `HashMap`, `HashSet`, and Day 3's reconciliation dedup all
silently misbehave. Equality is on `tradeRef` because that's the business
key — two feeds publishing `"TRD-2026-0001"` are the same trade.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Use IntelliJ → `Cmd+N` → `equals() and hashCode()` to generate boilerplate,
  then trim to use only `tradeRef`.
- Use the `instanceof` pattern (Java 16+):
  `if (!(o instanceof Trade other)) return false;` — cleaner than a cast.
- Test in `main`: build two `Trade`s with the same `tradeRef` but different
  prices; `t1.equals(t2)` must be `true`, `t1.hashCode() == t2.hashCode()`.
</details>

<details>
<summary>Hint 3 — Method skeleton</summary>

```java
// Inside Trade.java, after the getters and before the Builder:

@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Trade other)) return false;
    // TODO: compare tradeRef only — use Objects.equals(...)
    return false;
}

@Override
public int hashCode() {
    // TODO: hash only on tradeRef
    return 0;
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/model/Trade.java`

```java
// backend/src/main/java/com/dbtraining/tradeflow/model/Trade.java
// Append these methods inside class Trade (after the getters, before the Builder).
// Add `import java.util.Objects;` near the top of the file if not already there.

@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Trade other)) return false;
    return Objects.equals(tradeRef, other.tradeRef);
}

@Override
public int hashCode() {
    return Objects.hash(tradeRef);
}

@Override
public String toString() {
    return "Trade[" + tradeRef
            + " | " + instrumentId
            + " | " + quantity + " @ " + price
            + " | " + tradeDate
            + " | " + status + "]";
}
```

Then add this two-Trade assertion inside `TradeflowApplication.main` (this is the AC's "unit test in main"):

```java
// backend/src/main/java/com/dbtraining/tradeflow/TradeflowApplication.java
Trade a = Trade.builder()
        .tradeRef("TRD-1")
        .instrumentId(1L).counterpartyId(1L)
        .quantity(new BigDecimal("100")).price(new BigDecimal("50.00"))
        .tradeDate(LocalDate.now())
        .build();
Trade b = Trade.builder()
        .tradeRef("TRD-1")                     // same tradeRef ...
        .instrumentId(1L).counterpartyId(1L)
        .quantity(new BigDecimal("200")).price(new BigDecimal("99.99"))  // ... different qty/price
        .tradeDate(LocalDate.now())
        .build();
if (!a.equals(b))                    throw new AssertionError("equals broken");
if (a.hashCode() != b.hashCode())    throw new AssertionError("hashCode broken");
```
</details>

**Files to touch:** `Trade.java`.

---

### TICKET-I026 — Console `main` — formatted trade list

**What**
- Add a `printDay2Demo()` private static method on `TradeflowApplication` that builds 5 hardcoded `Trade` instances via the Builder and prints them as a column-aligned `TRADE_REF | INSTRUMENT_ID | CP_ID | QTY | PRICE | DATE | STATUS` table.

**Why**
- Proves end-to-end that the domain model compiles and constructs without a DB — the last sanity check before Day 4 wires JDBC. The `printf` format-string habit (`%-15s`, `%n`) is what students will reuse all week for diagnostics.

**Observe**
- `./mvnw exec:java -Dexec.mainClass=com.dbtraining.tradeflow.TradeflowApplication` prints the banner, a header row, a `---` separator, and 5 trade rows whose `STATUS` column stays fixed-width even when values vary between `MATCHED` (7 chars) and `UNMATCHED` (9 chars).

**Acceptance criteria:**
- [ ] Output columns: `TRADE_REF | INSTRUMENT_ID | CP_ID | QTY | PRICE | DATE | STATUS`.
- [ ] Status column has fixed width (so rows align).
- [ ] Easy to read in a terminal.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

`System.out.printf("%-12s | %-13s | ...%n", ...)` — the `-` left-aligns and
the number pins the column width. Hardcode 5 sample trades inline in
`TradeflowApplication.main` (no DB yet — that's Day 4). Use `List.of(...)`
+ `forEach` to print them.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Add a private static `printDay2Demo()` method on `TradeflowApplication`
  and call it from `main` after `printBanner()`.
- Build five `Trade` instances using the Builder. Pick a mix of statuses
  so the column-width contract is visible (PENDING / MATCHED / UNMATCHED /
  DISPUTED / CANCELLED — all are 8-9 chars).
- The header row uses the same `printf` format string as the data rows so
  columns line up.
- Add a separator line: `System.out.println("-".repeat(95));`.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
private static void printDay2Demo() {
    List<Trade> trades = List.of(
        Trade.builder().tradeRef("TRD-2026-0001")
            .instrumentId(1L).counterpartyId(1L)
            .quantity(new BigDecimal("1000.00")).price(new BigDecimal("245.50"))
            .tradeDate(LocalDate.of(2026, 3, 1)).status(TradeStatus.MATCHED).build()
        // TODO: 4 more sample trades, mixed statuses
    );

    System.out.printf("%-15s | %-13s | %-5s | %-10s | %-10s | %-12s | %-10s%n",
        "TRADE_REF", "INSTRUMENT_ID", "CP_ID", "QTY", "PRICE", "DATE", "STATUS");
    System.out.println("-".repeat(95));
    // TODO: forEach trade -> System.out.printf(...) using the same format
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/TradeflowApplication.java`

```java
// backend/src/main/java/com/dbtraining/tradeflow/TradeflowApplication.java
package com.dbtraining.tradeflow;

import com.dbtraining.tradeflow.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class TradeflowApplication {

    public static void main(String[] args) {
        printBanner();
        printDay2Demo();
    }

    private static void printBanner() {
        System.out.println();
        System.out.println("  Deutsche Bank — TDI 2026 Graduate Technical Training");
        System.out.println("  Intermediate Track — Case Study: Trade Reconciliation");
        System.out.println();
    }

    /** TICKET-I026 — Console demo of the domain model independent of the DB. */
    private static void printDay2Demo() {
        List<Trade> trades = List.of(
            Trade.builder().tradeRef("TRD-2026-0001")
                .instrumentId(1L).counterpartyId(1L)
                .quantity(new BigDecimal("1000.00")).price(new BigDecimal("245.50"))
                .tradeDate(LocalDate.of(2026, 3, 1)).status(TradeStatus.MATCHED).build(),
            Trade.builder().tradeRef("TRD-2026-0002")
                .instrumentId(1L).counterpartyId(2L)
                .quantity(new BigDecimal("500.00")).price(new BigDecimal("246.00"))
                .tradeDate(LocalDate.of(2026, 3, 1)).status(TradeStatus.UNMATCHED).build(),
            Trade.builder().tradeRef("TRD-2026-0003")
                .instrumentId(2L).counterpartyId(1L)
                .quantity(new BigDecimal("100000.00")).price(new BigDecimal("99.50"))
                .tradeDate(LocalDate.of(2026, 3, 2)).status(TradeStatus.MATCHED).build(),
            Trade.builder().tradeRef("TRD-2026-0004")
                .instrumentId(3L).counterpartyId(2L)
                .quantity(new BigDecimal("10.00")).price(new BigDecimal("2125.75"))
                .tradeDate(LocalDate.of(2026, 3, 3)).status(TradeStatus.DISPUTED).build(),
            Trade.builder().tradeRef("TRD-2026-0005")
                .instrumentId(2L).counterpartyId(3L)
                .quantity(new BigDecimal("750.00")).price(new BigDecimal("100.25"))
                .tradeDate(LocalDate.of(2026, 3, 4)).status(TradeStatus.PENDING).build()
        );

        System.out.println();
        System.out.println("== Day-2 domain-model demo (TICKET-I026) ===========================================");
        System.out.printf("%-15s | %-13s | %-5s | %-10s | %-10s | %-12s | %-10s%n",
            "TRADE_REF", "INSTRUMENT_ID", "CP_ID", "QTY", "PRICE", "DATE", "STATUS");
        System.out.println("-".repeat(95));
        trades.forEach(t -> System.out.printf("%-15s | %-13s | %-5s | %10s | %10s | %-12s | %-10s%n",
            t.getTradeRef(),
            t.getInstrumentId(),
            t.getCounterpartyId(),
            t.getQuantity(),
            t.getPrice(),
            t.getTradeDate(),
            t.getStatus()));
        System.out.println("====================================================================================");
        System.out.println();
    }
}
```
</details>

**Files to touch:** `TradeflowApplication.java`.

---

### TICKET-I027 — Push to develop + first PR

**What**
- Branch from up-to-date `develop`, stage one commit per ticket with `[TICKET-IXXX]` in each message, push the feature branch, and open a PR against `develop` (not `main`) with one reviewer assigned.

**Why**
- This is the first real GitFlow round-trip students do as a cohort — the per-ticket commits and `[TICKET-IXXX]` prefix establish the trace-to-ticket habit that every PR for the rest of the programme will be reviewed against. Merging into `develop` (not `main`) also keeps Day 9's release exercise honest.

**Observe**
- `gh pr view` shows base = `develop`; `git log --oneline develop..HEAD` lists one commit per ticket each beginning with `[TICKET-I0`; CI status on the PR is green before the merge button gets pressed.

**Acceptance criteria:**
- [ ] All Day 2 work merged via PR into `develop`.
- [ ] One team-mate reviews; comments addressed.
- [ ] Commit messages reference ticket IDs (`[TICKET-I017] Add Trade class`).

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

GitFlow recap: feature branches branch from `develop`, ship via PR back
into `develop`. Master / `main` is release-only. Your commits must each
reference a ticket ID in the message so reviewers can trace work back to
the issue tracker.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Feature branch name: `feature/day2-domain-model` (or split per ticket
  if you prefer — `feature/I017-trade-class`, etc.).
- One commit per logical unit — don't lump all 11 model classes into a
  single "[TICKET-I017-I026] Day 2 work" commit. Reviewers can't follow it.
- Self-review the diff before pushing — IntelliJ's "Local Changes" view or
  `git diff --stat develop...HEAD` makes this easy.
- After push, open the PR against `develop` (not `main`), assign one
  reviewer, link the ticket IDs in the description.
</details>

<details>
<summary>Hint 3 — Commands and PR template</summary>

```bash
# From the repo root
git checkout develop
git pull
git checkout -b feature/day2-domain-model

# Stage and commit one ticket at a time
git add backend/src/main/java/com/dbtraining/tradeflow/model/Trade.java
git commit -m "[TICKET-I017] Add Trade class (POJO + getters)"

git add backend/src/main/java/com/dbtraining/tradeflow/model/Trade.java
git commit -m "[TICKET-I018] Add Trade.Builder with validation"

# ...repeat for each ticket...

git push -u origin feature/day2-domain-model
```

PR description template:
```
## Summary
Day 2 — domain model. Implements TICKET-I016 through TICKET-I026.

## Tickets
- I016: package scaffold
- I017: Trade POJO
- I018: Trade.Builder
- I019: TradeStatus enum
- I020: AssetClass enum
- I021: DiscrepancyType enum
- I022: Counterparty
- I023: Instrument
- I024: ReconResult
- I025: Trade equals/hashCode
- I026: Console demo

## Verification
- ./mvnw compile : BUILD SUCCESS
- ./mvnw exec:java prints the formatted trade table
```
</details>

<details>
<summary>Reference — full walkthrough</summary>

```bash
# 1. Branch from up-to-date develop
git checkout develop
git pull origin develop
git checkout -b feature/day2-domain-model

# 2. Self-review before pushing
git diff develop...HEAD --stat
git diff develop...HEAD             # full diff, scroll through it

# 3. Confirm CI is green locally before pushing
./mvnw verify

# 4. Push and open PR
git push -u origin feature/day2-domain-model
# Open the PR via GitHub UI or:
gh pr create --base develop --title "Day 2 — domain model (I016–I026)" \
  --body-file .github/pull_request_template.md

# 5. After review approval — squash is the team standard
gh pr merge --squash

# 6. Delete the feature branch (GitHub usually does this, but verify)
git push origin --delete feature/day2-domain-model
```

**Steps:**
1. Stage each ticket's files as its own commit (see Hint 3), each message
   prefixed `[TICKET-IXXX]`.
2. Push the branch and open the PR against `develop` (never `main`).
3. Tag one reviewer; respond to comments by pushing more commits to the
   same branch — no force-push needed, the PR squash-merges on the way in.
4. Once approved and CI is green, merge into `develop` and delete the
   feature branch.
</details>

---

## Run and Observe — End of Sprint 1 (Domain Model)

You've shipped 12 tickets (I016–I027): five sub-packages, three enums,
four POJOs, a Builder, an `equals/hashCode` override on `Trade`, and a
console `main` that prints a formatted trade list. No DB, no Spring Boot
yet — just plain Java. Prove the model compiles and runs end-to-end before
you open the PR.

**Run:**

> **Terminal** — from `tradeflow-studentscopy/backend/`

```bash
./mvnw clean compile
./mvnw exec:java -Dexec.mainClass=com.dbtraining.tradeflow.TradeflowApplication
```

The compile should end with `BUILD SUCCESS` and zero warnings. The
`exec:java` run should print your banner followed by the column-aligned
trade table from TICKET-I026.

**Observe:**

| Check | Expected after Sprint 1 |
|---|---|
| `./mvnw clean compile` | `BUILD SUCCESS`, 0 errors, 0 warnings |
| `ls backend/src/main/java/com/dbtraining/tradeflow/model/` | 7 files — `Trade.java`, `Counterparty.java`, `Instrument.java`, `ReconResult.java`, `TradeStatus.java`, `AssetClass.java`, `DiscrepancyType.java` (+ optional `package-info.java`) |
| `./mvnw exec:java ...` output | Banner + table header + 5 trade rows, columns visibly aligned, STATUS column fixed-width |
| `find backend/src/main/java -name 'package-info.java' \| wc -l` | At least 5 — one per sub-package (model, service, dao, util, exception) |
| `grep -rn 'public void set' backend/src/main/java/com/dbtraining/tradeflow/model/` | No matches — `Trade`, `Counterparty`, `Instrument`, `ReconResult` are immutable (Builder-only construction) |

**Negative tests — prove your Builder validation and `equals` actually work:**

```java
// 1. Builder rejects null tradeRef (TICKET-I018 AC: required-field validation)
Trade.builder()
    .instrumentId(1L).counterpartyId(1L)
    .quantity(new BigDecimal("100")).price(new BigDecimal("50"))
    .tradeDate(LocalDate.now()).status(TradeStatus.PENDING)
    .build();
// Expected: IllegalStateException or NullPointerException — tradeRef is required.

// 2. Two Trades with the same tradeRef are equal (TICKET-I025)
Trade a = Trade.builder().tradeRef("TRD-1").instrumentId(1L).counterpartyId(1L)
    .quantity(new BigDecimal("1")).price(new BigDecimal("1"))
    .tradeDate(LocalDate.now()).status(TradeStatus.PENDING).build();
Trade b = Trade.builder().tradeRef("TRD-1").instrumentId(2L).counterpartyId(9L)
    .quantity(new BigDecimal("99")).price(new BigDecimal("99"))
    .tradeDate(LocalDate.now()).status(TradeStatus.MATCHED).build();
assert a.equals(b) && a.hashCode() == b.hashCode();
// Expected: assertion passes — equality keyed on tradeRef per I025.
```

You can paste these into `TradeflowApplication.main` temporarily, or write
them as JUnit tests under `backend/src/test/java/.../model/`.

**If something looks wrong:** compile errors usually point at a missing
`import` (the enums in `model/` package vs. the root `TradeflowApplication`)
or a package declaration that doesn't match the folder path. Re-read
TICKET-I016 Hint 3 (layout) and TICKET-I025 Hint 2 (equals contract).

---

## Sprint 3 — Liquibase taught + applied (30 min teach + 60 min lab)

This is where Liquibase is taught proper (per the Excel curriculum) and
applied to the changeset XML files you wrote yesterday on Day 1. Two
graded tickets — `I009` (wire) + `I010` (run + verify).

**Full step-by-step walkthrough:** [day02-liquibase.md](./day02-liquibase.md) —
read before the lab, refer to during it.

### Just-in-time primer — Liquibase in 5 minutes

You wrote 8-9 changeset XML files yesterday under
`backend/src/main/resources/db/changelog/changes/`. Today you wire them
into the master changelog, boot the app, and watch every changeset go
green.

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
   run and Liquibase will refuse to start (checksum mismatch). To change
   something later, write a *new* changeset.
2. **Order matters.** Parent tables before children, FKs after both tables
   exist.
3. **Every changeset needs a `<rollback>` block.**
4. **No `.sql` files for schema.** Use Liquibase's XML elements.

---

### TICKET-I009 — Wire the master changelog

**What**
- Update `db.changelog-master.xml` to `<include>` every Day-1 changeset in dependency order (parents → children → FKs → seed → views → audit_log).
- Add the Spring Boot Liquibase wiring: `liquibase-core` on `pom.xml`, `spring.liquibase.change-log` (no `classpath:` prefix), and `spring.jpa.hibernate.ddl-auto: none`.

**Why**
- I009 is where the 8–9 changesets students authored on Day 1 finally meet the runtime — without the master `<include>` list and the YAML pointer, Liquibase silently runs zero changesets and the H2 schema stays empty. This is also where the "no `classpath:` prefix" gotcha bites; better here than on Day 4 when JDBC is added on top.

**Observe**
- `grep -c '<include' backend/src/main/resources/db/changelog/db.changelog-master.xml` returns 8 or 9 (one per Day-1 changeset); `grep -n 'classpath:' backend/src/main/resources/application.yml` returns no `spring.liquibase.change-log:` hits; `grep -n 'ddl-auto' backend/src/main/resources/application.yml` shows `none`.

**Acceptance criteria:**
- [ ] `org.liquibase:liquibase-core` is on the `backend/pom.xml` classpath
  (Boot 3 auto-configures — **no** `spring-boot-starter-liquibase` needed).
- [ ] `application.yml` sets
  `spring.liquibase.change-log: db/changelog/db.changelog-master.xml`
  (NO `classpath:` prefix).
- [ ] `spring.jpa.hibernate.ddl-auto: none` — Liquibase owns the schema.
- [ ] All 8-9 Day-1 changesets included in the master changelog.
- [ ] Order: parent tables (`instruments`, `counterparties`) before
  children (`trades`, `settlements`, `recon_breaks`); FKs/indexes after;
  seed + views + `audit_log` last.

**Files to touch:** `backend/pom.xml`, `backend/src/main/resources/application.yml`, `backend/src/main/resources/db/changelog/db.changelog-master.xml`.

---

### TICKET-I010 — Run the migration + verify

**What**
- Run `./mvnw spring-boot:run` against the dev (H2) profile and verify in `/h2-console` that every Day-1 changeset applied, all 5 business tables + `audit_log` + 2 views exist, and `DATABASECHANGELOG` has one row per `<include>`.

**Why**
- This is the first time students see their authored schema actually materialise — turning Day 1's XML from "we wrote it" into "the database has the tables". It's also the rehearsal for Day 4: if Liquibase boots clean today, tomorrow's JDBC connection just inherits a populated schema.

**Observe**
- Boot log contains `Successfully acquired change log lock` and one `ChangeSet ... ran successfully` line per Day-1 changeset; `/h2-console` `SELECT COUNT(*) FROM DATABASECHANGELOG WHERE EXECTYPE='EXECUTED'` returns the same count as the `<include>` lines in master changelog; `SHOW TABLES` lists `TRADES`, `INSTRUMENTS`, `COUNTERPARTIES`, `SETTLEMENTS`, `RECON_BREAKS`, `AUDIT_LOG`.

**Acceptance criteria:**
- [ ] `./mvnw spring-boot:run` boots without errors.
- [ ] Log shows each changeset: `ChangeSet ... ran successfully`.
- [ ] Open `/h2-console` (JDBC URL `jdbc:h2:mem:tradeflow`, user `sa`) —
  all 5 tables + `audit_log` + 2 views exist with the right columns and constraints.
- [ ] `DATABASECHANGELOG` table lists each of your changesets with
  `EXECTYPE=EXECUTED`.

**Hints:**
- If a changeset fails, fix it in place and `./mvnw clean spring-boot:run`
  (the H2 DB is in-memory, so a full restart re-applies cleanly).
- For Postgres / UAT profile, `liquibase rollback` is the right way to undo.

---

<details>
<summary>Step-by-step walkthrough + 90% solution (click to expand)</summary>

### Steps

1. **Add `liquibase-core`** to `backend/pom.xml` (Boot's auto-config picks it up — no need for `spring-boot-starter-liquibase`).
2. **Point Spring at the master changelog** in `application.yml`.
3. **Put the changelog on the classpath** —
   `backend/src/main/resources/db/changelog/db.changelog-master.xml`.
4. **Boot the app** with `./mvnw spring-boot:run`.
5. **Scan the startup log** for the Liquibase banner — every changeset should
   say `EXECUTED`.
6. **Verify in the DB** that the tables exist.

### 90% solution

**`backend/pom.xml`** — add inside `<dependencies>`:

```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

> **Don't add `spring-boot-starter-liquibase`.** Boot 3.x activates Liquibase
> auto-config from `liquibase-core` alone. The starter is redundant and our
> reference pom doesn't use it — see `day02-liquibase.md` §2 for the full
> explanation.

**`backend/src/main/resources/application.yml`** — add:

```yaml
spring:
  liquibase:
    enabled: true
    # NO `classpath:` prefix — Liquibase 4.24+ (pulled in by Boot 3.2.4)
    # treats `classpath:` as part of the filename and 404s.
    change-log: db/changelog/db.changelog-master.xml
  jpa:
    hibernate:
      ddl-auto: none   # Liquibase owns the schema, NOT Hibernate
```

**Master changelog** — `backend/src/main/resources/db/changelog/db.changelog-master.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <include file="db/changelog/changes/001-init.xml"/>
    <include file="db/changelog/changes/002-schema.xml"/>
    <include file="db/changelog/changes/003-audit-log.xml"/>

</databaseChangeLog>
```

**Verify before moving on:**
- Every Day-1 changelog is listed in `<include>` order above.
- `ddl-auto: none` is set in `application.yml` (else Hibernate races Liquibase and you lose).

### Verify

Startup log should contain:

```
Liquibase Community ... started at ...
Reading from databasechangelog
Successfully acquired change log lock
Running Changeset: db/changelog/changes/001-init.xml::001::trainer
...
Successfully released change log lock
```

If you see `FileNotFoundException: classpath:db/changelog/...` — you wrote
`change-log: classpath:db/...`. Remove the `classpath:` prefix and retry.

</details>

---

## Run and Observe — End of Sprint 2 (Liquibase wiring)

The additional-topic lab wired `liquibase-core` into `pom.xml`, pointed
`spring.liquibase.change-log` at your master changelog, and (optionally)
booted Spring for the first time. This is a dry-run for Day 4 — if it
works today, Day 4's bootstrap is half done.

**Run:**

> **Terminal** — from `tradeflow-studentscopy/backend/`

```bash
./mvnw spring-boot:run
```

Watch the boot log for the Liquibase banner. Every Day-1 changeset you
listed in `db.changelog-master.xml` should print a `Running Changeset:`
line and finish with `Successfully released change log lock`.

**Observe:**

| Check | Expected after Sprint 2 |
|---|---|
| `./mvnw compile` | `BUILD SUCCESS` — `liquibase-core` resolves from Maven Central |
| Boot log line `Liquibase Community ... started at ...` | Present once, near the top of the Liquibase block |
| Boot log line `Running Changeset: db/changelog/changes/...` | One per `<include>` in your master changelog (no `classpath:` prefix in the path) |
| H2 console (`http://localhost:8080/h2-console`, JDBC `jdbc:h2:mem:tradeflow`) — `SHOW TABLES;` | All Day-1 business tables + `DATABASECHANGELOG`, `DATABASECHANGELOGLOCK` |
| `application.yml` setting `spring.jpa.hibernate.ddl-auto` | `none` — Liquibase owns the schema, not Hibernate |

**If something looks wrong:**

- `FileNotFoundException: classpath:db/changelog/...` — you wrote
  `change-log: classpath:db/...` in `application.yml`. Remove the
  `classpath:` prefix (Liquibase 4.24+ treats it as part of the filename).
- `Table already exists` — Hibernate raced Liquibase. Confirm
  `spring.jpa.hibernate.ddl-auto: none` is set.
- Boot starts but no Liquibase log lines — `liquibase-core` isn't on the
  classpath, or `spring.liquibase.enabled` is `false`. Re-read
  [`day02-liquibase.md`](./day02-liquibase.md) §2.

---

## End-of-day checklist

- [ ] All **14 tickets** merged (`I016`–`I027` Java OOP + `I009`, `I010` Liquibase).
- [ ] `./mvnw compile` clean — no warnings.
- [ ] `TradeflowApplication.main` runs and prints the trade table.
- [ ] **`I009`/`I010`:** master changelog includes every Day-1 changeset
  in dependency order; `./mvnw spring-boot:run` shows every changeset
  `EXECUTED` in the log; `DATABASECHANGELOG` table populated in H2.

Next: [Day 3 — OOP patterns + SOLID](../day3/README.md)
