-- Fresh baseline for an empty MySQL 8 database.
-- Tables are ordered so every referenced table already exists.

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `full_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone_number` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` enum('ACADEMIC_STAFF','ADMIN','CASHIER','MANAGER','OWNER','STUDENT','TEACHER') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users_phone_number` (`phone_number`),
  UNIQUE KEY `uk_users_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `permissions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permissions_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `centers` (
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `owner_user_id` bigint NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `subdomain` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_centers_subdomain` (`subdomain`),
  KEY `idx_centers_owner_user_id` (`owner_user_id`),
  CONSTRAINT `fk_centers_owner_user_id` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `membership` (
  `center_id` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `joined_at` datetime(6) NOT NULL,
  `joined_by_user_id` bigint DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_membership_user_id_center_id` (`user_id`,`center_id`),
  KEY `idx_membership_center_id` (`center_id`),
  KEY `idx_membership_joined_by_user_id` (`joined_by_user_id`),
  CONSTRAINT `fk_membership_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`),
  CONSTRAINT `fk_membership_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_membership_joined_by_user_id` FOREIGN KEY (`joined_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `permission_id` bigint NOT NULL,
  `role` enum('ACADEMIC_STAFF','ADMIN','CASHIER','MANAGER','OWNER','STUDENT','TEACHER') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission_role_permission_id` (`role`,`permission_id`),
  KEY `idx_role_permission_permission_id` (`permission_id`),
  CONSTRAINT `fk_role_permission_permission_id` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_permission` (
  `granted_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `permission_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_permission_user_id_permission_id` (`user_id`,`permission_id`),
  KEY `idx_user_permission_permission_id` (`permission_id`),
  CONSTRAINT `fk_user_permission_permission_id` FOREIGN KEY (`permission_id`) REFERENCES `permissions` (`id`),
  CONSTRAINT `fk_user_permission_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `user_sessions` (
  `is_active` bit(1) NOT NULL,
  `rotation_count` int NOT NULL,
  `absolute_expire_at` datetime(6) NOT NULL,
  `center_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `expired_at` datetime(6) NOT NULL,
  `last_used_at` datetime(6) DEFAULT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  `device_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `id` varchar(36) COLLATE utf8mb4_unicode_ci NOT NULL,
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `revoked_reason` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `device_key` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `refresh_token_hash` varchar(88) COLLATE utf8mb4_unicode_ci NOT NULL,
  `device_name` varchar(120) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `user_agent` text COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `idx_sessions_user_id` (`user_id`),
  KEY `idx_sessions_id_active` (`id`,`is_active`),
  KEY `idx_sessions_user_device` (`user_id`,`device_key`),
  KEY `idx_sessions_cleanup` (`is_active`,`last_used_at`),
  KEY `idx_user_sessions_center_id` (`center_id`),
  CONSTRAINT `fk_user_sessions_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_user_sessions_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`),
  CONSTRAINT `user_sessions_chk_1` CHECK ((`device_type` in (_utf8mb4'DESKTOP',_utf8mb4'UNKNOWN',_utf8mb4'TABLET',_utf8mb4'MOBILE')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
