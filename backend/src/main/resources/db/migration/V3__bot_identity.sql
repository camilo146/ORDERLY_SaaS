-- ══════════════════════════════════════════════════════════════════════════
-- V3 — Bot identity / mascot per business
-- ══════════════════════════════════════════════════════════════════════════

ALTER TABLE businesses
    ADD COLUMN IF NOT EXISTS bot_name  VARCHAR(80)  DEFAULT 'Orderly',
    ADD COLUMN IF NOT EXISTS bot_emoji VARCHAR(10)  DEFAULT '🤖';
