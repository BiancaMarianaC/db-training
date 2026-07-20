-- ============================================================================
-- seed_data.sql — TICKET-I009
-- ============================================================================
-- WHAT:    Insert sample reference + trade data for local dev / testing.
-- HOW:     Plain INSERT statements wrapped in a single transaction.
-- WHY:     We need realistic data to write meaningful queries (Day 1) and to
--          see the dashboard show something on Day 7.
-- OBSERVE: After running, `SELECT count(*) FROM trades;` returns 100.
-- ============================================================================
-- HINT: run this AFTER the Liquibase migrations create the tables.
-- HINT: this file is NOT a Liquibase changeset — it is run by hand or via
--       `data.sql` in Spring Boot (dev profile). For prod-like data we
--       prefer a dedicated Liquibase data-only changeset.
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- 5 Counterparties — well-known brokers/banks for realism
-- ----------------------------------------------------------------------------
-- TODO(TICKET-I009): insert 5 counterparties with proper LEI codes + regions.
-- HINT: LEI is 20 chars. Examples (these are real LEIs, public info):
--   Goldman Sachs International       W22LROWP2IHZNBB6K528  EMEA
--   Morgan Stanley & Co.              I7331LVKQX1L8TUI8447  NAMR
--   Citigroup Global Markets Limited  XKZZ2JZF41MRHTR1V493  EMEA
--   Barclays Bank PLC                 G5GSEF7VJP5I7OUK5573  EMEA
--   Nomura International PLC          DGQCSV2PHVF7I2743539  APAC

-- INSERT INTO counterparties (name, lei_code, region) VALUES
-- ('Goldman Sachs International',      'W22LROWP2IHZNBB6K528', 'EMEA'),
-- ('Morgan Stanley & Co.',             'I7331LVKQX1L8TUI8447', 'NAMR'),
-- ('Citigroup Global Markets Limited', 'XKZZ2JZF41MRHTR1V493', 'EMEA'),
-- ('Barclays Bank PLC',                'G5GSEF7VJP5I7OUK5573', 'EMEA'),
-- ('Nomura International PLC',         'DGQCSV2PHVF7I2743539', 'APAC');


-- ----------------------------------------------------------------------------
-- 20 Instruments — mixed asset classes
-- ----------------------------------------------------------------------------
-- TODO(TICKET-I009): insert 20 instruments.
-- HINT examples:
--   EQUITY: SAP.DE, BMW.DE, AAPL, MSFT, TSLA, NVDA, GOOGL, AMZN, META, NESN.SW
--   FX:     EURUSD, GBPUSD, USDJPY, EURGBP
--   FIXED_INCOME: BUND_10Y, UST_10Y, GILT_10Y
--   COMMODITY: GOLD_SPOT, BRENT_CRUDE
--   DERIVATIVE: ES_FUT (S&P 500 future)


-- ----------------------------------------------------------------------------
-- 100 Trades — spread across the last 30 days, mixed statuses
-- ----------------------------------------------------------------------------
-- TODO(TICKET-I009): generate 100 trades with realistic spread.
-- HINT: use generate_series for dates + random() for status. Example:
--
-- INSERT INTO trades (trade_ref, instrument_id, counterparty_id, quantity,
--                     price, trade_date, status)
-- SELECT
--   'TRD-2026-' || lpad(g::text, 4, '0'),
--   1 + (random() * 19)::int,                          -- instrument_id 1..20
--   1 + (random() * 4)::int,                           -- counterparty_id 1..5
--   round((10 + random() * 990)::numeric, 4),
--   round((50 + random() * 450)::numeric, 4),
--   current_date - (random() * 30)::int,
--   (ARRAY['PENDING','MATCHED','UNMATCHED','DISPUTED','CANCELLED'])
--       [1 + (random() * 4)::int]
-- FROM generate_series(1, 100) g;


-- ----------------------------------------------------------------------------
-- A few OPEN recon_results so the dashboard has something to show
-- ----------------------------------------------------------------------------
-- TODO(TICKET-I009): insert ~10 recon_results referencing real trade IDs,
--                    using each discrepancy_type at least once.


COMMIT;

-- After running:
--   SELECT count(*) FROM counterparties;   -- expect 5
--   SELECT count(*) FROM instruments;      -- expect 20
--   SELECT count(*) FROM trades;           -- expect 100
--   SELECT count(*) FROM recon_results;    -- expect ~10
