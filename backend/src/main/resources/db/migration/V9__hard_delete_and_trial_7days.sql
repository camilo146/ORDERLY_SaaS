-- ══════════════════════════════════════════════════════════════════════════
-- V9 — Hard delete cleanup + trial period to 7 days
-- ══════════════════════════════════════════════════════════════════════════

-- ── Update trial period from 14 to 7 days across all plans ────────────────
UPDATE plans SET trial_days = 7 WHERE trial_days IS NULL OR trial_days != 7;

-- ── Clean up existing soft-deleted (CANCELLED) businesses ─────────────────
-- All child tables have ON DELETE CASCADE from V1, so this one DELETE removes:
-- subscriptions, subscription_usage, orders, order_items, products, customers,
-- categories, whatsapp_channels, complaints, conversations, conversation_messages,
-- handoff_sessions, notification_queue, message_templates
-- audit_logs and email_logs are NOT linked by FK and are intentionally preserved.
DELETE FROM businesses WHERE status = 'CANCELLED';
