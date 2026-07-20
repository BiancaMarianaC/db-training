-- ============================================================================
-- data.sql — Spring Boot SQL initializer (dev profile)
-- ============================================================================
-- This file runs AFTER Liquibase on every startup of the `dev` profile.
-- Spring Boot's initializer refuses to run an EMPTY script, so the SELECT 1
-- below is a placeholder until you complete the Day-1 seed-data ticket.
--
-- TICKET-I011..I013 (Sprint 2): replace this placeholder with the seed
-- inserts the day-1 README spells out (5 instruments, 4 counterparties,
-- 10 trades with mixed statuses).
--
-- Two ways to add the seed:
--   (a) edit this file directly with INSERT statements, OR
--   (b) PREFERRED — author a Liquibase <loadData> changeset alongside your
--       schema changesets, so the seed travels with migrations.
-- ============================================================================

SELECT 1;
