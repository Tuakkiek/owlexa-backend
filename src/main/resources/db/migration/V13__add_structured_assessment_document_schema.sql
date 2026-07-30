-- Structured assessment-document storage. This migration is additive only;
-- existing assessments and assignments remain LEGACY and receive no block backfill.

ALTER TABLE `assessments`
  ADD COLUMN `document_format` ENUM('LEGACY','STRUCTURED_V1')
    COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'LEGACY' AFTER `type`,
  ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0 AFTER `updated_at`;

CREATE TABLE `assessment_blocks` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `assessment_id` BIGINT NOT NULL,
  `block_type` ENUM('RICH_TEXT','IMAGE','AUDIO','QUESTION','DIVIDER','PAGE_BREAK')
    COLLATE utf8mb4_unicode_ci NOT NULL,
  `position` INT NOT NULL,
  `content_json` LONGTEXT COLLATE utf8mb4_bin DEFAULT NULL,
  `file_id` BIGINT DEFAULT NULL,
  `caption` VARCHAR(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `alignment` ENUM('LEFT','CENTER','RIGHT')
    COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `question_id` BIGINT DEFAULT NULL,
  `points` DECIMAL(6,2) DEFAULT NULL,
  `assessment_item_id` BIGINT DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assessment_blocks_assessment_position` (`assessment_id`, `position`),
  UNIQUE KEY `uk_assessment_blocks_assessment_question` (`assessment_id`, `question_id`),
  UNIQUE KEY `uk_assessment_blocks_assessment_item` (`assessment_item_id`),
  KEY `idx_assessment_blocks_file_id` (`file_id`),
  KEY `idx_assessment_blocks_question_id` (`question_id`),
  CONSTRAINT `chk_assessment_blocks_position` CHECK (`position` >= 1),
  CONSTRAINT `chk_assessment_blocks_content_json`
    CHECK (`content_json` IS NULL OR JSON_VALID(`content_json`)),
  CONSTRAINT `chk_assessment_blocks_payload` CHECK (
    (
      `block_type` = 'RICH_TEXT'
      AND `content_json` IS NOT NULL
      AND `file_id` IS NULL
      AND `caption` IS NULL
      AND `alignment` IS NULL
      AND `question_id` IS NULL
      AND `points` IS NULL
      AND `assessment_item_id` IS NULL
    )
    OR (
      `block_type` = 'IMAGE'
      AND `content_json` IS NULL
      AND `file_id` IS NOT NULL
      AND `question_id` IS NULL
      AND `points` IS NULL
      AND `assessment_item_id` IS NULL
    )
    OR (
      `block_type` = 'AUDIO'
      AND `content_json` IS NULL
      AND `file_id` IS NOT NULL
      AND `caption` IS NULL
      AND `alignment` IS NULL
      AND `question_id` IS NULL
      AND `points` IS NULL
      AND `assessment_item_id` IS NULL
    )
    OR (
      `block_type` = 'QUESTION'
      AND `content_json` IS NULL
      AND `file_id` IS NULL
      AND `caption` IS NULL
      AND `alignment` IS NULL
      AND `question_id` IS NOT NULL
      AND `points` IS NOT NULL
      AND `points` > 0
    )
    OR (
      `block_type` IN ('DIVIDER', 'PAGE_BREAK')
      AND `content_json` IS NULL
      AND `file_id` IS NULL
      AND `caption` IS NULL
      AND `alignment` IS NULL
      AND `question_id` IS NULL
      AND `points` IS NULL
      AND `assessment_item_id` IS NULL
    )
  ),
  CONSTRAINT `fk_assessment_blocks_assessment_id`
    FOREIGN KEY (`assessment_id`) REFERENCES `assessments` (`id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_assessment_blocks_file_id`
    FOREIGN KEY (`file_id`) REFERENCES `files` (`id`),
  CONSTRAINT `fk_assessment_blocks_question_id`
    FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`),
  CONSTRAINT `fk_assessment_blocks_assessment_item_id`
    FOREIGN KEY (`assessment_item_id`) REFERENCES `assessment_items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `assignments`
  ADD COLUMN `document_format` ENUM('LEGACY','STRUCTURED_V1')
    COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'LEGACY' AFTER `type`;

CREATE TABLE `assignment_blocks` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `assignment_id` BIGINT NOT NULL,
  `block_type` ENUM('RICH_TEXT','IMAGE','AUDIO','QUESTION','DIVIDER','PAGE_BREAK')
    COLLATE utf8mb4_unicode_ci NOT NULL,
  `position` INT NOT NULL,
  `content_json` LONGTEXT COLLATE utf8mb4_bin DEFAULT NULL,
  `file_id` BIGINT DEFAULT NULL,
  `caption` VARCHAR(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `alignment` ENUM('LEFT','CENTER','RIGHT')
    COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `assignment_item_id` BIGINT DEFAULT NULL,
  `created_at` DATETIME(6) NOT NULL,
  `updated_at` DATETIME(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_assignment_blocks_assignment_position` (`assignment_id`, `position`),
  UNIQUE KEY `uk_assignment_blocks_assignment_item` (`assignment_item_id`),
  KEY `idx_assignment_blocks_file_id` (`file_id`),
  CONSTRAINT `chk_assignment_blocks_position` CHECK (`position` >= 1),
  CONSTRAINT `chk_assignment_blocks_content_json`
    CHECK (`content_json` IS NULL OR JSON_VALID(`content_json`)),
  CONSTRAINT `chk_assignment_blocks_payload` CHECK (
    (
      `block_type` = 'RICH_TEXT'
      AND `content_json` IS NOT NULL
      AND `file_id` IS NULL
      AND `caption` IS NULL
      AND `alignment` IS NULL
      AND `assignment_item_id` IS NULL
    )
    OR (
      `block_type` = 'IMAGE'
      AND `content_json` IS NULL
      AND `file_id` IS NOT NULL
      AND `assignment_item_id` IS NULL
    )
    OR (
      `block_type` = 'AUDIO'
      AND `content_json` IS NULL
      AND `file_id` IS NOT NULL
      AND `caption` IS NULL
      AND `alignment` IS NULL
      AND `assignment_item_id` IS NULL
    )
    OR (
      `block_type` = 'QUESTION'
      AND `content_json` IS NULL
      AND `file_id` IS NULL
      AND `caption` IS NULL
      AND `alignment` IS NULL
      AND `assignment_item_id` IS NOT NULL
    )
    OR (
      `block_type` IN ('DIVIDER', 'PAGE_BREAK')
      AND `content_json` IS NULL
      AND `file_id` IS NULL
      AND `caption` IS NULL
      AND `alignment` IS NULL
      AND `assignment_item_id` IS NULL
    )
  ),
  CONSTRAINT `fk_assignment_blocks_assignment_id`
    FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`)
    ON DELETE CASCADE,
  CONSTRAINT `fk_assignment_blocks_file_id`
    FOREIGN KEY (`file_id`) REFERENCES `files` (`id`),
  CONSTRAINT `fk_assignment_blocks_assignment_item_id`
    FOREIGN KEY (`assignment_item_id`) REFERENCES `assignment_items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
