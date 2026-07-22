-- Migration: Add idempotency_key column to payments table
-- Purpose: Support idempotency for payment creation (prevents duplicate payments on retry)
-- Date: 2026-07-22

-- Add the column (nullable — only set when client sends Idempotency-Key header)
ALTER TABLE payments
ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(64) NULL;

-- Add unique constraint to enforce one-payment-per-idempotency-key
-- Uses a partial unique index: only non-null keys are constrained
-- MySQL syntax:
-- ALTER TABLE payments ADD UNIQUE INDEX uk_payments_idempotency_key (idempotency_key);
-- For databases that don't support partial unique indexes, use:
CREATE UNIQUE INDEX IF NOT EXISTS uk_payments_idempotency_key ON payments (idempotency_key);
