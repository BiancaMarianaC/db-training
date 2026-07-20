package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.model.ReconResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * ============================================================================
 * ReconReportExporter — TICKET-I037
 * ============================================================================
 * WHAT:    Writes reconciliation results to a CSV file.
 * HOW:     Vanilla java.nio.file + BufferedWriter. NO external CSV library
 *          — the point is to learn quoting/escaping by hand.
 * WHY:     Ops users download daily recon CSVs to feed downstream systems.
 * OBSERVE: Field values containing `,` or `"` are quoted; embedded `"` is
 *          doubled to `""`.
 * ============================================================================
 *  TODO(TICKET-I037):
 *    public void exportReconReport(List<ReconResult> results, Path target)
 *
 *  HINTS:
 *    1. Write to a `.tmp` sibling file, then Files.move(..., ATOMIC_MOVE) —
 *       no half-written file is ever visible to a reader.
 *    2. Use try-with-resources for the writer.
 *    3. Header row: "trade_id,status,discrepancy_type,resolved_at"
 *    4. Helper: private String escape(String value) — handles comma, quote, newline.
 * ============================================================================
 */
public class ReconReportExporter {

    public void exportReconReport(List<ReconResult> results, Path target) throws IOException {
        // TODO(TICKET-I037): implement.
        throw new UnsupportedOperationException("TICKET-I037: implement CSV export");
    }
}
