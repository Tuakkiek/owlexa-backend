-- Create teaching_time_slots table and add time_slot_id reference to recurring rules and legacy schedules.

CREATE TABLE `teaching_time_slots` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `center_id` bigint NOT NULL,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `period` enum('MORNING','AFTERNOON','EVENING') COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `display_order` int NOT NULL DEFAULT 0,
  `is_active` bit(1) NOT NULL DEFAULT b'1',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`),
  KEY `idx_teaching_time_slots_center_active` (`center_id`, `is_active`),
  KEY `idx_teaching_time_slots_center_start` (`center_id`, `start_time`),
  CONSTRAINT `fk_teaching_time_slots_center` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `schedule_recurring_rules`
  ADD COLUMN `time_slot_id` bigint DEFAULT NULL AFTER `room_id`,
  ADD KEY `idx_schedule_recurring_rules_time_slot_id` (`time_slot_id`),
  ADD CONSTRAINT `fk_schedule_recurring_rules_time_slot_id` FOREIGN KEY (`time_slot_id`) REFERENCES `teaching_time_slots` (`id`);

ALTER TABLE `schedules`
  ADD COLUMN `time_slot_id` bigint DEFAULT NULL AFTER `room_id`,
  ADD KEY `idx_schedules_time_slot_id` (`time_slot_id`),
  ADD CONSTRAINT `fk_schedules_time_slot_id` FOREIGN KEY (`time_slot_id`) REFERENCES `teaching_time_slots` (`id`);

-- Backfill existing recurring schedule rules into distinct teaching_time_slots
INSERT INTO `teaching_time_slots`
  (`id`, `center_id`, `name`, `period`, `start_time`, `end_time`, `display_order`, `is_active`, `created_at`, `updated_at`)
VALUES
  (101, 1, 'Ca sáng 1', 'MORNING', '08:00:00', '10:30:00', 1, b'1', NOW(6), NOW(6)),
  (102, 1, 'Ca tối 1', 'EVENING', '18:00:00', '20:30:00', 2, b'1', NOW(6), NOW(6)),
  (103, 1, 'Ca tối 2', 'EVENING', '19:45:00', '21:15:00', 3, b'1', NOW(6), NOW(6)),
  (104, 1, 'Ca tối 3', 'EVENING', '20:00:00', '21:30:00', 4, b'1', NOW(6), NOW(6)),
  (201, 2, 'Ca tối 1', 'EVENING', '18:30:00', '20:30:00', 1, b'1', NOW(6), NOW(6));

UPDATE `schedule_recurring_rules` r
JOIN `teaching_time_slots` t ON r.center_id = t.center_id AND r.start_time = t.start_time AND r.end_time = t.end_time
SET r.time_slot_id = t.id;

UPDATE `schedules` s
JOIN `teaching_time_slots` t ON s.center_id = t.center_id AND s.start_time = t.start_time AND s.end_time = t.end_time
SET s.time_slot_id = t.id;
