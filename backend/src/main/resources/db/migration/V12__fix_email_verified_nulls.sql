-- V12 — Backfill NULL email_verified values left by Hibernate ddl-auto.
-- Hibernate added the column without DEFAULT, so existing rows have NULL.
-- V11 skipped adding it (IF NOT EXISTS), leaving the NULL problem.
UPDATE app_users SET email_verified = FALSE WHERE email_verified IS NULL;
ALTER TABLE app_users ALTER COLUMN email_verified SET DEFAULT FALSE;
