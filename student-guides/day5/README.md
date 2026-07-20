# Day 5 — Spring Boot Foundations: REST Backend Skeleton

> Theme: **Bootstrap the Spring Boot app, JPA entities, Swagger.**
> Tickets: **I054 – I067** (14 tickets)
> Module: Spring Boot — Modules 1 & 2

By end of day:

- A Spring Boot app boots on `localhost:8080` with `dev` / `uat` / `prod` profiles.
- JPA entities replace the Day-4 JDBC DAOs.
- Swagger UI lists every endpoint your team will build on Day 6.
- Liquibase runs automatically at startup.

---

## Sprint 4 — Spring Boot Backend

### TICKET-I054 — Generate Spring Boot project

**What**
- A `backend/pom.xml` with the Web, Data JPA, Validation, Actuator, Postgres, H2, Liquibase, and Springdoc starters wired in.

**Why**
- Every subsequent day depends on this jar booting — Days 6-10 assume `./mvnw spring-boot:run` works on every laptop.
- The dependency list freezes the contract: H2 today, the same Postgres driver Day 7's UAT uses tomorrow.

**Observe**
- `./mvnw clean compile` prints `BUILD SUCCESS`.
- `./mvnw spring-boot:run` boots and `curl http://localhost:8080/actuator/health` returns `{"status":"UP"}`.

**Acceptance criteria:**
- [ ] Java 17, Maven, packaging JAR.
- [ ] Starters: Web, Data JPA, PostgreSQL Driver, H2, Actuator, Validation, Liquibase.
- [ ] `./mvnw spring-boot:run` boots; `/actuator/health` returns `{"status":"UP"}`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

The starter `pom.xml` is already in the repo — your job is to verify it has the
right starters and uncomment what is missing. Spring Initializr (`start.spring.io`)
is the canonical generator if you want to compare; the version-aligned starter
parent is `spring-boot-starter-parent` 3.2.x with `java.version` 17.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. Open `backend/pom.xml`. Check `<parent>` is `spring-boot-starter-parent` 3.2.x
   and `<java.version>` is `17`.
2. Confirm these starters are present under `<dependencies>`:
   `spring-boot-starter-web`, `spring-boot-starter-data-jpa`,
   `spring-boot-starter-validation`, `spring-boot-starter-actuator`,
   `org.postgresql:postgresql` (runtime), `com.h2database:h2` (runtime),
   `liquibase-core`.
3. Add `springdoc-openapi-starter-webmvc-ui` here too (used by I063).
4. `./mvnw clean compile` should print `BUILD SUCCESS`.
5. `./mvnw spring-boot:run` boots; curl `/actuator/health`.
</details>

<details>
<summary>Hint 3 — pom.xml skeleton</summary>

```xml
<project>
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.4</version>
  </parent>

  <groupId>com.dbtraining</groupId>
  <artifactId>tradeflow</artifactId>
  <version>0.1.0-SNAPSHOT</version>

  <properties>
    <java.version>17</java.version>
    <springdoc.version>2.3.0</springdoc.version>
  </properties>

  <dependencies>
    <!-- TODO: spring-boot-starter-web -->
    <!-- TODO: spring-boot-starter-data-jpa -->
    <!-- TODO: spring-boot-starter-validation -->
    <!-- TODO: spring-boot-starter-actuator -->
    <!-- TODO: postgresql (runtime) + h2 (runtime) -->
    <!-- TODO: liquibase-core -->
    <!-- TODO: springdoc-openapi-starter-webmvc-ui -->
  </dependencies>
</project>
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.4</version>
    <relativePath/>
  </parent>

  <groupId>com.dbtraining</groupId>
  <artifactId>tradeflow</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <name>tradeflow</name>

  <properties>
    <java.version>17</java.version>
    <springdoc.version>2.3.0</springdoc.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>runtime</scope>
    </dependency>
    <dependency>
      <groupId>org.liquibase</groupId>
      <artifactId>liquibase-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
      <version>${springdoc.version}</version>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```
</details>

**Files to touch:** `backend/pom.xml`.

---

### TICKET-I055 — Multi-profile configuration

**What**
- `application.yml` + `application-dev.yml` + `application-uat.yml` + `application-prod.yml` selecting H2 / Postgres / Postgres+env-vars respectively.

**Why**
- Day 7's deploy script flips `SPRING_PROFILES_ACTIVE=uat`; if the profile split is wrong here the deploy fails silently.
- Teaches that the same jar runs in three environments — no per-env rebuild, the Twelve-Factor lesson.

**Observe**
- Default boot logs `The following 1 profile is active: "dev"`.
- `SPRING_PROFILES_ACTIVE=uat ./mvnw spring-boot:run` logs `HikariPool-1 - Added connection` against Postgres.

**Acceptance criteria:**
- [ ] `application.yml` — common settings, `spring.profiles.active: dev` by default.
- [ ] `application-dev.yml` — H2 in-memory, `ddl-auto: none` (Liquibase owns the schema).
- [ ] `application-uat.yml` — PostgreSQL on `localhost:5432`.
- [ ] `application-prod.yml` — PostgreSQL with env-var-driven config.
- [ ] `SPRING_PROFILES_ACTIVE=uat ./mvnw spring-boot:run` connects to Postgres.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Spring loads `application.yml` first, then overlays the active profile's
`application-<profile>.yml` on top. Keep shared bits (Liquibase changelog, JPA
defaults, Actuator) in the base file; let each profile redefine only its
datasource + log levels. Never hard-code real passwords — use
`${POSTGRES_PASSWORD}` placeholders.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. `application.yml` — `spring.application.name`, `spring.profiles.active: dev`,
   `spring.liquibase.change-log`, `spring.jpa.hibernate.ddl-auto: none`, Actuator
   exposure.
2. `application-dev.yml` — `jdbc:h2:mem:tradeflow;MODE=PostgreSQL`, H2 console on,
   DEBUG logging.
3. `application-uat.yml` — PostgreSQL JDBC URL with `${POSTGRES_HOST:localhost}`
   defaults so it falls back to local, Hikari pool tuned.
4. `application-prod.yml` — same Postgres shape but **no defaults** (envs are
   required), `root: WARN` logging, Actuator narrowed to `health, prometheus`.
5. Test all three with `SPRING_PROFILES_ACTIVE=<name> ./mvnw spring-boot:run`.
</details>

<details>
<summary>Hint 3 — YAML config skeleton</summary>

```yaml
# application.yml
spring:
  application:
    name: tradeflow
  profiles:
    active: dev
  liquibase:
    enabled: true
    change-log: db/changelog/db.changelog-master.xml  # no classpath: prefix
  jpa:
    hibernate:
      ddl-auto: none
    open-in-view: false

management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
```

```yaml
# application-dev.yml — TODO: H2 datasource + h2 console
# application-uat.yml — TODO: postgres datasource with localhost defaults
# application-prod.yml — TODO: postgres datasource, env vars only
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit:** `backend/src/main/resources/application.yml`, `application-dev.yml`, `application-uat.yml`, `application-prod.yml`

```yaml
# application.yml
spring:
  application:
    name: tradeflow
  profiles:
    active: dev
  liquibase:
    enabled: true
    change-log: db/changelog/db.changelog-master.xml
  jpa:
    hibernate:
      ddl-auto: none
    open-in-view: false
    properties:
      hibernate:
        format_sql: true

management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus

springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
```

```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:h2:mem:tradeflow;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  h2:
    console:
      enabled: true
      path: /h2-console
  sql:
    init:
      mode: embedded

logging:
  level:
    com.dbtraining.tradeflow: DEBUG
    org.hibernate.SQL: DEBUG
```

```yaml
# application-uat.yml
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:tradeflow}
    username: ${POSTGRES_USER:tradeflow_user}
    password: ${POSTGRES_PASSWORD:changeme}
    hikari:
      maximum-pool-size: 10
      connection-timeout: 5000

logging:
  level:
    com.dbtraining.tradeflow: INFO
    org.hibernate.SQL: WARN
```

```yaml
# application-prod.yml — no defaults for any env var (fail-fast)
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT:5432}/${POSTGRES_DB}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
    hikari:
      maximum-pool-size: 20
      connection-timeout: 3000

management:
  endpoints:
    web:
      exposure:
        include: health, prometheus

logging:
  level:
    root: WARN
    com.dbtraining.tradeflow: INFO
```
</details>

**Files to touch:** `backend/src/main/resources/application*.yml`.

---

### TICKET-I056 — `@Entity Trade`

**What**
- A `@Entity Trade` mapped to the `trades` table with `@ManyToOne` links to `Instrument` and `Counterparty`.

**Why**
- Day 4's hand-rolled JDBC `TradeDao` is replaced today by a JPA-managed entity — students should feel the line-count drop and the `ResultSet → POJO` mapping evaporate.
- Day 6's `TradeController.create()` will `tradeRepository.save(trade)` — that one-liner only works if the entity wiring here is right.

**Observe**
- Boot log shows Hibernate `create table trades` (dev profile) or no DDL on uat (Liquibase owns it).
- `select * from trades` in H2 console returns rows after `data.sql` runs.

**Acceptance criteria:**
- [ ] `@Entity @Table(name="trades")`.
- [ ] `@Id @GeneratedValue` on `id`.
- [ ] `@ManyToOne` to `Instrument` and `Counterparty` (eager fetch is fine for now).
- [ ] `@Enumerated(EnumType.STRING)` for `status`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

JPA needs a no-arg constructor — add a `protected Trade() {}` so the public
Builder remains the only path callers use. Mark `@ManyToOne(fetch = LAZY)` —
eager fetching on every Trade load is what kills the Day-7 N+1 perf lab.
Keep equality on `tradeRef` (business key), not `id` (database surrogate).
</details>

<details>
<summary>Hint 2 — More guided</summary>

- `@Entity @Table(name = "trades")` at the class level.
- `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` on `id` (matches
  Postgres `BIGSERIAL` and H2 autoincrement).
- `@Column(name = "trade_ref", nullable = false, unique = true, length = 30)`.
- `@ManyToOne(fetch = FetchType.LAZY, optional = false)` + `@JoinColumn(name = "instrument_id")`. Same for counterparty.
- `BigDecimal quantity` / `price` with `precision = 18, scale = 4`.
- `@Enumerated(EnumType.STRING)` on `status` — stores readable strings, not ordinals.
- Keep your Builder; just add the JPA annotations on top.
</details>

<details>
<summary>Hint 3 — Entity skeleton</summary>

```java
@Entity
@Table(name = "trades")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: trade_ref VARCHAR(30) NOT NULL UNIQUE
    // TODO: @ManyToOne LAZY to Instrument with @JoinColumn(name="instrument_id")
    // TODO: @ManyToOne LAZY to Counterparty with @JoinColumn(name="counterparty_id")
    // TODO: BigDecimal quantity, price (precision=18, scale=4, NOT NULL)
    // TODO: LocalDate tradeDate NOT NULL @Column(name="trade_date")
    // TODO: @Enumerated(STRING) TradeStatus status NOT NULL
    // TODO: Instant createdAt NOT NULL updatable=false @Column(name="created_at")

    /** JPA no-arg constructor. */
    protected Trade() {}

    // Builder stays as in Day 2 — just keep it.
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/model/Trade.java`

```java
package com.dbtraining.tradeflow.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "trades")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_ref", nullable = false, unique = true, length = 30)
    private String tradeRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id")
    private Instrument instrument;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "counterparty_id")
    private Counterparty counterparty;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal price;

    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TradeStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** No-arg constructor required by JPA. */
    protected Trade() {}

    private Trade(Builder b) {
        this.tradeRef     = b.tradeRef;
        this.instrument   = b.instrument;
        this.counterparty = b.counterparty;
        this.quantity     = b.quantity;
        this.price        = b.price;
        this.tradeDate    = b.tradeDate;
        this.status       = b.status != null ? b.status : TradeStatus.PENDING;
        this.createdAt    = b.createdAt != null ? b.createdAt : Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String tradeRef;
        private Instrument instrument;
        private Counterparty counterparty;
        private BigDecimal quantity;
        private BigDecimal price;
        private LocalDate tradeDate;
        private TradeStatus status;
        private Instant createdAt;

        public Builder tradeRef(String v)          { this.tradeRef = v; return this; }
        public Builder instrument(Instrument v)    { this.instrument = v; return this; }
        public Builder counterparty(Counterparty v){ this.counterparty = v; return this; }
        public Builder quantity(BigDecimal v)      { this.quantity = v; return this; }
        public Builder price(BigDecimal v)         { this.price = v; return this; }
        public Builder tradeDate(LocalDate v)      { this.tradeDate = v; return this; }
        public Builder status(TradeStatus v)       { this.status = v; return this; }
        public Builder createdAt(Instant v)        { this.createdAt = v; return this; }

        public Trade build() { return new Trade(this); }
    }

    public Long getId()                  { return id; }
    public String getTradeRef()          { return tradeRef; }
    public Instrument getInstrument()    { return instrument; }
    public Counterparty getCounterparty(){ return counterparty; }
    public BigDecimal getQuantity()      { return quantity; }
    public BigDecimal getPrice()         { return price; }
    public LocalDate getTradeDate()      { return tradeDate; }
    public TradeStatus getStatus()       { return status; }
    public Instant getCreatedAt()        { return createdAt; }

    public void setStatus(TradeStatus status) { this.status = status; }

    public BigDecimal getNotional() {
        return quantity == null || price == null ? null : quantity.multiply(price);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Trade other)) return false;
        return Objects.equals(tradeRef, other.tradeRef);
    }

    @Override
    public int hashCode() { return Objects.hash(tradeRef); }
}
```
</details>

**Files to touch:** `model/Trade.java`.

---

### TICKET-I057 — `@Entity Counterparty`, `@Entity Instrument`

**What**
- Two JPA entities `Counterparty` (LEI-keyed) and `Instrument` (symbol-keyed) mapped to their Day-1 tables.

**Why**
- Trade has `@ManyToOne` to both — without these entities the I056 wiring won't compile.
- `unique=true` on `leiCode` and `symbol` enforces the business invariant the Day-1 schema already encodes, so JPA and the DB agree.

**Observe**
- Hibernate startup prints `add unique key` (dev) or matches the Liquibase changelog (uat) without complaint.
- `counterpartyRepository.findByLeiCode("DEUTDEFF")` returns the seeded row.
- [ ] Both annotated with JPA `@Entity` + `@Table`.
- [ ] Both have `id` PK; `Counterparty.leiCode` and `Instrument.symbol` `@Column(unique=true)`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Same pattern as Trade — protected no-arg constructor, `@Id @GeneratedValue`,
keep your Builder. Both classes carry a business key (`leiCode` for
Counterparty, `symbol` for Instrument) — that's what equals/hashCode use.
`Instrument.isin` is nullable (FX / commodities have no ISIN) but still unique
when set.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- `Counterparty`: `@Table(name = "counterparties")`. Columns `name`,
  `lei_code` (20 chars, NOT NULL UNIQUE), `region` (10 chars).
- `Instrument`: `@Table(name = "instruments")`. Columns `symbol` (20 chars
  UNIQUE), `name` (200), `asset_class` (`@Enumerated(STRING)`), `currency` (3),
  `isin` (12, nullable, unique).
- Keep your Builders. Add `protected Counterparty() {}` and `protected Instrument() {}`.
- equals on `leiCode` / `symbol`, not on `id`.
</details>

<details>
<summary>Hint 3 — Entity skeletons</summary>

```java
@Entity
@Table(name = "counterparties")
public class Counterparty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "lei_code", nullable = false, unique = true, length = 20)
    private String leiCode;

    // TODO: region (length 10, NOT NULL)
    // TODO: protected no-arg ctor + Builder (paste from Day 2)
    // TODO: equals/hashCode on leiCode
}

@Entity
@Table(name = "instruments")
public class Instrument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;
    // TODO: name (length 200, NOT NULL)
    // TODO: @Enumerated(STRING) assetClass with @Column(name="asset_class")
    // TODO: currency (length 3, NOT NULL)
    // TODO: isin (length 12, unique, NULLABLE)
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit:** `backend/src/main/java/com/dbtraining/tradeflow/model/Counterparty.java`, `backend/src/main/java/com/dbtraining/tradeflow/model/Instrument.java`

```java
// Counterparty.java
package com.dbtraining.tradeflow.model;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "counterparties")
public class Counterparty {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "lei_code", nullable = false, unique = true, length = 20)
    private String leiCode;

    @Column(nullable = false, length = 10)
    private String region;

    protected Counterparty() {}

    private Counterparty(Builder b) {
        this.name    = b.name;
        this.leiCode = b.leiCode;
        this.region  = b.region;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String name;
        private String leiCode;
        private String region;

        public Builder name(String v)    { this.name = v; return this; }
        public Builder leiCode(String v) { this.leiCode = v; return this; }
        public Builder region(String v)  { this.region = v; return this; }

        public Counterparty build() { return new Counterparty(this); }
    }

    public Long getId()        { return id; }
    public String getName()    { return name; }
    public String getLeiCode() { return leiCode; }
    public String getRegion()  { return region; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Counterparty other)) return false;
        return Objects.equals(leiCode, other.leiCode);
    }
    @Override public int hashCode() { return Objects.hash(leiCode); }
}
```

```java
// Instrument.java
package com.dbtraining.tradeflow.model;

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "instruments")
public class Instrument {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_class", nullable = false, length = 20)
    private AssetClass assetClass;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(unique = true, length = 12)
    private String isin;   // nullable — FX/commodities have no ISIN

    protected Instrument() {}

    private Instrument(Builder b) {
        this.symbol     = b.symbol;
        this.name       = b.name;
        this.assetClass = b.assetClass;
        this.currency   = b.currency;
        this.isin       = b.isin;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String symbol;
        private String name;
        private AssetClass assetClass;
        private String currency;
        private String isin;

        public Builder symbol(String v)         { this.symbol = v; return this; }
        public Builder name(String v)           { this.name = v; return this; }
        public Builder assetClass(AssetClass v) { this.assetClass = v; return this; }
        public Builder currency(String v)       { this.currency = v; return this; }
        public Builder isin(String v)           { this.isin = v; return this; }

        public Instrument build() { return new Instrument(this); }
    }

    public Long getId()              { return id; }
    public String getSymbol()        { return symbol; }
    public String getName()          { return name; }
    public AssetClass getAssetClass(){ return assetClass; }
    public String getCurrency()      { return currency; }
    public String getIsin()          { return isin; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Instrument other)) return false;
        return Objects.equals(symbol, other.symbol);
    }
    @Override public int hashCode() { return Objects.hash(symbol); }
}
```
</details>

**Files to touch:** `model/Counterparty.java`, `model/Instrument.java`.

---

### TICKET-I058 — `@Entity ReconResult`

**What**
- An entity `ReconResult` mapped to the `recon_breaks` table with `@ManyToOne` to `Trade` and a nullable `resolvedAt`.

**Why**
- I062's `TradeService.reconcileTrades()` will persist these rows; without the entity that service can't compile.
- Day-1 chose the table name `recon_breaks`; mapping the Java class `ReconResult` to it teaches that entity name and table name are independent.

**Observe**
- Hibernate logs `Hibernate: insert into recon_breaks` when a break is saved.
- A row with `resolved_at = null` persists without constraint failure.
- [ ] `@ManyToOne` to `Trade`.
- [ ] `@Enumerated(EnumType.STRING)` for `discrepancyType` (allow null).
- [ ] `resolvedAt` is nullable `Instant`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Class name is legacy `ReconResult`; the **table** is `recon_breaks` (Day-1
schema). Bridge them with `@Table(name = "recon_breaks")` — no rename
required. `resolvedAt` is nullable: open breaks have no resolution time yet.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- `@Entity @Table(name = "recon_breaks")` — class name stays `ReconResult`.
- Inner enum `Status { OPEN, RESOLVED, SUPPRESSED }` — small, lives next to
  the entity.
- `@ManyToOne(fetch = LAZY, optional = false)` to `Trade` with
  `@JoinColumn(name = "trade_id")`.
- `@Enumerated(STRING)` on `discrepancyType` (DiscrepancyType enum) and
  `status`.
- `detectedAt`: NOT NULL, `updatable = false`. `resolvedAt`: nullable.
- Add a `resolve()` method that flips status + stamps `resolvedAt = now()`.
</details>

<details>
<summary>Hint 3 — Entity skeleton</summary>

```java
@Entity
@Table(name = "recon_breaks")
public class ReconResult {

    public enum Status { OPEN, RESOLVED, SUPPRESSED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_id")
    private Trade trade;

    // TODO: @Enumerated(STRING) discrepancyType (DiscrepancyType, NOT NULL, length 30)
    // TODO: @Enumerated(STRING) status (NOT NULL, length 20, default OPEN)
    // TODO: detectedAt (NOT NULL, updatable=false)
    // TODO: resolvedAt (NULLABLE)

    protected ReconResult() {}

    // TODO: resolve() flips status + stamps resolvedAt
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/model/ReconResult.java`

```java
package com.dbtraining.tradeflow.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "recon_breaks")
public class ReconResult {

    public enum Status { OPEN, RESOLVED, SUPPRESSED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_id")
    private Trade trade;

    @Enumerated(EnumType.STRING)
    @Column(name = "discrepancy_type", nullable = false, length = 30)
    private DiscrepancyType discrepancyType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;       // nullable

    protected ReconResult() {}

    private ReconResult(Builder b) {
        this.trade           = b.trade;
        this.discrepancyType = b.discrepancyType;
        this.status          = b.status != null ? b.status : Status.OPEN;
        this.detectedAt      = b.detectedAt != null ? b.detectedAt : Instant.now();
        this.resolvedAt      = b.resolvedAt;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Trade trade;
        private DiscrepancyType discrepancyType;
        private Status status;
        private Instant detectedAt;
        private Instant resolvedAt;

        public Builder trade(Trade v)                       { this.trade = v; return this; }
        public Builder discrepancyType(DiscrepancyType v)   { this.discrepancyType = v; return this; }
        public Builder status(Status v)                     { this.status = v; return this; }
        public Builder detectedAt(Instant v)                { this.detectedAt = v; return this; }
        public Builder resolvedAt(Instant v)                { this.resolvedAt = v; return this; }

        public ReconResult build() { return new ReconResult(this); }
    }

    public void resolve() {
        if (this.status == Status.RESOLVED) return;
        this.status = Status.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    public boolean isOpen() { return status == Status.OPEN; }

    public Long getId()                          { return id; }
    public Trade getTrade()                      { return trade; }
    public DiscrepancyType getDiscrepancyType()  { return discrepancyType; }
    public Status getStatus()                    { return status; }
    public Instant getDetectedAt()               { return detectedAt; }
    public Instant getResolvedAt()               { return resolvedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReconResult other)) return false;
        return Objects.equals(trade, other.trade)
            && discrepancyType == other.discrepancyType
            && Objects.equals(detectedAt, other.detectedAt);
    }

    @Override
    public int hashCode() { return Objects.hash(trade, discrepancyType, detectedAt); }
}
```
</details>

**Files to touch:** `model/ReconResult.java`.

---

### TICKET-I059 — `@Entity AuditLog`

**What**
- A JPA entity mapping the Day-1 `audit_log` table, with `oldValue` / `newValue` typed as JSONB on Postgres.

**Why**
- Day 8's audit trigger writes into this table; the entity has to read the same columns or Day-8 reporting breaks.
- Teaches Hibernate's `@JdbcTypeCode(SqlTypes.JSON)` — the bridge between Postgres' JSONB type and a plain `String` field.

**Observe**
- `select * from audit_log where table_name='trades'` returns the JSON payload as text.
- H2 (dev) tolerates `jsonb` as `VARCHAR` so the same entity boots under both profiles.
- [ ] Fields: `id, entity, entityId, action, oldValue, newValue, timestamp, userName`.
- [ ] `oldValue` / `newValue` mapped as `@Column(columnDefinition="jsonb")` —
  see Hibernate `@JdbcTypeCode(SqlTypes.JSON)`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

The `audit_log` table was built in Day 1 (TICKET-I015) — the column names you
chose then are what your entity must mirror. The reference implementation uses
`table_name`, `operation`, `row_pk`, `before_data`, `after_data`, `changed_by`,
`changed_at`. Map JSONB with `@JdbcTypeCode(SqlTypes.JSON)` — on the Java side
the field type stays `String` (raw JSON) so the entity layer is payload-agnostic.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- `@Entity @Table(name = "audit_log")`.
- Inner enum `Operation { I, U, D }`. `@Enumerated(STRING)` + `columnDefinition = "CHAR(1)"`.
- `tableName` `VARCHAR(64)` NOT NULL.
- `rowPk` `Long` NOT NULL.
- `beforeData` / `afterData` — `@JdbcTypeCode(SqlTypes.JSON)` +
  `@Column(columnDefinition = "jsonb")`. Java type `String`.
- `changedBy` `VARCHAR(50)` NOT NULL. `changedAt` `Instant` NOT NULL `updatable = false`.
- This entity is read-mostly (rows only inserted by the Postgres trigger from
  Day 1 or by Java in tests). No setters needed.
</details>

<details>
<summary>Hint 3 — Entity skeleton</summary>

```java
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    public enum Operation { I, U, D }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: tableName  VARCHAR(64) NOT NULL
    // TODO: operation  CHAR(1)  NOT NULL  (@Enumerated STRING)
    // TODO: rowPk      BIGINT   NOT NULL
    // TODO: beforeData JSONB    NULL     (@JdbcTypeCode(SqlTypes.JSON))
    // TODO: afterData  JSONB    NULL     (@JdbcTypeCode(SqlTypes.JSON))
    // TODO: changedBy  VARCHAR(50) NOT NULL
    // TODO: changedAt  Instant  NOT NULL  updatable=false

    protected AuditLog() {}
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/model/AuditLog.java`

```java
package com.dbtraining.tradeflow.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    public enum Operation { I, U, D }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false, length = 64)
    private String tableName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private Operation operation;

    @Column(name = "row_pk", nullable = false)
    private Long rowPk;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data", columnDefinition = "jsonb")
    private String beforeData;   // nullable — INSERT has no before

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data", columnDefinition = "jsonb")
    private String afterData;    // nullable — DELETE has no after

    @Column(name = "changed_by", nullable = false, length = 50)
    private String changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected AuditLog() {}

    private AuditLog(Builder b) {
        this.tableName  = b.tableName;
        this.operation  = b.operation;
        this.rowPk      = b.rowPk;
        this.beforeData = b.beforeData;
        this.afterData  = b.afterData;
        this.changedBy  = b.changedBy;
        this.changedAt  = b.changedAt != null ? b.changedAt : Instant.now();
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String tableName;
        private Operation operation;
        private Long rowPk;
        private String beforeData;
        private String afterData;
        private String changedBy;
        private Instant changedAt;

        public Builder tableName(String v)     { this.tableName = v; return this; }
        public Builder operation(Operation v)  { this.operation = v; return this; }
        public Builder rowPk(Long v)           { this.rowPk = v; return this; }
        public Builder beforeData(String v)    { this.beforeData = v; return this; }
        public Builder afterData(String v)     { this.afterData = v; return this; }
        public Builder changedBy(String v)     { this.changedBy = v; return this; }
        public Builder changedAt(Instant v)    { this.changedAt = v; return this; }

        public AuditLog build() { return new AuditLog(this); }
    }

    public Long getId()             { return id; }
    public String getTableName()    { return tableName; }
    public Operation getOperation() { return operation; }
    public Long getRowPk()          { return rowPk; }
    public String getBeforeData()   { return beforeData; }
    public String getAfterData()    { return afterData; }
    public String getChangedBy()    { return changedBy; }
    public Instant getChangedAt()   { return changedAt; }
}
```
</details>

**Files to touch:** `model/AuditLog.java`.

---

### TICKET-I060 — `TradeRepository` (Spring Data JPA)

**What**
- A `TradeRepository extends JpaRepository<Trade, Long>` with three derived finders (status, date range, tradeRef).

**Why**
- Spring Data generates the SQL — the line-count drop versus Day 4's hand-rolled JDBC is the lesson.
- Day 6's `GET /api/v1/trades?status=PENDING` calls `findByStatus(...)` directly; the derived-finder names here are the API contract.

**Observe**
- Boot log shows `Bean 'tradeRepository' of type ...$Repository...JpaRepository` is created (Spring Data proxied it).
- Calling `tradeRepository.findByStatus(PENDING)` in a `@SpringBootTest` returns the seeded row.
- [ ] `interface TradeRepository extends JpaRepository<Trade, Long>`.
- [ ] Custom finder: `List<Trade> findByStatus(TradeStatus status)`.
- [ ] Custom finder: `List<Trade> findByTradeDateBetween(LocalDate from, LocalDate to)`.
- [ ] `Optional<Trade> findByTradeRef(String tradeRef)`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Spring Data JPA generates the implementation at runtime from the method name —
no `@Query`, no `@Repository` boilerplate needed (though `@Repository` is a
helpful marker). Naming follows the contract `findBy<Field><Predicate>` —
e.g. `findByStatus`, `findByTradeDateBetween`, `findByTradeRef`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. Extend `JpaRepository<Trade, Long>` — you instantly get `save`, `findById`,
   `findAll`, `count`, `delete`, plus `Pageable` overloads.
2. Add derived finders:
   - `List<Trade> findByStatus(TradeStatus status)`
   - `Page<Trade> findByStatus(TradeStatus status, Pageable pageable)` (Day-6 paginated list)
   - `List<Trade> findByTradeDateBetween(LocalDate from, LocalDate to)`
   - `Optional<Trade> findByTradeRef(String tradeRef)`
3. Two more useful for service-layer guards:
   - `boolean existsByTradeRef(String tradeRef)` — POST duplicate check
   - `long countByStatus(TradeStatus status)` — used by the Day-6 status gauge.
</details>

<details>
<summary>Hint 3 — Repository interface</summary>

```java
package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByStatus(TradeStatus status);

    // TODO: Page<Trade> overload of findByStatus that takes Pageable
    // TODO: findByTradeDateBetween
    // TODO: findByTradeRef returning Optional<Trade>
    // TODO: existsByTradeRef + countByStatus
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/repository/TradeRepository.java`

```java
package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findByStatus(TradeStatus status);

    Page<Trade> findByStatus(TradeStatus status, Pageable pageable);

    List<Trade> findByTradeDateBetween(LocalDate from, LocalDate to);

    Optional<Trade> findByTradeRef(String tradeRef);

    boolean existsByTradeRef(String tradeRef);

    long countByStatus(TradeStatus status);

    List<Trade> findByCounterpartyId(Long counterpartyId);
}
```
</details>

**Files to touch:** `repository/TradeRepository.java`.

---

### TICKET-I061 — `ReconResultRepository`

**What**
- A `ReconResultRepository` with a derived `findByStatus` plus a JPQL `@Query` joining through `Trade` to `Counterparty`.

**Why**
- Introduces the first `@Query` — students see when derived finders run out and JPQL takes over (any time you need a multi-entity JOIN).
- Day 6's `GET /api/v1/recon/breaks?counterpartyId=...` will call `findUnresolvedByCounterparty` — this method becomes Day-6's filter contract.

**Observe**
- Hibernate logs the joined SQL `select ... from recon_breaks rr inner join trades t ... inner join counterparties c ...` when the JPQL fires.
- Result contains only `Status.OPEN` rows for the given counterparty.
- [ ] `findByStatus(String status)`.
- [ ] `@Query` for `findUnresolvedByCounterparty(Long counterpartyId)`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Most finders here are derived (`findByStatus`, `countByStatus`). The
counterparty lookup needs a JOIN through Trade — that's where `@Query` (JPQL)
earns its keep. Use the `ReconResult.Status` enum, not a raw `String`, so the
type system catches typos.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Extend `JpaRepository<ReconResult, Long>`.
- Derived: `List<ReconResult> findByStatus(ReconResult.Status status)`,
  `long countByStatus(ReconResult.Status status)`.
- JPQL `@Query` for "open breaks for counterparty X" — joins
  `ReconResult.trade` then `trade.counterparty`. The `@Param("counterpartyId")`
  binds the method argument to the `:counterpartyId` placeholder.
</details>

<details>
<summary>Hint 3 — Repository interface</summary>

```java
@Repository
public interface ReconResultRepository extends JpaRepository<ReconResult, Long> {

    List<ReconResult> findByStatus(ReconResult.Status status);

    long countByStatus(ReconResult.Status status);

    // TODO: @Query JPQL — open breaks for a counterparty (JOIN through Trade)
    @Query("""
           select r from ReconResult r
             join r.trade t
           where /* TODO: r.status = OPEN AND t.counterparty.id = :counterpartyId */
           """)
    List<ReconResult> findUnresolvedByCounterparty(@Param("counterpartyId") Long counterpartyId);
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/repository/ReconResultRepository.java`

```java
package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.ReconResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReconResultRepository extends JpaRepository<ReconResult, Long> {

    List<ReconResult> findByStatus(ReconResult.Status status);

    long countByStatus(ReconResult.Status status);

    List<ReconResult> findByTradeId(Long tradeId);

    @Query("""
           select r from ReconResult r
             join r.trade t
           where r.status = com.dbtraining.tradeflow.model.ReconResult$Status.OPEN
             and t.counterparty.id = :counterpartyId
           """)
    List<ReconResult> findUnresolvedByCounterparty(@Param("counterpartyId") Long counterpartyId);
}
```
</details>

**Files to touch:** `repository/ReconResultRepository.java`.

---

### TICKET-I062 — `TradeService` (rewrite for JPA)

**What**
- A `@Service TradeService` that uses `TradeRepository` for all persistence, with `@Transactional` boundaries on write methods.

**Why**
- Day 4's hand-rolled JDBC code is replaced today by JPA repositories — students should feel the line-count drop (typically 200+ → ~80 lines).
- Constructor injection (no field `@Autowired`) is the pattern Day 9's testing lab depends on; teach it here before mocks get involved.

**Observe**
- App boots without `NoSuchBeanDefinitionException` — proves the constructor wiring resolved.
- A controller call `POST /api/v1/trades` returns the saved Trade with a generated `id`.

**Acceptance criteria:**
- [ ] `createTrade(TradeRequest)` → returns saved Trade.
- [ ] `reconcileTrades()` → triggers `ReconciliationService`, persists `ReconResult` rows.
- [ ] `getTradesByFilter(filterDTO)` → uses the repo finders.
- [ ] Annotated `@Service`; constructor injection (no field-level `@Autowired`).

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Constructor injection beats field `@Autowired` — it makes dependencies
explicit, testable without Spring, and final. Wrap writes in `@Transactional`,
reads in `@Transactional(readOnly = true)`. Return DTOs from public methods,
not entities (avoids lazy-init blow-ups outside the session).
</details>

<details>
<summary>Hint 2 — More guided</summary>

- `@Service` on the class.
- Constructor takes `TradeRepository`, `InstrumentRepository`, `CounterpartyRepository`.
- Reads (`findAll`, `findById`, `findByStatus`, `findByDateRange`) →
  `@Transactional(readOnly = true)`. Map to `TradeDto.from(trade)`.
- `createTrade(TradeRequest)`:
  1. Reject if `existsByTradeRef` (409 mapping happens in controller).
  2. Look up Instrument + Counterparty by ID (404 if missing).
  3. `Trade.builder().build()`, `tradeRepository.save(...)`.
  4. Return mapped DTO.
- `updateStatus(id, newStatus)`: load, guard against terminal status, mutate,
  rely on JPA dirty-checking to persist on commit.
- `softDelete(id)`: load, set status to CANCELLED — no row deletion.
</details>

<details>
<summary>Hint 3 — Service skeleton</summary>

```java
@Service
public class TradeService {

    private final TradeRepository tradeRepository;
    private final InstrumentRepository instrumentRepository;
    private final CounterpartyRepository counterpartyRepository;

    public TradeService(TradeRepository tradeRepository,
                        InstrumentRepository instrumentRepository,
                        CounterpartyRepository counterpartyRepository) {
        this.tradeRepository       = tradeRepository;
        this.instrumentRepository  = instrumentRepository;
        this.counterpartyRepository = counterpartyRepository;
    }

    @Transactional(readOnly = true)
    public List<TradeDto> findAll() { /* TODO */ return List.of(); }

    @Transactional
    public TradeDto createTrade(TradeRequest request) {
        // TODO: uniqueness guard
        // TODO: instrument + counterparty lookup
        // TODO: Trade.builder()...build()
        // TODO: save + return mapped DTO
        return null;
    }

    // TODO: updateStatus, softDelete, findByStatus, findByDateRange
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/service/TradeService.java`

```java
package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.TradeDto;
import com.dbtraining.tradeflow.dto.TradeRequest;
import com.dbtraining.tradeflow.exception.TradeNotFoundException;
import com.dbtraining.tradeflow.model.Counterparty;
import com.dbtraining.tradeflow.model.Instrument;
import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.repository.CounterpartyRepository;
import com.dbtraining.tradeflow.repository.InstrumentRepository;
import com.dbtraining.tradeflow.repository.TradeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TradeService {

    private final TradeRepository tradeRepository;
    private final InstrumentRepository instrumentRepository;
    private final CounterpartyRepository counterpartyRepository;

    public TradeService(TradeRepository tradeRepository,
                        InstrumentRepository instrumentRepository,
                        CounterpartyRepository counterpartyRepository) {
        this.tradeRepository       = tradeRepository;
        this.instrumentRepository  = instrumentRepository;
        this.counterpartyRepository = counterpartyRepository;
    }

    @Transactional(readOnly = true)
    public List<TradeDto> findAll() {
        return tradeRepository.findAll().stream().map(TradeDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<TradeDto> findAll(Pageable pageable) {
        return tradeRepository.findAll(pageable).map(TradeDto::from);
    }

    @Transactional(readOnly = true)
    public TradeDto findById(Long id) {
        return tradeRepository.findById(id).map(TradeDto::from)
                .orElseThrow(() -> new TradeNotFoundException("Trade " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<TradeDto> findByStatus(TradeStatus status) {
        return tradeRepository.findByStatus(status).stream().map(TradeDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<TradeDto> findPageByStatus(TradeStatus status, Pageable pageable) {
        return tradeRepository.findByStatus(status, pageable).map(TradeDto::from);
    }

    @Transactional(readOnly = true)
    public List<TradeDto> findByDateRange(LocalDate from, LocalDate to) {
        return tradeRepository.findByTradeDateBetween(from, to).stream().map(TradeDto::from).toList();
    }

    @Transactional
    public TradeDto createTrade(TradeRequest request) {
        if (tradeRepository.existsByTradeRef(request.tradeRef())) {
            throw new IllegalStateException("Trade with tradeRef '" + request.tradeRef() + "' already exists");
        }
        Instrument instrument = instrumentRepository.findById(request.instrumentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "instrumentId " + request.instrumentId() + " not found"));
        Counterparty counterparty = counterpartyRepository.findById(request.counterpartyId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "counterpartyId " + request.counterpartyId() + " not found"));

        Trade trade = Trade.builder()
                .tradeRef(request.tradeRef())
                .instrument(instrument)
                .counterparty(counterparty)
                .quantity(request.quantity())
                .price(request.price())
                .tradeDate(request.tradeDate())
                .status(TradeStatus.PENDING)
                .build();

        Trade saved = tradeRepository.save(trade);
        return TradeDto.from(saved);
    }

    @Transactional
    public TradeDto updateStatus(Long id, TradeStatus newStatus) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("Trade " + id + " not found"));
        if (trade.getStatus() != null && trade.getStatus().isTerminal()) {
            throw new IllegalStateException(
                    "Trade " + id + " is in terminal status " + trade.getStatus() + " and cannot transition");
        }
        trade.setStatus(newStatus);
        return TradeDto.from(trade);
    }

    @Transactional
    public void softDelete(Long id) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new TradeNotFoundException("Trade " + id + " not found"));
        trade.setStatus(TradeStatus.CANCELLED);
        // JPA dirty-checking flushes the UPDATE at commit — no explicit save() needed.
    }
}
```
</details>

**Files to touch:** `service/TradeService.java`.

---

### TICKET-I063 — Swagger / OpenAPI config

**What**
- A Springdoc dependency in `pom.xml` plus a `@Configuration OpenApiConfig` setting the API title and version.

**Why**
- I060's Swagger docs become the contract Day 7's frontend codes against — no Swagger means the frontend team is blocked guessing payload shapes.
- Springdoc replaces hand-written Postman collections; the auto-generated `/v3/api-docs` JSON is the single source of truth.

**Observe**
- `curl http://localhost:8080/v3/api-docs` returns OpenAPI 3 JSON with `"title":"TradeFlow API"`.
- Browser hit on `/swagger-ui.html` redirects to `/swagger-ui/index.html` and renders the UI.

**Acceptance criteria:**
- [ ] `pom.xml` has `springdoc-openapi-starter-webmvc-ui`.
- [ ] `/swagger-ui.html` redirects to `/swagger-ui/index.html` (Springdoc default).
- [ ] OpenAPI JSON visible at `/v3/api-docs`.
- [ ] App title is `"TradeFlow API"`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

The `springdoc-openapi-starter-webmvc-ui` dependency does 90% of the work
automatically — it scans controllers and exposes both `/v3/api-docs` (JSON) and
`/swagger-ui.html` (UI). The `@Configuration` bean is just for branding: title,
description, version.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Confirm `springdoc-openapi-starter-webmvc-ui` is in `pom.xml` (I054).
- Optionally pin paths in `application.yml`:
  ```yaml
  springdoc:
    api-docs:
      path: /v3/api-docs
    swagger-ui:
      path: /swagger-ui.html
  ```
- Add a `@Configuration` class with a `@Bean OpenAPI` returning project metadata.
- Boot, open `http://localhost:8080/swagger-ui.html` — title in the top-left
  must read "TradeFlow API".
</details>

<details>
<summary>Hint 3 — Config skeleton</summary>

```java
package com.dbtraining.tradeflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tradeflowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        // TODO: title = "TradeFlow API"
                        // TODO: description (one line) + version = "v1"
                );
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/config/OpenApiConfig.java`

```java
package com.dbtraining.tradeflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tradeflowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TradeFlow API")
                        .description("Trade reconciliation REST API — Deutsche Bank TDI 2026 case study.")
                        .version("v1")
                        .contact(new Contact()
                                .name("TradeFlow Team")
                                .email("tradeflow@dbtraining.example"))
                        .license(new License()
                                .name("Internal — Deutsche Bank TDI")));
    }
}
```
</details>

**Files to touch:** `config/OpenApiConfig.java`.

---

### TICKET-I064 — Annotate controllers with OpenAPI tags

**What**
- `@Tag`, `@Operation`, `@ApiResponse`, `@Parameter` annotations on every existing controller method.

**Why**
- Swagger without descriptions is a list of URLs; the frontend team can't tell `GET /trades/{id}` 404s from 200s without these.
- Sets the convention for Day 6 — every new endpoint added tomorrow must already arrive annotated.

**Observe**
- Swagger UI now shows a `Trades` section with human-readable summaries (not raw method names).
- Each endpoint's expandable card lists the `200`, `400`, `404` response shapes with example payloads.
- [ ] Every endpoint has `@Operation(summary=...)`.
- [ ] Standard responses (`@ApiResponse(responseCode="200")`, `"400"`, `"404"`).
- [ ] `@Parameter(description="...")` on path/query params.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

You're seeding patterns for Day-6 controllers. Annotate each existing endpoint
with `@Operation(summary = "...")` and document the expected HTTP responses
with `@ApiResponses`. `@Tag` at the class level groups endpoints in Swagger UI.
Pull all imports from `io.swagger.v3.oas.annotations.*`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

- Class-level `@Tag(name = "Trades", description = "...")`.
- Per-method `@Operation(summary = "...")` — keep summaries short, descriptive.
- `@ApiResponses({ @ApiResponse(responseCode = "200", description = "..."),
  @ApiResponse(responseCode = "404", ...) })`.
- `@Parameter(description = "...")` on `@PathVariable` / `@RequestParam` (or `@Schema` on body fields — handled by the DTO).
- 401/403 only matter once Day 6 wires security — annotate them now so Swagger UI documents the future state.
</details>

<details>
<summary>Hint 3 — Controller skeleton</summary>

```java
@RestController
@RequestMapping("/api/v1/trades")
@Tag(name = "Trades", description = "Trade management endpoints")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @Operation(summary = "List trades (paginated, optional status + date filters)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Page of trades returned")
        // TODO: 401 + 403 for Day-6 security
    })
    @GetMapping
    public Page<TradeDto> list(/* TODO: status + Pageable */) {
        return null;
    }

    // TODO: @Operation/@ApiResponses for POST, PUT /{id}/status, DELETE /{id}
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/controller/TradeController.java`

```java
package com.dbtraining.tradeflow.controller;

import com.dbtraining.tradeflow.dto.TradeDto;
import com.dbtraining.tradeflow.dto.TradeRequest;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/trades")
@Tag(name = "Trades", description = "Trade management endpoints")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) { this.tradeService = tradeService; }

    @Operation(summary = "List trades (paginated, optional status + date filters)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of trades returned"),
            @ApiResponse(responseCode = "401", description = "Auth missing"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    @GetMapping
    public Page<TradeDto> list(
            @Parameter(description = "Optional status filter") @RequestParam(required = false) TradeStatus status,
            @PageableDefault(size = 20, sort = "tradeDate", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return status != null
                ? tradeService.findPageByStatus(status, pageable)
                : tradeService.findAll(pageable);
    }

    @Operation(summary = "Create a new trade")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trade created, Location header set"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "Duplicate tradeRef")
    })
    @PostMapping
    public ResponseEntity<TradeDto> create(@Valid @RequestBody TradeRequest request) {
        TradeDto saved = tradeService.createTrade(request);
        return ResponseEntity
                .created(URI.create("/api/v1/trades/" + saved.id()))
                .body(saved);
    }

    @Operation(summary = "Update trade status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "404", description = "Trade not found"),
            @ApiResponse(responseCode = "409", description = "Trade already in terminal status")
    })
    @PutMapping("/{id}/status")
    public ResponseEntity<TradeDto> updateStatus(
            @Parameter(description = "Trade id") @PathVariable Long id,
            @RequestParam TradeStatus status) {
        return ResponseEntity.ok(tradeService.updateStatus(id, status));
    }

    @Operation(summary = "Soft-delete a trade")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Trade cancelled"),
            @ApiResponse(responseCode = "404", description = "Trade not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Trade id") @PathVariable Long id) {
        tradeService.softDelete(id);
        return ResponseEntity.noContent().build();
    }
}
```
</details>

**Files to touch:** `controller/*Controller.java` (mostly Day-6 work — start the patterns today).

---

### TICKET-I065 — Verify Swagger UI

**What**
- A manual walk-through of `/swagger-ui.html` confirming every endpoint loads, lists examples, and `Try it out` returns a non-error response.

**Why**
- Catches the cases where I064's annotations compile but render wrong (e.g. wrong `@Tag`, missing schema).
- Day 7 starts with the frontend team opening Swagger UI; a broken page on Monday morning costs an hour of debugging.

**Observe**
- Title bar at top reads `TradeFlow API`.
- `GET /api/v1/trades` from the `Try it out` button returns HTTP 200 with the seeded JSON array.
- [ ] Open `/swagger-ui.html` — all endpoints listed.
- [ ] Each has descriptions and example request/response bodies.
- [ ] You can hit `GET /api/v1/trades` from the UI and get a result.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Verification is mostly a click-through. Boot the dev profile, open the UI,
expand each endpoint, hit "Try it out" then "Execute". If a request returns a
500, the controller-to-service wiring is wrong — fix that first.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. `./mvnw spring-boot:run` with the dev profile.
2. Open `http://localhost:8080/swagger-ui.html` — expect the title bar to read
   "TradeFlow API".
3. Click each endpoint group (Trades, Recon). Confirm:
   - Endpoint summary visible.
   - Parameters listed with descriptions.
   - Response codes listed (200, 400, 404 etc.).
4. "Try it out" → Execute on `GET /api/v1/trades`. Expect 200 + empty page (or
   seeded data if I066 already done).
5. POST a trade through the UI — expect 201 + Location header.
</details>

<details>
<summary>Hint 3 — Verification commands</summary>

```bash
# Boot
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# Confirm OpenAPI JSON is correct
curl -s http://localhost:8080/v3/api-docs | jq '.info.title'
# → "TradeFlow API"

# Smoke a GET via curl
curl -s http://localhost:8080/api/v1/trades | jq '.content | length'
```

In the browser:
- `http://localhost:8080/swagger-ui.html` — full UI.
- Click `Trades` group, then `GET /api/v1/trades` → `Try it out` → `Execute`.
</details>

<details>
<summary>Reference — full walkthrough</summary>

### Verification checklist

1. **Boot dev profile**
   ```bash
   cd backend
   SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
   ```
   Look for `Started TradeflowApplication`.

2. **OpenAPI JSON sanity**
   ```bash
   curl -s http://localhost:8080/v3/api-docs | jq '.info'
   # → { "title": "TradeFlow API", "description": "...", "version": "v1" }
   ```

3. **Open Swagger UI** → `http://localhost:8080/swagger-ui.html`. Confirm:
   - Title bar reads "TradeFlow API".
   - Endpoint groups (`Trades`, `Recon`) collapse/expand correctly.
   - Each endpoint shows summary + parameters + response codes.

4. **Smoke GET**
   - Click `GET /api/v1/trades` → `Try it out` → `Execute`.
   - Status 200, JSON body returned.

5. **Smoke POST**
   ```json
   {
     "tradeRef": "TR-DEMO-001",
     "instrumentId": 1,
     "counterpartyId": 1,
     "quantity": 100,
     "price": 250.50,
     "tradeDate": "2026-06-10"
   }
   ```
   Paste into the POST body, Execute. Expect 201 + `Location: /api/v1/trades/N`.

6. **Confirm the Recon endpoints respond.** Each should return 200 (even if the
   body is empty until Day 6 wires the recon engine).

7. **Screenshot the Swagger UI** and attach it to your PR — that is the artefact
   the instructor checks at the Day-5 checkpoint.
</details>

---

### TICKET-I066 — `data.sql` for dev profile

**What**
- A `src/main/resources/data.sql` inserting 3 counterparties, 5 instruments, 10 trades — gated to the `dev` profile only.

**Why**
- Day 6's controller tests and Day 7's frontend dev both need rows on screen the moment the app boots — no manual INSERTs.
- Teaches `spring.sql.init.mode: embedded` — production seed data lives in Liquibase, dev seed data lives here, and the two never collide.

**Observe**
- Boot log under `dev` shows `Executed SQL script from class path resource [data.sql]`.
- `GET /api/v1/trades` returns the 10 seeded rows immediately after startup.

**Acceptance criteria:**
- [ ] `src/main/resources/data.sql` inserts 3 counterparties + 5 instruments + 10 trades.
- [ ] Only runs under `dev` profile (`spring.sql.init.mode: embedded`).
- [ ] `spring.jpa.defer-datasource-initialization: true` so JPA runs before `data.sql`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

`data.sql` runs after Liquibase by default on Spring Boot 3.x — you do NOT
need `spring.jpa.defer-datasource-initialization: true`. Enabling it creates a
circular bean dependency (`liquibase` ↔ `entityManagerFactory`). Just set
`spring.sql.init.mode: embedded` in `application-dev.yml` so it runs on H2 only.
Never put production seed data here — Liquibase owns that.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. In `application-dev.yml`:
   ```yaml
   spring:
     sql:
       init:
         mode: embedded   # runs data.sql on H2 only
   ```
   Do NOT set `defer-datasource-initialization: true`.
2. In `data.sql`:
   - 3 INSERTs into `counterparties` (LEI 20 chars exact, region in APAC|EMEA|NAMR|LATAM).
   - 5 INSERTs into `instruments` (covering 3+ asset classes).
   - 10 INSERTs into `trades` (varied statuses, varied dates).
3. Restart dev profile; H2 console `SELECT count(*) FROM trades;` → 10.
4. Restart **uat** profile — Postgres data should come from Liquibase, NOT data.sql (because `mode: embedded` skips H2-only init on Postgres).
</details>

<details>
<summary>Hint 3 — data.sql skeleton</summary>

```sql
-- backend/src/main/resources/data.sql
-- Dev-profile seed only (runs on H2). Liquibase owns the uat/prod seed.

INSERT INTO counterparties (name, lei_code, region) VALUES
  ('Deutsche Bank AG',       '7LTWFZYICNSX8D621K86', 'EMEA'),
  ('Goldman Sachs Group Inc','784F5XWPLTWKTBV3E584', 'NAMR'),
  ('Nomura Holdings Inc',    '6N69WMNCQOWKSDLVDX42', 'APAC');

-- TODO: 5 instruments — EQUITY, FIXED_INCOME, FX, COMMODITY at minimum.
-- TODO: 10 trades — vary status, vary trade_date across the last 30 days.
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit:** `backend/src/main/resources/data.sql`, `backend/src/main/resources/application-dev.yml`

```sql
-- backend/src/main/resources/data.sql
-- Dev seed only. spring.sql.init.mode=embedded restricts to H2.

INSERT INTO counterparties (name, lei_code, region) VALUES
  ('Deutsche Bank AG',        '7LTWFZYICNSX8D621K86', 'EMEA'),
  ('Goldman Sachs Group Inc', '784F5XWPLTWKTBV3E584', 'NAMR'),
  ('Nomura Holdings Inc',     '6N69WMNCQOWKSDLVDX42', 'APAC');

INSERT INTO instruments (symbol, name, asset_class, currency, isin) VALUES
  ('SAP.DE',  'SAP SE',                'EQUITY',       'EUR', 'DE0007164600'),
  ('NVDA',    'NVIDIA Corp',           'EQUITY',       'USD', 'US67066G1040'),
  ('EURUSD',  'EUR/USD spot',          'FX',           'USD',  NULL),
  ('BUND10Y', 'German 10Y Bund',       'FIXED_INCOME', 'EUR', 'DE0001102606'),
  ('XAU',     'Gold spot',             'COMMODITY',    'USD',  NULL);

INSERT INTO trades (trade_ref, instrument_id, counterparty_id, quantity, price, trade_date, status, created_at) VALUES
  ('TR-001', 1, 1,     500.0000,  120.5000, CURRENT_DATE - 5, 'MATCHED',   CURRENT_TIMESTAMP),
  ('TR-002', 2, 2,     100.0000,  890.2500, CURRENT_DATE - 4, 'PENDING',   CURRENT_TIMESTAMP),
  ('TR-003', 3, 1, 1000000.0000,    1.0850, CURRENT_DATE - 3, 'MATCHED',   CURRENT_TIMESTAMP),
  ('TR-004', 4, 3,       5.0000,   99.4000, CURRENT_DATE - 2, 'UNMATCHED', CURRENT_TIMESTAMP),
  ('TR-005', 5, 2,     100.0000, 2150.0000, CURRENT_DATE - 2, 'DISPUTED',  CURRENT_TIMESTAMP),
  ('TR-006', 1, 3,     200.0000,  121.0000, CURRENT_DATE - 1, 'PENDING',   CURRENT_TIMESTAMP),
  ('TR-007', 2, 1,      50.0000,  895.0000, CURRENT_DATE - 1, 'MATCHED',   CURRENT_TIMESTAMP),
  ('TR-008', 3, 2,  500000.0000,    1.0855, CURRENT_DATE,     'PENDING',   CURRENT_TIMESTAMP),
  ('TR-009', 4, 1,      10.0000,   99.5500, CURRENT_DATE,     'MATCHED',   CURRENT_TIMESTAMP),
  ('TR-010', 5, 3,      50.0000, 2148.7500, CURRENT_DATE,     'UNMATCHED', CURRENT_TIMESTAMP);
```

```yaml
# application-dev.yml — add or confirm
spring:
  sql:
    init:
      mode: embedded   # only runs on H2 (dev)
```
</details>

**Files to touch:** `backend/src/main/resources/data.sql`, `application-dev.yml`.

---

### TICKET-I067 — Verify multi-profile run

**What**
- A signed-off run against all three profiles (`dev`, `uat`, `prod`) with `/actuator/health` green on each.

**Why**
- Proves the I055 profile split actually works end-to-end — students often pass I055 in isolation but break it later when they add a property to `application.yml` instead of the profile-specific file.
- Locks in the Day-7 hand-off: the deploy pipeline assumes any of the three profiles boots cleanly given the right env vars.

**Observe**
- All three runs log the matching active profile (`The following 1 profile is active: "uat"`).
- `curl /actuator/health` returns `{"status":"UP"}` in each case, with `db` component up under `dev`/`uat`/`prod`.
- [ ] `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run` boots with H2.
- [ ] `SPRING_PROFILES_ACTIVE=uat ./mvnw spring-boot:run` connects to PostgreSQL and Liquibase runs the changelog.
- [ ] Both produce a green `/actuator/health`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Run each profile in turn, hit `/actuator/health`, confirm `{"status":"UP"}`.
For uat you need Postgres running locally (`docker compose up -d postgres`).
Liquibase log lines (`ChangeSet ... ran successfully`) appear in both profiles
because Liquibase is wired in `application.yml`, not per-profile.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Three runs, one per profile:

1. **dev (H2)** — default. `./mvnw spring-boot:run`. Check log says
   `H2 console available at /h2-console`.
2. **uat (Postgres)** — `docker compose up -d postgres`, then
   `SPRING_PROFILES_ACTIVE=uat ./mvnw spring-boot:run`. Check log says
   `HikariPool-1 - Added connection`.
3. **prod (Postgres + env vars)** — set `POSTGRES_HOST`, `POSTGRES_DB`,
   `POSTGRES_USER`, `POSTGRES_PASSWORD` envs, then
   `SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run`. If any env is missing,
   boot must fail loudly (that's the point — no silent defaults in prod).

After each: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`.
</details>

<details>
<summary>Hint 3 — Verification commands</summary>

```bash
# 1. Dev (H2)
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run &
sleep 15 && curl -s http://localhost:8080/actuator/health
# → {"status":"UP"}

# 2. UAT (Postgres)
docker compose up -d postgres
SPRING_PROFILES_ACTIVE=uat ./mvnw spring-boot:run &
sleep 15 && curl -s http://localhost:8080/actuator/health
# → {"status":"UP"}

# 3. Prod (fail-fast on missing envs)
unset POSTGRES_HOST POSTGRES_USER POSTGRES_PASSWORD POSTGRES_DB
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
# → expect boot failure: "Could not resolve placeholder 'POSTGRES_HOST'"

# Now set envs and retry
export POSTGRES_HOST=localhost POSTGRES_DB=tradeflow \
       POSTGRES_USER=tradeflow_user POSTGRES_PASSWORD=changeme
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
```
</details>

<details>
<summary>Reference — full walkthrough</summary>

### Full verification matrix

| Profile | DB                   | Liquibase | data.sql | Expected log lines |
|---------|----------------------|-----------|----------|---------------------|
| dev     | H2 in-memory         | yes       | yes      | "H2 console available at /h2-console" |
| uat     | Postgres (Docker)    | yes       | no       | "HikariPool-1 - Added connection" |
| prod    | Postgres (env-only)  | yes       | no       | No SQL DEBUG lines  |

### Steps

1. **Dev**
   ```bash
   SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
   # In another shell:
   curl -s http://localhost:8080/actuator/health
   ```
   Open `/h2-console`, JDBC URL `jdbc:h2:mem:tradeflow`, user `sa`, blank password.
   Run `SELECT count(*) FROM trades;` → expect 10 (from data.sql).

2. **UAT**
   ```bash
   docker compose up -d postgres
   SPRING_PROFILES_ACTIVE=uat ./mvnw spring-boot:run
   curl -s http://localhost:8080/actuator/health
   docker exec -it <postgres-container> psql -U tradeflow_user -d tradeflow -c "\dt"
   ```
   Expect Liquibase to have created all the tables; data.sql did NOT run.

3. **Prod (smoke)**
   ```bash
   export POSTGRES_HOST=localhost POSTGRES_DB=tradeflow \
          POSTGRES_USER=tradeflow_user POSTGRES_PASSWORD=changeme
   SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
   curl -s http://localhost:8080/actuator/health
   # Verify NO SQL DEBUG lines appear in the log.
   ```

4. **Prod fail-fast check**
   ```bash
   unset POSTGRES_HOST
   SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
   # Expected: "Could not resolve placeholder 'POSTGRES_HOST'" — boot aborts.
   ```

5. **Document in the PR** the boot time observed for each profile
   (e.g. dev = 4s, uat = 7s, prod = 7s).
</details>

---

## Run and Observe — End of Sprint 4 (Spring Boot foundations)

You've shipped 14 tickets (I054–I067) and the Spring Boot app lights up
for the first time. Before the instructor checkpoint, prove it to yourself.

**Run:**

> **Terminal #1** — from `tradeflow-studentscopy/backend/`

```bash
# Ctrl+C if running, then:
./mvnw spring-boot:run
```

**Observe — watch the boot log:**

| Check | Expected after Sprint 4 |
|---|---|
| `Started ... Application in N seconds` | Boot completes without stack traces |
| `HHH000204: Processing PersistenceUnitInfo` / `Hibernate:` lines | JPA scans your 5 `@Entity` classes (Trade, Counterparty, Instrument, ReconResult, AuditLog) |
| `Liquibase: Successfully applied N changesets` | All Day-1/2 changesets ran on H2 |
| `H2 console available at '/h2-console'` | Dev profile is active |
| `Tomcat started on port(s): 8080` | App is listening |

**Browser checks:**

| URL | What |
|---|---|
| <http://localhost:8080/actuator/health> | `{"status":"UP"}` |
| <http://localhost:8080/swagger-ui.html> | endpoint list grows as you ship controllers (I064 tags visible) |
| <http://localhost:8080/v3/api-docs> | raw OpenAPI JSON with your `@Tag` groupings |
| <http://localhost:8080/h2-console> | JDBC `jdbc:h2:mem:tradeflow`, user `sa`, blank password — entities visible as tables, `data.sql` seed populated `trades` |

**Negative tests — prove your multi-profile config actually works:**

```bash
# 1. Start app under uat profile with Postgres NOT running — should fail fast
SPRING_PROFILES_ACTIVE=uat ./mvnw spring-boot:run
# Expected: HikariPool connection refused, app exits within ~10s (not silent fallback to H2)

# 2. Start app under prod profile with required env vars UNSET — should fail fast
unset POSTGRES_HOST POSTGRES_USER POSTGRES_PASSWORD
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
# Expected: "Could not resolve placeholder 'POSTGRES_HOST'" — boot aborts
```

If either negative test silently boots on H2 instead of failing, your profile
isolation is broken — re-read TICKET-I055 Reference Solution and TICKET-I067.

**If something looks wrong:**
- Liquibase errors on boot: [`../day2/day02-liquibase.md`](../day2/day02-liquibase.md) §8.
- `@Entity` not found / table missing: re-check column names match snake_case
  schema from Day 1 (TICKET-I056 Hint 3).
- Swagger UI 404: confirm springdoc-openapi dependency in `pom.xml` (TICKET-I063).
- `data.sql` didn't run: only the `dev` profile auto-runs it; check
  `spring.sql.init.mode=always` (TICKET-I066).

---

**Instructor checkpoint:** Before you close out Day 5, get the instructor
to walk Swagger UI with you and confirm the uat profile boots clean against
the Docker Postgres.

---

## End-of-day checklist

- [ ] 14 tickets merged.
- [ ] Spring Boot boots in both `dev` and `uat` profiles.
- [ ] Swagger UI lists every endpoint.
- [ ] Liquibase shows in startup logs: "Successfully applied N migrations".

Next: [Day 6 — REST + Security + Monitoring](../day6/README.md)
