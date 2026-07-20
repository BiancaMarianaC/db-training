package com.dbtraining.tradeflow.kafka;

import com.dbtraining.tradeflow.dto.TradeEvent;

/**
 * ============================================================================
 * TradeEventProducer — TICKET-I115 (Day 9)
 * ============================================================================
 * WHAT:    Publishes TradeEvent messages to Kafka topic `trade-events`.
 * HOW:     KafkaTemplate<String, TradeEvent>. Called from TradeService.
 * WHY:     Decouples "trade saved" from "recon ran" / "audit recorded" —
 *          new consumers can subscribe without touching the producer.
 * OBSERVE: Kafdrop (localhost:9000) shows the message immediately after
 *          POST /api/v1/trades succeeds.
 * ============================================================================
 *
 *  TODO(TICKET-I115):
 *    @Service
 *    public class TradeEventProducer {
 *        private static final Logger log = LoggerFactory.getLogger(...);
 *        private final KafkaTemplate<String, TradeEvent> kafkaTemplate;
 *        private final String topic;
 *
 *        public TradeEventProducer(KafkaTemplate<String, TradeEvent> t,
 *                                  @Value("${tradeflow.kafka.topics.trades}") String topic) { ... }
 *
 *        public void publish(TradeEvent e) {
 *            kafkaTemplate.send(topic, e.tradeRef(), e)
 *                .whenComplete((ok, ex) -> {
 *                    if (ex != null) log.error("Failed to publish {}", e.tradeRef(), ex);
 *                    else log.info("Published {} -> partition={}", e.tradeRef(),
 *                                  ok.getRecordMetadata().partition());
 *                });
 *        }
 *    }
 *
 *  GOTCHA: NEVER let a Kafka publish failure roll back the DB transaction.
 *          Publish AFTER commit (use TransactionSynchronizationManager or
 *          @TransactionalEventListener), or accept eventual consistency.
 * ============================================================================
 */
public class TradeEventProducer {

    public void publish(TradeEvent event) {
        throw new UnsupportedOperationException("TICKET-I115");
    }
}
