package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ============================================================================
 * BondTrade — TICKET-I031
 * ============================================================================
 * WHAT:    A trade on a fixed-income instrument.
 * WHY:     Bonds need coupon and maturity to compute yield / settlement.
 * ============================================================================
 *  TODO(TICKET-I031):
 *    extends BaseTrade.
 *    Extra fields:
 *      private final BigDecimal couponRate;       // 0.0 .. 100.0
 *      private final LocalDate maturityDate;      // must be after tradeDate
 *      private final BigDecimal faceValue;
 *
 *    Validate in builder:
 *      - couponRate >= 0 && couponRate <= 100
 *      - maturityDate.isAfter(tradeDate)
 *
 *    assetClassDescription() returns
 *      "Bond coupon " + couponRate + "% mat " + maturityDate.
 * ============================================================================
 */
public class BondTrade /* extends BaseTrade */ {
    // TODO(TICKET-I031): extend BaseTrade, add coupon/maturity/faceValue, override.
}
