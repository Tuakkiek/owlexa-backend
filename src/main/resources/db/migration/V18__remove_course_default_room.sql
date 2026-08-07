ALTER TABLE `courses`
  DROP FOREIGN KEY `fk_courses_default_room_id`,
  DROP INDEX `idx_courses_default_room_id`,
  DROP COLUMN `default_room_id`;
