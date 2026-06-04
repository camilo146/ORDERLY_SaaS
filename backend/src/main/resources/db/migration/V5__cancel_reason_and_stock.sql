-- ══════════════════════════════════════════════════════════════════════════
-- V5 — Cancel reason for orders + stock tracking for products
-- ══════════════════════════════════════════════════════════════════════════

-- Cancel reason stored when an order is rejected/cancelled
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS cancel_reason TEXT DEFAULT NULL;

-- Stock tracking per product (NULL = unlimited)
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS stock INTEGER DEFAULT NULL;
