ALTER TABLE `attendances`
  ADD COLUMN `schedule_event_id` bigint DEFAULT NULL AFTER `schedule_id`,
  MODIFY COLUMN `schedule_id` bigint DEFAULT NULL,
  ADD KEY `idx_attendances_schedule_event_id` (`schedule_event_id`),
  ADD CONSTRAINT `fk_attendances_schedule_event_id` FOREIGN KEY (`schedule_event_id`) REFERENCES `schedule_events` (`id`);
