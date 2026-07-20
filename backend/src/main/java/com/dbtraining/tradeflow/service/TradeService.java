package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.dto.TradeDto;
import com.dbtraining.tradeflow.dto.TradeRequest;
import com.dbtraining.tradeflow.model.BaseTrade;
import com.dbtraining.tradeflow.model.TradeStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ============================================================================
 * TradeService — TICKET-I041..I043 + TICKET-I062
 * ============================================================================
 * WHAT:    Business-logic facade for Trade operations.
 *          Day 4: HashMap-backed + Streams pipelines.
 *          Day 5: rewritten to use TradeRepository (Spring Data JPA).
 *          Day 6: also publishes TradeEvent to Kafka (TICKET-I115).
 * HOW:     @Service from Day 1 (so Spring can wire it into controllers).
 *          Day-1 default is a no-op stub — controllers bypass it via
 *          JdbcTemplate. Day-4 onward, students replace the stubs.
 * WHY:     Controllers stay thin — all rules and persistence live here.
 * OBSERVE: Switching from HashMap to JPA on Day 5 should NOT require changing
 *          callers (controller code stays the same).
 * ============================================================================
 *
 *  TICKET-I041: refactor in-memory store to Map<String, BaseTrade>.
 *  TICKET-I042: Streams pipeline — sumByCounterparty.
 *  TICKET-I043: Streams pipeline — topNByValue.
 *  TICKET-I062: rewrite using JPA repositories + DTOs (Day 5).
 * ============================================================================
 */
@Service
public class TradeService {

    // TODO(TICKET-I041): in-memory store as HashMap keyed by tradeRef.
    private final Map<String, BaseTrade> tradesByRef = new HashMap<>();

    // TODO(TICKET-I062) [Day 5]: replace the Map with TradeRepository injection:
    //   private final TradeRepository tradeRepository;
    //   public TradeService(TradeRepository tradeRepository) { ... }

    public Collection<BaseTrade> getAllTrades() {
        // TODO(TICKET-I041): return an unmodifiable view of the values.
        return Collections.unmodifiableCollection(tradesByRef.values());
    }

    public void addTrade(BaseTrade trade) {
        // TODO(TICKET-I041): put in the map keyed by tradeRef. Reject duplicates.
        throw new UnsupportedOperationException("TICKET-I041");
    }

    /**
     * TODO(TICKET-I042):
     *   Streams pipeline that:
     *     - filters trades by status == MATCHED
     *     - groups by counterpartyId
     *     - sums quantity * price into BigDecimal
     */
    public Map<Long, BigDecimal> sumByCounterparty() {
        throw new UnsupportedOperationException("TICKET-I042");
    }

    /**
     * TODO(TICKET-I043):
     *   Top N trades by notional value (quantity * price) descending.
     */
    public List<BaseTrade> topNByValue(int n) {
        throw new UnsupportedOperationException("TICKET-I043");
    }

    /**
     * TODO(TICKET-I062) [Day 5]:
     *   Convert TradeRequest -> Trade entity, save via TradeRepository,
     *   publish TradeEvent on success (TICKET-I115), return TradeDto.
     */
    public TradeDto createTrade(TradeRequest request) {
        throw new UnsupportedOperationException("TICKET-I062");
    }

    public TradeDto updateStatus(Long id, TradeStatus newStatus) {
        // TODO(TICKET-I070): implement on Day 6.
        throw new UnsupportedOperationException("TICKET-I070");
    }

    public void softDelete(Long id) {
        // TODO(TICKET-I071): implement soft delete + audit log on Day 6.
        throw new UnsupportedOperationException("TICKET-I071");
    }
}
