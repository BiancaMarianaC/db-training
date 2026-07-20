package com.dbtraining.tradeflow.service;

import java.nio.file.Path;

/**
 * ============================================================================
 * TradeProcessor — TICKET-I039
 * ============================================================================
 * WHAT:    Top-level orchestrator: parse → validate → reconcile → export.
 * WHY:     SRP — this class composes the others; it doesn't parse, validate,
 *          or reconcile itself.
 * ============================================================================
 *  TODO(TICKET-I039):
 *    Constructor takes TradeParser, TradeValidator, ReconciliationService,
 *    ReconReportExporter.
 *
 *    Method: void process(Path internal, Path external, Path outputCsv)
 *      - parse both files
 *      - validate both lists
 *      - matchTrades + generateReport
 *      - export discrepancies to CSV
 *      - log the ReconSummary to stdout
 * ============================================================================
 */
public class TradeProcessor {

    public void process(Path internal, Path external, Path outputCsv) {
        throw new UnsupportedOperationException("TICKET-I039: implement TradeProcessor");
    }
}
