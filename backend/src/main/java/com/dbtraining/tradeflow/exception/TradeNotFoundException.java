package com.dbtraining.tradeflow.exception;

/**
 * ============================================================================
 * TradeNotFoundException — (used by Day-6 tickets I070, I071)
 * ============================================================================
 * WHAT:    Thrown when a requested trade ID does not exist.
 * HOW:     RuntimeException — caller usually can't recover, but we map it
 *          to a clean 404 in GlobalExceptionHandler (TICKET-I075).
 * ============================================================================
 */
public class TradeNotFoundException extends RuntimeException {

    public TradeNotFoundException(String message) {
        super(message);
    }
}
