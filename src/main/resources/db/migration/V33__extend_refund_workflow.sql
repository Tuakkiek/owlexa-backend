-- Extend refunds table with approval workflow fields
ALTER TABLE refunds
  ADD COLUMN status ENUM('REQUESTED','APPROVED','REJECTED','PAID') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'REQUESTED',
  ADD COLUMN refund_method ENUM('BANK_TRANSFER','CASH','ONLINE','QR_CODE','SEPAY') COLLATE utf8mb4_unicode_ci NULL,
  ADD COLUMN requested_by_user_id BIGINT NULL,
  ADD COLUMN approved_by_user_id BIGINT NULL,
  ADD COLUMN approved_at DATETIME(6) NULL,
  ADD COLUMN rejected_reason TEXT NULL,
  ADD COLUMN related_enrollment_id BIGINT NULL,
  ADD CONSTRAINT fk_refunds_requested_by FOREIGN KEY (requested_by_user_id) REFERENCES users (id),
  ADD CONSTRAINT fk_refunds_approved_by FOREIGN KEY (approved_by_user_id) REFERENCES users (id),
  ADD CONSTRAINT fk_refunds_related_enrollment FOREIGN KEY (related_enrollment_id) REFERENCES class_enrollments (id);

-- Backfill: set existing refunds to PAID status (they were instant refunds)
UPDATE refunds SET status = 'PAID', requested_by_user_id = created_by_user_id WHERE status = 'REQUESTED';
