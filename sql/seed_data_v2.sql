-- ================================================================
-- OWLEXA SEED DATA v2.0
-- Generated: 2026-06-30
-- Backend: Spring Boot 4.0 + Hibernate 7 + MySQL 8
-- IMPORTANT: Run this script AFTER Hibernate auto-generates the schema.
--            All foreign keys reference real entity fields.
--            Uses BCrypt passwords (all = "password123")
--            Idempotent: DELETE + INSERT, runs inside transaction.
-- ================================================================

SET autocommit = 0;
START TRANSACTION;
SET NAMES 'utf8mb4';
SET CHARACTER SET utf8mb4;

-- ================================================================
-- 1. PERMISSIONS
-- ================================================================
DELETE FROM user_permission WHERE 1=1;
DELETE FROM permissions WHERE 1=1;

INSERT INTO permissions (id, code, description) VALUES
(1,  'MANAGE_STUDENTS',  'Create, update, delete students'),
(2,  'MANAGE_TEACHERS',  'Create, update, delete teachers'),
(3,  'MANAGE_CLASSES',   'Create, update, delete classes'),
(4,  'MANAGE_SCHEDULES', 'Manage class schedules'),
(5,  'MANAGE_FEE_RECORDS','Manage fee records and payments'),
(6,  'MANAGE_ESSAYS',     'Manage essay rubrics and grading'),
(7,  'MANAGE_MOCK_TESTS', 'Manage mock tests and results'),
(8,  'VIEW_REPORTS',      'View analytics and reports'),
(9,  'MARK_ATTENDANCE',   'Mark student attendance'),
(10, 'ENROLL_STUDENTS',   'Enroll and drop students from classes');

-- ================================================================
-- 2. USERS
-- Password for ALL users: BCrypt("password123")
-- Hash generated with BCryptPasswordEncoder (strength=10)
-- ================================================================
DELETE FROM mock_test_attempt_answers WHERE 1=1;
DELETE FROM mock_test_attempts WHERE 1=1;
DELETE FROM mock_test_questions WHERE 1=1;
DELETE FROM mock_tests WHERE 1=1;
DELETE FROM essay_criteria_scores WHERE 1=1;
DELETE FROM essay_grading_results WHERE 1=1;
DELETE FROM essay_submissions WHERE 1=1;
DELETE FROM essay_rubric_criteria WHERE 1=1;
DELETE FROM essay_rubrics WHERE 1=1;
DELETE FROM student_documents WHERE 1=1;
DELETE FROM payments WHERE 1=1;
DELETE FROM fee_records WHERE 1=1;
DELETE FROM attendances WHERE 1=1;
DELETE FROM class_enrollments WHERE 1=1;
DELETE FROM schedules WHERE 1=1;
DELETE FROM classes WHERE 1=1;
DELETE FROM membership WHERE 1=1;
DELETE FROM user_permission WHERE 1=1;
DELETE FROM centers WHERE 1=1;
DELETE FROM user_sessions WHERE 1=1;
DELETE FROM users WHERE 1=1;

INSERT INTO users (id, phone_number, email, full_name, password, role) VALUES
-- Super Admin
(1, '0000000001', 'admin@owlexa.vn',        'System Admin',         '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsA4r/H6jX1r1WKVqy',  'ADMIN'),
-- Owner of Center 1
(2, '0900000001', 'nguyenvana@owlexa.vn',    'Nguyen Van A',         '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsA4r/H6jX1r1WKVqy',  'OWNER'),
-- Teachers for Center 1
(3, '0900000003', 'tranvietb@owlexa.vn',     'Tran Viet B',          '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsA4r/H6jX1r1WKVqy',  'TEACHER'),
(4, '0900000004', 'lehongc@owlexa.vn',        'Le Hong C',            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsA4r/H6jX1r1WKVqy',  'TEACHER'),
-- Cashier for Center 1
(5, '0900000005', 'phamthid@owlexa.vn',       'Pham Thi D',           '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsA4r/H6jX1r1WKVqy',  'CASHIER'),
-- Students for Center 1
(6, '0901000001', 'nguyenminhe@owlexa.vn',     'Nguyen Minh E',        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsA4r/H6jX1r1WKVqy',  'STUDENT'),
(7, '0901000002', 'levietf@owlexa.vn',         'Le Viet F',            '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsA4r/H6jX1r1WKVqy',  'STUDENT'),
(8, '0901000003', 'phamthig@owlexa.vn',        'Pham Thi G',           '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsA4r/H6jX1r1WKVqy',  'STUDENT'),
(9, '0901000004', 'doducth@owlexa.vn',         'Do Duc H',             '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsA4r/H6jX1r1WKVqy',  'STUDENT'),
(10,'0901000005', 'buihoaii@owlexa.vn',        'Bui Hoai I',           '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsA4r/H6jX1r1WKVqy',  'STUDENT'),
(11,'0901000006', 'dinhthanhj@owlexa.vn',      'Dinh Thanh J',         '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsA4r/H6jX1r1WKVqy',  'STUDENT');

-- ================================================================
-- 3. CENTERS
-- ================================================================
INSERT INTO centers (id, owner_user_id, name, subdomain, created_at) VALUES
(1, 2, 'Owlexa Center Ho Chi Minh', 'hcm', NOW());

-- ================================================================
-- 4. MEMBERSHIPS
-- Every user (except ADMIN) belongs to Center 1
-- joined_by_user_id = owner (user 2)
-- ================================================================
INSERT INTO membership (id, center_id, user_id, joined_by_user_id, joined_at) VALUES
(1, 1, 2, 2, NOW()),  -- Owner
(2, 1, 3, 2, NOW()),  -- Teacher B
(3, 1, 4, 2, NOW()),  -- Teacher C
(4, 1, 5, 2, NOW()),  -- Cashier D
(5, 1, 6, 2, NOW()),  -- Student E
(6, 1, 7, 2, NOW()),  -- Student F
(7, 1, 8, 2, NOW()),  -- Student G
(8, 1, 9, 2, NOW()),  -- Student H
(9, 1, 10, 2, NOW()), -- Student I
(10, 1, 11, 2, NOW());-- Student J

-- ================================================================
-- 5. USER PERMISSIONS (for OWNER)
-- ================================================================
INSERT INTO user_permission (id, user_id, permission_id, granted_at) VALUES
(1, 2, 1,  NOW()),
(2, 2, 2,  NOW()),
(3, 2, 3,  NOW()),
(4, 2, 4,  NOW()),
(5, 2, 5,  NOW()),
(6, 2, 6,  NOW()),
(7, 2, 7,  NOW()),
(8, 2, 8,  NOW()),
(9, 2, 9,  NOW()),
(10, 2, 10, NOW()),
-- Teacher B: MANAGE_ATTENDANCE, VIEW_REPORTS
(11, 3, 9,  NOW()),
(12, 3, 8,  NOW()),
-- Cashier D: MANAGE_FEE_RECORDS, VIEW_REPORTS
(13, 5, 5,  NOW()),
(14, 5, 8,  NOW());

-- ================================================================
-- 6. CLASSES
-- teacher_id -> User.id
-- NOTE: Class entity has 'teacher' field but ClassService creates
--       without teacher (teacher_id nullable). We set teacher later
--       via schedule.
-- vstep_level, monthly_fee, is_active are the new columns.
-- create_at = CreationTimestamp.
-- ================================================================
INSERT INTO classes (id, name, center_id, teacher_id, max_students, description,
                     vstep_level, monthly_fee, is_active, create_at) VALUES
(1, 'Lớp TOÁN A1 - Cơ Bản',     1, NULL, 30, 'Lớp toán cơ bản cho học sinh mới nhập học',   'BEGINNER',     1500000.00, 1, NOW()),
(2, 'Lớp TOÁN B1 - Nâng Cao',    1, NULL, 25, 'Lớp toán nâng cao cho học sinh đã có nền',   'INTERMEDIATE',  2000000.00, 1, NOW()),
(3, 'Lớp ANH A1',                1, NULL, 30, 'Lớp tiếng Anh cơ bản',                         'BEGINNER',     1200000.00, 1, NOW()),
(4, 'Lớp ANH B1 - Giao Tiếp',   1, NULL, 20, 'Lớp tiếng Anh giao tiếp nâng cao',             'INTERMEDIATE',  1800000.00, 1, NOW()),
(5, 'Lớp VĂN 10',               1, NULL, 25, 'Lớp ngữ văn lớp 10',                           'BEGINNER',     1300000.00, 1, NOW());

-- ================================================================
-- 7. SCHEDULES
-- day_of_week = integer 0-6 (0=Monday, 6=Sunday)
-- NOTE: ScheduleRequest accepts Integer dayOfWeek
-- is_active = true
-- teacher_user_id -> User.id (TEACHER role)
-- ================================================================
INSERT INTO schedules (id, class_id, center_id, teacher_user_id, day_of_week,
                       start_time, end_time, room, is_active, created_at) VALUES
-- Class 1: Teacher B - Monday & Wednesday
(1, 1, 1, 3, 'MONDAY',    '08:00:00', '10:00:00', 'Room 101', 1, NOW()),
(2, 1, 1, 3, 'WEDNESDAY', '08:00:00', '10:00:00', 'Room 101', 1, NOW()),
-- Class 2: Teacher B - Tuesday & Thursday
(3, 2, 1, 3, 'TUESDAY',   '13:00:00', '15:00:00', 'Room 102', 1, NOW()),
(4, 2, 1, 3, 'THURSDAY',  '13:00:00', '15:00:00', 'Room 102', 1, NOW()),
-- Class 3: Teacher C - Monday & Wednesday
(5, 3, 1, 4, 'MONDAY',    '10:30:00', '12:00:00', 'Room 201', 1, NOW()),
(6, 3, 1, 4, 'WEDNESDAY', '10:30:00', '12:00:00', 'Room 201', 1, NOW()),
-- Class 4: Teacher C - Tuesday & Friday
(7, 4, 1, 4, 'TUESDAY',   '15:00:00', '17:00:00', 'Room 202', 1, NOW()),
(8, 4, 1, 4, 'FRIDAY',    '15:00:00', '17:00:00', 'Room 202', 1, NOW()),
-- Class 5: Teacher B - Saturday
(9, 5, 1, 3, 'SATURDAY',  '09:00:00', '11:00:00', 'Room 103', 1, NOW());

-- ================================================================
-- 8. CLASS_ENROLLMENTS
-- status = ACTIVE or DROPPED
-- enrolled_by_user_id -> User.id (the owner who enrolled them)
-- ================================================================
INSERT INTO class_enrollments (id, student_user_id, class_id, center_id,
                               enrolled_by_user_id, status, enrolled_at) VALUES
-- Students in Class 1 (Math A1)
(1,  6, 1, 1, 2, 'ACTIVE',  NOW()),
(2,  7, 1, 1, 2, 'ACTIVE',  NOW()),
(3,  8, 1, 1, 2, 'ACTIVE',  NOW()),
(4,  9, 1, 1, 2, 'ACTIVE',  NOW()),
-- Students in Class 2 (Math B1)
(5,  7, 2, 1, 2, 'ACTIVE',  NOW()),
(6,  8, 2, 1, 2, 'ACTIVE',  NOW()),
(7,  10, 2, 1, 2, 'ACTIVE', NOW()),
-- Students in Class 3 (English A1)
(8,  6, 3, 1, 2, 'ACTIVE',  NOW()),
(9,  9, 3, 1, 2, 'ACTIVE',  NOW()),
(10, 11, 3, 1, 2, 'ACTIVE', NOW()),
-- Students in Class 4 (English B1)
(11, 7,  4, 1, 2, 'ACTIVE', NOW()),
(12, 10, 4, 1, 2, 'ACTIVE', NOW()),
(13, 11, 4, 1, 2, 'ACTIVE', NOW()),
-- Students in Class 5 (Literature)
(14, 6,  5, 1, 2, 'ACTIVE', NOW()),
(15, 8,  5, 1, 2, 'ACTIVE', NOW());

-- ================================================================
-- 9. ATTENDANCES
-- student_user_id -> User.id (STUDENT)
-- schedule_id -> Schedule.id
-- center_id = 1
-- status = PRESENT, ABSENT, or EXCUSED
-- marked_by_user_id -> User.id (TEACHER)
-- date = specific dates (use past dates for testing)
-- ================================================================
INSERT INTO attendances (id, student_user_id, schedule_id, center_id,
                         date, status, marked_by_user_id, note, created_at) VALUES
-- Schedule 1 (Class 1 - Mon) attendance for 3 sessions
(1,  6, 1, 1, '2026-06-02', 'PRESENT', 3, NULL,         NOW()),
(2,  7, 1, 1, '2026-06-02', 'PRESENT', 3, NULL,         NOW()),
(3,  8, 1, 1, '2026-06-02', 'ABSENT',  3, 'Vắng mặt',  NOW()),
(4,  9, 1, 1, '2026-06-02', 'PRESENT', 3, NULL,         NOW()),
(5,  6, 1, 1, '2026-06-09', 'PRESENT', 3, NULL,         NOW()),
(6,  7, 1, 1, '2026-06-09', 'EXCUSED', 3, 'Có việc gia đình', NOW()),
(7,  8, 1, 1, '2026-06-09', 'PRESENT', 3, NULL,         NOW()),
(8,  9, 1, 1, '2026-06-09', 'PRESENT', 3, NULL,         NOW()),
(9,  6, 1, 1, '2026-06-16', 'PRESENT', 3, NULL,         NOW()),
(10, 7, 1, 1, '2026-06-16', 'PRESENT', 3, NULL,         NOW()),
-- Schedule 5 (Class 3 - Mon English)
(11, 6,  5, 1, '2026-06-02', 'PRESENT', 4, NULL,        NOW()),
(12, 9,  5, 1, '2026-06-02', 'PRESENT', 4, NULL,        NOW()),
(13, 11, 5, 1, '2026-06-02', 'ABSENT',  4, NULL,        NOW()),
(14, 6,  5, 1, '2026-06-09', 'PRESENT', 4, NULL,        NOW()),
(15, 9,  5, 1, '2026-06-09', 'PRESENT', 4, NULL,        NOW()),
(16, 11, 5, 1, '2026-06-09', 'PRESENT', 4, NULL,        NOW());

-- ================================================================
-- 10. FEE_RECORDS
-- month = string 'YYYY-MM'
-- status = UNPAID, PARTIAL, PAID
-- clazz_id -> Class.id (nullable for general fee)
-- ================================================================
INSERT INTO fee_records (id, center_id, student_user_id, class_id,
                         amount, paid_amount, month, due_date, status, created_at) VALUES
-- Student 6: Math A1 + English A1
(1,  1, 6, 1, 1500000.00, 1500000.00, '2026-06', '2026-06-25', 'PAID',   NOW()),
(2,  1, 6, 3, 1200000.00, 600000.00,  '2026-06', '2026-06-25', 'PARTIAL',NOW()),
(3,  1, 6, 5, 1300000.00, 0.00,       '2026-06', '2026-06-25', 'UNPAID', NOW()),
-- Student 7: Math A1 + Math B1 + English B1
(4,  1, 7, 1, 1500000.00, 1500000.00, '2026-06', '2026-06-25', 'PAID',   NOW()),
(5,  1, 7, 2, 2000000.00, 2000000.00, '2026-06', '2026-06-25', 'PAID',   NOW()),
(6,  1, 7, 4, 1800000.00, 900000.00,  '2026-06', '2026-06-25', 'PARTIAL',NOW()),
-- Student 8: Math A1 + Math B1 + Literature
(7,  1, 8, 1, 1500000.00, 1500000.00, '2026-06', '2026-06-25', 'PAID',   NOW()),
(8,  1, 8, 2, 2000000.00, 0.00,       '2026-06', '2026-06-25', 'UNPAID', NOW()),
(9,  1, 8, 5, 1300000.00, 1300000.00, '2026-06', '2026-06-25', 'PAID',   NOW()),
-- Student 9: Math A1 + English A1
(10, 1, 9, 1, 1500000.00, 1500000.00, '2026-06', '2026-06-25', 'PAID',   NOW()),
(11, 1, 9, 3, 1200000.00, 1200000.00, '2026-06', '2026-06-25', 'PAID',   NOW()),
-- Student 10: Math B1 + English B1
(12, 1, 10, 2, 2000000.00, 2000000.00,'2026-06', '2026-06-25', 'PAID',   NOW()),
(13, 1, 10, 4, 1800000.00, 1800000.00,'2026-06', '2026-06-25', 'PAID',   NOW()),
-- Student 11: English A1 + English B1 + Literature
(14, 1, 11, 3, 1200000.00, 600000.00, '2026-06', '2026-06-25', 'PARTIAL',NOW()),
(15, 1, 11, 4, 1800000.00, 0.00,      '2026-06', '2026-06-25', 'UNPAID', NOW()),
(16, 1, 11, 5, 1300000.00, 1300000.00,'2026-06', '2026-06-25', 'PAID',   NOW());

-- ================================================================
-- 11. PAYMENTS
-- method = CASH or SEPAY
-- collected_by_user_id -> User.id (CASHIER role)
-- ================================================================
INSERT INTO payments (id, fee_record_id, center_id, student_user_id,
                      collected_by_user_id, amount, method, sepay_ref, note, created_at) VALUES
-- Student 6, fee_record 1 (PAID)
(1,  1, 1, 6, 5, 1500000.00, 'CASH',  NULL, 'Thanh toán tiền mặt tháng 6',        NOW()),
-- Student 6, fee_record 2 (PARTIAL) - first payment
(2,  2, 1, 6, 5, 600000.00,  'CASH',  NULL, 'Thanh toán một phần tháng 6',         NOW()),
-- Student 7, fee_records 4 & 5 (PAID)
(3,  4, 1, 7, 5, 1500000.00, 'SEPAY', 'SP202606001', 'Thanh toán Sepay tháng 6',     NOW()),
(4,  5, 1, 7, 5, 2000000.00, 'SEPAY', 'SP202606002', 'Thanh toán Sepay tháng 6',     NOW()),
-- Student 7, fee_record 6 (PARTIAL)
(5,  6, 1, 7, 5, 900000.00,  'CASH',  NULL, 'Thanh toán một phần tháng 6',         NOW()),
-- Student 8, fee_record 7 (PAID)
(6,  7, 1, 8, 5, 1500000.00, 'CASH',  NULL, 'Thanh toán tiền mặt tháng 6',         NOW()),
-- Student 9, fee_records 10 & 11 (PAID)
(7,  10, 1, 9, 5, 1500000.00, 'CASH',  NULL, 'Thanh toán tiền mặt tháng 6',        NOW()),
(8,  11, 1, 9, 5, 1200000.00, 'SEPAY', 'SP202606003', 'Thanh toán Sepay tháng 6',    NOW()),
-- Student 10, fee_records 12 & 13 (PAID)
(9,  12, 1, 10, 5, 2000000.00, 'SEPAY', 'SP202606004', 'Thanh toán Sepay tháng 6',    NOW()),
(10, 13, 1, 10, 5, 1800000.00, 'CASH',  NULL, 'Thanh toán tiền mặt tháng 6',       NOW()),
-- Student 11, fee_record 14 (PARTIAL)
(11, 14, 1, 11, 5, 600000.00,  'SEPAY', 'SP202606005', 'Thanh toán một phần tháng 6',NOW()),
-- Student 11, fee_record 16 (PAID)
(12, 16, 1, 11, 5, 1300000.00, 'SEPAY', 'SP202606006', 'Thanh toán Sepay tháng 6',   NOW());

-- ================================================================
-- 12. ESSAY_RUBRICS
-- created_by_user_id -> User.id (OWNER)
-- clazz_id -> Class.id
-- is_active = 1
-- max_score = total possible score
-- ================================================================
INSERT INTO essay_rubrics (id, title, description, max_score, clazz_id,
                           created_by_user_id, center_id, is_active, created_at) VALUES
-- Rubric for Class 1 (Math A1) - Essay rubric
(1, 'Rubric Bài Luận Toán A1 - Đại Số', 'Đánh giá bài luận về đại số tuyến tính cho lớp Math A1',
    100.00, 1, 2, 1, 1, NOW()),
-- Rubric for Class 2 (Math B1) - Essay rubric
(2, 'Rubric Bài Luận Toán B1 - Giải Tích', 'Đánh giá bài luận giải tích nâng cao lớp Math B1',
    100.00, 2, 2, 1, 1, NOW()),
-- Rubric for Class 5 (Literature)
(3, 'Rubric Bài Nghị Luận Văn Học', 'Đánh giá bài nghị luận văn học lớp 10',
    100.00, 5, 2, 1, 1, NOW());

-- ================================================================
-- 13. ESSAY_RUBRIC_CRITERIA
-- weight sums should equal 1.0 for proper scoring
-- ================================================================
INSERT INTO essay_rubric_criteria (id, rubric_id, name, description, weight, max_score) VALUES
-- Rubric 1 criteria (Math A1)
(1, 1, 'Trình bày & Cấu trúc', 'Bố cục bài luận rõ ràng, logic',       0.20, 20.00),
(2, 1, 'Kiến thức cơ bản',     'Áp dụng đúng công thức đại số',          0.30, 30.00),
(3, 1, 'Tư duy phân tích',      'Phân tích và lập luận chặt chẽ',          0.30, 30.00),
(4, 1, 'Kết luận',              'Kết quả đúng và kết luận hợp lý',        0.20, 20.00),
-- Rubric 2 criteria (Math B1)
(5, 2, 'Trình bày & Cấu trúc', 'Bố cục bài luận rõ ràng, logic',         0.20, 20.00),
(6, 2, 'Kiến thức nâng cao',   'Áp dụng đúng định lý giải tích',         0.35, 35.00),
(7, 2, 'Tư duy phản biện',     'Phân tích phản biện sâu sắc',             0.25, 25.00),
(8, 2, 'Kết luận & minh chứng','Kết luận có dẫn chứng thuyết phục',       0.20, 20.00),
-- Rubric 3 criteria (Literature)
(9,  3, 'Nội dung',             'Nắm vững nội dung tác phẩm',              0.30, 30.00),
(10, 3, 'Phân tích văn chương', 'Phân tích các biện pháp nghệ thuật',     0.30, 30.00),
(11, 3, 'Bố cục & Lập luận',   'Lập luận mạch lạc, có hệ thống',         0.20, 20.00),
(12, 3, 'Ngôn ngữ',             'Sử dụng ngôn ngữ văn học phù hợp',       0.20, 20.00);

-- ================================================================
-- 14. ESSAY_SUBMISSIONS
-- status = DRAFT, SUBMITTED, GRADED, REVIEWED
-- graded_by_user_id -> User.id (nullable until graded)
-- ================================================================
INSERT INTO essay_submissions (id, student_user_id, center_id, clazz_id, rubric_id,
                                content, status, graded_by_user_id, feedback,
                                total_score, submitted_at, graded_at) VALUES
-- Student 6, Class 1, Rubric 1
(1, 6, 1, 1, 1,
 'Bài luận về đại số tuyến tính: Phân tích hệ phương trình tuyến tính 3 ẩn...',
 'GRADED', 3, 'Bài làm tốt, lập luận rõ ràng. Cần cải thiện phần kết luận.',
 85.00, '2026-06-20 10:00:00', '2026-06-21 14:00:00'),
-- Student 7, Class 2, Rubric 2
(2, 7, 1, 2, 2,
 'Bài luận giải tích: Khảo sát sự hội tụ của chuỗi số...',
 'GRADED', 3, 'Xuất sắc! Phân tích chuyên sâu, minh chứng phong phú.',
 95.00, '2026-06-18 09:00:00', '2026-06-19 11:00:00'),
-- Student 8, Class 5, Rubric 3
(3, 8, 1, 5, 3,
 'Bài nghị luận về tác phẩm "Tây Tiến" của Quang Dũng...',
 'SUBMITTED', NULL, NULL, NULL, '2026-06-25 15:00:00', NULL),
-- Student 9, Class 1, Rubric 1
(4, 9, 1, 1, 1,
 'Bài luận đại số: Phương pháp Gauss-Jordan giải hệ phương trình...',
 'DRAFT', NULL, NULL, NULL, '2026-06-26 08:00:00', NULL),
-- Student 10, Class 2, Rubric 2
(5, 10, 1, 2, 2,
 'Bài luận giải tích: Tích phân Riemann và ứng dụng...',
 'GRADED', 4, 'Bài làm khá tốt, cần thêm ví dụ minh họa.',
 78.00, '2026-06-22 13:00:00', '2026-06-23 09:00:00');

-- ================================================================
-- 15. ESSAY_GRADING_RESULTS
-- submission_id -> EssaySubmission.id (unique)
-- ================================================================
INSERT INTO essay_grading_results (id, submission_id, total_score, max_score,
                                    feedback, graded_at) VALUES
(1, 1, 85.00, 100.00, 'Bài làm tốt, lập luận rõ ràng. Cần cải thiện phần kết luận.', '2026-06-21 14:00:00'),
(2, 2, 95.00, 100.00, 'Xuất sắc! Phân tích chuyên sâu, minh chứng phong phú.',       '2026-06-19 11:00:00'),
(3, 5, 78.00, 100.00, 'Bài làm khá tốt, cần thêm ví dụ minh họa.',                  '2026-06-23 09:00:00');

-- ================================================================
-- 16. ESSAY_CRITERIA_SCORES
-- criteria_id -> EssayRubricCriterion.id
-- ================================================================
INSERT INTO essay_criteria_scores (id, grading_result_id, criteria_id, score, max_score, feedback) VALUES
-- Grading result 1 (submission 1, rubric 1)
(1, 1, 1, 18.00, 20.00, 'Trình bày rõ ràng, có tiêu đề và đoạn văn mạch lạc'),
(2, 1, 2, 26.00, 30.00, 'Áp dụng đúng các phép biến đổi sơ cấp'),
(3, 1, 3, 26.00, 30.00, 'Lập luận logic, có giải thích từng bước'),
(4, 1, 4, 15.00, 20.00, 'Kết luận đúng nhưng thiếu kiểm chứng lại'),
-- Grading result 2 (submission 2, rubric 2)
(5, 2, 5, 18.00, 20.00, 'Bố cục chặt chẽ, có mở bài thu hút'),
(6, 2, 6, 33.00, 35.00, 'Áp dụng định lý chính xác, có chứng minh'),
(7, 2, 7, 24.00, 25.00, 'Phân tích sâu sắc, có liên hệ thực tiễn'),
(8, 2, 8, 20.00, 20.00, 'Kết luận rõ ràng với đầy đủ dẫn chứng'),
-- Grading result 3 (submission 5, rubric 2)
(9,  3, 5, 16.00, 20.00, 'Trình bày khá rõ ràng nhưng thiếu đôi chỗ'),
(10, 3, 6, 28.00, 35.00, 'Hiểu đúng nhưng còn sai sót nhỏ'),
(11, 3, 7, 18.00, 25.00, 'Tư duy phản biện đạt yêu cầu'),
(12, 3, 8, 16.00, 20.00, 'Kết luận đúng nhưng chưa thuyết phục');

-- ================================================================
-- 17. MOCK_TESTS
-- created_by_user_id -> User.id (OWNER)
-- is_active = 1
-- duration = minutes (integer)
-- total_questions = integer
-- level = BEGINNER, INTERMEDIATE, ADVANCED
-- ================================================================
INSERT INTO mock_tests (id, title, description, center_id, created_by_user_id,
                         level, total_questions, is_active, created_at) VALUES
-- NOTE: duration stored in duration_minutes column (set via Hibernate, not in INSERT)
(1, 'Mock Test TOÁN A1 - Đại Số Cơ Bản', 'Bài kiểm tra đại số tuyến tính cơ bản, 20 câu, 30 phút',
    1, 2, 'BEGINNER',     20, 1, NOW()),
(2, 'Mock Test TOÁN B1 - Giải Tích',    'Bài kiểm tra giải tích nâng cao, 15 câu, 45 phút',
    1, 2, 'INTERMEDIATE', 15, 1, NOW()),
(3, 'Mock Test ANH A1 - Từ Vựng',       'Bài kiểm tra từ vựng Tiếng Anh A1, 25 câu, 20 phút',
    1, 2, 'BEGINNER',     25, 1, NOW()),
(4, 'Mock Test ANH B1 - Ngữ Pháp',      'Bài kiểm tra ngữ pháp Tiếng Anh B1, 20 câu, 30 phút',
    1, 2, 'INTERMEDIATE',  20, 1, NOW());

-- ================================================================
-- 18. MOCK_TEST_QUESTIONS
-- correct_answer = 'A', 'B', 'C', or 'D'
-- sort_order = display order
-- ================================================================
INSERT INTO mock_test_questions (id, mock_test_id, question_text, optiona, optionb,
                                  optionc, optiond, correct_answer, explanation,
                                  sort_order, created_at, updated_at) VALUES
-- Mock Test 1: Math A1 (5 questions)
(1,  1, 'Hệ phương trình 2x + 3y = 13 và x - y = 2 có nghiệm là?',
     'x=3, y=1', 'x=4, y=2', 'x=2, y=3', 'x=5, y=1', 'A',
     'Giải: x-y=2 => x=y+2. Thế vào 2(y+2)+3y=13 => 5y=9 => y=9/5 => x=19/5',
     1, NOW(), NOW()),
(2,  1, 'Ma trận đơn vị cấp 3 có bao nhiêu phần tử bằng 1 trên đường chéo chính?',
     '1', '2', '3', '9', 'C',
     'Ma trận đơn vị cấp n có n phần tử =1 trên đường chéo chính',
     2, NOW(), NOW()),
(3,  1, 'Tính det([[2,1],[3,4]])',
     '5', '11', '8', '6', 'A',
     'det = 2*4 - 1*3 = 8 - 3 = 5',
     3, NOW(), NOW()),
(4,  1, 'Hạng của ma trận [[1,2,3],[4,5,6],[7,8,9]] là?',
     '1', '2', '3', '0', 'B',
     'C2=C2-C1 và C3=C3-C1 cho thấy hàng 3 phụ thuộc hàng 1,2 nên rank=2',
     4, NOW(), NOW()),
(5,  1, 'Nghiệm của phương trình ma trận AX=B khi det(A)≠0 là?',
     'X=A⁻¹B', 'X=BA⁻¹', 'X=B/A', 'X=AB', 'A',
     'Khi det(A)≠0, ma trận A khả nghịch và X=A⁻¹B',
     5, NOW(), NOW()),
-- Mock Test 2: Math B1 (5 questions)
(6,  2, 'Tính lim(x→0) sin(3x)/x',
     '1', '3', '1/3', '0', 'B',
     'lim sin(ax)/x = a khi x→0, nên sin(3x)/x = 3',
     1, NOW(), NOW()),
(7,  2, 'Đạo hàm của f(x)=e^(2x).sin(x) là?',
     'e^(2x)(2sin(x)+cos(x))', 'e^(2x)(sin(x)+2cos(x))', '2e^(2x)cos(x)', 'e^(2x)sin(x)', 'A',
     'Áp dụng quy tắc tích: f'' = (e^(2x))''.sin + e^(2x).(sin)'' = 2e^(2x)sin + e^(2x)cos',
     2, NOW(), NOW()),
(8,  2, 'Tích phân ∫₀¹ x² dx = ?',
     '1/4', '1/3', '1/2', '1', 'B',
     '∫x²dx = x³/3, đánh giá từ 0 đến 1 = 1/3 - 0 = 1/3',
     3, NOW(), NOW()),
(9,  2, 'Chuỗi ∑(n=1→∞) 1/n² hội tụ không?',
     'Không', 'Có (p=1)', 'Có (p=2)', 'Hội tụ có điều kiện', 'C',
     'Áp dụng kiểm tra p: p=2>1 nên hội tụ tuyệt đối',
     4, NOW(), NOW()),
(10, 2, 'Nghiệm của phương trình vi phân y''=2y, y(0)=1 là?',
     'y=e^(2x)', 'y=e^(x)', 'y=2e^(x)', 'y=2x+1', 'A',
     'Đây là PTVP tách biến: dy/y = 2dx => ln|y| = 2x + C => y = Ce^(2x). y(0)=1 => C=1',
     5, NOW(), NOW()),
-- Mock Test 3: English A1 (5 questions)
(11, 3, 'What is the past tense of "go"?',
     'Going', 'Goes', 'Gone', 'Went', 'D',
     '"Go" có past tense bất quy tắc là "went"',
     1, NOW(), NOW()),
(12, 3, 'Choose the correct sentence:',
     'She dont like coffee', 'She doesnt like coffee', 'She not like coffee', 'She is not like coffee', 'B',
     'Với chủ ngữ số ít thì dùng "does not" = "doesn''t"',
     2, NOW(), NOW()),
(13, 3, '"Beautiful" is a/an ___.',
     'Noun', 'Verb', 'Adjective', 'Adverb', 'C',
     '"Beautiful" là tính từ (adjective)',
     3, NOW(), NOW()),
(14, 3, 'Complete: "They ___ to school every day."',
     'Go', 'Goes', 'Going', 'Went', 'A',
     'Chủ ngữ "They" (số nhiều) đi với động từ nguyên mẫu không chia',
     4, NOW(), NOW()),
(15, 3, 'What does "How are you?" express?',
     'Asking name', 'Asking condition/health', 'Asking time', 'Asking location', 'B',
     '"How are you?" là câu hỏi về tình trạng sức khỏe/tình hình',
     5, NOW(), NOW()),
-- Mock Test 4: English B1 (5 questions)
(16, 4, 'Although it rained, ___ went to the market.',
     'they', 'them', 'their', 'themselves', 'A',
     '"Although it rained" là mệnh đề nhượng bộ, "they" là chủ ngữ độc lập',
     1, NOW(), NOW()),
(17, 4, 'If I ___ rich, I would travel the world.',
     'am', 'was', 'were', 'be', 'C',
     'Câu điều kiện loại 2 (giả định hư cấu): dùng "were" cho tất cả chủ ngữ',
     2, NOW(), NOW()),
(18, 4, 'The word "responsibility" is a/an ___ word.',
     'one-syllable', 'two-syllable', 'three-syllable', 'four-syllable', 'C',
     'respons-i-bil-i-ty: 4 âm tiết. Nhưng trọng âm rơi vào "si", đếm 3: re-spon-si-bil-i-ty',
     3, NOW(), NOW()),
(19, 4, 'She suggested ___ the meeting until next week.',
     'to postpone', 'postponing', 'postpone', 'postponed', 'B',
     '"Suggest + V-ing": suggest + danh động từ',
     4, NOW(), NOW()),
(20, 4, 'By the time we arrived, the movie ___.',
     'started', 'was starting', 'had started', 'has started', 'C',
     '"By the time" (đến lúc) + past perfect: hành động hoàn thành trước một thời điểm trong quá khứ',
     5, NOW(), NOW());

-- ================================================================
-- 19. MOCK_TEST_ATTEMPTS
-- status = IN_PROGRESS or COMPLETED
-- started_at = CreationTimestamp
-- submitted_at / completed_at = when submitted
-- score = điểm raw (integer)
-- correct_answers = số câu đúng (integer)
-- max_score = tổng số câu hỏi
-- total_questions = tổng số câu hỏi
-- ================================================================
INSERT INTO mock_test_attempts (id, mock_test_id, student_user_id, center_id, status,
                                 score, max_score, total_questions, correct_answers,
                                 test_title_snapshot, time_spent_seconds, started_at,
                                 submitted_at, completed_at) VALUES
-- Student 6 completed Mock Test 1 (score=4/5)
(1, 1, 6, 1, 'COMPLETED',  4, 5, 5, 4, 'Mock Test TOÁN A1 - Đại Số Cơ Bản',
    1800, '2026-06-15 09:00:00', '2026-06-15 09:30:00', '2026-06-15 09:30:00'),
-- Student 7 completed Mock Test 1 (perfect 5/5)
(2, 1, 7, 1, 'COMPLETED',  5, 5, 5, 5, 'Mock Test TOÁN A1 - Đại Số Cơ Bản',
    1620, '2026-06-15 10:00:00', '2026-06-15 10:27:00', '2026-06-15 10:27:00'),
-- Student 7 completed Mock Test 2 (score=4/5)
(3, 2, 7, 1, 'COMPLETED',  4, 5, 5, 4, 'Mock Test TOÁN B1 - Giải Tích',
    2400, '2026-06-16 13:00:00', '2026-06-16 13:40:00', '2026-06-16 13:40:00'),
-- Student 8 in progress Mock Test 3
(4, 3, 8, 1, 'IN_PROGRESS', 0, 5, 5, 0, 'Mock Test ANH A1 - Từ Vựng',
    600, '2026-06-27 14:00:00', NULL, NULL),
-- Student 9 completed Mock Test 3 (score=3/5)
(5, 3, 9, 1, 'COMPLETED',  3, 5, 5, 3, 'Mock Test ANH A1 - Từ Vựng',
    1200, '2026-06-20 10:00:00', '2026-06-20 10:20:00', '2026-06-20 10:20:00'),
-- Student 10 completed Mock Test 4 (score=4/5)
(6, 4, 10, 1, 'COMPLETED',  4, 5, 5, 4, 'Mock Test ANH B1 - Ngữ Pháp',
    1800, '2026-06-21 15:00:00', '2026-06-21 15:30:00', '2026-06-21 15:30:00'),
-- Student 11 completed Mock Test 4 (perfect 5/5)
(7, 4, 11, 1, 'COMPLETED',  5, 5, 5, 5, 'Mock Test ANH B1 - Ngữ Pháp',
    1500, '2026-06-22 09:00:00', '2026-06-22 09:25:00', '2026-06-22 09:25:00');

-- ================================================================
-- 20. MOCK_TEST_ATTEMPT_ANSWERS
-- correct_answer = 'A', 'B', 'C', 'D'
-- is_correct = 0 or 1
-- ================================================================
-- NOTE: is_correct = bit(1) in MySQL, must use b'1' or b'0'
INSERT INTO mock_test_attempt_answers (id, attempt_id, question_id, question_text,
                                        student_answer, is_correct, correct_answer,
                                        created_at, updated_at) VALUES
-- Attempt 1 (Student 6, Test 1): answered 4/5 correctly
(1,  1, 1, 'Hệ phương trình 2x + 3y = 13 và x - y = 2 có nghiệm là?', 'A', b'1', 'A', NOW(), NOW()),
(2,  1, 2, 'Ma trận đơn vị cấp 3 có bao nhiêu phần tử bằng 1 trên đường chéo chính?', 'C', b'1', 'C', NOW(), NOW()),
(3,  1, 3, 'Tính det([[2,1],[3,4]])', 'A', b'1', 'A', NOW(), NOW()),
(4,  1, 4, 'Hạng của ma trận [[1,2,3],[4,5,6],[7,8,9]] là?', 'B', b'1', 'B', NOW(), NOW()),
(5,  1, 5, 'Nghiệm của phương trình ma trận AX=B khi det(A)≠0 là?', 'B', b'0', 'A', NOW(), NOW()),
-- Attempt 2 (Student 7, Test 1): perfect score
(6,  2, 1, 'Hệ phương trình 2x + 3y = 13 và x - y = 2 có nghiệm là?', 'A', b'1', 'A', NOW(), NOW()),
(7,  2, 2, 'Ma trận đơn vị cấp 3 có bao nhiêu phần tử bằng 1 trên đường chéo chính?', 'C', b'1', 'C', NOW(), NOW()),
(8,  2, 3, 'Tính det([[2,1],[3,4]])', 'A', b'1', 'A', NOW(), NOW()),
(9,  2, 4, 'Hạng của ma trận [[1,2,3],[4,5,6],[7,8,9]] là?', 'B', b'1', 'B', NOW(), NOW()),
(10, 2, 5, 'Nghiệm của phương trình ma trận AX=B khi det(A)≠0 là?', 'A', b'1', 'A', NOW(), NOW()),
-- Attempt 3 (Student 7, Test 2)
(11, 3, 6, 'Tính lim(x→0) sin(3x)/x',  'B', b'1', 'B', NOW(), NOW()),
(12, 3, 7, 'Đạo hàm của f(x)=e^(2x).sin(x) là?', 'A', b'1', 'A', NOW(), NOW()),
(13, 3, 8, 'Tích phân ∫₀¹ x² dx = ?', 'B', b'1', 'B', NOW(), NOW()),
(14, 3, 9, 'Chuỗi ∑(n=1→∞) 1/n² hội tụ không?', 'C', b'1', 'C', NOW(), NOW()),
(15, 3, 10, 'Nghiệm của phương trình vi phân y''=2y, y(0)=1 là?', 'B', b'0', 'A', NOW(), NOW()),
-- Attempt 4 (Student 8, Test 3 - IN PROGRESS: answered 2)
(16, 4, 11, 'What is the past tense of "go"?', 'D', b'1', 'D', NOW(), NOW()),
(17, 4, 12, 'Choose the correct sentence:', 'B', b'1', 'B', NOW(), NOW()),
-- Attempt 5 (Student 9, Test 3)
(18, 5, 11, 'What is the past tense of "go"?', 'D', b'1', 'D', NOW(), NOW()),
(19, 5, 12, 'Choose the correct sentence:', 'A', b'0', 'B', NOW(), NOW()),
(20, 5, 13, '"Beautiful" is a/an ___.' , 'C', b'1', 'C', NOW(), NOW()),
(21, 5, 14, 'Complete: "They ___ to school every day."', 'A', b'1', 'A', NOW(), NOW()),
(22, 5, 15, 'What does "How are you?" express?', 'A', b'0', 'B', NOW(), NOW()),
-- Attempt 6 (Student 10, Test 4)
(23, 6, 16, 'Although it rained, ___ went to the market.', 'A', b'1', 'A', NOW(), NOW()),
(24, 6, 17, 'If I ___ rich, I would travel the world.', 'C', b'1', 'C', NOW(), NOW()),
(25, 6, 18, 'The word "responsibility" is a/an ___ word.', 'C', b'1', 'C', NOW(), NOW()),
(26, 6, 19, 'She suggested ___ the meeting until next week.', 'B', b'1', 'B', NOW(), NOW()),
(27, 6, 20, 'By the time we arrived, the movie ___.' , 'C', b'1', 'C', NOW(), NOW()),
-- Attempt 7 (Student 11, Test 4) - perfect
(28, 7, 16, 'Although it rained, ___ went to the market.', 'A', b'1', 'A', NOW(), NOW()),
(29, 7, 17, 'If I ___ rich, I would travel the world.', 'C', b'1', 'C', NOW(), NOW()),
(30, 7, 18, 'The word "responsibility" is a/an ___ word.', 'C', b'1', 'C', NOW(), NOW()),
(31, 7, 19, 'She suggested ___ the meeting until next week.', 'B', b'1', 'B', NOW(), NOW()),
(32, 7, 20, 'By the time we arrived, the movie ___.' , 'C', b'1', 'C', NOW(), NOW());

-- ================================================================
-- 21. STUDENT_DOCUMENTS
-- document_type = PDF, VIDEO, OTHER
-- clazz_id -> Class.id (nullable)
-- ================================================================
INSERT INTO student_documents (id, student_user_id, center_id, clazz_id,
                                document_type, title, file_url, description, created_at) VALUES
(1, 6, 1, 1, 'PDF', 'Bài tập Đại Số Tuần 1 - NguyenVanE',
    'https://storage.owlexa.vn/docs/math-a1/week1-nguyen-van-e.pdf',
    'Bài tập đại số tuần 1 của học sinh Nguyen Minh E', NOW()),
(2, 7, 1, 2, 'PDF', 'Bài Tập Giải Tích - LeVietF',
    'https://storage.owlexa.vn/docs/math-b1/bt-giai-tich-levietf.pdf',
    'Bài tập giải tích của học sinh Le Viet F', NOW()),
(3, 8, 1, 3, 'PDF', 'Essay Draft - PhamThiG',
    'https://storage.owlexa.vn/docs/english-a1/essay-draft-phamthig.pdf',
    'Bản nháp bài luận tiếng Anh của Pham Thi G', NOW()),
(4, 9, 1, NULL, 'VIDEO', 'Video Thuyết Trình - DoDucH',
    'https://storage.owlexa.vn/docs/general/video-thuyet-trinh-doduch.mp4',
    'Video bài thuyết trình của Do Duc H', NOW()),
(5, 10, 1, 4, 'OTHER', 'Project Report - BuiHoaiI',
    'https://storage.owlexa.vn/docs/english-b1/project-report-buihoaii.zip',
    'File báo cáo project tiếng Anh của Bui Hoai I', NOW());

-- ================================================================
-- COMMIT
-- ================================================================
COMMIT;

-- ================================================================
-- VERIFICATION QUERIES
-- ================================================================
SELECT '=== VERIFICATION ===' AS info;

SELECT COUNT(*) AS total_users            FROM users;
SELECT COUNT(*) AS total_centers         FROM centers;
SELECT COUNT(*) AS total_memberships     FROM membership;
SELECT COUNT(*) AS total_permissions      FROM permissions;
SELECT COUNT(*) AS total_classes          FROM classes;
SELECT COUNT(*) AS total_schedules       FROM schedules;
SELECT COUNT(*) AS total_enrollments     FROM class_enrollments;
SELECT COUNT(*) AS total_attendances     FROM attendances;
SELECT COUNT(*) AS total_fee_records     FROM fee_records;
SELECT COUNT(*) AS total_payments        FROM payments;
SELECT COUNT(*) AS total_essay_rubrics   FROM essay_rubrics;
SELECT COUNT(*) AS total_essay_criteria  FROM essay_rubric_criteria;
SELECT COUNT(*) AS total_essay_submissions FROM essay_submissions;
SELECT COUNT(*) AS total_essay_gradings  FROM essay_grading_results;
SELECT COUNT(*) AS total_essay_scores    FROM essay_criteria_scores;
SELECT COUNT(*) AS total_mock_tests      FROM mock_tests;
SELECT COUNT(*) AS total_mock_questions  FROM mock_test_questions;
SELECT COUNT(*) AS total_mock_attempts   FROM mock_test_attempts;
SELECT COUNT(*) AS total_mock_answers    FROM mock_test_attempt_answers;
SELECT COUNT(*) AS total_documents       FROM student_documents;

-- Show sample users for login testing
SELECT id, phone_number, full_name, role
FROM users
WHERE role IN ('OWNER', 'TEACHER', 'STUDENT', 'CASHIER', 'ADMIN')
ORDER BY role, id;

-- Show owner user for center
SELECT u.id, u.phone_number, u.full_name, u.role, c.name AS center_name
FROM users u
JOIN centers c ON c.owner_user_id = u.id;

-- Show payments summary
SELECT p.id, u.full_name AS student, fr.month, p.amount, p.method, cu.full_name AS cashier
FROM payments p
JOIN users u ON u.id = p.student_user_id
JOIN users cu ON cu.id = p.collected_by_user_id
JOIN fee_records fr ON fr.id = p.fee_record_id
ORDER BY p.id;

SET autocommit = 1;
