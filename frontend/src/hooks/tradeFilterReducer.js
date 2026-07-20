/**
 * ============================================================================
 * tradeFilterReducer.js — TICKET-I109
 * ============================================================================
 * WHAT:    Reducer for the trade-filter state.
 * WHY:     useReducer is cleaner than useState when multiple fields move
 *          together (status + dateRange + sortField + ...).
 * ============================================================================
 *
 *  TODO(TICKET-I109): add cases for every action.
 *  Actions:
 *    SET_STATUS         { status }
 *    SET_DATE_RANGE     { from, to }
 *    SET_COUNTERPARTY   { counterparty }
 *    SET_SORT           { field, dir }
 *    RESET
 * ============================================================================
 */
export const initialFilters = {
    status: null,
    from: null,
    to: null,
    counterparty: null,
    sortField: 'tradeDate',
    sortDir: 'desc'
};

export function tradeFilterReducer(state, action) {
    switch (action.type) {
        case 'SET_STATUS':       return { ...state, status: action.status };
        case 'SET_DATE_RANGE':   return { ...state, from: action.from, to: action.to };
        case 'SET_COUNTERPARTY': return { ...state, counterparty: action.counterparty };
        case 'SET_SORT':         return { ...state, sortField: action.field, sortDir: action.dir };
        case 'RESET':            return initialFilters;
        default:
            // TODO(TICKET-I109): keep a defensive throw — typos in action.type
            // become loud errors instead of silent bugs.
            throw new Error('Unknown filter action: ' + action.type);
    }
}
