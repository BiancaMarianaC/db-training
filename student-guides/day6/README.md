# Day 6 — REST APIs, Security & Observability

> Theme: **Ship the API + lock it down + measure it.**
> Tickets: **I068 – I085** (18 tickets)
> Module: Spring Boot — Modules 3, 4 + Prometheus/Grafana

By end of day:

- Full REST API for trades + recon.
- Spring Security with basic-auth and 3 roles.
- Prometheus scrapes the app, Grafana shows real charts.
- MockMvc tests cover happy path + auth failures.

---

## Sprint 5 — APIs, Security & Monitoring

### TICKET-I068 — `GET /api/v1/trades` (paginated)

**What**
- Paginated `GET /api/v1/trades` returning `Page<TradeDto>` with optional `status` filter and `@PageableDefault(size=20, sort="tradeDate" DESC)`.

**Why**
- Day 5's Trade entity + repo were inert — today's controller is the first time external callers can touch them, and Day 7's frontend depends on the exact `{content, totalElements, size, number}` envelope for its trade-blotter table.

**Observe**
- `curl -u viewer:viewer-pw 'localhost:8080/api/v1/trades?page=0&size=5'` returns HTTP 200 with `totalElements` present; same URL with no `-u` returns HTTP 401.

**Acceptance criteria:**
- [ ] Query params: `status`, `from`, `to`, `page`, `size`, `sort`.
- [ ] Returns Spring Data `Page<TradeDto>`.
- [ ] Default page size 20, max 100.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Spring auto-wires a `Pageable` parameter on any controller method — you do
not parse `page` / `size` / `sort` by hand. Return a DTO (`TradeDto`), never
the JPA entity directly (lazy-loading inside the JSON serialiser will bite
you). Default the page size via `@PageableDefault(size = 20)` so unbounded
clients can't ask for 10,000 rows in one call.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Shape of the method:

1. `@GetMapping` on `/api/v1/trades` (class-level `@RequestMapping` covers the prefix).
2. Parameters: `@RequestParam(required=false) TradeStatus status`, plus `Pageable pageable`.
3. Annotate the `Pageable` with `@PageableDefault(size = 20, sort = "tradeDate", direction = Sort.Direction.DESC)`.
4. If `status` is null → call `tradeService.findAll(pageable)`; otherwise `findPageByStatus(status, pageable)`.
5. Spring serialises `Page<TradeDto>` to the familiar `{content, totalElements, size, number, ...}` envelope.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
@RestController
@RequestMapping("/api/v1/trades")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @GetMapping
    public Page<TradeDto> list(
            @RequestParam(required = false) TradeStatus status,
            @PageableDefault(size = 20, sort = "tradeDate",
                             direction = Sort.Direction.DESC) Pageable pageable) {
        // TODO: if status != null, return tradeService.findPageByStatus(status, pageable)
        // TODO: else return tradeService.findAll(pageable)
        return null;
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/controller/TradeController.java`

```java
@RestController
@RequestMapping("/api/v1/trades")
@Tag(name = "Trades", description = "Trade management endpoints")
public class TradeController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @Operation(summary = "List trades (paginated, optional status filter)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of trades returned"),
            @ApiResponse(responseCode = "401", description = "Auth missing"),
            @ApiResponse(responseCode = "403", description = "Insufficient role")
    })
    @GetMapping
    public Page<TradeDto> list(
            @RequestParam(required = false) TradeStatus status,
            @PageableDefault(size = 20, sort = "tradeDate",
                             direction = Sort.Direction.DESC) Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("page size must be <= " + MAX_PAGE_SIZE);
        }
        return status != null
                ? tradeService.findPageByStatus(status, pageable)
                : tradeService.findAll(pageable);
    }

    @Operation(summary = "List trades whose tradeDate falls within a range")
    @GetMapping("/by-date")
    public Page<TradeDto> listByDate(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @PageableDefault(size = 20, sort = "tradeDate",
                             direction = Sort.Direction.DESC) Pageable pageable) {
        return tradeService.findByTradeDateBetween(from, to, pageable);
    }
}
```
</details>

**Files to touch:** `controller/TradeController.java`, `dto/TradeDto.java`.

---

### TICKET-I069 — `POST /api/v1/trades`

**What**
- `POST /api/v1/trades` that validates a `TradeRequest` record with `@Valid`, returns `201 Created` + a `Location` header, and rejects bad payloads with `400`.

**Why**
- Day 5 only had GETs — now trades enter the system through HTTP, and I075's global exception handler depends on `MethodArgumentNotValidException` actually firing here for its 400 envelope to be exercised.

**Observe**
- Valid POST returns `HTTP/1.1 201` with `Location: /api/v1/trades/<id>`; posting `{"quantity":-1}` returns `400` with body `{code:"VALIDATION_FAILED", details:{quantity:"..."}}`.

**Acceptance criteria:**
- [ ] Body: `TradeRequest` (validated with `@Valid`).
- [ ] Returns `201 Created` + `Location` header.
- [ ] Returns the saved trade as JSON.
- [ ] Validation errors → `400 Bad Request` with field details.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Bean Validation triggers automatically when the parameter is marked
`@Valid @RequestBody`. The Spring framework then throws
`MethodArgumentNotValidException` — your global exception handler (I075)
converts that into a 400. Use `ResponseEntity.created(URI)` to ship the
`Location` header and `201` status in one call.
</details>

<details>
<summary>Hint 2 — More guided</summary>

The flow:

1. `TradeRequest` is a Java `record` with `@NotBlank`, `@NotNull`, `@Positive` on each field.
2. Controller method: `public ResponseEntity<TradeDto> create(@Valid @RequestBody TradeRequest request)`.
3. Service does the work: `TradeDto saved = tradeService.createTrade(request);`.
4. Return: `ResponseEntity.created(URI.create("/api/v1/trades/" + saved.id())).body(saved);`.
5. Duplicate `tradeRef` → service throws `IllegalStateException` → 409 (handled by I075).
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
public record TradeRequest(
        @NotBlank String tradeRef,
        @NotNull @Positive Long instrumentId,
        @NotNull @Positive Long counterpartyId,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal price,
        @NotNull LocalDate tradeDate
) {}

// In TradeController:
@PostMapping
public ResponseEntity<TradeDto> create(@Valid @RequestBody TradeRequest request) {
    // TODO: delegate to tradeService.createTrade(request)
    // TODO: return ResponseEntity.created(URI...).body(saved)
    return null;
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit:** `backend/src/main/java/com/dbtraining/tradeflow/dto/TradeRequest.java` and `backend/src/main/java/com/dbtraining/tradeflow/controller/TradeController.java`

```java
// TradeRequest.java
public record TradeRequest(
        @NotBlank @Pattern(regexp = "TRD-\\d{4}-\\d{4}") String tradeRef,
        @NotNull @Positive Long instrumentId,
        @NotNull @Positive Long counterpartyId,
        @NotNull @Positive BigDecimal quantity,
        @NotNull @Positive BigDecimal price,
        @NotNull @PastOrPresent LocalDate tradeDate
) {}

// TradeController.java
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
```
</details>

**Files to touch:** `controller/TradeController.java`, `dto/TradeRequest.java`.

---

### TICKET-I070 — `PUT /api/v1/trades/{id}/status`

**What**
- `PUT /api/v1/trades/{id}/status` that flips a single field via a tiny `StatusUpdate` record, returns 404 on missing id, and emits an audit-log entry.

**Why**
- Recon flow (Day 5) needs status transitions to reach `MATCHED`/`UNMATCHED` from outside JPA dirty-checking; Day 9 Grafana's `tradeflow_trades_by_status` pie (I081) only moves when this endpoint fires.

**Observe**
- `PUT .../trades/1/status` with `{"status":"MATCHED"}` returns HTTP 200 with the updated DTO; `.../trades/9999/status` returns HTTP 404 envelope `code:"TRADE_NOT_FOUND"`.

**Acceptance criteria:**
- [ ] Body: `{ "status": "MATCHED" }`.
- [ ] Returns updated trade.
- [ ] 404 if trade not found.
- [ ] Audit-log entry written.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

A status update is partial — not a full PUT of the trade. Keep the body
minimal: a tiny record with a single `status` field. Look up the trade,
guard against transitioning out of a terminal status (`CANCELLED`, `SETTLED`),
then set the new status and let JPA dirty-checking persist the change inside
a `@Transactional` boundary.
</details>

<details>
<summary>Hint 2 — More guided</summary>

The service does the real work; the controller is thin:

1. Define `record StatusUpdate(@NotNull TradeStatus status) {}` (inner type on the controller is fine).
2. `@PutMapping("/{id}/status")` with `@PathVariable Long id` and `@Valid @RequestBody StatusUpdate body`.
3. Delegate to `tradeService.updateStatus(id, body.status())`.
4. Service throws `TradeNotFoundException` → I075 maps to 404.
5. Service throws `IllegalStateException` on terminal-state attempt → I075 maps to 409.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
@PutMapping("/{id}/status")
public TradeDto updateStatus(@PathVariable Long id,
                             @Valid @RequestBody StatusUpdate body) {
    // TODO: return tradeService.updateStatus(id, body.status())
    return null;
}

public record StatusUpdate(@NotNull TradeStatus status) {}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit:** `backend/src/main/java/com/dbtraining/tradeflow/controller/TradeController.java` and `backend/src/main/java/com/dbtraining/tradeflow/service/TradeService.java`

```java
// TradeController.java
@Operation(summary = "Update a trade's status")
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated"),
        @ApiResponse(responseCode = "404", description = "Trade not found"),
        @ApiResponse(responseCode = "409", description = "Cannot transition from terminal status")
})
@PutMapping("/{id}/status")
public ResponseEntity<TradeDto> updateStatus(@PathVariable Long id,
                                             @Valid @RequestBody StatusUpdate body) {
    TradeDto updated = tradeService.updateStatus(id, body.status());
    return ResponseEntity.ok()
            .location(URI.create("/api/v1/trades/" + id))
            .body(updated);
}

public record StatusUpdate(@NotNull TradeStatus status) {}

// TradeService.java
private static final Map<TradeStatus, Set<TradeStatus>> ALLOWED = Map.of(
        TradeStatus.PENDING,   Set.of(TradeStatus.MATCHED, TradeStatus.CANCELLED),
        TradeStatus.MATCHED,   Set.of(TradeStatus.SETTLED, TradeStatus.CANCELLED),
        TradeStatus.UNMATCHED, Set.of(TradeStatus.MATCHED, TradeStatus.CANCELLED)
);

@Transactional
public TradeDto updateStatus(Long id, TradeStatus newStatus) {
    Trade trade = tradeRepository.findById(id)
            .orElseThrow(() -> new TradeNotFoundException("Trade " + id + " not found"));
    if (trade.getStatus().isTerminal()) {
        throw new IllegalStateException(
                "Trade " + id + " is in terminal state " + trade.getStatus());
    }
    Set<TradeStatus> allowed = ALLOWED.getOrDefault(trade.getStatus(), Set.of());
    if (!allowed.contains(newStatus)) {
        throw new IllegalStateException(
                "Illegal transition " + trade.getStatus() + " -> " + newStatus);
    }
    trade.setStatus(newStatus);
    // Audit row is written by the Day-2 DB trigger on UPDATE.
    return TradeDto.from(trade);
}
```
</details>

**Files to touch:** `controller/TradeController.java`.

---

### TICKET-I071 — `DELETE /api/v1/trades/{id}` (soft delete)

**What**
- `DELETE /api/v1/trades/{id}` that sets status to `CANCELLED` (no row delete), writes an audit-log entry, and returns `204 No Content`.

**Why**
- Regulators need a forever-audit-trail — hard DELETE would lose the row's history; Day 5's audit_log table is the destination, and Day 9 trainers will show how the soft-delete keeps `tradeflow_trades_by_status{status="CANCELLED"}` ticking up.

**Observe**
- `curl -i -X DELETE .../trades/1` returns `HTTP/1.1 204`; the subsequent `GET .../trades/1` still returns 200 with `status: "CANCELLED"`.

**Acceptance criteria:**
- [ ] Trade `status` set to `CANCELLED` (no actual row delete).
- [ ] Audit-log entry written.
- [ ] Returns `204 No Content`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Soft delete means the row stays in the database — you only flip a status
flag. That preserves the audit trail (regulators need to see cancelled
trades). Return `204 No Content` because there's no body to send back.
</details>

<details>
<summary>Hint 2 — More guided</summary>

The pattern is symmetrical with the PUT status update:

1. `@DeleteMapping("/{id}")` with `@PathVariable Long id`.
2. Return `ResponseEntity<Void>`.
3. Service looks up the trade, sets `status = CANCELLED`, lets JPA flush.
4. On not-found: `TradeNotFoundException` → I075 maps to 404.
5. Controller returns `ResponseEntity.noContent().build()`.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
@DeleteMapping("/{id}")
public ResponseEntity<Void> softDelete(@PathVariable Long id) {
    // TODO: tradeService.softDelete(id);
    // TODO: return ResponseEntity.noContent().build();
    return null;
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit:** `backend/src/main/java/com/dbtraining/tradeflow/controller/TradeController.java` and `backend/src/main/java/com/dbtraining/tradeflow/service/TradeService.java`

```java
// TradeController.java
@Operation(summary = "Soft-delete a trade (sets status to CANCELLED)")
@ApiResponses({
        @ApiResponse(responseCode = "204", description = "Soft-deleted (idempotent)"),
        @ApiResponse(responseCode = "404", description = "Trade not found"),
        @ApiResponse(responseCode = "409", description = "Trade already SETTLED")
})
@DeleteMapping("/{id}")
public ResponseEntity<Void> softDelete(@PathVariable Long id) {
    tradeService.softDelete(id);
    return ResponseEntity.noContent().build();
}

// TradeService.java
@Transactional
public void softDelete(Long id) {
    Trade trade = tradeRepository.findById(id)
            .orElseThrow(() -> new TradeNotFoundException("Trade " + id + " not found"));
    if (trade.getStatus() == TradeStatus.CANCELLED) {
        return;  // idempotent
    }
    if (trade.getStatus() == TradeStatus.SETTLED) {
        throw new IllegalStateException("Trade " + id + " is SETTLED — cannot cancel");
    }
    trade.setStatus(TradeStatus.CANCELLED);
    // Audit row is written by the Day-2 DB trigger on UPDATE.
}
```
</details>

**Files to touch:** `controller/TradeController.java`.

---

### TICKET-I072 — `POST /api/v1/recon/run`

**What**
- `POST /api/v1/recon/run` that kicks off `ReconciliationService.runForAll()` and returns a `ReconSummary` JSON; only TRADER or ADMIN can call it.

**Why**
- Day 5's reconciliation logic was triggered only by tests — now operators can invoke it from a Postman click or a cron; I079's `tradeflow_recon_run_seconds` timer wraps the body so Day 9 Grafana p95 has data to chart.

**Observe**
- `curl -u trader:trader-pw -X POST .../recon/run` returns HTTP 200 with `{matchedCount, unmatchedCount, breakdownByType}`; same call as viewer returns HTTP 403; `/actuator/prometheus | grep tradeflow_recon_run_seconds_count` is non-zero after one run.

**Acceptance criteria:**
- [ ] Triggers `ReconciliationService.run()`.
- [ ] Returns `ReconSummary` JSON.
- [ ] Only callable by `ROLE_TRADER` or `ROLE_ADMIN`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

This endpoint kicks off a (potentially slow) batch job and reports the
outcome. The role restriction is enforced by `SecurityConfig` (the
`HttpMethod.POST` rule on `/api/v1/**` already restricts to TRADER) — you do
not need `@PreAuthorize` here unless you want belt-and-braces. Wrap the
service call in a Micrometer `Timer` (`tradeflow_recon_run_seconds`) so
Grafana can chart latency.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Two layers:

1. **Controller:** `@PostMapping("/run")` returns `ReconSummary` — Spring serialises the record straight to JSON.
2. **Service:** `runForAll()` queries `recon_breaks` for OPEN/RESOLVED/SUPPRESSED counts, builds a `Map<DiscrepancyType, Integer>`, returns `new ReconSummary(...)`.
3. Wrap the body in `reconRunTimer.record(() -> ...)` for the metric.
4. Make the service method `@Transactional(readOnly = true)`.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
@RestController
@RequestMapping("/api/v1/recon")
public class ReconController {

    private final ReconciliationService reconService;

    public ReconController(ReconciliationService reconService) {
        this.reconService = reconService;
    }

    @PostMapping("/run")
    public ReconSummary run() {
        // TODO: return reconService.runForAll();
        return null;
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit:** `backend/src/main/java/com/dbtraining/tradeflow/controller/ReconController.java` and `backend/src/main/java/com/dbtraining/tradeflow/service/ReconciliationService.java`

```java
// ReconController.java
@RestController
@RequestMapping("/api/v1/recon")
@Tag(name = "Reconciliation", description = "Run recon + manage breaks")
public class ReconController {

    private final ReconciliationService reconService;

    public ReconController(ReconciliationService reconService) {
        this.reconService = reconService;
    }

    @Operation(summary = "Trigger a reconciliation run and return the summary")
    @PostMapping("/run")
    public ReconSummary run() {
        return reconService.runForAll();
    }
}

// ReconciliationService.java
@Transactional(readOnly = true)
public ReconSummary runForAll() {
    return reconRunTimer.record(() -> {
        long matched   = reconResultRepository.countByStatus(ReconResult.Status.RESOLVED);
        long unmatched = reconResultRepository.countByStatus(ReconResult.Status.OPEN);

        Map<DiscrepancyType, Integer> breakdown = new EnumMap<>(DiscrepancyType.class);
        for (DiscrepancyType t : DiscrepancyType.values()) breakdown.put(t, 0);
        for (ReconResult r : reconResultRepository.findByStatus(ReconResult.Status.OPEN)) {
            breakdown.merge(r.getDiscrepancyType(), 1, Integer::sum);
        }

        long total = matched + unmatched;
        return new ReconSummary(total, total, matched, unmatched, breakdown);
    });
}
```

Day-6 keeps authorization in `SecurityConfig` (see I077): `POST /api/v1/**` requires
`hasRole("TRADER")`, so `viewer` calls return 403 without any method-level annotation.
</details>

**Files to touch:** `controller/ReconController.java`.

---

### TICKET-I073 — `GET /api/v1/recon/results?status=OPEN`

**What**
- `GET /api/v1/recon/results` returning a paginated `Page<ReconResultDto>`, filterable by `status` (defaults to `OPEN`) and counterparty.

**Why**
- Same pagination shape as I068 — Day 7's breaks-blotter table consumes the identical envelope; defaulting to OPEN means the human-facing screen shows what humans actually want without query gymnastics.

**Observe**
- `curl -u viewer:viewer-pw '.../recon/results?status=OPEN&page=0&size=10'` returns HTTP 200 with `{content:[...], totalElements:N, size:10, number:0}`.

**Acceptance criteria:**
- [ ] Paginated list of `ReconResult`.
- [ ] Filterable by status and counterparty.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Same pattern as I068 (paginated trades) — `Pageable` is auto-wired, return
`Page<ReconResultDto>`. Default the `status` query param to `OPEN` since
that's what humans care about 95% of the time. The repository returns
JPA entities — map them to a DTO inside the service.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Endpoint structure:

1. `@GetMapping("/results")` returning `Page<ReconResultDto>`.
2. Params: `@RequestParam(required=false, defaultValue="OPEN") String status` + `@PageableDefault(size = 20) Pageable pageable`.
3. Delegate to `reconService.listOpenBreaks(pageable)` (Day-6 surfaces only OPEN; future days can add RESOLVED/SUPPRESSED filters).
4. Service: pull the list of `OPEN` results, map to `ReconResultDto`, return `PageImpl<>(subList, pageable, totalSize)`.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
@GetMapping("/results")
public Page<ReconResultDto> listResults(
        @RequestParam(required = false, defaultValue = "OPEN") String status,
        @PageableDefault(size = 20) Pageable pageable) {
    // TODO: return reconService.listOpenBreaks(pageable);
    return Page.empty();
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit:** `backend/src/main/java/com/dbtraining/tradeflow/controller/ReconController.java` and `backend/src/main/java/com/dbtraining/tradeflow/service/ReconciliationService.java`

```java
// ReconController.java
@Operation(summary = "List recon breaks (paginated; defaults to OPEN)")
@GetMapping("/results")
public Page<ReconResultDto> listResults(
        @RequestParam(required = false, defaultValue = "OPEN") String status,
        @RequestParam(required = false) Long counterpartyId,
        @PageableDefault(size = 20) Pageable pageable) {
    ReconResult.Status parsed = ReconResult.Status.valueOf(status.toUpperCase());
    return reconService.listBreaks(parsed, counterpartyId, pageable);
}

// ReconciliationService.java
@Transactional(readOnly = true)
public Page<ReconResultDto> listBreaks(ReconResult.Status status,
                                       Long counterpartyId,
                                       Pageable pageable) {
    Page<ReconResult> page = (counterpartyId == null)
            ? reconResultRepository.findByStatus(status, pageable)
            : reconResultRepository.findByStatusAndCounterpartyId(status, counterpartyId, pageable);
    return page.map(ReconResultDto::from);
}
```

Add the two derived queries to `ReconResultRepository`:

```java
Page<ReconResult> findByStatus(ReconResult.Status status, Pageable pageable);
Page<ReconResult> findByStatusAndCounterpartyId(ReconResult.Status status,
                                                Long counterpartyId,
                                                Pageable pageable);
```
</details>

**Files to touch:** `controller/ReconController.java`.

---

### TICKET-I074 — `PUT /api/v1/recon/{id}/resolve`

**What**
- `PUT /api/v1/recon/{id}/resolve` that flips `status=RESOLVED`, sets `resolvedAt=now()`, increments `tradeflow_recon_resolved_total`, and is idempotent.

**Why**
- Closing breaks is the whole point of recon — until this exists, the OPEN count just climbs forever; idempotency matters because a flaky frontend retry must not double-count the metric Day 9 charts.

**Observe**
- First `PUT .../recon/1/resolve` returns 204 and bumps `tradeflow_recon_resolved_total` by 1; second call returns 204 with the counter unchanged; `.../recon/9999/resolve` returns 404.

**Acceptance criteria:**
- [ ] Sets `status=RESOLVED`, `resolvedAt=now()`.
- [ ] Audit-log entry written.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

A "resolve" is a state transition on a single recon break. Treat it like
soft-delete from I071: PUT (or PATCH), 204 on success, 404 on missing id.
The operation must be idempotent — calling resolve twice on the same break
should not double-count anything (especially the `tradeflow_recon_resolved_total`
metric).
</details>

<details>
<summary>Hint 2 — More guided</summary>

Service pseudo-code:

1. `findById(id).orElseThrow(TradeNotFoundException)`.
2. If already `RESOLVED` → return (idempotent — no exception, no metric bump).
3. Otherwise call `r.resolve()` (entity helper that sets `status=RESOLVED` + `resolvedAt=now()`).
4. Increment `reconResolvedCounter` (a Micrometer `Counter`).
5. Audit-log row written by Day-2 trigger or explicit in service.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
@PutMapping("/{id}/resolve")
public ResponseEntity<Void> resolve(@PathVariable Long id) {
    // TODO: reconService.resolveBreak(id);
    // TODO: return ResponseEntity.noContent().build();
    return null;
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit:** `backend/src/main/java/com/dbtraining/tradeflow/controller/ReconController.java` and `backend/src/main/java/com/dbtraining/tradeflow/service/ReconciliationService.java`

```java
// ReconController.java
@Operation(summary = "Mark a recon break as RESOLVED")
@ApiResponses({
        @ApiResponse(responseCode = "204", description = "Resolved (idempotent)"),
        @ApiResponse(responseCode = "404", description = "Break not found")
})
@PutMapping("/{id}/resolve")
public ResponseEntity<Void> resolve(@PathVariable Long id) {
    reconService.resolveBreak(id);
    return ResponseEntity.noContent().build();
}

// ReconciliationService.java
@Transactional
public void resolveBreak(Long id) {
    ReconResult r = reconResultRepository.findById(id)
            .orElseThrow(() -> new TradeNotFoundException("Recon break " + id + " not found"));
    if (r.getStatus() == ReconResult.Status.RESOLVED) {
        return;  // idempotent
    }
    r.setStatus(ReconResult.Status.RESOLVED);
    r.setResolvedAt(Instant.now());
    reconResolvedCounter.increment();
    // Audit row is written by the Day-2 DB trigger on UPDATE.
}
```
</details>

---

### TICKET-I075 — Global exception handler

**What**
- `@RestControllerAdvice GlobalExceptionHandler` that converts `TradeNotFoundException`, `MethodArgumentNotValidException`, `IllegalStateException`, `AccessDeniedException`, and the generic catch-all into a `{code, message, details, timestamp, path}` JSON envelope.

**Why**
- Without this, Day 5's exceptions leak as Whitelabel HTML or raw stack traces — Day 7's frontend cannot parse those, and a 500 with a stack trace in production is an information-disclosure incident.

**Observe**
- `curl -i .../trades/9999` returns HTTP 404 with body `{"code":"TRADE_NOT_FOUND",...}`; an uncaught error returns `code:"INTERNAL_ERROR"` (no stack trace in the body) while the server log shows the full stack via `log.error(...)`.

**Acceptance criteria:**
- [ ] `@RestControllerAdvice GlobalExceptionHandler`.
- [ ] Handles: `TradeValidationException`, `TradeNotFoundException`,
  `MethodArgumentNotValidException`, generic `Exception`.
- [ ] Envelope: `{ "code", "message", "timestamp", "path" }`.
- [ ] Never leaks stack traces in production.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

`@RestControllerAdvice` is a `@ControllerAdvice` whose handler methods
implicitly produce JSON (no `@ResponseBody` needed). Order matters:
specific exceptions first, the catch-all `Exception.class` handler last.
The catch-all must log the stack trace server-side (`log.error(..., ex)`)
and return only a generic message to the caller — never echo `ex.getMessage()`
verbatim from a server-side bug.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Build the envelope as a `record ErrorEnvelope(String code, String message,
Map<String,String> details, String timestamp, String path)`. Then:

1. One `@ExceptionHandler(...)` method per exception class you care about.
2. Each returns `ResponseEntity<ErrorEnvelope>` with the right `HttpStatus`.
3. Inject `HttpServletRequest` into each method to fill the `path`.
4. Centralise envelope construction in a private `build(...)` helper.
5. `MethodArgumentNotValidException` is special — pull field errors via `ex.getBindingResult().getFieldErrors()` and stuff them into `details`.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorEnvelope(String code, String message,
                                Map<String,String> details,
                                String timestamp, String path) {}

    @ExceptionHandler(TradeNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> notFound(TradeNotFoundException ex,
                                                  HttpServletRequest req) {
        // TODO: build NOT_FOUND envelope
        return null;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorEnvelope> beanValidation(...) { /* TODO */ }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEnvelope> uncaught(...) { /* TODO: log + generic 500 */ }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/exception/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorEnvelope(String code, String message,
                                Map<String,String> details,
                                String timestamp, String path) {}

    @ExceptionHandler(TradeNotFoundException.class)
    public ResponseEntity<ErrorEnvelope> notFound(TradeNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "TRADE_NOT_FOUND", ex.getMessage(), null, req);
    }

    @ExceptionHandler(TradeValidationException.class)
    public ResponseEntity<ErrorEnvelope> tradeValidation(TradeValidationException ex,
                                                         HttpServletRequest req) {
        String code = "VALIDATION_" + ex.getCode().name();
        return build(HttpStatus.BAD_REQUEST, code, ex.getMessage(), null, req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorEnvelope> beanValidation(MethodArgumentNotValidException ex,
                                                        HttpServletRequest req) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (var fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fe.getField(),
                    fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid");
        }
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "One or more fields are invalid", fieldErrors, req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorEnvelope> dataIntegrity(DataIntegrityViolationException ex,
                                                       HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "DATA_CONFLICT",
                "Unique constraint or referential integrity violation", null, req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorEnvelope> illegalState(IllegalStateException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), null, req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorEnvelope> badArg(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), null, req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorEnvelope> accessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "FORBIDDEN", "Insufficient role for this resource", null, req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorEnvelope> uncaught(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Unexpected error — see server logs", null, req);
    }

    private static ResponseEntity<ErrorEnvelope> build(HttpStatus status, String code,
                                                       String message,
                                                       Map<String,String> details,
                                                       HttpServletRequest req) {
        ErrorEnvelope body = new ErrorEnvelope(code, message, details,
                Instant.now().toString(),
                req != null ? req.getRequestURI() : null);
        return ResponseEntity.status(status).body(body);
    }
}
```
</details>

**Files to touch:** `exception/GlobalExceptionHandler.java`.

---

### TICKET-I076 — Spring Security: basic auth + 3 users

**What**
- `SecurityConfig` exposing 3 BCrypt-hashed in-memory users (`viewer`, `trader`, `admin`), HTTP Basic, STATELESS session policy, and `permitAll()` for `/actuator/health`, `/swagger-ui/**`, `/v3/api-docs/**`.

**Why**
- Day 5's Swagger UI shipped every endpoint wide open — today the same endpoints get locked by Basic auth; Day 7's frontend will read the same 3-role contract to gate buttons; Day 9 Prometheus needs `/actuator/prometheus` left in the permitAll list so scrapes don't 401.

**Observe**
- Boot log no longer prints the `Using generated security password` line; `curl -i .../api/v1/trades` returns 401 with `WWW-Authenticate: Basic`; `curl -i -u viewer:viewer-pw .../api/v1/trades` returns 200.

**Acceptance criteria:**
- [ ] In-memory users: `admin` (ROLE_ADMIN), `trader` (ROLE_TRADER),
  `viewer` (ROLE_VIEWER).
- [ ] BCrypt-hashed passwords.
- [ ] All routes require auth except `/actuator/health` and `/swagger-ui/**`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

For a stateless REST API: HTTP Basic auth + `BCryptPasswordEncoder` for
hashing + `InMemoryUserDetailsManager` for 3 hard-coded users. Disable
CSRF (no cookies / forms involved), set session policy to STATELESS, and
list the genuinely public endpoints (`/actuator/health`, `/swagger-ui/**`,
`/v3/api-docs/**`, `/h2-console/**` in dev) under `permitAll()`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Three Spring beans, one config class:

1. `PasswordEncoder` → `new BCryptPasswordEncoder()`.
2. `InMemoryUserDetailsManager users(PasswordEncoder encoder)` — build 3 `User.withUsername(...).password(encoder.encode(...)).roles(...).build()`.
3. `SecurityFilterChain filterChain(HttpSecurity http)` — disable CSRF, set STATELESS, define `permitAll()` + `authenticated()` matchers, enable `.httpBasic()`.

Notice in the trainer that `trader` has BOTH `VIEWER` and `TRADER` roles, and `admin` has all three. Roles are additive, not hierarchical out of the box.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public InMemoryUserDetailsManager users(PasswordEncoder encoder) {
        UserDetails viewer = User.withUsername("viewer")
                .password(encoder.encode("viewer-pw")).roles("VIEWER").build();
        // TODO: trader (VIEWER + TRADER)
        // TODO: admin  (VIEWER + TRADER + ADMIN)
        return new InMemoryUserDetailsManager(viewer);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/swagger-ui/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(b -> {})
                .build();
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/config/SecurityConfig.java`

```java
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public InMemoryUserDetailsManager users(PasswordEncoder encoder) {
        UserDetails viewer = User.withUsername("viewer")
                .password(encoder.encode("viewer-pw"))
                .roles("VIEWER").build();
        UserDetails trader = User.withUsername("trader")
                .password(encoder.encode("trader-pw"))
                .roles("VIEWER", "TRADER").build();
        UserDetails admin = User.withUsername("admin")
                .password(encoder.encode("admin-pw"))
                .roles("VIEWER", "TRADER", "ADMIN").build();
        return new InMemoryUserDetailsManager(viewer, trader, admin);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // h2-console renders in an iframe on the same origin — allow it in dev.
                .headers(h -> h.frameOptions(f -> f.disable()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/h2-console/**"
                        ).permitAll()
                        // Everything else in /actuator/** is admin-only (extended in I077).
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(b -> {})
                .build();
    }
}
```
</details>

**Files to touch:** `config/SecurityConfig.java`.

---

### TICKET-I077 — Role-based route protection

**What**
- `requestMatchers` rules so GET on `/api/v1/**` requires VIEWER, POST/PUT/DELETE require TRADER, and `/actuator/**` (beyond the permitAll allowlist) requires ADMIN.

**Why**
- I076 only proved "are you logged in"; this proves "are you allowed" — Day 7's UI uses the same role names for button-visibility; without the DELETE matcher the soft-delete from I071 leaks open to viewers.

**Observe**
- `curl -i -u viewer:viewer-pw -X POST .../api/v1/trades` returns 403; `curl -i -u trader:trader-pw .../actuator/metrics` returns 403 while admin gets 200; the matrix (3 users x 4 methods) is reproducible from a shell loop.

**Acceptance criteria:**
- [ ] `GET` endpoints: ROLE_VIEWER or higher.
- [ ] `POST/PUT/DELETE`: ROLE_TRADER or higher.
- [ ] `/actuator/**` (except `/health`): ROLE_ADMIN only.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Route protection by HTTP method + URL prefix lives inside the
`authorizeHttpRequests(...)` block, BEFORE the catch-all
`.anyRequest().authenticated()`. Use `.requestMatchers(HttpMethod.GET,
"/api/v1/**").hasRole("VIEWER")` style — one line per (method, prefix,
role) combo. Spring's `hasRole("X")` checks for `ROLE_X` — no `ROLE_`
prefix needed (the `roles(...)` builder added it for you).
</details>

<details>
<summary>Hint 2 — More guided</summary>

Order rules in the chain — MOST SPECIFIC FIRST:

1. `permitAll()` for open endpoints (`/actuator/health`, swagger, h2-console).
2. `/actuator/**` → `hasRole("ADMIN")` (catches everything not in the permitAll list).
3. `HttpMethod.GET, "/api/v1/**"` → `hasRole("VIEWER")`.
4. `HttpMethod.POST, "/api/v1/**"` → `hasRole("TRADER")`.
5. `HttpMethod.PUT, "/api/v1/**"` → `hasRole("TRADER")`.
6. `HttpMethod.DELETE, "/api/v1/**"` → `hasRole("TRADER")`.
7. `.anyRequest().authenticated()`.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/actuator/health", "/actuator/info",
                     "/actuator/prometheus",
                     "/swagger-ui/**", "/v3/api-docs/**",
                     "/h2-console/**").permitAll()
    .requestMatchers("/actuator/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.GET,    "/api/v1/**").hasRole("VIEWER")
    .requestMatchers(HttpMethod.POST,   "/api/v1/**").hasRole("TRADER")
    // TODO: PUT + DELETE on /api/v1/**
    .anyRequest().authenticated())
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/java/com/dbtraining/tradeflow/config/SecurityConfig.java`

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.frameOptions(f -> f.disable()))
            .authorizeHttpRequests(auth -> auth
                    // Open endpoints (also scraped by Prometheus)
                    .requestMatchers(
                            "/actuator/health",
                            "/actuator/info",
                            "/actuator/prometheus",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**",
                            "/h2-console/**"
                    ).permitAll()

                    // Actuator (beyond health/info/prometheus) — admin only
                    .requestMatchers("/actuator/**").hasRole("ADMIN")

                    // Trade + recon API — role-per-method
                    .requestMatchers(HttpMethod.GET,    "/api/v1/**").hasRole("VIEWER")
                    .requestMatchers(HttpMethod.POST,   "/api/v1/**").hasRole("TRADER")
                    .requestMatchers(HttpMethod.PUT,    "/api/v1/**").hasRole("TRADER")
                    .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasRole("TRADER")

                    .anyRequest().authenticated())
            .httpBasic(b -> {})
            .build();
}
```

Role/method matrix this enforces:

| User    | GET /api | POST/PUT/DELETE /api | /actuator/health | /actuator/metrics |
|---------|----------|----------------------|------------------|-------------------|
| viewer  | 200      | 403                  | 200              | 403               |
| trader  | 200      | 200/201/204          | 200              | 403               |
| admin   | 200      | 200/201/204          | 200              | 200               |
</details>

**Files to touch:** `config/SecurityConfig.java`.

---

### TICKET-I078 — Actuator: expose `/actuator/prometheus`

**What**
- `application.yml` change so `management.endpoints.web.exposure.include` lists `health, info, metrics, prometheus`, with `endpoint.health.show-details: when-authorized` and a common `metrics.tags.application` tag.

**Why**
- Spring Boot exposes only health/info by default — Prometheus needs `/actuator/prometheus` reachable; this is the config step that turns the I079 dependency into a working scrape target Day 9 Grafana can query.

**Observe**
- `curl http://localhost:8080/actuator/prometheus | head -5` returns `# HELP` / `# TYPE` lines; `grep application=` shows every metric carries the common tag.

**Acceptance criteria:**
- [ ] `management.endpoints.web.exposure.include: health,info,metrics,prometheus`.
- [ ] `curl /actuator/prometheus` returns Prometheus-formatted metrics.
- [ ] `jvm_memory_used_bytes`, `http_server_requests_seconds_*` visible.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Spring Boot Actuator ships everything by default but exposes only
`/actuator/health` and `/actuator/info` over HTTP. To expose Prometheus
output you (a) include `micrometer-registry-prometheus` (ticket I079) and
(b) add `prometheus` to the `management.endpoints.web.exposure.include`
list in `application.yml`. Remember to also `permitAll()` the path in
SecurityConfig so the scraper doesn't need credentials.
</details>

<details>
<summary>Hint 2 — More guided</summary>

`application.yml` snippet structure:

1. Top-level `management:` block.
2. `endpoints.web.exposure.include: health, info, metrics, prometheus`.
3. `endpoint.health.show-details: when-authorized` so unauthenticated callers see only `{status: UP}`.
4. `metrics.tags.application: ${spring.application.name}` adds a common tag — Grafana panels filter by it.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        # TODO: include health, info, metrics, prometheus
        include: health, info
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    tags:
      application: ${spring.application.name}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/main/resources/application.yml`

```yaml
# application.yml — TICKET-I078
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    tags:
      application: ${spring.application.name}
  prometheus:
    metrics:
      export:
        step: 10s   # aligns with prometheus.yml scrape_interval: 10s
```
</details>

**Files to touch:** `application.yml`.

---

### TICKET-I079 — `micrometer-registry-prometheus`

**What**
- `micrometer-registry-prometheus` dependency plus a `tradeflow_trades_created_total` Counter inside `TradeService.createTrade(...)` and a `tradeflow_recon_run_seconds` Timer wrapping `ReconciliationService.runForAll()`.

**Why**
- The default JVM/HTTP metrics are generic — these custom meters speak the domain (trades, recon) and feed Day 9's Grafana dashboard panels that the business sponsor cares about (throughput + recon latency p95).

**Observe**
- After one POST + one /recon/run, `curl -s .../actuator/prometheus | grep tradeflow_` shows `tradeflow_trades_created_total 1.0`, `tradeflow_recon_run_seconds_count 1.0`, and `tradeflow_recon_run_seconds_sum <ms>`.

**Acceptance criteria:**
- [ ] Dependency added to `pom.xml`.
- [ ] Custom counter: `tradeflow_trades_created_total` incremented on every POST.
- [ ] Custom timer: `tradeflow_recon_run_seconds` wraps `ReconciliationService.run()`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

`micrometer-registry-prometheus` registers a `PrometheusMeterRegistry`
bean — you get a `MeterRegistry` injection for free, and the Actuator
endpoint serialises in Prometheus format. Build custom meters once
(typically in the service constructor or a `@Bean` method) and store the
reference; bumping them later is just `counter.increment()` or
`timer.record(() -> { ... })`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Three changes:

1. Add the `micrometer-registry-prometheus` dependency to `backend/pom.xml`.
2. Inject `MeterRegistry` into `TradeService` and `ReconciliationService` constructors.
3. In `TradeService`: build a `Counter` named `tradeflow_trades_created_total` in the constructor; call `.increment()` inside `createTrade(...)`.
4. In `ReconciliationService`: build a `Timer` named `tradeflow_recon_run_seconds`; wrap the body of `runForAll()` in `reconRunTimer.record(() -> ...)`.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```xml
<!-- pom.xml -->
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```java
// TradeService.java
private final Counter tradesCreatedCounter;

public TradeService(..., MeterRegistry meterRegistry) {
    // TODO: this.tradesCreatedCounter = Counter.builder("tradeflow_trades_created_total")
    //         .description("...").register(meterRegistry);
}

@Transactional
public TradeDto createTrade(TradeRequest request) {
    // ... existing save logic ...
    // TODO: tradesCreatedCounter.increment();
    return TradeDto.from(saved);
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit:** `backend/pom.xml`, `backend/src/main/java/com/dbtraining/tradeflow/service/TradeService.java`, `backend/src/main/java/com/dbtraining/tradeflow/service/ReconciliationService.java`

```xml
<!-- backend/pom.xml -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```java
// TradeService.java
private final Counter tradesCreatedCounter;

public TradeService(TradeRepository tradeRepository,
                    InstrumentRepository instrumentRepository,
                    CounterpartyRepository counterpartyRepository,
                    TradeEventProducer eventProducer,
                    MeterRegistry meterRegistry) {
    this.tradeRepository        = tradeRepository;
    this.instrumentRepository   = instrumentRepository;
    this.counterpartyRepository = counterpartyRepository;
    this.eventProducer          = eventProducer;
    this.tradesCreatedCounter = Counter.builder("tradeflow_trades_created_total")
            .description("Total trades successfully created via POST /api/v1/trades")
            .register(meterRegistry);
}

@Transactional
public TradeDto createTrade(TradeRequest request) {
    /* ... validation + save ... */
    Trade saved = tradeRepository.save(trade);
    tradesCreatedCounter.increment();
    return TradeDto.from(saved);
}

// ReconciliationService.java
private final Timer reconRunTimer;
private final Counter reconResolvedCounter;

public ReconciliationService(ReconResultRepository reconResultRepository,
                             MeterRegistry meterRegistry) {
    this.reconResultRepository = reconResultRepository;
    this.reconRunTimer = Timer.builder("tradeflow_recon_run_seconds")
            .description("Time taken for a full reconciliation run")
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram()
            .register(meterRegistry);
    this.reconResolvedCounter = Counter.builder("tradeflow_recon_resolved_total")
            .description("Count of recon breaks marked RESOLVED")
            .register(meterRegistry);
}
```
</details>

**Files to touch:** `service/TradeService.java`, `service/ReconciliationService.java`.

---

### TICKET-I080 — Grafana: API dashboard

**What**
- Grafana dashboard JSON at `monitoring/grafana/provisioning/dashboards/api.json` with three panels: per-URI request rate, p95 latency via `histogram_quantile`, and 5xx error rate.

**Why**
- I078/I079 produced raw metrics; this is the first time a non-engineer can read them — the same JSON is what Day 9 Grafana sessions extend and what Day 10's CI/CD picks up when it provisions the demo environment.

**Observe**
- `http://localhost:3000` shows "TradeFlow — API"; while a curl-loop hits `/api/v1/trades`, the request-rate panel ticks above 0 reqps and the p95 panel reads under 100ms.

**Acceptance criteria:**
- [ ] Grafana JSON committed to `monitoring/grafana/provisioning/dashboards/api.json`.
- [ ] Panels:
  - Request rate (`rate(http_server_requests_seconds_count[1m])`)
  - p95 latency (`histogram_quantile(0.95, ...)`)
  - Error rate by endpoint

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Build the dashboard interactively in the Grafana UI first — picking
PromQL queries by guessing the JSON shape will end in tears. Once you're
happy, "Share" → "Export" → "Save to file" (or just copy the JSON from
the editor view). Commit that JSON under
`monitoring/grafana/provisioning/dashboards/api.json` and Grafana will
auto-load it on next boot via the `dashboard.yml` provisioner.
</details>

<details>
<summary>Hint 2 — More guided</summary>

The three core panels and their PromQL:

1. **Request rate**: `sum(rate(http_server_requests_seconds_count{uri=~"/api/.*"}[1m])) by (uri)` — type `timeseries`, unit `reqps`.
2. **p95 latency**: `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri=~"/api/.*"}[5m])) by (le, uri))` — unit `s`.
3. **Error rate**: `sum(rate(http_server_requests_seconds_count{uri=~"/api/.*",status=~"5.."}[1m])) by (uri)`.

Make sure each panel's `datasource.uid` is `"prometheus"` (matches the provisioned datasource).
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```json
{
  "title": "TradeFlow — API",
  "uid": "tradeflow-api",
  "schemaVersion": 38,
  "refresh": "10s",
  "panels": [
    {
      "id": 1,
      "title": "Request rate (req/s)",
      "type": "timeseries",
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [
        { "expr": "sum(rate(http_server_requests_seconds_count{uri=~\"/api/.*\"}[1m])) by (uri)" }
      ]
    }
    // TODO: p95 latency panel
    // TODO: error-rate panel
  ]
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `monitoring/grafana/provisioning/dashboards/api.json`

```json
{
  "title": "TradeFlow — API",
  "uid": "tradeflow-api",
  "schemaVersion": 38,
  "refresh": "10s",
  "time": { "from": "now-15m", "to": "now" },
  "panels": [
    {
      "id": 1, "title": "Request rate (req/s)", "type": "timeseries",
      "gridPos": { "x": 0, "y": 0, "w": 12, "h": 8 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [{
        "refId": "A",
        "expr": "sum(rate(http_server_requests_seconds_count{uri=~\"/api/.*\"}[1m])) by (uri)",
        "legendFormat": "{{uri}}"
      }],
      "fieldConfig": { "defaults": { "unit": "reqps" } }
    },
    {
      "id": 2, "title": "p95 latency (s)", "type": "timeseries",
      "gridPos": { "x": 12, "y": 0, "w": 12, "h": 8 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [{
        "refId": "A",
        "expr": "histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{uri=~\"/api/.*\"}[5m])) by (le, uri))",
        "legendFormat": "p95 {{uri}}"
      }],
      "fieldConfig": { "defaults": { "unit": "s" } }
    },
    {
      "id": 3, "title": "Error rate (5xx)", "type": "timeseries",
      "gridPos": { "x": 0, "y": 8, "w": 24, "h": 8 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [{
        "refId": "A",
        "expr": "sum(rate(http_server_requests_seconds_count{uri=~\"/api/.*\",status=~\"5..\"}[1m])) by (uri)"
      }]
    },
    {
      "id": 4, "title": "JVM heap used (bytes)", "type": "timeseries",
      "gridPos": { "x": 0, "y": 16, "w": 12, "h": 6 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [{
        "refId": "A",
        "expr": "sum(jvm_memory_used_bytes{area=\"heap\"}) by (id)",
        "legendFormat": "{{id}}"
      }],
      "fieldConfig": { "defaults": { "unit": "bytes" } }
    },
    {
      "id": 5, "title": "Trades created (total)", "type": "stat",
      "gridPos": { "x": 12, "y": 16, "w": 12, "h": 6 },
      "datasource": { "type": "prometheus", "uid": "prometheus" },
      "targets": [{
        "refId": "A",
        "expr": "tradeflow_trades_created_total"
      }],
      "fieldConfig": { "defaults": { "unit": "short" } }
    }
  ]
}
```
</details>

**Files to touch:** `monitoring/grafana/provisioning/dashboards/api.json`.

---

### TICKET-I081 — Grafana: trade-status pie chart

**What**
- One Micrometer `Gauge` per `TradeStatus` value (tagged `status=<NAME>`) backed by `TradeRepository.countByStatus(...)`, plus a Grafana pie panel querying `tradeflow_trades_by_status`.

**Why**
- I079's Counter only tells you "how many trades were created"; the gauge is the *current* distribution, which is what business stakeholders and Day 9's morning-standup screenshot actually want; the same metric also feeds Day 10's deploy smoke check.

**Observe**
- `curl -s .../actuator/prometheus | grep tradeflow_trades_by_status` shows one line per status (`status="PENDING"`, `status="MATCHED"`, etc.); the Grafana panel renders a slice per status, slices resize as I070 status updates land.

**Acceptance criteria:**
- [ ] Panel queries `tradeflow_trades_by_status` (define this gauge).
- [ ] Pie chart with one slice per status value.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

A Micrometer `Gauge` reports an instantaneous value — perfect for "how
many trades are in state X right now." Build one gauge per `TradeStatus`,
each tagged `status=<NAME>`. The Grafana pie chart panel then groups
slices by the `status` label automatically.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Two layers:

1. **Service:** loop over `TradeStatus.values()` in the `TradeService` constructor; for each, `Gauge.builder("tradeflow_trades_by_status", repo, r -> (double) r.countByStatus(status)).tag("status", status.name()).register(meterRegistry);`.
2. **Repo:** ensure `TradeRepository.countByStatus(TradeStatus)` exists — Spring Data derives the query from the name.
3. **Dashboard:** add a `piechart` panel querying `tradeflow_trades_by_status` with `legendFormat: "{{status}}"`.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
// TradeService constructor
for (TradeStatus status : TradeStatus.values()) {
    Gauge.builder("tradeflow_trades_by_status",
                    tradeRepository,
                    r -> (double) r.countByStatus(status))
            .description("Live count of trades per status")
            .tag("status", status.name())
            .register(meterRegistry);
}
```

```json
// api.json — pie chart panel
{
  "id": 7, "title": "Trades by status", "type": "piechart",
  "datasource": { "type": "prometheus", "uid": "prometheus" },
  "targets": [
    { "expr": "tradeflow_trades_by_status", "legendFormat": "{{status}}" }
  ]
  // TODO: gridPos + pieType
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**Files to edit:** `backend/src/main/java/com/dbtraining/tradeflow/service/TradeService.java` and `monitoring/grafana/provisioning/dashboards/api.json`

```java
// TradeService.java — inside the constructor (TICKET-I081)
for (TradeStatus status : TradeStatus.values()) {
    Gauge.builder("tradeflow_trades_by_status",
                    tradeRepository,
                    r -> (double) r.countByStatus(status))
            .description("Live count of trades per status")
            .tag("status", status.name())
            .register(meterRegistry);
}
```

```java
// TradeRepository.java
long countByStatus(TradeStatus status);
```

```json
// monitoring/grafana/provisioning/dashboards/api.json — TICKET-I081 panel
{
  "id": 7,
  "title": "Trades by status",
  "type": "piechart",
  "gridPos": { "x": 0, "y": 22, "w": 12, "h": 10 },
  "datasource": { "type": "prometheus", "uid": "prometheus" },
  "targets": [{
    "refId": "A",
    "expr": "tradeflow_trades_by_status",
    "legendFormat": "{{status}}"
  }],
  "options": {
    "legend": { "displayMode": "list", "placement": "right", "showLegend": true },
    "pieType": "donut"
  },
  "fieldConfig": {
    "defaults": { "unit": "short" },
    "overrides": [
      { "matcher": { "id": "byName", "options": "PENDING" },
        "properties": [{ "id": "color", "value": { "mode": "fixed", "fixedColor": "yellow" } }] },
      { "matcher": { "id": "byName", "options": "MATCHED" },
        "properties": [{ "id": "color", "value": { "mode": "fixed", "fixedColor": "green" } }] },
      { "matcher": { "id": "byName", "options": "UNMATCHED" },
        "properties": [{ "id": "color", "value": { "mode": "fixed", "fixedColor": "red" } }] },
      { "matcher": { "id": "byName", "options": "SETTLED" },
        "properties": [{ "id": "color", "value": { "mode": "fixed", "fixedColor": "blue" } }] },
      { "matcher": { "id": "byName", "options": "CANCELLED" },
        "properties": [{ "id": "color", "value": { "mode": "fixed", "fixedColor": "grey" } }] }
    ]
  }
}
```
</details>

**Files to touch:** Add gauge in `TradeService`, panel in `api.json`.

---

### TICKET-I082 — JUnit: `createTrade_validInput_returns201`

**What**
- `@WebMvcTest(TradeController.class)` slice with a `@MockBean TradeService`, `@WithMockUser(roles = "TRADER")`, posting valid JSON and asserting `status().isCreated()` plus the `Location` header.

**Why**
- I069 only proved the happy path with curl during development; this is the regression net for every future change, and it pins the contract Day 7's frontend integration depends on (`201 + Location: /api/v1/trades/{id}`).

**Observe**
- `./mvnw test -Dtest=TradeControllerTest#createTrade_validInput_returns201` is green in under 2 seconds; the test log shows `Status = 201` and `Location = /api/v1/trades/42`.

**Acceptance criteria:**
- [ ] `@WebMvcTest(TradeController.class)`.
- [ ] Mocks `TradeService`.
- [ ] Posts valid JSON, asserts `status().isCreated()`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

`@WebMvcTest` slices the test to just the MVC layer — fast and isolated
from JPA / Kafka / Security beans you don't need. Mock the `TradeService`
with `@MockBean` and stub `createTrade(any())` to return a fixed DTO.
Inject `MockMvc` and post valid JSON; assert status, `Location` header,
and a couple of `jsonPath` fields.
</details>

<details>
<summary>Hint 2 — More guided</summary>

The recipe:

1. `@WebMvcTest(controllers = TradeController.class)`.
2. `@Import(SecurityConfig.class)` so auth rules apply.
3. `@MockBean TradeService tradeService;`.
4. Annotate the test method with `@WithMockUser(roles = "TRADER")`.
5. Stub: `when(tradeService.createTrade(any())).thenReturn(sampleDto(...));`.
6. Perform: `mvc.perform(post("/api/v1/trades").contentType(JSON).content(body)).andExpect(status().isCreated()).andExpect(header().string("Location", "..."));`.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
@WebMvcTest(controllers = TradeController.class)
@Import(SecurityConfig.class)
class TradeControllerTest {

    @Autowired MockMvc mvc;
    @MockBean TradeService tradeService;

    @Test
    @WithMockUser(roles = "TRADER")
    void createTrade_validInput_returns201() throws Exception {
        // TODO: stub when(tradeService.createTrade(any())).thenReturn(sampleDto(42L));
        // TODO: mvc.perform(post(...)).andExpect(status().isCreated());
    }
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/test/java/com/dbtraining/tradeflow/controller/TradeControllerTest.java`

```java
@WebMvcTest(controllers = TradeController.class)
@Import({com.dbtraining.tradeflow.config.SecurityConfig.class,
         com.dbtraining.tradeflow.exception.GlobalExceptionHandler.class})
class TradeControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockBean TradeService tradeService;

    @Test
    @WithMockUser(roles = "TRADER")
    void createTrade_validInput_returns201() throws Exception {
        TradeDto saved = sampleDto(42L, "TRD-NEW-0001");
        when(tradeService.createTrade(any())).thenReturn(saved);

        String body = """
                {
                  "tradeRef": "TRD-NEW-0001",
                  "instrumentId": 1,
                  "counterpartyId": 1,
                  "quantity": 100,
                  "price": 250.50,
                  "tradeDate": "2026-03-01"
                }
                """;

        mvc.perform(post("/api/v1/trades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/trades/42"))
                .andExpect(jsonPath("$.tradeRef").value("TRD-NEW-0001"))
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(tradeService).createTrade(any());
    }

    static TradeDto sampleDto(Long id, String ref) {
        return new TradeDto(id, ref, 1L, 1L,
                new BigDecimal("100"), new BigDecimal("250.50"),
                LocalDate.of(2026, 3, 1), TradeStatus.PENDING, Instant.now());
    }
}
```
</details>

**Files to touch:** `src/test/java/.../controller/TradeControllerTest.java`.

---

### TICKET-I083 — JUnit: `createTrade_invalidInput_returns400`

**What**
- Two MockMvc tests: missing-`quantity` and negative-`quantity`, both asserting HTTP 400 with `$.code == "VALIDATION_FAILED"` and `$.details.quantity` present.

**Why**
- I075's GlobalExceptionHandler is only useful if it actually fires — this test pair pins the envelope shape Day 7's frontend will deserialise; without it a refactor of the handler silently breaks every form on the UI.

**Observe**
- `./mvnw test -Dtest=TradeControllerTest#createTrade_*_returns400` is green; the test report shows the response body contains a `details.quantity` string (no stack trace key).

**Acceptance criteria:**
- [ ] Missing `quantity` → 400.
- [ ] Negative `quantity` → 400.
- [ ] Asserts error envelope shape.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

You're verifying two things: that bean-validation fires, and that
`GlobalExceptionHandler` (I075) shapes the response into the agreed
envelope. Post a deliberately broken JSON body and assert
`status().isBadRequest()` plus `jsonPath("$.code")` equals
`"VALIDATION_FAILED"` and `details.quantity` exists.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Two tests, one shape:

1. `createTrade_missingQuantity_returns400` — body has every field EXCEPT `quantity`.
2. `createTrade_negativeQuantity_returns400` — body has `"quantity": -100`.
3. Both expect: `status 400`, `$.code == "VALIDATION_FAILED"`, `$.details.quantity` exists.
4. No `@MockBean` stubbing needed for these — the request never reaches the service.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
@Test
@WithMockUser(roles = "TRADER")
void createTrade_missingQuantity_returns400() throws Exception {
    String body = """
            { "tradeRef": "TRD-X", "instrumentId": 1, "counterpartyId": 1,
              "price": 250.50, "tradeDate": "2026-03-01" }
            """;
    // TODO: mvc.perform(post...).andExpect(status().isBadRequest())
    //                            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
    //                            .andExpect(jsonPath("$.details.quantity").exists());
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/test/java/com/dbtraining/tradeflow/controller/TradeControllerTest.java`

```java
@Test
@WithMockUser(roles = "TRADER")
void createTrade_missingQuantity_returns400() throws Exception {
    String body = """
            {
              "tradeRef": "TRD-NEW-0002",
              "instrumentId": 1,
              "counterpartyId": 1,
              "price": 250.50,
              "tradeDate": "2026-03-01"
            }
            """;
    mvc.perform(post("/api/v1/trades")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.details.quantity").exists());
}

@Test
@WithMockUser(roles = "TRADER")
void createTrade_negativeQuantity_returns400() throws Exception {
    String body = """
            {
              "tradeRef": "TRD-NEG-0001",
              "instrumentId": 1,
              "counterpartyId": 1,
              "quantity": -100,
              "price": 250.50,
              "tradeDate": "2026-03-01"
            }
            """;
    mvc.perform(post("/api/v1/trades")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.details.quantity",
                    org.hamcrest.Matchers.containsString("must be greater than 0")));
}

@Test
@WithMockUser(roles = "TRADER")
void createTrade_futureTradeDate_returns400() throws Exception {
    String body = """
            {
              "tradeRef": "TRD-FUT-0001",
              "instrumentId": 1,
              "counterpartyId": 1,
              "quantity": 100,
              "price": 250.50,
              "tradeDate": "2099-01-01"
            }
            """;
    mvc.perform(post("/api/v1/trades")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.details.tradeDate").exists());
}
```
</details>

---

### TICKET-I084 — MockMvc: paginated GET

**What**
- MockMvc test stubbing `tradeService.findAll(Pageable)` with a `PageImpl<>(list, pageable, 12)` and asserting `$.content.length() == 3`, `$.totalElements == 12`, `$.size == 5`.

**Why**
- The Spring `Page<>` JSON envelope is one of the easiest contracts to accidentally break (e.g. by returning a `List` for "simplicity") — this test makes that regression loud, and Day 7's pagination controls depend on the field names being stable.

**Observe**
- `./mvnw test -Dtest=TradeControllerTest#list_*` is green; the second test (`list_withStatusFilter_delegatesToFilteredFinder`) shows `verify(tradeService).findPageByStatus(...)` succeeding.

**Acceptance criteria:**
- [ ] Test pagination: `?page=0&size=5` returns 5 items + `totalElements`.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Stub the service to return a `Page<TradeDto>` (use `PageImpl<>(list,
pageable, totalElementsAcrossAllPages)`). MockMvc serialises that page into
the standard envelope — your assertions then inspect `$.content` (array),
`$.totalElements`, `$.size`, `$.number`.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Recipe:

1. Build `Pageable pageable = PageRequest.of(0, 5);`.
2. Build `Page<TradeDto> page = new PageImpl<>(List.of(...3 dtos...), pageable, 12);` — 12 is total across all pages.
3. `when(tradeService.findAll(any(Pageable.class))).thenReturn(page);`.
4. Perform `get("/api/v1/trades?page=0&size=5")`.
5. Assert `content.length() == 3`, `totalElements == 12`, `size == 5`.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```java
@Test
@WithMockUser(roles = "VIEWER")
void list_paginated_returnsPageEnvelope() throws Exception {
    Pageable pageable = PageRequest.of(0, 5);
    // TODO: build a Page<TradeDto> with 3 dtos + totalElements = 12
    // TODO: when(tradeService.findAll(any(Pageable.class))).thenReturn(page);
    // TODO: mvc.perform(get("/api/v1/trades?page=0&size=5"))
    //          .andExpect(status().isOk())
    //          .andExpect(jsonPath("$.content.length()").value(3));
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `backend/src/test/java/com/dbtraining/tradeflow/controller/TradeControllerTest.java`

```java
@Test
@WithMockUser(roles = "VIEWER")
void list_paginated_returnsPageEnvelope() throws Exception {
    Pageable pageable = PageRequest.of(0, 5);
    Page<TradeDto> page = new PageImpl<>(
            List.of(sampleDto(1L, "TRD-1"),
                    sampleDto(2L, "TRD-2"),
                    sampleDto(3L, "TRD-3")),
            pageable, 12);
    when(tradeService.findAll(any(Pageable.class))).thenReturn(page);

    mvc.perform(get("/api/v1/trades?page=0&size=5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(3))
            .andExpect(jsonPath("$.totalElements").value(12))
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.totalPages").value(3));
}

@Test
@WithMockUser(roles = "VIEWER")
void list_withStatusFilter_delegatesToFilteredFinder() throws Exception {
    when(tradeService.findPageByStatus(eq(TradeStatus.UNMATCHED), any()))
            .thenReturn(Page.empty());

    mvc.perform(get("/api/v1/trades?status=UNMATCHED"))
            .andExpect(status().isOk());

    verify(tradeService).findPageByStatus(eq(TradeStatus.UNMATCHED), any());
}

@Test
void list_withoutAuth_returns401() throws Exception {
    mvc.perform(get("/api/v1/trades"))
            .andExpect(status().isUnauthorized());
}

@Test
@WithMockUser(roles = "VIEWER")
void viewer_cannotPost_returns403() throws Exception {
    String body = """
            {
              "tradeRef": "TRD-RO-0001",
              "instrumentId": 1,
              "counterpartyId": 1,
              "quantity": 1,
              "price": 1,
              "tradeDate": "2026-03-01"
            }
            """;
    mvc.perform(post("/api/v1/trades")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isForbidden());
}
```
</details>

---

### TICKET-I085 — Postman collection

**What**
- `postman/tradeflow.postman_collection.json` covering Health, Trades, and Recon folders, using collection-level Basic auth via `{{username}}`/`{{password}}` variables and `pm.test` assertions on status + JSON shape.

**Why**
- Every other ticket today was verified by a one-off curl — the collection is the repeatable acceptance gate (run as Newman in Day 10's CI) and the artefact future cohorts inherit when they demo against a fresh checkout.

**Observe**
- `npx newman run postman/tradeflow.postman_collection.json --env-var username=trader --env-var password=trader-pw` exits 0 with every request green; flipping to `username=viewer` turns POST/PUT/DELETE rows red (403).

**Acceptance criteria:**
- [ ] `postman/tradeflow.postman_collection.json` covers every endpoint.
- [ ] Auth header set via collection-level variable.
- [ ] Tests assert status codes + JSON shape.

**Hints — progressive (open only what you need):**

<details>
<summary>Hint 1 — Basic nudge</summary>

Build the collection inside Postman first, then "Export". Use a
collection-level `auth` block with Basic Auth and `{{username}}` /
`{{password}}` variables — that way running the collection as `viewer`
vs `trader` vs `admin` is a 1-line variable change. Each request gets a
small `test` script asserting status code + key JSON fields.
</details>

<details>
<summary>Hint 2 — More guided</summary>

Collection structure:

1. Top-level `auth.basic` with `{{username}} / {{password}}`.
2. Top-level `variable` block: `baseUrl`, `username`, `password`, `tradeId`, `breakId`.
3. Folders: `Health`, `Trades`, `Recon`.
4. Each request: `url` uses `{{baseUrl}}/...`, `event[].script` asserts `pm.response.to.have.status(...)` + a JSON-shape assertion.
</details>

<details>
<summary>Hint 3 — Code skeleton</summary>

```json
{
  "info": { "name": "TradeFlow API",
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json" },
  "auth": { "type": "basic",
            "basic": [
              { "key": "username", "value": "{{username}}", "type": "string" },
              { "key": "password", "value": "{{password}}", "type": "string" }
            ] },
  "variable": [
    { "key": "baseUrl",  "value": "http://localhost:8080" },
    { "key": "username", "value": "trader" },
    { "key": "password", "value": "trader-pw" }
  ],
  "item": [
    /* TODO: Health folder (GET /actuator/health) */
    /* TODO: Trades folder (GET/POST/PUT/DELETE on /api/v1/trades) */
    /* TODO: Recon folder (POST /run, GET /results, PUT /resolve) */
  ]
}
```
</details>

<details>
<summary>Reference Solution — complete, copy-paste ready</summary>

**File to edit:** `postman/tradeflow.postman_collection.json`

```json
{
  "info": {
    "name": "TradeFlow API",
    "description": "TICKET-I085 — Postman collection covering all REST endpoints with auth + assertion tests.",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "auth": {
    "type": "basic",
    "basic": [
      { "key": "username", "value": "{{username}}", "type": "string" },
      { "key": "password", "value": "{{password}}", "type": "string" }
    ]
  },
  "variable": [
    { "key": "baseUrl",   "value": "http://localhost:8080" },
    { "key": "username",  "value": "trader" },
    { "key": "password",  "value": "trader-pw" },
    { "key": "tradeId",   "value": "1" },
    { "key": "breakId",   "value": "1" }
  ],
  "item": [
    {
      "name": "Health",
      "item": [
        {
          "name": "GET /actuator/health",
          "request": { "method": "GET",
                       "url": { "raw": "{{baseUrl}}/actuator/health" } },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "pm.test('200', () => pm.response.to.have.status(200));",
              "pm.test('status UP', () => pm.expect(pm.response.json().status).to.eql('UP'));"
            ]}
          }]
        }
      ]
    },
    {
      "name": "Trades",
      "item": [
        {
          "name": "GET /api/v1/trades (paginated)",
          "request": { "method": "GET",
                       "url": { "raw": "{{baseUrl}}/api/v1/trades?page=0&size=20" } },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "pm.test('200', () => pm.response.to.have.status(200));",
              "pm.test('page envelope', () => {",
              "  const body = pm.response.json();",
              "  pm.expect(body).to.have.property('content');",
              "  pm.expect(body).to.have.property('totalElements');",
              "});"
            ]}
          }]
        },
        {
          "name": "POST /api/v1/trades (happy path)",
          "request": {
            "method": "POST",
            "header": [{ "key": "Content-Type", "value": "application/json" }],
            "url": { "raw": "{{baseUrl}}/api/v1/trades" },
            "body": { "mode": "raw",
                      "raw": "{\n  \"tradeRef\": \"TRD-2026-{{$timestamp}}\",\n  \"instrumentId\": 1,\n  \"counterpartyId\": 1,\n  \"quantity\": 1000,\n  \"price\": 245.50,\n  \"tradeDate\": \"2026-03-15\"\n}" }
          },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "pm.test('201', () => pm.response.to.have.status(201));",
              "pm.test('Location header', () => pm.expect(pm.response.headers.get('Location')).to.match(/\\/api\\/v1\\/trades\\/\\d+/));",
              "const loc = pm.response.headers.get('Location');",
              "if (loc) pm.collectionVariables.set('tradeId', loc.split('/').pop());"
            ]}
          }]
        },
        {
          "name": "PUT /api/v1/trades/{{tradeId}}/status",
          "request": {
            "method": "PUT",
            "header": [{ "key": "Content-Type", "value": "application/json" }],
            "url": { "raw": "{{baseUrl}}/api/v1/trades/{{tradeId}}/status" },
            "body": { "mode": "raw", "raw": "{ \"status\": \"MATCHED\" }" }
          },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "pm.test('200', () => pm.response.to.have.status(200));",
              "pm.test('status flipped', () => pm.expect(pm.response.json().status).to.eql('MATCHED'));"
            ]}
          }]
        },
        {
          "name": "DELETE /api/v1/trades/{{tradeId}}",
          "request": {
            "method": "DELETE",
            "url": { "raw": "{{baseUrl}}/api/v1/trades/{{tradeId}}" }
          },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "pm.test('204', () => pm.response.to.have.status(204));"
            ]}
          }]
        }
      ]
    },
    {
      "name": "Recon",
      "item": [
        {
          "name": "POST /api/v1/recon/run",
          "request": { "method": "POST",
                       "url": { "raw": "{{baseUrl}}/api/v1/recon/run" } },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "pm.test('200', () => pm.response.to.have.status(200));",
              "pm.test('summary shape', () => {",
              "  const b = pm.response.json();",
              "  pm.expect(b).to.have.property('matchedCount');",
              "  pm.expect(b).to.have.property('unmatchedCount');",
              "  pm.expect(b).to.have.property('breakdownByType');",
              "});"
            ]}
          }]
        },
        {
          "name": "GET /api/v1/recon/results?status=OPEN",
          "request": { "method": "GET",
                       "url": { "raw": "{{baseUrl}}/api/v1/recon/results?status=OPEN&page=0&size=20" } },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "pm.test('200', () => pm.response.to.have.status(200));",
              "pm.test('page envelope', () => pm.expect(pm.response.json()).to.have.property('content'));"
            ]}
          }]
        },
        {
          "name": "PUT /api/v1/recon/{{breakId}}/resolve",
          "request": { "method": "PUT",
                       "url": { "raw": "{{baseUrl}}/api/v1/recon/{{breakId}}/resolve" } },
          "event": [{
            "listen": "test",
            "script": { "exec": [
              "pm.test('204', () => pm.response.to.have.status(204));"
            ]}
          }]
        }
      ]
    }
  ]
}
```

Run via Newman in CI:

```bash
npx newman run postman/tradeflow.postman_collection.json \
    --env-var username=trader --env-var password=trader-pw
```
</details>

**Files to touch:** `postman/`.

---

## Run and Observe — End of Sprint 5 (REST + Security + Monitoring + Tests)

You've shipped 18 tickets (I068-I085): 7 REST endpoints, a global exception handler, basic auth with 3 users, role-based protection, Actuator + Prometheus metrics, two Grafana dashboards, MockMvc tests, and a Postman collection. Before the instructor checkpoint, prove the whole API works end-to-end with auth, validation, and metrics.

**Run:**

> **Terminal #1** — from `tradeflow-studentscopy/backend/`

```bash
# Ctrl+C if running, then:
./mvnw spring-boot:run
```

> **Terminal #2** — from repo root, for curl + the monitoring stack:

```bash
docker compose up -d prometheus grafana
```

Boot log should show Spring Security initialised (look for `Using generated security password` suppressed because you defined 3 users), the 7 controller mappings registered, and `/actuator/prometheus` exposed.

**Observe:**

| Check | Expected after Sprint 5 |
|---|---|
| Boot log lists `RequestMappingHandlerMapping` entries for `/api/v1/trades`, `/api/v1/recon/*` | 7 endpoints registered (GET/POST/PUT/DELETE on trades, POST/GET/PUT on recon) |
| `./mvnw test` | All MockMvc + JUnit tests green, including `createTrade_validInput_returns201` and `createTrade_invalidInput_returns400` |
| `curl -s http://localhost:8080/actuator/health` | `{"status":"UP"}` (no auth required on `/actuator/health`) |
| `curl -s -u admin:admin-pw http://localhost:8080/actuator/prometheus \| grep http_server_requests` | Several `http_server_requests_seconds_count{...}` lines, one per endpoint hit |
| Prometheus `/targets` page | `backend:8080` target is `UP`, last scrape < 15s ago |

**Browser / curl checks — happy path with correct auth:**

```bash
# 1. Trader can list trades (paginated)
curl -u trader:trader-pw http://localhost:8080/api/v1/trades?page=0&size=5
# Expected: 200 + JSON page object with content[], totalElements, etc.

# 2. Trader can create a trade
curl -u trader:trader-pw -X POST http://localhost:8080/api/v1/trades \
  -H 'Content-Type: application/json' \
  -d '{"tradeRef":"T-OBS-1","instrumentId":1,"counterpartyId":1,"quantity":100,"price":50.0,"tradeDate":"2026-06-12","status":"PENDING"}'
# Expected: 201 Created + Location header

# 3. Trader can update status
curl -u trader:trader-pw -X PUT http://localhost:8080/api/v1/trades/1/status \
  -H 'Content-Type: application/json' -d '{"status":"MATCHED"}'
# Expected: 200 + updated trade

# 4. Recon run
curl -u admin:admin-pw -X POST http://localhost:8080/api/v1/recon/run
# Expected: 202 Accepted (or 200 with summary)

# 5. Viewer can read but not write
curl -u viewer:viewer-pw http://localhost:8080/api/v1/trades
# Expected: 200 + JSON
```

| URL | What |
|---|---|
| <http://localhost:8080/swagger-ui.html> | All 7 endpoints listed under `Trades` + `Recon` tags |
| <http://localhost:8080/actuator/prometheus> | Prometheus text format, `http_server_requests_seconds_*` series present |
| <http://localhost:9090/targets> | Prometheus scrapes `backend:8080` as `UP` |
| <http://localhost:9090/graph?g0.expr=rate(http_server_requests_seconds_count%5B1m%5D)> | Per-endpoint request-rate graph |
| <http://localhost:3000> | Grafana (admin/admin) — API dashboard + trade-status pie chart both render |

**Negative tests — prove security + validation + exception handling actually fire:**

```bash
# 1. Unauthenticated request to protected endpoint
curl -i http://localhost:8080/api/v1/trades
# Expected: 401 Unauthorized + WWW-Authenticate: Basic realm=...

# 2. Wrong password
curl -i -u trader:wrong http://localhost:8080/api/v1/trades
# Expected: 401 Unauthorized

# 3. Viewer (read-only) tries to POST — role-based protection
curl -i -u viewer:viewer-pw -X POST http://localhost:8080/api/v1/trades \
  -H 'Content-Type: application/json' -d '{}'
# Expected: 403 Forbidden

# 4. Invalid payload — global exception handler converts validation error
curl -i -u trader:trader-pw -X POST http://localhost:8080/api/v1/trades \
  -H 'Content-Type: application/json' \
  -d '{"tradeRef":"","quantity":-1}'
# Expected: 400 Bad Request + JSON error body with field-level messages (NOT a stack trace)

# 5. Trade not found — global exception handler converts to 404
curl -i -u trader:trader-pw http://localhost:8080/api/v1/trades/999999
# Expected: 404 Not Found + JSON error body
```

If all five negative cases return the documented status codes with JSON error bodies (not HTML error pages or stack traces), your security chain + `@RestControllerAdvice` are correctly wired.

**Postman / Newman smoke:**

```bash
npx newman run postman/tradeflow.postman_collection.json \
  --env-var username=trader --env-var password=trader-pw
# Expected: every request green, all pm.test assertions pass
```

Re-run with `--env-var username=viewer --env-var password=viewer-pw` and the POST/PUT/DELETE rows should turn red (403) — that proves auth + RBAC end-to-end.

**If something looks wrong:**
- 401 on every request (even with correct user/pw) → re-read I076 hints (password encoder mismatch is the usual cause).
- 403 where you expect 200 → re-read I077 (role names need the `ROLE_` prefix in `hasRole(...)` vs `hasAuthority(...)`).
- `/actuator/prometheus` returns 404 → re-read I078 (`management.endpoints.web.exposure.include` config).
- Prometheus target shows `DOWN` → see [`day06-prometheus-grafana.md`](./day06-prometheus-grafana.md) §6 (scrape config + Docker network name).
- Grafana panel shows "No data" → confirm the datasource UID matches the dashboard JSON, then check the PromQL in I080/I081 hints.

---

**Instructor checkpoint:** Before you start the Prometheus + Grafana hands-on lab, get the instructor to review your running API (Swagger UI + a green Newman run + Prometheus `UP` target).

---

## Prometheus + Grafana hands-on (Day 6 PM, 1.5 hrs)

- Bring up Prometheus + Grafana from `docker-compose.yml`.
- Confirm Prometheus targets list shows `backend:8080` as `UP`.
- Build the Grafana dashboard in the UI, then export to `api.json`.

This is integrated into tickets I078-I081 — no extra ticket.

**Full step-by-step walkthrough:** [day06-prometheus-grafana.md](./day06-prometheus-grafana.md) —
1.5 hr guided lab with code, PromQL, pitfalls, and a common-mistakes field guide.

<details>
<summary>Reference — full walkthrough</summary>

### Steps

1. **Expose the Prometheus endpoint** in Spring Boot.
2. **Add the Prometheus + Grafana services** to `docker-compose.yml`.
3. **Write `monitoring/prometheus/prometheus.yml`** with a scrape job pointing at `backend`.
4. **Boot the stack** and verify the target is `UP`.
5. **Build the dashboard** in Grafana UI, then export JSON to `monitoring/grafana/dashboards/api.json`.

### 90% solution

**`backend/pom.xml`** — expose `/actuator/prometheus`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**`application.yml`** — match the reference config:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    tags:
      application: ${spring.application.name}
```

**`monitoring/prometheus/prometheus.yml`** (note the subfolder):

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'prometheus-self'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'recon-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      # Inside docker-compose, host is `backend`. Outside (local dev),
      # use `host.docker.internal:8080`.
      - targets: ['backend:8080']
```

**`docker-compose.yml`** — add services:

```yaml
  prometheus:
    image: prom/prometheus:latest
    container_name: tradeflow-prometheus
    depends_on:
      - backend
    ports:
      - "9090:9090"
    volumes:
      - ./monitoring/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro

  grafana:
    image: grafana/grafana:latest
    container_name: tradeflow-grafana
    depends_on:
      - prometheus
    ports:
      - "3000:3000"
    environment:
      GF_SECURITY_ADMIN_USER: ${GRAFANA_USER:-admin}
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_PASSWORD:-admin}
    volumes:
      - ./monitoring/grafana/provisioning:/etc/grafana/provisioning:ro
```

**`monitoring/grafana/provisioning/datasources/prometheus.yml`** — auto-wire Prometheus:

```yaml
apiVersion: 1
datasources:
  - name: prometheus
    uid: prometheus
    type: prometheus
    access: proxy
    url: http://prometheus:9090
    isDefault: true
```

**`monitoring/grafana/provisioning/dashboards/dashboard.yml`** — load JSON dashboards:

```yaml
apiVersion: 1
providers:
  - name: tradeflow
    folder: TradeFlow
    type: file
    options:
      path: /etc/grafana/provisioning/dashboards
```

Drop the `api.json` from I080/I081 alongside `dashboard.yml` and Grafana picks it up on boot.

### Verify

```bash
docker compose up -d prometheus grafana
curl http://localhost:8080/actuator/prometheus | head -20      # backend exposes metrics
open http://localhost:9090/targets                              # recon-service = UP
open http://localhost:3000                                      # admin / admin
```

If Prometheus shows the target as DOWN — check the `targets:` host. Inside
the docker network it's `backend:8080`, NOT `localhost:8080`.

</details>

> **Forward link:** Day 10's CI/CD pipeline pushes this same backend image
> (with `/actuator/prometheus` exposed) to GHCR, so the demo laptop pulls
> exactly what Prometheus scraped in CI. See [day10-local-cicd.md](../day10/day10-local-cicd.md).

---

## End-of-day checklist

- [ ] 18 tickets merged.
- [ ] Auth: `viewer` can read trades, `trader` can write, `admin` reads `/actuator`.
- [ ] Grafana shows live request rate while you hit endpoints from Postman.
- [ ] Postman collection green for all routes.

Next: [Day 7 — HTML + CSS dashboard](../day7/README.md)
