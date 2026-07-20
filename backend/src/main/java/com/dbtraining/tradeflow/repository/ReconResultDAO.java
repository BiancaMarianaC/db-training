package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.ReconResult;

import java.util.List;

/**
 * ============================================================================
 * ReconResultDAO — TICKET-I046 (Day 4)
 * ============================================================================
 * WHAT:    JDBC DAO for recon_results.
 * HOW:     PreparedStatement + try-with-resources.
 * WHY:     The matching engine on Day 3 writes results here.
 * ============================================================================
 */
public class ReconResultDAO {

    public long insert(ReconResult result) {
        throw new UnsupportedOperationException("TICKET-I046");
    }

    public List<ReconResult> findByTradeId(long tradeId) {
        throw new UnsupportedOperationException("TICKET-I046");
    }

    public List<ReconResult> findUnresolved() {
        // HINT: WHERE status = 'OPEN'
        throw new UnsupportedOperationException("TICKET-I046");
    }
}
