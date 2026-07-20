package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.dto.TradeEvent;

/**
 * ============================================================================
 * ReconEventConsumer — TICKET-I117
 * ============================================================================
 * WHAT:    On every CREATED TradeEvent, runs reconciliation for that trade
 *          and writes a ReconResult.
 * HOW:     @KafkaListener with consumer group `recon-group`. Calls
 *          ReconciliationService.runForTrade(tradeRef).
 * WHY:     Real-time recon — the moment a trade lands, we know if it has a
 *          counterpart.
 * OBSERVE: Postgres `recon_results` table grows in real time as you post
 *          trades from the UI.
 * ============================================================================
 *
 *  TODO(TICKET-I117):
 *    @Component
 *    public class ReconEventConsumer {
 *        private final ReconciliationService recon;
 *
 *        @KafkaListener(topics = "${tradeflow.kafka.topics.trades}",
 *                       groupId = "recon-group")
 *        public void onEvent(TradeEvent e) {
 *            if (e.action() != Action.CREATED) return;
 *            recon.runForTrade(e.tradeRef());
 *        }
 *    }
 * ============================================================================
 */
public class ReconEventConsumer {

    public void onEvent(TradeEvent event) {
        throw new UnsupportedOperationException("TICKET-I117");
    }
}
