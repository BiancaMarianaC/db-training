package com.dbtraining.tradeflow.repository;

import com.dbtraining.tradeflow.model.Counterparty;

import java.util.List;

/**
 * ============================================================================
 * CounterpartyDAO — TICKET-I047 (Day 4)
 * ============================================================================
 */
public class CounterpartyDAO {

    public List<Counterparty> findAll() {
        throw new UnsupportedOperationException("TICKET-I047");
    }

    public List<Counterparty> findByRegion(String region) {
        // HINT: WHERE region = ? -- but ALSO validate region is in the allowed set
        // BEFORE you hit the DB. Saves a round-trip on bad input.
        throw new UnsupportedOperationException("TICKET-I047");
    }
}
