-- ══════════════════════════════════════════════════════════════════════════
-- V7 — CEO / Super Admin panel: audit logs, email logs, force-logout support
-- ══════════════════════════════════════════════════════════════════════════

-- ── Force-logout: allow the CEO to invalidate all existing sessions for a user
ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS forced_logout_at TIMESTAMPTZ;

-- ── Admin audit log: immutable record of every CEO action taken on the platform
CREATE TABLE IF NOT EXISTS admin_audit_logs (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID         NOT NULL,
    actor_email VARCHAR(255) NOT NULL,
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id   UUID,
    target_name VARCHAR(255),
    details     TEXT,
    ip_address  VARCHAR(45),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor   ON admin_audit_logs (actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_target  ON admin_audit_logs (target_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created ON admin_audit_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action  ON admin_audit_logs (action);

-- ── Email log: every email sent through the CEO panel is recorded here
CREATE TABLE IF NOT EXISTS email_logs (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_email VARCHAR(255) NOT NULL,
    recipient_name  VARCHAR(200),
    subject         VARCHAR(255) NOT NULL,
    body_text       TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'SENT',
    sent_at         TIMESTAMPTZ,
    error_message   TEXT,
    sent_by_id      UUID,
    sent_by_email   VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_email_logs_recipient ON email_logs (recipient_email);
CREATE INDEX IF NOT EXISTS idx_email_logs_created   ON email_logs (created_at DESC);
