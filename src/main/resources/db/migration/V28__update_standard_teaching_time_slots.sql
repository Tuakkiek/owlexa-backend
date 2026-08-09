-- Update teaching_time_slots and existing schedule rules/events to standard 6 slots per center.

-- Clear foreign keys temporarily on rules/schedules so we can re-assign time_slot_id cleanly
UPDATE `schedule_recurring_rules` SET `time_slot_id` = NULL;
UPDATE `schedules` SET `time_slot_id` = NULL;

-- Delete old time slots
DELETE FROM `teaching_time_slots`;

-- Seed standard 6 time slots for Center 1
INSERT INTO `teaching_time_slots`
  (`id`, `center_id`, `name`, `period`, `start_time`, `end_time`, `display_order`, `is_active`, `created_at`, `updated_at`)
VALUES
  (101, 1, 'Ca sáng 1',  'MORNING',   '07:00:00', '08:30:00', 1, b'1', NOW(6), NOW(6)),
  (102, 1, 'Ca sáng 2',  'MORNING',   '08:35:00', '10:05:00', 2, b'1', NOW(6), NOW(6)),
  (103, 1, 'Ca chiều 1', 'AFTERNOON', '13:00:00', '14:30:00', 3, b'1', NOW(6), NOW(6)),
  (104, 1, 'Ca chiều 2', 'AFTERNOON', '14:35:00', '16:05:00', 4, b'1', NOW(6), NOW(6)),
  (105, 1, 'Ca tối 1',   'EVENING',   '18:15:00', '19:45:00', 5, b'1', NOW(6), NOW(6)),
  (106, 1, 'Ca tối 2',   'EVENING',   '19:50:00', '21:20:00', 6, b'1', NOW(6), NOW(6));

-- Seed standard 6 time slots for Center 2
INSERT INTO `teaching_time_slots`
  (`id`, `center_id`, `name`, `period`, `start_time`, `end_time`, `display_order`, `is_active`, `created_at`, `updated_at`)
VALUES
  (201, 2, 'Ca sáng 1',  'MORNING',   '07:00:00', '08:30:00', 1, b'1', NOW(6), NOW(6)),
  (202, 2, 'Ca sáng 2',  'MORNING',   '08:35:00', '10:05:00', 2, b'1', NOW(6), NOW(6)),
  (203, 2, 'Ca chiều 1', 'AFTERNOON', '13:00:00', '14:30:00', 3, b'1', NOW(6), NOW(6)),
  (204, 2, 'Ca chiều 2', 'AFTERNOON', '14:35:00', '16:05:00', 4, b'1', NOW(6), NOW(6)),
  (205, 2, 'Ca tối 1',   'EVENING',   '18:15:00', '19:45:00', 5, b'1', NOW(6), NOW(6)),
  (206, 2, 'Ca tối 2',   'EVENING',   '19:50:00', '21:20:00', 6, b'1', NOW(6), NOW(6));

-- Map existing recurring rules in demo database to the standard 6 slots
UPDATE `schedule_recurring_rules` SET `time_slot_id` = 105, `start_time` = '18:15:00', `end_time` = '19:45:00' WHERE `id` = 1001;
UPDATE `schedule_recurring_rules` SET `time_slot_id` = 105, `start_time` = '18:15:00', `end_time` = '19:45:00' WHERE `id` = 1002;
UPDATE `schedule_recurring_rules` SET `time_slot_id` = 102, `start_time` = '08:35:00', `end_time` = '10:05:00' WHERE `id` = 1003;
UPDATE `schedule_recurring_rules` SET `time_slot_id` = 106, `start_time` = '19:50:00', `end_time` = '21:20:00' WHERE `id` = 1004;
UPDATE `schedule_recurring_rules` SET `time_slot_id` = 205, `start_time` = '18:15:00', `end_time` = '19:45:00' WHERE `id` = 1005;
UPDATE `schedule_recurring_rules` SET `time_slot_id` = 105, `start_time` = '18:15:00', `end_time` = '19:45:00' WHERE `id` = 1006;

-- Update remaining rules if any by matching exact start_time or fallback
UPDATE `schedule_recurring_rules` r
JOIN `teaching_time_slots` t ON r.center_id = t.center_id AND r.start_time = t.start_time AND r.end_time = t.end_time
SET r.time_slot_id = t.id
WHERE r.time_slot_id IS NULL;

-- Update schedule_events to match the new slot times of their recurring rules
UPDATE `schedule_events` e
JOIN `schedule_recurring_rules` r ON e.recurring_rule_id = r.id
SET e.start_time = r.start_time, e.end_time = r.end_time;
