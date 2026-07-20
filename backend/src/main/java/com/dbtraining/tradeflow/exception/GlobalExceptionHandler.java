package com.dbtraining.tradeflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * ============================================================================
 * GlobalExceptionHandler — TICKET-I075
 * ============================================================================
 * WHAT:    @RestControllerAdvice that converts thrown exceptions into a
 *          consistent JSON error envelope.
 * HOW:     One @ExceptionHandler method per exception type, returning a
 *          ResponseEntity<Map<...>>.
 * WHY:     Every API consumer (React, Postman, tests) gets the same shape:
 *          { "code", "message", "timestamp", "path" }. No stack traces in
 *          production. Easier to consume on the frontend.
 * OBSERVE: POST a trade with missing fields → response body has these 4
 *          keys, HTTP 400.
 * ============================================================================
 *  TODO(TICKET-I075):
 *    - handle TradeValidationException → 400
 *    - handle TradeNotFoundException → 404
 *    - handle MethodArgumentNotValidException → 400 with field details
 *    - handle generic Exception → 500 (last-resort catch-all)
 *
 *  HINT: define a small `record ErrorEnvelope(...)` and return it from
 *        each handler — cleaner than Map<String,Object>.
 * ============================================================================
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------
    // Skeleton handler — replace with the full set per TICKET-I075.
    // ------------------------------------------------------------------
    @ExceptionHandler(TradeNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(TradeNotFoundException ex) {
        // TODO(TICKET-I075): build full envelope including the request path.
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "code", "TRADE_NOT_FOUND",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        ));
    }

    // TODO(TICKET-I075): add handler for TradeValidationException -> 400.
    // TODO(TICKET-I075): add handler for MethodArgumentNotValidException -> 400.
    // TODO(TICKET-I075): add handler for Exception.class -> 500 (NEVER leak stack trace).
}
