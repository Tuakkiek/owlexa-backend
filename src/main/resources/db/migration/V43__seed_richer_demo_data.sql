-- Richer demo data for a freshly rebuilt local database.
-- Default password for all seeded non-admin users: password123
-- BCrypt hash: $2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS

SET @seed_password = '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS';
SET @doc_empty = '{"type":"doc","content":[{"type":"paragraph"}]}';
SET @doc_overview = '{"type":"doc","content":[{"type":"heading","attrs":{"level":2},"content":[{"type":"text","text":"Demo learning material"}]},{"type":"paragraph","content":[{"type":"text","text":"This seeded document gives teachers and students realistic content for local testing."}]}]}';

-- ---------------------------------------------------------------------
-- Organization, staff, teachers, and students
-- ---------------------------------------------------------------------

INSERT IGNORE INTO `users` (`id`, `email`, `full_name`, `password`, `phone_number`, `role`, `is_active`) VALUES
(101, 'manager.metro@owlexa.edu.vn', 'Tran Gia Bao', @seed_password, '0910000101', 'MANAGER', b'1'),
(102, 'academic.metro@owlexa.edu.vn', 'Nguyen Minh Chau', @seed_password, '0910000102', 'ACADEMIC_STAFF', b'1'),
(103, 'cashier.metro@owlexa.edu.vn', 'Pham Thu Ngan', @seed_password, '0910000103', 'CASHIER', b'1'),
(104, 'teacher.alex@owlexa.edu.vn', 'Alex Morgan', @seed_password, '0910000104', 'TEACHER', b'1'),
(105, 'teacher.maya@owlexa.edu.vn', 'Maya Tran', @seed_password, '0910000105', 'TEACHER', b'1'),
(106, 'teacher.minh@owlexa.edu.vn', 'Le Quang Minh', @seed_password, '0910000106', 'TEACHER', b'1'),
(107, 'teacher.linh@owlexa.edu.vn', 'Do Phuong Linh', @seed_password, '0910000107', 'TEACHER', b'1'),
(108, 'teacher.owen@owlexa.edu.vn', 'Owen Parker', @seed_password, '0910000108', 'TEACHER', b'1'),
(121, 'student.anhthu@owlexa.edu.vn', 'Vo Anh Thu', @seed_password, '0910000121', 'STUDENT', b'1'),
(122, 'student.baongoc@owlexa.edu.vn', 'Huynh Bao Ngoc', @seed_password, '0910000122', 'STUDENT', b'1'),
(123, 'student.duy@owlexa.edu.vn', 'Phan Quoc Duy', @seed_password, '0910000123', 'STUDENT', b'1'),
(124, 'student.han@owlexa.edu.vn', 'Mai Gia Han', @seed_password, '0910000124', 'STUDENT', b'1'),
(125, 'student.khoa@owlexa.edu.vn', 'Dang Minh Khoa', @seed_password, '0910000125', 'STUDENT', b'1'),
(126, 'student.lam@owlexa.edu.vn', 'Bui Hoang Lam', @seed_password, '0910000126', 'STUDENT', b'1'),
(127, 'student.my@owlexa.edu.vn', 'Ngo Tra My', @seed_password, '0910000127', 'STUDENT', b'1'),
(128, 'student.nam@owlexa.edu.vn', 'Hoang Duc Nam', @seed_password, '0910000128', 'STUDENT', b'1'),
(129, 'student.nhi@owlexa.edu.vn', 'Le Yen Nhi', @seed_password, '0910000129', 'STUDENT', b'1'),
(130, 'student.phuc@owlexa.edu.vn', 'Tran Bao Phuc', @seed_password, '0910000130', 'STUDENT', b'1'),
(131, 'student.quynh@owlexa.edu.vn', 'Nguyen Nhu Quynh', @seed_password, '0910000131', 'STUDENT', b'1'),
(132, 'student.son@owlexa.edu.vn', 'Pham Thai Son', @seed_password, '0910000132', 'STUDENT', b'1'),
(133, 'student.tam@owlexa.edu.vn', 'Do Ngoc Tam', @seed_password, '0910000133', 'STUDENT', b'1'),
(134, 'student.tri@owlexa.edu.vn', 'Cao Minh Tri', @seed_password, '0910000134', 'STUDENT', b'1'),
(135, 'student.tuan@owlexa.edu.vn', 'Vu Anh Tuan', @seed_password, '0910000135', 'STUDENT', b'1'),
(136, 'student.vy@owlexa.edu.vn', 'Le Tuong Vy', @seed_password, '0910000136', 'STUDENT', b'1'),
(137, 'student.hothiyen@owlexa.edu.vn', 'Ho Thi Yen', @seed_password, '0910000137', 'STUDENT', b'1'),
(138, 'student.anhtai@owlexa.edu.vn', 'Tran Anh Tai', @seed_password, '0910000138', 'STUDENT', b'1'),
(139, 'student.hoainam@owlexa.edu.vn', 'Nguyen Hoai Nam', @seed_password, '0910000139', 'STUDENT', b'1'),
(140, 'student.minhanh@owlexa.edu.vn', 'Phung Minh Anh', @seed_password, '0910000140', 'STUDENT', b'1'),
(141, 'manager.online@owlexa.edu.vn', 'Le Khanh Vy', @seed_password, '0910000141', 'MANAGER', b'1'),
(142, 'academic.online@owlexa.edu.vn', 'Do Thanh Tu', @seed_password, '0910000142', 'ACADEMIC_STAFF', b'1'),
(143, 'cashier.online@owlexa.edu.vn', 'Nguyen Phuong Mai', @seed_password, '0910000143', 'CASHIER', b'1'),
(144, 'teacher.sophia@owlexa.edu.vn', 'Sophia Nguyen', @seed_password, '0910000144', 'TEACHER', b'1'),
(145, 'teacher.brian@owlexa.edu.vn', 'Brian Lee', @seed_password, '0910000145', 'TEACHER', b'1'),
(151, 'student.online01@owlexa.edu.vn', 'Online Student 01', @seed_password, '0910000151', 'STUDENT', b'1'),
(152, 'student.online02@owlexa.edu.vn', 'Online Student 02', @seed_password, '0910000152', 'STUDENT', b'1'),
(153, 'student.online03@owlexa.edu.vn', 'Online Student 03', @seed_password, '0910000153', 'STUDENT', b'1'),
(154, 'student.online04@owlexa.edu.vn', 'Online Student 04', @seed_password, '0910000154', 'STUDENT', b'1'),
(155, 'student.online05@owlexa.edu.vn', 'Online Student 05', @seed_password, '0910000155', 'STUDENT', b'1'),
(156, 'student.online06@owlexa.edu.vn', 'Online Student 06', @seed_password, '0910000156', 'STUDENT', b'1'),
(157, 'student.online07@owlexa.edu.vn', 'Online Student 07', @seed_password, '0910000157', 'STUDENT', b'1'),
(158, 'student.online08@owlexa.edu.vn', 'Online Student 08', @seed_password, '0910000158', 'STUDENT', b'1'),
(159, 'student.online09@owlexa.edu.vn', 'Online Student 09', @seed_password, '0910000159', 'STUDENT', b'1'),
(160, 'student.online10@owlexa.edu.vn', 'Online Student 10', @seed_password, '0910000160', 'STUDENT', b'1');

INSERT IGNORE INTO `centers` (`id`, `owner_user_id`, `name`, `subdomain`, `created_at`, `is_active`) VALUES
(3, 1, 'Owlexa Metro Campus - District 7', 'd7-metro', '2026-08-01 08:00:00.000000', b'1'),
(4, 1, 'Owlexa Online Academy', 'online-academy', '2026-08-01 08:15:00.000000', b'1');

INSERT IGNORE INTO `membership` (`center_id`, `user_id`, `joined_by_user_id`, `joined_at`)
SELECT 3, u.id, 1, '2026-08-01 09:00:00.000000'
FROM `users` u
WHERE u.id IN (1,101,102,103,104,105,106,107,108,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140);

INSERT IGNORE INTO `membership` (`center_id`, `user_id`, `joined_by_user_id`, `joined_at`)
SELECT 4, u.id, 1, '2026-08-01 09:30:00.000000'
FROM `users` u
WHERE u.id IN (1,141,142,143,144,145,151,152,153,154,155,156,157,158,159,160);

INSERT IGNORE INTO `teacher_center_profile` (`center_id`, `teacher_user_id`, `salary`, `currency`, `created_at`, `updated_at`) VALUES
(3, 104, 38000000.00, 'VND', '2026-08-01 10:00:00.000000', '2026-08-01 10:00:00.000000'),
(3, 105, 30000000.00, 'VND', '2026-08-01 10:05:00.000000', '2026-08-01 10:05:00.000000'),
(3, 106, 27000000.00, 'VND', '2026-08-01 10:10:00.000000', '2026-08-01 10:10:00.000000'),
(3, 107, 25000000.00, 'VND', '2026-08-01 10:15:00.000000', '2026-08-01 10:15:00.000000'),
(3, 108, 42000000.00, 'VND', '2026-08-01 10:20:00.000000', '2026-08-01 10:20:00.000000'),
(4, 144, 34000000.00, 'VND', '2026-08-01 10:30:00.000000', '2026-08-01 10:30:00.000000'),
(4, 145, 36000000.00, 'VND', '2026-08-01 10:35:00.000000', '2026-08-01 10:35:00.000000');

-- ---------------------------------------------------------------------
-- Courses, rooms, classes, time slots, and generated schedule events
-- ---------------------------------------------------------------------

INSERT IGNORE INTO `courses` (`id`, `center_id`, `code`, `name`, `description`, `default_duration`, `default_session_count`, `default_teacher_user_id`, `default_monthly_fee`, `is_active`, `created_at`, `updated_at`) VALUES
(101, 3, 'TOEIC900-D7', 'TOEIC Master 900+', 'Advanced TOEIC program with weekly mock tests and targeted listening drills.', 72, 24, 104, 5200000.00, b'1', '2026-08-01 11:00:00.000000', '2026-08-01 11:00:00.000000'),
(102, 3, 'IELTSF-D7', 'IELTS Foundation 5.5+', 'Foundation class for learners moving from general English to IELTS.', 60, 20, 105, 3900000.00, b'1', '2026-08-01 11:05:00.000000', '2026-08-01 11:05:00.000000'),
(103, 3, 'KIDSFLY-D7', 'Cambridge Flyers for Kids', 'Young learner course with vocabulary games, phonics, and speaking routines.', 48, 16, 107, 2600000.00, b'1', '2026-08-01 11:10:00.000000', '2026-08-01 11:10:00.000000'),
(104, 3, 'PUB-SPEAK', 'Public Speaking Studio', 'Presentation skills, storytelling, debate, and pronunciation coaching.', 36, 12, 108, 3200000.00, b'1', '2026-08-01 11:15:00.000000', '2026-08-01 11:15:00.000000'),
(111, 4, 'ONLINE-TOEIC', 'Online TOEIC Sprint', 'Remote TOEIC preparation with live classes and auto-graded homework.', 48, 16, 144, 2800000.00, b'1', '2026-08-01 11:20:00.000000', '2026-08-01 11:20:00.000000'),
(112, 4, 'ACAD-WRITE', 'Academic Writing Lab', 'Online writing workshop with AI feedback and teacher review.', 40, 10, 145, 3400000.00, b'1', '2026-08-01 11:25:00.000000', '2026-08-01 11:25:00.000000'),
(113, 4, 'INTERVIEW', 'English Interview Prep', 'Practical interview class for job seekers and university applicants.', 24, 8, 144, 2400000.00, b'1', '2026-08-01 11:30:00.000000', '2026-08-01 11:30:00.000000');

INSERT IGNORE INTO `rooms` (`id`, `center_id`, `code`, `name`, `capacity`, `description`, `is_active`, `created_at`, `updated_at`) VALUES
(101, 3, 'D7-A101', 'Metro A101', 24, 'Smart classroom for TOEIC and IELTS evening classes.', b'1', '2026-08-01 12:00:00.000000', '2026-08-01 12:00:00.000000'),
(102, 3, 'D7-A102', 'Metro A102', 18, 'Discussion room with movable desks.', b'1', '2026-08-01 12:05:00.000000', '2026-08-01 12:05:00.000000'),
(103, 3, 'D7-KIDS', 'Kids Studio', 16, 'Colorful room for young learners.', b'1', '2026-08-01 12:10:00.000000', '2026-08-01 12:10:00.000000'),
(104, 3, 'D7-LAB', 'Listening Lab', 22, 'Computer lab with individual headsets.', b'1', '2026-08-01 12:15:00.000000', '2026-08-01 12:15:00.000000'),
(105, 3, 'D7-STAGE', 'Presentation Stage', 30, 'Small stage for speech practice and mock presentations.', b'1', '2026-08-01 12:20:00.000000', '2026-08-01 12:20:00.000000'),
(111, 4, 'ONLINE-A', 'Online Room A', 120, 'Primary live-stream classroom.', b'1', '2026-08-01 12:30:00.000000', '2026-08-01 12:30:00.000000'),
(112, 4, 'ONLINE-B', 'Online Room B', 120, 'Workshop and consultation room.', b'1', '2026-08-01 12:35:00.000000', '2026-08-01 12:35:00.000000');

INSERT IGNORE INTO `teaching_time_slots` (`id`, `center_id`, `name`, `period`, `start_time`, `end_time`, `display_order`, `is_active`, `created_at`, `updated_at`) VALUES
(301, 3, 'Morning 1', 'MORNING', '08:00:00', '09:30:00', 1, b'1', '2026-08-01 12:40:00.000000', '2026-08-01 12:40:00.000000'),
(302, 3, 'Morning 2', 'MORNING', '09:45:00', '11:15:00', 2, b'1', '2026-08-01 12:40:00.000000', '2026-08-01 12:40:00.000000'),
(303, 3, 'Evening 1', 'EVENING', '18:15:00', '19:45:00', 3, b'1', '2026-08-01 12:40:00.000000', '2026-08-01 12:40:00.000000'),
(304, 3, 'Evening 2', 'EVENING', '19:50:00', '21:20:00', 4, b'1', '2026-08-01 12:40:00.000000', '2026-08-01 12:40:00.000000'),
(401, 4, 'Online Evening', 'EVENING', '19:00:00', '20:30:00', 1, b'1', '2026-08-01 12:45:00.000000', '2026-08-01 12:45:00.000000'),
(402, 4, 'Online Weekend', 'MORNING', '09:00:00', '10:30:00', 2, b'1', '2026-08-01 12:45:00.000000', '2026-08-01 12:45:00.000000');

INSERT IGNORE INTO `classes` (`id`, `center_id`, `course_id`, `teacher_user_id`, `name`, `description`, `start_date`, `end_date`, `monthly_fee`, `status`, `create_at`) VALUES
(101, 3, 101, 104, 'D7 TOEIC 900+ Aug 2026', 'Advanced TOEIC class with lab practice every Thursday.', '2026-08-18', '2026-10-15', 5200000.00, 'ACTIVE', '2026-08-01 13:00:00.000000'),
(102, 3, 102, 105, 'D7 IELTS Foundation Aug 2026', 'IELTS foundation class for grammar, reading, and speaking confidence.', '2026-08-17', '2026-10-09', 3900000.00, 'ACTIVE', '2026-08-01 13:05:00.000000'),
(103, 3, 103, 107, 'D7 Flyers Weekend Aug 2026', 'Weekend young learner class for Cambridge Flyers.', '2026-08-22', '2026-10-18', 2600000.00, 'ACTIVE', '2026-08-01 13:10:00.000000'),
(104, 3, 104, 108, 'D7 Public Speaking Sep 2026', 'Planned public speaking studio opening next month.', '2026-09-07', '2026-10-19', 3200000.00, 'PLANNED', '2026-08-01 13:15:00.000000'),
(111, 4, 111, 144, 'Online TOEIC Sprint Aug 2026', 'Live online TOEIC sprint with weekly timed quizzes.', '2026-08-18', '2026-10-08', 2800000.00, 'ACTIVE', '2026-08-01 13:20:00.000000'),
(112, 4, 112, 145, 'Online Academic Writing Lab', 'Saturday writing lab using AI grading plus teacher moderation.', '2026-08-16', '2026-09-20', 3400000.00, 'ACTIVE', '2026-08-01 13:25:00.000000'),
(113, 4, 113, 144, 'Interview Prep Sep 2026', 'Planned online interview class.', '2026-09-03', '2026-09-26', 2400000.00, 'PLANNED', '2026-08-01 13:30:00.000000');

INSERT IGNORE INTO `schedule_recurring_rules` (`id`, `center_id`, `class_id`, `room_id`, `time_slot_id`, `teacher_user_id`, `repeat_type`, `days_of_week`, `start_date`, `end_date`, `start_time`, `end_time`, `type`, `is_active`, `created_at`, `updated_at`) VALUES
(2101, 3, 101, 101, 303, 104, 'WEEKLY', '2,4', '2026-08-18', '2026-10-15', '18:15:00', '19:45:00', 'THEORY_CLASS', b'1', '2026-08-01 14:00:00.000000', '2026-08-01 14:00:00.000000'),
(2102, 3, 102, 102, 304, 105, 'WEEKLY', '1,3,5', '2026-08-17', '2026-10-09', '19:50:00', '21:20:00', 'THEORY_CLASS', b'1', '2026-08-01 14:05:00.000000', '2026-08-01 14:05:00.000000'),
(2103, 3, 103, 103, 301, 107, 'WEEKLY', '6,7', '2026-08-22', '2026-10-18', '08:00:00', '09:30:00', 'THEORY_CLASS', b'1', '2026-08-01 14:10:00.000000', '2026-08-01 14:10:00.000000'),
(2104, 3, 104, 105, 303, 108, 'WEEKLY', '1,3', '2026-09-07', '2026-10-19', '18:15:00', '19:45:00', 'THEORY_CLASS', b'1', '2026-08-01 14:15:00.000000', '2026-08-01 14:15:00.000000'),
(2111, 4, 111, 111, 401, 144, 'WEEKLY', '2,4', '2026-08-18', '2026-10-08', '19:00:00', '20:30:00', 'ONLINE_CLASS', b'1', '2026-08-01 14:20:00.000000', '2026-08-01 14:20:00.000000'),
(2112, 4, 112, 112, 402, 145, 'WEEKLY', '6', '2026-08-16', '2026-09-20', '09:00:00', '10:30:00', 'ONLINE_CLASS', b'1', '2026-08-01 14:25:00.000000', '2026-08-01 14:25:00.000000');

INSERT INTO `schedule_events` (`center_id`, `class_id`, `recurring_rule_id`, `room_id`, `teacher_user_id`, `event_date`, `start_time`, `end_time`, `lesson_number`, `event_type`, `status`, `title`, `note`, `created_at`, `updated_at`)
SELECT g.center_id, g.class_id, g.rule_id, g.room_id, g.teacher_user_id,
       g.event_date, g.start_time, g.end_time, g.lesson_number,
       CASE WHEN g.type = 'ONLINE_CLASS' THEN 'ONLINE_LESSON' ELSE 'LESSON' END,
       'SCHEDULED', c.name, NULL, NOW(6), NOW(6)
FROM (
    SELECT r.id AS rule_id, r.center_id, r.class_id, r.room_id, r.teacher_user_id, r.start_time, r.end_time, r.type,
           DATE_ADD(r.start_date, INTERVAL offsets.day_offset DAY) AS event_date,
           ROW_NUMBER() OVER (
               PARTITION BY r.id
               ORDER BY DATE_ADD(r.start_date, INTERVAL offsets.day_offset DAY)
           ) AS lesson_number
    FROM `schedule_recurring_rules` r
    CROSS JOIN (
        SELECT ones.n + tens.n * 10 AS day_offset
        FROM (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
              UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
        CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
                    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
    ) offsets
    WHERE r.id IN (2101,2102,2103,2104,2111,2112)
      AND DATE_ADD(r.start_date, INTERVAL offsets.day_offset DAY) <= r.end_date
      AND FIND_IN_SET(
            IF(DAYOFWEEK(DATE_ADD(r.start_date, INTERVAL offsets.day_offset DAY)) = 1,
               7, DAYOFWEEK(DATE_ADD(r.start_date, INTERVAL offsets.day_offset DAY)) - 1),
            r.days_of_week
          ) > 0
) g
JOIN `classes` c ON c.id = g.class_id
WHERE g.lesson_number <= CASE g.class_id
    WHEN 101 THEN 18 WHEN 102 THEN 20 WHEN 103 THEN 16 WHEN 104 THEN 12 WHEN 111 THEN 16 WHEN 112 THEN 6 ELSE 20 END
  AND NOT EXISTS (
      SELECT 1 FROM `schedule_events` e
      WHERE e.recurring_rule_id = g.rule_id
        AND e.event_date = g.event_date
  );

INSERT IGNORE INTO `schedule_events` (`id`, `center_id`, `class_id`, `recurring_rule_id`, `room_id`, `teacher_user_id`, `event_date`, `start_time`, `end_time`, `lesson_number`, `event_type`, `status`, `title`, `note`, `created_at`, `updated_at`) VALUES
(3101, 3, 101, NULL, 104, 104, '2026-09-19', '18:00:00', '20:00:00', NULL, 'EXAM', 'SCHEDULED', 'D7 TOEIC Full Mock Test 01', 'Saturday mock test in the listening lab.', '2026-08-01 14:40:00.000000', '2026-08-01 14:40:00.000000'),
(3102, 3, 102, NULL, 102, 105, '2026-09-12', '18:30:00', '20:30:00', NULL, 'PRACTICE', 'SCHEDULED', 'IELTS Speaking Clinic', 'One-on-one speaking feedback night.', '2026-08-01 14:45:00.000000', '2026-08-01 14:45:00.000000'),
(3103, 4, 112, NULL, 112, 145, '2026-09-05', '09:00:00', '11:00:00', NULL, 'PRACTICE', 'SCHEDULED', 'Writing Lab Live Review', 'Teacher review workshop after AI feedback.', '2026-08-01 14:50:00.000000', '2026-08-01 14:50:00.000000');

-- ---------------------------------------------------------------------
-- Enrollment, attendance, fees, payments, refunds, and documents
-- ---------------------------------------------------------------------

INSERT IGNORE INTO `class_enrollments` (`id`, `center_id`, `class_id`, `student_user_id`, `enrolled_by_user_id`, `status`, `enrolled_at`, `drop_reason`, `dropped_at`, `dropped_by_user_id`, `version`) VALUES
(2001, 3, 101, 121, 102, 'ACTIVE', '2026-08-02 09:00:00.000000', NULL, NULL, NULL, 0),
(2002, 3, 101, 122, 102, 'ACTIVE', '2026-08-02 09:05:00.000000', NULL, NULL, NULL, 0),
(2003, 3, 101, 123, 102, 'ACTIVE', '2026-08-02 09:10:00.000000', NULL, NULL, NULL, 0),
(2004, 3, 101, 124, 102, 'ACTIVE', '2026-08-02 09:15:00.000000', NULL, NULL, NULL, 0),
(2005, 3, 101, 125, 102, 'ACTIVE', '2026-08-02 09:20:00.000000', NULL, NULL, NULL, 0),
(2006, 3, 101, 126, 102, 'ACTIVE', '2026-08-02 09:25:00.000000', NULL, NULL, NULL, 0),
(2007, 3, 101, 127, 102, 'PENDING', '2026-08-02 09:30:00.000000', NULL, NULL, NULL, 0),
(2008, 3, 101, 128, 102, 'DROPPED', '2026-08-02 09:35:00.000000', 'PERSONAL', '2026-08-12 10:00:00.000000', 102, 1),
(2009, 3, 102, 129, 102, 'ACTIVE', '2026-08-03 09:00:00.000000', NULL, NULL, NULL, 0),
(2010, 3, 102, 130, 102, 'ACTIVE', '2026-08-03 09:05:00.000000', NULL, NULL, NULL, 0),
(2011, 3, 102, 131, 102, 'ACTIVE', '2026-08-03 09:10:00.000000', NULL, NULL, NULL, 0),
(2012, 3, 102, 132, 102, 'ACTIVE', '2026-08-03 09:15:00.000000', NULL, NULL, NULL, 0),
(2013, 3, 102, 133, 102, 'ACTIVE', '2026-08-03 09:20:00.000000', NULL, NULL, NULL, 0),
(2014, 3, 102, 134, 102, 'ACTIVE', '2026-08-03 09:25:00.000000', NULL, NULL, NULL, 0),
(2015, 3, 103, 135, 102, 'ACTIVE', '2026-08-04 09:00:00.000000', NULL, NULL, NULL, 0),
(2016, 3, 103, 136, 102, 'ACTIVE', '2026-08-04 09:05:00.000000', NULL, NULL, NULL, 0),
(2017, 3, 103, 137, 102, 'ACTIVE', '2026-08-04 09:10:00.000000', NULL, NULL, NULL, 0),
(2018, 3, 103, 138, 102, 'ACTIVE', '2026-08-04 09:15:00.000000', NULL, NULL, NULL, 0),
(2019, 3, 103, 139, 102, 'ACTIVE', '2026-08-04 09:20:00.000000', NULL, NULL, NULL, 0),
(2020, 3, 103, 140, 102, 'ACTIVE', '2026-08-04 09:25:00.000000', NULL, NULL, NULL, 0),
(2021, 4, 111, 151, 142, 'ACTIVE', '2026-08-04 10:00:00.000000', NULL, NULL, NULL, 0),
(2022, 4, 111, 152, 142, 'ACTIVE', '2026-08-04 10:05:00.000000', NULL, NULL, NULL, 0),
(2023, 4, 111, 153, 142, 'ACTIVE', '2026-08-04 10:10:00.000000', NULL, NULL, NULL, 0),
(2024, 4, 111, 154, 142, 'ACTIVE', '2026-08-04 10:15:00.000000', NULL, NULL, NULL, 0),
(2025, 4, 111, 155, 142, 'SUSPENDED', '2026-08-04 10:20:00.000000', NULL, NULL, NULL, 1),
(2026, 4, 112, 156, 142, 'ACTIVE', '2026-08-05 10:00:00.000000', NULL, NULL, NULL, 0),
(2027, 4, 112, 157, 142, 'ACTIVE', '2026-08-05 10:05:00.000000', NULL, NULL, NULL, 0),
(2028, 4, 112, 158, 142, 'ACTIVE', '2026-08-05 10:10:00.000000', NULL, NULL, NULL, 0),
(2029, 4, 112, 159, 142, 'ACTIVE', '2026-08-05 10:15:00.000000', NULL, NULL, NULL, 0),
(2030, 4, 112, 160, 142, 'ACTIVE', '2026-08-05 10:20:00.000000', NULL, NULL, NULL, 0);

INSERT IGNORE INTO `attendances` (`center_id`, `schedule_id`, `schedule_event_id`, `student_user_id`, `marked_by_user_id`, `status`, `note`, `date`, `created_at`)
SELECT e.center_id, NULL, e.id, ce.student_user_id, e.teacher_user_id,
       CASE
         WHEN ce.student_user_id IN (123,132,158) THEN 'LATE'
         WHEN ce.student_user_id IN (126,139) THEN 'ABSENT'
         ELSE 'PRESENT'
       END,
       CASE
         WHEN ce.student_user_id IN (123,132,158) THEN 'Arrived late but completed the class activity.'
         WHEN ce.student_user_id IN (126,139) THEN 'Absent in seeded attendance sample.'
         ELSE 'Present and participated.'
       END,
       e.event_date,
       TIMESTAMP(e.event_date, e.end_time)
FROM `schedule_events` e
JOIN `class_enrollments` ce ON ce.class_id = e.class_id AND ce.status = 'ACTIVE'
WHERE e.event_date IN ('2026-08-18','2026-08-19','2026-08-22','2026-08-23')
  AND e.class_id IN (101,102,103,111,112);

INSERT IGNORE INTO `teacher_attendances` (`center_id`, `teacher_user_id`, `schedule_event_id`, `schedule_id`, `marked_by_user_id`, `status`, `note`, `date`, `created_at`)
SELECT DISTINCT e.center_id, e.teacher_user_id, e.id, NULL,
       CASE WHEN e.center_id = 3 THEN 102 ELSE 142 END,
       CASE WHEN e.teacher_user_id = 105 AND e.event_date = '2026-08-19' THEN 'LATE' ELSE 'PRESENT' END,
       CASE WHEN e.teacher_user_id = 105 AND e.event_date = '2026-08-19' THEN 'Teacher started five minutes late.' ELSE 'Class delivered as scheduled.' END,
       e.event_date,
       TIMESTAMP(e.event_date, e.end_time)
FROM `schedule_events` e
WHERE e.event_date IN ('2026-08-18','2026-08-19','2026-08-22','2026-08-23')
  AND e.class_id IN (101,102,103,111,112);

INSERT INTO `fee_records` (`center_id`, `class_id`, `student_user_id`, `month`, `amount`, `paid_amount`, `due_date`, `status`, `created_at`, `version`)
SELECT ce.center_id, ce.class_id, ce.student_user_id, '2026-08', c.monthly_fee,
       CASE
         WHEN ce.student_user_id IN (121,122,124,129,130,135,136,151,152,156,157) THEN c.monthly_fee
         WHEN ce.student_user_id IN (123,131,153,158) THEN ROUND(c.monthly_fee / 2, 2)
         ELSE 0
       END,
       '2026-08-10',
       CASE
         WHEN ce.student_user_id IN (121,122,124,129,130,135,136,151,152,156,157) THEN 'PAID'
         WHEN ce.student_user_id IN (123,131,153,158) THEN 'PARTIAL'
         WHEN ce.student_user_id IN (126,133,139,155) THEN 'OVERDUE'
         ELSE 'UNPAID'
       END,
       '2026-08-01 08:00:00.000000',
       0
FROM `class_enrollments` ce
JOIN `classes` c ON c.id = ce.class_id
WHERE ce.class_id IN (101,102,103,111,112)
  AND ce.status IN ('ACTIVE','SUSPENDED')
  AND NOT EXISTS (
      SELECT 1 FROM `fee_records` fr
      WHERE fr.student_user_id = ce.student_user_id
        AND fr.class_id = ce.class_id
        AND fr.month = '2026-08'
  );

INSERT INTO `fee_records` (`center_id`, `class_id`, `student_user_id`, `month`, `amount`, `paid_amount`, `due_date`, `status`, `created_at`, `version`)
SELECT ce.center_id, ce.class_id, ce.student_user_id, '2026-09', c.monthly_fee, 0,
       '2026-09-10', 'UNPAID', '2026-08-14 08:00:00.000000', 0
FROM `class_enrollments` ce
JOIN `classes` c ON c.id = ce.class_id
WHERE ce.class_id IN (101,102,103,111,112)
  AND ce.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1 FROM `fee_records` fr
      WHERE fr.student_user_id = ce.student_user_id
        AND fr.class_id = ce.class_id
        AND fr.month = '2026-09'
  );

INSERT INTO `payments` (`center_id`, `fee_record_id`, `student_user_id`, `collected_by_user_id`, `receipt_number`, `idempotency_key`, `amount`, `method`, `status`, `sepay_ref`, `note`, `created_at`, `expires_at`)
SELECT fr.center_id, fr.id, fr.student_user_id,
       CASE WHEN fr.center_id = 3 THEN 103 ELSE 143 END,
       CONCAT('R43-', fr.id),
       CONCAT('seed43-', fr.id),
       fr.paid_amount,
       CASE
         WHEN fr.student_user_id IN (121,129,151,156) THEN 'SEPAY'
         WHEN fr.student_user_id IN (122,130,152,157) THEN 'BANK_TRANSFER'
         ELSE 'CASH'
       END,
       'ACTIVE',
       CASE WHEN fr.student_user_id IN (121,129,151,156) THEN CONCAT('SEED43-TX-', fr.id) ELSE NULL END,
       CONCAT('Seeded payment for ', fr.month),
       TIMESTAMP(fr.due_date, '10:00:00'),
       NULL
FROM `fee_records` fr
WHERE fr.month = '2026-08'
  AND fr.paid_amount > 0
  AND NOT EXISTS (
      SELECT 1 FROM `payments` p WHERE p.idempotency_key = CONCAT('seed43-', fr.id)
  );

INSERT IGNORE INTO `installments` (`center_id`, `fee_record_id`, `expected_amount`, `paid_amount`, `due_date`, `status`)
SELECT fr.center_id, fr.id, ROUND(fr.amount / 2, 2), fr.paid_amount, '2026-08-10',
       CASE WHEN fr.paid_amount >= ROUND(fr.amount / 2, 2) THEN 'PAID' ELSE 'PENDING' END
FROM `fee_records` fr
WHERE fr.month = '2026-08'
  AND fr.status = 'PARTIAL';

INSERT IGNORE INTO `refunds` (`center_id`, `payment_id`, `created_by_user_id`, `amount`, `reason`, `created_at`, `status`, `refund_method`, `requested_by_user_id`, `approved_by_user_id`, `approved_at`, `rejected_reason`, `related_enrollment_id`)
SELECT p.center_id, p.id, 103, 650000.00, 'Learner changed to a lighter study plan.', '2026-08-13 15:00:00.000000',
       'APPROVED', 'BANK_TRANSFER', 103, 1, '2026-08-13 17:00:00.000000', NULL, ce.id
FROM `payments` p
JOIN `fee_records` fr ON fr.id = p.fee_record_id
JOIN `class_enrollments` ce ON ce.class_id = fr.class_id AND ce.student_user_id = fr.student_user_id
WHERE fr.student_user_id = 124 AND fr.class_id = 101 AND fr.month = '2026-08'
  AND NOT EXISTS (SELECT 1 FROM `refunds` r WHERE r.payment_id = p.id);

INSERT IGNORE INTO `student_documents` (`center_id`, `clazz_id`, `student_user_id`, `uploader_user_id`, `title`, `description`, `file_url`, `document_type`, `created_at`) VALUES
(3, 101, 121, 104, 'TOEIC 900 Week 1 Homework Pack', 'Listening drills and reading vocabulary for week 1.', 'https://cdn.owlexa.vn/demo/d7/toeic900-week01.pdf', 'PDF', '2026-08-18 21:00:00.000000'),
(3, 102, 129, 105, 'IELTS Foundation Speaking Prompts', 'Speaking part 1 and part 2 prompts for guided practice.', 'https://cdn.owlexa.vn/demo/d7/ielts-foundation-speaking.pdf', 'PDF', '2026-08-19 21:30:00.000000'),
(3, 103, NULL, 107, 'Flyers Parent Briefing Video', 'Short video introduction for parents.', 'https://cdn.owlexa.vn/demo/d7/flyers-parent-briefing.mp4', 'VIDEO', '2026-08-20 10:00:00.000000'),
(4, 112, 156, 145, 'Academic Writing Feedback Guide', 'Guide for reading AI feedback before teacher review.', 'https://cdn.owlexa.vn/demo/online/writing-feedback-guide.pdf', 'PDF', '2026-08-20 11:00:00.000000');

INSERT IGNORE INTO `audit_logs` (`center_id`, `user_id`, `action`, `entity_type`, `entity_id`, `description`, `ip_address`, `created_at`) VALUES
(3, 102, 'SEED_ENROLLMENT', 'CLASS', 101, 'Seeded District 7 enrollments for local QA.', '127.0.0.1', '2026-08-14 09:00:00.000000'),
(3, 103, 'SEED_PAYMENT', 'PAYMENT', NULL, 'Seeded mixed payment states for cashier dashboards.', '127.0.0.1', '2026-08-14 09:05:00.000000'),
(4, 142, 'SEED_ONLINE_CLASS', 'CLASS', 112, 'Seeded online writing lab workflow.', '127.0.0.1', '2026-08-14 09:10:00.000000');

-- ---------------------------------------------------------------------
-- Files, question banks, assessments, assignments, submissions, AI review
-- ---------------------------------------------------------------------

INSERT IGNORE INTO `files` (`id`, `center_id`, `original_name`, `stored_name`, `mime_type`, `file_type`, `extension`, `size`, `path`, `url`, `storage_provider`, `status`, `uploaded_by`, `created_at`, `updated_at`, `last_referenced_at`) VALUES
(2101, 3, 'd7-toeic-mini-test-audio.mp3', 'seed43-d7-toeic-mini-test-audio.mp3', 'audio/mpeg', 'AUDIO', 'mp3', 7340032, 'demo/d7/toeic-mini-test-audio.mp3', 'https://cdn.owlexa.vn/demo/d7/toeic-mini-test-audio.mp3', 'S3', 'ACTIVE', 104, '2026-08-10 08:00:00.000000', '2026-08-10 08:00:00.000000', '2026-08-14 09:00:00.000000'),
(2102, 4, 'online-writing-rubric.pdf', 'seed43-online-writing-rubric.pdf', 'application/pdf', 'PDF', 'pdf', 1048576, 'demo/online/writing-rubric.pdf', 'https://cdn.owlexa.vn/demo/online/writing-rubric.pdf', 'S3', 'ACTIVE', 145, '2026-08-10 08:05:00.000000', '2026-08-10 08:05:00.000000', '2026-08-14 09:05:00.000000');

INSERT IGNORE INTO `grading_criteria` (`id`, `center_id`, `created_by`, `updated_by`, `name`, `content_json`, `created_at`, `updated_at`) VALUES
(2101, 3, 104, 104, 'TOEIC short response rubric', '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Score for accuracy, vocabulary control, and task completion."}]}]}', '2026-08-10 09:00:00.000000', '2026-08-10 09:00:00.000000'),
(2102, 3, 105, 105, 'IELTS speaking band descriptors', '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Assess fluency, coherence, lexical range, grammar, and pronunciation."}]}]}', '2026-08-10 09:10:00.000000', '2026-08-10 09:10:00.000000'),
(2111, 4, 145, 145, 'Academic writing lab rubric', '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Evaluate thesis clarity, paragraph unity, evidence, academic tone, and grammar."}]}]}', '2026-08-10 09:20:00.000000', '2026-08-10 09:20:00.000000');

INSERT IGNORE INTO `question_collections` (`id`, `center_id`, `code`, `name`, `description`, `created_by`, `updated_by`, `created_at`, `updated_at`) VALUES
(2101, 3, 'D7_TOEIC_MINI', 'D7 TOEIC Mini Tests', 'Short TOEIC sets for weekly homework.', 104, 104, '2026-08-10 10:00:00.000000', '2026-08-10 10:00:00.000000'),
(2102, 3, 'D7_IELTS_SPK', 'D7 IELTS Speaking Bank', 'Speaking prompts and short written reflections.', 105, 105, '2026-08-10 10:10:00.000000', '2026-08-10 10:10:00.000000'),
(2111, 4, 'ONLINE_WRITING', 'Online Academic Writing Bank', 'Essay prompts for online writing lab.', 145, 145, '2026-08-10 10:20:00.000000', '2026-08-10 10:20:00.000000');

INSERT IGNORE INTO `questions` (`id`, `center_id`, `collection_id`, `question_code`, `section_code`, `display_order`, `created_by`, `updated_by`, `grading_criteria_id`, `type`, `difficulty`, `points`, `content_json`, `sample_answer_json`, `explanation_json`, `created_at`, `updated_at`) VALUES
(2201, 3, 2101, 'D7-TC-001', 'PART_5', 1, 104, 104, NULL, 'MULTIPLE_CHOICE', 'EASY', 1.00, '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"The new policy will be implemented ------- the end of this quarter."}]}]}', NULL, '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"By introduces a deadline."}]}]}', '2026-08-10 11:00:00.000000', '2026-08-10 11:00:00.000000'),
(2202, 3, 2101, 'D7-TC-002', 'PART_5', 2, 104, 104, NULL, 'MULTIPLE_CHOICE', 'MEDIUM', 1.00, '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Our accounting team has requested a ------- review of the invoice records."}]}]}', NULL, '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Comprehensive is the best adjective for a full review."}]}]}', '2026-08-10 11:05:00.000000', '2026-08-10 11:05:00.000000'),
(2203, 3, 2101, 'D7-TC-003', 'PART_6', 3, 104, 104, NULL, 'MULTIPLE_CHOICE', 'MEDIUM', 1.00, '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Choose the sentence that best completes the customer service email."}]}]}', NULL, '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"The correct sentence keeps the professional tone and gives a clear next step."}]}]}', '2026-08-10 11:10:00.000000', '2026-08-10 11:10:00.000000'),
(2204, 3, 2101, 'D7-TC-004', 'SHORT_RESPONSE', 4, 104, 104, 2101, 'ESSAY', 'MEDIUM', 5.00, '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Write a short response to a client who asks to reschedule a product demo."}]}]}', '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Thank you for letting us know. We can reschedule the demo for Thursday afternoon and will send an updated calendar invitation shortly."}]}]}', '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"A good answer acknowledges the request and proposes a clear new time."}]}]}', '2026-08-10 11:15:00.000000', '2026-08-10 11:15:00.000000'),
(2211, 4, 2111, 'ON-WR-001', 'TASK_2', 1, 145, 145, 2111, 'ESSAY', 'HARD', 10.00, '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Some universities are replacing final exams with continuous assessment. Discuss both views and give your opinion."}]}]}', '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"A balanced essay should compare exam reliability with continuous assessment fairness, then present a clear opinion."}]}]}', '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"The prompt requires both views plus a personal position."}]}]}', '2026-08-10 11:20:00.000000', '2026-08-10 11:20:00.000000'),
(2212, 4, 2111, 'ON-WR-002', 'TASK_1', 2, 145, 145, 2111, 'ESSAY', 'MEDIUM', 8.00, '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Summarize the main trends shown in a chart about online course enrollment from 2022 to 2026."}]}]}', '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"The response should identify the overall upward trend and compare the strongest and weakest categories."}]}]}', '{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":"Task 1 requires overview, key comparisons, and accurate data language."}]}]}', '2026-08-10 11:25:00.000000', '2026-08-10 11:25:00.000000');

INSERT IGNORE INTO `question_options` (`question_id`, `display_order`, `content`, `is_correct`, `created_at`, `updated_at`) VALUES
(2201, 1, 'by', b'1', '2026-08-10 11:00:00.000000', '2026-08-10 11:00:00.000000'),
(2201, 2, 'for', b'0', '2026-08-10 11:00:00.000000', '2026-08-10 11:00:00.000000'),
(2201, 3, 'during', b'0', '2026-08-10 11:00:00.000000', '2026-08-10 11:00:00.000000'),
(2201, 4, 'among', b'0', '2026-08-10 11:00:00.000000', '2026-08-10 11:00:00.000000'),
(2202, 1, 'comprehensive', b'1', '2026-08-10 11:05:00.000000', '2026-08-10 11:05:00.000000'),
(2202, 2, 'comprehend', b'0', '2026-08-10 11:05:00.000000', '2026-08-10 11:05:00.000000'),
(2202, 3, 'comprehension', b'0', '2026-08-10 11:05:00.000000', '2026-08-10 11:05:00.000000'),
(2202, 4, 'comprehensively', b'0', '2026-08-10 11:05:00.000000', '2026-08-10 11:05:00.000000'),
(2203, 1, 'Please confirm which time works best for your team.', b'1', '2026-08-10 11:10:00.000000', '2026-08-10 11:10:00.000000'),
(2203, 2, 'We are not responsible for your schedule.', b'0', '2026-08-10 11:10:00.000000', '2026-08-10 11:10:00.000000'),
(2203, 3, 'Nobody can answer your question today.', b'0', '2026-08-10 11:10:00.000000', '2026-08-10 11:10:00.000000'),
(2203, 4, 'This topic is unrelated to the email.', b'0', '2026-08-10 11:10:00.000000', '2026-08-10 11:10:00.000000');

INSERT IGNORE INTO `assessments` (`id`, `center_id`, `created_by`, `updated_by`, `title`, `description`, `content_json`, `audio_file_id`, `playback_mode`, `status`, `created_at`, `updated_at`) VALUES
(2201, 3, 104, 104, 'D7 TOEIC Mini Test A', 'Short TOEIC mixed-skills homework.', @doc_overview, 2101, 'PRACTICE', 'PUBLISHED', '2026-08-11 08:00:00.000000', '2026-08-11 08:00:00.000000'),
(2211, 4, 145, 145, 'Online Writing Diagnostic', 'Diagnostic writing assessment for the online lab.', @doc_overview, NULL, 'PRACTICE', 'PUBLISHED', '2026-08-11 08:10:00.000000', '2026-08-11 08:10:00.000000');

INSERT IGNORE INTO `assessment_content_blocks` (`id`, `assessment_id`, `position`, `title`, `content_json`, `created_at`, `updated_at`) VALUES
(2201, 2201, 0, 'TOEIC mini test instructions', @doc_overview, '2026-08-11 08:00:00.000000', '2026-08-11 08:00:00.000000'),
(2211, 2211, 0, 'Writing diagnostic instructions', @doc_overview, '2026-08-11 08:10:00.000000', '2026-08-11 08:10:00.000000');

INSERT IGNORE INTO `assessment_items` (`id`, `assessment_id`, `block_id`, `question_id`, `display_order`, `points`, `content_json`, `explanation_json`, `sample_answer_json`, `grading_criteria_id`, `grading_criteria_content_json`, `grading_criteria_name`, `title`, `difficulty`, `question_type`, `created_at`, `updated_at`)
SELECT 2300 + q.display_order, 2201, 2201, q.id, q.display_order, q.points, q.content_json, q.explanation_json, q.sample_answer_json,
       q.grading_criteria_id, gc.content_json, gc.name, q.question_code, q.difficulty, q.type, NOW(6), NOW(6)
FROM `questions` q
LEFT JOIN `grading_criteria` gc ON gc.id = q.grading_criteria_id
WHERE q.id IN (2201,2202,2203,2204);

INSERT IGNORE INTO `assessment_items` (`id`, `assessment_id`, `block_id`, `question_id`, `display_order`, `points`, `content_json`, `explanation_json`, `sample_answer_json`, `grading_criteria_id`, `grading_criteria_content_json`, `grading_criteria_name`, `title`, `difficulty`, `question_type`, `created_at`, `updated_at`)
SELECT 2400 + q.display_order, 2211, 2211, q.id, q.display_order, q.points, q.content_json, q.explanation_json, q.sample_answer_json,
       q.grading_criteria_id, gc.content_json, gc.name, q.question_code, q.difficulty, q.type, NOW(6), NOW(6)
FROM `questions` q
LEFT JOIN `grading_criteria` gc ON gc.id = q.grading_criteria_id
WHERE q.id IN (2211,2212);

INSERT IGNORE INTO `assessment_item_options` (`assessment_item_id`, `display_order`, `content`, `is_correct`, `created_at`, `updated_at`)
SELECT ai.id, qo.display_order, qo.content, qo.is_correct, NOW(6), NOW(6)
FROM `assessment_items` ai
JOIN `question_options` qo ON qo.question_id = ai.question_id
WHERE ai.assessment_id IN (2201,2211);

INSERT IGNORE INTO `assignments` (`id`, `assessment_id`, `assessment_snapshot_at`, `audio_file_id`, `playback_mode`, `center_id`, `created_by`, `updated_by`, `title`, `description`, `content_json`, `status`, `open_at`, `due_at`, `attempt_limit`, `show_score`, `allow_review`, `access_password`, `time_limit_minutes`, `created_at`, `updated_at`) VALUES
(2301, 2201, '2026-08-12 09:00:00.000000', 2101, 'PRACTICE', 3, 104, 104, 'D7 TOEIC Mini Test A - Homework', 'Assigned to D7 TOEIC 900+ active students.', @doc_overview, 'ACTIVE', '2026-08-12 09:00:00.000000', '2026-08-25 23:59:00.000000', 2, b'1', b'1', NULL, 45, '2026-08-12 09:00:00.000000', '2026-08-12 09:00:00.000000'),
(2311, 2211, '2026-08-12 10:00:00.000000', NULL, 'PRACTICE', 4, 145, 145, 'Online Writing Diagnostic - Week 1', 'Assigned to online writing lab students.', @doc_overview, 'ACTIVE', '2026-08-12 10:00:00.000000', '2026-08-24 23:59:00.000000', 1, b'1', b'1', NULL, 60, '2026-08-12 10:00:00.000000', '2026-08-12 10:00:00.000000');

INSERT IGNORE INTO `assignment_content_blocks` (`id`, `assignment_id`, `assessment_block_id`, `position`, `title`, `content_json`, `created_at`, `updated_at`) VALUES
(2301, 2301, 2201, 0, 'TOEIC homework instructions', @doc_overview, '2026-08-12 09:00:00.000000', '2026-08-12 09:00:00.000000'),
(2311, 2311, 2211, 0, 'Writing diagnostic instructions', @doc_overview, '2026-08-12 10:00:00.000000', '2026-08-12 10:00:00.000000');

INSERT IGNORE INTO `assignment_targets` (`assignment_id`, `class_id`, `student_user_id`, `target_type`, `created_at`, `updated_at`) VALUES
(2301, 101, NULL, 'CLASS', '2026-08-12 09:00:00.000000', '2026-08-12 09:00:00.000000'),
(2311, 112, NULL, 'CLASS', '2026-08-12 10:00:00.000000', '2026-08-12 10:00:00.000000');

INSERT IGNORE INTO `assignment_recipients` (`assignment_id`, `class_id`, `student_user_id`, `source_type`, `status`, `assigned_at`, `created_at`, `updated_at`)
SELECT 2301, 101, ce.student_user_id, 'CLASS', 'ASSIGNED', '2026-08-12 09:00:00.000000', '2026-08-12 09:00:00.000000', '2026-08-12 09:00:00.000000'
FROM `class_enrollments` ce
WHERE ce.class_id = 101 AND ce.status = 'ACTIVE';

INSERT IGNORE INTO `assignment_recipients` (`assignment_id`, `class_id`, `student_user_id`, `source_type`, `status`, `assigned_at`, `created_at`, `updated_at`)
SELECT 2311, 112, ce.student_user_id, 'CLASS', 'ASSIGNED', '2026-08-12 10:00:00.000000', '2026-08-12 10:00:00.000000', '2026-08-12 10:00:00.000000'
FROM `class_enrollments` ce
WHERE ce.class_id = 112 AND ce.status = 'ACTIVE';

INSERT IGNORE INTO `assignment_items` (`id`, `assignment_id`, `block_id`, `assessment_item_id`, `display_order`, `points`, `content_json`, `explanation_json`, `sample_answer_json`, `grading_criteria_content_json`, `grading_criteria_name`, `title`, `difficulty`, `question_type`, `created_at`, `updated_at`)
SELECT 23000 + ai.display_order, 2301, 2301, ai.id, ai.display_order, ai.points, ai.content_json, ai.explanation_json,
       ai.sample_answer_json, ai.grading_criteria_content_json, ai.grading_criteria_name, ai.title, ai.difficulty, ai.question_type, NOW(6), NOW(6)
FROM `assessment_items` ai
WHERE ai.assessment_id = 2201;

INSERT IGNORE INTO `assignment_items` (`id`, `assignment_id`, `block_id`, `assessment_item_id`, `display_order`, `points`, `content_json`, `explanation_json`, `sample_answer_json`, `grading_criteria_content_json`, `grading_criteria_name`, `title`, `difficulty`, `question_type`, `created_at`, `updated_at`)
SELECT 23100 + ai.display_order, 2311, 2311, ai.id, ai.display_order, ai.points, ai.content_json, ai.explanation_json,
       ai.sample_answer_json, ai.grading_criteria_content_json, ai.grading_criteria_name, ai.title, ai.difficulty, ai.question_type, NOW(6), NOW(6)
FROM `assessment_items` ai
WHERE ai.assessment_id = 2211;

INSERT IGNORE INTO `assignment_item_options` (`assignment_item_id`, `display_order`, `content`, `is_correct`, `created_at`, `updated_at`)
SELECT assi.id, aio.display_order, aio.content, aio.is_correct, NOW(6), NOW(6)
FROM `assignment_items` assi
JOIN `assessment_item_options` aio ON aio.assessment_item_id = assi.assessment_item_id
WHERE assi.assignment_id IN (2301,2311);

INSERT IGNORE INTO `submission_attempts` (`assignment_recipient_id`, `attempt_number`, `active_attempt_key`, `assignment_title_snapshot`, `status`, `started_at`, `submitted_at`, `last_saved_at`, `expires_at`, `audio_position_seconds`, `audio_completed`, `auto_score`, `max_score`, `created_at`, `updated_at`)
SELECT ar.id, 1, NULL, a.title,
       CASE WHEN ar.student_user_id IN (123,158) THEN 'IN_PROGRESS' ELSE 'SUBMITTED' END,
       '2026-08-13 19:00:00.000000',
       CASE WHEN ar.student_user_id IN (123,158) THEN NULL ELSE '2026-08-13 19:42:00.000000' END,
       '2026-08-13 19:42:00.000000',
       '2026-08-13 20:00:00',
       CASE WHEN a.audio_file_id IS NOT NULL THEN 620 ELSE 0 END,
       CASE WHEN a.audio_file_id IS NOT NULL AND ar.student_user_id NOT IN (123) THEN b'1' ELSE b'0' END,
       NULL,
       NULL,
       '2026-08-13 19:00:00.000000',
       '2026-08-13 19:42:00.000000'
FROM `assignment_recipients` ar
JOIN `assignments` a ON a.id = ar.assignment_id
WHERE (ar.assignment_id = 2301 AND ar.student_user_id IN (121,122,123))
   OR (ar.assignment_id = 2311 AND ar.student_user_id IN (156,157,158));

INSERT IGNORE INTO `submission_answers` (`attempt_id`, `assignment_item_id`, `answer_text`, `auto_score`, `max_score`, `graded_at`, `created_at`, `updated_at`)
SELECT sa.id, ai.id,
       CASE
         WHEN ai.question_type = 'ESSAY' AND ar.assignment_id = 2301 THEN 'Thank you for your message. We can move the product demo to Thursday afternoon and I will send the updated invitation today.'
         WHEN ai.question_type = 'ESSAY' AND ar.assignment_id = 2311 THEN 'Continuous assessment can reduce exam pressure and show progress over time. However, final exams are easier to standardize. I believe universities should combine both approaches.'
         ELSE NULL
       END,
       CASE WHEN ai.question_type = 'MULTIPLE_CHOICE' AND ar.student_user_id IN (121,122) THEN ai.points ELSE NULL END,
       ai.points,
       CASE WHEN ai.question_type = 'MULTIPLE_CHOICE' AND ar.student_user_id IN (121,122) THEN '2026-08-13 19:42:00.000000' ELSE NULL END,
       NOW(6), NOW(6)
FROM `submission_attempts` sa
JOIN `assignment_recipients` ar ON ar.id = sa.assignment_recipient_id
JOIN `assignment_items` ai ON ai.assignment_id = ar.assignment_id
WHERE ar.assignment_id IN (2301,2311);

INSERT IGNORE INTO `submission_answer_options` (`submission_answer_id`, `assignment_item_option_id`, `created_at`)
SELECT ans.id, aio.id, NOW(6)
FROM `submission_answers` ans
JOIN `assignment_items` ai ON ai.id = ans.assignment_item_id AND ai.question_type = 'MULTIPLE_CHOICE'
JOIN `assignment_item_options` aio ON aio.assignment_item_id = ai.id AND aio.is_correct = b'1'
JOIN `submission_attempts` sa ON sa.id = ans.attempt_id
JOIN `assignment_recipients` ar ON ar.id = sa.assignment_recipient_id
WHERE ar.student_user_id IN (121,122);

INSERT IGNORE INTO `ai_grading_jobs` (`id`, `submission_attempt_id`, `requested_by`, `status`, `model_provider`, `model_name`, `prompt_template_version`, `prompt_builder_version`, `system_prompt`, `user_prompt`, `temperature`, `max_tokens`, `active_job_key`, `started_at`, `completed_at`, `failed_at`, `error_message`, `created_at`, `updated_at`)
SELECT 2601, sa.id, 145, 'COMPLETED', 'GEMINI', 'gemini-2.5-flash', 'seed43-writing-v1', 'seed43-builder-v1',
       'You are grading an academic writing response.', 'Grade the seeded writing response with concise feedback.',
       0.10, 2048, NULL, '2026-08-13 19:45:00.000000', '2026-08-13 19:45:12.000000', NULL, NULL,
       '2026-08-13 19:45:00.000000', '2026-08-13 19:45:12.000000'
FROM `submission_attempts` sa
JOIN `assignment_recipients` ar ON ar.id = sa.assignment_recipient_id
WHERE ar.assignment_id = 2311 AND ar.student_user_id = 156;

INSERT IGNORE INTO `ai_grading_results` (`id`, `job_id`, `submission_attempt_id`, `ai_score`, `max_score`, `confidence`, `summary`, `overall_feedback`, `raw_response`, `created_at`, `updated_at`)
SELECT 2601, 2601, j.submission_attempt_id, 14.00, 18.00, 0.9100,
       'Clear position with basic comparison of assessment methods.',
       'Good task coverage. Add more specific evidence and improve paragraph transitions.',
       '{"summary":"Seeded Gemini grading result","score":14}',
       '2026-08-13 19:45:12.000000', '2026-08-13 19:45:12.000000'
FROM `ai_grading_jobs` j
WHERE j.id = 2601;

INSERT IGNORE INTO `ai_grading_item_results` (`result_id`, `submission_answer_id`, `assignment_item_id`, `ai_score`, `max_score`, `confidence`, `feedback`, `rubric_analysis`, `created_at`, `updated_at`)
SELECT 2601, ans.id, ans.assignment_item_id,
       CASE ai.display_order WHEN 1 THEN 8.00 ELSE 6.00 END,
       ans.max_score,
       0.9000,
       'Seeded AI feedback: relevant answer with room for stronger support.',
       'The response addresses the task and uses mostly clear language.',
       '2026-08-13 19:45:12.000000', '2026-08-13 19:45:12.000000'
FROM `submission_answers` ans
JOIN `assignment_items` ai ON ai.id = ans.assignment_item_id
JOIN `submission_attempts` sa ON sa.id = ans.attempt_id
JOIN `assignment_recipients` ar ON ar.id = sa.assignment_recipient_id
WHERE ar.assignment_id = 2311 AND ar.student_user_id = 156;

INSERT IGNORE INTO `teacher_reviews` (`id`, `submission_attempt_id`, `selected_ai_grading_result_id`, `created_by`, `updated_by`, `finalized_by`, `released_by`, `status`, `final_score`, `max_score`, `overall_comment`, `version`, `created_at`, `updated_at`, `finalized_at`, `released_at`)
SELECT 2701, j.submission_attempt_id, 2601, 145, 145, 145, NULL, 'FINALIZED', 15.00, 18.00,
       'Teacher adjusted the score upward for clear organization and relevant examples.',
       1, '2026-08-13 20:00:00.000000', '2026-08-13 20:10:00.000000', '2026-08-13 20:10:00.000000', NULL
FROM `ai_grading_jobs` j
WHERE j.id = 2601;

INSERT IGNORE INTO `teacher_review_items` (`review_id`, `submission_answer_id`, `assignment_item_id`, `display_order_snapshot`, `question_title_snapshot`, `final_score`, `max_score`, `item_comment`, `created_at`, `updated_at`)
SELECT 2701, ans.id, ans.assignment_item_id, ai.display_order, ai.title,
       CASE ai.display_order WHEN 1 THEN 8.50 ELSE 6.50 END,
       ans.max_score,
       'Teacher review seeded for moderation workflow.',
       '2026-08-13 20:00:00.000000', '2026-08-13 20:10:00.000000'
FROM `submission_answers` ans
JOIN `assignment_items` ai ON ai.id = ans.assignment_item_id
JOIN `submission_attempts` sa ON sa.id = ans.attempt_id
JOIN `assignment_recipients` ar ON ar.id = sa.assignment_recipient_id
WHERE ar.assignment_id = 2311 AND ar.student_user_id = 156;

INSERT IGNORE INTO `file_references` (`file_id`, `center_id`, `owner_type`, `owner_id`, `created_at`) VALUES
(2101, 3, 'ASSESSMENT', 2201, '2026-08-11 08:00:00.000000'),
(2101, 3, 'ASSIGNMENT', 2301, '2026-08-12 09:00:00.000000'),
(2102, 4, 'GRADING_CRITERIA', 2111, '2026-08-10 09:20:00.000000');
