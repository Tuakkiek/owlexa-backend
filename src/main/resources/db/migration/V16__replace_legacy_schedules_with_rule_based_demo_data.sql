-- Replace legacy weekly rows with the rule/event scheduling model.
-- This migration is intentionally destructive for demo schedule data.

DELETE FROM `attendances`;
DELETE FROM `schedule_events`;
DELETE FROM `schedule_recurring_rules`;
DELETE FROM `schedules`;

ALTER TABLE `schedules` AUTO_INCREMENT = 1;
ALTER TABLE `schedule_recurring_rules` AUTO_INCREMENT = 1001;
ALTER TABLE `schedule_events` AUTO_INCREMENT = 1001;

UPDATE `courses`
SET
  `name` = 'TOEIC 650+',
  `description` = 'Demo course for the new rule-based schedule flow.',
  `default_duration` = 90,
  `default_session_count` = 24,
  `default_teacher_user_id` = 8,
  `default_room_id` = 1,
  `updated_at` = NOW(6)
WHERE `id` = 1;

UPDATE `courses`
SET
  `name` = 'IELTS Speaking Plus',
  `description` = 'Demo IELTS course with recurring class sessions and one-off exam events.',
  `default_duration` = 150,
  `default_session_count` = 18,
  `default_teacher_user_id` = 10,
  `default_room_id` = 2,
  `updated_at` = NOW(6)
WHERE `id` = 2;

UPDATE `courses`
SET
  `name` = 'VSTEP B2 Weekend',
  `default_duration` = 150,
  `default_session_count` = 16,
  `default_teacher_user_id` = 9,
  `default_room_id` = 3,
  `updated_at` = NOW(6)
WHERE `id` = 3;

UPDATE `courses`
SET
  `name` = 'English Communication Online',
  `default_duration` = 90,
  `default_session_count` = 20,
  `default_teacher_user_id` = 9,
  `default_room_id` = 4,
  `updated_at` = NOW(6)
WHERE `id` = 4;

UPDATE `classes`
SET
  `name` = 'TOEIC 650+ T8-2026',
  `description` = 'Mon/Wed/Fri evening recurring class generated into lesson events.',
  `start_date` = '2026-07-27',
  `end_date` = '2026-09-18',
  `teacher_user_id` = 8,
  `max_students` = 20,
  `status` = 'ACTIVE'
WHERE `id` = 1;

UPDATE `classes`
SET
  `name` = 'IELTS Speaking T8-2026',
  `description` = 'Tue/Thu/Sat evening class with a one-off mock test.',
  `start_date` = '2026-07-28',
  `end_date` = '2026-09-19',
  `teacher_user_id` = 10,
  `max_students` = 15,
  `status` = 'ACTIVE'
WHERE `id` = 2;

UPDATE `classes`
SET
  `name` = 'VSTEP B2 Weekend T8',
  `description` = 'Weekend morning class for testing student weekly calendar.',
  `start_date` = '2026-08-01',
  `end_date` = '2026-09-20',
  `teacher_user_id` = 9,
  `max_students` = 25,
  `status` = 'ACTIVE'
WHERE `id` = 3;

UPDATE `classes`
SET
  `name` = 'Communication Online T8',
  `description` = 'Online Mon/Wed evening class generated from a recurring rule.',
  `start_date` = '2026-07-27',
  `end_date` = '2026-09-16',
  `teacher_user_id` = 9,
  `max_students` = 18,
  `status` = 'ACTIVE'
WHERE `id` = 4;

UPDATE `classes`
SET
  `name` = 'TD TOEIC 650+ T8',
  `description` = 'Thu Duc branch demo class for center scoped schedule events.',
  `start_date` = '2026-07-28',
  `end_date` = '2026-09-17',
  `teacher_user_id` = 11,
  `status` = 'ACTIVE'
WHERE `id` = 6;

INSERT INTO `schedule_recurring_rules`
  (`id`, `center_id`, `class_id`, `room_id`, `teacher_user_id`, `repeat_type`, `days_of_week`, `start_date`, `end_date`, `start_time`, `end_time`, `type`, `is_active`, `created_at`, `updated_at`)
VALUES
  (1001, 1, 1, 1, 8,  'WEEKLY', '1,3,5', '2026-07-27', '2026-09-18', '19:45:00', '21:15:00', 'THEORY_CLASS', b'1', NOW(6), NOW(6)),
  (1002, 1, 2, 2, 10, 'WEEKLY', '2,4,6', '2026-07-28', '2026-09-19', '18:00:00', '20:30:00', 'THEORY_CLASS', b'1', NOW(6), NOW(6)),
  (1003, 1, 3, 3, 9,  'WEEKLY', '6,7',   '2026-08-01', '2026-09-20', '08:00:00', '10:30:00', 'THEORY_CLASS', b'1', NOW(6), NOW(6)),
  (1004, 1, 4, 4, 9,  'WEEKLY', '1,3',   '2026-07-27', '2026-09-16', '20:00:00', '21:30:00', 'ONLINE_CLASS', b'1', NOW(6), NOW(6)),
  (1005, 2, 6, 5, 11, 'WEEKLY', '2,4',   '2026-07-28', '2026-09-17', '18:30:00', '20:30:00', 'THEORY_CLASS', b'1', NOW(6), NOW(6));

INSERT INTO `schedule_events`
  (`id`, `center_id`, `class_id`, `recurring_rule_id`, `room_id`, `teacher_user_id`, `event_date`, `start_time`, `end_time`, `lesson_number`, `event_type`, `status`, `title`, `note`, `created_at`, `updated_at`)
VALUES
  (1001, 1, 1, 1001, 1, 8,  '2026-07-27', '19:45:00', '21:15:00', 1,  'LESSON',        'SCHEDULED', 'TOEIC 650+ T8-2026', NULL, NOW(6), NOW(6)),
  (1002, 1, 1, 1001, 1, 8,  '2026-07-29', '19:45:00', '21:15:00', 2,  'LESSON',        'SCHEDULED', 'TOEIC 650+ T8-2026', NULL, NOW(6), NOW(6)),
  (1003, 1, 1, 1001, 1, 8,  '2026-07-31', '19:45:00', '21:15:00', 3,  'LESSON',        'SCHEDULED', 'TOEIC 650+ T8-2026', NULL, NOW(6), NOW(6)),
  (1004, 1, 1, 1001, 1, 8,  '2026-08-03', '19:45:00', '21:15:00', 4,  'LESSON',        'SCHEDULED', 'TOEIC 650+ T8-2026', NULL, NOW(6), NOW(6)),
  (1005, 1, 1, 1001, 1, 8,  '2026-08-05', '19:45:00', '21:15:00', 5,  'LESSON',        'SCHEDULED', 'TOEIC 650+ T8-2026', NULL, NOW(6), NOW(6)),
  (1006, 1, 1, 1001, 1, 8,  '2026-08-07', '19:45:00', '21:15:00', 6,  'LESSON',        'SCHEDULED', 'TOEIC 650+ T8-2026', NULL, NOW(6), NOW(6)),
  (1007, 1, 1, 1001, 1, 8,  '2026-08-10', '19:45:00', '21:15:00', 7,  'LESSON',        'SCHEDULED', 'TOEIC 650+ T8-2026', NULL, NOW(6), NOW(6)),
  (1008, 1, 1, 1001, 1, 8,  '2026-08-12', '19:45:00', '21:15:00', 8,  'LESSON',        'SCHEDULED', 'TOEIC 650+ T8-2026', NULL, NOW(6), NOW(6)),
  (1009, 1, 1, 1001, 1, 8,  '2026-08-14', '19:45:00', '21:15:00', 9,  'LESSON',        'CANCELLED', 'TOEIC 650+ T8-2026', 'Cancelled because the room is under maintenance.', NOW(6), NOW(6)),
  (1010, 1, 1, 1001, 1, 8,  '2026-08-17', '19:45:00', '21:15:00', 10, 'LESSON',        'SCHEDULED', 'TOEIC 650+ T8-2026', NULL, NOW(6), NOW(6)),
  (1011, 1, 1, 1001, 1, 8,  '2026-08-19', '19:45:00', '21:15:00', 11, 'LESSON',        'SCHEDULED', 'TOEIC 650+ T8-2026', NULL, NOW(6), NOW(6)),
  (1012, 1, 1, 1001, 1, 8,  '2026-08-21', '19:45:00', '21:15:00', 12, 'LESSON',        'SCHEDULED', 'TOEIC 650+ T8-2026', NULL, NOW(6), NOW(6)),
  (1013, 1, 1, NULL, 3, 8,  '2026-08-02', '09:00:00', '10:30:00', NULL, 'PRACTICE',    'SCHEDULED', 'TOEIC Speaking Lab', 'One-off practice event, not generated from a rule.', NOW(6), NOW(6)),
  (1014, 1, 1, NULL, 3, 8,  '2026-08-22', '08:00:00', '10:00:00', NULL, 'EXAM',        'SCHEDULED', 'TOEIC Mock Test 01', 'One-off exam event.', NOW(6), NOW(6)),

  (1021, 1, 2, 1002, 2, 10, '2026-07-28', '18:00:00', '20:30:00', 1,  'LESSON',        'SCHEDULED', 'IELTS Speaking T8-2026', NULL, NOW(6), NOW(6)),
  (1022, 1, 2, 1002, 2, 10, '2026-07-30', '18:00:00', '20:30:00', 2,  'LESSON',        'SCHEDULED', 'IELTS Speaking T8-2026', NULL, NOW(6), NOW(6)),
  (1023, 1, 2, 1002, 2, 10, '2026-08-01', '18:00:00', '20:30:00', 3,  'LESSON',        'SCHEDULED', 'IELTS Speaking T8-2026', NULL, NOW(6), NOW(6)),
  (1024, 1, 2, 1002, 2, 10, '2026-08-04', '18:00:00', '20:30:00', 4,  'LESSON',        'SCHEDULED', 'IELTS Speaking T8-2026', NULL, NOW(6), NOW(6)),
  (1025, 1, 2, 1002, 2, 10, '2026-08-06', '18:00:00', '20:30:00', 5,  'LESSON',        'SCHEDULED', 'IELTS Speaking T8-2026', NULL, NOW(6), NOW(6)),
  (1026, 1, 2, 1002, 2, 10, '2026-08-08', '18:00:00', '20:30:00', 6,  'LESSON',        'SCHEDULED', 'IELTS Speaking T8-2026', NULL, NOW(6), NOW(6)),
  (1027, 1, 2, NULL, 2, 10, '2026-08-15', '08:30:00', '10:30:00', NULL, 'EXAM',        'SCHEDULED', 'IELTS Speaking Mock Test', NULL, NOW(6), NOW(6)),

  (1031, 1, 3, 1003, 3, 9,  '2026-08-01', '08:00:00', '10:30:00', 1,  'LESSON',        'SCHEDULED', 'VSTEP B2 Weekend T8', NULL, NOW(6), NOW(6)),
  (1032, 1, 3, 1003, 3, 9,  '2026-08-02', '08:00:00', '10:30:00', 2,  'LESSON',        'SCHEDULED', 'VSTEP B2 Weekend T8', NULL, NOW(6), NOW(6)),
  (1033, 1, 3, 1003, 3, 9,  '2026-08-08', '08:00:00', '10:30:00', 3,  'LESSON',        'SCHEDULED', 'VSTEP B2 Weekend T8', NULL, NOW(6), NOW(6)),
  (1034, 1, 3, 1003, 3, 9,  '2026-08-09', '08:00:00', '10:30:00', 4,  'LESSON',        'SCHEDULED', 'VSTEP B2 Weekend T8', NULL, NOW(6), NOW(6)),

  (1041, 1, 4, 1004, 4, 9,  '2026-07-27', '20:00:00', '21:30:00', 1,  'ONLINE_LESSON', 'SCHEDULED', 'Communication Online T8', NULL, NOW(6), NOW(6)),
  (1042, 1, 4, 1004, 4, 9,  '2026-07-29', '20:00:00', '21:30:00', 2,  'ONLINE_LESSON', 'SCHEDULED', 'Communication Online T8', NULL, NOW(6), NOW(6)),
  (1043, 1, 4, 1004, 4, 9,  '2026-08-03', '20:00:00', '21:30:00', 3,  'ONLINE_LESSON', 'SCHEDULED', 'Communication Online T8', NULL, NOW(6), NOW(6)),
  (1044, 1, 4, 1004, 4, 9,  '2026-08-05', '20:00:00', '21:30:00', 4,  'ONLINE_LESSON', 'SCHEDULED', 'Communication Online T8', NULL, NOW(6), NOW(6)),

  (1051, 2, 6, 1005, 5, 11, '2026-07-28', '18:30:00', '20:30:00', 1,  'LESSON',        'SCHEDULED', 'TD TOEIC 650+ T8', NULL, NOW(6), NOW(6)),
  (1052, 2, 6, 1005, 5, 11, '2026-07-30', '18:30:00', '20:30:00', 2,  'LESSON',        'SCHEDULED', 'TD TOEIC 650+ T8', NULL, NOW(6), NOW(6)),
  (1053, 2, 6, 1005, 5, 11, '2026-08-04', '18:30:00', '20:30:00', 3,  'LESSON',        'SCHEDULED', 'TD TOEIC 650+ T8', NULL, NOW(6), NOW(6)),
  (1054, 2, 6, 1005, 5, 11, '2026-08-06', '18:30:00', '20:30:00', 4,  'LESSON',        'SCHEDULED', 'TD TOEIC 650+ T8', NULL, NOW(6), NOW(6));
