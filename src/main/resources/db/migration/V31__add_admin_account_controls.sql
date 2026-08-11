ALTER TABLE `users`
  ADD COLUMN `is_active` bit(1) NOT NULL DEFAULT b'1';

ALTER TABLE `centers`
  ADD COLUMN `is_active` bit(1) NOT NULL DEFAULT b'1';

CREATE TABLE `admin_audit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_user_id` bigint NOT NULL,
  `action` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `target_id` bigint NOT NULL,
  `target_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `previous_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `new_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_admin_audit_created_at` (`created_at`),
  KEY `idx_admin_audit_target` (`target_type`, `target_id`),
  KEY `idx_admin_audit_admin_user_id` (`admin_user_id`),
  CONSTRAINT `fk_admin_audit_admin_user_id`
    FOREIGN KEY (`admin_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
