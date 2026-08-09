ALTER TABLE `teacher_attendances`
  ADD COLUMN `schedule_event_id` bigint DEFAULT NULL AFTER `teacher_user_id`,
  ADD COLUMN `schedule_id` bigint DEFAULT NULL AFTER `schedule_event_id`,
  ADD KEY `idx_teacher_attendances_schedule_event_id` (`schedule_event_id`),
  ADD KEY `idx_teacher_attendances_schedule_id` (`schedule_id`),
  ADD CONSTRAINT `fk_teacher_attendances_schedule_event_id` FOREIGN KEY (`schedule_event_id`) REFERENCES `schedule_events` (`id`),
  ADD CONSTRAINT `fk_teacher_attendances_schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `schedules` (`id`);
