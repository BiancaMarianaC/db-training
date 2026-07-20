package com.dbtraining.tradeflow.exception;

/**
 * ============================================================================
 * TradeValidationException — TICKET-I032
 * ============================================================================
 * WHAT:    Checked exception thrown when a Trade fails validation.
 * HOW:     `extends Exception` (checked — callers must declare or catch).
 * WHY:     Validation errors are RECOVERABLE — callers will likely want to
 *          surface them to the user. Checked exceptions force that handling.
 *          (Compare: InsufficientDataException, which is UNRECOVERABLE and
 *           therefore unchecked.)
 * OBSERVE: TradeController catches this on Day 6 and returns 400 Bad Request.
 * ============================================================================
 *  TODO(TICKET-I032):
 *    - extend Exception (NOT RuntimeException)
 *    - inner enum Code { MISSING_FIELD, INVALID_VALUE, REFERENCE_NOT_FOUND }
 *    - constructor (Code, String message)
 *    - getCode() accessor
 * ============================================================================
 */
public class TradeValidationException extends Exception {

    public enum Code {
        MISSING_FIELD,
        INVALID_VALUE,
        REFERENCE_NOT_FOUND
    }

    // TODO(TICKET-I032): private final Code code; getCode(); ctor(Code, String).

    public TradeValidationException(String message) {
        super(message);
    }
}
