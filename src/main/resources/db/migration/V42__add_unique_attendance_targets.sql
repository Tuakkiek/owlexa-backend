ALTER TABLE `attendances`
  ADD UNIQUE KEY `uk_attendances_event_student_date`
    (`center_id`, `schedule_event_id`, `student_user_id`, `date`),
  ADD UNIQUE KEY `uk_attendances_schedule_student_date`
    (`center_id`, `schedule_id`, `student_user_id`, `date`);
