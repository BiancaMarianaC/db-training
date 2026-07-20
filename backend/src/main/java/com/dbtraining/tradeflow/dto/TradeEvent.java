package com.dbtraining.tradeflow.dto;

import java.time.Instant;

/**
 * ============================================================================
 * TradeEvent — TICKET-I114 (Day 9 Kafka)
 * ============================================================================
 * WHAT:    The Kafka message published every time a trade is created/updated/
 *          cancelled.
 * HOW:     Plain POJO / record — serialized to JSON by Spring Kafka.
 * WHY:     Downstream consumers (Recon, Audit) react to these instead of
 *          polling the DB.
 * OBSERVE: After POST /api/v1/trades, Kafdrop shows a message on `trade-events`
 *          whose value matches this shape.
 * ============================================================================
 *  HINTS:
 *  - Use record, NOT class (you get the no-arg constructor problem with
 *    Jackson + record on older versions — use Spring Boot 3.x which has the fix).
 *  - The `payload` field is a TradeDto so consumers get the full state, not
 *    just IDs (avoids the consumer needing to query the DB).
 * ============================================================================
 */
public record TradeEvent(
        String tradeRef,
        Action action,
        Instant timestamp,
        TradeDto payload
) {

    public enum Action {
        CREATED,
        UPDATED,
        CANCELLED
    }
}
