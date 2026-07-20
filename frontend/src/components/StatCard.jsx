/**
 * ============================================================================
 * StatCard.jsx — TICKET-I103
 * ============================================================================
 * WHAT:    Reusable summary card (caption + big value).
 * WHY:     Repeats four times on the dashboard.
 * ============================================================================
 */
export default function StatCard({ caption, value }) {
    return (
        <article className="card">
            <h2 className="card-caption">{caption}</h2>
            <p className="card-value">{value ?? '—'}</p>
        </article>
    );
}
