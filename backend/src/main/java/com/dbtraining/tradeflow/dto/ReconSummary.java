package com.dbtraining.tradeflow.dto;

import com.dbtraining.tradeflow.model.DiscrepancyType;

import java.util.Map;

/**
 * ============================================================================
 * ReconSummary — TICKET-I036
 * ============================================================================
 * WHAT:    Rolled-up output of a reconciliation run.
 * WHY:     Both the console (Day 3) and REST API (Day 6 `POST /api/v1/recon/run`)
 *          return this — same shape, two surfaces.
 * ============================================================================
 *
 *  HINTS:
 *  - `totalInternal`, `totalExternal` = sizes of the input lists.
 *  - `matchedCount` = how many trades matched cleanly.
 *  - `unmatchedCount` = how many had at least one discrepancy.
 *  - `breakdownByType` = how many of each DiscrepancyType.
 * ============================================================================
 */
public record ReconSummary(
        int totalInternal,
        int totalExternal,
        int matchedCount,
        int unmatchedCount,
        Map<DiscrepancyType, Integer> breakdownByType
) {
}
