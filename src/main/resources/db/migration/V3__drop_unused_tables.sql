-- Drops legacy tables that are no longer used by current runtime flow.
-- Current service flow uses STOCK_MASTER / STOCK_UNIT_MASTER from common DB
-- and local PostgreSQL vector side tables.

DROP TABLE IF EXISTS compatibility_rules;
DROP TABLE IF EXISTS unit;
DROP TABLE IF EXISTS inventory;
