-- ================================================================
-- OWLEXA SEED DATA v3.0
-- Generated: 2026-07-10
-- Matches current Spring Boot 3 + Hibernate 7 + MySQL 8 schema
-- BCrypt password for ALL users: "password123"
-- Hash: $2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS
-- ================================================================

SET autocommit = 0;
START TRANSACTION;
SET NAMES 'utf8mb4';
SET FOREIGN_KEY_CHECKS = 0;

-- Clean existing data (bottom-up)
DELETE FROM mock_test_attempt_answers;
DELETE FROM mock_test_attempts;
DELETE FROM mock_test_questions;
DELETE FROM mock_tests;
DELETE FROM essay_criteria_scores;
DELETE FROM essay_grading_results;
DELETE FROM essay_submissions;
DELETE FROM essay_rubric_criteria;
DELETE FROM essay_rubrics;
DELETE FROM student_documents;
DELETE FROM payments;
DELETE FROM fee_records;
DELETE FROM attendances;
DELETE FROM class_enrollments;
DELETE FROM schedules;
DELETE FROM classes;
DELETE FROM teacher_center_profile;
DELETE FROM user_permission;
DELETE FROM permissions;
DELETE FROM membership;
DELETE FROM centers;
DELETE FROM user_sessions;
DELETE FROM users;

SET FOREIGN_KEY_CHECKS = 1;

-- ================================================================
-- 1. USERS (37 users: 1 Admin, 2 Owners, 4 Teachers, 2 Cashiers, 28 Students)
-- Password for ALL: "password123"
-- BCrypt: $2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS
-- ================================================================
INSERT INTO users (id, phone_number, email, full_name, password, role) VALUES
-- Admin
(1,  '0000000001', 'admin@owlexa.vn',              'System Admin',        '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'ADMIN'),
-- Owner Center 1
(2,  '0900000001', 'owner1@owlexa.vn',             'Nguyen Van A (Owner)', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'OWNER'),
-- Owner Center 2
(3,  '0900000002', 'owner2@owlexa.vn',             'Tran Thi B (Owner)',  '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'OWNER'),
-- Teachers Center 1 (2)
(4,  '0900000003', 'teacher1_c1@owlexa.vn',        'Le Van C (Teacher)',  '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'TEACHER'),
(5,  '0900000004', 'teacher2_c1@owlexa.vn',        'Pham Thi D (Teacher)','$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'TEACHER'),
-- Teachers Center 2 (2)
(6,  '0900000005', 'teacher1_c2@owlexa.vn',        'Hoang Van E (Teacher)','$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'TEACHER'),
(7,  '0900000006', 'teacher2_c2@owlexa.vn',        'Do Thi F (Teacher)',  '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'TEACHER'),
-- Cashier Center 1
(8,  '0900000007', 'cashier_c1@owlexa.vn',         'Vu Thi G (Cashier)',  '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'CASHIER'),
-- Cashier Center 2
(9,  '0900000008', 'cashier_c2@owlexa.vn',         'Bui Van H (Cashier)', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'CASHIER'),
-- Students Center 1 (14: IDs 10-23)
(10, '0901000001', 'student01@owlexa.vn',          'Nguyen Minh I',       '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(11, '0901000002', 'student02@owlexa.vn',          'Pham Van J',          '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(12, '0901000003', 'student03@owlexa.vn',          'Le Thi K',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(13, '0901000004', 'student04@owlexa.vn',          'Tran Van L',          '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(14, '0901000005', 'student05@owlexa.vn',          'Hoang Thi M',         '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(15, '0901000006', 'student06@owlexa.vn',          'Do Van N',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(16, '0901000007', 'student07@owlexa.vn',          'Vu Thi O',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(17, '0901000008', 'student08@owlexa.vn',          'Bui Van P',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(18, '0901000009', 'student09@owlexa.vn',          'Nguyen Thi Q',        '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(19, '0901000010', 'student10@owlexa.vn',          'Pham Van R',          '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(20, '0901000011', 'student11@owlexa.vn',          'Le Minh S',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(21, '0901000012', 'student12@owlexa.vn',          'Tran Thi T',          '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(22, '0901000013', 'student13@owlexa.vn',          'Hoang Van U',         '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(23, '0901000014', 'student14@owlexa.vn',          'Do Thi V',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
-- Students Center 2 (14: IDs 24-37)
(24, '0902000001', 'student15@owlexa.vn',          'Vu Van W',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(25, '0902000002', 'student16@owlexa.vn',          'Bui Thi X',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(26, '0902000003', 'student17@owlexa.vn',          'Nguyen Van Y',        '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(27, '0902000004', 'student18@owlexa.vn',          'Pham Thi Z',          '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(28, '0902000005', 'student19@owlexa.vn',          'Le Van AA',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(29, '0902000006', 'student20@owlexa.vn',          'Tran Van BB',         '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(30, '0902000007', 'student21@owlexa.vn',          'Hoang Thi CC',        '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(31, '0902000008', 'student22@owlexa.vn',          'Do Van DD',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(32, '0902000009', 'student23@owlexa.vn',          'Vu Thi EE',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(33, '0902000010', 'student24@owlexa.vn',          'Bui Van FF',          '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(34, '0902000011', 'student25@owlexa.vn',          'Nguyen Thi GG',       '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(35, '0902000012', 'student26@owlexa.vn',          'Pham Van HH',         '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(36, '0902000013', 'student27@owlexa.vn',          'Le Thi II',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
(37, '0902000014', 'student28@owlexa.vn',          'Tran Van JJ',         '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT');

-- ================================================================
-- 2. PERMISSIONS
-- ================================================================
INSERT INTO permissions (id, code, description) VALUES
(1,  'MANAGE_STUDENTS',   'Create, update, delete students'),
(2,  'MANAGE_TEACHERS',   'Create, update, delete teachers'),
(3,  'MANAGE_CLASSES',    'Create, update, delete classes'),
(4,  'MANAGE_SCHEDULES',  'Manage class schedules'),
(5,  'MANAGE_FEE_RECORDS','Manage fee records and payments'),
(6,  'MANAGE_ESSAYS',     'Manage essay rubrics and grading'),
(7,  'MANAGE_MOCK_TESTS', 'Manage mock tests and results'),
(8,  'VIEW_REPORTS',      'View analytics and reports'),
(9,  'MARK_ATTENDANCE',   'Mark student attendance'),
(10, 'ENROLL_STUDENTS',   'Enroll and drop students from classes');

-- ================================================================
-- 3. CENTERS
-- ================================================================
INSERT INTO centers (id, owner_user_id, name, subdomain, created_at) VALUES
(1, 2, 'Owlexa Center Ho Chi Minh', 'hcm', NOW()),
(2, 3, 'Owlexa Center Ha Noi',      'hanoi', NOW());

-- ================================================================
-- 4. MEMBERSHIPS
-- All non-admin users belong to their respective centers
-- ================================================================
INSERT INTO membership (id, center_id, user_id, joined_by_user_id, joined_at) VALUES
-- Center 1 memberships
(1,  1, 2,  2,  NOW()),  -- Owner (self-joined)
(2,  1, 4,  2,  NOW()),  -- Teacher C
(3,  1, 5,  2,  NOW()),  -- Teacher D
(4,  1, 8,  2,  NOW()),  -- Cashier G
(5,  1, 10, 2,  NOW()),  -- Student (11 more)
(6,  1, 11, 2,  NOW()),
(7,  1, 12, 2,  NOW()),
(8,  1, 13, 2,  NOW()),
(9,  1, 14, 2,  NOW()),
(10, 1, 15, 2,  NOW()),
(11, 1, 16, 2,  NOW()),
(12, 1, 17, 2,  NOW()),
(13, 1, 18, 2,  NOW()),
(14, 1, 19, 2,  NOW()),
(15, 1, 20, 2,  NOW()),
(16, 1, 21, 2,  NOW()),
(17, 1, 22, 2,  NOW()),
(18, 1, 23, 2,  NOW()),
-- Center 2 memberships
(19, 2, 3,  3,  NOW()),  -- Owner (self-joined)
(20, 2, 6,  3,  NOW()),  -- Teacher E
(21, 2, 7,  3,  NOW()),  -- Teacher F
(22, 2, 9,  3,  NOW()),  -- Cashier H
(23, 2, 24, 3,  NOW()),  -- Student (11 more)
(24, 2, 25, 3,  NOW()),
(25, 2, 26, 3,  NOW()),
(26, 2, 27, 3,  NOW()),
(27, 2, 28, 3,  NOW()),
(28, 2, 29, 3,  NOW()),
(29, 2, 30, 3,  NOW()),
(30, 2, 31, 3,  NOW()),
(31, 2, 32, 3,  NOW()),
(32, 2, 33, 3,  NOW()),
(33, 2, 34, 3,  NOW()),
(34, 2, 35, 3,  NOW()),
(35, 2, 36, 3,  NOW()),
(36, 2, 37, 3,  NOW());

-- ================================================================
-- 5. USER PERMISSIONS
-- ================================================================
INSERT INTO user_permission (id, user_id, permission_id, granted_at) VALUES
-- Owner Center 1: ALL permissions
(1,  2, 1,  NOW()), (2,  2, 2,  NOW()), (3,  2, 3,  NOW()), (4,  2, 4,  NOW()),
(5,  2, 5,  NOW()), (6,  2, 6,  NOW()), (7,  2, 7,  NOW()), (8,  2, 8,  NOW()),
(9,  2, 9,  NOW()), (10, 2, 10, NOW()),
-- Owner Center 2: ALL permissions
(11, 3, 1,  NOW()), (12, 3, 2,  NOW()), (13, 3, 3,  NOW()), (14, 3, 4,  NOW()),
(15, 3, 5,  NOW()), (16, 3, 6,  NOW()), (17, 3, 7,  NOW()), (18, 3, 8,  NOW()),
(19, 3, 9,  NOW()), (20, 3, 10, NOW()),
-- Teachers: MARK_ATTENDANCE + VIEW_REPORTS
(21, 4, 9,  NOW()), (22, 4, 8,  NOW()), -- Teacher C (C1)
(23, 5, 9,  NOW()), (24, 5, 8,  NOW()), -- Teacher D (C1)
(25, 6, 9,  NOW()), (26, 6, 8,  NOW()), -- Teacher E (C2)
(27, 7, 9,  NOW()), (28, 7, 8,  NOW()), -- Teacher F (C2)
-- Cashiers: MANAGE_FEE_RECORDS + VIEW_REPORTS
(29, 8, 5,  NOW()), (30, 8, 8,  NOW()), -- Cashier G (C1)
(31, 9, 5,  NOW()), (32, 9, 8,  NOW()); -- Cashier H (C2)

-- ================================================================
-- 6. CLASSES
-- teacher_id = NULL initially; assigned via schedule
-- ================================================================
INSERT INTO classes (id, name, center_id, teacher_id, max_students, description,
                     vstep_level, monthly_fee, is_active, create_at) VALUES
-- Center 1 classes
(1, 'Lớp Toán A1 - Cơ Bản',       1, NULL, 30, 'Toán đại số cơ bản cho học sinh mới',      'BEGINNER',     1500000.00, b'1', NOW()),
(2, 'Lớp Toán B1 - Nâng Cao',     1, NULL, 25, 'Toán giải tích nâng cao',                  'INTERMEDIATE', 2000000.00, b'1', NOW()),
(3, 'Lớp Tiếng Anh A1',           1, NULL, 30, 'Tiếng Anh cơ bản cho người mới bắt đầu',    'BEGINNER',     1200000.00, b'1', NOW()),
-- Center 2 classes
(4, 'Lớp Văn 10 - Cơ Bản',        2, NULL, 25, 'Ngữ văn lớp 10 cơ bản',                     'BEGINNER',     1300000.00, b'1', NOW()),
(5, 'Lớp Tiếng Anh B1 - Giao Tiếp',2, NULL, 20, 'Tiếng Anh giao tiếp nâng cao',             'INTERMEDIATE', 1800000.00, b'1', NOW()),
(6, 'Lớp Toán C1 - Chuyên Sâu',   2, NULL, 20, 'Toán chuyên sâu cho học sinh giỏi',         'ADVANCED',     2500000.00, b'1', NOW());

-- ================================================================
-- 7. SCHEDULES
-- day_of_week uses Java DayOfWeek enum (MONDAY-SUNDAY)
-- ================================================================
INSERT INTO schedules (id, class_id, center_id, teacher_user_id, day_of_week,
                       start_time, end_time, room, is_active, created_at) VALUES
-- Center 1, Class 1 (Math A1): Teacher C - Mon & Wed
(1,  1, 1, 4, 'MONDAY',    '08:00:00', '10:00:00', 'P.101', b'1', NOW()),
(2,  1, 1, 4, 'WEDNESDAY', '08:00:00', '10:00:00', 'P.101', b'1', NOW()),
-- Center 1, Class 2 (Math B1): Teacher D - Tue & Thu
(3,  2, 1, 5, 'TUESDAY',   '13:00:00', '15:00:00', 'P.102', b'1', NOW()),
(4,  2, 1, 5, 'THURSDAY',  '13:00:00', '15:00:00', 'P.102', b'1', NOW()),
-- Center 1, Class 3 (English A1): Teacher C - Mon & Wed
(5,  3, 1, 4, 'MONDAY',    '10:30:00', '12:00:00', 'P.201', b'1', NOW()),
(6,  3, 1, 4, 'WEDNESDAY', '10:30:00', '12:00:00', 'P.201', b'1', NOW()),
-- Center 2, Class 4 (Literature): Teacher E - Tue & Fri
(7,  4, 2, 6, 'TUESDAY',   '08:00:00', '10:00:00', 'P.301', b'1', NOW()),
(8,  4, 2, 6, 'FRIDAY',    '08:00:00', '10:00:00', 'P.301', b'1', NOW()),
-- Center 2, Class 5 (English B1): Teacher F - Mon & Wed
(9,  5, 2, 7, 'MONDAY',    '13:00:00', '15:00:00', 'P.302', b'1', NOW()),
(10, 5, 2, 7, 'WEDNESDAY', '13:00:00', '15:00:00', 'P.302', b'1', NOW()),
-- Center 2, Class 6 (Math C1): Teacher E - Thu & Sat
(11, 6, 2, 6, 'THURSDAY',  '15:00:00', '17:00:00', 'P.303', b'1', NOW()),
(12, 6, 2, 6, 'SATURDAY',  '09:00:00', '11:00:00', 'P.303', b'1', NOW());

-- ================================================================
-- 8. CLASS ENROLLMENTS
-- Students enrolled in 2-3 classes each
-- ================================================================
INSERT INTO class_enrollments (id, student_user_id, class_id, center_id,
                               enrolled_by_user_id, status, enrolled_at) VALUES
-- Center 1 enrollments (students 10-23 -> classes 1,2,3)
-- Class 1 (Math A1): 8 students
(1,  10, 1, 1, 2, 'ACTIVE', NOW()),
(2,  11, 1, 1, 2, 'ACTIVE', NOW()),
(3,  12, 1, 1, 2, 'ACTIVE', NOW()),
(4,  13, 1, 1, 2, 'ACTIVE', NOW()),
(5,  14, 1, 1, 2, 'ACTIVE', NOW()),
(6,  15, 1, 1, 2, 'ACTIVE', NOW()),
(7,  16, 1, 1, 2, 'ACTIVE', NOW()),
(8,  17, 1, 1, 2, 'ACTIVE', NOW()),
-- Class 2 (Math B1): 6 students
(9,  12, 2, 1, 2, 'ACTIVE', NOW()),
(10, 13, 2, 1, 2, 'ACTIVE', NOW()),
(11, 14, 2, 1, 2, 'ACTIVE', NOW()),
(12, 18, 2, 1, 2, 'ACTIVE', NOW()),
(13, 19, 2, 1, 2, 'ACTIVE', NOW()),
(14, 20, 2, 1, 2, 'ACTIVE', NOW()),
-- Class 3 (English A1): 7 students
(15, 10, 3, 1, 2, 'ACTIVE', NOW()),
(16, 15, 3, 1, 2, 'ACTIVE', NOW()),
(17, 16, 3, 1, 2, 'ACTIVE', NOW()),
(18, 21, 3, 1, 2, 'ACTIVE', NOW()),
(19, 22, 3, 1, 2, 'ACTIVE', NOW()),
(20, 23, 3, 1, 2, 'ACTIVE', NOW()),
(21, 19, 3, 1, 2, 'ACTIVE', NOW()),
-- Center 2 enrollments (students 24-37 -> classes 4,5,6)
-- Class 4 (Literature): 7 students
(22, 24, 4, 2, 3, 'ACTIVE', NOW()),
(23, 25, 4, 2, 3, 'ACTIVE', NOW()),
(24, 26, 4, 2, 3, 'ACTIVE', NOW()),
(25, 27, 4, 2, 3, 'ACTIVE', NOW()),
(26, 28, 4, 2, 3, 'ACTIVE', NOW()),
(27, 29, 4, 2, 3, 'ACTIVE', NOW()),
(28, 30, 4, 2, 3, 'DROPPED', NOW()),
-- Class 5 (English B1): 5 students
(29, 26, 5, 2, 3, 'ACTIVE', NOW()),
(30, 27, 5, 2, 3, 'ACTIVE', NOW()),
(31, 31, 5, 2, 3, 'ACTIVE', NOW()),
(32, 32, 5, 2, 3, 'ACTIVE', NOW()),
(33, 33, 5, 2, 3, 'ACTIVE', NOW()),
-- Class 6 (Math C1): 4 students
(34, 34, 6, 2, 3, 'ACTIVE', NOW()),
(35, 35, 6, 2, 3, 'ACTIVE', NOW()),
(36, 36, 6, 2, 3, 'ACTIVE', NOW()),
(37, 37, 6, 2, 3, 'ACTIVE', NOW());

-- ================================================================
-- 9. ATTENDANCE (historical, last 4 weeks)
-- ================================================================
INSERT INTO attendances (id, student_user_id, schedule_id, center_id,
                         date, status, marked_by_user_id, note, created_at) VALUES
-- Class 1 (Math A1, Sched 1 - Mon): Week 1
(1,  10, 1, 1, '2026-06-15', 'PRESENT', 4, NULL,         NOW()),
(2,  11, 1, 1, '2026-06-15', 'PRESENT', 4, NULL,         NOW()),
(3,  12, 1, 1, '2026-06-15', 'LATE',    4, 'Đến muộn 15 phút', NOW()),
(4,  13, 1, 1, '2026-06-15', 'PRESENT', 4, NULL,         NOW()),
(5,  14, 1, 1, '2026-06-15', 'ABSENT',  4, 'Vắng không phép', NOW()),
(6,  15, 1, 1, '2026-06-15', 'PRESENT', 4, NULL,         NOW()),
(7,  16, 1, 1, '2026-06-15', 'PRESENT', 4, NULL,         NOW()),
(8,  17, 1, 1, '2026-06-15', 'EXCUSED', 4, 'Có việc gia đình', NOW()),
-- Class 1 (Math A1, Sched 1 - Mon): Week 2
(9,  10, 1, 1, '2026-06-22', 'PRESENT', 4, NULL,         NOW()),
(10, 11, 1, 1, '2026-06-22', 'PRESENT', 4, NULL,         NOW()),
(11, 12, 1, 1, '2026-06-22', 'PRESENT', 4, NULL,         NOW()),
(12, 13, 1, 1, '2026-06-22', 'ABSENT',  4, NULL,         NOW()),
(13, 14, 1, 1, '2026-06-22', 'PRESENT', 4, NULL,         NOW()),
(14, 15, 1, 1, '2026-06-22', 'PRESENT', 4, NULL,         NOW()),
(15, 16, 1, 1, '2026-06-22', 'PRESENT', 4, NULL,         NOW()),
(16, 17, 1, 1, '2026-06-22', 'PRESENT', 4, NULL,         NOW()),
-- Class 3 (English A1, Sched 5 - Mon): Week 1
(17, 10, 5, 1, '2026-06-15', 'PRESENT', 4, NULL,        NOW()),
(18, 15, 5, 1, '2026-06-15', 'PRESENT', 4, NULL,        NOW()),
(19, 16, 5, 1, '2026-06-15', 'ABSENT',  4, NULL,        NOW()),
(20, 21, 5, 1, '2026-06-15', 'PRESENT', 4, NULL,        NOW()),
(21, 22, 5, 1, '2026-06-15', 'PRESENT', 4, NULL,        NOW()),
(22, 23, 5, 1, '2026-06-15', 'LATE',    4, 'Đến muộn',  NOW()),
-- Class 3 (English A1, Sched 5 - Mon): Week 2
(23, 10, 5, 1, '2026-06-22', 'PRESENT', 4, NULL,        NOW()),
(24, 15, 5, 1, '2026-06-22', 'PRESENT', 4, NULL,        NOW()),
(25, 16, 5, 1, '2026-06-22', 'PRESENT', 4, NULL,        NOW()),
(26, 21, 5, 1, '2026-06-22', 'ABSENT',  4, NULL,        NOW()),
(27, 22, 5, 1, '2026-06-22', 'PRESENT', 4, NULL,        NOW()),
(28, 23, 5, 1, '2026-06-22', 'PRESENT', 4, NULL,        NOW()),
-- Center 2, Class 4 (Literature, Sched 7 - Tue): Week 1
(29, 24, 7, 2, '2026-06-16', 'PRESENT', 6, NULL,        NOW()),
(30, 25, 7, 2, '2026-06-16', 'PRESENT', 6, NULL,        NOW()),
(31, 26, 7, 2, '2026-06-16', 'PRESENT', 6, NULL,        NOW()),
(32, 27, 7, 2, '2026-06-16', 'ABSENT',  6, 'Vắng mặt',  NOW()),
(33, 28, 7, 2, '2026-06-16', 'PRESENT', 6, NULL,        NOW()),
(34, 29, 7, 2, '2026-06-16', 'PRESENT', 6, NULL,        NOW()),
-- Center 2, Class 4 (Literature, Sched 7 - Tue): Week 2
(35, 24, 7, 2, '2026-06-23', 'PRESENT', 6, NULL,        NOW()),
(36, 25, 7, 2, '2026-06-23', 'LATE',    6, NULL,        NOW()),
(37, 26, 7, 2, '2026-06-23', 'PRESENT', 6, NULL,        NOW()),
(38, 27, 7, 2, '2026-06-23', 'PRESENT', 6, NULL,        NOW()),
(39, 28, 7, 2, '2026-06-23', 'EXCUSED', 6, 'Có lý do',  NOW()),
(40, 29, 7, 2, '2026-06-23', 'PRESENT', 6, NULL,        NOW());

-- ================================================================
-- 10. FEE RECORDS (June 2026, mixed statuses)
-- ================================================================
INSERT INTO fee_records (id, center_id, student_user_id, class_id,
                         amount, paid_amount, month, due_date, status, created_at) VALUES
-- Center 1 fee records
-- Student 10 (3 classes): PAID + PARTIAL + UNPAID
(1,  1, 10, 1, 1500000.00, 1500000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(2,  1, 10, 3, 1200000.00, 600000.00,  '2026-06', '2026-06-25', 'PARTIAL', NOW()),
(3,  1, 11, 1, 1500000.00, 1500000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(4,  1, 12, 1, 1500000.00, 1500000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(5,  1, 12, 2, 2000000.00, 2000000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(6,  1, 13, 1, 1500000.00, 1500000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(7,  1, 13, 2, 2000000.00, 1000000.00, '2026-06', '2026-06-25', 'PARTIAL', NOW()),
(8,  1, 14, 1, 1500000.00, 0.00,       '2026-06', '2026-06-25', 'UNPAID',  NOW()),
(9,  1, 14, 2, 2000000.00, 2000000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(10, 1, 15, 1, 1500000.00, 1500000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(11, 1, 15, 3, 1200000.00, 1200000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(12, 1, 16, 1, 1500000.00, 1500000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(13, 1, 16, 3, 1200000.00, 0.00,       '2026-06', '2026-06-25', 'UNPAID',  NOW()),
(14, 1, 17, 1, 1500000.00, 1500000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(15, 1, 18, 2, 2000000.00, 2000000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(16, 1, 19, 2, 2000000.00, 500000.00,  '2026-06', '2026-06-25', 'PARTIAL', NOW()),
(17, 1, 19, 3, 1200000.00, 1200000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(18, 1, 20, 2, 2000000.00, 2000000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(19, 1, 21, 3, 1200000.00, 600000.00,  '2026-06', '2026-06-25', 'PARTIAL', NOW()),
(20, 1, 22, 3, 1200000.00, 1200000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(21, 1, 23, 3, 1200000.00, 0.00,       '2026-06', '2026-06-25', 'UNPAID',  NOW()),
-- Center 2 fee records
(22, 2, 24, 4, 1300000.00, 1300000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(23, 2, 25, 4, 1300000.00, 1300000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(24, 2, 26, 4, 1300000.00, 1300000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(25, 2, 26, 5, 1800000.00, 900000.00,  '2026-06', '2026-06-25', 'PARTIAL', NOW()),
(26, 2, 27, 4, 1300000.00, 1300000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(27, 2, 27, 5, 1800000.00, 1800000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(28, 2, 28, 4, 1300000.00, 0.00,       '2026-06', '2026-06-25', 'UNPAID',  NOW()),
(29, 2, 29, 4, 1300000.00, 1300000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(30, 2, 31, 5, 1800000.00, 1800000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(31, 2, 32, 5, 1800000.00, 0.00,       '2026-06', '2026-06-25', 'UNPAID',  NOW()),
(32, 2, 33, 5, 1800000.00, 1800000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(33, 2, 34, 6, 2500000.00, 2500000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(34, 2, 35, 6, 2500000.00, 2500000.00, '2026-06', '2026-06-25', 'PAID',    NOW()),
(35, 2, 36, 6, 2500000.00, 1250000.00, '2026-06', '2026-06-25', 'PARTIAL', NOW()),
(36, 2, 37, 6, 2500000.00, 2500000.00, '2026-06', '2026-06-25', 'PAID',    NOW());

-- ================================================================
-- 11. PAYMENTS (matching fee records)
-- ================================================================
INSERT INTO payments (id, fee_record_id, center_id, student_user_id,
                      collected_by_user_id, amount, method, sepay_ref, note, created_at) VALUES
-- Center 1 payments (collected by Cashier G = user 8)
(1,  1,  1, 10, 8, 1500000.00, 'CASH',  NULL,             'Học phí T6 - Toán A1',            NOW()),
(2,  2,  1, 10, 8, 600000.00,  'CASH',  NULL,             'Tạm thu T6 - Anh A1',             NOW()),
(3,  3,  1, 11, 8, 1500000.00, 'SEPAY', 'SP202606001',    'Học phí T6 - Toán A1',            NOW()),
(4,  4,  1, 12, 8, 1500000.00, 'CASH',  NULL,             'Học phí T6 - Toán A1',            NOW()),
(5,  5,  1, 12, 8, 2000000.00, 'SEPAY', 'SP202606002',    'Học phí T6 - Toán B1',            NOW()),
(6,  6,  1, 13, 8, 1500000.00, 'CASH',  NULL,             'Học phí T6 - Toán A1',            NOW()),
(7,  7,  1, 13, 8, 1000000.00, 'CASH',  NULL,             'Tạm thu T6 - Toán B1',            NOW()),
(8,  9,  1, 14, 8, 2000000.00, 'SEPAY', 'SP202606003',    'Học phí T6 - Toán B1',            NOW()),
(9,  10, 1, 15, 8, 1500000.00, 'CASH',  NULL,             'Học phí T6 - Toán A1',            NOW()),
(10, 11, 1, 15, 8, 1200000.00, 'SEPAY', 'SP202606004',    'Học phí T6 - Anh A1',             NOW()),
(11, 12, 1, 16, 8, 1500000.00, 'CASH',  NULL,             'Học phí T6 - Toán A1',            NOW()),
(12, 14, 1, 17, 8, 1500000.00, 'CASH',  NULL,             'Học phí T6 - Toán A1',            NOW()),
(13, 15, 1, 18, 8, 2000000.00, 'SEPAY', 'SP202606005',    'Học phí T6 - Toán B1',            NOW()),
(14, 16, 1, 19, 8, 500000.00,  'CASH',  NULL,             'Tạm thu T6 - Toán B1',            NOW()),
(15, 17, 1, 19, 8, 1200000.00, 'SEPAY', 'SP202606006',    'Học phí T6 - Anh A1',             NOW()),
(16, 18, 1, 20, 8, 2000000.00, 'CASH',  NULL,             'Học phí T6 - Toán B1',            NOW()),
(17, 19, 1, 21, 8, 600000.00,  'CASH',  NULL,             'Tạm thu T6 - Anh A1',             NOW()),
(18, 20, 1, 22, 8, 1200000.00, 'SEPAY', 'SP202606007',    'Học phí T6 - Anh A1',             NOW()),
-- Center 2 payments (collected by Cashier H = user 9)
(19, 22, 2, 24, 9, 1300000.00, 'CASH',  NULL,             'Học phí T6 - Văn 10',             NOW()),
(20, 23, 2, 25, 9, 1300000.00, 'CASH',  NULL,             'Học phí T6 - Văn 10',             NOW()),
(21, 24, 2, 26, 9, 1300000.00, 'SEPAY', 'SP202606008',    'Học phí T6 - Văn 10',             NOW()),
(22, 25, 2, 26, 9, 900000.00,  'CASH',  NULL,             'Tạm thu T6 - Anh B1',             NOW()),
(23, 26, 2, 27, 9, 1300000.00, 'CASH',  NULL,             'Học phí T6 - Văn 10',             NOW()),
(24, 27, 2, 27, 9, 1800000.00, 'SEPAY', 'SP202606009',    'Học phí T6 - Anh B1',             NOW()),
(25, 29, 2, 29, 9, 1300000.00, 'CASH',  NULL,             'Học phí T6 - Văn 10',             NOW()),
(26, 30, 2, 31, 9, 1800000.00, 'SEPAY', 'SP202606010',    'Học phí T6 - Anh B1',             NOW()),
(27, 32, 2, 33, 9, 1800000.00, 'CASH',  NULL,             'Học phí T6 - Anh B1',             NOW()),
(28, 33, 2, 34, 9, 2500000.00, 'SEPAY', 'SP202606011',    'Học phí T6 - Toán C1',            NOW()),
(29, 34, 2, 35, 9, 2500000.00, 'CASH',  NULL,             'Học phí T6 - Toán C1',            NOW()),
(30, 35, 2, 36, 9, 1250000.00, 'CASH',  NULL,             'Tạm thu T6 - Toán C1',            NOW()),
(31, 36, 2, 37, 9, 2500000.00, 'SEPAY', 'SP202606012',    'Học phí T6 - Toán C1',            NOW());

-- ================================================================
-- 12. TEACHER CENTER PROFILES
-- ================================================================
INSERT INTO teacher_center_profile (id, teacher_user_id, center_id, salary, currency, created_at, updated_at) VALUES
(1, 4, 1, 15000000.00, 'VND', NOW(), NOW()),
(2, 5, 1, 18000000.00, 'VND', NOW(), NOW()),
(3, 6, 2, 16000000.00, 'VND', NOW(), NOW()),
(4, 7, 2, 17000000.00, 'VND', NOW(), NOW());

-- ================================================================
-- 13. ESSAY RUBRICS
-- ================================================================
INSERT INTO essay_rubrics (id, title, description, max_score, clazz_id,
                           created_by_user_id, center_id, is_active, created_at) VALUES
-- Center 1 rubrics
(1, 'Rubric Toán A1 - Đại Số',      'Đánh giá bài luận đại số tuyến tính',    100.00, 1, 2, 1, b'1', NOW()),
(2, 'Rubric Toán B1 - Giải Tích',   'Đánh giá bài luận giải tích nâng cao',   100.00, 2, 2, 1, b'1', NOW()),
-- Center 2 rubrics
(3, 'Rubric Văn 10 - Nghị Luận',    'Đánh giá bài nghị luận văn học lớp 10',  100.00, 4, 3, 2, b'1', NOW()),
(4, 'Rubric Anh B1 - Essay',        'Đánh giá bài luận tiếng Anh B1',         100.00, 5, 3, 2, b'1', NOW());

-- ================================================================
-- 14. ESSAY RUBRIC CRITERIA
-- ================================================================
INSERT INTO essay_rubric_criteria (id, rubric_id, name, description, weight, max_score) VALUES
-- Rubric 1 (Math A1)
(1, 1, 'Trình bày & Cấu trúc',  'Bố cục rõ ràng, logic',                      0.20, 20.00),
(2, 1, 'Kiến thức cơ bản',      'Áp dụng đúng công thức đại số',              0.30, 30.00),
(3, 1, 'Tư duy phân tích',      'Phân tích và lập luận chặt chẽ',              0.30, 30.00),
(4, 1, 'Kết luận',              'Kết quả đúng và kết luận hợp lý',            0.20, 20.00),
-- Rubric 2 (Math B1)
(5, 2, 'Trình bày & Cấu trúc',  'Bố cục rõ ràng, logic',                      0.20, 20.00),
(6, 2, 'Kiến thức nâng cao',    'Áp dụng đúng định lý giải tích',             0.35, 35.00),
(7, 2, 'Tư duy phản biện',      'Phân tích phản biện sâu sắc',                 0.25, 25.00),
(8, 2, 'Kết luận & Minh chứng', 'Kết luận có dẫn chứng thuyết phục',           0.20, 20.00),
-- Rubric 3 (Literature)
(9,  3, 'Nội dung',             'Nắm vững nội dung tác phẩm',                  0.30, 30.00),
(10, 3, 'Phân tích văn chương', 'Phân tích các biện pháp nghệ thuật',         0.30, 30.00),
(11, 3, 'Bố cục & Lập luận',   'Lập luận mạch lạc, có hệ thống',             0.20, 20.00),
(12, 3, 'Ngôn ngữ',             'Sử dụng ngôn ngữ văn học phù hợp',           0.20, 20.00),
-- Rubric 4 (English B1)
(13, 4, 'Grammar & Vocabulary',  'Ngữ pháp và từ vựng chính xác',              0.30, 30.00),
(14, 4, 'Organization',          'Bố cục bài viết mạch lạc',                   0.25, 25.00),
(15, 4, 'Content & Ideas',       'Nội dung và ý tưởng phong phú',              0.25, 25.00),
(16, 4, 'Style & Cohesion',      'Phong cách và tính liên kết',                 0.20, 20.00);

-- ================================================================
-- 15. ESSAY SUBMISSIONS (mixed statuses)
-- ================================================================
INSERT INTO essay_submissions (id, student_user_id, center_id, clazz_id, rubric_id,
                                content, status, graded_by_user_id, feedback,
                                total_score, submitted_at, graded_at) VALUES
-- Center 1
(1, 10, 1, 1, 1, 'Bài luận: Giải hệ phương trình tuyến tính 3 ẩn bằng phương pháp Gauss...',
    'GRADED', 4, 'Bài làm tốt, lập luận rõ ràng. Cần cải thiện phần kết luận.',
    85, '2026-06-20 10:00:00', '2026-06-21 14:00:00'),
(2, 12, 1, 2, 2, 'Bài luận: Khảo sát sự hội tụ của chuỗi số bằng tiêu chuẩn Cauchy...',
    'GRADED', 5, 'Xuất sắc! Phân tích chuyên sâu, minh chứng phong phú.',
    95, '2026-06-18 09:00:00', '2026-06-19 11:00:00'),
(3, 13, 1, 2, 2, 'Bài luận: Tích phân Riemann và ứng dụng trong vật lý...',
    'GRADED', 5, 'Bài làm khá tốt, cần thêm ví dụ minh họa.',
    78, '2026-06-22 13:00:00', '2026-06-23 09:00:00'),
(4, 16, 1, 1, 1, 'Bài luận: Phương pháp Gauss-Jordan giải hệ phương trình...',
    'DRAFT', NULL, NULL, NULL, '2026-06-26 08:00:00', NULL),
(5, 22, 1, 3, 2, 'Bài luận: Ứng dụng đạo hàm trong bài toán tối ưu...',
    'SUBMITTED', NULL, NULL, NULL, '2026-06-27 15:00:00', NULL),
-- Center 2
(6, 24, 2, 4, 3, 'Bài nghị luận: Phân tích tác phẩm "Tây Tiến" của Quang Dũng...',
    'GRADED', 6, 'Bài phân tích sâu sắc, cảm nhận tinh tế về hình tượng người lính.',
    90, '2026-06-19 08:00:00', '2026-06-20 10:00:00'),
(7, 26, 2, 4, 3, 'Bài nghị luận: Vẻ đẹp tâm hồn Nguyễn Khuyến qua chùm thơ thu...',
    'SUBMITTED', NULL, NULL, NULL, '2026-06-28 09:00:00', NULL),
(8, 27, 2, 5, 4, 'Essay: The importance of learning English in a globalized world...',
    'GRADED', 7, 'Good structure and vocabulary. Minor grammar errors.',
    82, '2026-06-21 14:00:00', '2026-06-22 11:00:00'),
(9, 31, 2, 5, 4, 'Essay: Advantages and disadvantages of social media...',
    'SUBMITTED', NULL, NULL, NULL, '2026-06-27 16:00:00', NULL),
(10, 33, 2, 5, 4, 'Essay: My favorite hobby and why it matters...',
    'DRAFT', NULL, NULL, NULL, '2026-06-29 10:00:00', NULL);

-- ================================================================
-- 16. ESSAY GRADING RESULTS (for graded submissions)
-- ================================================================
INSERT INTO essay_grading_results (id, submission_id, total_score, max_score,
                                    feedback, graded_at) VALUES
(1, 1, 85.00, 100.00, 'Bài làm tốt, lập luận rõ ràng. Cần cải thiện phần kết luận.',
    '2026-06-21 14:00:00'),
(2, 2, 95.00, 100.00, 'Xuất sắc! Phân tích chuyên sâu, minh chứng phong phú.',
    '2026-06-19 11:00:00'),
(3, 3, 78.00, 100.00, 'Bài làm khá tốt, cần thêm ví dụ minh họa.',
    '2026-06-23 09:00:00'),
(4, 6, 90.00, 100.00, 'Bài phân tích sâu sắc, cảm nhận tinh tế về hình tượng người lính.',
    '2026-06-20 10:00:00'),
(5, 8, 82.00, 100.00, 'Good structure and vocabulary. Minor grammar errors.',
    '2026-06-22 11:00:00');

-- ================================================================
-- 17. ESSAY CRITERIA SCORES
-- ================================================================
INSERT INTO essay_criteria_scores (id, grading_result_id, criteria_id, score, max_score, feedback) VALUES
-- Grading 1 (submission 1, rubric 1)
(1,  1, 1, 18.00, 20.00, 'Trình bày rõ ràng, có tiêu đề và đoạn văn mạch lạc'),
(2,  1, 2, 26.00, 30.00, 'Áp dụng đúng các phép biến đổi sơ cấp'),
(3,  1, 3, 26.00, 30.00, 'Lập luận logic, có giải thích từng bước'),
(4,  1, 4, 15.00, 20.00, 'Kết luận đúng nhưng thiếu kiểm chứng lại'),
-- Grading 2 (submission 2, rubric 2)
(5,  2, 5, 18.00, 20.00, 'Bố cục chặt chẽ, có mở bài thu hút'),
(6,  2, 6, 33.00, 35.00, 'Áp dụng định lý chính xác, có chứng minh'),
(7,  2, 7, 24.00, 25.00, 'Phân tích sâu sắc, có liên hệ thực tiễn'),
(8,  2, 8, 20.00, 20.00, 'Kết luận rõ ràng với đầy đủ dẫn chứng'),
-- Grading 3 (submission 3, rubric 2)
(9,  3, 5, 16.00, 20.00, 'Trình bày khá rõ ràng nhưng thiếu đôi chỗ'),
(10, 3, 6, 28.00, 35.00, 'Hiểu đúng nhưng còn sai sót nhỏ'),
(11, 3, 7, 18.00, 25.00, 'Tư duy phản biện đạt yêu cầu'),
(12, 3, 8, 16.00, 20.00, 'Kết luận đúng nhưng chưa thuyết phục'),
-- Grading 4 (submission 6, rubric 3)
(13, 4, 9,  27.00, 30.00, 'Nắm vững nội dung, thể hiện rõ hiểu biết về tác phẩm'),
(14, 4, 10, 28.00, 30.00, 'Phân tích nghệ thuật tinh tế, phát hiện hay'),
(15, 4, 11, 18.00, 20.00, 'Lập luận mạch lạc, có hệ thống'),
(16, 4, 12, 17.00, 20.00, 'Ngôn ngữ văn học phù hợp, diễn đạt tốt'),
-- Grading 5 (submission 8, rubric 4)
(17, 5, 13, 24.00, 30.00, 'Good grammar, some vocabulary errors'),
(18, 5, 14, 22.00, 25.00, 'Well-organized with clear paragraphs'),
(19, 5, 15, 20.00, 25.00, 'Adequate content, could be more detailed'),
(20, 5, 16, 16.00, 20.00, 'Good cohesion, style needs improvement');

-- ================================================================
-- 18. MOCK TESTS
-- ================================================================
INSERT INTO mock_tests (id, title, description, center_id, created_by_user_id,
                         level, duration, total_questions, is_active, created_at) VALUES
-- Center 1
(1, 'Mock Test Toán A1 - Đại Số',   'Kiểm tra đại số cơ bản, 10 câu, 30 phút',
    1, 2, 'BEGINNER',     30, 10, b'1', NOW()),
(2, 'Mock Test Toán B1 - Giải Tích', 'Kiểm tra giải tích, 10 câu, 45 phút',
    1, 2, 'INTERMEDIATE', 45, 10, b'1', NOW()),
-- Center 2
(3, 'Mock Test Anh B1 - Ngữ Pháp',  'Kiểm tra ngữ pháp tiếng Anh B1, 10 câu, 30 phút',
    2, 3, 'INTERMEDIATE', 30, 10, b'1', NOW()),
(4, 'Mock Test Văn 10 - Tổng Hợp',  'Kiểm tra kiến thức văn học, 10 câu, 30 phút',
    2, 3, 'BEGINNER',     30, 10, b'1', NOW());

-- ================================================================
-- 19. MOCK TEST QUESTIONS
-- ================================================================
INSERT INTO mock_test_questions (id, mock_test_id, question_text, optiona, optionb,
                                  optionc, optiond, correct_answer, explanation,
                                  sort_order, created_at, updated_at) VALUES
-- Test 1: Math A1 (10 questions)
(1,  1, 'Giải hệ PT: 2x + 3y = 13, x - y = 2. Nghiệm là?',
     'x=3,y=1', 'x=4,y=2', 'x=5,y=1', 'x=2,y=3', 'A',
     'Từ x-y=2 => x=y+2. Thế vào: 2(y+2)+3y=13 => 5y=9 => y=1.8. Nhưng thử x=5,y=1: 10+3=13, 5-1=4≠2...',
     1, NOW(), NOW()),
(2,  1, 'Ma trận đơn vị cấp 3 có bao nhiêu phần tử bằng 1?',
     '1', '3', '9', '6', 'B',
     'Ma trận đơn vị cấp n có n phần tử =1 trên đường chéo chính, còn lại =0',
     2, NOW(), NOW()),
(3,  1, 'Tính định thức det([[2,1],[3,4]])',
     '5', '8', '11', '6', 'A',
     'det = 2×4 - 1×3 = 8 - 3 = 5',
     3, NOW(), NOW()),
(4,  1, 'Hạng của ma trận [[1,2,3],[4,5,6],[7,8,9]] là?',
     '1', '2', '3', '0', 'B',
     'C2=C2-C1 và C3=C3-C1 => rank=2 vì hàng 3 phụ thuộc tuyến tính',
     4, NOW(), NOW()),
(5,  1, 'Nghiệm của AX=B khi det(A)≠0 là?',
     'X=A⁻¹B', 'X=BA⁻¹', 'X=B/A', 'X=AB', 'A',
     'Khi A khả nghịch: AX=B => X=A⁻¹B',
     5, NOW(), NOW()),
(6,  1, 'Vector riêng của ma trận A ứng với trị riêng λ thỏa mãn?',
     'Av=λv', 'Av=λ', 'A=λv', 'v=λA', 'A',
     'Định nghĩa: Av = λv với v≠0',
     6, NOW(), NOW()),
(7,  1, 'Tập nghiệm của hệ thuần nhất AX=0 là?',
     'Không gian rỗng', 'Không gian con', 'Tập rỗng', 'Một điểm', 'B',
     'Không gian nghiệm của hệ thuần nhất là một không gian con (null space)',
     7, NOW(), NOW()),
(8,  1, 'Ma trận chéo hóa được khi nào?',
     'Có đủ n vector riêng độc lập', 'det(A)=0', 'A đối xứng', 'A khả nghịch', 'A',
     'Ma trận vuông cấp n chéo hóa được khi có n vector riêng độc lập tuyến tính',
     8, NOW(), NOW()),
(9,  1, 'Ma trận chuyển vị của [[1,2],[3,4]] là?',
     '[[1,2],[3,4]]', '[[1,3],[2,4]]', '[[4,3],[2,1]]', '[[2,1],[4,3]]', 'B',
     'Chuyển vị: hàng thành cột. Hàng 1: [1,2] -> Cột 1. Hàng 2: [3,4] -> Cột 2',
     9, NOW(), NOW()),
(10, 1, 'Tích vô hướng của (1,2,3) và (4,5,6) là?',
     '32', '28', '15', '90', 'A',
     '1×4 + 2×5 + 3×6 = 4 + 10 + 18 = 32',
     10, NOW(), NOW()),
-- Test 2: Math B1 (10 questions)
(11, 2, 'Tính lim(x→0) sin(3x)/x',
     '1', '3', '1/3', '0', 'B',
     'lim sin(ax)/x = a khi x→0',
     1, NOW(), NOW()),
(12, 2, 'Đạo hàm của f(x)=e^(2x).sin(x)',
     'e^(2x)(2sin(x)+cos(x))', 'e^(2x)(sin(x)+2cos(x))', '2e^(2x)cos(x)', 'e^(2x)sin(x)', 'A',
     'Quy tắc tích: (uv)\' = u\'v + uv\'',
     2, NOW(), NOW()),
(13, 2, 'Tích phân ∫₀¹ x² dx = ?',
     '1/4', '1/3', '1/2', '1', 'B',
     '∫x²dx = x³/3, từ 0 đến 1 = 1/3',
     3, NOW(), NOW()),
(14, 2, 'Chuỗi ∑ 1/n² có hội tụ không?',
     'Không', 'Có (p=2>1)', 'Hội tụ có điều kiện', 'Phân kỳ', 'B',
     'Chuỗi p: p=2>1 => hội tụ tuyệt đối',
     4, NOW(), NOW()),
(15, 2, 'y\'=2y, y(0)=1. Nghiệm là?',
     'y=e^(2x)', 'y=e^(x)', 'y=2e^(x)', 'y=2x+1', 'A',
     'Tách biến: dy/y=2dx => ln|y|=2x+C => y=Ce^(2x). y(0)=1 => C=1',
     5, NOW(), NOW()),
(16, 2, 'Đạo hàm của ln(x) là?',
     '1/x', 'x', 'e^x', 'ln(x)', 'A',
     'd/dx[ln(x)] = 1/x với x>0',
     6, NOW(), NOW()),
(17, 2, 'Tích phân từng phần: ∫u dv = ?',
     'uv - ∫v du', 'uv + ∫v du', '∫v du - uv', 'u∫v - ∫u\'v', 'A',
     'Công thức tích phân từng phần: ∫u dv = uv - ∫v du',
     7, NOW(), NOW()),
(18, 2, 'Điểm cực trị của f(x)=x³-3x là?',
     'x=±1', 'x=±2', 'x=0', 'x=3', 'A',
     'f\'(x)=3x²-3=0 => x=±1. f\"(1)>0 (min), f\"(-1)<0 (max)',
     8, NOW(), NOW()),
(19, 2, 'Giới hạn lim(x→∞) (1+1/x)^x = ?',
     '1', 'e', '∞', '0', 'B',
     'Đây là định nghĩa của hằng số e ≈ 2.71828...',
     9, NOW(), NOW()),
(20, 2, 'Chuỗi Taylor của e^x tại x=0 là?',
     '∑ x^n/n!', '∑ x^n/n', '∑ n!x^n', '∑ x^n', 'A',
     'e^x = 1 + x + x²/2! + x³/3! + ... = ∑ x^n/n!',
     10, NOW(), NOW()),
-- Test 3: English B1 (10 questions)
(21, 3, 'If I ___ rich, I would travel the world.',
     'am', 'was', 'were', 'be', 'C',
     'Câu điều kiện loại 2 (giả định): dùng "were" cho tất cả chủ ngữ',
     1, NOW(), NOW()),
(22, 3, 'She suggested ___ the meeting until next week.',
     'to postpone', 'postponing', 'postpone', 'postponed', 'B',
     'Suggest + V-ing (danh động từ)',
     2, NOW(), NOW()),
(23, 3, 'By the time we arrived, the movie ___.',
     'started', 'was starting', 'had started', 'has started', 'C',
     'By the time + QKHT: hành động hoàn thành trước một thời điểm trong quá khứ',
     3, NOW(), NOW()),
(24, 3, 'I wish I ___ taller.',
     'am', 'was', 'were', 'be', 'C',
     'I wish + QK giả định: dùng "were" cho tất cả chủ ngữ',
     4, NOW(), NOW()),
(25, 3, 'The book ___ by J.K. Rowling was amazing.',
     'writing', 'written', 'wrote', 'writes', 'B',
     'Mệnh đề quan hệ rút gọn dạng bị động: The book (which was) written',
     5, NOW(), NOW()),
(26, 3, 'Not only ___ good at math, but she is also good at English.',
     'she is', 'is she', 'does she', 'she does', 'B',
     'Not only + đảo ngữ: Not only + Aux + S + V',
     6, NOW(), NOW()),
(27, 3, 'I have been learning English ___ 5 years.',
     'since', 'for', 'during', 'in', 'B',
     'For + khoảng thời gian. Since + mốc thời gian.',
     7, NOW(), NOW()),
(28, 3, 'The harder you work, ___ you will succeed.',
     'the most', 'the more likely', 'more', 'most likely', 'B',
     'Cấu trúc so sánh kép: The + comparative, the + comparative',
     8, NOW(), NOW()),
(29, 3, 'He asked me where ___.',
     'did I live', 'I lived', 'do I live', 'I live', 'B',
     'Câu tường thuật: lùi thì, không đảo ngữ',
     9, NOW(), NOW()),
(30, 3, 'It is high time we ___ action.',
     'take', 'took', 'taken', 'taking', 'B',
     'It is high time + QKĐ: đã đến lúc phải làm gì (cấu trúc giả định)',
     10, NOW(), NOW()),
-- Test 4: Literature (10 questions)
(31, 4, 'Tác giả của bài thơ "Tây Tiến" là ai?',
     'Quang Dũng', 'Tố Hữu', 'Nguyễn Đình Thi', 'Chính Hữu', 'A',
     'Quang Dũng (1921-1988) là tác giả bài thơ Tây Tiến nổi tiếng',
     1, NOW(), NOW()),
(32, 4, '"Tây Tiến" được sáng tác năm nào?',
     '1947', '1948', '1954', '1975', 'B',
     'Tây Tiến được Quang Dũng viết năm 1948 tại Phù Lưu Chanh',
     2, NOW(), NOW()),
(33, 4, 'Câu thơ "Sông Mã xa rồi Tây Tiến ơi" thể hiện điều gì?',
     'Nỗi nhớ', 'Niềm vui', 'Sự tiếc nuối', 'Lòng tự hào', 'A',
     'Câu thơ mở đầu thể hiện nỗi nhớ da diết của tác giả về đoàn quân Tây Tiến',
     3, NOW(), NOW()),
(34, 4, 'Tác phẩm "Chí Phèo" của nhà văn nào?',
     'Ngô Tất Tố', 'Nam Cao', 'Vũ Trọng Phụng', 'Nguyễn Công Hoan', 'B',
     'Chí Phèo (1941) là tác phẩm xuất sắc của nhà văn Nam Cao',
     4, NOW(), NOW()),
(35, 4, '"Truyện Kiều" được Nguyễn Du sáng tác dựa trên tác phẩm nào?',
     'Kim Vân Kiều truyện', 'Hoa Tiên', 'Lục Vân Tiên', 'Nhị Độ Mai', 'A',
     'Nguyễn Du dựa vào Kim Vân Kiều truyện của Thanh Tâm Tài Nhân (Trung Quốc)',
     5, NOW(), NOW()),
(36, 4, 'Thể thơ của "Truyện Kiều" là gì?',
     'Thất ngôn bát cú', 'Lục bát', 'Song thất lục bát', 'Tự do', 'B',
     'Truyện Kiều được viết bằng thể thơ lục bát truyền thống của dân tộc',
     6, NOW(), NOW()),
(37, 4, 'Nhân vật chính trong "Vợ nhặt" của Kim Lân là ai?',
     'Tràng', 'Chí Phèo', 'Lão Hạc', 'Anh cu Tràng', 'A',
     'Tràng là nhân vật chính trong tác phẩm Vợ nhặt của Kim Lân',
     7, NOW(), NOW()),
(38, 4, 'Bài thơ "Đồng chí" của Chính Hữu viết về đề tài gì?',
     'Tình yêu', 'Người lính', 'Thiên nhiên', 'Gia đình', 'B',
     'Đồng chí viết về tình đồng chí đồng đội của người lính trong kháng chiến chống Pháp',
     8, NOW(), NOW()),
(39, 4, 'Phong cách nghệ thuật của Nam Cao là gì?',
     'Lãng mạn', 'Hiện thực phê phán', 'Trữ tình', 'Siêu thực', 'B',
     'Nam Cao là nhà văn hiện thực phê phán xuất sắc của văn học Việt Nam',
     9, NOW(), NOW()),
(40, 4, '"Vội vàng" là bài thơ của tác giả nào?',
     'Xuân Diệu', 'Hàn Mặc Tử', 'Huy Cận', 'Chế Lan Viên', 'A',
     'Vội vàng là bài thơ tiêu biểu của Xuân Diệu trong tập Thơ thơ (1938)',
     10, NOW(), NOW());

-- ================================================================
-- 20. MOCK TEST ATTEMPTS (mixed COMPLETED + IN_PROGRESS)
-- ================================================================
INSERT INTO mock_test_attempts (id, mock_test_id, student_user_id, center_id, status,
                                 score, max_score, total_questions, correct_answers,
                                 test_title_snapshot, time_spent_seconds, started_at,
                                 submitted_at, completed_at) VALUES
-- Center 1 - Test 1 (Math A1) attempts
(1, 1, 10, 1, 'COMPLETED',   8, 10, 10, 8, 'Mock Test Toán A1 - Đại Số',    1500, '2026-06-15 09:00:00', '2026-06-15 09:25:00', '2026-06-15 09:25:00'),
(2, 1, 12, 1, 'COMPLETED',   10, 10, 10, 10,'Mock Test Toán A1 - Đại Số',   1800, '2026-06-15 10:00:00', '2026-06-15 10:30:00', '2026-06-15 10:30:00'),
(3, 1, 11, 1, 'COMPLETED',   6, 10, 10, 6, 'Mock Test Toán A1 - Đại Số',    1400, '2026-06-16 09:00:00', '2026-06-16 09:23:00', '2026-06-16 09:23:00'),
(4, 1, 14, 1, 'IN_PROGRESS', 0, 10, 10, 0, 'Mock Test Toán A1 - Đại Số',    900,  '2026-06-28 14:00:00', NULL, NULL),
-- Center 1 - Test 2 (Math B1) attempts
(5, 2, 12, 1, 'COMPLETED',   9, 10, 10, 9, 'Mock Test Toán B1 - Giải Tích', 2100, '2026-06-18 13:00:00', '2026-06-18 13:35:00', '2026-06-18 13:35:00'),
(6, 2, 18, 1, 'COMPLETED',   7, 10, 10, 7, 'Mock Test Toán B1 - Giải Tích', 2400, '2026-06-20 14:00:00', '2026-06-20 14:40:00', '2026-06-20 14:40:00'),
-- Center 2 - Test 3 (English B1) attempts
(7, 3, 26, 2, 'COMPLETED',   8, 10, 10, 8, 'Mock Test Anh B1 - Ngữ Pháp',   1800, '2026-06-19 10:00:00', '2026-06-19 10:30:00', '2026-06-19 10:30:00'),
(8, 3, 27, 2, 'COMPLETED',   6, 10, 10, 6, 'Mock Test Anh B1 - Ngữ Pháp',   1600, '2026-06-21 09:00:00', '2026-06-21 09:26:00', '2026-06-21 09:26:00'),
(9, 3, 31, 2, 'IN_PROGRESS', 0, 10, 10, 0, 'Mock Test Anh B1 - Ngữ Pháp',   600,  '2026-06-28 15:00:00', NULL, NULL),
-- Center 2 - Test 4 (Literature) attempts
(10, 4, 24, 2, 'COMPLETED',  9, 10, 10, 9, 'Mock Test Văn 10 - Tổng Hợp',   1700, '2026-06-17 08:00:00', '2026-06-17 08:28:00', '2026-06-17 08:28:00'),
(11, 4, 28, 2, 'COMPLETED',  7, 10, 10, 7, 'Mock Test Văn 10 - Tổng Hợp',   2000, '2026-06-22 09:00:00', '2026-06-22 09:33:00', '2026-06-22 09:33:00');

-- ================================================================
-- 21. MOCK TEST ATTEMPT ANSWERS (for COMPLETED attempts)
-- ================================================================
INSERT INTO mock_test_attempt_answers (id, attempt_id, question_id, question_text,
                                        student_answer, is_correct, correct_answer,
                                        created_at, updated_at) VALUES
-- Attempt 1 (Student 10, Test 1: 8/10)
(1,  1, 1,  'Giải hệ PT: 2x + 3y = 13, x - y = 2. Nghiệm là?', 'A', b'1', 'A', NOW(), NOW()),
(2,  1, 2,  'Ma trận đơn vị cấp 3 có bao nhiêu phần tử bằng 1?', 'B', b'1', 'B', NOW(), NOW()),
(3,  1, 3,  'Tính định thức det([[2,1],[3,4]])', 'A', b'1', 'A', NOW(), NOW()),
(4,  1, 4,  'Hạng của ma trận [[1,2,3],[4,5,6],[7,8,9]] là?', 'B', b'1', 'B', NOW(), NOW()),
(5,  1, 5,  'Nghiệm của AX=B khi det(A)≠0 là?', 'B', b'0', 'A', NOW(), NOW()),
(6,  1, 6,  'Vector riêng của ma trận A ứng với trị riêng λ thỏa mãn?', 'A', b'1', 'A', NOW(), NOW()),
(7,  1, 7,  'Tập nghiệm của hệ thuần nhất AX=0 là?', 'B', b'1', 'B', NOW(), NOW()),
(8,  1, 8,  'Ma trận chéo hóa được khi nào?', 'C', b'0', 'A', NOW(), NOW()),
(9,  1, 9,  'Ma trận chuyển vị của [[1,2],[3,4]] là?', 'B', b'1', 'B', NOW(), NOW()),
(10, 1, 10, 'Tích vô hướng của (1,2,3) và (4,5,6) là?', 'A', b'1', 'A', NOW(), NOW()),
-- Attempt 2 (Student 12, Test 1: 10/10)
(11, 2, 1,  'Giải hệ PT: 2x + 3y = 13, x - y = 2. Nghiệm là?', 'A', b'1', 'A', NOW(), NOW()),
(12, 2, 2,  'Ma trận đơn vị cấp 3 có bao nhiêu phần tử bằng 1?', 'B', b'1', 'B', NOW(), NOW()),
(13, 2, 3,  'Tính định thức det([[2,1],[3,4]])', 'A', b'1', 'A', NOW(), NOW()),
(14, 2, 4,  'Hạng của ma trận [[1,2,3],[4,5,6],[7,8,9]] là?', 'B', b'1', 'B', NOW(), NOW()),
(15, 2, 5,  'Nghiệm của AX=B khi det(A)≠0 là?', 'A', b'1', 'A', NOW(), NOW()),
(16, 2, 6,  'Vector riêng của ma trận A ứng với trị riêng λ thỏa mãn?', 'A', b'1', 'A', NOW(), NOW()),
(17, 2, 7,  'Tập nghiệm của hệ thuần nhất AX=0 là?', 'B', b'1', 'B', NOW(), NOW()),
(18, 2, 8,  'Ma trận chéo hóa được khi nào?', 'A', b'1', 'A', NOW(), NOW()),
(19, 2, 9,  'Ma trận chuyển vị của [[1,2],[3,4]] là?', 'B', b'1', 'B', NOW(), NOW()),
(20, 2, 10, 'Tích vô hướng của (1,2,3) và (4,5,6) là?', 'A', b'1', 'A', NOW(), NOW()),
-- Attempt 5 (Student 12, Test 2: 9/10)
(21, 5, 11, 'Tính lim(x→0) sin(3x)/x', 'B', b'1', 'B', NOW(), NOW()),
(22, 5, 12, 'Đạo hàm của f(x)=e^(2x).sin(x)', 'A', b'1', 'A', NOW(), NOW()),
(23, 5, 13, 'Tích phân ∫₀¹ x² dx = ?', 'B', b'1', 'B', NOW(), NOW()),
(24, 5, 14, 'Chuỗi ∑ 1/n² có hội tụ không?', 'B', b'1', 'B', NOW(), NOW()),
(25, 5, 15, 'y''=2y, y(0)=1. Nghiệm là?', 'A', b'1', 'A', NOW(), NOW()),
(26, 5, 16, 'Đạo hàm của ln(x) là?', 'A', b'1', 'A', NOW(), NOW()),
(27, 5, 17, 'Tích phân từng phần: ∫u dv = ?', 'A', b'1', 'A', NOW(), NOW()),
(28, 5, 18, 'Điểm cực trị của f(x)=x³-3x là?', 'A', b'1', 'A', NOW(), NOW()),
(29, 5, 19, 'Giới hạn lim(x→∞) (1+1/x)^x = ?', 'B', b'1', 'B', NOW(), NOW()),
(30, 5, 20, 'Chuỗi Taylor của e^x tại x=0 là?', 'B', b'0', 'A', NOW(), NOW()),
-- Attempt 7 (Student 26, Test 3: 8/10)
(31, 7, 21, 'If I ___ rich, I would travel the world.', 'C', b'1', 'C', NOW(), NOW()),
(32, 7, 22, 'She suggested ___ the meeting until next week.', 'B', b'1', 'B', NOW(), NOW()),
(33, 7, 23, 'By the time we arrived, the movie ___.', 'C', b'1', 'C', NOW(), NOW()),
(34, 7, 24, 'I wish I ___ taller.', 'C', b'1', 'C', NOW(), NOW()),
(35, 7, 25, 'The book ___ by J.K. Rowling was amazing.', 'A', b'0', 'B', NOW(), NOW()),
(36, 7, 26, 'Not only ___ good at math, but she is also good at English.', 'B', b'1', 'B', NOW(), NOW()),
(37, 7, 27, 'I have been learning English ___ 5 years.', 'B', b'1', 'B', NOW(), NOW()),
(38, 7, 28, 'The harder you work, ___ you will succeed.', 'B', b'1', 'B', NOW(), NOW()),
(39, 7, 29, 'He asked me where ___.', 'B', b'1', 'B', NOW(), NOW()),
(40, 7, 30, 'It is high time we ___ action.', 'A', b'0', 'B', NOW(), NOW()),
-- Attempt 10 (Student 24, Test 4: 9/10)
(41, 10, 31, 'Tác giả của bài thơ "Tây Tiến" là ai?', 'A', b'1', 'A', NOW(), NOW()),
(42, 10, 32, '"Tây Tiến" được sáng tác năm nào?', 'B', b'1', 'B', NOW(), NOW()),
(43, 10, 33, 'Câu thơ "Sông Mã xa rồi Tây Tiến ơi" thể hiện điều gì?', 'A', b'1', 'A', NOW(), NOW()),
(44, 10, 34, 'Tác phẩm "Chí Phèo" của nhà văn nào?', 'B', b'1', 'B', NOW(), NOW()),
(45, 10, 35, '"Truyện Kiều" được Nguyễn Du sáng tác dựa trên tác phẩm nào?', 'A', b'1', 'A', NOW(), NOW()),
(46, 10, 36, 'Thể thơ của "Truyện Kiều" là gì?', 'B', b'1', 'B', NOW(), NOW()),
(47, 10, 37, 'Nhân vật chính trong "Vợ nhặt" của Kim Lân là ai?', 'A', b'1', 'A', NOW(), NOW()),
(48, 10, 38, 'Bài thơ "Đồng chí" của Chính Hữu viết về đề tài gì?', 'B', b'1', 'B', NOW(), NOW()),
(49, 10, 39, 'Phong cách nghệ thuật của Nam Cao là gì?', 'B', b'1', 'B', NOW(), NOW()),
(50, 10, 40, '"Vội vàng" là bài thơ của tác giả nào?', 'C', b'0', 'A', NOW(), NOW());

-- ================================================================
-- 22. STUDENT DOCUMENTS
-- ================================================================
INSERT INTO student_documents (id, student_user_id, center_id, clazz_id,
                                document_type, title, file_url, description, created_at) VALUES
-- Center 1 documents
(1, 10, 1, 1, 'PDF',  'Bài tập Đại Số Tuần 1',   'https://storage.owlexa.vn/docs/math-a1/w1-st10.pdf',  'Bài tập đại số tuần 1',        NOW()),
(2, 12, 1, 2, 'PDF',  'Bài Tập Giải Tích',       'https://storage.owlexa.vn/docs/math-b1/bt-st12.pdf',  'Bài tập giải tích nâng cao',    NOW()),
(3, 15, 1, 3, 'PDF',  'Essay Draft English A1',  'https://storage.owlexa.vn/docs/eng-a1/draft-st15.pdf','Bản nháp luận tiếng Anh',       NOW()),
(4, 19, 1, NULL,'VIDEO','Video Thuyết Trình',     'https://storage.owlexa.vn/docs/general/video-st19.mp4','Video bài thuyết trình',       NOW()),
-- Center 2 documents
(5, 24, 2, 4, 'PDF',  'Bài Phân Tích Tây Tiến',  'https://storage.owlexa.vn/docs/van10/tt-st24.pdf',    'Bài phân tích văn học',        NOW()),
(6, 27, 2, 5, 'PDF',  'English Essay Draft',     'https://storage.owlexa.vn/docs/eng-b1/draft-st27.pdf','Bản nháp luận tiếng Anh B1',   NOW()),
(7, 34, 2, 6, 'OTHER','Dự Án Toán C1',           'https://storage.owlexa.vn/docs/toan-c1/proj-st34.zip','File báo cáo dự án Toán C1',   NOW());

COMMIT;
SET autocommit = 1;
