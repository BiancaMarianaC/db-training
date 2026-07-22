-- ============================================================================
-- queries.sql — Day 1 analytical queries
-- ============================================================================
-- WHAT:    The 5 analytical queries (TICKET-I012) + the Window Function query
--          (TICKET-I013) + the CTE query (TICKET-I014).
-- HOW:     Standard SQL aggregates / window functions / CTEs.
-- WHY:     Prove that the schema supports the dashboard's analytics needs
--          BEFORE you write any Java.
-- OBSERVE: Each query should return non-empty results against the seeded data.
-- ============================================================================


-- ============================================================================
-- TICKET-I012 — Five analytical queries
-- ============================================================================

-- ---------------------------------------------------------------------------
-- 1) Daily trade volume by date (count + total notional)
-- ---------------------------------------------------------------------------
-- TODO(TICKET-I012): write a query returning columns:
--   trade_date | trade_count | total_notional
-- Order by trade_date DESC.
-- HINT: SUM(quantity * price) as total_notional.
-- select trade_date, trade_count,  SUM(quantity * price) as total_notional from tableName


-- ---------------------------------------------------------------------------
-- 2) Proxy P&L by instrument
-- ---------------------------------------------------------------------------
-- TODO(TICKET-I012): aggregate notional value per instrument symbol.
-- Columns: symbol | total_value | trade_count


-- ---------------------------------------------------------------------------
-- 3) Top 3 counterparties by trade count
-- ---------------------------------------------------------------------------
-- TODO(TICKET-I012): GROUP BY counterparty, ORDER BY trade_count DESC, LIMIT 3.


-- ---------------------------------------------------------------------------
-- 4) Trades pending reconciliation (status PENDING or UNMATCHED)
-- ---------------------------------------------------------------------------
-- TODO(TICKET-I012): filter trades by status, JOIN instruments + counterparties
-- for readable output.


-- ---------------------------------------------------------------------------
-- 5) Average lag between trade_date and recon resolved_at (in hours)
-- ---------------------------------------------------------------------------
-- TODO(TICKET-I012):
--   SELECT AVG(EXTRACT(EPOCH FROM (resolved_at - created_at)) / 3600.0)
--   FROM recon_results WHERE status='RESOLVED';


-- ============================================================================
-- TICKET-I013 — Window function: running total of trades per day
-- ============================================================================
-- TODO(TICKET-I013):
--   columns: trade_date | daily_count | running_total
--   HINT: SUM(COUNT(*)) OVER (ORDER BY trade_date ROWS BETWEEN UNBOUNDED
--         PRECEDING AND CURRENT ROW)
--   You can either count + cumulative-sum or use COUNT(*) OVER (...).


-- ============================================================================
-- TICKET-I014 — CTE rolling up counterparty → instrument → date
-- ============================================================================
-- TODO(TICKET-I014):
--   With a CTE `trades_with_meta` that joins trades + counterparties +
--   instruments, then GROUP BY GROUPING SETS to produce:
--     - one row per (counterparty, instrument, trade_date)
--     - subtotal rows per (counterparty, instrument)
--     - grand total per counterparty
--
--   Example skeleton:
--
--   WITH trades_with_meta AS (
--       SELECT c.name AS counterparty,
--              i.symbol  AS instrument,
--              t.trade_date,
--              t.quantity * t.price AS notional
--       FROM trades t
--       JOIN counterparties c ON c.id = t.counterparty_id
--       JOIN instruments    i ON i.id = t.instrument_id
--   )
--   SELECT counterparty, instrument, trade_date, SUM(notional) AS total
--   FROM trades_with_meta
--   GROUP BY GROUPING SETS (
--       (counterparty, instrument, trade_date),
--       (counterparty, instrument),
--       (counterparty)
--   )
--   ORDER BY counterparty, instrument, trade_date;


-- ============================================================================
-- STRETCH GOAL — recursive CTE: trace trade execution -> settlement -> break
-- ============================================================================
-- See day1-database-sql.md "Stretch Goal" section.
