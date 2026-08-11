-- Fresh baseline for an empty MySQL 8 database.
-- Tables are ordered so every referenced table already exists.

CREATE TABLE `grading_criteria` (
  `center_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` bigint NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` bigint NOT NULL,
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_grading_criteria_center_id` (`center_id`),
  KEY `idx_grading_criteria_created_by` (`created_by`),
  KEY `idx_grading_criteria_updated_by` (`updated_by`),
  CONSTRAINT `fk_grading_criteria_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_grading_criteria_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_grading_criteria_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `question_collections` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `center_id` bigint NOT NULL,
  `code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` varchar(1000) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_by` bigint NOT NULL,
  `updated_by` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `active_name` varchar(255) COLLATE utf8mb4_unicode_ci
    GENERATED ALWAYS AS (
      CASE WHEN `deleted_at` IS NULL THEN `name` ELSE NULL END
    ) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_collections_id_center` (`id`, `center_id`),
  UNIQUE KEY `uk_question_collections_center_code` (`center_id`, `code`),
  UNIQUE KEY `uk_question_collections_center_active_name` (`center_id`, `active_name`),
  KEY `idx_question_collections_center_deleted_name` (`center_id`, `deleted_at`, `name`),
  KEY `idx_question_collections_created_by` (`created_by`),
  KEY `idx_question_collections_updated_by` (`updated_by`),
  CONSTRAINT `fk_question_collections_center` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`),
  CONSTRAINT `fk_question_collections_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_question_collections_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `questions` (
  `points` decimal(6,2) DEFAULT NULL,
  `center_id` bigint NOT NULL,
  `collection_id` bigint NOT NULL,
  `display_order` int NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` bigint NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `grading_criteria_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` bigint NOT NULL,
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `explanation` longtext COLLATE utf8mb4_unicode_ci,
  `sample_answer` longtext COLLATE utf8mb4_unicode_ci,
  `section_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `difficulty` enum('EASY','HARD','MEDIUM') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` enum('ESSAY','MULTIPLE_CHOICE') COLLATE utf8mb4_unicode_ci NOT NULL,
  `active_display_order` int
    GENERATED ALWAYS AS (
      CASE WHEN `deleted_at` IS NULL THEN `display_order` ELSE NULL END
    ) STORED,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_questions_collection_active_display_order` (`collection_id`, `active_display_order`),
  KEY `idx_questions_center_id` (`center_id`),
  KEY `idx_questions_collection_center` (`collection_id`, `center_id`),
  KEY `idx_questions_collection_active_order` (`collection_id`, `deleted_at`, `display_order`),
  KEY `idx_questions_collection_section_active_order` (`collection_id`, `section_code`, `deleted_at`, `display_order`),
  KEY `idx_questions_center_active_created` (`center_id`, `deleted_at`, `created_at`),
  KEY `idx_questions_center_active_updated` (`center_id`, `deleted_at`, `updated_at`),
  KEY `idx_questions_created_by` (`created_by`),
  KEY `idx_questions_grading_criteria_id` (`grading_criteria_id`),
  KEY `idx_questions_updated_by` (`updated_by`),
  CONSTRAINT `chk_questions_display_order` CHECK (`display_order` >= 1),
  CONSTRAINT `fk_questions_collection_center`
    FOREIGN KEY (`collection_id`, `center_id`)
    REFERENCES `question_collections` (`id`, `center_id`),
  CONSTRAINT `fk_questions_grading_criteria_id` FOREIGN KEY (`grading_criteria_id`) REFERENCES `grading_criteria` (`id`),
  CONSTRAINT `fk_questions_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_questions_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`),
  CONSTRAINT `fk_questions_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `question_options` (
  `display_order` int NOT NULL,
  `is_correct` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_question_options_question_id` (`question_id`),
  CONSTRAINT `chk_question_options_display_order` CHECK (`display_order` >= 1),
  CONSTRAINT `fk_question_options_question_id` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `assessments` (
  `center_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` bigint NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` bigint NOT NULL,
  `description` longtext COLLATE utf8mb4_unicode_ci,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('ARCHIVED','DRAFT','PUBLISHED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_assessments_center_id` (`center_id`),
  KEY `idx_assessments_created_by` (`created_by`),
  KEY `idx_assessments_updated_by` (`updated_by`),
  CONSTRAINT `fk_assessments_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`),
  CONSTRAINT `fk_assessments_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_assessments_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `assessment_items` (
  `display_order` int NOT NULL,
  `points` decimal(6,2) DEFAULT NULL,
  `assessment_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `grading_criteria_id` bigint DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `explanation` longtext COLLATE utf8mb4_unicode_ci,
  `grading_criteria_content` longtext COLLATE utf8mb4_unicode_ci,
  `grading_criteria_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sample_answer` longtext COLLATE utf8mb4_unicode_ci,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `difficulty` enum('EASY','HARD','MEDIUM') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `question_type` enum('ESSAY','MULTIPLE_CHOICE') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_assessment_items_assessment_id` (`assessment_id`),
  KEY `idx_assessment_items_grading_criteria_id` (`grading_criteria_id`),
  KEY `idx_assessment_items_question_id` (`question_id`),
  CONSTRAINT `fk_assessment_items_question_id` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`),
  CONSTRAINT `fk_assessment_items_grading_criteria_id` FOREIGN KEY (`grading_criteria_id`) REFERENCES `grading_criteria` (`id`),
  CONSTRAINT `fk_assessment_items_assessment_id` FOREIGN KEY (`assessment_id`) REFERENCES `assessments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `assessment_item_options` (
  `display_order` int NOT NULL,
  `is_correct` bit(1) NOT NULL,
  `assessment_item_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_assessment_item_options_assessment_item_id` (`assessment_item_id`),
  CONSTRAINT `fk_assessment_item_options_assessment_item_id` FOREIGN KEY (`assessment_item_id`) REFERENCES `assessment_items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `assignments` (
  `attempt_limit` int DEFAULT NULL,
  `assessment_id` bigint NOT NULL,
  `assessment_snapshot_at` datetime(6) DEFAULT NULL,
  `center_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `created_by` bigint NOT NULL,
  `deleted_at` datetime(6) DEFAULT NULL,
  `due_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `open_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `updated_by` bigint NOT NULL,
  `description` longtext COLLATE utf8mb4_unicode_ci,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('ACTIVE','ARCHIVED','CLOSED','DRAFT','SCHEDULED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_assignments_assessment_id` (`assessment_id`),
  KEY `idx_assignments_center_id` (`center_id`),
  KEY `idx_assignments_created_by` (`created_by`),
  KEY `idx_assignments_updated_by` (`updated_by`),
  CONSTRAINT `fk_assignments_center_id` FOREIGN KEY (`center_id`) REFERENCES `centers` (`id`),
  CONSTRAINT `fk_assignments_assessment_id` FOREIGN KEY (`assessment_id`) REFERENCES `assessments` (`id`),
  CONSTRAINT `fk_assignments_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `fk_assignments_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `assignment_targets` (
  `assignment_id` bigint NOT NULL,
  `class_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_user_id` bigint DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `target_type` enum('CLASS','STUDENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_assignment_targets_assignment_id` (`assignment_id`),
  KEY `idx_assignment_targets_class_id` (`class_id`),
  KEY `idx_assignment_targets_student_user_id` (`student_user_id`),
  CONSTRAINT `fk_assignment_targets_assignment_id` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`),
  CONSTRAINT `fk_assignment_targets_class_id` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
  CONSTRAINT `fk_assignment_targets_student_user_id` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `assignment_recipients` (
  `assigned_at` datetime(6) NOT NULL,
  `assignment_id` bigint NOT NULL,
  `class_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `student_user_id` bigint NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `source_type` enum('CLASS','STUDENT') COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('ASSIGNED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_assignment_recipients_assignment_id` (`assignment_id`),
  KEY `idx_assignment_recipients_class_id` (`class_id`),
  KEY `idx_assignment_recipients_student_user_id` (`student_user_id`),
  CONSTRAINT `fk_assignment_recipients_class_id` FOREIGN KEY (`class_id`) REFERENCES `classes` (`id`),
  CONSTRAINT `fk_assignment_recipients_assignment_id` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`),
  CONSTRAINT `fk_assignment_recipients_student_user_id` FOREIGN KEY (`student_user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `assignment_items` (
  `display_order` int NOT NULL,
  `points` decimal(6,2) DEFAULT NULL,
  `assessment_item_id` bigint DEFAULT NULL,
  `assignment_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  `explanation` longtext COLLATE utf8mb4_unicode_ci,
  `grading_criteria_content` longtext COLLATE utf8mb4_unicode_ci,
  `grading_criteria_name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sample_answer` longtext COLLATE utf8mb4_unicode_ci,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `difficulty` enum('EASY','HARD','MEDIUM') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `question_type` enum('ESSAY','MULTIPLE_CHOICE') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_assignment_items_assessment_item_id` (`assessment_item_id`),
  KEY `idx_assignment_items_assignment_id` (`assignment_id`),
  CONSTRAINT `fk_assignment_items_assessment_item_id` FOREIGN KEY (`assessment_item_id`) REFERENCES `assessment_items` (`id`),
  CONSTRAINT `fk_assignment_items_assignment_id` FOREIGN KEY (`assignment_id`) REFERENCES `assignments` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `assignment_item_options` (
  `display_order` int NOT NULL,
  `is_correct` bit(1) NOT NULL,
  `assignment_item_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `content` longtext COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_assignment_item_options_assignment_item_id` (`assignment_item_id`),
  CONSTRAINT `fk_assignment_item_options_assignment_item_id` FOREIGN KEY (`assignment_item_id`) REFERENCES `assignment_items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `submission_attempts` (
  `attempt_number` int NOT NULL,
  `auto_score` decimal(8,2) DEFAULT NULL,
  `max_score` decimal(8,2) DEFAULT NULL,
  `active_attempt_key` bigint DEFAULT NULL,
  `assignment_recipient_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `last_saved_at` datetime(6) DEFAULT NULL,
  `started_at` datetime(6) NOT NULL,
  `submitted_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `assignment_title_snapshot` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('AUTO_SUBMITTED','IN_PROGRESS','SUBMITTED') COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_submission_attempts_assignment_recipient_id` (`assignment_recipient_id`),
  CONSTRAINT `fk_submission_attempts_assignment_recipient_id` FOREIGN KEY (`assignment_recipient_id`) REFERENCES `assignment_recipients` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `submission_answers` (
  `auto_score` decimal(8,2) DEFAULT NULL,
  `max_score` decimal(8,2) DEFAULT NULL,
  `assignment_item_id` bigint NOT NULL,
  `attempt_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `graded_at` datetime(6) DEFAULT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `updated_at` datetime(6) NOT NULL,
  `answer_text` longtext COLLATE utf8mb4_unicode_ci,
  PRIMARY KEY (`id`),
  KEY `idx_submission_answers_assignment_item_id` (`assignment_item_id`),
  KEY `idx_submission_answers_attempt_id` (`attempt_id`),
  CONSTRAINT `fk_submission_answers_attempt_id` FOREIGN KEY (`attempt_id`) REFERENCES `submission_attempts` (`id`),
  CONSTRAINT `fk_submission_answers_assignment_item_id` FOREIGN KEY (`assignment_item_id`) REFERENCES `assignment_items` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `submission_answer_options` (
  `assignment_item_option_id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `submission_answer_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_submission_answer_options_assignment_item_option_id` (`assignment_item_option_id`),
  KEY `idx_submission_answer_options_submission_answer_id` (`submission_answer_id`),
  CONSTRAINT `fk_submission_answer_options_assignment_item_option_id` FOREIGN KEY (`assignment_item_option_id`) REFERENCES `assignment_item_options` (`id`),
  CONSTRAINT `fk_submission_answer_options_submission_answer_id` FOREIGN KEY (`submission_answer_id`) REFERENCES `submission_answers` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
