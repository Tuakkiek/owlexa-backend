-- Regenerate demo schedules from course.default_session_count.
-- The schedule model now treats course session count as the source of truth.

DELETE FROM `attendances`;
DELETE FROM `schedule_events`;
DELETE FROM `schedule_recurring_rules`;
DELETE FROM `schedules`;

ALTER TABLE `schedules` AUTO_INCREMENT = 1;
ALTER TABLE `schedule_recurring_rules` AUTO_INCREMENT = 1001;
ALTER TABLE `schedule_events` AUTO_INCREMENT = 1001;

UPDATE `courses`
SET
  `default_session_count` = 24,
  `default_duration` = 90,
  `default_teacher_user_id` = 8,
  `updated_at` = NOW(6)
WHERE `id` = 1;

UPDATE `courses`
SET
  `default_session_count` = 18,
  `default_duration` = 150,
  `default_teacher_user_id` = 10,
  `updated_at` = NOW(6)
WHERE `id` = 2;

UPDATE `courses`
SET
  `default_session_count` = 16,
  `default_duration` = 150,
  `default_teacher_user_id` = 9,
  `updated_at` = NOW(6)
WHERE `id` = 3;

UPDATE `courses`
SET
  `default_session_count` = 20,
  `default_duration` = 90,
  `default_teacher_user_id` = 9,
  `updated_at` = NOW(6)
WHERE `id` = 4;

INSERT INTO `courses`
  (`code`, `name`, `description`, `default_duration`, `default_session_count`, `default_monthly_fee`, `default_max_students`, `default_teacher_user_id`, `is_active`, `created_at`, `updated_at`)
SELECT
  'VSTEP-B1',
  'VSTEP B1',
  'Demo VSTEP B1 course with exactly 24 sessions.',
  90,
  24,
  2500000,
  20,
  8,
  b'1',
  NOW(6),
  NOW(6)
WHERE NOT EXISTS (SELECT 1 FROM `courses` WHERE `code` = 'VSTEP-B1');

UPDATE `courses`
SET
  `name` = 'VSTEP B1',
  `description` = 'Demo VSTEP B1 course with exactly 24 sessions.',
  `default_duration` = 90,
  `default_session_count` = 24,
  `default_monthly_fee` = 2500000,
  `default_max_students` = 20,
  `default_teacher_user_id` = 8,
  `is_active` = b'1',
  `updated_at` = NOW(6)
WHERE `code` = 'VSTEP-B1';

SET @vstep_b1_course_id = (SELECT `id` FROM `courses` WHERE `code` = 'VSTEP-B1' LIMIT 1);
SET @vstep_b1_class_id = (
  SELECT MIN(`id`)
  FROM `classes`
  WHERE `center_id` = 1 AND `course_id` = @vstep_b1_course_id
);

INSERT INTO `classes`
  (`center_id`, `course_id`, `teacher_user_id`, `name`, `description`, `start_date`, `end_date`, `max_students`, `monthly_fee`, `status`, `create_at`)
SELECT
  1,
  @vstep_b1_course_id,
  8,
  'VSTEP B1 T8-2026',
  'Tue/Thu/Sat evening class generated from course session count.',
  '2026-08-04',
  '2026-09-26',
  20,
  2500000,
  'ACTIVE',
  NOW(6)
WHERE @vstep_b1_class_id IS NULL;

SET @vstep_b1_class_id = (
  SELECT MIN(`id`)
  FROM `classes`
  WHERE `center_id` = 1 AND `course_id` = @vstep_b1_course_id
);

UPDATE `classes`
SET
  `name` = 'VSTEP B1 T8-2026',
  `description` = 'Tue/Thu/Sat evening class generated from course session count.',
  `start_date` = '2026-08-04',
  `end_date` = '2026-09-26',
  `teacher_user_id` = 8,
  `max_students` = 20,
  `monthly_fee` = 2500000,
  `status` = 'ACTIVE'
WHERE `id` = @vstep_b1_class_id;

CREATE TEMPORARY TABLE `tmp_schedule_rule_seed` (
  `rule_id` BIGINT PRIMARY KEY,
  `center_id` BIGINT NOT NULL,
  `class_id` BIGINT NOT NULL,
  `room_id` BIGINT NOT NULL,
  `teacher_user_id` BIGINT NOT NULL,
  `days_of_week` VARCHAR(32) NOT NULL,
  `start_date` DATE NOT NULL,
  `start_time` TIME NOT NULL,
  `end_time` TIME NOT NULL,
  `schedule_type` VARCHAR(32) NOT NULL,
  `session_count` INT NOT NULL,
  `title` VARCHAR(255) NOT NULL
);

INSERT INTO `tmp_schedule_rule_seed`
  (`rule_id`, `center_id`, `class_id`, `room_id`, `teacher_user_id`, `days_of_week`, `start_date`, `start_time`, `end_time`, `schedule_type`, `session_count`, `title`)
VALUES
  (1001, 1, 1, 1, 8,  '1,3,5', '2026-07-27', '19:45:00', '21:15:00', 'THEORY_CLASS', 24, 'TOEIC 650+ T8-2026'),
  (1002, 1, 2, 2, 10, '2,4,6', '2026-07-28', '18:00:00', '20:30:00', 'THEORY_CLASS', 18, 'IELTS Speaking T8-2026'),
  (1003, 1, 3, 3, 9,  '6,7',   '2026-08-01', '08:00:00', '10:30:00', 'THEORY_CLASS', 16, 'VSTEP B2 Weekend T8'),
  (1004, 1, 4, 4, 9,  '1,3',   '2026-07-27', '20:00:00', '21:30:00', 'ONLINE_CLASS', 20, 'Communication Online T8'),
  (1005, 2, 6, 5, 11, '2,4',   '2026-07-28', '18:30:00', '20:30:00', 'THEORY_CLASS', 24, 'TD TOEIC 650+ T8'),
  (1006, 1, @vstep_b1_class_id, 1, 8, '2,4,6', '2026-08-04', '19:45:00', '21:15:00', 'THEORY_CLASS', 24, 'VSTEP B1 T8-2026');

CREATE TEMPORARY TABLE `tmp_schedule_numbers` (`n` INT PRIMARY KEY);

INSERT INTO `tmp_schedule_numbers` (`n`)
WITH RECURSIVE `seq` (`n`) AS (
  SELECT 0
  UNION ALL
  SELECT `n` + 1 FROM `seq` WHERE `n` < 220
)
SELECT `n` FROM `seq`;

CREATE TEMPORARY TABLE `tmp_schedule_occurrences` AS
SELECT
  `ranked`.`rule_id`,
  `ranked`.`center_id`,
  `ranked`.`class_id`,
  `ranked`.`room_id`,
  `ranked`.`teacher_user_id`,
  `ranked`.`days_of_week`,
  `ranked`.`start_date`,
  `ranked`.`start_time`,
  `ranked`.`end_time`,
  `ranked`.`schedule_type`,
  `ranked`.`session_count`,
  `ranked`.`title`,
  `ranked`.`event_date`,
  `ranked`.`lesson_number`
FROM (
  SELECT
    `seed`.*,
    DATE_ADD(`seed`.`start_date`, INTERVAL `numbers`.`n` DAY) AS `event_date`,
    ROW_NUMBER() OVER (
      PARTITION BY `seed`.`rule_id`
      ORDER BY DATE_ADD(`seed`.`start_date`, INTERVAL `numbers`.`n` DAY)
    ) AS `lesson_number`
  FROM `tmp_schedule_rule_seed` `seed`
  JOIN `tmp_schedule_numbers` `numbers`
    ON FIND_IN_SET(WEEKDAY(DATE_ADD(`seed`.`start_date`, INTERVAL `numbers`.`n` DAY)) + 1, `seed`.`days_of_week`) > 0
) `ranked`
WHERE `ranked`.`lesson_number` <= `ranked`.`session_count`;

INSERT INTO `schedule_recurring_rules`
  (`id`, `center_id`, `class_id`, `room_id`, `teacher_user_id`, `repeat_type`, `days_of_week`, `start_date`, `end_date`, `start_time`, `end_time`, `type`, `is_active`, `created_at`, `updated_at`)
SELECT
  `seed`.`rule_id`,
  `seed`.`center_id`,
  `seed`.`class_id`,
  `seed`.`room_id`,
  `seed`.`teacher_user_id`,
  'WEEKLY',
  `seed`.`days_of_week`,
  `seed`.`start_date`,
  MAX(`occ`.`event_date`),
  `seed`.`start_time`,
  `seed`.`end_time`,
  `seed`.`schedule_type`,
  b'1',
  NOW(6),
  NOW(6)
FROM `tmp_schedule_rule_seed` `seed`
JOIN `tmp_schedule_occurrences` `occ` ON `occ`.`rule_id` = `seed`.`rule_id`
GROUP BY
  `seed`.`rule_id`,
  `seed`.`center_id`,
  `seed`.`class_id`,
  `seed`.`room_id`,
  `seed`.`teacher_user_id`,
  `seed`.`days_of_week`,
  `seed`.`start_date`,
  `seed`.`start_time`,
  `seed`.`end_time`,
  `seed`.`schedule_type`;

INSERT INTO `schedule_events`
  (`center_id`, `class_id`, `recurring_rule_id`, `room_id`, `teacher_user_id`, `event_date`, `start_time`, `end_time`, `lesson_number`, `event_type`, `status`, `title`, `note`, `created_at`, `updated_at`)
SELECT
  `occ`.`center_id`,
  `occ`.`class_id`,
  `occ`.`rule_id`,
  `occ`.`room_id`,
  `occ`.`teacher_user_id`,
  `occ`.`event_date`,
  `occ`.`start_time`,
  `occ`.`end_time`,
  `occ`.`lesson_number`,
  CASE
    WHEN `occ`.`rule_id` = 1006 AND `occ`.`lesson_number` = `occ`.`session_count` THEN 'EXAM'
    WHEN `occ`.`schedule_type` = 'ONLINE_CLASS' THEN 'ONLINE_LESSON'
    ELSE 'LESSON'
  END,
  'SCHEDULED',
  CASE
    WHEN `occ`.`rule_id` = 1006 AND `occ`.`lesson_number` = `occ`.`session_count` THEN 'Kiem tra cuoi khoa VSTEP B1'
    ELSE `occ`.`title`
  END,
  CASE
    WHEN `occ`.`rule_id` = 1006 AND `occ`.`lesson_number` = `occ`.`session_count` THEN 'Demo override: final generated lesson is replaced by an exam event.'
    ELSE NULL
  END,
  NOW(6),
  NOW(6)
FROM `tmp_schedule_occurrences` `occ`
ORDER BY `occ`.`rule_id`, `occ`.`lesson_number`;

INSERT INTO `schedule_events`
  (`center_id`, `class_id`, `recurring_rule_id`, `room_id`, `teacher_user_id`, `event_date`, `start_time`, `end_time`, `lesson_number`, `event_type`, `status`, `title`, `note`, `created_at`, `updated_at`)
VALUES
  (1, 1, NULL, 3, 8,  '2026-08-02', '09:00:00', '10:30:00', NULL, 'PRACTICE', 'SCHEDULED', 'TOEIC Speaking Lab', 'One-off practice event, not generated from a rule.', NOW(6), NOW(6)),
  (1, 2, NULL, 2, 10, '2026-09-12', '08:30:00', '10:30:00', NULL, 'EXAM', 'SCHEDULED', 'IELTS Speaking Mock Test', 'One-off exam event.', NOW(6), NOW(6));

UPDATE `classes` `c`
JOIN (
  SELECT `class_id`, MIN(`event_date`) AS `start_date`, MAX(`event_date`) AS `end_date`
  FROM `tmp_schedule_occurrences`
  GROUP BY `class_id`
) `bounds` ON `bounds`.`class_id` = `c`.`id`
SET
  `c`.`start_date` = `bounds`.`start_date`,
  `c`.`end_date` = `bounds`.`end_date`;

DROP TEMPORARY TABLE `tmp_schedule_occurrences`;
DROP TEMPORARY TABLE `tmp_schedule_numbers`;
DROP TEMPORARY TABLE `tmp_schedule_rule_seed`;
