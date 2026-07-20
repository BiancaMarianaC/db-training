package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.model.BaseTrade;

import java.nio.file.Path;
import java.util.List;

/**
 * ============================================================================
 * TradeParser — TICKET-I039
 * ============================================================================
 * WHAT:    Reads CSV (or JSON) and produces a List<BaseTrade>.
 * HOW:     Stream the file line-by-line, parse each row, build a Trade via
 *          its Builder.
 * WHY:     Separation of concerns (SRP): parsing is its own job, distinct
 *          from validation and from reconciliation.
 * ============================================================================
 *  TODO(TICKET-I039):
 *    parseCsv(Path file) -> List<BaseTrade>
 *
 *  HINTS:
 *    - Files.lines(file).skip(1)            // skip header
 *    - .map(line -> line.split(",", -1))    // -1 keeps trailing empties
 *    - Detect asset class from a column to pick subclass.
 *    - Throw InsufficientDataException with line number on a bad row.
 * ============================================================================
 */
public class TradeParser {

    public List<BaseTrade> parseCsv(Path file) {
        throw new UnsupportedOperationException("TICKET-I039: implement TradeParser.parseCsv");
    }
}
