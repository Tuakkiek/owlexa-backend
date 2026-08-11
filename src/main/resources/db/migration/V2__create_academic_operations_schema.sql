-- Fresh baseline for an empty MySQL 8 database.
-- Tables are ordered so every referenced table already exists.

CREATE TABLE `courses` (
  `default_duration` int DEFAULT NULL,
  `default_max_students` int DEFAULT NULL,
  `default_monthly_fee` double DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_courses_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `rooms` (
  `capacity` int DEFAULT NULL,
  `is_active` bit(1) NOT NULL,
  `center_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `code` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_rooms_center_id` (`center_id`),
  CONSTRAINT `fk_rooms_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `classes` (
  `max_students` int DEFAULT NULL,
  `monthly_fee` double DEFAULT NULL,
  `center_id` bigint NOT NULL,
  `course_id` bigint DEFAULT NULL,
  `create_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` text COLLATE utf8mb4_unicode_ci,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('PLANNED','ACTIVE','FINISHED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_classes_center_id` (`center_id`),
  KEY `idx_classes_course_id` (`course_id`),
  CONSTRAINT `fk_classes_course_id` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`),
  CONSTRAINT `fk_classes_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `schedules` (
  `end_time` time NOT NULL,
  `start_time` time NOT NULL,
  `center_id` bigint NOT NULL,
  `class_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `room_id` bigint DEFAULT NULL,
  `teacher_user_id` bigint DEFAULT NULL,
  `day_of_week` enum('FRIDAY','MONDAY','SATURDAY','SUNDAY','THURSDAY','TUESDAY','WEDNESDAY') COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` enum('CANCELLED','EXAM','ONLINE_CLASS','THEORY_CLASS') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_schedules_center_id` (`center_id`),
  KEY `idx_schedules_class_id` (`class_id`),
  KEY `idx_schedules_room_id` (`room_id`),
  KEY `idx_schedules_teacher_user_id` (`teacher_user_id`),
  CONSTRAINT `fk_schedules_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`),
  CONSTRAINT `fk_schedules_room_id` FOREIGN KEY (`room_id`) REFERENCES `rooms` (`id`),
  CONSTRAINT `fk_schedules_teacher_user_id` FOREIGN KEY (`teacher_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_schedules_class_id` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `class_enrollments` (
  `center_id` bigint NOT NULL,
  `class_id` bigint NOT NULL,
  `enrolled_at` datetime(6) NOT NULL,
  `enrolled_by_user_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_user_id` bigint NOT NULL,
  `status` enum('ACTIVE','DROPPED','PENDING','SUSPENDED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_class_enrollments_center_id` (`center_id`),
  KEY `idx_class_enrollments_class_id` (`class_id`),
  KEY `idx_class_enrollments_enrolled_by_user_id` (`enrolled_by_user_id`),
  KEY `idx_class_enrollments_student_user_id` (`student_user_id`),
  CONSTRAINT `fk_class_enrollments_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`),
  CONSTRAINT `fk_class_enrollments_class_id` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
  CONSTRAINT `fk_class_enrollments_student_user_id` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_class_enrollments_enrolled_by_user_id` FOREIGN KEY (`enrolled_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `attendances` (
  `date` date NOT NULL,
  `center_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `marked_by_user_id` bigint DEFAULT NULL,
  `schedule_id` bigint NOT NULL,
  `student_user_id` bigint NOT NULL,
  `note` text COLLATE utf8mb4_unicode_ci,
  `status` enum('ABSENT','EXCUSED','LATE','PRESENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_attendances_center_id` (`center_id`),
  KEY `idx_attendances_marked_by_user_id` (`marked_by_user_id`),
  KEY `idx_attendances_schedule_id` (`schedule_id`),
  KEY `idx_attendances_student_user_id` (`student_user_id`),
  CONSTRAINT `fk_attendances_student_user_id` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_attendances_schedule_id` FOREIGN KEY (`schedule_id`) REFERENCES `schedules` (`id`),
  CONSTRAINT `fk_attendances_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`),
  CONSTRAINT `fk_attendances_marked_by_user_id` FOREIGN KEY (`marked_by_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `teacher_attendances` (
  `date` date NOT NULL,
  `center_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `marked_by_user_id` bigint DEFAULT NULL,
  `teacher_user_id` bigint NOT NULL,
  `note` text COLLATE utf8mb4_unicode_ci,
  `status` enum('ABSENT','LATE','LEAVE','PRESENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_teacher_attendances_center_id` (`center_id`),
  KEY `idx_teacher_attendances_marked_by_user_id` (`marked_by_user_id`),
  KEY `idx_teacher_attendances_teacher_user_id` (`teacher_user_id`),
  CONSTRAINT `fk_teacher_attendances_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`),
  CONSTRAINT `fk_teacher_attendances_marked_by_user_id` FOREIGN KEY (`marked_by_user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_teacher_attendances_teacher_user_id` FOREIGN KEY (`teacher_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `teacher_center_profile` (
  `currency` varchar(3) COLLATE utf8mb4_unicode_ci NOT NULL,
  `salary` decimal(12,2) DEFAULT NULL,
  `center_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `teacher_user_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_center_profile_teacher_center` (`teacher_user_id`,`center_id`),
  KEY `idx_teacher_center_profile_center_id` (`center_id`),
  CONSTRAINT `fk_teacher_center_profile_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`),
  CONSTRAINT `fk_teacher_center_profile_teacher_user_id` FOREIGN KEY (`teacher_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `student_documents` (
  `center_id` bigint NOT NULL,
  `clazz_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_user_id` bigint NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci,
  `file_url` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `document_type` enum('OTHER','PDF','VIDEO') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_student_documents_center_id` (`center_id`),
  KEY `idx_student_documents_clazz_id` (`clazz_id`),
  KEY `idx_student_documents_student_user_id` (`student_user_id`),
  CONSTRAINT `fk_student_documents_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`),
  CONSTRAINT `fk_student_documents_clazz_id` FOREIGN KEY (`clazz_id`) REFERENCES `classes` (`id`),
  CONSTRAINT `fk_student_documents_student_user_id` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
