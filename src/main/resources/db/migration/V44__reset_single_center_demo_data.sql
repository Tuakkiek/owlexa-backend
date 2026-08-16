-- Reset demo data to one focused center with richer operational history.
-- Default password for every seeded user: password123
-- BCrypt hash: $2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS

SET @seed_password = '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS';
SET @demo_now = '2026-08-15';
SET @old_foreign_key_checks = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `admin_audit_logs`;
TRUNCATE TABLE `teacher_review_items`;
TRUNCATE TABLE `teacher_reviews`;
TRUNCATE TABLE `ai_grading_item_results`;
TRUNCATE TABLE `ai_grading_results`;
TRUNCATE TABLE `ai_grading_jobs`;
TRUNCATE TABLE `submission_answer_options`;
TRUNCATE TABLE `submission_answers`;
TRUNCATE TABLE `submission_attempts`;
TRUNCATE TABLE `assignment_item_options`;
TRUNCATE TABLE `assignment_items`;
TRUNCATE TABLE `assignment_recipients`;
TRUNCATE TABLE `assignment_targets`;
TRUNCATE TABLE `assignment_content_blocks`;
TRUNCATE TABLE `assignments`;
TRUNCATE TABLE `assessment_item_options`;
TRUNCATE TABLE `assessment_items`;
TRUNCATE TABLE `assessment_content_blocks`;
TRUNCATE TABLE `assessments`;
TRUNCATE TABLE `question_options`;
TRUNCATE TABLE `questions`;
TRUNCATE TABLE `question_collections`;
TRUNCATE TABLE `grading_criteria`;
TRUNCATE TABLE `file_references`;
TRUNCATE TABLE `files`;
TRUNCATE TABLE `refunds`;
TRUNCATE TABLE `payments`;
TRUNCATE TABLE `installments`;
TRUNCATE TABLE `fee_records`;
TRUNCATE TABLE `audit_logs`;
TRUNCATE TABLE `sepay_webhook_events`;
TRUNCATE TABLE `attendances`;
TRUNCATE TABLE `teacher_attendances`;
TRUNCATE TABLE `student_documents`;
TRUNCATE TABLE `class_enrollments`;
TRUNCATE TABLE `schedule_events`;
TRUNCATE TABLE `schedule_recurring_rules`;
TRUNCATE TABLE `schedules`;
TRUNCATE TABLE `teaching_time_slots`;
TRUNCATE TABLE `teacher_center_profile`;
TRUNCATE TABLE `classes`;
TRUNCATE TABLE `rooms`;
TRUNCATE TABLE `courses`;
TRUNCATE TABLE `membership`;
TRUNCATE TABLE `user_permission`;
TRUNCATE TABLE `user_sessions`;
TRUNCATE TABLE `centers`;
TRUNCATE TABLE `users`;

SET FOREIGN_KEY_CHECKS = @old_foreign_key_checks;

CREATE TEMPORARY TABLE `tmp_seed_numbers` (`n` INT NOT NULL PRIMARY KEY);

INSERT INTO `tmp_seed_numbers` (`n`)
SELECT ones.n + tens.n * 10 + hundreds.n * 100
FROM (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) ones
CROSS JOIN (
    SELECT 0 n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) tens
CROSS JOIN (
    SELECT 0 n UNION ALL SELECT 1
) hundreds
WHERE ones.n + tens.n * 10 + hundreds.n * 100 <= 199;

INSERT INTO `users` (`id`, `email`, `full_name`, `password`, `phone_number`, `role`, `is_active`) VALUES
(1, 'owner.saigon@owlexa.edu.vn', 'Nguyễn Mai Anh', @seed_password, '0901000001', 'OWNER', b'1'),
(2, 'cashier.lan@owlexa.edu.vn', 'Trần Thanh Lan', @seed_password, '0901000002', 'CASHIER', b'1'),
(3, 'cashier.minh@owlexa.edu.vn', 'Phạm Quang Minh', @seed_password, '0901000003', 'CASHIER', b'1'),
(11, 'teacher.anna@owlexa.edu.vn', 'Nguyễn Thảo An', @seed_password, '0901000011', 'TEACHER', b'1'),
(12, 'teacher.brian@owlexa.edu.vn', 'Trần Bình An', @seed_password, '0901000012', 'TEACHER', b'1'),
(13, 'teacher.chloe@owlexa.edu.vn', 'Phạm Khánh Chi', @seed_password, '0901000013', 'TEACHER', b'1'),
(14, 'teacher.david@owlexa.edu.vn', 'Lê Minh Đức', @seed_password, '0901000014', 'TEACHER', b'1'),
(15, 'teacher.emma@owlexa.edu.vn', 'Võ Thanh Hà', @seed_password, '0901000015', 'TEACHER', b'1'),
(16, 'teacher.frank@owlexa.edu.vn', 'Hoàng Quang Huy', @seed_password, '0901000016', 'TEACHER', b'1'),
(17, 'teacher.grace@owlexa.edu.vn', 'Đặng Gia Linh', @seed_password, '0901000017', 'TEACHER', b'1'),
(18, 'teacher.henry@owlexa.edu.vn', 'Bùi Anh Minh', @seed_password, '0901000018', 'TEACHER', b'1'),
(19, 'teacher.iris@owlexa.edu.vn', 'Đỗ Phương Thảo', @seed_password, '0901000019', 'TEACHER', b'1'),
(20, 'teacher.jason@owlexa.edu.vn', 'Phan Quốc Việt', @seed_password, '0901000020', 'TEACHER', b'1');

INSERT INTO `users` (`id`, `email`, `full_name`, `password`, `phone_number`, `role`, `is_active`)
SELECT
    100 + n,
    CONCAT('student', LPAD(n, 2, '0'), '@owlexa.edu.vn'),
    CASE n
        WHEN 1 THEN 'Lê Minh An' WHEN 2 THEN 'Trần Bảo Anh' WHEN 3 THEN 'Phạm Gia Bảo' WHEN 4 THEN 'Nguyễn Ngọc Bích'
        WHEN 5 THEN 'Võ Quang Châu' WHEN 6 THEN 'Đặng Khánh Chi' WHEN 7 THEN 'Bùi Đức Cường' WHEN 8 THEN 'Hoàng Mỹ Duyên'
        WHEN 9 THEN 'Đỗ Thành Đạt' WHEN 10 THEN 'Mai Lam Giang' WHEN 11 THEN 'Cao Minh Hà' WHEN 12 THEN 'Lý Gia Hân'
        WHEN 13 THEN 'Nguyễn Anh Khoa' WHEN 14 THEN 'Trần Hoàng Lâm' WHEN 15 THEN 'Phạm Thùy Linh' WHEN 16 THEN 'Lê Tuệ Minh'
        WHEN 17 THEN 'Võ Quỳnh Như' WHEN 18 THEN 'Đặng Hoài Nam' WHEN 19 THEN 'Bùi Yến Nhi' WHEN 20 THEN 'Hoàng Bảo Ngọc'
        WHEN 21 THEN 'Đỗ Minh Quân' WHEN 22 THEN 'Mai Gia Quỳnh' WHEN 23 THEN 'Cao Đức Sơn' WHEN 24 THEN 'Lý Ngọc Tâm'
        WHEN 25 THEN 'Nguyễn Thanh Thảo' WHEN 26 THEN 'Trần Minh Thư' WHEN 27 THEN 'Phạm Anh Tuấn' WHEN 28 THEN 'Lê Bảo Trân'
        WHEN 29 THEN 'Võ Tường Vy' WHEN 30 THEN 'Đặng Hoàng Việt' WHEN 31 THEN 'Bùi Phương Yến' WHEN 32 THEN 'Hoàng Nhật Anh'
        WHEN 33 THEN 'Đỗ Bảo Châu' WHEN 34 THEN 'Mai Quốc Duy' WHEN 35 THEN 'Cao Gia Huy' WHEN 36 THEN 'Lý Khánh Linh'
        WHEN 37 THEN 'Nguyễn Trà My' WHEN 38 THEN 'Trần Đức Phúc' WHEN 39 THEN 'Phạm Minh Trí' ELSE 'Lê Anh Vũ'
    END,
    @seed_password,
    CONCAT('090100', LPAD(100 + n, 4, '0')),
    'STUDENT',
    b'1'
FROM `tmp_seed_numbers`
WHERE n BETWEEN 1 AND 40;

INSERT INTO `centers` (`id`, `owner_user_id`, `name`, `subdomain`, `created_at`, `is_active`) VALUES
(1, 1, 'Owlexa Saigon Learning Center', 'saigon-demo', '2026-05-20 08:00:00.000000', b'1');

INSERT INTO `membership` (`center_id`, `user_id`, `joined_by_user_id`, `joined_at`)
SELECT 1, u.id, 1, '2026-05-20 09:00:00.000000'
FROM `users` u;

INSERT INTO `teacher_center_profile` (`center_id`, `teacher_user_id`, `salary`, `currency`, `created_at`, `updated_at`)
SELECT 1, u.id, 22000000 + (u.id - 10) * 1250000, 'VND', '2026-05-20 10:00:00.000000', '2026-08-01 10:00:00.000000'
FROM `users` u
WHERE u.role = 'TEACHER';

INSERT INTO `courses` (`id`, `center_id`, `code`, `name`, `description`, `default_duration`, `default_session_count`, `default_teacher_user_id`, `default_monthly_fee`, `is_active`, `created_at`, `updated_at`) VALUES
(101, 1, 'TOEIC-650', 'TOEIC 650 Foundation', 'Listening, grammar, and reading strategy for TOEIC 650 target.', 90, 24, 11, 3200000.00, b'1', '2026-05-21 08:00:00.000000', '2026-05-21 08:00:00.000000'),
(102, 1, 'IELTS-FDN', 'IELTS Foundation 5.5', 'Core IELTS skills with weekly speaking practice.', 90, 24, 12, 3900000.00, b'1', '2026-05-21 08:05:00.000000', '2026-05-21 08:05:00.000000'),
(103, 1, 'KIDS-FLYERS', 'Cambridge Flyers Weekend', 'Young learner program with phonics, vocabulary games, and speaking routines.', 60, 18, 13, 2600000.00, b'1', '2026-05-21 08:10:00.000000', '2026-05-21 08:10:00.000000'),
(104, 1, 'COMM-B1', 'Communication B1', 'Practical speaking, pronunciation, and confidence building.', 72, 20, 14, 3000000.00, b'1', '2026-05-21 08:15:00.000000', '2026-05-21 08:15:00.000000'),
(105, 1, 'IELTS-65', 'IELTS Intensive 6.5', 'Integrated IELTS course with mock tests and writing feedback.', 96, 24, 15, 4700000.00, b'1', '2026-05-21 08:20:00.000000', '2026-05-21 08:20:00.000000'),
(106, 1, 'BUS-ENG', 'Business English Workshop', 'Email, presentation, meeting, and negotiation practice.', 48, 16, 18, 3500000.00, b'1', '2026-05-21 08:25:00.000000', '2026-05-21 08:25:00.000000');

INSERT INTO `rooms` (`id`, `center_id`, `code`, `name`, `capacity`, `description`, `is_active`, `created_at`, `updated_at`) VALUES
(101, 1, 'SG-A101', 'Room A101', 24, 'Main classroom with smart board.', b'1', '2026-05-21 09:00:00.000000', '2026-05-21 09:00:00.000000'),
(102, 1, 'SG-A102', 'Room A102', 20, 'Discussion room for IELTS and communication classes.', b'1', '2026-05-21 09:05:00.000000', '2026-05-21 09:05:00.000000'),
(103, 1, 'SG-KIDS', 'Kids Studio', 18, 'Flexible room for young learners.', b'1', '2026-05-21 09:10:00.000000', '2026-05-21 09:10:00.000000'),
(104, 1, 'SG-LAB', 'Listening Lab', 22, 'Computer lab with headsets.', b'1', '2026-05-21 09:15:00.000000', '2026-05-21 09:15:00.000000'),
(105, 1, 'SG-STAGE', 'Speaking Stage', 30, 'Presentation practice room.', b'1', '2026-05-21 09:20:00.000000', '2026-05-21 09:20:00.000000'),
(106, 1, 'SG-B201', 'Room B201', 20, 'Quiet classroom for workshops.', b'1', '2026-05-21 09:25:00.000000', '2026-05-21 09:25:00.000000');

INSERT INTO `teaching_time_slots` (`id`, `center_id`, `name`, `period`, `start_time`, `end_time`, `display_order`, `is_active`, `created_at`, `updated_at`) VALUES
(101, 1, 'Morning 1', 'MORNING', '08:00:00', '09:30:00', 1, b'1', '2026-05-21 10:00:00.000000', '2026-05-21 10:00:00.000000'),
(102, 1, 'Morning 2', 'MORNING', '09:45:00', '11:15:00', 2, b'1', '2026-05-21 10:00:00.000000', '2026-05-21 10:00:00.000000'),
(103, 1, 'Afternoon 1', 'AFTERNOON', '14:00:00', '15:30:00', 3, b'1', '2026-05-21 10:00:00.000000', '2026-05-21 10:00:00.000000'),
(104, 1, 'Evening 1', 'EVENING', '18:00:00', '19:30:00', 4, b'1', '2026-05-21 10:00:00.000000', '2026-05-21 10:00:00.000000'),
(105, 1, 'Evening 2', 'EVENING', '19:45:00', '21:15:00', 5, b'1', '2026-05-21 10:00:00.000000', '2026-05-21 10:00:00.000000'),
(106, 1, 'Weekend Intensive', 'MORNING', '08:30:00', '11:00:00', 6, b'1', '2026-05-21 10:00:00.000000', '2026-05-21 10:00:00.000000');

INSERT INTO `classes` (`id`, `center_id`, `course_id`, `teacher_user_id`, `name`, `description`, `start_date`, `end_date`, `monthly_fee`, `status`, `create_at`) VALUES
(101, 1, 101, 11, 'TOEIC 650 Morning A', 'Monday and Wednesday morning TOEIC foundation class.', '2026-07-01', '2026-09-23', 3200000.00, 'ACTIVE', '2026-06-01 08:00:00.000000'),
(102, 1, 102, 12, 'IELTS Foundation Evening A', 'Tuesday and Thursday evening IELTS foundation class.', '2026-07-02', '2026-09-24', 3900000.00, 'ACTIVE', '2026-06-01 08:05:00.000000'),
(103, 1, 103, 13, 'Flyers Weekend A', 'Saturday and Sunday morning young learner group.', '2026-06-29', '2026-08-30', 2600000.00, 'ACTIVE', '2026-06-01 08:10:00.000000'),
(104, 1, 104, 14, 'Communication B1 Evening', 'Monday and Friday communication practice.', '2026-07-06', '2026-09-18', 3000000.00, 'ACTIVE', '2026-06-01 08:15:00.000000'),
(105, 1, 105, 15, 'IELTS Intensive 6.5', 'Tuesday and Thursday intensive IELTS class.', '2026-07-07', '2026-10-01', 4700000.00, 'ACTIVE', '2026-06-01 08:20:00.000000'),
(106, 1, 101, 16, 'TOEIC Sprint 850', 'Monday, Wednesday, Friday TOEIC sprint.', '2026-08-03', '2026-09-25', 4200000.00, 'ACTIVE', '2026-07-10 08:00:00.000000'),
(107, 1, 103, 17, 'Teen Grammar Weekend', 'Weekend grammar and writing support.', '2026-07-04', '2026-08-30', 2400000.00, 'ACTIVE', '2026-06-10 08:00:00.000000'),
(108, 1, 106, 18, 'Business English Friday', 'Friday evening workplace English workshop.', '2026-07-10', '2026-09-11', 3500000.00, 'ACTIVE', '2026-06-10 08:05:00.000000'),
(109, 1, 104, 19, 'Pronunciation Lab Saturday', 'Saturday afternoon pronunciation coaching.', '2026-08-01', '2026-09-12', 2800000.00, 'ACTIVE', '2026-07-10 08:10:00.000000'),
(110, 1, 105, 20, 'Academic Writing Summer', 'Completed writing class kept for history reports.', '2026-06-01', '2026-07-31', 4500000.00, 'FINISHED', '2026-05-15 08:00:00.000000');

INSERT INTO `schedules` (`id`, `center_id`, `class_id`, `room_id`, `time_slot_id`, `teacher_user_id`, `day_of_week`, `start_time`, `end_time`, `type`, `created_at`) VALUES
(101, 1, 101, 101, 101, 11, 'MONDAY', '08:00:00', '09:30:00', 'THEORY_CLASS', '2026-06-01 09:00:00.000000'),
(102, 1, 101, 101, 101, 11, 'WEDNESDAY', '08:00:00', '09:30:00', 'THEORY_CLASS', '2026-06-01 09:00:00.000000'),
(103, 1, 102, 102, 105, 12, 'TUESDAY', '19:45:00', '21:15:00', 'THEORY_CLASS', '2026-06-01 09:05:00.000000'),
(104, 1, 102, 102, 105, 12, 'THURSDAY', '19:45:00', '21:15:00', 'THEORY_CLASS', '2026-06-01 09:05:00.000000'),
(105, 1, 103, 103, 106, 13, 'SATURDAY', '08:30:00', '11:00:00', 'THEORY_CLASS', '2026-06-01 09:10:00.000000'),
(106, 1, 103, 103, 106, 13, 'SUNDAY', '08:30:00', '11:00:00', 'THEORY_CLASS', '2026-06-01 09:10:00.000000'),
(107, 1, 104, 105, 104, 14, 'MONDAY', '18:00:00', '19:30:00', 'THEORY_CLASS', '2026-06-01 09:15:00.000000'),
(108, 1, 104, 105, 104, 14, 'FRIDAY', '18:00:00', '19:30:00', 'THEORY_CLASS', '2026-06-01 09:15:00.000000'),
(109, 1, 105, 104, 105, 15, 'TUESDAY', '19:45:00', '21:15:00', 'THEORY_CLASS', '2026-06-01 09:20:00.000000'),
(110, 1, 105, 104, 105, 15, 'THURSDAY', '19:45:00', '21:15:00', 'THEORY_CLASS', '2026-06-01 09:20:00.000000'),
(111, 1, 106, 101, 104, 16, 'MONDAY', '18:00:00', '19:30:00', 'THEORY_CLASS', '2026-07-10 09:00:00.000000'),
(112, 1, 106, 101, 104, 16, 'WEDNESDAY', '18:00:00', '19:30:00', 'THEORY_CLASS', '2026-07-10 09:00:00.000000'),
(113, 1, 106, 101, 104, 16, 'FRIDAY', '18:00:00', '19:30:00', 'THEORY_CLASS', '2026-07-10 09:00:00.000000'),
(114, 1, 107, 106, 106, 17, 'SATURDAY', '08:30:00', '11:00:00', 'THEORY_CLASS', '2026-06-10 09:00:00.000000'),
(115, 1, 107, 106, 106, 17, 'SUNDAY', '08:30:00', '11:00:00', 'THEORY_CLASS', '2026-06-10 09:00:00.000000'),
(116, 1, 108, 102, 104, 18, 'FRIDAY', '18:00:00', '19:30:00', 'THEORY_CLASS', '2026-06-10 09:05:00.000000'),
(117, 1, 109, 105, 103, 19, 'SATURDAY', '14:00:00', '15:30:00', 'THEORY_CLASS', '2026-07-10 09:10:00.000000'),
(118, 1, 110, 104, 105, 20, 'TUESDAY', '19:45:00', '21:15:00', 'THEORY_CLASS', '2026-05-15 09:00:00.000000'),
(119, 1, 110, 104, 105, 20, 'THURSDAY', '19:45:00', '21:15:00', 'THEORY_CLASS', '2026-05-15 09:00:00.000000');

INSERT INTO `schedule_recurring_rules` (`id`, `center_id`, `class_id`, `room_id`, `time_slot_id`, `teacher_user_id`, `repeat_type`, `days_of_week`, `start_date`, `end_date`, `start_time`, `end_time`, `type`, `is_active`, `created_at`, `updated_at`) VALUES
(101, 1, 101, 101, 101, 11, 'WEEKLY', '1,3', '2026-07-01', '2026-09-23', '08:00:00', '09:30:00', 'THEORY_CLASS', b'1', '2026-06-01 09:00:00.000000', '2026-06-01 09:00:00.000000'),
(102, 1, 102, 102, 105, 12, 'WEEKLY', '2,4', '2026-07-02', '2026-09-24', '19:45:00', '21:15:00', 'THEORY_CLASS', b'1', '2026-06-01 09:05:00.000000', '2026-06-01 09:05:00.000000'),
(103, 1, 103, 103, 106, 13, 'WEEKLY', '6,7', '2026-06-29', '2026-08-30', '08:30:00', '11:00:00', 'THEORY_CLASS', b'1', '2026-06-01 09:10:00.000000', '2026-06-01 09:10:00.000000'),
(104, 1, 104, 105, 104, 14, 'WEEKLY', '1,5', '2026-07-06', '2026-09-18', '18:00:00', '19:30:00', 'THEORY_CLASS', b'1', '2026-06-01 09:15:00.000000', '2026-06-01 09:15:00.000000'),
(105, 1, 105, 104, 105, 15, 'WEEKLY', '2,4', '2026-07-07', '2026-10-01', '19:45:00', '21:15:00', 'THEORY_CLASS', b'1', '2026-06-01 09:20:00.000000', '2026-06-01 09:20:00.000000'),
(106, 1, 106, 101, 104, 16, 'WEEKLY', '1,3,5', '2026-08-03', '2026-09-25', '18:00:00', '19:30:00', 'THEORY_CLASS', b'1', '2026-07-10 09:00:00.000000', '2026-07-10 09:00:00.000000'),
(107, 1, 107, 106, 106, 17, 'WEEKLY', '6,7', '2026-07-04', '2026-08-30', '08:30:00', '11:00:00', 'THEORY_CLASS', b'1', '2026-06-10 09:00:00.000000', '2026-06-10 09:00:00.000000'),
(108, 1, 108, 102, 104, 18, 'WEEKLY', '5', '2026-07-10', '2026-09-11', '18:00:00', '19:30:00', 'THEORY_CLASS', b'1', '2026-06-10 09:05:00.000000', '2026-06-10 09:05:00.000000'),
(109, 1, 109, 105, 103, 19, 'WEEKLY', '6', '2026-08-01', '2026-09-12', '14:00:00', '15:30:00', 'THEORY_CLASS', b'1', '2026-07-10 09:10:00.000000', '2026-07-10 09:10:00.000000'),
(110, 1, 110, 104, 105, 20, 'WEEKLY', '2,4', '2026-06-01', '2026-07-31', '19:45:00', '21:15:00', 'THEORY_CLASS', b'1', '2026-05-15 09:00:00.000000', '2026-05-15 09:00:00.000000');

INSERT INTO `schedule_events` (`center_id`, `class_id`, `recurring_rule_id`, `room_id`, `teacher_user_id`, `event_date`, `start_time`, `end_time`, `lesson_number`, `event_type`, `status`, `title`, `note`, `created_at`, `updated_at`)
SELECT g.center_id, g.class_id, g.rule_id, g.room_id, g.teacher_user_id,
       g.event_date, g.start_time, g.end_time, g.lesson_number,
       'LESSON', 'SCHEDULED', c.name, NULL, NOW(6), NOW(6)
FROM (
    SELECT r.id AS rule_id, r.center_id, r.class_id, r.room_id, r.teacher_user_id,
           r.start_time, r.end_time,
           DATE_ADD(r.start_date, INTERVAL n.n DAY) AS event_date,
           ROW_NUMBER() OVER (
               PARTITION BY r.id
               ORDER BY DATE_ADD(r.start_date, INTERVAL n.n DAY)
           ) AS lesson_number
    FROM `schedule_recurring_rules` r
    JOIN `tmp_seed_numbers` n ON n.n <= DATEDIFF(r.end_date, r.start_date)
    WHERE FIND_IN_SET(
              IF(DAYOFWEEK(DATE_ADD(r.start_date, INTERVAL n.n DAY)) = 1,
                 7,
                 DAYOFWEEK(DATE_ADD(r.start_date, INTERVAL n.n DAY)) - 1),
              r.days_of_week
          ) > 0
) g
JOIN `classes` c ON c.id = g.class_id
JOIN `courses` co ON co.id = c.course_id
WHERE g.lesson_number <= co.default_session_count;

INSERT INTO `schedule_events` (`center_id`, `class_id`, `recurring_rule_id`, `room_id`, `teacher_user_id`, `event_date`, `start_time`, `end_time`, `lesson_number`, `event_type`, `status`, `title`, `note`, `created_at`, `updated_at`) VALUES
(1, 101, NULL, 104, 11, '2026-08-29', '08:00:00', '10:00:00', NULL, 'EXAM', 'SCHEDULED', 'TOEIC 650 Mock Test 01', 'Seeded mock test for assessment calendar.', '2026-08-01 09:00:00.000000', '2026-08-01 09:00:00.000000'),
(1, 105, NULL, 104, 15, '2026-09-05', '18:00:00', '20:30:00', NULL, 'EXAM', 'SCHEDULED', 'IELTS 6.5 Full Mock Test', 'Full mock test with listening lab.', '2026-08-01 09:05:00.000000', '2026-08-01 09:05:00.000000');

CREATE TEMPORARY TABLE `tmp_seed_enrollments` (
    `class_id` BIGINT NOT NULL,
    `student_user_id` BIGINT NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `enrolled_at` DATETIME(6) NOT NULL,
    `drop_reason` VARCHAR(30) NULL,
    `dropped_at` DATETIME(6) NULL,
    `dropped_by_user_id` BIGINT NULL
);

INSERT INTO `tmp_seed_enrollments` (`class_id`, `student_user_id`, `enrolled_at`)
SELECT 101, 100 + n, '2026-06-20 09:00:00.000000' FROM `tmp_seed_numbers` WHERE n BETWEEN 1 AND 12;

INSERT INTO `tmp_seed_enrollments` (`class_id`, `student_user_id`, `enrolled_at`)
SELECT 102, 100 + n, '2026-06-21 09:00:00.000000' FROM `tmp_seed_numbers` WHERE n BETWEEN 13 AND 24;

INSERT INTO `tmp_seed_enrollments` (`class_id`, `student_user_id`, `enrolled_at`)
SELECT 103, 100 + n, '2026-06-15 09:00:00.000000' FROM `tmp_seed_numbers` WHERE n BETWEEN 25 AND 34;

INSERT INTO `tmp_seed_enrollments` (`class_id`, `student_user_id`, `enrolled_at`)
SELECT 104, 100 + n, '2026-06-25 09:00:00.000000' FROM `tmp_seed_numbers` WHERE n BETWEEN 35 AND 40;

INSERT INTO `tmp_seed_enrollments` (`class_id`, `student_user_id`, `enrolled_at`)
SELECT 104, 100 + n, '2026-06-25 09:00:00.000000' FROM `tmp_seed_numbers` WHERE n BETWEEN 1 AND 4;

INSERT INTO `tmp_seed_enrollments` (`class_id`, `student_user_id`, `enrolled_at`)
SELECT 105, 100 + n, '2026-06-26 09:00:00.000000' FROM `tmp_seed_numbers` WHERE n BETWEEN 5 AND 16;

INSERT INTO `tmp_seed_enrollments` (`class_id`, `student_user_id`, `enrolled_at`)
SELECT 106, 100 + n, '2026-07-20 09:00:00.000000' FROM `tmp_seed_numbers` WHERE n BETWEEN 17 AND 28;

INSERT INTO `tmp_seed_enrollments` (`class_id`, `student_user_id`, `enrolled_at`)
SELECT 107, 100 + n, '2026-06-25 09:00:00.000000' FROM `tmp_seed_numbers` WHERE n BETWEEN 29 AND 40;

INSERT INTO `tmp_seed_enrollments` (`class_id`, `student_user_id`, `enrolled_at`)
SELECT 108, student_user_id, '2026-07-01 09:00:00.000000'
FROM (
    SELECT 101 student_user_id UNION ALL SELECT 105 UNION ALL SELECT 109 UNION ALL SELECT 113 UNION ALL SELECT 117
    UNION ALL SELECT 121 UNION ALL SELECT 125 UNION ALL SELECT 129 UNION ALL SELECT 133 UNION ALL SELECT 137
) business_students;

INSERT INTO `tmp_seed_enrollments` (`class_id`, `student_user_id`, `enrolled_at`)
SELECT 109, student_user_id, '2026-07-25 09:00:00.000000'
FROM (
    SELECT 102 student_user_id UNION ALL SELECT 106 UNION ALL SELECT 110 UNION ALL SELECT 114 UNION ALL SELECT 118
    UNION ALL SELECT 122 UNION ALL SELECT 126 UNION ALL SELECT 130 UNION ALL SELECT 134 UNION ALL SELECT 138
) pronunciation_students;

INSERT INTO `tmp_seed_enrollments` (`class_id`, `student_user_id`, `enrolled_at`)
SELECT 110, student_user_id, '2026-05-20 09:00:00.000000'
FROM (
    SELECT 103 student_user_id UNION ALL SELECT 107 UNION ALL SELECT 111 UNION ALL SELECT 115 UNION ALL SELECT 119
    UNION ALL SELECT 123 UNION ALL SELECT 127 UNION ALL SELECT 131 UNION ALL SELECT 135 UNION ALL SELECT 139
) writing_students;

UPDATE `tmp_seed_enrollments`
SET `status` = 'DROPPED',
    `drop_reason` = 'PERSONAL',
    `dropped_at` = '2026-08-12 10:30:00.000000',
    `dropped_by_user_id` = 1
WHERE `class_id` = 106 AND `student_user_id` = 128;

INSERT INTO `class_enrollments` (`center_id`, `class_id`, `student_user_id`, `enrolled_by_user_id`, `status`, `enrolled_at`, `drop_reason`, `dropped_at`, `dropped_by_user_id`, `version`)
SELECT 1, class_id, student_user_id, 1, status, enrolled_at, drop_reason, dropped_at, dropped_by_user_id, 0
FROM `tmp_seed_enrollments`;

INSERT INTO `attendances` (`center_id`, `schedule_id`, `schedule_event_id`, `student_user_id`, `marked_by_user_id`, `status`, `note`, `date`, `created_at`)
SELECT
    1,
    NULL,
    se.id,
    ce.student_user_id,
    se.teacher_user_id,
    CASE MOD(se.id + ce.student_user_id, 23)
        WHEN 0 THEN 'ABSENT'
        WHEN 1 THEN 'LATE'
        WHEN 2 THEN 'EXCUSED'
        ELSE 'PRESENT'
    END,
    CASE MOD(se.id + ce.student_user_id, 23)
        WHEN 0 THEN 'Absent in seeded attendance history.'
        WHEN 1 THEN 'Arrived after class started.'
        WHEN 2 THEN 'Excused absence with parent notice.'
        ELSE NULL
    END,
    se.event_date,
    TIMESTAMP(se.event_date, se.start_time) + INTERVAL 10 MINUTE
FROM `schedule_events` se
JOIN `class_enrollments` ce ON ce.class_id = se.class_id
WHERE se.event_type = 'LESSON'
  AND se.event_date < @demo_now
  AND DATE(ce.enrolled_at) <= se.event_date
  AND (ce.dropped_at IS NULL OR DATE(ce.dropped_at) >= se.event_date);

INSERT INTO `teacher_attendances` (`center_id`, `teacher_user_id`, `schedule_event_id`, `schedule_id`, `marked_by_user_id`, `status`, `note`, `date`, `created_at`)
SELECT
    1,
    se.teacher_user_id,
    se.id,
    NULL,
    1,
    CASE MOD(se.id + se.teacher_user_id, 29)
        WHEN 0 THEN 'LATE'
        WHEN 1 THEN 'LEAVE'
        WHEN 2 THEN 'ABSENT'
        ELSE 'PRESENT'
    END,
    CASE MOD(se.id + se.teacher_user_id, 29)
        WHEN 0 THEN 'Teacher checked in late.'
        WHEN 1 THEN 'Approved leave.'
        WHEN 2 THEN 'Substitute teacher arranged.'
        ELSE NULL
    END,
    se.event_date,
    TIMESTAMP(se.event_date, se.start_time) - INTERVAL 15 MINUTE
FROM `schedule_events` se
WHERE se.event_type = 'LESSON'
  AND se.event_date < @demo_now;

CREATE TEMPORARY TABLE `tmp_seed_fee_months` (
    `month` VARCHAR(7) NOT NULL,
    `month_start` DATE NOT NULL,
    `due_date` DATE NOT NULL
);

INSERT INTO `tmp_seed_fee_months` (`month`, `month_start`, `due_date`) VALUES
('2026-06', '2026-06-01', '2026-06-10'),
('2026-07', '2026-07-01', '2026-07-10'),
('2026-08', '2026-08-01', '2026-08-10'),
('2026-09', '2026-09-01', '2026-09-10');

INSERT INTO `fee_records` (`center_id`, `class_id`, `student_user_id`, `month`, `amount`, `paid_amount`, `due_date`, `status`, `created_at`, `version`)
SELECT
    1,
    ce.class_id,
    ce.student_user_id,
    fm.month,
    c.monthly_fee,
    CASE
        WHEN fm.month < '2026-08' THEN c.monthly_fee
        WHEN fm.month = '2026-08' AND MOD(ce.student_user_id + ce.class_id, 5) IN (0, 3, 4) THEN c.monthly_fee
        WHEN fm.month = '2026-08' AND MOD(ce.student_user_id + ce.class_id, 5) = 1 THEN ROUND(c.monthly_fee / 2, 2)
        ELSE 0
    END,
    fm.due_date,
    CASE
        WHEN fm.month < '2026-08' THEN 'PAID'
        WHEN fm.month = '2026-08' AND MOD(ce.student_user_id + ce.class_id, 5) IN (0, 3, 4) THEN 'PAID'
        WHEN fm.month = '2026-08' AND MOD(ce.student_user_id + ce.class_id, 5) = 1 THEN 'PARTIAL'
        WHEN fm.month = '2026-08' THEN 'OVERDUE'
        ELSE 'UNPAID'
    END,
    TIMESTAMP(fm.month_start, '08:00:00'),
    0
FROM `class_enrollments` ce
JOIN `classes` c ON c.id = ce.class_id
JOIN `tmp_seed_fee_months` fm
WHERE fm.month_start >= STR_TO_DATE(DATE_FORMAT(c.start_date, '%Y-%m-01'), '%Y-%m-%d')
  AND fm.month_start <= STR_TO_DATE(DATE_FORMAT(COALESCE(DATE(ce.dropped_at), c.end_date), '%Y-%m-01'), '%Y-%m-%d');

INSERT INTO `payments` (`center_id`, `fee_record_id`, `student_user_id`, `collected_by_user_id`, `receipt_number`, `idempotency_key`, `amount`, `method`, `status`, `sepay_ref`, `note`, `created_at`, `expires_at`)
SELECT
    1,
    fr.id,
    fr.student_user_id,
    CASE MOD(fr.id, 2) WHEN 0 THEN 2 ELSE 3 END,
    CONCAT('OWX-', REPLACE(fr.month, '-', ''), '-', LPAD(fr.id, 5, '0')),
    CONCAT('seed-payment-', fr.id),
    fr.paid_amount,
    CASE MOD(fr.id, 4)
        WHEN 0 THEN 'CASH'
        WHEN 1 THEN 'BANK_TRANSFER'
        WHEN 2 THEN 'QR_CODE'
        ELSE 'SEPAY'
    END,
    'ACTIVE',
    CASE WHEN MOD(fr.id, 4) IN (2, 3) THEN CONCAT('SEED-SEPAY-', fr.id) ELSE NULL END,
    CASE fr.status
        WHEN 'PAID' THEN 'Seeded full tuition payment.'
        WHEN 'PARTIAL' THEN 'Seeded first installment payment.'
        ELSE NULL
    END,
    TIMESTAMP(fr.due_date, '09:00:00') + INTERVAL MOD(fr.student_user_id, 5) DAY,
    NULL
FROM `fee_records` fr
WHERE fr.paid_amount > 0;

INSERT INTO `installments` (`center_id`, `fee_record_id`, `expected_amount`, `paid_amount`, `due_date`, `status`)
SELECT
    1,
    fr.id,
    ROUND(fr.amount / 2, 2),
    CASE WHEN slot.n = 1 THEN LEAST(fr.paid_amount, ROUND(fr.amount / 2, 2)) ELSE GREATEST(fr.paid_amount - ROUND(fr.amount / 2, 2), 0) END,
    CASE WHEN slot.n = 1 THEN fr.due_date ELSE DATE_ADD(fr.due_date, INTERVAL 15 DAY) END,
    CASE
        WHEN slot.n = 1 AND fr.paid_amount >= ROUND(fr.amount / 2, 2) THEN 'PAID'
        WHEN slot.n = 2 AND fr.paid_amount >= fr.amount THEN 'PAID'
        WHEN slot.n = 2 AND fr.status = 'PARTIAL' THEN 'PENDING'
        WHEN fr.due_date < @demo_now THEN 'OVERDUE'
        ELSE 'PENDING'
    END
FROM `fee_records` fr
JOIN (SELECT 1 n UNION ALL SELECT 2) slot
WHERE fr.status IN ('PARTIAL', 'OVERDUE', 'UNPAID');

INSERT INTO `refunds` (`payment_id`, `center_id`, `amount`, `reason`, `created_by_user_id`, `created_at`, `status`, `refund_method`, `requested_by_user_id`, `approved_by_user_id`, `approved_at`, `rejected_reason`, `related_enrollment_id`)
SELECT
    p.id,
    1,
    ROUND(p.amount / 2, 2),
    'Dropped class after partial course completion.',
    2,
    '2026-08-13 10:00:00.000000',
    'PAID',
    'BANK_TRANSFER',
    2,
    1,
    '2026-08-13 11:00:00.000000',
    NULL,
    ce.id
FROM `payments` p
JOIN `fee_records` fr ON fr.id = p.fee_record_id
JOIN `class_enrollments` ce ON ce.class_id = fr.class_id AND ce.student_user_id = fr.student_user_id
WHERE fr.class_id = 106
  AND fr.student_user_id = 128
  AND fr.month = '2026-08'
LIMIT 1;

INSERT INTO `student_documents` (`center_id`, `clazz_id`, `student_user_id`, `uploader_user_id`, `title`, `description`, `file_url`, `document_type`, `created_at`) VALUES
(1, 101, NULL, 11, 'TOEIC 650 Week 1 Pack', 'Class handout and vocabulary list.', 'https://cdn.owlexa.vn/demo/saigon/toeic-650-week-1.pdf', 'PDF', '2026-07-01 12:00:00.000000'),
(1, 102, NULL, 12, 'IELTS Speaking Prompts', 'Speaking practice questions for foundation learners.', 'https://cdn.owlexa.vn/demo/saigon/ielts-speaking-prompts.pdf', 'PDF', '2026-07-03 12:00:00.000000'),
(1, 103, NULL, 13, 'Flyers Picture Cards', 'Young learner classroom material.', 'https://cdn.owlexa.vn/demo/saigon/flyers-picture-cards.pdf', 'PDF', '2026-07-05 12:00:00.000000'),
(1, 105, NULL, 15, 'IELTS Writing Checklist', 'Task response and coherence checklist.', 'https://cdn.owlexa.vn/demo/saigon/ielts-writing-checklist.pdf', 'PDF', '2026-07-08 12:00:00.000000'),
(1, 110, 115, 20, 'Writing Final Feedback', 'Completed class feedback sample.', 'https://cdn.owlexa.vn/demo/saigon/writing-final-feedback-115.pdf', 'PDF', '2026-07-31 18:00:00.000000');

INSERT INTO `audit_logs` (`center_id`, `user_id`, `action`, `entity_type`, `entity_id`, `description`, `ip_address`, `created_at`)
SELECT 1, p.collected_by_user_id, 'PAYMENT_COLLECTED', 'PAYMENT', p.id,
       CONCAT('Seeded payment receipt ', p.receipt_number), '127.0.0.1', p.created_at
FROM `payments` p;

INSERT INTO `audit_logs` (`center_id`, `user_id`, `action`, `entity_type`, `entity_id`, `description`, `ip_address`, `created_at`)
SELECT 1, 1, 'ATTENDANCE_MARKED', 'SCHEDULE_EVENT', se.id,
       CONCAT('Seeded attendance for ', se.title, ' on ', se.event_date), '127.0.0.1', TIMESTAMP(se.event_date, se.end_time)
FROM `schedule_events` se
WHERE se.event_type = 'LESSON'
  AND se.event_date < @demo_now
  AND MOD(se.id, 5) = 0;

INSERT INTO `sepay_webhook_events` (`matched_payment_id`, `processed_at`, `received_at`, `sepay_transaction_id`, `transfer_amount`, `content`, `processing_note`, `account_number`, `gateway`, `payment_code`, `raw_payload`, `reference_code`, `sub_account`, `transaction_date`, `transfer_type`, `processing_status`)
SELECT p.id, p.created_at + INTERVAL 3 MINUTE, p.created_at + INTERVAL 2 MINUTE,
       900000 + p.id, CAST(p.amount AS UNSIGNED),
       CONCAT('OWLEXA PAYMENT ', p.receipt_number), 'Seeded matched webhook event.',
       '70740011223344', 'MB', p.receipt_number,
       CONCAT('{"receipt":"', p.receipt_number, '"}'), CONCAT('REF', p.id), NULL,
       DATE_FORMAT(p.created_at, '%Y-%m-%d %H:%i:%s'), 'in', 'MATCHED'
FROM `payments` p
WHERE p.method IN ('QR_CODE', 'SEPAY')
LIMIT 40;

DROP TEMPORARY TABLE IF EXISTS `tmp_seed_fee_months`;
DROP TEMPORARY TABLE IF EXISTS `tmp_seed_enrollments`;
DROP TEMPORARY TABLE IF EXISTS `tmp_seed_numbers`;
