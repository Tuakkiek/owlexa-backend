-- Preserve assessment-level rich content in assignment snapshots.

ALTER TABLE `assignments`
  ADD COLUMN `content_json` LONGTEXT COLLATE utf8mb4_bin NULL AFTER `description`;

UPDATE `assignments` a
JOIN `assessments` s ON s.`id` = a.`assessment_id`
SET a.`content_json` =
  CASE
    WHEN s.`content_json` IS NULL OR TRIM(s.`content_json`) = '' THEN
      JSON_OBJECT(
        'type', 'doc',
        'content', JSON_ARRAY(JSON_OBJECT('type', 'paragraph'))
      )
    ELSE s.`content_json`
  END;

UPDATE `assignments`
SET `content_json` = JSON_OBJECT(
    'type', 'doc',
    'content', JSON_ARRAY(JSON_OBJECT('type', 'paragraph'))
)
WHERE `content_json` IS NULL OR TRIM(`content_json`) = '';

ALTER TABLE `assignments`
  MODIFY COLUMN `content_json` LONGTEXT COLLATE utf8mb4_bin NOT NULL,
  ADD CONSTRAINT `chk_assignments_content_json`
    CHECK (JSON_VALID(`content_json`));
