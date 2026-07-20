package com.dbtraining.tradeflow.model;

/**
 * ============================================================================
 * AssetClass — TICKET-I020
 * ============================================================================
 * WHAT:    Category of financial instrument.
 * WHY:     Drives subclass choice (EquityTrade vs FXTrade vs BondTrade) and
 *          downstream routing (FX trades go to a different settlement system).
 * OBSERVE: Used in @Enumerated(EnumType.STRING) on the Instrument entity.
 * ============================================================================
 */
public enum AssetClass {

    EQUITY,
    FIXED_INCOME,
    FX,
    COMMODITY,
    DERIVATIVE;

    /**
     * TODO(TICKET-I020): implement isCash().
     * Cash products = EQUITY, FIXED_INCOME, FX.
     * Non-cash       = COMMODITY, DERIVATIVE.
     */
    public boolean isCash() {
        // HINT: a switch expression keeps this compact and readable:
        //   return switch (this) { case EQUITY, FIXED_INCOME, FX -> true; default -> false; };
        throw new UnsupportedOperationException("TICKET-I020: implement AssetClass.isCash()");
    }
}
