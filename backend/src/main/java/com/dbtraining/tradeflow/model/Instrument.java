package com.dbtraining.tradeflow.model;

/**
 * ============================================================================
 * Instrument — TICKET-I023 + TICKET-I057
 * ============================================================================
 * WHAT:    What is being traded — equity, bond, FX pair, future, etc.
 * HOW:     POJO on Day 2; @Entity on Day 5.
 * WHY:     The trade table has an FK to this; the dashboard groups trades by
 *          instrument.
 * OBSERVE: Set assetClass = EQUITY, currency = "EUR", and the trade summary
 *          should treat it as a cash product.
 * ============================================================================
 *  TODO(TICKET-I023) [Day 2]:
 *    Fields: id (Long), symbol (String, e.g. "SAP.DE"), name (String),
 *            assetClass (AssetClass), currency (String length 3).
 *    Validate currency length in the Builder.
 *
 *  TODO(TICKET-I057) [Day 5]:
 *    Make this a JPA entity.
 *    - @Enumerated(EnumType.STRING) on assetClass
 *    - @Column(length = 3, nullable = false) on currency
 * ============================================================================
 */
public class Instrument {
    // TODO(TICKET-I023): fields, private ctor, Builder, getters.
}
