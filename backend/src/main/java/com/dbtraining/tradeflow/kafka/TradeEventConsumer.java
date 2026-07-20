package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.dto.TradeEvent;

/**
 * ============================================================================
 * TradeEventConsumer — TICKET-I116
 * ============================================================================
 * WHAT:    Simple log-only consumer to confirm the pipeline works.
 * HOW:     @KafkaListener on the `trade-events` topic.
 * WHY:     Smoke test before the more interesting Recon + Audit consumers
 *          (I117, I119) take action.
 * OBSERVE: Log line "Received TradeEvent[tradeRef=..., action=CREATED]"
 *          for every published event.
 * ============================================================================
 *
 *  TODO(TICKET-I116):
 *    @Component
 *    public class TradeEventConsumer {
 *        @KafkaListener(topics = "${tradeflow.kafka.topics.trades}",
 *                       groupId = "trade-log-group")
 *        public void consume(TradeEvent event) {
 *            log.info("Received {}", event);
 *        }
 *    }
 * ============================================================================
 */
public class TradeEventConsumer {

    public void consume(TradeEvent event) {
        throw new UnsupportedOperationException("TICKET-I116");
    }
}
