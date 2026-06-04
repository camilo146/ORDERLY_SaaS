-- ══════════════════════════════════════════════════════════════════════════
-- V6 — Subscription system overhaul: plans rework + overage tracking
-- ══════════════════════════════════════════════════════════════════════════

-- Extend plans table: add code column, USD pricing, overage config, feature flags
ALTER TABLE plans
    ADD COLUMN IF NOT EXISTS code VARCHAR(50) UNIQUE,
    ADD COLUMN IF NOT EXISTS monthly_price_usd NUMERIC(12,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS annual_price_usd NUMERIC(12,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS overage_rate_cop NUMERIC(12,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS overage_rate_usd NUMERIC(10,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS overage_block_size INTEGER NOT NULL DEFAULT 500,
    ADD COLUMN IF NOT EXISTS trial_days INTEGER NOT NULL DEFAULT 14,
    ADD COLUMN IF NOT EXISTS analytics_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS multi_location BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS custom_branding BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS priority_support BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill codes for legacy plans
UPDATE plans SET code = 'basic'   WHERE name = 'Básico'  AND code IS NULL;
UPDATE plans SET code = 'pro'     WHERE name = 'Pro'     AND code IS NULL;
UPDATE plans SET code = 'premium' WHERE name = 'Premium' AND code IS NULL;

-- Insert / upsert new commercial plans
INSERT INTO plans (
    code, name,
    monthly_price_cop, annual_price_cop,
    monthly_price_usd, annual_price_usd,
    monthly_order_limit, product_limit, user_limit, template_limit,
    handoff_enabled, analytics_enabled, custom_messages_enabled,
    trial_days, is_active,
    overage_rate_cop, overage_rate_usd, overage_block_size,
    multi_location, custom_branding, priority_support
)
VALUES
  ('starter', 'Starter',
   49900, 499000, 12.99, 129.90,
   300, 100, 2, 3,
   TRUE, FALSE, TRUE, 10, TRUE,
   9900, 1.99, 500, FALSE, FALSE, FALSE),

  ('business', 'Business',
   109900, 1099000, 29.00, 290.00,
   2000, 500, 10, 20,
   TRUE, TRUE, TRUE, 14, TRUE,
   9900, 1.99, 500, FALSE, FALSE, TRUE),

  ('enterprise', 'Enterprise',
   299900, 2999000, 79.00, 790.00,
   10000, 2000, 50, 100,
   TRUE, TRUE, TRUE, 14, TRUE,
   0, 0, 500, TRUE, TRUE, TRUE)
ON CONFLICT (code) DO UPDATE SET
    monthly_price_cop   = EXCLUDED.monthly_price_cop,
    annual_price_cop    = EXCLUDED.annual_price_cop,
    monthly_price_usd   = EXCLUDED.monthly_price_usd,
    annual_price_usd    = EXCLUDED.annual_price_usd,
    monthly_order_limit = EXCLUDED.monthly_order_limit,
    product_limit       = EXCLUDED.product_limit,
    user_limit          = EXCLUDED.user_limit,
    analytics_enabled   = EXCLUDED.analytics_enabled,
    overage_rate_cop    = EXCLUDED.overage_rate_cop,
    overage_rate_usd    = EXCLUDED.overage_rate_usd,
    overage_block_size  = EXCLUDED.overage_block_size,
    multi_location      = EXCLUDED.multi_location,
    custom_branding     = EXCLUDED.custom_branding,
    priority_support    = EXCLUDED.priority_support,
    updated_at          = NOW();

-- Expand subscription status values to include SUSPENDED
ALTER TABLE subscriptions DROP CONSTRAINT IF EXISTS subscriptions_status_check;
ALTER TABLE subscriptions
    ADD CONSTRAINT subscriptions_status_check
    CHECK (status IN ('TRIAL','ACTIVE','PAST_DUE','SUSPENDED','CANCELED',
                      'trialing','active','past_due','cancelled','expired'));

-- Monthly usage tracking per business per billing period
CREATE TABLE IF NOT EXISTS subscription_usage (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    business_id        UUID NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
    subscription_id    UUID REFERENCES subscriptions(id) ON DELETE SET NULL,
    period_start       TIMESTAMPTZ NOT NULL,
    period_end         TIMESTAMPTZ NOT NULL,
    plan_order_limit   INTEGER NOT NULL DEFAULT 0,
    orders_count       INTEGER NOT NULL DEFAULT 0,
    overage_count      INTEGER NOT NULL DEFAULT 0,
    overage_block_size INTEGER NOT NULL DEFAULT 500,
    overage_blocks     INTEGER NOT NULL DEFAULT 0,
    overage_rate_cop   NUMERIC(12,2) NOT NULL DEFAULT 0,
    overage_rate_usd   NUMERIC(10,4) NOT NULL DEFAULT 0,
    overage_charge_cop NUMERIC(12,2) NOT NULL DEFAULT 0,
    overage_charge_usd NUMERIC(10,4) NOT NULL DEFAULT 0,
    is_finalized       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (business_id, period_start)
);

CREATE INDEX IF NOT EXISTS idx_subscription_usage_business_period
    ON subscription_usage (business_id, period_start DESC);

DROP TRIGGER IF EXISTS trg_subscription_usage_updated_at ON subscription_usage;
CREATE TRIGGER trg_subscription_usage_updated_at
    BEFORE UPDATE ON subscription_usage
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
