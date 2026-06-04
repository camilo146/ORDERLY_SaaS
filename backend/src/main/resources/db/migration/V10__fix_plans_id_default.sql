-- V10 — Ensure plans.id has gen_random_uuid() default.
-- Needed on servers where the table was created by Hibernate ddl-auto (no DEFAULT)
-- instead of by Flyway V1 (which includes DEFAULT gen_random_uuid()).
ALTER TABLE plans
    ALTER COLUMN id SET DEFAULT gen_random_uuid();
