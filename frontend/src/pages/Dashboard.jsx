/**
 * ============================================================================
 * Dashboard.jsx — TICKET-I103
 * ============================================================================
 * WHAT:    Landing page with 4 summary cards.
 * WHY:     The number Ops users look at first thing in the morning.
 * ============================================================================
 *
 *  TODO(TICKET-I103):
 *    - useTradeData + useReconResults
 *    - derive: total trades, matched %, unmatched count, avg processing time
 *    - refresh every 30s (useEffect + setInterval, cleared on unmount)
 * ============================================================================
 */
import { useMemo } from 'react';
import StatCard from '../components/StatCard.jsx';
import { useTradeData } from '../hooks/useTradeData.js';
import { useReconResults } from '../hooks/useReconResults.js';

export default function Dashboard() {
    const filters = useMemo(() => ({ size: 500 }), []);
    const { trades, loading } = useTradeData(filters);
    const { results: openBreaks } = useReconResults('OPEN');

    const total = trades.length;
    const matched = trades.filter(t => t.status === 'MATCHED').length;
    const matchedPct = total ? Math.round((matched / total) * 100) + '%' : '—';

    return (
        <>
            <h1>Operations Dashboard</h1>
            <section className="cards">
                <StatCard caption="Total Trades"        value={loading ? '…' : total} />
                <StatCard caption="Matched %"           value={loading ? '…' : matchedPct} />
                <StatCard caption="Unmatched Count"     value={openBreaks.length} />
                <StatCard caption="Avg Processing Time" value="—" />
                {/* TODO(TICKET-I103): wire avg processing time from /api/v1/recon/results. */}
            </section>
        </>
    );
}
