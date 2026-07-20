/**
 * ============================================================================
 * StatusBadge.jsx — TICKET-I108
 * ============================================================================
 * WHAT:    Reusable coloured pill for trade / recon status.
 * WHY:     Single source of truth for status colour. Change it once, every
 *          page updates.
 *
 *  USAGE:
 *    <StatusBadge status="MATCHED" />        ->  green pill
 *    <StatusBadge status="OPEN" />           ->  red pill
 *    <StatusBadge status="DISPUTED" />       ->  amber pill
 * ============================================================================
 */
import styles from './StatusBadge.module.css';

const COLOURS = {
    MATCHED:   styles.matched,
    PENDING:   styles.pending,
    UNMATCHED: styles.unmatched,
    DISPUTED:  styles.disputed,
    CANCELLED: styles.cancelled,
    OPEN:      styles.unmatched,
    RESOLVED:  styles.matched,
    IGNORED:   styles.pending
};

export default function StatusBadge({ status }) {
    const cls = COLOURS[status] || styles.pending;
    return <span className={`${styles.badge} ${cls}`}>{status}</span>;
}
