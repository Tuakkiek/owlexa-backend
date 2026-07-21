ALTER TABLE payments ADD COLUMN expires_at DATETIME NULL AFTER voided_at;

CREATE INDEX idx_payments_status_expires ON payments (status, expires_at);
