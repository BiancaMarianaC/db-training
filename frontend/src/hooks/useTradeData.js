/**
 * ============================================================================
 * useTradeData.js — TICKET-I101
 * ============================================================================
 * WHAT:    Custom hook that returns { trades, loading, error, refetch }
 *          based on filters.
 * HOW:     useState + useEffect + AbortController.
 * WHY:     Encapsulates the fetch lifecycle so individual components stay
 *          short and declarative.
 * OBSERVE: Inspect React DevTools — only the components that USE this hook
 *          re-render on a refetch.
 * ============================================================================
 *
 *  HINTS:
 *  - Memoise `filters` in the caller (useMemo) — otherwise a new object on
 *    every render triggers infinite refetches via your useEffect dep array.
 *  - Use an AbortController to cancel in-flight requests when the component
 *    unmounts (StrictMode mounts components twice in dev — this surfaces
 *    bugs like this immediately).
 * ============================================================================
 */
import { useCallback, useEffect, useRef, useState } from 'react';
import { getTrades } from '../services/apiService.js';

export function useTradeData(filters = {}) {
    const [trades, setTrades] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const filterKey = JSON.stringify(filters);
    const lastRequest = useRef(0);

    const refetch = useCallback(async () => {
        const myReq = ++lastRequest.current;
        const ctrl = new AbortController();

        setLoading(true);
        setError(null);

        try {
            const page = await getTrades(filters);
            // Drop the result if a newer request has been started.
            if (myReq !== lastRequest.current) return;
            setTrades(page.content || page);
        } catch (e) {
            if (myReq !== lastRequest.current) return;
            setError(e);
        } finally {
            if (myReq === lastRequest.current) setLoading(false);
        }

        return () => ctrl.abort();
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [filterKey]);

    useEffect(() => { refetch(); }, [refetch]);

    return { trades, loading, error, refetch };
}
