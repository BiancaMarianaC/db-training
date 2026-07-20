package com.dbtraining.tradeflow.model;

import java.math.BigDecimal;

/**
 * ============================================================================
 * FXTrade — TICKET-I030
 * ============================================================================
 * WHAT:    A foreign-exchange trade (EUR/USD, GBP/JPY, ...).
 * WHY:     FX has no exchange — pricing is OTC and settled bilaterally.
 *          We track the currency pair + spot rate explicitly.
 * ============================================================================
 *  TODO(TICKET-I030):
 *    extends BaseTrade.
 *    Extra fields:
 *      private final String baseCurrency;    // "EUR" in EUR/USD
 *      private final String quoteCurrency;   // "USD" in EUR/USD
 *      private final BigDecimal spotRate;
 *
 *    Validation in builder: baseCurrency != quoteCurrency.
 *    assetClassDescription() returns baseCurrency + "/" + quoteCurrency.
 * ============================================================================
 */
public class FXTrade /* extends BaseTrade */ {
    // TODO(TICKET-I030): extend BaseTrade, add base/quote/spot, override.
}
