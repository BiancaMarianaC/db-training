# Day 3 — OOP Patterns, Inheritance & Business Logic

> Theme: **Inheritance, SOLID, custom exceptions, file I/O.**
> Tickets: **I028 – I040** (13 tickets)
> Module: Java — Modules 3 & 4

By end of day:

- Asset-class-specific Trade subtypes (`EquityTrade`, `FXTrade`, `BondTrade`).
- A `ReconciliationService` that matches internal vs external trades.
- Custom exceptions for clean error semantics.
- CSV export of a recon report.
- Code structured around SOLID principles.

---

## Sprint 2 — OOP Patterns & Business Logic

### TICKET-I028 — `BaseTrade` (abstract)

**What**
- New `model/BaseTrade.java`: abstract class with 8 `protected final` fields, `requireNonNull` validation in the constructor, abstract `assetClassDescription()`, and `equals`/`hashCode` keyed on `tradeRef`.

**Why**
- Centralising shared field validation in one constructor is the inheritance lesson — Day 4's JPA `@Inheritance` strategy choice (TABLE_PER_CLASS vs JOINED) depends directly on this hierarchy, and Day 5's REST DTOs serialise the subclass shape.

**Observe**
- `new BaseTrade(...)` in scratch code is refused by the compiler ("`BaseTrade` is abstract; cannot be instantiated"); `./mvnw -q compile` is `BUILD SUCCESS` after `model/BaseTrade.java` is in place.

**Acceptance criteria:**
- [ ] `BaseTrade` holds: `tradeRef, instrumentId, counterpartyId, quantity, price, tradeDate, status, createdAt`.
- [ ] Abstract method `String assetClassDescription()`.
- [ ] Cannot be instantiated (`abstract` keyword).
- [ ] Existing `Trade` either *extends* `BaseTrade` (rename to `GenericTrade`)
  or is replaced entirely — discuss with your team and write it up in the PR.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- This is the parsing/domain model used by `TradeParser` and `ReconciliationService` — it is *not* the JPA entity (`Trade.java` keeps that role).
- Make every field `protected final` and validate them in the constructor — once parsed, a trade never mutates.
- *Effective Java* Item 18 ("favor composition over inheritance") is the right reading before you start. Inheritance here is justified because all asset classes really do share the same 8 fields.
- Natural-key equality on `tradeRef` is what lets the recon matcher use `Map<String, BaseTrade>` cleanly.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Layout for the class:

1. `package com.dbtraining.tradeflow.model;` — sits next to `Trade`.
2. `public abstract class BaseTrade` — the `abstract` keyword forbids `new BaseTrade(...)`.
3. 8 `protected final` fields: `tradeRef, instrumentId, counterpartyId, quantity, price, tradeDate, status, createdAt`.
4. `protected` constructor (subclasses call it via `super(...)`) that:
   - `Objects.requireNonNull` each required field;
   - defaults `status` to `TradeStatus.PENDING` if null;
   - defaults `createdAt` to `Instant.now()` if null;
   - rejects `quantity <= 0` and `price < 0`.
5. Read-only getters.
6. `getNotional()` returns `quantity.multiply(price)` — handy for tests.
7. `abstract String assetClassDescription()` — subclasses must implement.
8. `equals` / `hashCode` on `tradeRef` only.
9. `toString` includes `assetClassDescription()` so subclass info appears in logs.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public abstract class BaseTrade {

    protected final String tradeRef;
    protected final Long instrumentId;
    protected final Long counterpartyId;
    protected final BigDecimal quantity;
    protected final BigDecimal price;
    protected final LocalDate tradeDate;
    protected final TradeStatus status;
    protected final Instant createdAt;

    protected BaseTrade(/* TODO 8 params */) {
        // TODO requireNonNull each required field
        // TODO default status to PENDING, createdAt to Instant.now()
        // TODO sanity check quantity > 0 and price >= 0
    }

    // TODO getters

    public BigDecimal getNotional() { return quantity.multiply(price); }

    public abstract String assetClassDescription();

    // TODO equals/hashCode on tradeRef
    // TODO toString that prints assetClassDescription()
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/model/BaseTrade.java`

```java
package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public abstract class BaseTrade {

    protected final String tradeRef;
    protected final Long instrumentId;
    protected final Long counterpartyId;
    protected final BigDecimal quantity;
    protected final BigDecimal price;
    protected final LocalDate tradeDate;
    protected final TradeStatus status;
    protected final Instant createdAt;

    protected BaseTrade(String tradeRef, Long instrumentId, Long counterpartyId,
                        BigDecimal quantity, BigDecimal price, LocalDate tradeDate,
                        TradeStatus status, Instant createdAt) {
        this.tradeRef       = Objects.requireNonNull(tradeRef,       "tradeRef required");
        this.instrumentId   = Objects.requireNonNull(instrumentId,   "instrumentId required");
        this.counterpartyId = Objects.requireNonNull(counterpartyId, "counterpartyId required");
        this.quantity       = Objects.requireNonNull(quantity,       "quantity required");
        this.price          = Objects.requireNonNull(price,          "price required");
        this.tradeDate      = Objects.requireNonNull(tradeDate,      "tradeDate required");
        this.status         = status != null ? status : TradeStatus.PENDING;
        this.createdAt      = createdAt != null ? createdAt : Instant.now();
        if (quantity.signum() <= 0) throw new IllegalStateException("quantity must be > 0");
        if (price.signum() < 0)     throw new IllegalStateException("price must be >= 0");
    }

    public String getTradeRef()       { return tradeRef; }
    public Long getInstrumentId()     { return instrumentId; }
    public Long getCounterpartyId()   { return counterpartyId; }
    public BigDecimal getQuantity()   { return quantity; }
    public BigDecimal getPrice()      { return price; }
    public LocalDate getTradeDate()   { return tradeDate; }
    public TradeStatus getStatus()    { return status; }
    public Instant getCreatedAt()     { return createdAt; }

    public BigDecimal getNotional() { return quantity.multiply(price); }

    public abstract String assetClassDescription();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseTrade other)) return false;
        return Objects.equals(tradeRef, other.tradeRef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tradeRef);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "[" + tradeRef
                + " | " + assetClassDescription()
                + " | " + quantity + " @ " + price
                + " | " + tradeDate
                + " | " + status + "]";
    }
}
```
</details>

**Files to touch:** `model/BaseTrade.java`, refactor `Trade.java`.

---

### TICKET-I029 — `EquityTrade`

**What**
- `model/EquityTrade.java` extends `BaseTrade` with `exchange` (String) and `lotSize` (int), plus a static nested `Builder` so callers don't deal with a 10-arg constructor.

**Why**
- This is the first concrete subclass — proves the `super(...)` chain wiring works before I030 and I031 repeat the pattern. The `Builder` style is the same shape Day 5 will reuse for REST request DTOs.

**Observe**
- `EquityTrade.builder()...exchange("XETR").lotSize(100).build().assetClassDescription()` returns the literal string `"Equity on XETR"`; building with `lotSize <= 0` throws `IllegalStateException("lotSize must be > 0")`.

**Acceptance criteria:**
- [ ] Extends `BaseTrade`.
- [ ] Extra fields: `exchange` (String), `lotSize` (int).
- [ ] `assetClassDescription()` returns `"Equity on " + exchange`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- `EquityTrade` extends `BaseTrade` — pass the 8 base fields up via `super(...)`.
- Validate the equity-specific fields in the constructor (`exchange` non-null, `lotSize > 0`).
- Use a static `Builder` so callers don't deal with a 10-argument constructor — the recon pipeline will create lots of these in tests.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Layout:

1. `public class EquityTrade extends BaseTrade` — concrete, not abstract.
2. Two `private final` fields: `String exchange`, `int lotSize`.
3. `private EquityTrade(Builder b)` — single private constructor reading from the builder, calls `super(b.tradeRef, b.instrumentId, ...)`.
4. Validate `exchange` non-null and `lotSize > 0` in the constructor.
5. Public `static Builder builder()` factory.
6. Override `assetClassDescription()` → `"Equity on " + exchange`.
7. Nested `public static final class Builder` with one fluent setter per field plus a `build()`.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class EquityTrade extends BaseTrade {

    private final String exchange;
    private final int lotSize;

    private EquityTrade(Builder b) {
        super(b.tradeRef, b.instrumentId, b.counterpartyId, b.quantity, b.price,
              b.tradeDate, b.status, b.createdAt);
        // TODO requireNonNull(exchange)
        // TODO check lotSize > 0
        this.exchange = b.exchange;
        this.lotSize  = b.lotSize;
    }

    public static Builder builder() { return new Builder(); }

    // TODO getters

    @Override
    public String assetClassDescription() { return "Equity on " + exchange; }

    public static final class Builder {
        // TODO fields mirroring constructor params
        // TODO fluent setters: tradeRef(...), instrumentId(...), ... exchange(...), lotSize(...)
        public EquityTrade build() { return new EquityTrade(this); }
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/model/EquityTrade.java`

```java
package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class EquityTrade extends BaseTrade {

    private final String exchange;
    private final int lotSize;

    private EquityTrade(Builder b) {
        super(b.tradeRef, b.instrumentId, b.counterpartyId, b.quantity, b.price,
              b.tradeDate, b.status, b.createdAt);
        this.exchange = Objects.requireNonNull(b.exchange, "exchange required");
        if (b.lotSize <= 0) throw new IllegalStateException("lotSize must be > 0");
        this.lotSize = b.lotSize;
    }

    public static Builder builder() { return new Builder(); }

    public String getExchange() { return exchange; }
    public int getLotSize()     { return lotSize; }

    @Override
    public String assetClassDescription() { return "Equity on " + exchange; }

    public static final class Builder {
        private String tradeRef;
        private Long instrumentId;
        private Long counterpartyId;
        private BigDecimal quantity;
        private BigDecimal price;
        private LocalDate tradeDate;
        private TradeStatus status;
        private Instant createdAt;
        private String exchange;
        private int lotSize;

        public Builder tradeRef(String v)         { this.tradeRef = v;       return this; }
        public Builder instrumentId(Long v)        { this.instrumentId = v;   return this; }
        public Builder counterpartyId(Long v)      { this.counterpartyId = v; return this; }
        public Builder quantity(BigDecimal v)      { this.quantity = v;       return this; }
        public Builder price(BigDecimal v)         { this.price = v;          return this; }
        public Builder tradeDate(LocalDate v)      { this.tradeDate = v;      return this; }
        public Builder status(TradeStatus v)       { this.status = v;         return this; }
        public Builder createdAt(Instant v)        { this.createdAt = v;      return this; }
        public Builder exchange(String v)          { this.exchange = v;       return this; }
        public Builder lotSize(int v)              { this.lotSize = v;        return this; }

        public EquityTrade build() { return new EquityTrade(this); }
    }
}
```
</details>

**Files to touch:** `model/EquityTrade.java`.

---

### TICKET-I030 — `FXTrade`

**What**
- `model/FXTrade.java` extends `BaseTrade` with `baseCurrency`, `quoteCurrency`, `spotRate`; constructor enforces ISO-4217 3-letter codes, `base != quote`, and `spotRate > 0`.

**Why**
- FX-specific validation lives on the FX subclass — not in `BaseTrade` and not in a Day-5 controller. Day 4's JPA `@Inheritance(SINGLE_TABLE)` choice has to model this column shape; getting the validation tight today prevents NPEs in Day 7 risk calcs that multiply by `spotRate`.

**Observe**
- `.baseCurrency("EUR").quoteCurrency("USD")...build().assetClassDescription()` returns `"EUR/USD"`; building with `.baseCurrency("USD").quoteCurrency("USD")` throws `IllegalStateException("base and quote currencies must differ")`.

**Acceptance criteria:**
- [ ] Extends `BaseTrade`.
- [ ] Extra fields: `baseCurrency`, `quoteCurrency`, `spotRate` (BigDecimal).
- [ ] `assetClassDescription()` returns the currency pair string (e.g. `"EUR/USD"`).

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- Same builder pattern as `EquityTrade`. Mirror the structure.
- FX has stricter validation: ISO-4217 currency codes are exactly 3 letters, base and quote must differ, `spotRate > 0`.
- `assetClassDescription()` is simply `baseCurrency + "/" + quoteCurrency`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Layout:

1. `public class FXTrade extends BaseTrade` with three new `final` fields: `baseCurrency`, `quoteCurrency` (Strings), `spotRate` (BigDecimal).
2. Private constructor takes a `Builder b`, calls `super(...)`, then runs FX-specific validation:
   - `requireNonNull` all three.
   - Both currencies must be length 3.
   - `baseCurrency.equals(quoteCurrency)` is illegal (no `USD/USD`).
   - `spotRate.signum() > 0`.
3. `assetClassDescription()` → `baseCurrency + "/" + quoteCurrency`.
4. Same nested fluent `Builder` with one setter per field.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class FXTrade extends BaseTrade {

    private final String baseCurrency;
    private final String quoteCurrency;
    private final BigDecimal spotRate;

    private FXTrade(Builder b) {
        super(b.tradeRef, b.instrumentId, b.counterpartyId, b.quantity, b.price,
              b.tradeDate, b.status, b.createdAt);
        // TODO requireNonNull(baseCurrency), requireNonNull(quoteCurrency), requireNonNull(spotRate)
        // TODO check both currencies are length 3
        // TODO check base != quote
        // TODO check spotRate > 0
        this.baseCurrency  = b.baseCurrency;
        this.quoteCurrency = b.quoteCurrency;
        this.spotRate      = b.spotRate;
    }

    public static Builder builder() { return new Builder(); }

    // TODO getters

    @Override
    public String assetClassDescription() { /* TODO */ return null; }

    public static final class Builder {
        // TODO fields + fluent setters mirroring BaseTrade + FX-specific
        public FXTrade build() { return new FXTrade(this); }
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/model/FXTrade.java`

```java
package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class FXTrade extends BaseTrade {

    private final String baseCurrency;
    private final String quoteCurrency;
    private final BigDecimal spotRate;

    private FXTrade(Builder b) {
        super(b.tradeRef, b.instrumentId, b.counterpartyId, b.quantity, b.price,
              b.tradeDate, b.status, b.createdAt);
        this.baseCurrency  = Objects.requireNonNull(b.baseCurrency,  "baseCurrency required");
        this.quoteCurrency = Objects.requireNonNull(b.quoteCurrency, "quoteCurrency required");
        this.spotRate      = Objects.requireNonNull(b.spotRate,      "spotRate required");
        if (baseCurrency.length() != 3 || quoteCurrency.length() != 3)
            throw new IllegalStateException("currencies must be ISO-4217 3-letter codes");
        if (baseCurrency.equals(quoteCurrency))
            throw new IllegalStateException("base and quote currencies must differ");
        if (spotRate.signum() <= 0)
            throw new IllegalStateException("spotRate must be > 0");
    }

    public static Builder builder() { return new Builder(); }

    public String getBaseCurrency()  { return baseCurrency; }
    public String getQuoteCurrency() { return quoteCurrency; }
    public BigDecimal getSpotRate()  { return spotRate; }

    @Override
    public String assetClassDescription() { return baseCurrency + "/" + quoteCurrency; }

    public static final class Builder {
        private String tradeRef;
        private Long instrumentId;
        private Long counterpartyId;
        private BigDecimal quantity;
        private BigDecimal price;
        private LocalDate tradeDate;
        private TradeStatus status;
        private Instant createdAt;
        private String baseCurrency;
        private String quoteCurrency;
        private BigDecimal spotRate;

        public Builder tradeRef(String v)         { this.tradeRef = v;       return this; }
        public Builder instrumentId(Long v)        { this.instrumentId = v;   return this; }
        public Builder counterpartyId(Long v)      { this.counterpartyId = v; return this; }
        public Builder quantity(BigDecimal v)      { this.quantity = v;       return this; }
        public Builder price(BigDecimal v)         { this.price = v;          return this; }
        public Builder tradeDate(LocalDate v)      { this.tradeDate = v;      return this; }
        public Builder status(TradeStatus v)       { this.status = v;         return this; }
        public Builder createdAt(Instant v)        { this.createdAt = v;      return this; }
        public Builder baseCurrency(String v)      { this.baseCurrency = v;   return this; }
        public Builder quoteCurrency(String v)     { this.quoteCurrency = v;  return this; }
        public Builder spotRate(BigDecimal v)      { this.spotRate = v;       return this; }

        public FXTrade build() { return new FXTrade(this); }
    }
}
```
</details>

**Files to touch:** `model/FXTrade.java`.

---

### TICKET-I031 — `BondTrade`

**What**
- `model/BondTrade.java` extends `BaseTrade` with `couponRate` (BigDecimal, 0–100), `maturityDate` (LocalDate strictly after `tradeDate`), and `faceValue` (BigDecimal > 0).

**Why**
- Closes the asset-class hierarchy and forces students to access `tradeDate` from the inherited `protected` field — the moment where `protected` vs `private` clicks. The bond's `maturityDate` rule is the first temporal validation, which Day 8's date-bucketing report relies on.

**Observe**
- Building with `tradeDate=2026-03-01, maturityDate=2026-03-01` throws `IllegalStateException("maturityDate must be after tradeDate")`; bumping `maturityDate` to `2031-03-01` builds cleanly and `assetClassDescription()` returns `"Bond coupon 5.0% mat 2031-03-01"`.

**Acceptance criteria:**
- [ ] Extends `BaseTrade`.
- [ ] Extra fields: `couponRate` (BigDecimal), `maturityDate` (LocalDate), `faceValue` (BigDecimal).
- [ ] Validates: `couponRate` between 0 and 100; `maturityDate` after `tradeDate`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- Same shape as `EquityTrade` / `FXTrade` — builder + `super(...)`.
- `couponRate` is a percentage stored as a `BigDecimal` (e.g. `5.25` means 5.25%). Range check: `>= 0 && <= 100`.
- `maturityDate.isAfter(tradeDate)` (not `equals`, not `before` — strict greater than).
- `faceValue > 0`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Layout:

1. `public class BondTrade extends BaseTrade` with three new `final` fields.
2. A `private static final BigDecimal HUNDRED = new BigDecimal("100")` constant — avoid `new BigDecimal("100")` on every validate call.
3. Private constructor reads from Builder, calls `super(...)`, then:
   - `requireNonNull` all three.
   - `couponRate` in `[0, 100]` (use `signum() < 0` and `compareTo(HUNDRED) > 0`).
   - `maturityDate.isAfter(tradeDate)` — note `tradeDate` is already a field on `BaseTrade`, so access it as `tradeDate` (it's `protected`).
   - `faceValue.signum() > 0`.
4. `assetClassDescription()` → `"Bond coupon " + couponRate + "% mat " + maturityDate`.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class BondTrade extends BaseTrade {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BigDecimal couponRate;
    private final LocalDate maturityDate;
    private final BigDecimal faceValue;

    private BondTrade(Builder b) {
        super(b.tradeRef, b.instrumentId, b.counterpartyId, b.quantity, b.price,
              b.tradeDate, b.status, b.createdAt);
        // TODO requireNonNull all three
        // TODO range-check couponRate
        // TODO maturityDate strictly after tradeDate
        // TODO faceValue > 0
        this.couponRate   = b.couponRate;
        this.maturityDate = b.maturityDate;
        this.faceValue    = b.faceValue;
    }

    public static Builder builder() { return new Builder(); }

    // TODO getters

    @Override
    public String assetClassDescription() { /* TODO */ return null; }

    public static final class Builder {
        // TODO mirror EquityTrade.Builder, swap last three setters for bond fields
        public BondTrade build() { return new BondTrade(this); }
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/model/BondTrade.java`

```java
package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class BondTrade extends BaseTrade {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final BigDecimal couponRate;
    private final LocalDate maturityDate;
    private final BigDecimal faceValue;

    private BondTrade(Builder b) {
        super(b.tradeRef, b.instrumentId, b.counterpartyId, b.quantity, b.price,
              b.tradeDate, b.status, b.createdAt);
        this.couponRate   = Objects.requireNonNull(b.couponRate,   "couponRate required");
        this.maturityDate = Objects.requireNonNull(b.maturityDate, "maturityDate required");
        this.faceValue    = Objects.requireNonNull(b.faceValue,    "faceValue required");
        if (couponRate.signum() < 0 || couponRate.compareTo(HUNDRED) > 0)
            throw new IllegalStateException("couponRate must be between 0 and 100");
        if (!maturityDate.isAfter(tradeDate))
            throw new IllegalStateException("maturityDate must be after tradeDate");
        if (faceValue.signum() <= 0)
            throw new IllegalStateException("faceValue must be > 0");
    }

    public static Builder builder() { return new Builder(); }

    public BigDecimal getCouponRate()    { return couponRate; }
    public LocalDate getMaturityDate()   { return maturityDate; }
    public BigDecimal getFaceValue()     { return faceValue; }

    @Override
    public String assetClassDescription() {
        return "Bond coupon " + couponRate + "% mat " + maturityDate;
    }

    public static final class Builder {
        private String tradeRef;
        private Long instrumentId;
        private Long counterpartyId;
        private BigDecimal quantity;
        private BigDecimal price;
        private LocalDate tradeDate;
        private TradeStatus status;
        private Instant createdAt;
        private BigDecimal couponRate;
        private LocalDate maturityDate;
        private BigDecimal faceValue;

        public Builder tradeRef(String v)         { this.tradeRef = v;       return this; }
        public Builder instrumentId(Long v)        { this.instrumentId = v;   return this; }
        public Builder counterpartyId(Long v)      { this.counterpartyId = v; return this; }
        public Builder quantity(BigDecimal v)      { this.quantity = v;       return this; }
        public Builder price(BigDecimal v)         { this.price = v;          return this; }
        public Builder tradeDate(LocalDate v)      { this.tradeDate = v;      return this; }
        public Builder status(TradeStatus v)       { this.status = v;         return this; }
        public Builder createdAt(Instant v)        { this.createdAt = v;      return this; }
        public Builder couponRate(BigDecimal v)    { this.couponRate = v;     return this; }
        public Builder maturityDate(LocalDate v)   { this.maturityDate = v;   return this; }
        public Builder faceValue(BigDecimal v)     { this.faceValue = v;      return this; }

        public BondTrade build() { return new BondTrade(this); }
    }
}
```
</details>

**Files to touch:** `model/BondTrade.java`.

---

### TICKET-I032 — `TradeValidationException`

**What**
- `exception/TradeValidationException.java` extending `Exception` (checked) with inner `enum Code { MISSING_FIELD, INVALID_VALUE, REFERENCE_NOT_FOUND }`, primary `(Code, message)` constructor, and a `getCode()` accessor.

**Why**
- Checked = recoverable: Day 6's REST controller turns each `Code` into a structured HTTP 400 error body, and Day 8's monitoring lab counts breaks by `Code`. The enum is what makes that machine-readable.

**Observe**
- `throw new TradeValidationException(Code.MISSING_FIELD, "x")` inside a method without `throws TradeValidationException` is refused by the compiler with `unreported exception TradeValidationException; must be caught or declared to be thrown`.

**Acceptance criteria:**
- [ ] Extends `Exception` (checked, not Runtime).
- [ ] Has an inner `enum Code { MISSING_FIELD, INVALID_VALUE, REFERENCE_NOT_FOUND }`.
- [ ] Constructor takes (`Code`, `message`).

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- Checked because validation failures are recoverable — callers (the REST controller on Day 6) will turn them into HTTP 400 responses with structured error bodies.
- `Code` is an *inner* enum, declared inside the exception class. The full name is `TradeValidationException.Code.MISSING_FIELD`.
- The `Code` is stored as an immutable `private final` field; expose it via `getCode()`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Structure:

1. `package com.dbtraining.tradeflow.exception;`
2. `public class TradeValidationException extends Exception` — **not** `RuntimeException`.
3. Public inner `enum Code { MISSING_FIELD, INVALID_VALUE, REFERENCE_NOT_FOUND }`.
4. `private final Code code` field.
5. Primary constructor: `public TradeValidationException(Code code, String message)` → `super(message); this.code = code;`.
6. Optional convenience constructor for legacy callers: `public TradeValidationException(String message)` defaulting `code` to `Code.INVALID_VALUE`.
7. `public Code getCode() { return code; }`.
</details>

<details>
<summary>Hint 3 — Exception skeleton</summary>

```java
package com.dbtraining.tradeflow.exception;

public class TradeValidationException extends Exception {

    public enum Code {
        // TODO three constants
    }

    private final Code code;

    public TradeValidationException(Code code, String message) {
        super(message);
        this.code = code;
    }

    // TODO optional single-arg constructor defaulting to INVALID_VALUE

    // TODO getCode()
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/exception/TradeValidationException.java`

```java
package com.dbtraining.tradeflow.exception;

public class TradeValidationException extends Exception {

    public enum Code {
        MISSING_FIELD,
        INVALID_VALUE,
        REFERENCE_NOT_FOUND
    }

    private final Code code;

    public TradeValidationException(Code code, String message) {
        super(message);
        this.code = code;
    }

    /** Legacy single-arg constructor — defaults to INVALID_VALUE. Prefer (Code, message). */
    public TradeValidationException(String message) {
        super(message);
        this.code = Code.INVALID_VALUE;
    }

    public TradeValidationException(Code code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public Code getCode() {
        return code;
    }
}
```
</details>

**Files to touch:** `exception/TradeValidationException.java`.

---

### TICKET-I033 — `InsufficientDataException`

**What**
- `exception/InsufficientDataException.java` extending `RuntimeException` with `(String message)` and `(String message, Throwable cause)` constructors — no extra state.

**Why**
- The unchecked counterpart to I032: programmer-error and unrecoverable parser failures (CSV row blew up on `NumberFormatException`) shouldn't pollute every method signature with `throws`. The cause-carrying ctor preserves the original `NumberFormatException` stack trace I039's `TradeParser.parseRow` wraps.

**Observe**
- `throw new InsufficientDataException("x")` inside a method *without* `throws` compiles cleanly; running it shows the wrapped cause in the stack trace as `Caused by: java.lang.NumberFormatException:` when the two-arg constructor is used.

**Acceptance criteria:**
- [ ] Runtime exception (extends `RuntimeException`).
- [ ] Thrown when a required trade field is `null` and the calling code can't recover.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- Unchecked because it signals "the caller passed garbage and there is no recovery" — same category as `IllegalArgumentException`.
- Pair this with the checked `TradeValidationException` (I032): checked = recoverable, unchecked = programmer error. The two together is the recoverable/unrecoverable distinction *Effective Java* Item 70 talks about.
- Add both `(message)` and `(message, cause)` constructors so you can wrap parse errors with their original `NumberFormatException`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Structure:

1. `package com.dbtraining.tradeflow.exception;`
2. `public class InsufficientDataException extends RuntimeException`.
3. Two constructors, both delegate to `super(...)`:
   - `(String message)`
   - `(String message, Throwable cause)` — used by `TradeParser` to wrap `NumberFormatException`.
4. No new state — just constructors.
</details>

<details>
<summary>Hint 3 — Exception skeleton</summary>

```java
package com.dbtraining.tradeflow.exception;

public class InsufficientDataException extends RuntimeException {

    public InsufficientDataException(String message) {
        super(message);
    }

    // TODO second constructor that also takes a Throwable cause
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/exception/InsufficientDataException.java`

```java
package com.dbtraining.tradeflow.exception;

public class InsufficientDataException extends RuntimeException {

    public InsufficientDataException(String message) {
        super(message);
    }

    public InsufficientDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
```
</details>

**Files to touch:** `exception/InsufficientDataException.java`.

---

### TICKET-I034 — `ReconciliationService.matchTrades`

**What**
- `service/ReconciliationService.matchTrades(List<BaseTrade> internal, List<BaseTrade> external)` indexes `external` by `tradeRef` in a `HashMap`, iterates `internal` with `remove(...)`, then sweeps the map for leftover externals — returns a `ReconReport`.

**Why**
- This is the single most-important method in the case study. The `O(n + m)` map-and-remove pattern (not nested loops) is what makes the Day 9 monitoring lab's 100k-trade benchmark finish in seconds instead of minutes; the leftover-sweep is what makes `MISSING_TRADE` symmetric.

**Observe**
- Feeding `matchTrades(List.of(eq1), List.of(eq1))` returns a `ReconReport` with `matched.size() == 1` and `discrepancies.size() == 0`; passing `null` for either list throws `NullPointerException("internal list required")` or `("external list required")`.

**Signature:**
```java
public ReconReport matchTrades(List<BaseTrade> internal, List<BaseTrade> external)
```

**Acceptance criteria:**
- [ ] Trades are matched on `tradeRef`.
- [ ] Returns a `ReconReport` with `matched`, `unmatched`, and `discrepancies` lists.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- Build a `Map<String, BaseTrade> externalByRef` once, then iterate the internal list — `O(n + m)` not `O(n * m)`.
- Use `Map.remove(key)` while iterating: any external trade still in the map at the end is `MISSING_TRADE` on the internal side.
- `ReconReport` carries `(totalInternal, totalExternal, matched, discrepancies)` — wire the input list sizes through so `generateReport` (I036) doesn't have to re-count.
- Defer the per-field classification to a private helper (`classify(in, out)`) — that's I035, you build the skeleton here.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Algorithm:

1. Null-guard both input lists with `Objects.requireNonNull`.
2. Build `externalByRef = external.stream().collect(Collectors.toMap(BaseTrade::getTradeRef, t -> t, (a, b) -> a))` — the third arg silently keeps the first on duplicate refs (a single tradeRef showing up twice in the external feed is itself a data quality issue, but not what this method is for).
3. Walk the internal list:
   - `out = externalByRef.remove(in.getTradeRef())` — pull AND remove.
   - If `out` is null → add a `MISSING_TRADE` discrepancy and continue.
   - Otherwise call `classify(in, out)` (I035). Empty list → matched; non-empty → discrepancy.
4. After the loop, every key still in `externalByRef` is an external-only trade — emit `MISSING_TRADE` for each.
5. Return `new ReconReport(internal.size(), external.size(), matched, discrepancies)`.

The `classify` method body itself is TICKET-I035 — leave it returning `List.of()` for now.
</details>

<details>
<summary>Hint 3 — Method skeleton</summary>

```java
public ReconReport matchTrades(List<BaseTrade> internal, List<BaseTrade> external) {
    Objects.requireNonNull(internal, "internal list required");
    Objects.requireNonNull(external, "external list required");

    // TODO Map<String, BaseTrade> externalByRef via stream.collect(toMap(...))

    List<BaseTrade> matched = new ArrayList<>();
    List<Discrepancy> discrepancies = new ArrayList<>();

    for (BaseTrade in : internal) {
        // TODO BaseTrade out = externalByRef.remove(in.getTradeRef());
        // TODO if out == null, add MISSING_TRADE discrepancy and continue
        // TODO List<DiscrepancyType> diffs = classify(in, out);
        // TODO empty diffs -> matched.add(in); else discrepancies.add(new Discrepancy(...))
    }

    // TODO for each remaining BaseTrade in externalByRef.values():
    //      add a MISSING_TRADE discrepancy

    return new ReconReport(internal.size(), external.size(), matched, discrepancies);
}

private List<DiscrepancyType> classify(BaseTrade in, BaseTrade out) {
    // TICKET-I035 — leave returning List.of() for now.
    return List.of();
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/service/ReconciliationService.java`

```java
package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.Discrepancy;
import com.dbtraining.tradeflow.dto.ReconReport;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.DiscrepancyType;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReconciliationService {

    public ReconReport matchTrades(List<BaseTrade> internal, List<BaseTrade> external) {
        Objects.requireNonNull(internal, "internal list required");
        Objects.requireNonNull(external, "external list required");

        Map<String, BaseTrade> externalByRef = external.stream()
                .collect(Collectors.toMap(BaseTrade::getTradeRef, t -> t, (a, b) -> a));

        List<BaseTrade> matched = new ArrayList<>();
        List<Discrepancy> discrepancies = new ArrayList<>();

        for (BaseTrade in : internal) {
            BaseTrade out = externalByRef.remove(in.getTradeRef());
            if (out == null) {
                discrepancies.add(new Discrepancy(in.getTradeRef(), List.of(DiscrepancyType.MISSING_TRADE)));
                continue;
            }
            List<DiscrepancyType> diffs = classify(in, out);
            if (diffs.isEmpty()) {
                matched.add(in);
            } else {
                discrepancies.add(new Discrepancy(in.getTradeRef(), diffs));
            }
        }

        // Sweep leftovers: every external trade still in the map has no internal counterpart.
        for (BaseTrade leftover : externalByRef.values()) {
            discrepancies.add(new Discrepancy(leftover.getTradeRef(),
                    List.of(DiscrepancyType.MISSING_TRADE)));
        }

        return new ReconReport(internal.size(), external.size(), matched, discrepancies);
    }

    private List<DiscrepancyType> classify(BaseTrade in, BaseTrade out) {
        // TICKET-I035 fills this in.
        return List.of();
    }
}
```
</details>

**Files to touch:** `service/ReconciliationService.java`.

---

### TICKET-I035 — Matching logic + discrepancy classification

**What**
- Private `classify(BaseTrade in, BaseTrade out)` inside `ReconciliationService`: returns a `List<DiscrepancyType>` accumulating `PRICE_MISMATCH`, `QUANTITY_MISMATCH`, `DATE_MISMATCH` using `compareTo` for `BigDecimal` and `Objects.equals` for `LocalDate`.

**Why**
- This is where the `BigDecimal.equals` vs `compareTo` war story lands: `1.0` does not equal `1.00` under `.equals()` because the scales differ. Getting it wrong silently misses real breaks; the Day 9 monitoring dashboard will surface those as "zero discrepancies" days that hide real problems.

**Observe**
- Two `EquityTrade`s with the same `tradeRef` but different `price` AND different `quantity` produce a returned diffs list containing exactly `[PRICE_MISMATCH, QUANTITY_MISMATCH]`; differing only on `tradeDate` produces exactly `[DATE_MISMATCH]`.

**Acceptance criteria:**
- [ ] Same `tradeRef` but different `quantity` → `QUANTITY_MISMATCH`.
- [ ] Different `price` → `PRICE_MISMATCH`.
- [ ] Different `tradeDate` → `DATE_MISMATCH`.
- [ ] Present internally but missing externally (or vice-versa) → `MISSING_TRADE`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- `BigDecimal.equals()` distinguishes `1.0` from `1.00` (different *scale*). Always compare amounts with `compareTo(other) == 0`.
- `LocalDate.equals()` is fine — there's no hidden scale.
- One trade may have several mismatches (price *and* quantity, say). Return a `List<DiscrepancyType>` so all of them surface in the report.
- `MISSING_TRADE` is *not* assigned here — that's handled by `matchTrades` (I034). `classify` only runs when both sides exist.
</details>

<details>
<summary>Hint 2 — More guided</summary>

This is a tiny method — three independent checks, push to a list, return it.

```
private List<DiscrepancyType> classify(BaseTrade in, BaseTrade out):
    diffs = new ArrayList<>(capacity hint: 2 or 3)
    if in.price.compareTo(out.price) != 0   -> diffs.add(PRICE_MISMATCH)
    if in.quantity.compareTo(out.quantity) != 0 -> diffs.add(QUANTITY_MISMATCH)
    if !Objects.equals(in.tradeDate, out.tradeDate) -> diffs.add(DATE_MISMATCH)
    return diffs
```

Use `Objects.equals` on `tradeDate` (defensive: dates are non-null in our data, but the helper is free insurance).
</details>

<details>
<summary>Hint 3 — Method skeleton</summary>

```java
private List<DiscrepancyType> classify(BaseTrade in, BaseTrade out) {
    List<DiscrepancyType> diffs = new ArrayList<>(2);
    // TODO add PRICE_MISMATCH when in.price differs from out.price (use compareTo)
    // TODO add QUANTITY_MISMATCH when in.quantity differs from out.quantity
    // TODO add DATE_MISMATCH when in.tradeDate differs from out.tradeDate
    return diffs;
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/service/ReconciliationService.java` — replace the stub `classify` body from I034.

```java
private List<DiscrepancyType> classify(BaseTrade in, BaseTrade out) {
    List<DiscrepancyType> diffs = new ArrayList<>(2);
    if (in.getPrice().compareTo(out.getPrice()) != 0)
        diffs.add(DiscrepancyType.PRICE_MISMATCH);
    if (in.getQuantity().compareTo(out.getQuantity()) != 0)
        diffs.add(DiscrepancyType.QUANTITY_MISMATCH);
    if (!Objects.equals(in.getTradeDate(), out.getTradeDate()))
        diffs.add(DiscrepancyType.DATE_MISMATCH);
    return diffs;
}
```
</details>

**Files to touch:** `service/ReconciliationService.java`.

---

### TICKET-I036 — `generateReport()` summary

**What**
- New `dto/ReconSummary.java` record + two methods on `ReconciliationService`: `generateReport(ReconReport)` returns a `ReconSummary` carrying `breakdownByType` as an `EnumMap` seeded with every `DiscrepancyType` set to `0`; `render(ReconSummary)` produces the console-friendly string.

**Why**
- The `EnumMap` seeded-with-zero pattern guarantees Day 6's REST API returns a predictable JSON shape (no missing keys when a break type happens to have zero count) — the React dashboard on Day 7 binds to all four keys.

**Observe**
- Feeding a `ReconReport` with 1 `PRICE_MISMATCH` and 1 `MISSING_TRADE` produces `breakdownByType == {PRICE_MISMATCH=1, QUANTITY_MISMATCH=0, DATE_MISMATCH=0, MISSING_TRADE=1}` — all four keys present even when their count is 0.

**Acceptance criteria:**
- [ ] `ReconSummary` has: `totalInternal, totalExternal, matchedCount,
  unmatchedCount, breakdownByType` (`Map<DiscrepancyType, Integer>`).
- [ ] `toString()` returns a tidy console-friendly report.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- `ReconSummary` is a `record` — 5 fields, no behaviour.
- Use `EnumMap<DiscrepancyType, Integer>` for the breakdown — faster than `HashMap` and gives a stable iteration order matching enum declaration.
- Initialise every enum constant to `0` so the report shows `MISSING_TRADE: 0` instead of omitting it — ops people read the missing line as "we don't track that yet" which is a bug.
- A nicely formatted `render(summary)` is more useful than `toString` for the day-3 console output (the Day-6 REST controller will return the JSON form of the record itself).
</details>

<details>
<summary>Hint 2 — More guided</summary>

Two artefacts:

1. **`dto/ReconSummary`** — Java record:
   ```
   record ReconSummary(int totalInternal, int totalExternal,
                      int matchedCount, int unmatchedCount,
                      Map<DiscrepancyType, Integer> breakdownByType) {}
   ```

2. **`ReconciliationService.generateReport(ReconReport)`**:
   - Null-guard.
   - `EnumMap<DiscrepancyType, Integer> breakdown` pre-populated with all four keys → 0.
   - Loop over `report.discrepancies()`, then over `d.types()`, and `breakdown.merge(t, 1, Integer::sum)`.
   - Build the `ReconSummary` with the sizes you already have on `ReconReport`, wrapping `breakdown` in `Collections.unmodifiableMap(...)`.

3. **`render(ReconSummary)`** — `StringBuilder`, header + 4 totals + a breakdown loop with `String.format("    - %-20s %d%n", type, count)`. This is the console-friendly output that satisfies the acceptance criterion.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
public ReconSummary generateReport(ReconReport report) {
    Objects.requireNonNull(report, "report required");

    Map<DiscrepancyType, Integer> breakdown = new EnumMap<>(DiscrepancyType.class);
    // TODO seed every DiscrepancyType value with 0
    // TODO for each Discrepancy d in report.discrepancies():
    //         for each DiscrepancyType t in d.types():
    //             breakdown.merge(t, 1, Integer::sum);

    return new ReconSummary(
            report.totalInternal(),
            report.totalExternal(),
            report.matched().size(),
            report.discrepancies().size(),
            Collections.unmodifiableMap(breakdown));
}

public String render(ReconSummary s) {
    StringBuilder sb = new StringBuilder();
    // TODO header line + 4 totals + breakdown loop
    return sb.toString();
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit:**
- `backend/src/main/java/com/dbtraining/tradeflow/dto/ReconSummary.java` (new)
- `backend/src/main/java/com/dbtraining/tradeflow/service/ReconciliationService.java` (add the two methods below)

**`ReconSummary.java`:**

```java
package com.dbtraining.tradeflow.dto;

import com.dbtraining.tradeflow.model.DiscrepancyType;
import java.util.Map;

public record ReconSummary(
        int totalInternal,
        int totalExternal,
        int matchedCount,
        int unmatchedCount,
        Map<DiscrepancyType, Integer> breakdownByType
) {}
```

**Service methods to add to `ReconciliationService`:**

```java
public ReconSummary generateReport(ReconReport report) {
    Objects.requireNonNull(report, "report required");

    Map<DiscrepancyType, Integer> breakdown = new EnumMap<>(DiscrepancyType.class);
    for (DiscrepancyType t : DiscrepancyType.values()) breakdown.put(t, 0);
    for (Discrepancy d : report.discrepancies()) {
        for (DiscrepancyType t : d.types()) breakdown.merge(t, 1, Integer::sum);
    }
    return new ReconSummary(
            report.totalInternal(),
            report.totalExternal(),
            report.matched().size(),
            report.discrepancies().size(),
            Collections.unmodifiableMap(breakdown));
}

public String render(ReconSummary s) {
    StringBuilder sb = new StringBuilder();
    sb.append("Reconciliation summary\n----------------------\n")
      .append(String.format("  Internal trades : %d%n", s.totalInternal()))
      .append(String.format("  External trades : %d%n", s.totalExternal()))
      .append(String.format("  Matched         : %d%n", s.matchedCount()))
      .append(String.format("  With breaks     : %d%n", s.unmatchedCount()))
      .append("  Breakdown:\n");
    s.breakdownByType().forEach((type, count) ->
            sb.append(String.format("    - %-20s %d%n", type, count)));
    return sb.toString();
}
```
</details>

**Files to touch:** `service/ReconciliationService.java`, `dto/ReconSummary.java`.

---

### TICKET-I037 — CSV export

**What**
- New `service/ReconReportExporter.java` with `exportReconReport(List<ReconResult>, Path target)`: writes header `trade_ref,status,discrepancy_type,resolved_at`, RFC-4180-escapes each field, stages to `target + ".tmp"` then `Files.move(..., ATOMIC_MOVE)`.

**Why**
- The Day 9 monitoring lab tails this CSV as its sample input — half-written rows from a non-atomic write would surface as parse errors there. The hand-rolled escape (no OpenCSV) makes students own the edge cases they'll otherwise paper over.

**Observe**
- After a run, `head -1 target/recon-results.csv` prints exactly `trade_ref,status,discrepancy_type,resolved_at`; a row whose `discrepancy_type` contains a comma is wrapped in `"..."` with embedded `"` doubled; `ls target/*.tmp` returns nothing (the staged file was atomic-moved).

**Acceptance criteria:**
- [ ] Header row: `trade_ref,status,discrepancy_type,resolved_at`.
- [ ] CSV escaping for commas and quotes inside values.
- [ ] File written atomically (write to `.tmp`, then `Files.move()`).

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- Vanilla `java.nio.file.Files` + `BufferedWriter`. No OpenCSV / commons-csv — the point is to handle the escape rules yourself once.
- Atomic write pattern: write to `target + ".tmp"` first, then `Files.move(tmp, target, REPLACE_EXISTING, ATOMIC_MOVE)`. Readers never see a half-written file.
- RFC-4180 escape: if a field contains a comma, quote, CR, or LF → wrap in double quotes and double-up any internal quote.
- `ATOMIC_MOVE` can fail on cross-device moves (different mount points) — catch the IOException and fall back to a non-atomic move.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Method shape:

```
exportReconReport(List<ReconResult> results, Path target):
    null-guard both args
    tmp = target.resolveSibling(target.fileName + ".tmp")
    Files.createDirectories(tmp.parent) if parent exists
    try-with-resources Files.newBufferedWriter(tmp, UTF_8):
        write header line
        for r in results: write(rowFor(r)); newLine()
        flush
    Files.move(tmp, target, REPLACE_EXISTING, ATOMIC_MOVE)
        catch IOException -> fallback to non-atomic Files.move(tmp, target, REPLACE_EXISTING)
```

`rowFor(ReconResult r)` returns a comma-joined string of 4 escaped fields: trade ref (from `r.getTrade()` — null-safe), status name, discrepancy type name, resolvedAt as ISO-8601 (or empty string).

`escape(String value)`:
- null → ""
- contains `,` or `"` or `\n` or `\r` → wrap in `"..."` and replace every `"` with `""`
- else → return as-is.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
@Service
public class ReconReportExporter {

    private static final String HEADER = "trade_ref,status,discrepancy_type,resolved_at";

    public void exportReconReport(List<ReconResult> results, Path target) throws IOException {
        // TODO null-guard results & target
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        if (tmp.getParent() != null) Files.createDirectories(tmp.getParent());

        try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            w.write(HEADER); w.newLine();
            for (ReconResult r : results) {
                // TODO w.write(rowFor(r)); w.newLine();
            }
            w.flush();
        }

        // TODO Files.move(tmp, target, REPLACE_EXISTING, ATOMIC_MOVE)
        //      with IOException fallback to non-atomic move
    }

    private String rowFor(ReconResult r) {
        // TODO build comma-joined 4 fields via escape(...)
        return "";
    }

    private String escape(String value) {
        // TODO RFC-4180 escape
        return value;
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/service/ReconReportExporter.java`

```java
package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.model.ReconResult;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class ReconReportExporter {

    private static final String HEADER = "trade_ref,status,discrepancy_type,resolved_at";

    public void exportReconReport(List<ReconResult> results, Path target) throws IOException {
        Objects.requireNonNull(results, "results required");
        Objects.requireNonNull(target,  "target required");

        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        if (tmp.getParent() != null) Files.createDirectories(tmp.getParent());

        try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            w.write(HEADER);
            w.newLine();
            for (ReconResult r : results) {
                w.write(rowFor(r));
                w.newLine();
            }
            w.flush();
        }

        try {
            Files.move(tmp, target,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailed) {
            // Cross-device or filesystem without atomic-move support: fall back to a plain replace.
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String rowFor(ReconResult r) {
        String tradeRef       = r.getTrade() != null ? r.getTrade().getTradeRef() : "";
        String status         = r.getStatus() != null ? r.getStatus().name() : "";
        String discrepancy    = r.getDiscrepancyType() != null ? r.getDiscrepancyType().name() : "";
        Instant resolved      = r.getResolvedAt();
        String resolvedStr    = resolved != null ? resolved.toString() : "";
        return String.join(",",
                escape(tradeRef),
                escape(status),
                escape(discrepancy),
                escape(resolvedStr));
    }

    /** RFC-4180-style CSV escaping. Wraps in quotes when needed; doubles embedded quotes. */
    private String escape(String value) {
        if (value == null) return "";
        boolean needsQuoting = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        if (!needsQuoting) return value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
```
</details>

**Files to touch:** `service/ReconReportExporter.java`.

---

### TICKET-I038 — SOLID: extract `ITradeValidator`

**What**
- New `service/validator/ITradeValidator.java` (one method: `void validate(BaseTrade) throws TradeValidationException`) and `EquityTradeValidator.java` `@Component` implementation enforcing `exchange` non-blank, `lotSize > 0`, and `quantity` a whole multiple of `lotSize`.

**Why**
- Interface Segregation lets the Day 6 controller depend on the one-method contract instead of the concrete class — and the strategy map in I039 wires zero, one, or many validators by class without editing dispatch code. Open/Closed in action.

**Observe**
- `new EquityTradeValidator().validate(equityTradeWithQuantity150LotSize100)` throws `TradeValidationException` whose `getCode()` returns `Code.INVALID_VALUE` and whose message contains `"quantity must be a whole multiple of lotSize=100"`; calling `validate` with a non-`EquityTrade` throws `INVALID_VALUE` with `"EquityTradeValidator only accepts EquityTrade"`.

**Acceptance criteria:**
- [ ] `ITradeValidator` interface with `void validate(BaseTrade) throws TradeValidationException`.
- [ ] `EquityTradeValidator implements ITradeValidator`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- Interface Segregation Principle: callers only need the `validate` operation. They shouldn't depend on knowing which concrete validator they hold.
- The interface declares `throws TradeValidationException` — checked exceptions are part of the method signature, so callers must handle them.
- `EquityTradeValidator` is a `@Component` so Spring wires it (Day 6 controller will inject the orchestrator).
- Use the `instanceof` pattern (`trade instanceof EquityTrade et`) to safely down-cast.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Two artefacts:

1. **`service/validator/ITradeValidator`** — pure interface, one method:
   ```
   void validate(BaseTrade trade) throws TradeValidationException
   ```

2. **`service/validator/EquityTradeValidator`** — `@Component implements ITradeValidator`:
   - First-class guard: if `trade` is not actually an `EquityTrade`, throw `TradeValidationException(INVALID_VALUE, "EquityTradeValidator only accepts EquityTrade")`.
   - Rules:
     - `exchange` non-blank → else `MISSING_FIELD`.
     - `lotSize > 0` → else `INVALID_VALUE`.
     - `quantity` is a whole multiple of `lotSize` (use `quantity.remainder(BigDecimal.valueOf(lotSize)).signum() == 0`) → else `INVALID_VALUE`.

Day-3 ships the equity one; FX and Bond validators are mirror images and ship in the trainer's reference solution. You can implement them too for extra credit.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
// service/validator/ITradeValidator.java
package com.dbtraining.tradeflow.service.validator;

import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.model.BaseTrade;

public interface ITradeValidator {
    void validate(BaseTrade trade) throws TradeValidationException;
}
```

```java
// service/validator/EquityTradeValidator.java
package com.dbtraining.tradeflow.service.validator;

import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.exception.TradeValidationException.Code;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.EquityTrade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EquityTradeValidator implements ITradeValidator {

    @Override
    public void validate(BaseTrade trade) throws TradeValidationException {
        // TODO instanceof pattern: throw INVALID_VALUE if not an EquityTrade
        // TODO exchange null/blank -> MISSING_FIELD
        // TODO lotSize <= 0 -> INVALID_VALUE
        // TODO quantity not a multiple of lotSize -> INVALID_VALUE
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit:**
- `backend/src/main/java/com/dbtraining/tradeflow/service/validator/ITradeValidator.java`
- `backend/src/main/java/com/dbtraining/tradeflow/service/validator/EquityTradeValidator.java`

**Interface:**

```java
package com.dbtraining.tradeflow.service.validator;

import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.model.BaseTrade;

public interface ITradeValidator {
    void validate(BaseTrade trade) throws TradeValidationException;
}
```

**Implementation:**

```java
package com.dbtraining.tradeflow.service.validator;

import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.exception.TradeValidationException.Code;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.EquityTrade;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class EquityTradeValidator implements ITradeValidator {

    @Override
    public void validate(BaseTrade trade) throws TradeValidationException {
        if (!(trade instanceof EquityTrade et)) {
            throw new TradeValidationException(Code.INVALID_VALUE,
                    "EquityTradeValidator only accepts EquityTrade");
        }
        if (et.getExchange() == null || et.getExchange().isBlank()) {
            throw new TradeValidationException(Code.MISSING_FIELD, "exchange is required");
        }
        if (et.getLotSize() <= 0) {
            throw new TradeValidationException(Code.INVALID_VALUE, "lotSize must be > 0");
        }
        BigDecimal lotSize = BigDecimal.valueOf(et.getLotSize());
        if (et.getQuantity().remainder(lotSize).signum() != 0) {
            throw new TradeValidationException(Code.INVALID_VALUE,
                    "quantity must be a whole multiple of lotSize=" + et.getLotSize());
        }
    }
}
```
</details>

**Files to touch:** `service/validator/ITradeValidator.java`, `service/validator/EquityTradeValidator.java`.

---

### TICKET-I039 — SOLID: SRP — split parsing / validation / processing

**What**
- Three new service classes: `TradeParser` (CSV → `List<BaseTrade>`, wraps row failures in `InsufficientDataException` with `file:line` context), `TradeValidator` (strategy-map dispatch by `trade.getClass()`, returns `List<TradeValidationException>` findings — doesn't fail-fast), `TradeProcessor` (parse → validate → reconcile → render). Each < 150 lines.

**Why**
- This is the Day-3 architectural moment: the same three classes become the seams for Day 5's controller injection and Day 8's batching. Returning findings instead of throwing also matters operationally — a single bad row in a 50k-row batch shouldn't abort processing.

**Observe**
- `wc -l` on each of `TradeParser.java`, `TradeValidator.java`, `TradeProcessor.java` shows each is under 150 lines; throwing a malformed CSV row at the parser produces `InsufficientDataException` whose message starts with the filename and line number (e.g. `internal-trades.csv:7 — quantity must be a number, got 'abc'`).

**Acceptance criteria:**
- [ ] `TradeParser` reads CSV rows and returns `BaseTrade`s.
- [ ] `TradeValidator` orchestrates validators per asset class.
- [ ] `TradeProcessor` calls parser → validator → recon service.
- [ ] Each class < 150 lines, single responsibility.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- `TradeParser` only knows about strings → domain objects. No validation, no DB.
- `TradeValidator` only routes to the right `ITradeValidator` based on `trade.getClass()`. No parsing.
- `TradeProcessor` only composes the others: parse → validate → reconcile → summary. No I/O details.
- Use composition (constructor injection) so each class can be swapped or unit-tested in isolation.
- `TradeValidator` returns a `List<TradeValidationException>` rather than fail-fast — a single bad row shouldn't tank the whole batch.
</details>

<details>
<summary>Hint 2 — More guided</summary>

CSV format the parser handles (header row 1):

```
asset_class,trade_ref,instrument_id,counterparty_id,quantity,price,trade_date,status,
exchange,lot_size,base_ccy,quote_ccy,spot_rate,coupon_rate,maturity_date,face_value
```

`asset_class` is `EQUITY|FX|BOND` and decides which subclass builder runs. Asset-specific columns are only read for that asset; the rest can be blank.

**Parser steps:**
1. `Files.lines(file)` → `List<String>`.
2. Skip row 0 (header). For each remaining non-blank line, call `parseRow(line)`.
3. Wrap any `RuntimeException` from `parseRow` in `InsufficientDataException` so the caller gets line numbers.
4. `parseRow` splits on `","` with limit `-1` (keep trailing empties), reads the 8 base columns, then `switch` on `asset_class` to pick the right builder.

**TradeValidator orchestrator:**
- Construct with the three concrete validators (Spring injects them).
- Map `EquityTrade.class -> equityValidator` etc.
- `validateAll(List<BaseTrade>)` walks the list, looks up the right `ITradeValidator` by class, calls `validate`, collects any thrown exceptions.

**TradeProcessor:**
- Compose `TradeParser`, `TradeValidator`, `ReconciliationService`, `ReconReportExporter` via constructor injection.
- `process(internalCsv, externalCsv)`: parse both → validate both (log findings) → `matchTrades` → `generateReport` → `render` to log → return `ReconSummary`.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
// service/TradeParser.java
@Service
public class TradeParser {
    public List<BaseTrade> parseCsv(Path file) {
        // TODO Files.lines, skip header, parseRow each line
        return List.of();
    }
    private BaseTrade parseRow(String line) {
        // TODO split with limit -1
        // TODO read base cols, switch on asset_class, build the right subtype
        return null;
    }
}
```

```java
// service/TradeValidator.java
@Service
public class TradeValidator {
    private final Map<Class<? extends BaseTrade>, ITradeValidator> strategies;
    public TradeValidator(EquityTradeValidator e, FXTradeValidator f, BondTradeValidator b) {
        // TODO populate strategies map
        this.strategies = Map.of();
    }
    public List<TradeValidationException> validateAll(List<BaseTrade> trades) {
        // TODO for each trade: lookup validator, call validate, collect exceptions
        return List.of();
    }
}
```

```java
// service/TradeProcessor.java
@Service
public class TradeProcessor {
    // TODO constructor-injected fields: parser, validator, recon, exporter
    public ReconSummary process(Path internalCsv, Path externalCsv) {
        // TODO parse both -> validate both -> matchTrades -> generateReport -> render -> return summary
        return null;
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit (three new files under `backend/src/main/java/com/dbtraining/tradeflow/service/`):**

**`TradeParser.java`:**

```java
package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.exception.InsufficientDataException;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.BondTrade;
import com.dbtraining.tradeflow.model.EquityTrade;
import com.dbtraining.tradeflow.model.FXTrade;
import com.dbtraining.tradeflow.model.TradeStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class TradeParser {

    public List<BaseTrade> parseCsv(Path file) {
        try (Stream<String> lines = Files.lines(file)) {
            List<String> rows = lines.toList();
            if (rows.isEmpty()) return List.of();

            List<BaseTrade> out = new ArrayList<>();
            for (int i = 1; i < rows.size(); i++) {  // skip header
                String line = rows.get(i).trim();
                if (line.isEmpty()) continue;
                try {
                    out.add(parseRow(line));
                } catch (RuntimeException e) {
                    throw new InsufficientDataException(
                            file.getFileName() + ":" + (i + 1) + " — " + e.getMessage(), e);
                }
            }
            return out;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private BaseTrade parseRow(String line) {
        String[] c = line.split(",", -1);
        if (c.length < 8) {
            throw new InsufficientDataException("expected at least 8 columns, got " + c.length);
        }
        String assetClass     = c[0].trim();
        String tradeRef       = c[1].trim();
        long instrumentId     = parseLong(c[2], "instrument_id");
        long counterpartyId   = parseLong(c[3], "counterparty_id");
        BigDecimal quantity   = parseDecimal(c[4], "quantity");
        BigDecimal price      = parseDecimal(c[5], "price");
        LocalDate tradeDate   = LocalDate.parse(c[6].trim());
        TradeStatus status    = c[7].isBlank() ? TradeStatus.PENDING : TradeStatus.valueOf(c[7].trim());

        return switch (assetClass) {
            case "EQUITY" -> EquityTrade.builder()
                    .tradeRef(tradeRef).instrumentId(instrumentId).counterpartyId(counterpartyId)
                    .quantity(quantity).price(price).tradeDate(tradeDate).status(status)
                    .exchange(c.length > 8 ? c[8].trim() : "")
                    .lotSize(c.length > 9 ? (int) parseLong(c[9], "lot_size") : 1)
                    .build();
            case "FX" -> FXTrade.builder()
                    .tradeRef(tradeRef).instrumentId(instrumentId).counterpartyId(counterpartyId)
                    .quantity(quantity).price(price).tradeDate(tradeDate).status(status)
                    .baseCurrency(c.length > 10 ? c[10].trim() : "")
                    .quoteCurrency(c.length > 11 ? c[11].trim() : "")
                    .spotRate(c.length > 12 ? parseDecimal(c[12], "spot_rate") : BigDecimal.ZERO)
                    .build();
            case "BOND" -> BondTrade.builder()
                    .tradeRef(tradeRef).instrumentId(instrumentId).counterpartyId(counterpartyId)
                    .quantity(quantity).price(price).tradeDate(tradeDate).status(status)
                    .couponRate(c.length > 13 ? parseDecimal(c[13], "coupon_rate") : BigDecimal.ZERO)
                    .maturityDate(c.length > 14 ? LocalDate.parse(c[14].trim()) : tradeDate.plusYears(1))
                    .faceValue(c.length > 15 ? parseDecimal(c[15], "face_value") : BigDecimal.ONE)
                    .build();
            default -> throw new InsufficientDataException("unknown asset_class: " + assetClass);
        };
    }

    private static long parseLong(String raw, String field) {
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            throw new InsufficientDataException(
                    field + " must be an integer, got '" + raw + "'", e);
        }
    }

    private static BigDecimal parseDecimal(String raw, String field) {
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new InsufficientDataException(
                    field + " must be a number, got '" + raw + "'", e);
        }
    }
}
```

**`TradeValidator.java`** (orchestrator):

```java
package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.BondTrade;
import com.dbtraining.tradeflow.model.EquityTrade;
import com.dbtraining.tradeflow.model.FXTrade;
import com.dbtraining.tradeflow.service.validator.BondTradeValidator;
import com.dbtraining.tradeflow.service.validator.EquityTradeValidator;
import com.dbtraining.tradeflow.service.validator.FXTradeValidator;
import com.dbtraining.tradeflow.service.validator.ITradeValidator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TradeValidator {

    private final Map<Class<? extends BaseTrade>, ITradeValidator> strategies;

    public TradeValidator(EquityTradeValidator equity,
                          FXTradeValidator fx,
                          BondTradeValidator bond) {
        this.strategies = Map.of(
                EquityTrade.class, equity,
                FXTrade.class,     fx,
                BondTrade.class,   bond
        );
    }

    public List<TradeValidationException> validateAll(List<BaseTrade> trades) {
        List<TradeValidationException> findings = new ArrayList<>();
        for (BaseTrade t : trades) {
            ITradeValidator v = strategies.get(t.getClass());
            if (v == null) {
                findings.add(new TradeValidationException(
                        TradeValidationException.Code.INVALID_VALUE,
                        "no validator registered for " + t.getClass().getSimpleName()
                                + " (tradeRef=" + t.getTradeRef() + ")"));
                continue;
            }
            try {
                v.validate(t);
            } catch (TradeValidationException ex) {
                findings.add(ex);
            }
        }
        return findings;
    }
}
```

**`TradeProcessor.java`** (composer):

```java
package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.ReconReport;
import com.dbtraining.tradeflow.dto.ReconSummary;
import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.model.BaseTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class TradeProcessor {

    private static final Logger log = LoggerFactory.getLogger(TradeProcessor.class);

    private final TradeParser parser;
    private final TradeValidator validator;
    private final ReconciliationService recon;
    private final ReconReportExporter exporter;

    public TradeProcessor(TradeParser parser, TradeValidator validator,
                          ReconciliationService recon, ReconReportExporter exporter) {
        this.parser = parser; this.validator = validator;
        this.recon  = recon;  this.exporter  = exporter;
    }

    public ReconSummary process(Path internalCsv, Path externalCsv) {
        log.info("Parsing internal feed: {}", internalCsv);
        List<BaseTrade> internal = parser.parseCsv(internalCsv);
        log.info("Parsing external feed: {}", externalCsv);
        List<BaseTrade> external = parser.parseCsv(externalCsv);

        for (TradeValidationException finding : validator.validateAll(internal)) {
            log.warn("internal validation finding: [{}] {}", finding.getCode(), finding.getMessage());
        }
        for (TradeValidationException finding : validator.validateAll(external)) {
            log.warn("external validation finding: [{}] {}", finding.getCode(), finding.getMessage());
        }

        ReconReport report = recon.matchTrades(internal, external);
        ReconSummary summary = recon.generateReport(report);
        log.info("\n{}", recon.render(summary));
        return summary;
    }
}
```
</details>

**Files to touch:** `service/TradeParser.java`, `service/TradeValidator.java`, `service/TradeProcessor.java`.

---

### TICKET-I040 — Manual run on sample data

**What**
- A `@Bean CommandLineRunner` in `TradeflowApplication` that invokes `processor.process(...)` on two CSVs (`backend/src/test/resources/internal-trades.csv` and `external-trades.csv`) hand-crafted to exercise one of each `DiscrepancyType` (`PRICE_MISMATCH`, `QUANTITY_MISMATCH`, `DATE_MISMATCH`, `MISSING_TRADE`).

**Why**
- End-to-end smoke proof that I028–I039 are wired together — the moment students see the printed `ReconSummary` with all four buckets non-zero. The CSV files become the fixture the Day 9 monitoring lab reads.

**Observe**
- `./mvnw -q spring-boot:run` prints a `ReconSummary` whose `breakdownByType` map shows `PRICE_MISMATCH=1, QUANTITY_MISMATCH=1, DATE_MISMATCH=1, MISSING_TRADE>=1` (the leftover-sweep from I034 fires for the unmatched ref); if `MISSING_TRADE` shows zero, that sweep is missing.

**Acceptance criteria:**
- [ ] Two CSV files in `src/test/resources/`: `internal-trades.csv`,
  `external-trades.csv` — one mismatch of each discrepancy type.
- [ ] Running `TradeflowApplication.main` prints the `ReconSummary` with all
  4 break types.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

- The CSV header must match what `TradeParser.parseRow` expects (asset_class first, then the 7 base fields, then the asset-specific tail columns).
- Engineer the data so every break type is exercised: one price mismatch, one quantity mismatch, one date mismatch, one trade-ref that only appears on one side.
- Use a `CommandLineRunner` `@Bean` (or `ApplicationRunner`) inside `TradeflowApplication` so the pipeline fires after Spring is up — calling `processor.process(...)` directly in `main` is fine but the runner pattern is cleaner.
- Save the run command in `scripts/run-recon-demo.sh` — instructor sign-off needs a reproducible one-liner.
</details>

<details>
<summary>Hint 2 — More guided</summary>

CSV row template (16 columns; blanks are fine for non-relevant asset-specific cells):

```
asset_class,trade_ref,instrument_id,counterparty_id,quantity,price,trade_date,status,exchange,lot_size,base_ccy,quote_ccy,spot_rate,coupon_rate,maturity_date,face_value
EQUITY,TRD-001,1,1,1000,245.50,2026-03-01,PENDING,XETR,100,,,,,,
FX,TRD-002,2,2,1000000,1.09,2026-03-01,PENDING,,,EUR,USD,1.09,,,
BOND,TRD-003,3,3,100000,99.50,2026-03-02,PENDING,,,,,,5.0,2031-03-02,100000
```

Plan the **external** CSV so:
- Row 1 (`TRD-001`) has a different `price` → `PRICE_MISMATCH`.
- Row 2 (`TRD-002`) has a different `quantity` → `QUANTITY_MISMATCH`.
- Row 3 (`TRD-003`) has a different `trade_date` → `DATE_MISMATCH`.
- Add a `TRD-004` that only exists internally (or only externally) → `MISSING_TRADE`.

In `TradeflowApplication`:

```java
@Bean
CommandLineRunner reconDemoRunner(TradeProcessor processor) {
    return args -> {
        Path internal = Path.of("src/test/resources/internal-trades.csv");
        Path external = Path.of("src/test/resources/external-trades.csv");
        processor.process(internal, external);
    };
}
```
</details>

<details>
<summary>Hint 3 — Sample CSV + runner skeleton</summary>

`backend/src/test/resources/internal-trades.csv`:

```
asset_class,trade_ref,instrument_id,counterparty_id,quantity,price,trade_date,status,exchange,lot_size,base_ccy,quote_ccy,spot_rate,coupon_rate,maturity_date,face_value
EQUITY,TRD-001,1,1,1000,245.50,2026-03-01,PENDING,XETR,100,,,,,,
FX,TRD-002,2,2,1000000,1.09,2026-03-01,PENDING,,,EUR,USD,1.09,,,
BOND,TRD-003,3,3,100000,99.50,2026-03-02,PENDING,,,,,,5.0,2031-03-02,100000
EQUITY,TRD-004,1,1,500,246.00,2026-03-01,PENDING,XETR,100,,,,,,
```

`backend/src/test/resources/external-trades.csv`:

```
asset_class,trade_ref,instrument_id,counterparty_id,quantity,price,trade_date,status,exchange,lot_size,base_ccy,quote_ccy,spot_rate,coupon_rate,maturity_date,face_value
EQUITY,TRD-001,1,1,1000,245.75,2026-03-01,PENDING,XETR,100,,,,,,
FX,TRD-002,2,2,1100000,1.09,2026-03-01,PENDING,,,EUR,USD,1.09,,,
BOND,TRD-003,3,3,100000,99.50,2026-03-03,PENDING,,,,,,5.0,2031-03-02,100000
```

(`TRD-004` deliberately missing from external → `MISSING_TRADE`.)

`scripts/run-recon-demo.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../backend"
./mvnw -q spring-boot:run
```
</details>

<details>
<summary>Reference — full walkthrough</summary>

**Files to edit:**
- `backend/src/main/java/com/dbtraining/tradeflow/TradeflowApplication.java` (add `@Bean CommandLineRunner`)
- `backend/src/test/resources/internal-trades.csv` (new)
- `backend/src/test/resources/external-trades.csv` (new)
- `scripts/run-recon-demo.sh` (new)

**Add the runner bean to `TradeflowApplication`:**

```java
@Bean
CommandLineRunner reconDemoRunner(TradeProcessor processor) {
    return args -> {
        Path internal = Path.of("src/test/resources/internal-trades.csv");
        Path external = Path.of("src/test/resources/external-trades.csv");
        ReconSummary summary = processor.process(internal, external);
        System.out.println();
        System.out.println("== Day-3 recon demo (TICKET-I040) ==================================================");
        System.out.println(summary);
        System.out.println("====================================================================================");
    };
}
```

(Imports needed: `org.springframework.boot.CommandLineRunner`, `org.springframework.context.annotation.Bean`, `java.nio.file.Path`, your `ReconSummary` and `TradeProcessor`.)

**`backend/src/test/resources/internal-trades.csv`:**

```
asset_class,trade_ref,instrument_id,counterparty_id,quantity,price,trade_date,status,exchange,lot_size,base_ccy,quote_ccy,spot_rate,coupon_rate,maturity_date,face_value
EQUITY,TRD-001,1,1,1000,245.50,2026-03-01,PENDING,XETR,100,,,,,,
FX,TRD-002,2,2,1000000,1.09,2026-03-01,PENDING,,,EUR,USD,1.09,,,
BOND,TRD-003,3,3,100000,99.50,2026-03-02,PENDING,,,,,,5.0,2031-03-02,100000
EQUITY,TRD-004,1,1,500,246.00,2026-03-01,PENDING,XETR,100,,,,,,
```

**`backend/src/test/resources/external-trades.csv`** (note `TRD-004` is deliberately missing → `MISSING_TRADE`):

```
asset_class,trade_ref,instrument_id,counterparty_id,quantity,price,trade_date,status,exchange,lot_size,base_ccy,quote_ccy,spot_rate,coupon_rate,maturity_date,face_value
EQUITY,TRD-001,1,1,1000,245.75,2026-03-01,PENDING,XETR,100,,,,,,
FX,TRD-002,2,2,1100000,1.09,2026-03-01,PENDING,,,EUR,USD,1.09,,,
BOND,TRD-003,3,3,100000,99.50,2026-03-03,PENDING,,,,,,5.0,2031-03-02,100000
```

**`scripts/run-recon-demo.sh`** (remember `chmod +x` after creating):

```bash
#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../backend"
./mvnw -q spring-boot:run
```

Running the script boots Spring and prints a `ReconSummary` whose `breakdownByType` map shows non-zero counts for `PRICE_MISMATCH`, `QUANTITY_MISMATCH`, `DATE_MISMATCH`, and `MISSING_TRADE`. Screenshot the output for instructor sign-off.
</details>

**Files to touch:** `TradeflowApplication.java`, `src/test/resources/`.

---

## Run and Observe — End of Sprint 2 (OOP Patterns & Business Logic)

You've shipped 13 tickets (I028–I040): an abstract `BaseTrade`, three subclasses, two custom exceptions, the `ReconciliationService`, CSV export, and the SOLID refactors. Before the AI Code Review Lab, prove the whole pipeline is wired and the structure is right.

**Run:**

> **Terminal** — from `tradeflow-studentscopy/backend/`

```bash
# Plain Java today — Spring isn't wired until Day 5.
# Step 1: everything compiles.
./mvnw -q compile

# Step 2: run the recon pipeline via a plain main() entry point.
# (Use whichever main you wired in I040 — TradeflowApplication or a
#  Day3Demo class. exec:java runs it without Spring.)
./mvnw -q exec:java -Dexec.mainClass=com.dbtraining.tradeflow.TradeflowApplication
```

The console should print a `ReconSummary` whose `breakdownByType` map shows non-zero counts for all four break types: `PRICE_MISMATCH`, `QUANTITY_MISMATCH`, `DATE_MISMATCH`, `MISSING_TRADE`.

**Observe:**

| Check | Expected after Sprint 2 |
|---|---|
| `./mvnw -q compile` | `BUILD SUCCESS` — no errors. `BaseTrade` is abstract; `EquityTrade`, `FXTrade`, `BondTrade` each `extends BaseTrade` and implement `assetClassDescription()` |
| `ls src/main/java/com/dbtraining/tradeflow/model` | `BaseTrade.java`, `EquityTrade.java`, `FXTrade.java`, `BondTrade.java` (plus the existing `Trade.java` JPA entity) |
| `ls src/main/java/com/dbtraining/tradeflow/exception` | `TradeValidationException.java`, `InsufficientDataException.java` — both extend a project base or `RuntimeException` |
| `ls src/main/java/com/dbtraining/tradeflow/service/validator` | `ITradeValidator.java` interface + at least one implementation (`EquityTradeValidator.java`); SRP split means parsing / validation / processing live in separate classes (I039) |
| `main()` recon output | `ReconSummary` printed with all 4 break-type buckets non-zero; CSV export file written to disk (I037) |

**Negative tests — prove your custom exceptions actually fire:**

```java
// 1. Constructing a trade with a null required field
new EquityTrade.Builder()
    .tradeRef(null)                  // null tradeRef
    .instrumentId(1L).counterpartyId(1L)
    .quantity(new BigDecimal("100")).price(new BigDecimal("245.50"))
    .tradeDate(LocalDate.now())
    .exchange("XETR").lotSize(100)
    .build();
// Expected: NullPointerException ("tradeRef required") from BaseTrade constructor.

// 2. ITradeValidator catches a domain rule violation
EquityTrade odd = new EquityTrade.Builder()
    .tradeRef("TRD-X").instrumentId(1L).counterpartyId(1L)
    .quantity(new BigDecimal("150"))  // not a multiple of lotSize 100
    .price(new BigDecimal("245.50"))
    .tradeDate(LocalDate.now())
    .exchange("XETR").lotSize(100)
    .build();
new EquityTradeValidator().validate(odd);
// Expected: TradeValidationException with code INVALID_VALUE.

// 3. ReconciliationService surfaces a missing trade
// Feed it an internal list that contains TRD-004 with no external counterpart.
// Expected: the returned ReconSummary's breakdownByType has MISSING_TRADE >= 1.
```

**If something looks wrong:**
- `BaseTrade` can be instantiated → you forgot the `abstract` keyword (I028).
- All 4 break-type buckets aren't populated → re-read I035 (discrepancy classification) and the I034 leftover-sweep that produces `MISSING_TRADE`.
- `EquityTradeValidator` compiles but does nothing → confirm it's actually called from the SRP-split processor (I039), not bypassed.
- CSV export is empty → check that I037's writer flushes/closes the stream.

---

## AI Code Review Lab (Day 3 PM — Sprint 3, 30 min)

Paste your `ReconciliationService.matchTrades` into Claude with the prompt:

> *"Review this method for null safety, edge cases, and code smell. Suggest one
> refactor that improves readability without changing behavior."*

- Identify ONE legit issue Claude finds.
- Fix it.
- In your PR description, paste the prompt + the diff + your one-line
  explanation of the fix.

This counts toward your daily commit log.

<details>
<summary>Step-by-step walkthrough + sample fixes (click to expand)</summary>

### Steps

1. **Open the file** `ReconciliationService.java` and copy `matchTrades(...)`
   in full — including imports if you reference custom types.
2. **Paste into Claude** with the prompt above. Try a *second* sharper prompt
   if the first is too generic.
3. **Read every suggestion critically** — AI flags style nits and real bugs in
   the same tone. Reject the nits, keep the bugs.
4. **Pick ONE issue** that's an actual defect (NPE, off-by-one, wrong
   comparison, unchecked Optional, mutable shared state).
5. **Write the fix** in code, run the tests, confirm green.
6. **PR description must include:** prompt + raw AI output + the diff + a
   one-liner on what was wrong and why your fix is correct.

### Sharper prompts to try

> *"You are a senior Java reviewer. Find bugs only — ignore formatting.
> For each bug: (1) line, (2) why it's wrong, (3) the minimal fix."*

> *"Walk through this method as if a `null` came in for every parameter,
> one at a time. Where does it NPE? Where does it silently produce a
> wrong answer?"*

### Common real findings (use these as a checklist when AI's output is vague)

| Symptom in code | Why it's a bug | Minimal fix |
|---|---|---|
| `if (a.equals(b))` where `a` may be null | NPE on null input | `Objects.equals(a, b)` |
| `BigDecimal.equals` on amounts | `1.0` ≠ `1.00` — fails on scale | `a.compareTo(b) == 0` |
| `trades.stream().filter(t -> t.getRef() == ref)` | reference compare instead of value | `.equals(ref)` |
| Iterating + mutating same `List` | `ConcurrentModificationException` | iterate copy, or use `removeIf` |
| `Optional.get()` without `isPresent` | crashes when empty | `.orElse(...)` or `.ifPresent(...)` |

### Example fix — pattern (adapt to YOUR class)

**Before:**

```java
if (trade.getCounterparty().equals(other.getCounterparty())) {
    matches.add(trade);
}
```

**After:**

```java
// counterparty can be null for OTC trades pre-allocation
if (Objects.equals(trade.getCounterparty(), other.getCounterparty())) {
    matches.add(trade);
}
```

**PR one-liner:** *"`String.equals` NPEs when `counterparty` is null (OTC
trades pre-allocation). Switched to `Objects.equals` which handles both
sides being null as equal."*

</details>

---

## Run and Observe — End of Sprint 3 (AI Code Review Lab)

You took an AI review of `matchTrades`, picked one real defect, and shipped a minimal fix. The deliverable is reviewable — recompile, re-run, and confirm nothing regressed.

**Run:**

> **Terminal** — from `tradeflow-studentscopy/backend/`

```bash
# Recompile after the AI-suggested fix.
./mvnw -q compile

# Re-run the recon pipeline to confirm the fix didn't break the happy path.
./mvnw -q exec:java -Dexec.mainClass=com.dbtraining.tradeflow.TradeflowApplication
```

The summary output should be unchanged for the happy path (all 4 break types still appear) — the fix should only show its effect under the edge case AI flagged (e.g. a null `counterparty` no longer NPEs).

**Observe:**

| Check | Expected after Sprint 3 |
|---|---|
| `./mvnw -q compile` | `BUILD SUCCESS` — the AI fix compiles cleanly |
| `git diff src/main/java/.../ReconciliationService.java` | A small, focused diff (typically 1–5 lines) — the one defect, not a stylistic rewrite |
| Recon output | `ReconSummary` still prints with all 4 break-type buckets non-zero — no behavioural regression |
| PR description draft | Contains: the exact prompt you used, the raw AI output, the diff, and your one-line "what was wrong + why the fix is correct" |
| Daily commit log | One commit referencing this fix; clearly distinguishable from the I028–I040 commits |

**If something looks wrong:**
- AI suggested a "fix" that changes behaviour beyond the bug (e.g. a wholesale rewrite) → reject it. Pick a different, narrower finding from the common-findings table above.
- Tests/recon output differs from Sprint 2 → your fix was too broad or wrong; revert and pick a different finding.
- AI's only suggestions are formatting nits → re-prompt with the "Find bugs only — ignore formatting" sharper prompt.

---

## End-of-day checklist

- [ ] 13 tickets merged.
- [ ] `mvn compile` clean.
- [ ] Run script prints a recon summary that includes all 4 break types.
- [ ] PRs reference ticket IDs.

Next: [Day 4 — Collections, JDBC & JUnit](../day4/README.md)
