-- Create assessment_content_blocks and assignment_content_blocks tables for document-driven multi-block assessment architecture.

CREATE TABLE `assessment_content_blocks` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `assessment_id` BIGINT NOT NULL,
  `position` INT NOT NULL DEFAULT 0,
  `title` VARCHAR(255) NULL,
  `content_json` LONGTEXT COLLATE utf8mb4_bin NOT NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_assessment_content_blocks_assessment` (`assessment_id`, `position`),
  CONSTRAINT `fk_assessment_content_blocks_assessment`
    FOREIGN KEY (`assessment_id`) REFERENCES `assessments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_assessment_blocks_content_json`
    CHECK (JSON_VALID(`content_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `assignment_content_blocks` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `assignment_id` BIGINT NOT NULL,
  `assessment_block_id` BIGINT NULL,
  `position` INT NOT NULL DEFAULT 0,
  `title` VARCHAR(255) NULL,
  `content_json` LONGTEXT COLLATE utf8mb4_bin NOT NULL,
  `created_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_assignment_content_blocks_assignment` (`assignment_id`, `position`),
  CONSTRAINT `fk_assignment_content_blocks_assignment`
    FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_assignment_blocks_content_json`
    CHECK (JSON_VALID(`content_json`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `assessment_items`
  ADD COLUMN `block_id` BIGINT NULL AFTER `assessment_id`,
  ADD CONSTRAINT `fk_assessment_items_block`
    FOREIGN KEY (`block_id`) REFERENCES `assessment_content_blocks` (`id`) ON DELETE SET NULL;

ALTER TABLE `assignment_items`
  ADD COLUMN `block_id` BIGINT NULL AFTER `assignment_id`,
  ADD CONSTRAINT `fk_assignment_items_block`
    FOREIGN KEY (`block_id`) REFERENCES `assignment_content_blocks` (`id`) ON DELETE SET NULL;

-- Migration data: Backfill default block for existing assessments
INSERT INTO `assessment_content_blocks` (`assessment_id`, `position`, `title`, `content_json`, `created_at`, `updated_at`)
SELECT
  a.`id`,
  0,
  'Nội dung chính',
  COALESCE(
    NULLIF(TRIM(a.`content_json`), ''),
    '{"type":"doc","content":[{"type":"paragraph"}]}'
  ),
  NOW(6),
  NOW(6)
FROM `assessments` a;

-- Link existing assessment_items to their assessment's default block
UPDATE `assessment_items` ai
JOIN `assessment_content_blocks` acb ON acb.`assessment_id` = ai.`assessment_id` AND acb.`position` = 0
SET ai.`block_id` = acb.`id`;

-- Migration data: Backfill default block for existing assignments
INSERT INTO `assignment_content_blocks` (`assignment_id`, `assessment_block_id`, `position`, `title`, `content_json`, `created_at`, `updated_at`)
SELECT
  a.`id`,
  acb.`id`,
  0,
  'Nội dung chính',
  COALESCE(
    NULLIF(TRIM(a.`content_json`), ''),
    '{"type":"doc","content":[{"type":"paragraph"}]}'
  ),
  NOW(6),
  NOW(6)
FROM `assignments` a
LEFT JOIN `assessment_content_blocks` acb ON acb.`assessment_id` = a.`assessment_id` AND acb.`position` = 0;

-- Link existing assignment_items to their assignment's default block
UPDATE `assignment_items` ai
JOIN `assignment_content_blocks` acb ON acb.`assignment_id` = ai.`assignment_id` AND acb.`position` = 0
SET ai.`block_id` = acb.`id`;
