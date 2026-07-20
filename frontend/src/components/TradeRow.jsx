/**
 * ============================================================================
 * TradeRow.jsx — TICKET-I105
 * ============================================================================
 * WHAT:    Expandable row — clicking shows detail panel with counterparty
 *          LEI, instrument ISIN, audit log.
 * ============================================================================
 *  TODO(TICKET-I105):
 *    - useState toggle for expanded / collapsed
 *    - on expand: fetch /api/v1/trades/{id} (or use already-loaded data)
 *    - show metadata in a sub-table or definition list
 *
 *  HINT: KEEP this component small. If it grows past 120 lines, split.
 * ============================================================================
 */
import { useState } from 'react';

export default function TradeRow({ trade }) {
    const [expanded, setExpanded] = useState(false);

    // TODO(TICKET-I105): implement the expanded view.
    return (
        <>
            <tr onClick={() => setExpanded(e => !e)} style={{ cursor: 'pointer' }}>
                <td>{trade.tradeRef}</td>
                <td>{trade.instrumentId}</td>
                <td>{trade.status}</td>
            </tr>
            {expanded && (
                <tr>
                    <td colSpan={3}>
                        {/* TODO(TICKET-I105): detail panel here. */}
                        <em>TODO: detail panel for {trade.tradeRef}</em>
                    </td>
                </tr>
            )}
        </>
    );
}
