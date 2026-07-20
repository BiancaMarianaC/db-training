package com.dbtraining.tradeflow.model;

/**
 * ============================================================================
 * DiscrepancyType — TICKET-I021
 * ============================================================================
 * WHAT:    The reason code for a reconciliation break.
 * WHY:     Ops users need to know WHY a trade broke — not just that it did.
 * OBSERVE: Day 3's ReconciliationService.matchTrades() decides which
 *          discrepancy type to flag based on field-by-field comparison.
 * ============================================================================
 */
public enum DiscrepancyType {

    PRICE_MISMATCH,
    QUANTITY_MISMATCH,
    DATE_MISMATCH,
    MISSING_TRADE;

    /**
     * TODO(TICKET-I021): implement describe() — return a short
     * human-readable string for the UI / CSV export.
     *
     * Examples:
     *   PRICE_MISMATCH    -> "Price does not match counterparty record"
     *   MISSING_TRADE     -> "Trade exists internally but not externally"
     */
    public String describe() {
        // HINT: switch expression on `this`.
        throw new UnsupportedOperationException("TICKET-I021: implement DiscrepancyType.describe()");
    }
}
