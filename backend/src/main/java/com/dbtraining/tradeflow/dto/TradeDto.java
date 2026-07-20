package com.dbtraining.tradeflow.dto;

import com.dbtraining.tradeflow.model.TradeStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * ============================================================================
 * TradeDto — Day 6 (TICKET-I068)
 * ============================================================================
 * WHAT:    Outbound DTO returned by the REST API.
 * HOW:     Java `record` — concise, immutable, automatic equals/hashCode.
 * WHY:     Never return the JPA entity from a controller — that couples your
 *          API contract to your DB schema and risks lazy-loading exceptions
 *          when Hibernate tries to serialize the entity outside a session.
 * OBSERVE: Day-9 Kafka events embed this same DTO inside TradeEvent.payload.
 * ============================================================================
 */
public record TradeDto(
        Long id,
        String tradeRef,
        Long instrumentId,
        Long counterpartyId,
        BigDecimal quantity,
        BigDecimal price,
        LocalDate tradeDate,
        TradeStatus status,
        Instant createdAt
) {
    // TODO(TICKET-I068): consider adding `static TradeDto from(Trade entity)`
    //                    so the mapping has one home.
}
