package com.dbtraining.tradeflow.model;

/**
 * ============================================================================
 * EquityTrade — TICKET-I029
 * ============================================================================
 * WHAT:    A trade on an equity (shares of a listed company).
 * WHY:     Equities are exchange-listed, so we need the exchange name + lot size.
 * ============================================================================
 *  TODO(TICKET-I029):
 *    extends BaseTrade.
 *    Extra fields:
 *      private final String exchange;     // e.g. "XETRA", "NASDAQ"
 *      private final int lotSize;         // typical 100 for US, 1 for European
 *
 *    assetClassDescription() returns "Equity on " + exchange.
 * ============================================================================
 */
public class EquityTrade /* extends BaseTrade */ {
    // TODO(TICKET-I029): extend BaseTrade, add exchange + lotSize, override
    //                    assetClassDescription().
}
