package com.dbtraining.tradeflow.service.validator;

import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.model.BaseTrade;

/**
 * ============================================================================
 * ITradeValidator — TICKET-I038
 * ============================================================================
 * WHAT:    Validation contract for any trade type.
 * HOW:     One method that throws when the trade is invalid.
 * WHY:     Apply ISP — callers depend ONLY on the validate operation, not
 *          on knowing which validator concrete class is in play.
 * OBSERVE: TradeValidator (the orchestrator, TICKET-I039) picks the right
 *          validator per asset class via polymorphism.
 * ============================================================================
 */
public interface ITradeValidator {

    /**
     * @throws TradeValidationException with a Code explaining why the trade
     *         was rejected.
     */
    void validate(BaseTrade trade) throws TradeValidationException;
}
