-- Rich assessment documents and centralized file metadata.
-- Binary objects are deliberately kept outside the database.

ALTER TABLE `assessments`
  ADD COLUMN `content_json` LONGTEXT COLLATE utf8mb4_bin NULL AFTER `description`;

-- Preserve existing plain-text descriptions as a valid ProseMirror document.
UPDATE `assessments`
SET `content_json` =
  CASE
    WHEN `description` IS NULL OR TRIM(`description`) = '' THEN
      JSON_OBJECT(
        'type', 'doc',
        'content', JSON_ARRAY(JSON_OBJECT('type', 'paragraph'))
      )
    ELSE
      JSON_OBJECT(
        'type', 'doc',
        'content', JSON_ARRAY(
          JSON_OBJECT(
            'type', 'paragraph',
            'content', JSON_ARRAY(
              JSON_OBJECT('type', 'text', 'text', `description`)
            )
          )
        )
      )
  END;

ALTER TABLE `assessments`
  MODIFY COLUMN `content_json` LONGTEXT COLLATE utf8mb4_bin NOT NULL,
  ADD CONSTRAINT `chk_assessments_content_json`
    CHECK (JSON_VALID(`content_json`));

CREATE TABLE `files` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `center_id` BIGINT NOT NULL,
  `original_name` VARCHAR(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `stored_name` VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `mime_type` VARCHAR(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `file_type` ENUM('IMAGE','AUDIO','VIDEO','PDF','ATTACHMENT')
    COLLATE utf8mb4_unicode_ci NOT NULL,
  `extension` VARCHAR(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `size` BIGINT NOT NULL,
  `path` VARCHAR(1024) COLLATE utf8mb4_unicode_ci NOT NULL,
  `url` VARCHAR(2048) COLLATE utf8mb4_unicode_ci NOT NULL,
  `storage_provider` ENUM('LOCAL','S3') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` ENUM('TEMPORARY','ACTIVE','ORPHANED','DELETED')
    COLLATE utf8mb4_unicode_ci NOT NULL,
  `uploaded_by` BIGINT NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  `last_referenced_at` DATETIME(6) DEFAULT NULL,
  `orphaned_at` DATETIME(6) DEFAULT NULL,
  `deleted_at` DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_files_stored_name` (`stored_name`),
  KEY `idx_files_center_status_created` (`center_id`, `status`, `created_at`),
  KEY `idx_files_uploaded_by` (`uploaded_by`),
  KEY `idx_files_orphaned_at` (`orphaned_at`),
  CONSTRAINT `fk_files_center_id`
    FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`),
  CONSTRAINT `fk_files_uploaded_by`
    FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`id`),
  CONSTRAINT `chk_files_size` CHECK (`size` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `file_references` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `file_id` BIGINT NOT NULL,
  `center_id` BIGINT NOT NULL,
  `owner_type` ENUM('ASSESSMENT','QUESTION','ASSIGNMENT')
    COLLATE utf8mb4_unicode_ci NOT NULL,
  `owner_id` BIGINT NOT NULL,
  `created_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_file_references_owner_file`
    (`owner_type`, `owner_id`, `file_id`),
  KEY `idx_file_references_file_id` (`file_id`),
  KEY `idx_file_references_owner` (`owner_type`, `owner_id`),
  KEY `idx_file_references_center_id` (`center_id`),
  CONSTRAINT `fk_file_references_file_id`
    FOREIGN KEY (`file_id`) REFERENCES `files` (`id`),
  CONSTRAINT `fk_file_references_center_id`
    FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Demo audio metadata used by the Listening Assessment seed completed in V11.
-- The binary object remains external to the database by design.
INSERT INTO `files` (
  `id`,
  `center_id`,
  `original_name`,
  `stored_name`,
  `mime_type`,
  `file_type`,
  `extension`,
  `size`,
  `path`,
  `url`,
  `storage_provider`,
  `status`,
  `uploaded_by`,
  `created_at`,
  `updated_at`,
  `last_referenced_at`
) VALUES (
  1,
  1,
  'toeic-test-1-listening.mp3',
  'seed-toeic-test-1-listening.mp3',
  'audio/mpeg',
  'AUDIO',
  'mp3',
  5242880,
  'demo/listening/toeic-test-1-listening.mp3',
  'https://cdn.owlexa.vn/demo/listening/toeic-test-1-listening.mp3',
  'S3',
  'ACTIVE',
  8,
  '2026-03-01 09:55:00.000000',
  '2026-03-01 09:55:00.000000',
  '2026-07-20 09:05:00.000000'
);
