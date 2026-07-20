/**
 * ============================================================================
 * Recon.jsx — TICKET-I107
 * ============================================================================
 * WHAT:    Recon-breaks page.
 * WHY:     Where Ops users actually resolve breaks.
 * ============================================================================
 *
 *  TODO(TICKET-I107):
 *    - filter pills All / OPEN / RESOLVED
 *    - resolve button calls apiService.resolveBreak(id)
 *    - optimistic UI: mark row resolved locally, rollback on error
 * ============================================================================
 */
import { useState } from 'react';
import StatusBadge from '../components/StatusBadge.jsx';
import { useReconResults } from '../hooks/useReconResults.js';
import { resolveBreak } from '../services/apiService.js';

export default function Recon() {
    const [filter, setFilter] = useState('OPEN');
    const { results, loading, error, refetch } = useReconResults(filter);

    // TODO(TICKET-I107): optimistic state shadow so we can roll back on error.
    const [optimistic, setOptimistic] = useState({});

    const onResolve = async (id) => {
        setOptimistic(prev => ({ ...prev, [id]: 'RESOLVED' }));
        try {
            await resolveBreak(id);
            refetch();
        } catch (e) {
            // Rollback
            setOptimistic(prev => {
                const next = { ...prev };
                delete next[id];
                return next;
            });
            alert('Resolve failed: ' + e.message);
        }
    };

    return (
        <>
            <h1>Reconciliation Breaks</h1>

            <div className="filters">
                {['OPEN', 'RESOLVED', 'IGNORED'].map(s => (
                    <button key={s}
                            className={filter === s ? 'active' : ''}
                            onClick={() => setFilter(s)}>
                        {s}
                    </button>
                ))}
            </div>

            {loading && <div className="loading">Loading…</div>}
            {error && <div className="error">{error.message}</div>}

            <table className="data-table">
                <thead>
                    <tr>
                        <th>Trade Ref</th>
                        <th>Discrepancy</th>
                        <th>Status</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    {results.map(r => {
                        const status = optimistic[r.id] || r.status;
                        return (
                            <tr key={r.id}>
                                <td>{r.tradeRef || r.tradeId}</td>
                                <td>{r.discrepancyType || '—'}</td>
                                <td><StatusBadge status={status} /></td>
                                <td>
                                    {status === 'OPEN' && (
                                        <button onClick={() => onResolve(r.id)}>Resolve</button>
                                    )}
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </>
    );
}
