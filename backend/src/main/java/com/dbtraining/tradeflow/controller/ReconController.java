package com.dbtraining.tradeflow.controller;

import com.dbtraining.tradeflow.dto.ReconSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * ReconController — TICKET-I072 + I073 + I074
 * ============================================================================
 * WHAT:    REST controller for /api/v1/recon.
 * ============================================================================
 *  TICKET-I072: POST /api/v1/recon/run         -> ReconSummary
 *  TICKET-I073: GET  /api/v1/recon/results     -> Page<ReconBreakDto>
 *  TICKET-I074: PUT  /api/v1/recon/{id}/resolve -> 204
 * ============================================================================
 *  Day-0 stub: GET returns empty list so the React Recon page boots clean.
 *  Replace with real DB-backed reads once the schema exists (Day 1) and
 *  the JDBC/JPA layer is in place (Day 4 / Day 5).
 * ============================================================================
 */
@RestController
@RequestMapping("/api/v1/recon")
@Tag(name = "Reconciliation", description = "Run recon + manage breaks")
public class ReconController {

    @Operation(summary = "Trigger a reconciliation run")
    @PostMapping("/run")
    public ReconSummary run() {
        // TODO(TICKET-I072): inject ReconciliationService, call run(), return summary.
        throw new UnsupportedOperationException("TICKET-I072");
    }

    @Operation(summary = "List reconciliation results")
    @GetMapping("/results")
    public List<Map<String, Object>> listResults(
            @RequestParam(required = false, defaultValue = "OPEN") String status) {
        // TODO(TICKET-I073): paginated query via ReconBreakRepository
        //   (or JdbcTemplate JOIN onto `trades` to surface trade_ref).
        //   Day-1 empty list keeps the UI working until you've built recon_breaks.
        return Collections.emptyList();
    }

    @Operation(summary = "Mark a break as resolved")
    @PutMapping("/{id}/resolve")
    public void resolve(@PathVariable Long id) {
        // TODO(TICKET-I074): update status to RESOLVED, set resolved_at, write audit log.
        throw new UnsupportedOperationException("TICKET-I074");
    }
}
