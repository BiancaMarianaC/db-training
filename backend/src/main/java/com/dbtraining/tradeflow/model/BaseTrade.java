package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * ============================================================================
 * BaseTrade — TICKET-I028
 * ============================================================================
 * WHAT:    Abstract superclass for asset-class-specific trade types
 *          (EquityTrade, FXTrade, BondTrade).
 * HOW:     Holds the common fields. Concrete classes add asset-class-specific
 *          fields and override the abstract describer.
 * WHY:     Demonstrates inheritance + polymorphism in real domain terms,
 *          and gives `ReconciliationService` one type to operate on.
 * OBSERVE: You CANNOT do `new BaseTrade(...)` — only the subclasses.
 *
 * GOTCHA:  Read "Effective Java" Item 18 — favour composition over inheritance.
 *          Discuss with your team: is BaseTrade the right call, or would a
 *          single Trade with an AssetClass enum + composition (e.g.
 *          AssetSpecificDetails) be cleaner? Document your choice in the PR.
 * ============================================================================
 */
public abstract class BaseTrade {

    // TODO(TICKET-I028): protected final fields for the shared values
    //   tradeRef, instrumentId, counterpartyId, quantity, price, tradeDate,
    //   status, createdAt.

    // TODO(TICKET-I028): protected constructor (subclasses call super(...)).

    // TODO(TICKET-I028): public getters.

    /**
     * Each asset class returns its own description for logs/UI.
     * EquityTrade → "Equity on XETRA"
     * FXTrade     → "FX EUR/USD"
     * BondTrade   → "Bond coupon 4.50% mat 2030-06-15"
     */
    public abstract String assetClassDescription();
}
