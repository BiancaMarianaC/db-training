package com.dbtraining.tradeflow.repository;

/**
 * ============================================================================
 * ReconResultRepository — TICKET-I061 (Day 5)
 * ============================================================================
 * WHAT:    JPA repository for ReconResult.
 * HOW:     extends JpaRepository<ReconResult, Long>.
 * ============================================================================
 *
 *  TODO(TICKET-I061):
 *    - findByStatus(String status)
 *    - @Query for findUnresolvedByCounterparty(Long counterpartyId)
 *
 *  HINT for the JOIN query:
 *    @Query("""
 *      select r from ReconResult r
 *        join r.trade t
 *      where r.status = 'OPEN' and t.counterpartyId = :cp
 *    """)
 *    List<ReconResult> findUnresolvedByCounterparty(@Param("cp") Long counterpartyId);
 * ============================================================================
 */
public interface ReconResultRepository {
    // TODO(TICKET-I061): see comments above.
}
