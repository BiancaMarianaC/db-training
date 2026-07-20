/**
 * ============================================================================
 * useReconResults.js — TICKET-I102
 * ============================================================================
 * WHAT:    Mirrors useTradeData for /api/v1/recon/results.
 * WHY:     Keeps page components free of fetch boilerplate.
 * ============================================================================
 */
import { useCallback, useEffect, useState } from 'react';
import { getReconResults } from '../services/apiService.js';

export function useReconResults(status = 'OPEN') {
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const refetch = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const page = await getReconResults({ status });
            setResults(page.content || page);
        } catch (e) {
            setError(e);
        } finally {
            setLoading(false);
        }
    }, [status]);

    useEffect(() => { refetch(); }, [refetch]);

    return { results, loading, error, refetch };
}
