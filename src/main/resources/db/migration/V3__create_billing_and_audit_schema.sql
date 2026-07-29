-- Fresh baseline for an empty MySQL 8 database.
-- Tables are ordered so every referenced table already exists.

CREATE TABLE `fee_records` (
  `amount` decimal(12,2) NOT NULL,
  `discount_amount` decimal(12,2) DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `paid_amount` decimal(12,2) DEFAULT NULL,
  `month` varchar(7) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `center_id` bigint NOT NULL,
  `class_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_user_id` bigint NOT NULL,
  `status` enum('OVERDUE','PAID','PARTIAL','UNPAID') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_fee_records_center_id` (`center_id`),
  KEY `idx_fee_records_class_id` (`class_id`),
  KEY `idx_fee_records_student_user_id` (`student_user_id`),
  CONSTRAINT `fk_fee_records_student_user_id` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_fee_records_class_id` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
  CONSTRAINT `fk_fee_records_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `discounts` (
  `value` decimal(12,2) NOT NULL,
  `center_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by_user_id` bigint NOT NULL,
  `fee_record_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci,
  `type` enum('FIXED','PERCENTAGE') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_discounts_center_id` (`center_id`),
  KEY `idx_discounts_created_by_user_id` (`created_by_user_id`),
  KEY `idx_discounts_fee_record_id` (`fee_record_id`),
  CONSTRAINT `fk_discounts_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`),
  CONSTRAINT `fk_discounts_fee_record_id` FOREIGN KEY (`fee_record_id`) REFERENCES `fee_records` (`id`),
  CONSTRAINT `fk_discounts_created_by_user_id` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `installments` (
  `due_date` date NOT NULL,
  `expected_amount` decimal(12,2) NOT NULL,
  `paid_amount` decimal(12,2) DEFAULT NULL,
  `center_id` bigint NOT NULL,
  `fee_record_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `status` enum('OVERDUE','PAID','PARTIALLY_PAID','PENDING') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_installments_center_id` (`center_id`),
  KEY `idx_installments_fee_record_id` (`fee_record_id`),
  CONSTRAINT `fk_installments_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`),
  CONSTRAINT `fk_installments_fee_record_id` FOREIGN KEY (`fee_record_id`) REFERENCES `fee_records` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `payments` (
  `amount` decimal(12,2) NOT NULL,
  `center_id` bigint NOT NULL,
  `collected_by_user_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) DEFAULT NULL,
  `fee_record_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_user_id` bigint NOT NULL,
  `voided_at` datetime(6) DEFAULT NULL,
  `voided_by_user_id` bigint DEFAULT NULL,
  `receipt_number` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `idempotency_key` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `note` text COLLATE utf8mb4_unicode_ci,
  `sepay_ref` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `void_reason` text COLLATE utf8mb4_unicode_ci,
  `method` enum('BANK_TRANSFER','CASH','ONLINE','QR_CODE','SEPAY') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('ACTIVE','EXPIRED','PENDING','VOIDED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payments_receipt_number` (`receipt_number`),
  UNIQUE KEY `uk_payments_idempotency_key` (`idempotency_key`),
  KEY `idx_payments_center_id` (`center_id`),
  KEY `idx_payments_collected_by_user_id` (`collected_by_user_id`),
  KEY `idx_payments_fee_record_id` (`fee_record_id`),
  KEY `idx_payments_student_user_id` (`student_user_id`),
  KEY `idx_payments_voided_by_user_id` (`voided_by_user_id`),
  CONSTRAINT `fk_payments_student_user_id` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_payments_fee_record_id` FOREIGN KEY (`fee_record_id`) REFERENCES `fee_records` (`id`),
  CONSTRAINT `fk_payments_collected_by_user_id` FOREIGN KEY (`collected_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_payments_voided_by_user_id` FOREIGN KEY (`voided_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_payments_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `refunds` (
  `amount` decimal(12,2) NOT NULL,
  `center_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by_user_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `payment_id` bigint NOT NULL,
  `reason` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `idx_refunds_center_id` (`center_id`),
  KEY `idx_refunds_created_by_user_id` (`created_by_user_id`),
  KEY `idx_refunds_payment_id` (`payment_id`),
  CONSTRAINT `fk_refunds_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`),
  CONSTRAINT `fk_refunds_created_by_user_id` FOREIGN KEY (`created_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_refunds_payment_id` FOREIGN KEY (`payment_id`) REFERENCES `payments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `audit_logs` (
  `center_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `entity_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `ip_address` varchar(45) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `action` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `entity_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `idx_audit_logs_center_id` (`center_id`),
  KEY `idx_audit_logs_user_id` (`user_id`),
  CONSTRAINT `fk_audit_logs_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_audit_logs_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `sepay_webhook_events` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `matched_payment_id` bigint DEFAULT NULL,
  `processed_at` datetime(6) DEFAULT NULL,
  `received_at` datetime(6) NOT NULL,
  `sepay_transaction_id` bigint NOT NULL,
  `transfer_amount` bigint DEFAULT NULL,
  `content` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `processing_note` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `account_number` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `gateway` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `raw_payload` text COLLATE utf8mb4_unicode_ci,
  `reference_code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sub_account` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `transaction_date` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `transfer_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `processing_status` enum('FAILED','IGNORED','MATCHED','RECEIVED','UNMATCHED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sepay_event_id` (`sepay_transaction_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
