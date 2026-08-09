-- Add center_id column to courses table to enforce tenant isolation per center
ALTER TABLE `courses`
  ADD COLUMN `center_id` bigint DEFAULT NULL AFTER `id`,
  ADD KEY `idx_courses_center_id` (`center_id`),
  ADD CONSTRAINT `fk_courses_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`);

-- Backfill existing courses by inferring center_id from associated classes, or falling back to the first center
UPDATE `courses` c
SET c.`center_id` = COALESCE(
  (SELECT cl.`center_id` FROM `classes` cl WHERE cl.`course_id` = c.`id` LIMIT 1),
  (SELECT `id` FROM `centers` ORDER BY `id` ASC LIMIT 1)
)
WHERE c.`center_id` IS NULL;

-- Drop global unique code index and add tenant-scoped composite unique index
ALTER TABLE `courses`
  DROP INDEX `uk_courses_code`,
  ADD UNIQUE KEY `uk_courses_center_code` (`center_id`, `code`);
