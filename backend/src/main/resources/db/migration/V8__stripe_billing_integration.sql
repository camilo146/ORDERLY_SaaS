-- ══════════════════════════════════════════════════════════════════════════
-- V8 — Stripe billing integration: customer IDs, price IDs, plan rework
-- ══════════════════════════════════════════════════════════════════════════

-- Add Stripe customer ID to businesses (one per tenant)
ALTER TABLE businesses
    ADD COLUMN IF NOT EXISTS stripe_customer_id VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_businesses_stripe_customer
    ON businesses (stripe_customer_id)
    WHERE stripe_customer_id IS NOT NULL;

-- Add Stripe price IDs to plans (environment-managed, stored for reference)
ALTER TABLE plans
    ADD COLUMN IF NOT EXISTS stripe_price_id_monthly VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stripe_price_id_annual  VARCHAR(255);

-- ── Rename + reprice existing plans to match new commercial tiers ───────────
-- Order matters: rename 'business' first to free the code, then rename 'enterprise' to 'business'

UPDATE plans
SET code = 'growth', name = 'Growth',
    monthly_price     = 55 * 100,
    annual_price      = 44 * 100 * 12,
    monthly_price_usd = 55.00,
    annual_price_usd  = 44.00 * 12,
    max_orders_per_month = 2000,
    max_dashboard_users  = 3,
    analytics_enabled    = TRUE,
    priority_support     = TRUE
WHERE code = 'business';

UPDATE plans
SET code = 'business', name = 'Business',
    monthly_price     = 199 * 100,
    annual_price      = 159 * 100 * 12,
    monthly_price_usd = 199.00,
    annual_price_usd  = 159.00 * 12,
    max_orders_per_month = NULL,
    max_dashboard_users  = NULL,
    multi_location       = TRUE,
    custom_branding      = TRUE,
    analytics_enabled    = TRUE,
    priority_support     = TRUE
WHERE code = 'enterprise';

UPDATE plans
SET monthly_price     = 17 * 100,
    annual_price      = 14 * 100 * 12,
    monthly_price_usd = 17.00,
    annual_price_usd  = 14.00 * 12,
    max_orders_per_month = 500,
    max_dashboard_users  = 1,
    analytics_enabled    = FALSE,
    priority_support     = FALSE
WHERE code = 'starter';

-- Insert 'growth' plan if rename above was a no-op (e.g. schema applied fresh)
INSERT INTO plans (
    code, name,
    monthly_price, annual_price,
    monthly_price_usd, annual_price_usd,
    max_orders_per_month, max_dashboard_users,
    analytics_enabled, priority_support,
    trial_days, overage_block_size
)
SELECT 'growth', 'Growth',
       55 * 100, 44 * 100 * 12,
       55.00, 44.00 * 12,
       2000, 3,
       TRUE, TRUE,
       14, 500
WHERE NOT EXISTS (SELECT 1 FROM plans WHERE code = 'growth');

-- Add stripe_invoice_id to subscription_usage for payment correlation
ALTER TABLE subscription_usage
    ADD COLUMN IF NOT EXISTS stripe_invoice_id VARCHAR(255);

-- Idempotent event deduplication table for Stripe webhook processing
CREATE TABLE IF NOT EXISTS stripe_processed_events (
    event_id   VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
