/**
 * ============================================================================
 * Trades.jsx — TICKET-I104 + TICKET-I109
 * ============================================================================
 * WHAT:    Page that combines filters + TradeTable.
 * WHY:     Demonstrates useReducer for multi-field filter state (TICKET-I109).
 * ============================================================================
 */
import { useMemo, useReducer } from 'react';
import { initialFilters, tradeFilterReducer } from '../hooks/tradeFilterReducer.js';
import { useTradeData } from '../hooks/useTradeData.js';
import TradeTable from '../components/TradeTable.jsx';

export default function Trades() {
    const [filters, dispatch] = useReducer(tradeFilterReducer, initialFilters);

    // Memoise the object we pass to the hook so it doesn't re-fetch on every render.
    const apiParams = useMemo(() => {
        const p = {};
        if (filters.status) p.status = filters.status;
        if (filters.from) p.from = filters.from;
        if (filters.to) p.to = filters.to;
        return p;
    }, [filters.status, filters.from, filters.to]);

    const { trades, loading, error } = useTradeData(apiParams);

    const onSortChange = (field) => {
        const dir = (filters.sortField === field && filters.sortDir === 'asc') ? 'desc' : 'asc';
        dispatch({ type: 'SET_SORT', field, dir });
    };

    return (
        <>
            <h1>Trades</h1>

            <div className="filters">
                {['MATCHED','PENDING','UNMATCHED','DISPUTED','CANCELLED'].map(s => (
                    <button key={s}
                            className={filters.status === s ? 'active' : ''}
                            onClick={() => dispatch({ type: 'SET_STATUS', status: filters.status === s ? null : s })}>
                        {s}
                    </button>
                ))}
                <button onClick={() => dispatch({ type: 'RESET' })}>Reset</button>
            </div>

            {error && <div className="error">{error.message}</div>}

            <TradeTable
                trades={trades}
                loading={loading}
                sortField={filters.sortField}
                sortDir={filters.sortDir}
                onSortChange={onSortChange}
            />
        </>
    );
}
