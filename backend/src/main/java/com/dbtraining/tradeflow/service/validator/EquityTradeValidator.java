package com.dbtraining.tradeflow.service.validator;

import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.model.BaseTrade;

/**
 * ============================================================================
 * EquityTradeValidator — TICKET-I038
 * ============================================================================
 * WHAT:    Validation rules specific to EquityTrade.
 * WHY:     Equity rules differ from FX/Bond (lot size > 0, exchange known).
 * ============================================================================
 *  TODO(TICKET-I038):
 *    - if (!(trade instanceof EquityTrade et)) throw new TradeValidationException(...)
 *    - et.exchange() must be non-blank
 *    - et.lotSize() > 0
 *    - et.quantity() must be a whole multiple of lotSize
 * ============================================================================
 */
public class EquityTradeValidator implements ITradeValidator {

    @Override
    public void validate(BaseTrade trade) throws TradeValidationException {
        // TODO(TICKET-I038): implement.
        throw new TradeValidationException("TICKET-I038: implement EquityTradeValidator");
    }
}
