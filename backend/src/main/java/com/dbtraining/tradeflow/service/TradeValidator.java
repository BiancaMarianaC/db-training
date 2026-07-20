package com.dbtraining.tradeflow.service;

import com.dbtraining.tradeflow.exception.TradeValidationException;
import com.dbtraining.tradeflow.model.BaseTrade;

import java.util.List;

/**
 * ============================================================================
 * TradeValidator — TICKET-I039
 * ============================================================================
 * WHAT:    Orchestrator that picks the right ITradeValidator per asset class
 *          and validates a batch.
 * WHY:     Open/Closed Principle — adding a new asset class means a new
 *          validator, NOT modifying this class.
 * ============================================================================
 *  TODO(TICKET-I039):
 *    - Inject Map<AssetClass, ITradeValidator> validators
 *    - validateAll(List<BaseTrade>) returns List<TradeValidationException> (or
 *      throws if you prefer fail-fast — discuss with team).
 * ============================================================================
 */
public class TradeValidator {

    public List<TradeValidationException> validateAll(List<BaseTrade> trades) {
        throw new UnsupportedOperationException("TICKET-I039: implement TradeValidator");
    }
}
