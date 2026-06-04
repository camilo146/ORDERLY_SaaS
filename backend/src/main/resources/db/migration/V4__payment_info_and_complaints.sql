-- ══════════════════════════════════════════════════════════════════════════
-- V4 — Payment method info per business + complaints table
-- ══════════════════════════════════════════════════════════════════════════

-- Columns for showing payment account numbers in the WhatsApp chatbot
ALTER TABLE businesses
    ADD COLUMN IF NOT EXISTS payment_nequi        VARCHAR(60)  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS payment_bank_name    VARCHAR(80)  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS payment_bank_account VARCHAR(60)  DEFAULT NULL,
    ADD COLUMN IF NOT EXISTS payment_bank_holder  VARCHAR(100) DEFAULT NULL;

-- Complaints submitted via WhatsApp chatbot
CREATE TABLE IF NOT EXISTS complaints (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id     UUID         NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    customer_phone  VARCHAR(30)  NOT NULL,
    description     TEXT         NOT NULL,
    evidence_url    VARCHAR(500) DEFAULT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_complaints_business_id ON complaints(business_id);
