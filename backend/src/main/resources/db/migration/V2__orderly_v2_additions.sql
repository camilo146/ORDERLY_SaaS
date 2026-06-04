-- ══════════════════════════════════════════════════════════════════════════
-- V2 — ORDERLY architecture v2.1 additions
-- ══════════════════════════════════════════════════════════════════════════

-- ── app_users table (internal platform users / operators) ─────────────────
CREATE TABLE IF NOT EXISTS app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(200) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'OPERATOR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── churn_alerts table ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS churn_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    alert_type VARCHAR(50) NOT NULL,
    severity VARCHAR(10) NOT NULL CHECK (severity IN ('RED','YELLOW','GREEN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMPTZ,
    resolved_by UUID REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_churn_alerts_business ON churn_alerts(business_id, resolved_at) WHERE resolved_at IS NULL;

-- ── businesses: add Evolution API + onboarding columns ────────────────────
ALTER TABLE businesses
    ADD COLUMN IF NOT EXISTS whatsapp_token TEXT,
    ADD COLUMN IF NOT EXISTS whatsapp_instance_id VARCHAR(200),
    ADD COLUMN IF NOT EXISTS demo_order_sent BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS onboarding_step INTEGER NOT NULL DEFAULT 0;

-- ── plans: add active-order limits (replacing monthly_order_limit) ────────
ALTER TABLE plans
    ADD COLUMN IF NOT EXISTS max_active_orders INTEGER,
    ADD COLUMN IF NOT EXISTS max_dashboard_users INTEGER;

-- Update plan limits: BASICO=15 active orders, PRO=50, PREMIUM=unlimited
UPDATE plans SET max_active_orders = 15,  max_dashboard_users = 2  WHERE code = 'basic';
UPDATE plans SET max_active_orders = 50,  max_dashboard_users = 5  WHERE code = 'pro';
UPDATE plans SET max_active_orders = NULL, max_dashboard_users = NULL WHERE code = 'premium';

-- Update plan prices to match business strategy (COP)
UPDATE plans SET monthly_price_cop = 49000,  annual_price_cop = 490000  WHERE code = 'basic';
UPDATE plans SET monthly_price_cop = 129000, annual_price_cop = 1290000 WHERE code = 'pro';
UPDATE plans SET monthly_price_cop = 299000, annual_price_cop = 2990000 WHERE code = 'premium';

-- ── notification_queue: add missing fields for TTL and message body ────────
ALTER TABLE notification_queue
    ADD COLUMN IF NOT EXISTS recipient_phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS message_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS message_body TEXT,
    ADD COLUMN IF NOT EXISTS sent_at TIMESTAMPTZ;

-- ── conversations: add missing states from architecture ───────────────────
-- Update the state CHECK constraint to include all required states
ALTER TABLE conversations DROP CONSTRAINT IF EXISTS conversations_state_check;
ALTER TABLE conversations ADD CONSTRAINT conversations_state_check
    CHECK (state IN (
        'IDLE','GREETING','BROWSING_CATEGORIES','BROWSING_PRODUCTS',
        'ADDING_ITEM','ADDRESS_REQUEST','PAYMENT_REQUEST',
        'ORDER_CONFIRMATION','ORDER_CREATED','HUMAN_HANDOFF',
        'COLLECTING_ORDER','CONFIRMING_ORDER','COMPLETED','CLOSED'
    ));

-- ── conversations: add cart_data for bot session ─────────────────────────
ALTER TABLE conversations
    ADD COLUMN IF NOT EXISTS cart_data JSONB,
    ADD COLUMN IF NOT EXISTS session_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS is_human_active BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS handoff_reason VARCHAR(100);

-- Add index for active conversations lookup
CREATE INDEX IF NOT EXISTS idx_conversations_active ON conversations(business_id, state) WHERE closed_at IS NULL;

-- ── whatsapp_channels: add Evolution API instance tracking ────────────────
ALTER TABLE whatsapp_channels
    ADD COLUMN IF NOT EXISTS instance_name VARCHAR(200),
    ADD COLUMN IF NOT EXISTS instance_state VARCHAR(30) DEFAULT 'close';
