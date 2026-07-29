ALTER TABLE `assessments`
    ADD COLUMN `audio_file_id` BIGINT DEFAULT NULL AFTER `content_json`,
    ADD COLUMN `playback_mode` ENUM('EXAM','PRACTICE') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PRACTICE' AFTER `audio_file_id`;

CREATE INDEX `idx_assessments_audio_file_id`
    ON `assessments` (`audio_file_id`);

ALTER TABLE `assessments`
    ADD CONSTRAINT `fk_assessments_audio_file_id`
        FOREIGN KEY (`audio_file_id`) REFERENCES `files` (`id`);

ALTER TABLE `assignments`
    ADD COLUMN `audio_file_id` BIGINT DEFAULT NULL AFTER `assessment_snapshot_at`,
    ADD COLUMN `playback_mode` ENUM('EXAM','PRACTICE') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PRACTICE' AFTER `audio_file_id`;

CREATE INDEX `idx_assignments_audio_file_id`
    ON `assignments` (`audio_file_id`);

ALTER TABLE `assignments`
    ADD CONSTRAINT `fk_assignments_audio_file_id`
        FOREIGN KEY (`audio_file_id`) REFERENCES `files` (`id`);

-- Complete the Listening demo workflow after audio columns exist.
UPDATE `assessments`
SET `audio_file_id` = 1,
    `playback_mode` = 'PRACTICE'
WHERE `id` = 3;

UPDATE `assignments`
SET `audio_file_id` = 1,
    `playback_mode` = 'PRACTICE'
WHERE `id` = 3;

INSERT INTO `file_references` (
    `id`,
    `file_id`,
    `center_id`,
    `owner_type`,
    `owner_id`,
    `created_at`
) VALUES
    (1, 1, 1, 'ASSESSMENT', 3, '2026-03-01 10:00:00.000000'),
    (2, 1, 1, 'ASSIGNMENT', 3, '2026-07-20 09:05:00.000000');
