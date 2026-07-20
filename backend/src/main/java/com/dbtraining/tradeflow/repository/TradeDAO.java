package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.Trade;
import com.dbtraining.tradeflow.model.TradeStatus;

import java.util.List;
import java.util.Optional;

/**
 * ============================================================================
 * TradeDAO — TICKET-I045 (Day 4 — raw JDBC)
 * ============================================================================
 * WHAT:    Raw JDBC data-access object for trades.
 * HOW:     PreparedStatement everywhere — NEVER string concatenation.
 * WHY:     Day 4 builds JDBC by hand so you understand what JPA hides on Day 5.
 * OBSERVE: Day 5's TradeRepository (Spring Data) replaces this class entirely.
 *          On Day 5 you can either delete this file or keep it for comparison.
 * ============================================================================
 *
 * HINTS:
 *  - try-with-resources for Connection, PreparedStatement, ResultSet.
 *  - For insert + generated key: Statement.RETURN_GENERATED_KEYS.
 *  - Map ResultSet → Trade via Trade.builder()...build().
 *  - Wrap SQLException in a RuntimeException with context (which method failed).
 * ============================================================================
 */
public class TradeDAO {

    // TODO(TICKET-I045): inject DataSource (HikariCP from DatabaseConfig).

    public long insert(Trade trade) {
        // TODO(TICKET-I045): INSERT ... RETURNING id; return the generated id.
        throw new UnsupportedOperationException("TICKET-I045");
    }

    public Optional<Trade> findByRef(String tradeRef) {
        // TODO(TICKET-I045): SELECT * FROM trades WHERE trade_ref = ? LIMIT 1.
        throw new UnsupportedOperationException("TICKET-I045");
    }

    public List<Trade> findAll() {
        // TODO(TICKET-I045): SELECT * FROM trades.
        throw new UnsupportedOperationException("TICKET-I045");
    }

    public void updateStatus(String tradeRef, TradeStatus newStatus) {
        // TODO(TICKET-I045): UPDATE trades SET status=? WHERE trade_ref=?.
        throw new UnsupportedOperationException("TICKET-I045");
    }
}
