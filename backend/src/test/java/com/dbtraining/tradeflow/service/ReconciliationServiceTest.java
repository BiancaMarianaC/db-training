package com.dbtraining.tradeflow.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * ============================================================================
 * ReconciliationServiceTest — TICKET-I048..I053
 * ============================================================================
 * WHAT:    JUnit + Mockito tests for the recon engine.
 * HOW:     @ExtendWith(MockitoExtension.class). Mock the DAOs, build sample
 *          trade lists, assert on the returned ReconReport.
 * WHY:     Day 4 sets a 70% coverage target. ReconciliationService is the
 *          critical path — it gets the most attention.
 * OBSERVE: `mvn test` runs these in a few seconds; JaCoCo report shows the
 *          coverage % per class.
 * ============================================================================
 */
@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    // TODO(TICKET-I048): test matchTrades_allMatched_returnsEmptyDiscrepancies.
    @Test
    void matchTrades_allMatched_returnsEmptyDiscrepancies() {
        fail("TICKET-I048: implement test");
    }

    // TODO(TICKET-I049): test matchTrades_priceMismatch_flagsDiscrepancy.
    @Test
    void matchTrades_priceMismatch_flagsDiscrepancy() {
        fail("TICKET-I049: implement test");
    }

    // TODO(TICKET-I050): test matchTrades_missingExternal_flagsMissingTrade.
    @Test
    void matchTrades_missingExternal_flagsMissingTrade() {
        fail("TICKET-I050: implement test");
    }

    // TODO(TICKET-I051): test with @Mock TradeDAO + verify(...).findAll() called.
    @Test
    void mockedTradeDAO_findAllCalledOnce() {
        fail("TICKET-I051: implement test");
    }

    // TODO(TICKET-I052): test with @Mock ReconResultDAO + ArgumentCaptor.
    @Test
    void mockedReconResultDAO_insertCalledPerDiscrepancy() {
        fail("TICKET-I052: implement test");
    }
}
