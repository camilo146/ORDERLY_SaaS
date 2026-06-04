-- V11 — Add email system columns to app_users.
-- Hibernate ddl-auto:update cannot safely add email_verified (boolean NOT NULL)
-- to a table with existing rows without a DEFAULT, so we do it explicitly here.
ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS email_verified           BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS email_verification_token VARCHAR(255),
    ADD COLUMN IF NOT EXISTS email_verification_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS password_reset_token     VARCHAR(255),
    ADD COLUMN IF NOT EXISTS password_reset_expires_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_login_at            TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS onboarding_email_sent_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS inactivity_email_sent_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS idx_app_users_verification_token
    ON app_users (email_verification_token)
    WHERE email_verification_token IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_app_users_reset_token
    ON app_users (password_reset_token)
    WHERE password_reset_token IS NOT NULL;
