package com.dbtraining.tradeflow.model;

/**
 * ============================================================================
 * TradeStatus — TICKET-I019
 * ============================================================================
 * WHAT:    Enum of possible Trade lifecycle states.
 * HOW:     Plain enum; values match the DB CHECK constraint on trades.status.
 * WHY:     Replace magic strings ("PENDING"/"MATCHED"/...) with type-safe
 *          values so the compiler catches typos.
 * OBSERVE: Java enums get a free .name()/.valueOf() — perfect for JPA
 *          (use @Enumerated(EnumType.STRING)).
 * ============================================================================
 *  HINT: keep this in sync with:
 *    - db/changelog/changes/004-create-trades.xml (the CHECK constraint)
 *    - Day-6 Spring Security / DTOs (you'll surface this via the REST API)
 * ============================================================================
 */
public enum TradeStatus {

    PENDING,
    MATCHED,
    UNMATCHED,
    DISPUTED,
    CANCELLED;

    /**
     * TODO(TICKET-I019): implement isTerminal().
     * A "terminal" status means no further state transitions are allowed.
     * MATCHED and CANCELLED are terminal; the others are not.
     */
    public boolean isTerminal() {
        // HINT: return this == MATCHED || this == CANCELLED;
        throw new UnsupportedOperationException("TICKET-I019: implement TradeStatus.isTerminal()");
    }
}
