# Day 4 — Collections, JDBC & Unit Testing

> Theme: **Real DB + real tests.**
> Tickets: **I041 – I053** (13 tickets)
> Module: Java — Modules 5 & 6

By end of day:

- `TradeService` uses Streams + Maps idiomatically.
- JDBC DAOs read/write the real PostgreSQL.
- A JUnit + Mockito suite covers ≥ 70% of the business code.

---

## Sprint 3 — Collections, JDBC & Tests

### TICKET-I041 — Refactor `TradeService` with a `HashMap`

**What**
- Switch the in-memory store from `List<BaseTrade>` to `Map<String, BaseTrade>` keyed by `tradeRef`; lookup goes from O(n) scan to O(1) map hit.

**Why**
- Day 5's JPA `TradeRepository` will give you `findByTradeRef(String)` for free — students need to feel the pain of writing the O(1) lookup by hand first, otherwise Spring Data feels like magic instead of an obvious win.
- Every downstream sprint (recon in I048-I052, REST in Day 6) relies on cheap lookup-by-ref; getting the data structure right today keeps those tests fast.

**Observe**
- `TradeService.java` no longer declares any `List<BaseTrade>` field; `grep -n "List<BaseTrade>" service/TradeService.java` returns nothing except method signatures.
- `getAllTrades()` return type is `Collection<BaseTrade>` (not `List`) and a caller that tries `.add(...)` on it throws `UnsupportedOperationException`.

**Acceptance criteria:**
- [ ] All lookups by `tradeRef` are O(1).
- [ ] `getAllTrades()` returns `Collection<BaseTrade>` (don't expose the map).

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

`HashMap` gives you O(1) `get` / `put` / `remove` keyed by the natural key
(`tradeRef`). The trade-off is iteration order is undefined — fine for a store,
not fine if the API contract was "FIFO". Never leak the map: hand back a
read-only view of `.values()` so callers cannot mutate your internal state.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Field: `private final Map<String, BaseTrade> trades = new HashMap<>();`
- `addTrade(BaseTrade t)` → `trades.put(t.getTradeRef(), t)`. Decide what to
  do on duplicate key (reject? overwrite? log?). Be explicit.
- `findByRef(String ref)` → `Optional.ofNullable(trades.get(ref))`.
- `getAllTrades()` → `Collections.unmodifiableCollection(trades.values())`.
- Drop any `List<BaseTrade>` field + the linear-scan loop that used it.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
public class TradeService {

    private final Map<String, BaseTrade> trades = new HashMap<>();

    public void addTrade(BaseTrade t) {
        // TODO: reject if tradeRef already present
        trades.put(t.getTradeRef(), t);
    }

    public Optional<BaseTrade> findByRef(String tradeRef) {
        // TODO
        return Optional.empty();
    }

    public Collection<BaseTrade> getAllTrades() {
        // TODO: return unmodifiable view
        return List.of();
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/service/TradeService.java`

```java
package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.model.BaseTrade;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TradeService {

    private final Map<String, BaseTrade> trades = new HashMap<>();

    public void addTrade(BaseTrade t) {
        if (trades.containsKey(t.getTradeRef())) {
            throw new IllegalStateException(
                    "Duplicate tradeRef: " + t.getTradeRef());
        }
        trades.put(t.getTradeRef(), t);
    }

    public Optional<BaseTrade> findByRef(String tradeRef) {
        return Optional.ofNullable(trades.get(tradeRef));
    }

    public Collection<BaseTrade> getAllTrades() {
        return Collections.unmodifiableCollection(trades.values());
    }

    public int size() {
        return trades.size();
    }
}
```
</details>

**Files to touch:** `service/TradeService.java`.

---

### TICKET-I042 — Streams pipeline 1

**What**
- A pure-function `Map<Long, BigDecimal> sumByCounterparty(List<BaseTrade>)` that filters to `MATCHED`, groups by `counterpartyId`, sums `quantity * price` in `BigDecimal`.

**Why**
- Day 6's `/api/reports/exposure` REST endpoint just wraps this method — getting the Streams collector right today means the controller is a one-liner tomorrow.
- Forces the `BigDecimal` discipline (no `summingDouble`); the same discipline catches the price-mismatch bug under test in I049.

**Observe**
- `./mvnw -pl backend compile` is clean; the method signature returns `Map<Long, BigDecimal>` (not `Map<Long, Double>`).
- A scratch test with 3 MATCHED trades for cp 1 and 2 for cp 2 returns a map of size exactly 2, no third entry for any non-MATCHED status.

**Acceptance criteria:**
- [ ] One Streams pipeline.
- [ ] Filters by `TradeStatus.MATCHED`.
- [ ] Groups by `counterpartyId`.
- [ ] Sums `quantity * price`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

The classic group-then-reduce shape: `filter → collect(groupingBy(..., reducing(...)))`.
`Collectors.reducing` lets you fold each group into a single value (here:
`BigDecimal` sum of notional). Do not use `summingDouble` for money — IEEE-754
will silently round.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Stream over the input list.
- `.filter(t -> t.getStatus() == TradeStatus.MATCHED)`.
- `.collect(Collectors.groupingBy(BaseTrade::getCounterpartyId, downstream))`.
- Downstream collector: `Collectors.reducing(BigDecimal.ZERO, BaseTrade::getNotional, BigDecimal::add)`.
- `BaseTrade` already exposes `getNotional()` = `quantity.multiply(price)` —
  use it instead of inlining the multiplication twice.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
public Map<Long, BigDecimal> sumByCounterparty(List<BaseTrade> input) {
    return input.stream()
            .filter(t -> /* TODO */ false)
            .collect(Collectors.groupingBy(
                    /* TODO key extractor */,
                    Collectors.reducing(
                            BigDecimal.ZERO,
                            /* TODO value extractor */,
                            BigDecimal::add)));
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/service/TradeService.java` (add this method to the class)

```java
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.TradeStatus;

/**
 * Sum notional (quantity * price) per counterparty across MATCHED trades only.
 * Returns an empty map on an empty input list.
 */
public Map<Long, BigDecimal> sumByCounterparty(List<BaseTrade> input) {
    return input.stream()
            .filter(t -> t.getStatus() == TradeStatus.MATCHED)
            .collect(Collectors.groupingBy(
                    BaseTrade::getCounterpartyId,
                    Collectors.reducing(
                            BigDecimal.ZERO,
                            BaseTrade::getNotional,
                            BigDecimal::add)));
}
```
</details>

**Files to touch:** `service/TradeService.java`.

---

### TICKET-I043 — Streams pipeline 2 — Top-N

**What**
- A `List<BaseTrade> topNByValue(int n)` that sorts the in-memory trades by `quantity * price` descending and returns the first `n`; throws `IllegalArgumentException` on `n <= 0`.

**Why**
- Day 7's dashboard "top-5 trades by notional" widget consumes this directly — building the Streams pipeline today means the React side has a stable contract before the JPA migration on Day 5.
- Drives the `Comparator.reversed()` + `.limit()` idiom that returns later for the recon ranking in Day 9.

**Observe**
- `topNByValue(0)` throws `IllegalArgumentException` with a message containing the offending value; `topNByValue(3)` on five trades with notionals 10/50/30/20/40 returns the three with 50/40/30 in that order.
- The returned list is `unmodifiableList` (from `.toList()`) — `.add(...)` on it throws `UnsupportedOperationException`.

**Acceptance criteria:**
- [ ] Sorts by `quantity * price` descending, limits to `n`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

`Comparator.comparing(...).reversed()` for descending order, then `.limit(n)`
and collect. Guard against `n <= 0` — Streams will happily accept `limit(0)`
and you'll silently return an empty list, which is rarely what callers wanted.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Validate `n > 0` at the top — `IllegalArgumentException` is the right call.
- Stream over `trades.values()` (the field from I041), not a separate list.
- Sort by `Comparator.comparing(BaseTrade::getNotional).reversed()`.
- `.limit(n).toList()` — Java 16+ returns an unmodifiable list directly.
- Do NOT mutate the underlying map; streams are read-only by design here.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
public List<BaseTrade> topNByValue(int n) {
    if (n <= 0) /* TODO: throw */;
    return trades.values().stream()
            .sorted(/* TODO comparator */)
            .limit(n)
            .toList();
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/service/TradeService.java` (add this method to the class)

```java
import java.util.Comparator;
import java.util.List;

import com.dbtraining.tradeflow.model.BaseTrade;

public List<BaseTrade> topNByValue(int n) {
    if (n <= 0) {
        throw new IllegalArgumentException("n must be > 0 (was " + n + ")");
    }
    return trades.values().stream()
            .sorted(Comparator.comparing(BaseTrade::getNotional).reversed()
                    .thenComparing(BaseTrade::getTradeRef))
            .limit(n)
            .toList();
}
```
</details>

**Files to touch:** `service/TradeService.java`.

---

### TICKET-I044 — `DatabaseConfig` utility (HikariCP)

**What**
- A `DatabaseConfig` utility with `static DataSource dataSource()` that returns a configured `HikariDataSource` — JDBC URL / user / password from env vars with localhost defaults, max pool 10, connection timeout 5 s.

**Why**
- Every DAO from I045-I047 will inject this `DataSource`; without a single shared pool you'd open a fresh JDBC connection per query and exhaust Postgres' `max_connections` by mid-morning.
- Day 5's Spring Boot autoconfiguration replaces this hand-rolled pool with the same Hikari instance configured via `application.properties` — students need to see the bare wiring once so the autoconfig is meaningful, not magical.

**Observe**
- Startup log emits exactly one line `HikariPool-1 - Start completed` (or `tradeflow-jdbc - Start completed` if you named the pool); a second call to `dataSource()` does NOT log it twice if you cached the singleton.
- `Connection refused` on `localhost:5432` means the Day-1 Postgres container isn't up; `docker compose ps postgres` should show `Up`.

**Acceptance criteria:**
- [ ] `getDataSource()` returns a configured `HikariDataSource`.
- [ ] Config from `application.properties` or env vars (`POSTGRES_*`).
- [ ] Max pool size 10; connection timeout 5s.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

HikariCP is the de-facto JDBC pool: open the connection once, reuse it many
times. Wrap it in a single static `dataSource()` factory so every DAO shares
one pool — opening a pool per DAO would defeat the point. Read URL / user /
password from env vars so the same JAR works in dev, CI, and UAT without
recompiling.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- One package-level utility class — `final`, private constructor — exposing
  a single static method that returns a `javax.sql.DataSource`.
- Inside: build a `HikariConfig`, set JDBC URL + user + password from env
  (with sensible localhost defaults), set max pool = 10, timeout = 5_000 ms.
- Give the pool a name (`tradeflow-jdbc`) so it shows up clearly in logs
  and JMX.
- Return `new HikariDataSource(cfg)`.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
public final class DatabaseConfig {

    private DatabaseConfig() {}

    public static DataSource dataSource() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(System.getenv().getOrDefault("JDBC_URL", /* TODO default */));
        cfg.setUsername(/* TODO */);
        cfg.setPassword(/* TODO */);
        cfg.setMaximumPoolSize(/* TODO */);
        cfg.setConnectionTimeout(/* TODO */);
        return new HikariDataSource(cfg);
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/config/DatabaseConfig.java`

```java
package com.dbtraining.tradeflow.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public final class DatabaseConfig {

    private static volatile DataSource instance;

    private DatabaseConfig() {}

    /** Lazily-built singleton — call as often as you like, you'll only ever get one pool. */
    public static DataSource dataSource() {
        DataSource local = instance;
        if (local == null) {
            synchronized (DatabaseConfig.class) {
                local = instance;
                if (local == null) {
                    HikariConfig cfg = new HikariConfig();
                    cfg.setJdbcUrl(System.getenv().getOrDefault(
                            "JDBC_URL", "jdbc:postgresql://localhost:5432/tradeflow"));
                    cfg.setUsername(System.getenv().getOrDefault("POSTGRES_USER", "tradeflow_user"));
                    cfg.setPassword(System.getenv().getOrDefault("POSTGRES_PASSWORD", "changeme"));
                    cfg.setMaximumPoolSize(10);
                    cfg.setConnectionTimeout(5_000);
                    cfg.setPoolName("tradeflow-jdbc");
                    instance = local = new HikariDataSource(cfg);
                }
            }
        }
        return local;
    }
}
```
</details>

**Files to touch:** `config/DatabaseConfig.java`.

---

### TICKET-I045 — `TradeDAO`

**What**
- A `TradeDAO` with `insert(Trade) → long`, `findByRef(String) → Optional<Trade>`, `findAll() → List<Trade>`, `updateStatus(String, TradeStatus) → int` — all via `PreparedStatement`, all under `try-with-resources`.

**Why**
- Day 5's JPA entities replace today's hand-rolled JDBC code — students need to feel the boilerplate (column mapping, generated-key plumbing, JOIN-by-hand) first, so `@Entity` + `JpaRepository` is recognised as a win, not a starting point.
- Hand-mapping rows into `Trade` + nested `Instrument` + `Counterparty` mirrors what Spring Data does behind the scenes; tomorrow's `@ManyToOne` annotations land with context.

**Observe**
- `grep -n "+ " backend/src/main/java/com/dbtraining/tradeflow/repository/TradeDAO.java | grep -i "SELECT\|INSERT\|UPDATE"` returns nothing — every value is bound via `setString`/`setLong`/etc.
- Round-trip smoke from a scratch `main()`: `long id = dao.insert(t); dao.findByRef(t.getTradeRef()).get()` returns a `Trade` whose `getInstrument().getSymbol()` is non-null (proves the JOIN).

**Acceptance criteria:**
- [ ] `insert(Trade) → long`.
- [ ] `findByRef(String tradeRef) → Optional<Trade>`.
- [ ] `findAll() → List<Trade>`.
- [ ] `updateStatus(String tradeRef, TradeStatus newStatus)`.
- [ ] All use `PreparedStatement` — NO string concatenation.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Raw JDBC + `PreparedStatement` everywhere — strings concatenated into SQL
are how SQL injection happens. Every connection / statement / result-set
opens inside `try-with-resources` so it auto-closes on success and on
exception. For the generated PK on insert, ask the driver for it via
`Statement.RETURN_GENERATED_KEYS`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Constructor takes a `DataSource` (the one from I044).
- `insert(Trade)` → `INSERT INTO trades (...) VALUES (?, ?, ?, ?, ?, ?, ?)`
  with `Statement.RETURN_GENERATED_KEYS`; pull the key from `ps.getGeneratedKeys()`.
- `findByRef(ref)` → `SELECT ... FROM trades t JOIN instruments i JOIN counterparties c
  WHERE t.trade_ref = ? LIMIT 1`. Returning a `Trade` with hydrated `Instrument`
  + `Counterparty` mirrors what JPA gives you on Day 5.
- `findAll()` → same SELECT, no WHERE, ordered by `trade_date DESC`.
- `updateStatus(ref, status)` → `UPDATE trades SET status = ? WHERE trade_ref = ?`,
  return `int affected`.
- One private `mapRow(ResultSet)` static helper to keep the query methods tidy.
- Wrap `SQLException` in your own `JdbcException` (runtime) so callers don't
  drown in checked-exception ceremony.
</details>

<details>
<summary>Hint 3 — DAO skeleton</summary>

```java
public class TradeDAO {

    private final DataSource dataSource;

    public TradeDAO(DataSource dataSource) { this.dataSource = dataSource; }

    public long insert(Trade trade) {
        String sql = "INSERT INTO trades " +
                "(trade_ref, instrument_id, counterparty_id, quantity, price, trade_date, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // TODO: bind params, executeUpdate, return generated key
            return -1;
        } catch (SQLException e) {
            throw new JdbcException("insert failed", e);
        }
    }

    public Optional<Trade> findByRef(String tradeRef) { /* TODO */ return Optional.empty(); }
    public List<Trade> findAll()                      { /* TODO */ return List.of(); }
    public int updateStatus(String tradeRef, TradeStatus newStatus) { /* TODO */ return 0; }

    private static Trade mapRow(ResultSet rs) throws SQLException { /* TODO */ return null; }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/repository/TradeDAO.java`

```java
package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class TradeDAO {

    private static final String SELECT_COLUMNS =
            "t.id AS t_id, t.trade_ref AS t_trade_ref, t.quantity AS t_quantity, " +
            "t.price AS t_price, t.trade_date AS t_trade_date, t.status AS t_status, " +
            "t.created_at AS t_created_at, " +
            "i.symbol AS i_symbol, i.name AS i_name, i.asset_class AS i_asset_class, " +
            "i.currency AS i_currency, i.isin AS i_isin, " +
            "c.name AS c_name, c.lei_code AS c_lei_code, c.region AS c_region ";

    private static final String FROM_JOIN =
            "FROM trades t " +
            "JOIN instruments i    ON i.id = t.instrument_id " +
            "JOIN counterparties c ON c.id = t.counterparty_id ";

    private final DataSource dataSource;

    public TradeDAO(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    public long insert(Trade trade) {
        String sql = "INSERT INTO trades " +
                "(trade_ref, instrument_id, counterparty_id, quantity, price, trade_date, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, trade.getTradeRef());
            ps.setLong(2,   trade.getInstrument().getId());
            ps.setLong(3,   trade.getCounterparty().getId());
            ps.setBigDecimal(4, trade.getQuantity());
            ps.setBigDecimal(5, trade.getPrice());
            ps.setDate(6, Date.valueOf(trade.getTradeDate()));
            ps.setString(7, trade.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new JdbcException("insert returned no generated key");
            }
        } catch (SQLException e) {
            throw new JdbcException("insert failed: " + trade.getTradeRef(), e);
        }
    }

    public Optional<Trade> findByRef(String tradeRef) {
        String sql = "SELECT " + SELECT_COLUMNS + FROM_JOIN + "WHERE t.trade_ref = ? LIMIT 1";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, tradeRef);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new JdbcException("findByRef failed: " + tradeRef, e);
        }
    }

    public List<Trade> findAll() {
        String sql = "SELECT " + SELECT_COLUMNS + FROM_JOIN + "ORDER BY t.trade_date DESC, t.id DESC";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Trade> out = new ArrayList<>();
            while (rs.next()) out.add(mapRow(rs));
            return out;
        } catch (SQLException e) {
            throw new JdbcException("findAll failed", e);
        }
    }

    /**
     * Update a trade's status. Returns the number of rows affected
     * (0 means tradeRef did not exist — caller decides whether to treat that
     * as an error).
     */
    public int updateStatus(String tradeRef, TradeStatus newStatus) {
        String sql = "UPDATE trades SET status = ? WHERE trade_ref = ?";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setString(2, tradeRef);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new JdbcException("updateStatus failed: " + tradeRef, e);
        }
    }

    private static Trade mapRow(ResultSet rs) throws SQLException {
        Instrument instrument = Instrument.builder()
                .symbol(rs.getString("i_symbol")).name(rs.getString("i_name"))
                .assetClass(AssetClass.valueOf(rs.getString("i_asset_class")))
                .currency(rs.getString("i_currency")).isin(rs.getString("i_isin")).build();
        Counterparty counterparty = Counterparty.builder()
                .name(rs.getString("c_name")).leiCode(rs.getString("c_lei_code"))
                .region(rs.getString("c_region")).build();
        return Trade.builder()
                .tradeRef(rs.getString("t_trade_ref"))
                .instrument(instrument).counterparty(counterparty)
                .quantity(rs.getBigDecimal("t_quantity"))
                .price(rs.getBigDecimal("t_price"))
                .tradeDate(rs.getDate("t_trade_date").toLocalDate())
                .status(TradeStatus.valueOf(rs.getString("t_status")))
                .build();
    }
}
```
</details>

**Files to touch:** `repository/TradeDAO.java`.

---

### TICKET-I046 — `ReconResultDAO`

**What**
- A `ReconResultDAO` with `insert(ReconResult) → long`, `findByTradeId(long) → List<ReconResult>`, `findUnresolved() → List<ReconResult>` — same `PreparedStatement` + try-with-resources recipe as `TradeDAO`.

**Why**
- I052's Mockito test on this DAO becomes the regression net for Day 6's `/api/recon/breaks` REST work — every discrepancy the matcher emits must land via `insert(...)`, and the only honest way to assert "it landed" is to verify the DAO call.
- Reinforces that legacy table names (`recon_breaks`) often diverge from current class names (`ReconResult`); students see the mismatch instead of being shielded from it.

**Observe**
- After inserting one `ReconResult` for an existing trade, `findUnresolved().size()` is exactly 1 and the row visible in h2-console / psql has `status = 'OPEN'` and a non-null `detected_at`.
- `findByTradeId(<non-existent-id>)` returns an empty list (NOT null), and no `SQLException` is thrown.

**Acceptance criteria:**
- [ ] `insert(ReconResult)`.
- [ ] `findByTradeId(long tradeId)`.
- [ ] `findUnresolved() → List<ReconResult>`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Same `PreparedStatement` + `try-with-resources` recipe as `TradeDAO`. The
underlying table is `recon_breaks` (legacy class name vs new table name —
get used to that mismatch, real codebases are full of it). `findUnresolved()`
just filters `WHERE status = 'OPEN'`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Constructor: takes a `DataSource`.
- `insert(ReconResult)` — INSERT into `recon_breaks (trade_id, discrepancy_type, status)`,
  ask for generated keys, return the new id.
- `findByTradeId(long)` — SELECT joined to `trades` so you have the `trade_ref`
  for display; `ORDER BY detected_at DESC`.
- `findUnresolved()` — same SELECT, `WHERE rb.status = 'OPEN'`.
- One `mapRow` helper.
</details>

<details>
<summary>Hint 3 — DAO skeleton</summary>

```java
public class ReconResultDAO {
    private final DataSource dataSource;
    public ReconResultDAO(DataSource ds) { this.dataSource = ds; }

    public long insert(ReconResult r) {
        String sql = "INSERT INTO recon_breaks (trade_id, discrepancy_type, status) VALUES (?, ?, ?)";
        // TODO try-with-resources, bind, return generated key
        return -1;
    }

    public List<ReconResult> findByTradeId(long tradeId) { /* TODO */ return List.of(); }
    public List<ReconResult> findUnresolved()            { /* TODO */ return List.of(); }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/repository/ReconResultDAO.java`

```java
package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.*;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.*;

public class ReconResultDAO {

    private static final String SELECT_COLUMNS = """
            SELECT rb.id, rb.discrepancy_type, rb.status, rb.detected_at, rb.resolved_at,
                   t.trade_ref
            FROM recon_breaks rb
            JOIN trades t ON t.id = rb.trade_id
            """;

    private final DataSource dataSource;

    public ReconResultDAO(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    public long insert(ReconResult result) {
        String sql = "INSERT INTO recon_breaks (trade_id, discrepancy_type, status) VALUES (?, ?, ?)";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, result.getTrade().getId());
            ps.setString(2, result.getDiscrepancyType().name());
            ps.setString(3, result.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new JdbcException("insert returned no generated key");
            }
        } catch (SQLException e) {
            throw new JdbcException("ReconResultDAO.insert failed", e);
        }
    }

    public List<ReconResult> findByTradeId(long tradeId) {
        String sql = SELECT_COLUMNS + " WHERE rb.trade_id = ? ORDER BY rb.detected_at DESC";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setLong(1, tradeId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ReconResult> out = new ArrayList<>();
                while (rs.next()) out.add(mapRow(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new JdbcException("findByTradeId failed: " + tradeId, e);
        }
    }

    public List<ReconResult> findUnresolved() {
        String sql = SELECT_COLUMNS + " WHERE rb.status = 'OPEN' ORDER BY rb.detected_at DESC";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<ReconResult> out = new ArrayList<>();
            while (rs.next()) out.add(mapRow(rs));
            return out;
        } catch (SQLException e) {
            throw new JdbcException("findUnresolved failed", e);
        }
    }

    private static ReconResult mapRow(ResultSet rs) throws SQLException {
        Instant resolvedAt = rs.getTimestamp("resolved_at") != null
                ? rs.getTimestamp("resolved_at").toInstant() : null;
        return ReconResult.builder()
                .trade(null)  // Day-5 JPA will hydrate; Day-4 callers use tradeRef directly
                .discrepancyType(DiscrepancyType.valueOf(rs.getString("discrepancy_type")))
                .status(ReconResult.Status.valueOf(rs.getString("status")))
                .detectedAt(rs.getTimestamp("detected_at").toInstant())
                .resolvedAt(resolvedAt)
                .build();
    }
}
```
</details>

**Files to touch:** `repository/ReconResultDAO.java`.

---

### TICKET-I047 — `CounterpartyDAO`

**What**
- A `CounterpartyDAO` with `findAll() → List<Counterparty>` and `findByRegion(String) → List<Counterparty>`; the region is validated against `{APAC, EMEA, NAMR, LATAM}` BEFORE the DB round-trip.

**Why**
- Day 6's region-filter on the dashboard pulls from this DAO; getting validation at the boundary today means the controller doesn't need a duplicate guard tomorrow.
- Smallest of the three DAOs — gives students one clean repetition of the JDBC recipe before Day 5 swaps the whole layer for Spring Data.

**Observe**
- `findAll().size()` against the Day-1 seed returns the seed counterparty count (≥ 5); rows arrive `ORDER BY name`.
- `findByRegion("XYZ")` throws `IllegalArgumentException` with a message listing the four allowed regions; `findByRegion("APAC")` against a region with zero rows returns an empty list, not null.

**Acceptance criteria:**
- [ ] `findAll()`.
- [ ] `findByRegion(String region) → List<Counterparty>`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Simplest DAO of the three. One SELECT, optional WHERE on `region`. Validate
`region` against the four allowed values BEFORE hitting the DB — a wasted
round-trip on a typo is a poor experience and a noisy log.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Declare `private static final Set<String> VALID_REGIONS = Set.of("APAC", "EMEA", "NAMR", "LATAM");`
- `findAll()` → `SELECT name, lei_code, region FROM counterparties ORDER BY name`.
- `findByRegion(region)` → validate, then `... WHERE region = ? ORDER BY name`.
- One `mapRow` helper.
</details>

<details>
<summary>Hint 3 — DAO skeleton</summary>

```java
public class CounterpartyDAO {

    private static final Set<String> VALID_REGIONS = Set.of(/* TODO */);
    private static final String SELECT = "SELECT name, lei_code, region FROM counterparties";

    private final DataSource dataSource;

    public CounterpartyDAO(DataSource ds) { this.dataSource = ds; }

    public List<Counterparty> findAll() { /* TODO */ return List.of(); }
    public List<Counterparty> findByRegion(String region) { /* TODO validate + query */ return List.of(); }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/repository/CounterpartyDAO.java`

```java
package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.Counterparty;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class CounterpartyDAO {

    private static final Set<String> VALID_REGIONS = Set.of("APAC", "EMEA", "NAMR", "LATAM");
    private static final String SELECT = "SELECT name, lei_code, region FROM counterparties";

    private final DataSource dataSource;

    public CounterpartyDAO(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource);
    }

    public List<Counterparty> findAll() {
        String sql = SELECT + " ORDER BY name";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Counterparty> out = new ArrayList<>();
            while (rs.next()) out.add(mapRow(rs));
            return out;
        } catch (SQLException e) {
            throw new JdbcException("CounterpartyDAO.findAll failed", e);
        }
    }

    /**
     * Returns counterparties in the given region (an empty list is a valid
     * result — no rows matched, not an error).
     */
    public List<Counterparty> findByRegion(String region) {
        if (region == null || !VALID_REGIONS.contains(region)) {
            throw new IllegalArgumentException(
                    "region must be one of " + VALID_REGIONS + " (was " + region + ")");
        }
        String sql = SELECT + " WHERE region = ? ORDER BY name";
        try (Connection cx = dataSource.getConnection();
             PreparedStatement ps = cx.prepareStatement(sql)) {
            ps.setString(1, region);
            try (ResultSet rs = ps.executeQuery()) {
                List<Counterparty> out = new ArrayList<>();
                while (rs.next()) out.add(mapRow(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new JdbcException("findByRegion failed: " + region, e);
        }
    }

    private static Counterparty mapRow(ResultSet rs) throws SQLException {
        return Counterparty.builder()
                .name(rs.getString("name"))
                .leiCode(rs.getString("lei_code"))
                .region(rs.getString("region"))
                .build();
    }
}
```
</details>

**Files to touch:** `repository/CounterpartyDAO.java`.

---

### TICKET-I048 — JUnit: all-matched → no discrepancies

**What**
- A JUnit 5 + AssertJ test `matchTrades_allMatched_returnsEmptyDiscrepancies` that feeds 3 internal + 3 identical external trades into `ReconciliationService.matchTrades(...)` and asserts an empty `discrepancies()` collection and `matched().hasSize(3)`.

**Why**
- This is the first regression net for the matcher — every subsequent recon ticket (I049-I052, Day 9 recon engine) can break the happy path silently without it.
- Establishes the `methodUnderTest_scenario_expected` naming convention that the rest of the bootcamp follows; students see the value of consistent test names when JaCoCo (I053) lists 30+ tests.

**Observe**
- `./mvnw -pl backend test -Dtest=ReconciliationServiceTest#matchTrades_allMatched_returnsEmptyDiscrepancies` ends in `Tests run: 1, Failures: 0, Errors: 0`.
- The test is a pure-function test: `grep -n "@SpringBootTest\|@DataJpaTest" ReconciliationServiceTest.java` returns nothing — no Spring context, sub-second run.

**Acceptance criteria:**
- [ ] Test name: `matchTrades_allMatched_returnsEmptyDiscrepancies`.
- [ ] Builds 3 internal + 3 external identical trades; asserts
  `report.discrepancies().isEmpty()`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

`ReconciliationService.matchTrades` is a pure function — no DB, no Spring
context. Build two `List<BaseTrade>` with the same `tradeRef`, same fields,
call it, assert the returned `ReconReport.discrepancies()` is empty and
`matched()` has size 3. AssertJ's fluent matchers (`assertThat(...)`) read
better than JUnit's `assertEquals`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Use a private `equity(String ref)` helper that returns a default
  `EquityTrade` — keeps tests readable.
- Naming: `matchTrades_allMatched_returnsEmptyDiscrepancies` —
  `methodUnderTest_scenario_expected` is the Day-4 convention.
- Assert all four: `discrepancies` empty, `matched` size 3, totals 3 + 3.
- AssertJ import: `import static org.assertj.core.api.Assertions.assertThat;`.
</details>

<details>
<summary>Hint 3 — Test skeleton</summary>

```java
@Test
void matchTrades_allMatched_returnsEmptyDiscrepancies() {
    List<BaseTrade> internal = List.of(equity("TRD-001"), equity("TRD-002"), equity("TRD-003"));
    List<BaseTrade> external = List.of(/* TODO same 3 */);

    ReconReport report = service.matchTrades(internal, external);

    // TODO: 4 assertions
}

private static BaseTrade equity(String ref) { /* TODO */ return null; }
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/test/java/com/dbtraining/tradeflow/service/ReconciliationServiceTest.java`

```java
package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.ReconReport;
import com.dbtraining.tradeflow.model.*;
import com.dbtraining.tradeflow.repository.ReconResultRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock private ReconResultRepository reconResultRepository;
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
    private ReconciliationService service;

    @BeforeEach
    void setUp() {
        service = new ReconciliationService(reconResultRepository, meterRegistry);
    }

    @Test
    void matchTrades_allMatched_returnsEmptyDiscrepancies() {
        List<BaseTrade> internal = List.of(equity("TRD-001"), equity("TRD-002"), equity("TRD-003"));
        List<BaseTrade> external = List.of(equity("TRD-001"), equity("TRD-002"), equity("TRD-003"));

        ReconReport report = service.matchTrades(internal, external);

        assertThat(report.discrepancies()).isEmpty();
        assertThat(report.matched()).hasSize(3);
        assertThat(report.totalInternal()).isEqualTo(3);
        assertThat(report.totalExternal()).isEqualTo(3);
    }

    private static BaseTrade equity(String tradeRef) {
        return EquityTrade.builder()
                .tradeRef(tradeRef).instrumentId(1L).counterpartyId(1L)
                .quantity(new BigDecimal("100")).price(new BigDecimal("245.50"))
                .tradeDate(LocalDate.of(2026, 3, 1))
                .status(TradeStatus.MATCHED)
                .exchange("XETRA").lotSize(100)
                .build();
    }
}
```
</details>

**Files to touch:** `src/test/java/.../service/ReconciliationServiceTest.java`.

---

### TICKET-I049 — JUnit: price mismatch flagged

**What**
- A JUnit test `matchTrades_priceMismatch_flagsDiscrepancy` that feeds one internal trade at `price = 245.50` and the same `tradeRef` externally at `price = 249.99`, then asserts exactly one discrepancy whose `types()` contains `PRICE_MISMATCH`.

**Why**
- I052's JUnit assertions on the matching logic become the regression net for Day 5+ REST work — this is the test that catches a "money compared as `double`" regression at the recon boundary.
- Drives the `BigDecimal` scale-vs-value distinction that resurfaces in Day 7's analytics aggregations.

**Observe**
- The test passes only when the matcher uses `BigDecimal#compareTo` (or `signum`), not `equals` — flip to `equals` in the service and the test still passes with `245.50` vs `249.99` but the sibling scale test (`245.5` vs `245.50`) breaks.
- `assertThat(d.types()).containsExactly(DiscrepancyType.PRICE_MISMATCH)` — `containsExactly`, not `contains`, so a regression that also flags `QUANTITY_MISMATCH` for the same row fails loudly.

**Acceptance criteria:**
- [ ] Test name: `matchTrades_priceMismatch_flagsDiscrepancy`.
- [ ] One pair has different `price`; assert `PRICE_MISMATCH` present.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Same fixture pattern as I048 but build the internal and external trade with
different `price`. The discrepancy contract is: matched is empty,
discrepancies has size 1, and the entry's `types()` contains
`DiscrepancyType.PRICE_MISMATCH`. Beware the `BigDecimal` scale gotcha —
`compareTo` (used by the service), not `equals`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Reuse the `equityWith(ref, qty, price, date)` overload.
- One internal: `price = 245.50`. One external: `price = 249.99`. Same qty,
  same date.
- Assertions: `discrepancies` size 1, the entry's `tradeRef` is `"TRD-001"`,
  its `types()` `containsExactly(PRICE_MISMATCH)`.
</details>

<details>
<summary>Hint 3 — Test skeleton</summary>

```java
@Test
void matchTrades_priceMismatch_flagsDiscrepancy() {
    BaseTrade in  = equityWith("TRD-001", new BigDecimal("100"),
                               new BigDecimal("245.50"), LocalDate.of(2026, 3, 1));
    BaseTrade out = equityWith("TRD-001", new BigDecimal("100"),
                               /* TODO different price */, LocalDate.of(2026, 3, 1));

    ReconReport report = service.matchTrades(List.of(in), List.of(out));

    // TODO: 3 assertions
}

private static BaseTrade equityWith(String ref, BigDecimal qty, BigDecimal price, LocalDate date) {
    /* TODO build EquityTrade */
    return null;
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/test/java/com/dbtraining/tradeflow/service/ReconciliationServiceTest.java` (add to the same class as I048)

```java
@Test
void matchTrades_priceMismatch_flagsDiscrepancy() {
    BaseTrade in  = equityWith("TRD-001", new BigDecimal("100"),
                               new BigDecimal("245.50"), LocalDate.of(2026, 3, 1));
    BaseTrade out = equityWith("TRD-001", new BigDecimal("100"),
                               new BigDecimal("249.99"), LocalDate.of(2026, 3, 1));

    ReconReport report = service.matchTrades(List.of(in), List.of(out));

    assertThat(report.matched()).isEmpty();
    assertThat(report.discrepancies()).hasSize(1);
    Discrepancy d = report.discrepancies().get(0);
    assertThat(d.tradeRef()).isEqualTo("TRD-001");
    assertThat(d.types()).containsExactly(DiscrepancyType.PRICE_MISMATCH);
}

/** Scale-difference regression test: 245.5 vs 245.50 are equal by compareTo. */
@Test
void matchTrades_priceScaleDifference_notFlagged() {
    BaseTrade in  = equityWith("TRD-002", new BigDecimal("100"),
                               new BigDecimal("245.5"),  LocalDate.of(2026, 3, 1));
    BaseTrade out = equityWith("TRD-002", new BigDecimal("100"),
                               new BigDecimal("245.50"), LocalDate.of(2026, 3, 1));

    ReconReport report = service.matchTrades(List.of(in), List.of(out));

    assertThat(report.discrepancies()).isEmpty();
    assertThat(report.matched()).hasSize(1);
}

private static BaseTrade equityWith(String tradeRef, BigDecimal qty, BigDecimal price, LocalDate date) {
    return EquityTrade.builder()
            .tradeRef(tradeRef).instrumentId(1L).counterpartyId(1L)
            .quantity(qty).price(price).tradeDate(date)
            .status(TradeStatus.MATCHED).exchange("XETRA").lotSize(100)
            .build();
}
```
</details>

**Files to touch:** Same test file as I048.

---

### TICKET-I050 — JUnit: missing external trade

**What**
- A JUnit test `matchTrades_missingExternal_flagsMissingTrade` (and the symmetric `missingInternal_*` sibling) that proves an internal trade with no external counterpart — and vice versa — both yield exactly one `MISSING_TRADE` discrepancy carrying the orphaned `tradeRef`.

**Why**
- Day 9's recon engine assumes both legs of the leftover loop are covered; without the sibling test a regression that only walks the internal side passes today and breaks production data tomorrow.
- Forces students to think about the "empty list" edge case that AssertJ makes one-liner cheap — a pattern they reuse across every collection-returning service.

**Observe**
- Both tests pass: `Tests run: 2` from `-Dtest=ReconciliationServiceTest#matchTrades_missing*`.
- `report.discrepancies().get(0).tradeRef()` equals `"TRD-INT-ONLY"` in one test and `"TRD-EXT-ONLY"` in the other — proves the matcher reports the orphan's own ref, not a placeholder.

**Acceptance criteria:**
- [ ] Test name: `matchTrades_missingExternal_flagsMissingTrade`.
- [ ] An internal trade with no external counterpart → `MISSING_TRADE`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Pass a non-empty internal list and an empty external list. The matcher's
contract: every internal trade with no external counterpart yields a
`MISSING_TRADE` discrepancy. Mirror with a sibling test that swaps the
arguments — missing internal also flags `MISSING_TRADE`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Internal: one trade with ref `"TRD-INT-ONLY"`.
- External: `List.of()`.
- Assert: discrepancies size 1, its `tradeRef` is `"TRD-INT-ONLY"`,
  `types().containsExactly(DiscrepancyType.MISSING_TRADE)`.
- Sibling (optional but cheap): same shape with the lists swapped.
</details>

<details>
<summary>Hint 3 — Test skeleton</summary>

```java
@Test
void matchTrades_missingExternal_flagsMissingTrade() {
    List<BaseTrade> internal = List.of(equity("TRD-INT-ONLY"));
    List<BaseTrade> external = List.of();

    ReconReport report = service.matchTrades(internal, external);

    // TODO: 3 assertions
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/test/java/com/dbtraining/tradeflow/service/ReconciliationServiceTest.java` (add both tests to the same class)

```java
@Test
void matchTrades_missingExternal_flagsMissingTrade() {
    List<BaseTrade> internal = List.of(equity("TRD-INT-ONLY"));
    List<BaseTrade> external = List.of();

    ReconReport report = service.matchTrades(internal, external);

    assertThat(report.discrepancies()).hasSize(1);
    assertThat(report.discrepancies().get(0).tradeRef()).isEqualTo("TRD-INT-ONLY");
    assertThat(report.discrepancies().get(0).types())
            .containsExactly(DiscrepancyType.MISSING_TRADE);
}

@Test
void matchTrades_missingInternal_flagsMissingTrade() {
    List<BaseTrade> internal = List.of();
    List<BaseTrade> external = List.of(equity("TRD-EXT-ONLY"));

    ReconReport report = service.matchTrades(internal, external);

    assertThat(report.discrepancies()).hasSize(1);
    assertThat(report.discrepancies().get(0).tradeRef()).isEqualTo("TRD-EXT-ONLY");
    assertThat(report.discrepancies().get(0).types())
            .containsExactly(DiscrepancyType.MISSING_TRADE);
}
```
</details>

**Files to touch:** Same test file as I048.

---

### TICKET-I051 — Mockito: mock `TradeDAO`

**What**
- A Mockito test that mocks `TradeDAO`, stubs `findAll()` with a fixed sample, drives the recon orchestrator once, then `verify(tradeDAO, times(1)).findAll()`.

**Why**
- Day 6's `@WebMvcTest` will use `@MockBean` (the Spring sibling of `@Mock`); learning plain Mockito here means the only new concept tomorrow is the Spring wiring.
- Establishes the "test in isolation" discipline — the service test must not need a live Postgres, so Day 4's CI stays fast (sub-second) even as the test count grows.

**Observe**
- The test class has `@ExtendWith(MockitoExtension.class)` at the top; missing that annotation surfaces as `NullPointerException` on the first `when(...)` call.
- `verify(tradeDAO, times(1)).findAll()` passes; calling `findAll()` twice from the orchestrator fails with `Wanted 1 time, but was 2 times`.

**Acceptance criteria:**
- [ ] `TradeDAO` mocked with `@Mock` + `MockitoExtension`.
- [ ] `when(tradeDAO.findAll()).thenReturn(sample)`.
- [ ] `verify(tradeDAO).findAll()` is called exactly once.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

`@ExtendWith(MockitoExtension.class)` + `@Mock private TradeDAO tradeDAO;` is
the whole setup. Stub the method you care about with
`when(...).thenReturn(...)`, exercise the code under test, then
`verify(...)` to confirm interactions. Don't mock data classes (`Trade`,
`ReconResult`) — only collaborators.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Class annotation: `@ExtendWith(MockitoExtension.class)`.
- Fields: `@Mock private TradeDAO tradeDAO;` (plus any other collaborators).
- Construct the service in `@BeforeEach` with the mocks.
- Stub: `when(tradeDAO.findAll()).thenReturn(List.of(sampleTrade()));`.
- Verify after the call: `verify(tradeDAO, times(1)).findAll();`.
- Use the `Mockito.times(...)` overload (instead of bare `verify`) so the
  intent is explicit.
</details>

<details>
<summary>Hint 3 — Test skeleton</summary>

```java
@ExtendWith(MockitoExtension.class)
class ReconciliationServiceMockTest {

    @Mock private TradeDAO tradeDAO;
    @Mock private ReconResultDAO reconResultDAO;

    private /* TODO service under test */ service;

    @BeforeEach
    void setUp() {
        // TODO wire the service with the mocks
    }

    @Test
    void runForAll_callsFindAllOnce() {
        when(tradeDAO.findAll()).thenReturn(List.of(/* TODO sample */));

        // TODO call service method

        verify(tradeDAO, times(1)).findAll();
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/test/java/com/dbtraining/tradeflow/service/ReconciliationServiceTest.java` (add this test to the same class; or create a sibling `ReconciliationServiceMockTest.java` if you prefer to keep interaction tests separate)

```java
package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.model.*;
import com.dbtraining.tradeflow.repository.ReconResultDAO;
import com.dbtraining.tradeflow.repository.TradeDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReconciliationServiceMockTest {

    @Mock private TradeDAO tradeDAO;
    @Mock private ReconResultDAO reconResultDAO;

    private ReconciliationOrchestrator service;

    @BeforeEach
    void setUp() {
        service = new ReconciliationOrchestrator(tradeDAO, reconResultDAO);
    }

    @Test
    void runForAll_callsFindAllExactlyOnce() {
        List<Trade> sample = List.of(sampleTrade("TRD-1"));
        when(tradeDAO.findAll()).thenReturn(sample);

        ReconSummary summary = service.runForAll();

        verify(tradeDAO, times(1)).findAll();
        assertThat(summary.totalTrades()).isEqualTo(1);
        // Happy-path: no discrepancies persisted because everything matched.
        verifyNoInteractions(reconResultDAO);
    }

    private static Trade sampleTrade(String ref) {
        return Trade.builder()
                .tradeRef(ref)
                .quantity(new BigDecimal("100")).price(new BigDecimal("245.50"))
                .tradeDate(LocalDate.of(2026, 3, 1))
                .status(TradeStatus.MATCHED)
                .build();
    }
}
```
</details>

**Files to touch:** Same test file (or a new sibling test).

---

### TICKET-I052 — Mockito: verify `ReconResultDAO.insert()`

**What**
- A Mockito test that arranges one internal-only trade so the matcher emits a `MISSING_TRADE` discrepancy, then uses `ArgumentCaptor<ReconResult>` + `verify(reconResultDAO, times(1)).insert(captor.capture())` and asserts the captured row's `discrepancyType == MISSING_TRADE` and `status == OPEN`.

**Why**
- This is the assertion shape Day 6's recon REST controller, Day 9's recon engine, and Day 10's end-to-end smoke all reuse — capturing the side effect (not just verifying the call) is the only honest way to test "we persisted the right thing".
- Pair it with the negative-path test (`verify(..., never()).insert(any())` on an all-matched input) and you have a regression net that catches the two most common recon bugs: silent drops and over-eager persistence.

**Observe**
- `captor.getValue().getDiscrepancyType()` is exactly `MISSING_TRADE`; `captor.getValue().getStatus()` is exactly `ReconResult.Status.OPEN`.
- `verify(reconResultDAO, never()).insert(any())` on the all-matched fixture passes — a regression that wrongly persists matched trades surfaces as `Never wanted but invoked`.

**Acceptance criteria:**
- [ ] Test that for each discrepancy, `reconResultDAO.insert(...)` is invoked.
- [ ] Use `ArgumentCaptor<ReconResult>` to assert the captured values.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

`ArgumentCaptor` records every argument a mock was called with so you can
inspect them after the fact. Combine it with `verify(...,
times(N)).insert(captor.capture())` to confirm both the count and the
content of the side effect.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Stub two internal trades, one matching external (so 1 discrepancy).
- `ArgumentCaptor<ReconResult> captor = ArgumentCaptor.forClass(ReconResult.class);`
- `verify(reconResultDAO, times(1)).insert(captor.capture());`
- Then `ReconResult captured = captor.getValue();` and assert its
  `discrepancyType` is what you expect.
- For multiple captures: `captor.getAllValues()` returns the full list in
  invocation order.
</details>

<details>
<summary>Hint 3 — Test skeleton</summary>

```java
@Test
void runForAll_oneDiscrepancy_insertsOneReconResult() {
    // arrange: 1 internal-only trade so the matcher reports MISSING_TRADE
    when(tradeDAO.findAll()).thenReturn(List.of(/* TODO */));

    // act
    service.runForAll();

    // assert
    ArgumentCaptor<ReconResult> captor = ArgumentCaptor.forClass(ReconResult.class);
    verify(reconResultDAO, /* TODO times */).insert(captor.capture());
    ReconResult inserted = captor.getValue();
    // TODO: assert discrepancyType
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/test/java/com/dbtraining/tradeflow/service/ReconciliationServiceTest.java` (or `ReconciliationServiceMockTest.java` — same class as I051)

```java
import com.dbtraining.tradeflow.model.*;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Test
void runForAll_oneDiscrepancy_insertsOneReconResult() {
    Trade internalOnly = sampleTrade("TRD-INT-ONLY");
    when(tradeDAO.findAll()).thenReturn(List.of(internalOnly));
    // External feed is empty in this test — we expect 1 MISSING_TRADE discrepancy.

    service.runForAll();

    ArgumentCaptor<ReconResult> captor = ArgumentCaptor.forClass(ReconResult.class);
    verify(reconResultDAO, times(1)).insert(captor.capture());
    ReconResult inserted = captor.getValue();

    assertThat(inserted.getDiscrepancyType())
            .isEqualTo(DiscrepancyType.MISSING_TRADE);
    assertThat(inserted.getStatus())
            .isEqualTo(ReconResult.Status.OPEN);
}

@Test
void runForAll_allMatched_neverCallsInsert() {
    Trade matched = sampleTrade("TRD-1");
    when(tradeDAO.findAll()).thenReturn(List.of(matched));
    // External feed (stubbed elsewhere) returns the same trade — nothing to flag.

    service.runForAll();

    verify(reconResultDAO, never()).insert(any(ReconResult.class));
}
```
</details>

**Files to touch:** Same test file.

---

### TICKET-I053 — Coverage > 70%

**What**
- Wire the `jacoco-maven-plugin` into `backend/pom.xml`, run `./mvnw test jacoco:report`, open `target/site/jacoco/index.html`, and add focused tests until `com.dbtraining.tradeflow.service` LINE coverage is ≥ 70%.

**Why**
- Day 10's CI gate enforces this threshold as a build break — landing the plugin and crossing 70% today means tomorrow's PRs don't get blocked at the finish line.
- The act of opening the HTML and reading red lines teaches students which branches the existing tests miss; this skill recurs every time someone adds a new error path.

**Observe**
- `backend/target/site/jacoco/index.html` exists after `./mvnw -pl backend test jacoco:report`; the `com.dbtraining.tradeflow.service` row shows a green LINE bar at ≥ 70%.
- `./mvnw -pl backend verify` (with the `<check>` execution wired) exits 0; lowering the threshold to e.g. 0.90 makes it fail with `Rule violated for package ... line covered ratio is 0.7x`.

**Acceptance criteria:**
- [ ] `mvn test jacoco:report` produces `target/site/jacoco/index.html`.
- [ ] Line coverage on `service/*` ≥ 70%.
- [ ] Report committed (or screenshot pasted in PR).

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

JaCoCo wires into Maven via the `jacoco-maven-plugin`. Run it once, open
the HTML report, look at the red lines for `service/*`, write one
focused test per gap. Don't chase 100% — chase the lines that contain
business decisions (if-branches, validation, error handling). DAO layer
needs real DB or Testcontainers — that's Day 5, leave it under-covered
today.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Add the plugin to `backend/pom.xml` if it's not there already (it's
  usually in the starter — check `<build><plugins>`).
- `./mvnw test jacoco:report` (or `verify` if you bind it to the verify
  phase).
- Open `backend/target/site/jacoco/index.html` in a browser.
- Drill into `com.dbtraining.tradeflow.service` — that's the package the
  ticket targets.
- Sort by "Missed Lines" descending; write one test per top entry until
  you cross 70%.
- Commit either the report folder (cheap) or a screenshot pasted into your
  PR description (cleaner).
</details>

<details>
<summary>Hint 3 — Plugin snippet</summary>

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <!-- TODO: optional <check> execution to fail the build if coverage < 70% -->
    </executions>
</plugin>
```
</details>

<details>
<summary>Reference — full walkthrough</summary>

**File to edit:** `backend/pom.xml` (add the plugin block inside `<build><plugins>`)

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <includes>
                            <include>com.dbtraining.tradeflow.service.*</include>
                        </includes>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Then run, from `backend/`:**

```bash
# Build coverage data + HTML report
./mvnw test jacoco:report

# Open the report (macOS / Linux)
open target/site/jacoco/index.html

# Enforce the 70% rule — non-zero exit if service/* is below threshold
./mvnw verify
```

Drill into `com.dbtraining.tradeflow.service` in the HTML, sort by "Missed Lines" descending, and add one focused test per top red method until the LINE bar crosses 70%. Common quick wins: a `null`-input guard test, an empty-list test, and a terminal-status-guard test on the matcher.
</details>

**Files to touch:** `backend/pom.xml` + one or two new `*Test.java` files in
`service/`.

---

## Run and Observe — End of Sprint 3 (Collections, JDBC & Tests)

You've shipped 13 tickets (I041–I053): `TradeService` is HashMap + Streams
based, three JDBC DAOs hit real Postgres through HikariCP, and JUnit +
Mockito + JaCoCo cover the service layer. Before the instructor checkpoint,
prove it to yourself in two passes — first the test suite, then a JDBC
smoke against the running database.

**Run:**

> **Terminal #1** — from `tradeflow-studentscopy/backend/` — make sure the
> Day-1 Postgres container is up first (`docker compose up -d postgres` from
> the repo root) so the JDBC DAOs have something to connect to.

```bash
# Full test suite + JaCoCo report in one shot
./mvnw verify

# Or just the test phase while iterating
./mvnw test

# Single test class while debugging
./mvnw test -Dtest=ReconciliationServiceTest
./mvnw test -Dtest=ReconciliationServiceMockTest
```

**Observe:**

| Check | Expected after Sprint 3 |
|---|---|
| `./mvnw test` final line | `BUILD SUCCESS`, `Tests run: 6+, Failures: 0, Errors: 0, Skipped: 0` (5 recon-service tests from I048–I052 + at least one extra coverage test from I053) |
| `ReconciliationServiceTest#matchTrades_allMatched_returnsEmptyDiscrepancies` | Passes — proves the happy-path matcher returns empty discrepancies + 3 matched |
| `ReconciliationServiceTest#matchTrades_priceMismatch_flagsDiscrepancy` | Passes — proves `PRICE_MISMATCH` is emitted with the right `tradeRef` |
| `ReconciliationServiceMockTest` — `verify(tradeDAO, times(1)).findAll()` | No `Wanted but not invoked` — confirms the orchestrator drives the DAO exactly once per run |
| `backend/target/site/jacoco/index.html` → `com.dbtraining.tradeflow.service` | LINE coverage ≥ 70% (green bar at top of the package row) |
| JDBC smoke from a scratch `main()` calling `TradeDAO.insert(...)` then `findByRef(...)` against the live Postgres | Round-trip returns the same `tradeRef`; pool log line `tradeflow-jdbc - Start completed` appears once at startup (HikariCP O(1) lookup via the HashMap keyed by `tradeRef` also proves I041) |

**Negative tests — prove your mocks and DAOs actually work:**

```java
// 1. Mockito: verify NO insert when everything matches (I052 stretch)
//    If the orchestrator wrongly persists matched trades as discrepancies,
//    this fails with "Never wanted but invoked" — exactly what we want.
@Test
void runForAll_allMatched_neverCallsInsert() {
    when(tradeDAO.findAll()).thenReturn(List.of(sampleTrade("TRD-1")));
    // external feed (stubbed elsewhere) returns the same trade

    service.runForAll();

    verify(reconResultDAO, never()).insert(any(ReconResult.class));
}

// 2. DAO: PreparedStatement binding rejects SQL injection attempt
//    findByRef must treat the input as a parameter, not concatenated SQL.
//    If a student concatenated the ref into the SQL string, this returns
//    rows or throws a syntax error; with PreparedStatement it returns empty.
Optional<Trade> result = tradeDAO.findByRef("TRD-001' OR '1'='1");
assertThat(result).isEmpty();
```

**If something looks wrong:**
- `Connection refused` on `localhost:5432` → Postgres container isn't up; `docker compose ps` from the repo root, then `docker compose up -d postgres`.
- `BUILD FAILURE ... no JDBC_URL` or auth errors → env vars not exported; re-check the `JDBC_URL` / `POSTGRES_USER` / `POSTGRES_PASSWORD` defaults in `DatabaseConfig` (I044).
- `Wanted but not invoked: tradeDAO.findAll()` in Mockito tests → the orchestrator (I051) isn't wired to the DAO yet; revisit the Reference Solution for I051.
- JaCoCo says `< 70%` on `service/*` → open `target/site/jacoco/index.html`, sort by missed lines, write one targeted test per top red method (I053's Reference walkthrough covers it).
- `BigDecimal` equality test fails despite same value → you used `.equals()` instead of `.compareTo() == 0`; scale matters (`245.5` vs `245.50`).

---

**Instructor checkpoint:** Before you move to Day 5, get the instructor to
review the JaCoCo report and the JDBC round-trip output.

---

## End-of-day checklist

- [ ] 13 tickets merged.
- [ ] `mvn test` green.
- [ ] JaCoCo report ≥ 70% on service layer.
- [ ] Connection to local Postgres works (verify with one CRUD round-trip via main()).

Next: [Day 5 — Spring Boot foundations](../day5/README.md)
