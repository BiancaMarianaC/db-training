package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.dto.TradeEvent;

/**
 * ============================================================================
 * AuditEventConsumer — TICKET-I119
 * ============================================================================
 * WHAT:    Writes one audit_log row per TradeEvent.
 * HOW:     @KafkaListener with consumer group `audit-group` — different
 *          from recon-group so it consumes in parallel.
 * WHY:     Audit is a parallel concern. If recon dies, audit still runs;
 *          if audit dies, recon still runs. Separate consumer groups give
 *          us that isolation for free.
 * OBSERVE: After each posted trade, `SELECT * FROM audit_log ORDER BY id DESC LIMIT 1;`
 *          returns the new row.
 * ============================================================================
 *
 *  TODO(TICKET-I119): mirror ReconEventConsumer, but call AuditService.record(event).
 *  HINT: use Jackson to serialize event.payload() to JSON for the new_value column.
 * ============================================================================
 */
public class AuditEventConsumer {

    public void onEvent(TradeEvent event) {
        throw new UnsupportedOperationException("TICKET-I119");
    }
}
