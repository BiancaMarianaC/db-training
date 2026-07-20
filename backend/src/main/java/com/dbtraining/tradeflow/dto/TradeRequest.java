package com.dbtraining.tradeflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ============================================================================
 * TradeRequest — TICKET-I069
 * ============================================================================
 * WHAT:    Inbound DTO for POST /api/v1/trades.
 * HOW:     Java `record` + Bean Validation annotations.
 * WHY:     Annotated validation triggers automatically when the controller
 *          method param is marked @Valid. Failed validation → HTTP 400 with
 *          field-level details.
 * OBSERVE: Sending {"quantity": -1} returns a 400 with a clean message:
 *          "quantity: must be greater than 0".
 * ============================================================================
 *
 *  TODO(TICKET-I069): tune the validation rules:
 *    - tradeRef: @NotBlank + @Pattern matching your team's ref format (TRD-YYYY-####)
 *    - instrumentId / counterpartyId: @NotNull @Positive
 *    - quantity / price: @NotNull @Positive
 *    - tradeDate: @NotNull, @PastOrPresent
 *    - status: optional — default to PENDING in the service if null
 * ============================================================================
 */
public record TradeRequest(

        @NotBlank
        String tradeRef,

        @NotNull @Positive
        Long instrumentId,

        @NotNull @Positive
        Long counterpartyId,

        @NotNull @Positive
        BigDecimal quantity,

        @NotNull @Positive
        BigDecimal price,

        @NotNull
        LocalDate tradeDate

        // TODO(TICKET-I069): add `status` if you want clients to pass it,
        //                    otherwise the service defaults it to PENDING.
) {
}
