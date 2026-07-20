# Day 9 — Apache Kafka (taught + applied)

> Theme: **Event-driven backend — concepts in AM, integration in PM.**
> Tickets: **I112 – I124 + I124A–I124D** (17 tickets — 13 Kafka + 4 React polish)
> Modules: Apache Kafka — Architecture, Spring Integration, Reliability, Observability (AM teaching) + Kafka sprint tickets (PM) + React polish tickets (PM end)

The day is now fully aligned: Kafka is taught in the morning and applied
in the afternoon against the same project. React advanced (Module 2) was
taught on Day 8; today you ship the four polish tickets that apply it
— `I124A`–`I124D` (BreakContext, ErrorBoundary, ResolveBreakModal,
withAuditLog HOC). These used to be "floating polish" with no
I-numbers; they now have full acceptance criteria below.

By end of day:

- Kafka concepts grounded: brokers, topics, partitions, consumer groups, DLT.
- Posting a trade fires a Kafka event.
- A consumer auto-runs reconciliation.
- An audit consumer writes to `audit_log`.
- Kafka consumer lag is visible in Grafana.

---

## Just-in-time primer — Apache Kafka in 10 minutes

This morning's classroom block covers the concepts behind every Kafka
ticket below. If you skipped or want the cheat-sheet, here's the minimum
you need before you open `application.yml`.

**Architecture.** A **broker** is one Kafka server; a cluster is several
brokers. A **topic** is a named stream of records (`trade-events` here).
Each topic is split into **partitions** so consumers can read in
parallel. Records inside a partition are strictly ordered by **offset**.

**Producers and consumers.** A **producer** writes records to a topic.
A **consumer** reads them. Two consumers in the *same* group share the
work (each record goes to one of them). Two consumers in *different*
groups both receive every record — that's how we run reconciliation and
audit in parallel:

```
trade-events topic
   │
   ├─ "recon-group"  → ReconEventConsumer  (runs reconciliation)
   └─ "audit-group"  → AuditEventConsumer  (writes audit_log)
```

**Partition keys.** `kafkaTemplate.send(topic, key, value)` — Kafka hashes
the key to pick the partition. Use `tradeRef` so all events for the same
trade land on the same partition and stay ordered.

**Spring for Kafka — the four touch points.**

| API | What it does |
|---|---|
| `KafkaTemplate<K,V>` | Inject and call `.send(topic, key, value)` to publish |
| `@KafkaListener(topics=..., groupId=...)` | Annotate a method to consume |
| `JsonSerializer` / `JsonDeserializer` | JSON ↔ POJO (set `trusted.packages`!) |
| `ConsumerFactory` / `ConcurrentKafkaListenerContainerFactory` | Tune retries, error handling, concurrency |

**Delivery semantics.** `ack-mode: record` = at-least-once. The consumer
commits its offset *after* your handler returns. A crash mid-handler →
the record is redelivered. Exactly-once is possible but needs producer
idempotency + transactional consumers — out of scope today.

**Error handling — DLT.** Wrap the deserializer in
`ErrorHandlingDeserializer`. Add a `DefaultErrorHandler` with a
`FixedBackOff(1s, 3)` and a `DeadLetterPublishingRecoverer`. Poison-pill
records (malformed JSON, deserialization failures) skip the retry loop
and land on `<topic>.DLT` for offline inspection.

**Observability.** `observation-enabled: true` switches on Spring-Kafka's
Micrometer integration. The Day-6 Prometheus scrape now also picks up
`kafka_consumer_fetch_manager_records_lag` (consumer lag) and producer
send rates. Consumer lag is the single number you watch in production.

> ✅ **Coverage closed:** `BreakContext`, `ErrorBoundary`,
> `ResolveBreakModal` and the `withAuditLog` HOC are now full tickets —
> `I124A`–`I124D` — with acceptance criteria below.

---

## Sprint 8 — Kafka Integration

### TICKET-I112 — Add `spring-kafka` to `pom.xml`

**What**
- Add `spring-kafka` (main) and `spring-kafka-test` (test scope) to `backend/pom.xml` so `KafkaTemplate`, `@KafkaListener`, and the JSON (de)serializers land on the classpath.

**Why**
- Every other Day-9 Kafka ticket (I113 config, I115 producer, I116-I117-I119 consumers) is a no-op without these JARs on the classpath; this is the gate for the whole sprint.
- `spring-kafka-test` is what TICKET-I122's `@EmbeddedKafka` integration test boots — without test-scope here, that test class won't compile.

**Observe**
- `./mvnw dependency:tree | grep spring-kafka` prints two lines (`spring-kafka:jar:compile` and `spring-kafka-test:jar:test`).
- Boot the app — startup log shows `KafkaAutoConfiguration` activated; absence means the dependency is missing or scoped wrong.

**Acceptance criteria:**
- [ ] `org.springframework.kafka:spring-kafka` added.
- [ ] `mvn dependency:tree` confirms it.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Spring Boot's BOM already pins a Kafka version that matches your Boot version.
Add the starter with no `<version>` block. Pair it with `spring-kafka-test`
(scope `test`) for the embedded broker you'll use in TICKET-I122.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. Open `backend/pom.xml`, find the `<dependencies>` block.
2. Add two entries — main and test — under a `<!-- Day 9 — Kafka -->` comment.
3. Reload Maven (`./mvnw clean install -DskipTests`).
4. Confirm with `./mvnw dependency:tree | grep spring-kafka`.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```xml
<!-- Day 9 — Kafka (TICKET-I112) -->
<dependency>
  <groupId>org.springframework.kafka</groupId>
  <artifactId>spring-kafka</artifactId>
</dependency>
<!-- TODO: add spring-kafka-test with scope=test -->
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/pom.xml`

```xml
<!-- ============================================================ -->
<!-- Day 9 — Kafka (TICKET-I112)                                  -->
<!-- ============================================================ -->
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
```
</details>

**Files to touch:** `backend/pom.xml`.

---

### TICKET-I113 — Kafka config in `application.yml`

**What**
- Add a `spring.kafka:` block to `application.yml` covering bootstrap servers, JSON serializer/deserializer, `trusted.packages=com.dbtraining.tradeflow.*`, consumer group `recon-group`, and `listener.ack-mode: record`.

**Why**
- I115's producer and I116/I117/I119's consumers all rely on this YAML at startup; a missing `trusted.packages` is the #1 cause of `IllegalArgumentException: The class 'TradeEvent' is not in the trusted packages` on first consume.
- `ack-mode: record` is the at-least-once setting that I119's audit consumer assumes — without it, a mid-handler crash silently swallows audit rows.

**Observe**
- Startup log line: `Bootstrap servers: [localhost:9092]` and `ConsumerConfig values:` block listing `value.deserializer = org.springframework.kafka.support.serializer.JsonDeserializer`.
- Misconfigured trusted packages → consumer log `Caused by: java.lang.IllegalArgumentException: The class ... is not in the trusted packages: [java.util, java.lang]`.

**Acceptance criteria:**
- [ ] `spring.kafka.bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}`.
- [ ] Consumer group `recon-group`.
- [ ] JSON serializer / deserializer.
- [ ] Trusted packages set so JSON deserializer accepts `com.dbtraining.tradeflow.*`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

`JsonDeserializer` refuses to deserialize into a class whose package isn't on
the trusted list — by design, to stop deserialization-gadget attacks. You
must declare `spring.json.trusted.packages` for your own DTO package.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Add a `spring.kafka:` block. Three sub-blocks:
- `bootstrap-servers` — env-var with `localhost:9092` fallback.
- `consumer` — string-key deserializer, JSON value deserializer, trusted
  packages property, default type.
- `producer` — string-key serializer, JSON value serializer, `acks: all`.

Then `listener.ack-mode: record` so the offset commits after each successful
handler call (at-least-once delivery — what you want for audit).
</details>

<details>
<summary>Hint 3 — Config skeleton</summary>

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.dbtraining.tradeflow.*"
        # TODO: set spring.json.value.default.type to your TradeEvent FQN
    producer:
      # TODO: acks, key-serializer, value-serializer
      properties:
        spring.json.add.type.headers: false
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/resources/application.yml`

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.dbtraining.tradeflow.*"
        spring.json.value.default.type: "com.dbtraining.tradeflow.dto.TradeEvent"
    producer:
      acks: all
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: false
    listener:
      ack-mode: record            # at-least-once
      observation-enabled: true   # Micrometer metrics for I120

tradeflow:
  kafka:
    topics:
      trades: trade-events
      dlt: trade-events.DLT
```
</details>

**Files to touch:** `application.yml`, `application-dev.yml`.

---

### TICKET-I114 — `TradeEvent` DTO

**What**
- Java `record TradeEvent(String tradeRef, Action action, Instant timestamp, TradeDto payload)` plus nested `enum Action { CREATED, UPDATED, CANCELLED }` under `dto/`.

**Why**
- Single source-of-truth payload that I115's producer serializes and I116/I117/I119's three consumer groups deserialize — any field drift here means every consumer redeploys together.
- `Action.CREATED` is the discriminator I117's recon consumer filters on; without the enum, recon would re-run on every UPDATED event and double-count results.

**Observe**
- `./mvnw compile` succeeds; class file appears under `target/classes/com/dbtraining/tradeflow/dto/TradeEvent.class`.
- A quick `new ObjectMapper().findAndRegisterModules().writeValueAsString(...)` test prints ISO-8601 `timestamp` (not a numeric epoch) — confirms `jsr310` is on the classpath.

**Acceptance criteria:**
- [ ] Fields: `tradeRef, action (enum CREATED/UPDATED/CANCELLED), timestamp, payload (TradeDto)`.
- [ ] Has a no-arg constructor for JSON deserialisation.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

A Java `record` is the cleanest fit — immutable, auto `equals/hashCode`,
and Spring Boot 3 + Jackson can deserialize records via the canonical
constructor. The nested enum lives inside the record.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. New file under `dto/TradeEvent.java`.
2. Declare it as a `record` with four components.
3. Declare a nested `enum Action { CREATED, UPDATED, CANCELLED }`.
4. `timestamp` is `java.time.Instant` — Jackson serializes it as ISO-8601 when
   `jackson-datatype-jsr310` is on the classpath (it is, via Boot).
5. `payload` is your existing `TradeDto`.
</details>

<details>
<summary>Hint 3 — DTO skeleton</summary>

```java
package com.dbtraining.tradeflow.dto;

import java.time.Instant;

public record TradeEvent(
        String tradeRef,
        Action action,
        Instant timestamp,
        TradeDto payload
) {
    public enum Action {
        // TODO: CREATED, UPDATED, CANCELLED
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/dto/TradeEvent.java`

```java
package com.dbtraining.tradeflow.dto;

import java.time.Instant;

/**
 * Kafka message published on every trade lifecycle change.
 * Plain record — Spring Kafka's JsonSerializer turns it into JSON,
 * JsonDeserializer rebuilds it via the canonical constructor.
 */
public record TradeEvent(
        String tradeRef,
        Action action,
        Instant timestamp,
        TradeDto payload
) {

    public enum Action {
        CREATED,
        UPDATED,
        CANCELLED
    }

    public static TradeEvent created(TradeDto payload) {
        return new TradeEvent(payload.tradeRef(), Action.CREATED, Instant.now(), payload);
    }
}
```
</details>

**Files to touch:** `dto/TradeEvent.java`.

---

### TICKET-I115 — `TradeEventProducer`

**What**
- `@Service TradeEventProducer` with `publish(TradeEvent)` that calls `kafkaTemplate.send(topic, event.tradeRef(), event)` and attaches `.whenComplete` for logging — wired into `TradeService.createTrade` after the DB insert.

**Why**
- I115's producer wires the trade-creation event consumed by I117's recon service and I119's audit service — without this publish call, both downstream consumers go silent and Day-10's demo has nothing on the feed.
- Using `tradeRef` as the partition key guarantees per-trade ordering (CREATED → UPDATED → CANCELLED never reorder across partitions) so the audit log stays correct.

**Observe**
- POST a trade → backend log: `Published TradeEvent tradeRef=TRD-... -> partition=0 offset=N` (DEBUG) or, on failure, `Failed to publish TradeEvent tradeRef=... action=CREATED`.
- Kafdrop UI → `trade-events` topic → Messages tab: one row with `key=TRD-...`, JSON value showing all four fields.

**Acceptance criteria:**
- [ ] `publish(TradeEvent event)` sends to `trade-events` topic.
- [ ] Called from `TradeService.createTrade()` AFTER the DB insert succeeds.
- [ ] Failures logged but DO NOT roll back the trade insert.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

`KafkaTemplate.send(topic, key, value)` returns a `CompletableFuture` —
attach `.whenComplete((result, ex) -> ...)` so a failed publish is logged
but never thrown. Using `tradeRef` as the key keeps per-trade ordering on
one partition.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. `@Service` class `TradeEventProducer`.
2. Inject `KafkaTemplate<String, TradeEvent>` plus the topic name from
   `${tradeflow.kafka.topics.trades}`.
3. Single method `publish(TradeEvent)` that calls `send(topic, key, value)`.
4. Attach a `.whenComplete` callback — error path logs, success path does
   nothing (or debug-logs partition/offset).
5. In `TradeService.createTrade`, call `eventProducer.publish(...)` AFTER
   `tradeRepository.save(trade)` returns.
</details>

<details>
<summary>Hint 3 — Producer skeleton</summary>

```java
@Service
public class TradeEventProducer {
    private static final Logger log = LoggerFactory.getLogger(TradeEventProducer.class);
    private final KafkaTemplate<String, TradeEvent> kafkaTemplate;
    private final String topic;

    public TradeEventProducer(KafkaTemplate<String, TradeEvent> kafkaTemplate,
                              @Value("${tradeflow.kafka.topics.trades}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(TradeEvent event) {
        // TODO: send with tradeRef as partition key + .whenComplete log on error
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/kafka/TradeEventProducer.java` (plus call-sites in `service/TradeService.java`)

```java
package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.dto.TradeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TradeEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TradeEventProducer.class);

    private final KafkaTemplate<String, TradeEvent> kafkaTemplate;
    private final String topic;

    public TradeEventProducer(KafkaTemplate<String, TradeEvent> kafkaTemplate,
                              @Value("${tradeflow.kafka.topics.trades}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(TradeEvent event) {
        kafkaTemplate.send(topic, event.tradeRef(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish TradeEvent tradeRef={} action={}",
                                event.tradeRef(), event.action(), ex);
                    } else if (log.isDebugEnabled()) {
                        log.debug("Published TradeEvent tradeRef={} -> partition={} offset={}",
                                event.tradeRef(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
```

Call-site wiring in `TradeService`:

```java
// after tradeRepository.save(trade) succeeds:
eventProducer.publish(new TradeEvent(
        saved.getTradeRef(),
        TradeEvent.Action.CREATED,
        Instant.now(),
        TradeDto.from(saved)));

// in updateStatus(...):
eventProducer.publish(new TradeEvent(
        saved.getTradeRef(),
        TradeEvent.Action.UPDATED,
        Instant.now(),
        TradeDto.from(saved)));

// in softDelete(...):
eventProducer.publish(new TradeEvent(
        saved.getTradeRef(),
        TradeEvent.Action.CANCELLED,
        Instant.now(),
        TradeDto.from(saved)));
```
</details>

**Files to touch:** `kafka/TradeEventProducer.java`, `service/TradeService.java`.

---

### TICKET-I116 — `TradeEventConsumer`

**What**
- `@Component TradeEventConsumer` with one `@KafkaListener(topics="${tradeflow.kafka.topics.trades}", groupId="trade-log-group")` method that logs `tradeRef` + `action`.

**Why**
- Forces students to see *one* consumer working end-to-end before they stack recon + audit on top — debugging three consumers at once on Day 9 PM eats the whole sprint.
- The distinct `groupId="trade-log-group"` is the proof-by-example for the morning teaching block: different groups each get a copy, same group splits the load.

**Observe**
- POST a trade → backend log within ~1s: `Received TradeEvent[tradeRef=TRD-..., action=CREATED]`.
- Kafdrop → Consumer Groups → `trade-log-group` shows lag=0 and the single partition assigned to one consumer instance.

**Acceptance criteria:**
- [ ] `@KafkaListener(topics="trade-events")` on a `consume(TradeEvent)` method.
- [ ] Logs every event with `tradeRef` and `action`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Pick its own `groupId` (e.g. `trade-log-group`). Each distinct group sees
every event — so this log consumer doesn't steal messages from the recon
or audit consumers you'll add next.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. `@Component` class.
2. One method annotated `@KafkaListener(topics="${tradeflow.kafka.topics.trades}",
   groupId="trade-log-group", containerFactory="kafkaListenerContainerFactory")`.
3. Method takes a single `TradeEvent` argument — Spring deserializes it via
   the consumer factory.
4. Body just logs `tradeRef` + `action`.
</details>

<details>
<summary>Hint 3 — Consumer skeleton</summary>

```java
@Component
public class TradeEventConsumer {
    private static final Logger log = LoggerFactory.getLogger(TradeEventConsumer.class);

    @KafkaListener(
            topics = "${tradeflow.kafka.topics.trades}",
            groupId = "trade-log-group",
            // TODO: containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(TradeEvent event) {
        // TODO: log tradeRef and action
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/kafka/TradeEventConsumer.java`

```java
package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.dto.TradeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TradeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TradeEventConsumer.class);

    @KafkaListener(
            topics = "${tradeflow.kafka.topics.trades}",
            groupId = "trade-log-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(TradeEvent event) {
        log.info("Received TradeEvent[tradeRef={}, action={}]",
                event.tradeRef(), event.action());
    }
}
```
</details>

**Files to touch:** `kafka/TradeEventConsumer.java`.

---

### TICKET-I117 — `ReconEventConsumer` — auto-reconcile on CREATED

**What**
- `@Component ReconEventConsumer` with `@KafkaListener(groupId="recon-group")` that early-returns unless `action == CREATED`, then calls `reconciliationService.runForTrade(event.tradeRef())`.

**Why**
- Closes the loop the Day-5 recon service started: a trade is now auto-reconciled the moment it lands, no scheduled job, no manual trigger — that's the headline of the Day-10 demo.
- Filtering on `CREATED` (not all actions) prevents duplicate `recon_results` rows every time the status moves PENDING → MATCHED via I115's UPDATED publish.

**Observe**
- POST a trade → backend log: `Reconciling tradeRef=TRD-...` followed by Day-5's `Recon: trade TRD-... status=...`.
- `SELECT * FROM recon_results ORDER BY id DESC LIMIT 1;` shows a fresh row with the new `tradeRef`.

**Acceptance criteria:**
- [ ] When `action == CREATED`, calls `ReconciliationService.runForTrade(tradeRef)`.
- [ ] Inserts any resulting `ReconResult`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Different `groupId` from the log consumer (`recon-group`) so it gets its own
copy of every event. Early-return on actions you don't care about — cheaper
than a `switch`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. `@Component`, constructor-inject `ReconciliationService`.
2. `@KafkaListener` with `groupId="recon-group"`, same topic + container
   factory as TICKET-I116.
3. Method: if `event.action() != CREATED`, return. Otherwise call
   `reconService.runForTrade(event.tradeRef())`.
4. The service already inserts the `ReconResult` (Day-5 code) — you just
   trigger it.
</details>

<details>
<summary>Hint 3 — Consumer skeleton</summary>

```java
@Component
public class ReconEventConsumer {
    private final ReconciliationService reconService;

    public ReconEventConsumer(ReconciliationService reconService) {
        this.reconService = reconService;
    }

    @KafkaListener(
            topics = "${tradeflow.kafka.topics.trades}",
            groupId = "recon-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onEvent(TradeEvent event) {
        // TODO: filter on CREATED, call reconService.runForTrade(...)
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/kafka/ReconEventConsumer.java`

```java
package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.dto.TradeEvent;
import com.dbtraining.tradeflow.service.ReconciliationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReconEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReconEventConsumer.class);

    private final ReconciliationService reconService;

    public ReconEventConsumer(ReconciliationService reconService) {
        this.reconService = reconService;
    }

    @KafkaListener(
            topics = "${tradeflow.kafka.topics.trades}",
            groupId = "recon-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onEvent(TradeEvent event) {
        if (event.action() != TradeEvent.Action.CREATED) {
            return;  // only newly-created trades get auto-reconciled
        }
        log.info("Reconciling tradeRef={}", event.tradeRef());
        reconService.runForTrade(event.tradeRef());
    }
}
```
</details>

**Files to touch:** `kafka/ReconEventConsumer.java`.

---

### TICKET-I118 — Kafka error handling

**What**
- `config/KafkaConfig.java` with three beans: `ConsumerFactory` wrapping `JsonDeserializer` in `ErrorHandlingDeserializer`, `ConcurrentKafkaListenerContainerFactory` attaching the error handler, and a `DefaultErrorHandler(DeadLetterPublishingRecoverer, FixedBackOff(1s,3))` routing failures to `trade-events.DLT`.

**Why**
- Without DLT, a single malformed JSON message wedges the consumer partition — every subsequent trade builds up consumer lag (visible in I121's Grafana panel) until the broker restarts.
- `addNotRetryableExceptions(DeserializationException.class)` short-circuits the 3-second retry loop on errors that will never succeed, so deserialization failures land on DLT instantly instead of stalling the partition for 3 seconds.

**Observe**
- Publish malformed JSON with `kafka-console-producer --topic trade-events`, type `{"bad":true}` → backend log: `Record sent to DLT topic trade-events.DLT` (recoverer log line), and Kafdrop shows the message on `trade-events.DLT`.
- Without the not-retryable rule: log shows three `Retrying record after 1000ms` lines before DLT — that's wasted time on an unrecoverable error.

**Acceptance criteria:**
- [ ] `@KafkaListener(errorHandler="kafkaErrorHandler")`.
- [ ] On parse / processing failure → log + send to a `trade-events.DLT` dead-letter topic.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

`DefaultErrorHandler` takes (recoverer, backoff). Use
`DeadLetterPublishingRecoverer` to send the failed record to a DLT
partition. `FixedBackOff(1_000L, 3)` = 3 retries one second apart, then DLT.
Deserialization errors will never succeed — mark them non-retryable.
</details>

<details>
<summary>Hint 2 — More guided</summary>

In `config/KafkaConfig.java`:
1. Annotate with `@Configuration @EnableKafka`.
2. Bean `ConsumerFactory<String, TradeEvent>` — wrap `JsonDeserializer` in
   `ErrorHandlingDeserializer` so bad JSON becomes a recoverable exception.
3. Bean `ConcurrentKafkaListenerContainerFactory` — sets the consumer
   factory and `setCommonErrorHandler(errorHandler)`.
4. Bean `DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object>)`
   that uses a `DeadLetterPublishingRecoverer` mapping each record to
   `<topic>.DLT` on the same partition.
5. Skip retries for `DeserializationException` and `IllegalArgumentException`.
</details>

<details>
<summary>Hint 3 — Config skeleton</summary>

```java
@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, TradeEvent> consumerFactory() {
        // TODO: ErrorHandlingDeserializer wrapping JsonDeserializer
        return null;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TradeEvent>
            kafkaListenerContainerFactory(ConsumerFactory<String, TradeEvent> cf,
                                          DefaultErrorHandler errorHandler) {
        // TODO: build the factory, attach error handler
        return null;
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // TODO: DeadLetterPublishingRecoverer + FixedBackOff(1000, 3)
        return null;
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/config/KafkaConfig.java`

```java
package com.dbtraining.tradeflow.config;

import com.dbtraining.tradeflow.dto.TradeEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;
    private final String dltTopic;

    public KafkaConfig(KafkaProperties kafkaProperties,
                       @Value("${tradeflow.kafka.topics.dlt:trade-events.DLT}") String dltTopic) {
        this.kafkaProperties = kafkaProperties;
        this.dltTopic = dltTopic;
    }

    @Bean
    public ConsumerFactory<String, TradeEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.dbtraining.tradeflow.*");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, TradeEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TradeEvent>
            kafkaListenerContainerFactory(ConsumerFactory<String, TradeEvent> consumerFactory,
                                          DefaultErrorHandler errorHandler) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, TradeEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setObservationEnabled(true);
        return factory;
    }

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(1000L, 3));

        // unrecoverable errors skip the retry loop and go straight to DLT
        handler.addNotRetryableExceptions(
                DeserializationException.class,
                IllegalArgumentException.class);
        return handler;
    }

    @Bean
    public NewTopic deadLetterTopic() {
        return TopicBuilder.name(dltTopic).partitions(1).replicas(1).build();
    }
}
```
</details>

**Files to touch:** `config/KafkaConfig.java`.

---

### TICKET-I119 — `AuditEventConsumer`

**What**
- `@Component AuditEventConsumer` with `@KafkaListener(groupId="audit-group")` calling `auditService.record(event)` on every event — no action-filter, audit captures all lifecycle changes.

**Why**
- This is the second proof-of-fan-out from the morning teach: `audit-group` and `recon-group` both receive every event because they're distinct consumer groups; if a student accidentally reuses `recon-group` only one of the two consumers sees each message.
- Persisting via Kafka rather than a synchronous JPA call means an audit DB hiccup never blocks the trade write — the consumer just retries off the partition.

**Observe**
- POST a trade → backend log: `Auditing tradeRef=TRD-... action=CREATED`; `SELECT COUNT(*) FROM audit_log;` increments by exactly 1 (CREATED) per POST and by 1 more per status change.
- Kafdrop → Consumer Groups → three groups listed (`trade-log-group`, `recon-group`, `audit-group`), each at lag=0 after the smoke test.

**Acceptance criteria:**
- [ ] Listens to `trade-events`.
- [ ] Writes one row to `audit_log` per event.
- [ ] Different consumer group from `recon-group` so it runs in parallel.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Mirror `ReconEventConsumer` but with `groupId="audit-group"` and an
`AuditService.record(event)` call instead of the recon trigger. Don't filter
on action — audit captures every lifecycle change.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. `@Component`, constructor-inject `AuditService`.
2. `@KafkaListener` with `groupId="audit-group"`, same topic + container
   factory.
3. Handler just calls `auditService.record(event)`.
4. `AuditService.record` is responsible for `INSERT INTO audit_log ...`
   (use the audit_log table from Day 1 TICKET-I015).
</details>

<details>
<summary>Hint 3 — Consumer skeleton</summary>

```java
@Component
public class AuditEventConsumer {
    private final AuditService auditService;

    public AuditEventConsumer(AuditService auditService) {
        this.auditService = auditService;
    }

    @KafkaListener(
            topics = "${tradeflow.kafka.topics.trades}",
            groupId = "audit-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onEvent(TradeEvent event) {
        // TODO: persist via auditService
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/kafka/AuditEventConsumer.java`

```java
package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.dto.TradeEvent;
import com.dbtraining.tradeflow.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AuditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditEventConsumer.class);

    private final AuditService auditService;

    public AuditEventConsumer(AuditService auditService) {
        this.auditService = auditService;
    }

    @KafkaListener(
            topics = "${tradeflow.kafka.topics.trades}",
            groupId = "audit-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onEvent(TradeEvent event) {
        log.debug("Auditing tradeRef={} action={}", event.tradeRef(), event.action());
        auditService.record(event);
    }
}
```
</details>

**Files to touch:** `kafka/AuditEventConsumer.java`.

---

### TICKET-I120 — Kafka metrics → Prometheus

**What**
- Confirm `spring.kafka.listener.observation-enabled: true` (set in I113) and `management.endpoints.web.exposure.include` lists `prometheus` so Micrometer's Kafka instrumentation surfaces on `/actuator/prometheus`.

**Why**
- Day-8's Prometheus scrape job is already running — this ticket just turns on Spring Kafka's Micrometer bridge so consumer lag becomes a queryable metric for I121's Grafana panel.
- Consumer lag is the single most important production-readiness signal for an event-driven system; without it, a stuck consumer hides for hours.

**Observe**
- `curl -s localhost:8080/actuator/prometheus | grep -E 'kafka_consumer_fetch_manager_records_lag|kafka_producer_record_send_total'` prints both series with `client_id` and `topic` labels populated.
- Absent metric → either `observation-enabled` is false or `micrometer-registry-prometheus` isn't on the classpath; the actuator endpoint returns the rest of the metrics but no `kafka_` prefix at all.

**Acceptance criteria:**
- [ ] `kafka_consumer_records_lag` and `kafka_producer_send_rate` are scraped.
- [ ] Visible in `/actuator/prometheus`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

You don't need new code. `spring-kafka` ships a Micrometer integration —
turn it on via `spring.kafka.listener.observation-enabled: true` (already
in TICKET-I113) and confirm `micrometer-registry-prometheus` is on the
classpath. The metric names appear automatically once consumers run.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. Confirm `application.yml` has `management.endpoints.web.exposure.include`
   containing `prometheus` (Day 8 work — already there).
2. Confirm `spring.kafka.listener.observation-enabled: true`.
3. Boot the app, hit `/actuator/prometheus`.
4. Search the output for `kafka_consumer_fetch_manager_records_lag` and
   `kafka_producer_record_send_total` (these are the actual Micrometer
   names Spring exposes).
5. If you want them tagged per consumer group, ensure `client.id` is set
   per `@KafkaListener` (Spring derives it from `groupId`).
</details>

<details>
<summary>Hint 3 — Config skeleton</summary>

```yaml
spring:
  kafka:
    listener:
      observation-enabled: true   # turns on Micrometer instrumentation
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  # TODO: tag all metrics with application name
```

Then:

```bash
curl -s localhost:8080/actuator/prometheus | grep kafka_
# TODO: confirm consumer lag + producer send total appear
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit:** `backend/src/main/resources/application.yml` and `backend/pom.xml`

```yaml
spring:
  kafka:
    listener:
      observation-enabled: true
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  metrics:
    tags:
      application: ${spring.application.name}
```

Confirm `pom.xml` has the Micrometer Prometheus registry on the runtime classpath:

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
    <scope>runtime</scope>
</dependency>
```

Verify with:

```bash
curl -s localhost:8080/actuator/prometheus | grep -E \
  'kafka_consumer_fetch_manager_records_lag|kafka_producer_record_send_total'
```
</details>

**Files to touch:** `application.yml` (verify), `pom.xml` (verify).

---

### TICKET-I121 — Grafana panel — Kafka lag

**What**
- `monitoring/grafana/provisioning/dashboards/kafka.json` with two timeseries panels — consumer lag by `client_id` and `rate(kafka_producer_record_send_total[1m])` — checked in so Day-10's demo laptop boots with the dashboard already loaded.

**Why**
- Day-10 demos the realtime pipeline live; opening Grafana to a blank "New dashboard" mid-demo wastes 90 seconds — provisioning JSON in the repo is the only way to guarantee the panel is there on `docker compose up`.
- A DLT stat panel (`increase(kafka_producer_record_send_total{topic="trade-events.DLT"}[5m])`) gives the trainer one screen to point at when explaining I118's error-handling story.

**Observe**
- `docker compose restart grafana` → log line `Provisioning dashboard 'TradeFlow — Kafka'`; UI → Dashboards → "TradeFlow — Kafka" appears under the `tradeflow` tag.
- POST 5 trades in a row → lag panel spikes then drains to 0 within ~5 seconds; produce-rate panel shows a 5-record burst.

**Acceptance criteria:**
- [ ] Panel: consumer lag by consumer group (line chart).
- [ ] Panel: messages produced per minute.
- [ ] Added to `monitoring/grafana/provisioning/dashboards/kafka.json`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Grafana provisioning takes a dashboard JSON dropped into
`provisioning/dashboards/`. Build the panels in the Grafana UI first
(easier), then click "Share → Export → Save to file" and commit that file.
PromQL key bits: `kafka_consumer_fetch_manager_records_lag` (lag) and
`rate(kafka_producer_record_send_total[1m])` (send rate).
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. Boot Grafana + Prometheus (Day 8 compose).
2. Create a new dashboard, add two panels:
   - **Consumer lag by group** — `sum(kafka_consumer_fetch_manager_records_lag{topic="trade-events"}) by (client_id)`, legend `{{client_id}}`.
   - **Records produced/sec** — `sum(rate(kafka_producer_record_send_total[1m])) by (client_id)`.
3. Save → Share → Export → "Export for sharing externally" → save JSON.
4. Drop the JSON into `monitoring/grafana/provisioning/dashboards/kafka.json`.
5. Restart Grafana — it should auto-load.
</details>

<details>
<summary>Hint 3 — Config skeleton</summary>

```json
{
  "title": "TradeFlow — Kafka",
  "uid": "tradeflow-kafka",
  "schemaVersion": 38,
  "panels": [
    {
      "id": 1,
      "title": "Consumer lag by group",
      "type": "timeseries",
      "targets": [
        { "expr": "sum(kafka_consumer_fetch_manager_records_lag{topic=\"trade-events\"}) by (client_id)",
          "legendFormat": "{{client_id}}" }
      ]
    }
    // TODO: producer rate panel + DLT stat panel
  ]
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `monitoring/grafana/provisioning/dashboards/kafka.json`

```json
{
  "title": "TradeFlow — Kafka",
  "uid": "tradeflow-kafka",
  "schemaVersion": 38,
  "version": 1,
  "refresh": "10s",
  "time": { "from": "now-30m", "to": "now" },
  "tags": ["tradeflow", "kafka"],
  "panels": [
    {
      "id": 1,
      "title": "Consumer lag by group",
      "type": "timeseries",
      "gridPos": { "x": 0, "y": 0, "w": 24, "h": 8 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [{
        "refId": "A",
        "expr": "sum(kafka_consumer_fetch_manager_records_lag{topic=\"trade-events\"}) by (client_id)",
        "legendFormat": "{{client_id}}"
      }]
    },
    {
      "id": 2,
      "title": "Records produced per second",
      "type": "timeseries",
      "gridPos": { "x": 0, "y": 8, "w": 12, "h": 8 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [{
        "refId": "A",
        "expr": "sum(rate(kafka_producer_record_send_total[1m])) by (client_id)",
        "legendFormat": "{{client_id}}"
      }]
    },
    {
      "id": 3,
      "title": "DLT messages (last 5m)",
      "type": "stat",
      "gridPos": { "x": 12, "y": 8, "w": 12, "h": 8 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "fieldConfig": {
        "defaults": {
          "thresholds": {
            "mode": "absolute",
            "steps": [
              { "color": "green", "value": null },
              { "color": "red", "value": 1 }
            ]
          }
        }
      },
      "targets": [{
        "refId": "A",
        "expr": "sum(increase(kafka_producer_record_send_total{topic=\"trade-events.DLT\"}[5m]))"
      }]
    }
  ]
}
```
</details>

**Files to touch:** `monitoring/grafana/provisioning/dashboards/kafka.json`.

---

### TICKET-I122 — Integration test — round-trip

**What**
- `TradeEventConsumerIT` under `src/test/java` annotated with `@SpringBootTest` + `@EmbeddedKafka(partitions=1, topics={"trade-events","trade-events.DLT"})`, publishing via the real producer and asserting both consumers ran with `@MockBean` + Awaitility.

**Why**
- Embedded Kafka catches the failure mode where everything works locally but `groupId`s collide in the build — the test fails in CI before the bug reaches Day-10's deployed demo.
- `auto-offset-reset=earliest` in the test property source is the one config that makes the difference between a passing test and one that hangs for 30 seconds — explicit in the test so students learn the gotcha.

**Observe**
- `./mvnw test -Dtest=TradeEventConsumerIT` finishes in <30s with both consumer verifications green; log shows `Started EmbeddedKafka broker on port N` then the consumer-group join lines.
- A hung test (no completion in 10s) → check `auto-offset-reset` and that `@DirtiesContext` is preventing broker reuse across classes.

**Acceptance criteria:**
- [ ] `@SpringBootTest` with `EmbeddedKafka`.
- [ ] Publishes a `TradeEvent`, asserts the consumer processes it (use a `CountDownLatch`).

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

`@EmbeddedKafka` boots an in-JVM broker. Mock the downstream services
(`ReconciliationService`, `AuditService`) with `@MockBean`, then
`verify(...)` they were called. `Awaitility.await().untilAsserted(...)`
beats a raw `CountDownLatch` for cleaner failure messages.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. `@SpringBootTest(classes = { KafkaConfig.class, TradeEventProducer.class,
   TradeEventConsumer.class, ReconEventConsumer.class, AuditEventConsumer.class })`.
2. `@EmbeddedKafka(partitions=1, topics={"trade-events","trade-events.DLT"})`.
3. `@TestPropertySource` to point `spring.kafka.bootstrap-servers` at
   `${spring.embedded.kafka.brokers}`.
4. `@MockBean ReconciliationService` and `@MockBean AuditService`.
5. Publish via `producer.publish(...)`.
6. `await().atMost(10, SECONDS).untilAsserted(() -> { verify(...) verify(...); })`.
</details>

<details>
<summary>Hint 3 — Test skeleton</summary>

```java
@SpringBootTest(classes = {
        KafkaConfig.class, TradeEventProducer.class, TradeEventConsumer.class,
        ReconEventConsumer.class, AuditEventConsumer.class })
@EmbeddedKafka(partitions = 1, topics = { "trade-events", "trade-events.DLT" })
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "tradeflow.kafka.topics.trades=trade-events",
        "tradeflow.kafka.topics.dlt=trade-events.DLT",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
@DirtiesContext
class TradeEventConsumerIT {

    @Autowired private TradeEventProducer producer;
    @MockBean   private ReconciliationService reconciliationService;
    @MockBean   private AuditService auditService;

    @Test
    void publishedEvent_isReceivedByBothConsumerGroups() {
        // TODO: build a CREATED event, publish, awaitility-verify both mocks
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/test/java/com/dbtraining/tradeflow/kafka/TradeEventConsumerIT.java`

```java
package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.config.KafkaConfig;
import com.dbtraining.tradeflow.dto.TradeDto;
import com.dbtraining.tradeflow.dto.TradeEvent;
import com.dbtraining.tradeflow.model.TradeStatus;
import com.dbtraining.tradeflow.service.AuditService;
import com.dbtraining.tradeflow.service.ReconciliationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(classes = {
        KafkaConfig.class,
        TradeEventProducer.class,
        TradeEventConsumer.class,
        ReconEventConsumer.class,
        AuditEventConsumer.class
})
@EmbeddedKafka(partitions = 1, topics = { "trade-events", "trade-events.DLT" })
@Import(TradeEventConsumerIT.TestKafkaConfig.class)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "tradeflow.kafka.topics.trades=trade-events",
        "tradeflow.kafka.topics.dlt=trade-events.DLT",
        "spring.kafka.consumer.auto-offset-reset=earliest"
})
@DirtiesContext
class TradeEventConsumerIT {

    @Autowired private TradeEventProducer producer;
    @MockBean   private ReconciliationService reconciliationService;
    @MockBean   private AuditService auditService;

    @Test
    void publishedEvent_isReceivedByBothConsumerGroups() throws InterruptedException {
        TradeEvent event = new TradeEvent(
                "TRD-IT-0001",
                TradeEvent.Action.CREATED,
                Instant.now(),
                samplePayload("TRD-IT-0001"));

        producer.publish(event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(reconciliationService).runForTrade(eq("TRD-IT-0001"));
            verify(auditService).record(any(TradeEvent.class));
        });
    }

    @Test
    void updatedEvent_skipsReconButStillAudits() {
        TradeEvent event = new TradeEvent(
                "TRD-IT-0002",
                TradeEvent.Action.UPDATED,
                Instant.now(),
                samplePayload("TRD-IT-0002"));

        producer.publish(event);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            verify(auditService).record(any(TradeEvent.class));
            verifyNoInteractions(reconciliationService);
        });
    }

    private static TradeDto samplePayload(String tradeRef) {
        return new TradeDto(100L, tradeRef, 1L, 1L,
                new BigDecimal("100"), new BigDecimal("245.50"),
                LocalDate.of(2026, 3, 1), TradeStatus.PENDING, Instant.now());
    }

    @TestConfiguration
    static class TestKafkaConfig {
        @Bean KafkaTemplate<String, TradeEvent> kafkaTemplate(
                ProducerFactory<String, TradeEvent> pf) {
            return new KafkaTemplate<>(pf);
        }
        @Bean ProducerFactory<String, TradeEvent> producerFactory(KafkaProperties props) {
            return new DefaultKafkaProducerFactory<>(props.buildProducerProperties());
        }
        @Bean KafkaTemplate<String, Object> dltKafkaTemplate(KafkaProperties props) {
            return new KafkaTemplate<>(
                    new DefaultKafkaProducerFactory<>(props.buildProducerProperties()));
        }
    }
}
```
</details>

**Files to touch:** `src/test/java/.../kafka/TradeEventConsumerIT.java`.

---

### TICKET-I123 — End-to-end smoke

**What**
- Manual end-to-end run: `docker compose up -d` for infra, `mvnw spring-boot:run` for backend, POST a trade via Swagger, then verify the trace through Kafdrop, backend logs, DB rows, and Grafana — captured as a 60-second screen recording on the PR.

**Why**
- The 1-minute screencast is the artefact the trainer grades during Day-10 demo prep — it's the only ticket that proves the *whole* Day-9 stack works together, not just each piece in isolation.
- Running the negative test (publish malformed JSON, watch it hit DLT) is the only way to *prove* I118's error handler is wired, not just that the code compiles.

**Observe**
- Single POST → three backend log lines within 1s (`Received`, `Reconciling`, `Auditing`), one new row in `recon_results` and one in `audit_log`, one message visible in Kafdrop on `trade-events`, Grafana lag panel spikes and drains.
- Negative path: `echo '{"bad":true}' | kafka-console-producer --topic trade-events` → message lands in `trade-events.DLT` in Kafdrop, recon and audit consumers do not log.

**Acceptance criteria:**
- [ ] Post a trade via Swagger.
- [ ] Kafdrop shows the message in `trade-events`.
- [ ] Recon consumer log: "Reconciled trade TRD-XYZ — status OPEN/MATCHED".
- [ ] `audit_log` table has a new row.
- [ ] Capture this as a 1-minute screen recording in your PR.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Boot everything (`docker compose up -d` for Kafka+Zookeeper+Kafdrop+Postgres,
then `./mvnw spring-boot:run` for the backend). Use Swagger at
`http://localhost:8080/swagger-ui.html` to POST a trade. Open Kafdrop at
`http://localhost:9000` to see the message.
</details>

<details>
<summary>Hint 2 — More guided</summary>

The pipeline you're exercising:
1. POST `/api/v1/trades` → DB insert → `TradeEventProducer.publish(CREATED)`.
2. `trade-events` topic gets one message (Kafdrop tab).
3. `ReconEventConsumer` (recon-group) calls `runForTrade` → row in
   `recon_results`.
4. `AuditEventConsumer` (audit-group) writes one row to `audit_log`.
5. `TradeEventConsumer` (trade-log-group) logs the receipt.

Record a 60-second screencast of all five steps, attach to the PR.
</details>

<details>
<summary>Hint 3 — Verification commands</summary>

```bash
docker compose up -d postgres kafka zookeeper kafdrop
SPRING_PROFILES_ACTIVE=uat ./mvnw spring-boot:run

# In another terminal:
curl -X POST http://localhost:8080/api/v1/trades \
     -H 'Content-Type: application/json' \
     -d '{"instrumentId":1,"counterpartyId":1,"quantity":100,"price":245.50,"tradeDate":"2026-03-15"}'

# Then check:
open http://localhost:9000          # Kafdrop -> trade-events topic, latest msg
open http://localhost:8080/h2-console # SELECT * FROM audit_log;
```
</details>

<details>
<summary>Reference — full walkthrough</summary>

**Step-by-step verification:**

1. **Infra up:**
   ```bash
   docker compose up -d postgres kafka zookeeper kafdrop prometheus grafana
   docker compose ps   # all 'Up'
   ```
2. **Backend up:**
   ```bash
   SPRING_PROFILES_ACTIVE=uat ./mvnw spring-boot:run
   # Log: Started TradeflowApplication
   # Log: 3 consumer subscriptions on trade-events
   ```
3. **POST a trade via Swagger** (`/swagger-ui.html` → POST /api/v1/trades).
4. **Kafdrop** (`http://localhost:9000`) — open `trade-events`, see one
   message; key = `TRD-...`, value = TradeEvent JSON.
5. **Backend logs** — three lines within ~1s:
   - `Received TradeEvent[tradeRef=TRD-..., action=CREATED]` (log consumer)
   - `Reconciling tradeRef=TRD-...` (recon consumer)
   - `Auditing tradeRef=TRD-... action=CREATED` (audit consumer, debug)
6. **DB checks:**
   ```sql
   SELECT * FROM recon_results ORDER BY id DESC LIMIT 1;  -- 1 row
   SELECT * FROM audit_log     ORDER BY id DESC LIMIT 1;  -- 1 row
   ```
7. **Grafana** — `http://localhost:3000` → TradeFlow — Kafka dashboard,
   consumer lag stays near 0, produce rate ticks up.
8. **Negative path — prove I118's DLT works:**
   ```bash
   echo '{"bad":true}' | docker compose exec -T kafka \
     kafka-console-producer --bootstrap-server localhost:9092 --topic trade-events
   ```
   The malformed record lands on `trade-events.DLT` in Kafdrop; recon and
   audit consumers do not log.
9. **Record a 60-second screen capture** showing steps 3 → 8 in order and
   attach to the PR.
</details>

**Files to touch:** none (manual verification + PR artefact).

---

### TICKET-I124 — AI review of Kafka config

**What**
- Paste `application.yml` (spring.kafka block) and `KafkaConfig.java` into Claude/Copilot with the production-readiness prompt; categorise findings, pick ONE actionable gap, ship the fix, and write up the review in `docs/ai-reviews/I124.md`.

**Why**
- Day 1's prompt-engineering teaching was abstract — this is the only ticket where students apply LLM review to *their own* code and must defend why they accepted or rejected each finding.
- Real production Kafka configs usually ship with `enable.idempotence=false` (Spring default) and no pre-declared DLT — both are legit findings an LLM will surface; finding them now saves a Day-10 demo bug.

**Observe**
- `docs/ai-reviews/I124.md` exists with the prompt verbatim, a findings table (≥3 rows categorised "already done" / "out of scope" / "legit + actionable"), and the commit hash of the fix.
- The shipped fix (e.g. `ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG=true`) is visible in `git log -p backend/src/main/java/...KafkaConfig.java` and TICKET-I122's integration test still passes.

**Acceptance criteria:**
- [ ] Prompt: *"Review this Spring Kafka configuration for production
  readiness. Focus on: ack mode, retry strategy, consumer-group rebalance,
  and DLT handling."*
- [ ] Document AT LEAST ONE legitimate finding + your fix in the PR.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

The point of this ticket is to *evaluate* AI output, not to accept it. Most
LLM "findings" on Kafka config are correct-but-irrelevant (e.g. "consider
TLS" when you're on localhost). Find one *actionable* gap and ship the fix.
</details>

<details>
<summary>Hint 2 — More guided</summary>

1. Paste both files into Claude / Copilot Chat with the exact prompt above.
2. Read the response. Categorise each finding as:
   - **Already done** (e.g. `acks=all` — show the line).
   - **Out of scope for bootcamp** (mTLS, ACLs, schema registry).
   - **Legit and actionable** — pick at least one and implement.
3. Common legit findings: missing `enable.idempotence=true` on the producer,
   no `max.poll.interval.ms` set so a slow consumer triggers rebalance, no
   pre-declared DLT topic.
4. Commit the fix in the same PR. Add a `docs/ai-reviews/I124.md` with the
   prompt, the response, and your verdict on each finding.
</details>

<details>
<summary>Hint 3 — Documentation skeleton</summary>

```markdown
# I124 — AI review of Kafka config

## Prompt
> Review this Spring Kafka configuration for production readiness. Focus
> on: ack mode, retry strategy, consumer-group rebalance, and DLT handling.

## Files reviewed
- `application.yml` (spring.kafka block)
- `KafkaConfig.java`

## Findings
| # | Finding | Verdict | Action |
|---|---|---|---|
| 1 | "Add `acks=all` on producer" | Already done | Pointed AI to line N. |
| 2 | "TODO" | "TODO" | "TODO" |
| 3 | "TODO" | "TODO" | "TODO" |

## Fix applied
(commit hash + diff snippet)
```
</details>

<details>
<summary>Reference — full walkthrough</summary>

**File to create:** `docs/ai-reviews/I124.md` (commit alongside the config fix in the same PR)

```markdown
# I124 — AI review of Kafka config

## Prompt
> Review this Spring Kafka configuration for production readiness. Focus
> on: ack mode, retry strategy, consumer-group rebalance, and DLT handling.

## Files reviewed
- `backend/src/main/resources/application.yml` (spring.kafka block)
- `backend/src/main/java/com/dbtraining/tradeflow/config/KafkaConfig.java`

## Findings
| # | Finding | Verdict | Action |
|---|---|---|---|
| 1 | "Set `acks=all` on producer" | Already done | Confirmed line in application.yml. |
| 2 | "Enable `enable.idempotence=true`" | Legit + actionable | Added — see commit XYZ. |
| 3 | "Pre-declare `trade-events.DLT` as a NewTopic bean" | Legit + actionable | Added in KafkaConfig — commit XYZ. |
| 4 | "Add SSL/SASL" | Out of bootcamp scope | Noted in BACKLOG.md. |
| 5 | "Use schema registry" | Out of bootcamp scope | Noted in BACKLOG.md. |

## Fix shipped — finding #2 (enable.idempotence=true)

Idempotent producers prevent duplicate records when a network blip causes
the broker ack to time out and the client retries: the broker dedupes by
producer-id + sequence-number per partition, giving exactly-once delivery
*within a single partition*. Combined with `acks=all` this is the default
production posture; without it, a flaky network during the Day-10 demo
would surface as duplicate audit rows.

```java
// KafkaConfig.java — producer side
@Bean
public ProducerFactory<String, TradeEvent> producerFactory(KafkaProperties props) {
    Map<String, Object> p = new HashMap<>(props.buildProducerProperties());
    p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    return new DefaultKafkaProducerFactory<>(p);
}
``\`

## Fix shipped — finding #3 (pre-declared DLT topic)

```java
@Bean
public NewTopic deadLetterTopic() {
    return TopicBuilder.name("trade-events.DLT").partitions(1).replicas(1).build();
}
``\`

## Regression check
`./mvnw test -Dtest=TradeEventConsumerIT` — both tests still green after
the producer-config change.
```

After committing, link `docs/ai-reviews/I124.md` from the PR description so the reviewer can see the verdict on every finding.
</details>

**Files to touch:** `docs/ai-reviews/I124.md`, plus whatever config file
your chosen fix lives in.

---

## Run and Observe — End of Sprint 8 (Kafka Integration)

You've shipped 13 tickets (I112–I124) and the trade pipeline now flows
through Kafka with three consumer groups, error handling, metrics, and a
Grafana lag panel. Before the instructor checkpoint, prove the round-trip.

**Run:**

> **Terminal #1** — bring Kafka up (from project root)

```bash
docker compose up -d kafka kafdrop
docker compose ps   # confirm both Up (healthy)
```

> **Terminal #2** — backend (from `backend/`)

```bash
./mvnw spring-boot:run
```

**Observe — Kafdrop + backend:**

| URL / Check | What |
|---|---|
| <http://localhost:9000> (Kafdrop) | topic `trade-events` exists; 3 consumer groups visible (trade, recon, audit) |
| backend log | `Received TradeEvent[tradeRef=...]` lines per posted trade |
| <http://localhost:8080/actuator/prometheus> | `grep kafka` shows producer + consumer metrics (records-consumed-total, records-lag) |
| <http://localhost:3000> Grafana → Kafka dashboard | consumer-lag chart visible, lag returns to 0 within seconds |

**Run end-to-end smoke** (after producer + all three consumers wired):

```bash
curl -u trader:trader-pw -X POST http://localhost:8080/api/v1/trades \
  -H "Content-Type: application/json" \
  -d '{"tradeRef":"TRD-SMOKE-1","instrumentId":1,"counterpartyId":1,"quantity":100,"price":250,"tradeDate":"2026-03-15"}'

docker compose logs --tail 20 backend | grep -E "Received|Reconciling|Auditing"
```

Expect 3 lines (one per consumer group: trade, recon, audit).

**Observe — Kafdrop messages:**

| Check | Expected after Sprint 8 |
|---|---|
| `trade-events` topic → Messages tab | key = `tradeRef`, value = TradeEvent JSON with all fields populated |
| Consumer groups view | 3 groups (trade, recon, audit), lag returns to 0 after smoke test |
| `trade-events.DLT` topic | empty unless you deliberately poisoned a message to test I118 |
| Grafana Kafka panel | lag spike during smoke, drains within 5s |

**If something looks wrong:** `kafka` not reachable → Docker not up or
port 9092 collision; consumers not firing → check `@KafkaListener(groupId=...)`
are distinct across I116/I117/I119; DLT filling → check error handler
config in I118; no metrics → confirm `observation-enabled: true` in
`application.yml` and prometheus actuator endpoint exposed.

---

**Instructor checkpoint:** Before you move on, get the instructor to
review your Kafka round-trip (Kafdrop topic, three consumer groups, and
the Grafana lag panel returning to 0).

---

## Sprint 9 — React advanced patterns (Day 9 PM)

The four tickets below apply what was taught on **Day 8 AM** (React
Module 2 — `useReducer` + Context, HOC + Render Props). Day-9 PM is the
project's only application surface for these patterns — without them
they exist only in the labs.

### TICKET-I124A — `BreakContext` + `useReducer` (shared open-breaks count)

**What**
- `frontend/src/context/BreakContext.jsx` exports `<BreakProvider>` + `useBreaks()`; reducer handles `HYDRATE` / `RESOLVE` / `REOPEN`; navbar badge, dashboard StatCard, and the resolve modal all read/write through it.

**Why**
- I124A's React polish makes the realtime Kafka-driven feed visible in the UI demoed Day 10 — when I119's audit consumer fires, the SSE/WebSocket bridge dispatches `HYDRATE` and the badge updates everywhere at once with no refetch.
- Day-8 AM taught `useReducer + Context`; this is the project's *only* application surface for it — without this ticket the pattern stays in the lab.

**Observe**
- React DevTools → Components panel shows `<BreakProvider>` at the top of the tree with `state={openCount: N}`; the badge, dashboard StatCard, and modal all reference the same context node.
- Click resolve in the modal → badge decrements by 1 in the navbar *and* the dashboard StatCard simultaneously, no network roundtrip in the Network tab.

**Acceptance criteria:**
- [ ] `frontend/src/context/BreakContext.jsx` exports `<BreakProvider>` +
  `useBreaks()` custom hook.
- [ ] Reducer handles `HYDRATE` / `RESOLVE` / `REOPEN` actions.
- [ ] `<App>` wraps `<Routes>` in `<BreakProvider>`.
- [ ] Navbar shows a badge with `useBreaks().openCount`; the dashboard
  StatCard reads from the same hook.
- [ ] Resolving a break in the modal dispatches `RESOLVE`; the badge
  decrements without a refetch.

**Files to touch:** `src/context/BreakContext.jsx` (new), `src/App.jsx`,
`src/components/Navbar.jsx`, `src/pages/Dashboard.jsx`,
`src/components/ResolveBreakModal.jsx`.

---

### TICKET-I124B — `ErrorBoundary` per top-level page

**What**
- `frontend/src/components/ErrorBoundary.jsx` class component implementing `getDerivedStateFromError` + `componentDidCatch`; `<App>` wraps each `<Route>` element in its own boundary.

**Why**
- One broken page (e.g. a Kafka SSE parse error in the realtime feed) would otherwise white-screen the whole app on Day-10 demo — per-route boundaries isolate the blast radius to a single tab.
- React 18 still supports error boundaries only as class components; this ticket is the only class component students write all bootcamp, on purpose, so they understand *why* hooks can't catch render errors.

**Observe**
- Drop `throw new Error("boundary test")` into one page component → only that route renders the fallback `<div role="alert">`; navbar + other routes remain interactive.
- Browser console shows `ErrorBoundary caught: Error: boundary test` followed by the component stack from `componentDidCatch`.

**Acceptance criteria:**
- [ ] `frontend/src/components/ErrorBoundary.jsx` is a class with
  `getDerivedStateFromError` + `componentDidCatch`.
- [ ] `<App>` wraps each `<Route>` element in its own `<ErrorBoundary>`
  so one broken page doesn't take the whole app down.
- [ ] Throw a deliberate error in one page → only that page shows the
  fallback; the navbar + other routes still work.

**Files to touch:** `src/components/ErrorBoundary.jsx` (new),
`src/App.jsx`.

---

### TICKET-I124C — `<ResolveBreakModal />` replacing `window.confirm`

**What**
- `frontend/src/components/ResolveBreakModal.jsx` using native `<dialog>` with break details + a resolution-note textarea (min 5 chars); confirm dispatches `BreakContext.RESOLVE` *and* PUTs `/api/v1/recon/{id}/resolve` with optimistic-update rollback on failure.

**Why**
- `window.confirm` blocks the entire JS event loop — incompatible with the Kafka-driven realtime feed because incoming SSE messages queue up behind the modal and arrive in a burst when the user clicks OK.
- Optimistic update + rollback is the pattern Day-10's demo highlights: badge decrements instantly, then rolls back if the backend returns 4xx — `useReducer` makes the rollback a single `dispatch({type:"REOPEN"})`.

**Observe**
- Click resolve → `<dialog>` opens centered, focus jumps to the textarea, Esc closes it, backdrop click closes it.
- Stub the API to return 500 → badge decrements then increments back within 1 second, modal stays open with an inline error toast.

**Acceptance criteria:**
- [ ] `frontend/src/components/ResolveBreakModal.jsx` renders a native
  `<dialog>` with break details + a resolution-note textarea
  (minimum 5 chars).
- [ ] Esc closes the dialog; clicking the backdrop closes it; focus
  trap works.
- [ ] Confirm dispatches `BreakContext.RESOLVE` *and* fires the PUT
  `/api/v1/recon/{id}/resolve` request.
- [ ] On API failure, the optimistic resolve rolls back (`REOPEN`
  action) and the modal shows the error.

**Files to touch:** `src/components/ResolveBreakModal.jsx` (new),
`src/pages/Recon.jsx`.

---

### TICKET-I124D — `withAuditLog` HOC

**What**
- `frontend/src/hoc/withAuditLog.jsx` exporting `(Component, label?) => AuditLoggedComponent` that logs mount once and every render, preserves `displayName` as `withAuditLog(Original)`, and forwards all props.

**Why**
- Day-8 AM taught the HOC pattern as `function: Component → Component`; wrapping `<TradeTable>` with it on Day-9 PM is the only project surface where students prove the pattern works on their own code.
- The console output is also the debugging tool that shows whether I124A's BreakContext is correctly *not* re-rendering the trade table on a resolve (the goal of the optimisation).

**Observe**
- React DevTools → Components shows the wrapped component named `withAuditLog(TradeTable)`, not the bare `TradeTable`.
- Resolve a break → browser console shows `[audit] ResolveBreakModal render` and `[audit] Navbar render`, but NOT `[audit] TradeTable render` — proves the context split is correct.

**Acceptance criteria:**
- [ ] `frontend/src/hoc/withAuditLog.jsx` is a function
  `(Component, label?) → AuditLoggedComponent`.
- [ ] Logs `[audit] <name> mounted` once and `[audit] <name> render` on
  every render.
- [ ] Preserves `displayName` for React DevTools
  (`withAuditLog(Original)`).
- [ ] Forwards all props through unchanged.
- [ ] Wrap `<TradeTable>` (or `<TradeRow>`) with it; resolve a break →
  confirm in the console that only the modal + badge re-render, not
  the trade table.

**Files to touch:** `src/hoc/withAuditLog.jsx` (new),
`src/components/TradeTable.jsx` (or wherever you apply it).

---

> **Note on Day 9 polish (pre-shift):** these four items used to be
> floating "recommended polish" with no I-numbers. They were promoted
> to real tickets in the timetable update so the Day 8 AM React Module 2
> teaching (Context, HOC, Render Props) actually applies to the
> project, not only to the labs.

---

## Sprint 9 walkthrough — React advanced patterns (reference solutions)

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

#### 1. `BreakContext` + `useReducer`

**File to edit:** `frontend/src/context/BreakContext.jsx`

```jsx
import { createContext, useContext, useEffect, useReducer } from "react";

const initial = { openCount: 0, lastUpdated: 0 };

function reducer(state, action) {
  switch (action.type) {
    case "HYDRATE":
      return { openCount: action.count, lastUpdated: Date.now() };
    case "RESOLVE":
      return { openCount: Math.max(0, state.openCount - 1), lastUpdated: Date.now() };
    case "REOPEN":
      return { openCount: state.openCount + 1, lastUpdated: Date.now() };
    default:
      return state;
  }
}

const BreakContext = createContext(null);

export function BreakProvider({ children }) {
  const [state, dispatch] = useReducer(reducer, initial);

  // Hydrate from REST on mount; SSE/WebSocket re-dispatches HYDRATE on push.
  useEffect(() => {
    fetch("/api/v1/recon/breaks/open-count")
      .then(r => r.json())
      .then(({ count }) => dispatch({ type: "HYDRATE", count }))
      .catch(() => { /* swallow — badge stays at 0 */ });
  }, []);

  return (
    <BreakContext.Provider value={{ state, dispatch }}>
      {children}
    </BreakContext.Provider>
  );
}

export function useBreaks() {
  const ctx = useContext(BreakContext);
  if (!ctx) throw new Error("useBreaks must be used inside <BreakProvider>");
  return ctx;
}
```

**Wrap `<App />` once in `src/App.jsx`:**

```jsx
import { BreakProvider } from "./context/BreakContext";

export default function App() {
  return (
    <BreakProvider>
      <Routes>{/* ...routes... */}</Routes>
    </BreakProvider>
  );
}
```

#### 2. `ErrorBoundary`

**File to edit:** `frontend/src/components/ErrorBoundary.jsx`

```jsx
import { Component } from "react";

export class ErrorBoundary extends Component {
  state = { hasError: false, error: null };

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    console.error("ErrorBoundary caught:", error, info);
  }

  render() {
    if (this.state.hasError) {
      return this.props.fallback ?? (
        <div role="alert" style={{ padding: 24 }}>
          <h2>Something broke on this page.</h2>
          <p>Refresh, or jump back to the dashboard.</p>
        </div>
      );
    }
    return this.props.children;
  }
}
```

**Use it per route** in `src/App.jsx`:

```jsx
<Route path="/breaks" element={
  <ErrorBoundary>
    <BreaksPage />
  </ErrorBoundary>
} />
```

#### 3. `ResolveBreakModal` (with focus trap + ESC)

**File to edit:** `frontend/src/components/ResolveBreakModal.jsx`

```jsx
import { useEffect, useRef, useState } from "react";
import { useBreaks } from "../context/BreakContext";

export function ResolveBreakModal({ open, breakId, onClose, onConfirm }) {
  const { dispatch } = useBreaks();
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const dialogRef = useRef(null);

  // ESC closes + focus trap
  useEffect(() => {
    if (!open) return;
    const root = dialogRef.current;
    const focusables = root.querySelectorAll(
      'button, [href], input, textarea, [tabindex]:not([tabindex="-1"])');
    const first = focusables[0];
    const last = focusables[focusables.length - 1];
    first?.focus();

    function onKey(e) {
      if (e.key === "Escape") { onClose(); return; }
      if (e.key === "Tab") {
        if (e.shiftKey && document.activeElement === first) {
          e.preventDefault(); last.focus();
        } else if (!e.shiftKey && document.activeElement === last) {
          e.preventDefault(); first.focus();
        }
      }
    }
    document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  if (!open) return null;

  async function submit() {
    setBusy(true); setError(null);
    dispatch({ type: "RESOLVE" });  // optimistic
    try {
      await onConfirm(reason);
      onClose();
    } catch (err) {
      dispatch({ type: "REOPEN" });  // rollback
      setError(err.message ?? "Resolve failed");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div role="dialog" aria-modal="true" aria-labelledby="rbm-title"
         className="modal-backdrop" ref={dialogRef}
         onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal">
        <h2 id="rbm-title">Resolve break {breakId}</h2>
        <label>
          Resolution reason
          <textarea value={reason} onChange={e => setReason(e.target.value)} rows={4} />
        </label>
        {error && <p role="alert" className="modal-error">{error}</p>}
        <div className="modal-actions">
          <button onClick={onClose} disabled={busy}>Cancel</button>
          <button onClick={submit} disabled={busy || reason.trim().length < 5}>
            {busy ? "Resolving…" : "Confirm resolve"}
          </button>
        </div>
      </div>
    </div>
  );
}
```

#### 4. `withAuditLog` HOC (TICKET-I124D)

**File to edit:** `frontend/src/hoc/withAuditLog.jsx`

```jsx
import { useEffect, useRef } from "react";

export function withAuditLog(Component, label) {
  const name = label ?? Component.displayName ?? Component.name ?? "Component";

  function AuditLoggedComponent(props) {
    const mounted = useRef(false);
    useEffect(() => {
      if (!mounted.current) {
        console.log(`[audit] ${name} mounted`);
        mounted.current = true;
      }
    }, []);
    console.log(`[audit] ${name} render`);
    return <Component {...props} />;
  }

  AuditLoggedComponent.displayName = `withAuditLog(${name})`;
  return AuditLoggedComponent;
}
```

Apply it where the prop-stability story lives — typically `TradeTable.jsx`:

```jsx
import { withAuditLog } from "../hoc/withAuditLog";
function TradeTable({ trades }) { /* ... */ }
export default withAuditLog(TradeTable, "TradeTable");
```

### Verify

- Trigger an exception inside `<BreaksPage />` (`throw new Error("test")`) —
  only that page shows the error UI; the rest of the app still works.
- Click resolve on a break — modal opens with break details, requires reason
  >= 5 chars, ESC closes it.
- Open two tabs; resolve in one — the navbar count updates in both
  (after the Kafka push is wired).
- React DevTools shows `withAuditLog(TradeTable)`; resolving a break logs
  `[audit] Navbar render` but not `[audit] TradeTable render`.

</details>

---

## End-of-day checklist

- [ ] 13 tickets merged.
- [ ] Posting via the React form fires a visible Kafka message in Kafdrop.
- [ ] Audit log has rows from the event consumer.
- [ ] Grafana shows Kafka lag panel.

> **Forward link:** Day 10's CI/CD pipeline ships this Kafka-enabled backend
> image to GHCR. Kafka + Zookeeper + Kafdrop themselves use upstream Docker
> Hub images straight from `docker-compose.yml` (nothing to build), so the
> demo laptop pulls a complete event-streaming stack with one `docker compose
> pull && up`. See [day10-local-cicd.md](../day10/day10-local-cicd.md).

Next: [Day 10 — Docker + CI/CD + Demo](../day10/README.md)
