-- ================================================================
-- OWLEXA ENGLISH LEARNING CENTER - SEED DATA v5.0
-- Generated: 2026-07-16
-- Rebuilt from the actual JPA entities in owlexa-backend (main branch)
-- so it matches the schema Hibernate generates (ddl-auto=update),
-- NOT the older sql/seed_data_v4_english.sql, which is now stale.
--
-- WHAT CHANGED vs v4.0 (verified against src/main/.../entity/*.java):
--   1. NEW `courses` table (modules/course) - a global curriculum
--      template. `classes.course_id` now REQUIRES a course
--      (ClassRequest.courseId is @NotNull) - v4 had no such table.
--   2. NEW `rooms` table (modules/room), scoped per center.
--      `schedules.room_id` is now a FK to rooms (ScheduleRequest.roomId
--      is @NotNull) - v4 stored room as a free-text varchar.
--   3. `classes.is_active` (boolean) no longer exists. It was replaced
--      by `classes.status` (VARCHAR, enum ClassStatus: PLANNING, OPEN,
--      FULL, IN_PROGRESS, FINISHED, ARCHIVED, CANCELLED).
--   4. `permissions.code` values in v4 (MANAGE_STUDENTS, VIEW_REPORTS,
--      MARK_ATTENDANCE, ...) are never referenced anywhere in the
--      codebase. The only permission code actually checked in code is
--      CENTER_CREATE (CenterController + AuthService.getDefaultPermissionCode).
--      This file uses the real codes: CENTER_CREATE, VIEW_STUDENT,
--      EDIT_FEE, VIEW_SALARY, MANAGE_CLASS, MANAGE_TEACHER.
--   All other tables (users, attendances, fee_records, payments,
--   essay_*, mock_test_*, student_documents) were already correct
--   in v4 and are carried over unchanged.
--
-- BCrypt password for ALL users: "password123"
-- Hash: $2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS
--
-- ALL CONTENT IS ENGLISH LEARNING ONLY:
--   TOEIC, VSTEP, English Grammar, Vocabulary, Pronunciation,
--   Business English, English Communication
-- ZERO math, literature, physics, chemistry, biology, or
-- Vietnamese academic subjects.
-- ================================================================

SET autocommit = 0;
START TRANSACTION;
SET NAMES 'utf8mb4';
SET FOREIGN_KEY_CHECKS = 0;

-- Clean existing data (bottom-up to respect FK)
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
DELETE FROM teacher_attendances;
DELETE FROM class_enrollments;
DELETE FROM schedules;
DELETE FROM classes;
DELETE FROM rooms;
DELETE FROM courses;
DELETE FROM teacher_center_profile;
DELETE FROM user_permission;
DELETE FROM permissions;
DELETE FROM membership;
DELETE FROM centers;
DELETE FROM user_sessions;
DELETE FROM users;

SET FOREIGN_KEY_CHECKS = 1;

-- Reset auto-increment
ALTER TABLE users AUTO_INCREMENT = 1;
ALTER TABLE permissions AUTO_INCREMENT = 1;
ALTER TABLE user_permission AUTO_INCREMENT = 1;
ALTER TABLE centers AUTO_INCREMENT = 1;
ALTER TABLE membership AUTO_INCREMENT = 1;
ALTER TABLE courses AUTO_INCREMENT = 1;
ALTER TABLE rooms AUTO_INCREMENT = 1;
ALTER TABLE teacher_center_profile AUTO_INCREMENT = 1;
ALTER TABLE classes AUTO_INCREMENT = 1;
ALTER TABLE schedules AUTO_INCREMENT = 1;
ALTER TABLE class_enrollments AUTO_INCREMENT = 1;
ALTER TABLE attendances AUTO_INCREMENT = 1;
ALTER TABLE teacher_attendances AUTO_INCREMENT = 1;
ALTER TABLE fee_records AUTO_INCREMENT = 1;
ALTER TABLE payments AUTO_INCREMENT = 1;
ALTER TABLE essay_rubrics AUTO_INCREMENT = 1;
ALTER TABLE essay_rubric_criteria AUTO_INCREMENT = 1;
ALTER TABLE essay_submissions AUTO_INCREMENT = 1;
ALTER TABLE essay_grading_results AUTO_INCREMENT = 1;
ALTER TABLE essay_criteria_scores AUTO_INCREMENT = 1;
ALTER TABLE mock_tests AUTO_INCREMENT = 1;
ALTER TABLE mock_test_questions AUTO_INCREMENT = 1;
ALTER TABLE mock_test_attempts AUTO_INCREMENT = 1;
ALTER TABLE mock_test_attempt_answers AUTO_INCREMENT = 1;
ALTER TABLE student_documents AUTO_INCREMENT = 1;

-- ================================================================
-- 1. USERS (59 total)
-- Password for ALL: "password123"
-- BCrypt: $2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS
-- ================================================================
INSERT INTO users (phone_number, email, full_name, password, role) VALUES
-- Admin (1)
('0000000001', 'admin@owlexa.vn',              'System Admin',               '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'ADMIN'),

-- Owner Center 1 - HCM (2)
('0903000001', 'owner.hcm@owlexa.vn',          'Nguyen Minh Tuan',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'OWNER'),

-- Owner Center 2 - Hanoi (3)
('0903000002', 'owner.hanoi@owlexa.vn',        'Tran Thi Lan Huong',        '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'OWNER'),

-- Teachers Center 1 - HCM (4-5)
('0904000001', 'teacher.david@owlexa.vn',      'David Nguyen',              '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'TEACHER'),
('0904000002', 'teacher.emma@owlexa.vn',       'Emma Tran',                 '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'TEACHER'),

-- Teachers Center 2 - Hanoi (6-7)
('0904000003', 'teacher.james@owlexa.vn',      'James Le',                  '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'TEACHER'),
('0904000004', 'teacher.sophie@owlexa.vn',     'Sophie Pham',              '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'TEACHER'),

-- Cashiers (8-9)
('0905000001', 'cashier.hcm@owlexa.vn',        'Mai Thi Thanh',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'CASHIER'),
('0905000002', 'cashier.hanoi@owlexa.vn',      'Pham Van Hung',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'CASHIER'),

-- Students Center 1 - HCM (10-34): 25 students
('0906100001', 'anh.nguyen@email.com',         'Nguyen Van Anh',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100002', 'bao.tran@email.com',           'Tran Quoc Bao',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100003', 'chi.le@email.com',             'Le Linh Chi',              '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100004', 'duc.pham@email.com',           'Pham Minh Duc',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100005', 'ha.hoang@email.com',           'Hoang Thanh Ha',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100006', 'khanh.vu@email.com',           'Vu Duy Khanh',             '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100007', 'lan.do@email.com',             'Do Ngoc Lan',              '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100008', 'minh.bui@email.com',           'Bui Quang Minh',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100009', 'nga.dang@email.com',           'Dang Thuy Nga',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100010', 'phu.nguyen@email.com',         'Nguyen Hong Phu',          '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100011', 'quynh.tran@email.com',         'Tran Nhu Quynh',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100012', 'tam.le@email.com',             'Le Thanh Tam',             '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100013', 'thao.pham@email.com',          'Pham Phuong Thao',         '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100014', 'trung.hoang@email.com',        'Hoang Duc Trung',          '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100015', 'uyen.vu@email.com',            'Vu Kim Uyen',              '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100016', 'vy.do@email.com',              'Do Tuong Vy',              '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100017', 'hoang.bui@email.com',          'Bui The Hoang',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100018', 'tuan.dang@email.com',          'Dang Anh Tuan',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100019', 'hien.nguyen@email.com',        'Nguyen Thi Hien',          '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100020', 'thang.tran@email.com',         'Tran Huu Thang',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100021', 'nhung.le@email.com',           'Le Hong Nhung',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100022', 'son.pham@email.com',           'Pham Ngoc Son',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100023', 'trinh.hoang@email.com',        'Hoang Thi Trinh',          '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100024', 'long.vu@email.com',            'Vu Thanh Long',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906100025', 'mai.do@email.com',             'Do Xuan Mai',              '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),

-- Students Center 2 - Hanoi (35-59): 25 students
('0906200001', 'linh.nguyen@email.com',        'Nguyen Khanh Linh',        '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200002', 'nam.tran@email.com',           'Tran Hoai Nam',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200003', 'ha.le@email.com',              'Le Thi Ha',                '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200004', 'cuong.pham@email.com',         'Pham Manh Cuong',          '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200005', 'thuy.hoang@email.com',         'Hoang Minh Thuy',          '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200006', 'vu.vu@email.com',              'Vu Quoc Vu',               '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200007', 'trang.do@email.com',           'Do Thu Trang',             '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200008', 'huy.bui@email.com',            'Bui Quang Huy',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200009', 'dung.dang@email.com',          'Dang Tien Dung',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200010', 'anh2.nguyen@email.com',        'Nguyen Lan Anh',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200011', 'tien.tran@email.com',          'Tran Van Tien',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200012', 'ngoc.le@email.com',            'Le Bao Ngoc',              '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200013', 'phuc.pham@email.com',          'Pham Hong Phuc',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200014', 'thu.hoang@email.com',          'Hoang Thi Thu',            '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200015', 'nhat.vu@email.com',            'Vu Minh Nhat',             '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200016', 'ngan.do@email.com',            'Do Kim Ngan',              '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200017', 'hieu.bui@email.com',           'Bui Trung Hieu',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200018', 'diem.dang@email.com',          'Dang My Diem',             '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200019', 'phong.nguyen@email.com',       'Nguyen Dinh Phong',        '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200020', 'yen.tran@email.com',           'Tran Hai Yen',             '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200021', 'khoa.le@email.com',            'Le Dang Khoa',             '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200022', 'ly.pham@email.com',            'Pham Thi Ly',              '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200023', 'dat.hoang@email.com',          'Hoang Thanh Dat',          '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200024', 'thao2.vu@email.com',           'Vu Phuong Thao',           '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT'),
('0906200025', 'tai.do@email.com',             'Do Van Tai',               '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', 'STUDENT');

-- ================================================================
-- 2. PERMISSIONS (aligned with V8__seed_rbac_data.sql — 57 codes)
-- Uses INSERT IGNORE so it is safe to run after Flyway migrations.
-- ================================================================
INSERT IGNORE INTO permissions (code, description) VALUES
('CENTER_VIEW', 'View center details'),
('CENTER_SETTINGS_UPDATE', 'Update center settings'),
('USER_VIEW', 'View users'),
('USER_CREATE', 'Create users'),
('USER_UPDATE', 'Update users'),
('USER_DEACTIVATE', 'Deactivate users'),
('STUDENT_VIEW', 'View students'),
('STUDENT_ENROLL', 'Enroll students'),
('STUDENT_UPDATE', 'Update students'),
('STUDENT_APPROVE_LEAVE', 'Approve student leave'),
('TEACHER_VIEW', 'View teachers'),
('TEACHER_ASSIGN', 'Assign teachers'),
('TEACHER_SCHEDULE_EDIT', 'Edit teacher schedule'),
('COURSE_VIEW', 'View courses'),
('COURSE_CREATE', 'Create courses'),
('COURSE_EDIT', 'Edit courses'),
('COURSE_ARCHIVE', 'Archive courses'),
('ROOM_VIEW', 'View rooms'),
('ROOM_BOOK', 'Book rooms'),
('ROOM_MAINTENANCE', 'Manage room maintenance'),
('CLASS_VIEW', 'View classes'),
('CLASS_CREATE', 'Create classes'),
('CLASS_OPEN', 'Open classes'),
('CLASS_START', 'Start classes'),
('CLASS_FINISH', 'Finish classes'),
('CLASS_ARCHIVE', 'Archive classes'),
('SCHEDULE_VIEW', 'View schedules'),
('SCHEDULE_GENERATE', 'Generate schedules'),
('SCHEDULE_EDIT_SINGLE', 'Edit single schedule'),
('SCHEDULE_EDIT_BULK', 'Bulk edit schedules'),
('ATTENDANCE_VIEW', 'View attendance'),
('ATTENDANCE_MARK', 'Mark attendance'),
('ATTENDANCE_OVERRIDE', 'Override attendance'),
('TEACHER_ATT_VIEW', 'View teacher attendance'),
('TEACHER_ATT_MARK', 'Mark teacher attendance'),
('TEACHER_ATT_OVERRIDE', 'Override teacher attendance'),
('FEE_VIEW', 'View fees'),
('FEE_GENERATE', 'Generate fees'),
('FEE_ADJUST', 'Adjust fees'),
('PAYMENT_VIEW', 'View payments'),
('PAYMENT_COLLECT', 'Collect payments'),
('PAYMENT_REFUND', 'Refund payments'),
('PAYMENT_VOID', 'Void payments'),
('ESSAY_VIEW', 'View essays'),
('ESSAY_SUBMIT', 'Submit essays'),
('ESSAY_GRADE', 'Grade essays'),
('TEST_VIEW', 'View tests'),
('TEST_CREATE', 'Create tests'),
('TEST_GRADE', 'Grade tests'),
('DOCUMENT_VIEW', 'View documents'),
('DOCUMENT_UPLOAD', 'Upload documents'),
('DOCUMENT_DELETE', 'Delete documents'),
('REPORT_ACADEMIC_VIEW', 'View academic reports'),
('REPORT_FINANCE_VIEW', 'View finance reports'),
('SALARY_VIEW', 'View salaries'),
('SALARY_CALCULATE', 'Calculate salaries'),
('SALARY_APPROVE', 'Approve salaries'),
('DASHBOARD_ACADEMIC', 'View academic dashboard'),
('DASHBOARD_FINANCE', 'View finance dashboard'),
('DASHBOARD_OWNER', 'View owner dashboard');

-- ================================================================
-- 2b. ROLE PERMISSIONS (aligned with V8__seed_rbac_data.sql)
-- Uses INSERT IGNORE so it is safe to run after Flyway migrations.
-- ================================================================

-- OWNER: all permissions
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'OWNER', id FROM permissions;

-- MANAGER: all except CENTER_SETTINGS_UPDATE, SALARY_APPROVE, PAYMENT_VOID
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'MANAGER', id FROM permissions
WHERE code NOT IN ('CENTER_SETTINGS_UPDATE', 'SALARY_APPROVE', 'PAYMENT_VOID');

-- ACADEMIC_STAFF: academic scope
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'ACADEMIC_STAFF', id FROM permissions
WHERE code IN (
    'STUDENT_VIEW', 'STUDENT_ENROLL', 'STUDENT_UPDATE', 'STUDENT_APPROVE_LEAVE',
    'TEACHER_VIEW', 'TEACHER_ASSIGN', 'TEACHER_SCHEDULE_EDIT',
    'COURSE_VIEW', 'COURSE_CREATE', 'COURSE_EDIT', 'COURSE_ARCHIVE',
    'ROOM_VIEW', 'ROOM_BOOK', 'ROOM_MAINTENANCE',
    'CLASS_VIEW', 'CLASS_CREATE', 'CLASS_OPEN', 'CLASS_START', 'CLASS_FINISH', 'CLASS_ARCHIVE',
    'SCHEDULE_VIEW', 'SCHEDULE_GENERATE', 'SCHEDULE_EDIT_SINGLE', 'SCHEDULE_EDIT_BULK',
    'ATTENDANCE_VIEW', 'ATTENDANCE_MARK', 'ATTENDANCE_OVERRIDE',
    'TEACHER_ATT_VIEW', 'TEACHER_ATT_MARK', 'TEACHER_ATT_OVERRIDE',
    'REPORT_ACADEMIC_VIEW', 'DASHBOARD_ACADEMIC'
);

-- CASHIER: finance scope
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'CASHIER', id FROM permissions
WHERE code IN (
    'STUDENT_VIEW',
    'FEE_VIEW', 'FEE_GENERATE', 'FEE_ADJUST',
    'PAYMENT_VIEW', 'PAYMENT_COLLECT', 'PAYMENT_REFUND',
    'REPORT_FINANCE_VIEW', 'DASHBOARD_FINANCE'
);

-- TEACHER: teaching scope
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'TEACHER', id FROM permissions
WHERE code IN (
    'CLASS_VIEW', 'SCHEDULE_VIEW', 'ATTENDANCE_MARK',
    'ESSAY_VIEW', 'ESSAY_GRADE',
    'TEST_VIEW', 'TEST_GRADE',
    'DOCUMENT_VIEW', 'DOCUMENT_UPLOAD'
);

-- STUDENT: limited scope
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'STUDENT', id FROM permissions
WHERE code IN (
    'STUDENT_VIEW', 'SCHEDULE_VIEW',
    'ESSAY_VIEW', 'ESSAY_SUBMIT',
    'PAYMENT_VIEW'
);

-- ================================================================
-- 3. CENTERS
-- ================================================================
INSERT INTO centers (owner_user_id, name, subdomain, created_at) VALUES
(2, 'Owlexa English Center - Ho Chi Minh', 'hcm',   NOW()),
(3, 'Owlexa English Center - Ha Noi',      'hanoi', NOW());

-- ================================================================
-- 4. MEMBERSHIPS
-- ================================================================
INSERT INTO membership (center_id, user_id, joined_by_user_id, joined_at) VALUES
-- Center 1 (HCM): Owner 2 + Teachers 4,5 + Cashier 8 + Students 10-34
(1, 2,  2,  NOW()), (1, 4,  2,  NOW()), (1, 5,  2,  NOW()), (1, 8,  2,  NOW()),
(1, 10, 2,  NOW()), (1, 11, 2,  NOW()), (1, 12, 2,  NOW()), (1, 13, 2,  NOW()),
(1, 14, 2,  NOW()), (1, 15, 2,  NOW()), (1, 16, 2,  NOW()), (1, 17, 2,  NOW()),
(1, 18, 2,  NOW()), (1, 19, 2,  NOW()), (1, 20, 2,  NOW()), (1, 21, 2,  NOW()),
(1, 22, 2,  NOW()), (1, 23, 2,  NOW()), (1, 24, 2,  NOW()), (1, 25, 2,  NOW()),
(1, 26, 2,  NOW()), (1, 27, 2,  NOW()), (1, 28, 2,  NOW()), (1, 29, 2,  NOW()),
(1, 30, 2,  NOW()), (1, 31, 2,  NOW()), (1, 32, 2,  NOW()), (1, 33, 2,  NOW()),
(1, 34, 2,  NOW()),
-- Center 2 (Hanoi): Owner 3 + Teachers 6,7 + Cashier 9 + Students 35-59
(2, 3,  3,  NOW()), (2, 6,  3,  NOW()), (2, 7,  3,  NOW()), (2, 9,  3,  NOW()),
(2, 35, 3,  NOW()), (2, 36, 3,  NOW()), (2, 37, 3,  NOW()), (2, 38, 3,  NOW()),
(2, 39, 3,  NOW()), (2, 40, 3,  NOW()), (2, 41, 3,  NOW()), (2, 42, 3,  NOW()),
(2, 43, 3,  NOW()), (2, 44, 3,  NOW()), (2, 45, 3,  NOW()), (2, 46, 3,  NOW()),
(2, 47, 3,  NOW()), (2, 48, 3,  NOW()), (2, 49, 3,  NOW()), (2, 50, 3,  NOW()),
(2, 51, 3,  NOW()), (2, 52, 3,  NOW()), (2, 53, 3,  NOW()), (2, 54, 3,  NOW()),
(2, 55, 3,  NOW()), (2, 56, 3,  NOW()), (2, 57, 3,  NOW()), (2, 58, 3,  NOW()),
(2, 59, 3,  NOW());

-- ================================================================
-- 5. USER PERMISSION OVERRIDES
-- Role defaults are handled by role_permission (section 2b).
-- A row in user_permission means the permission is DISABLED for that user.
-- No rows = all role permissions are enabled.
-- No default overrides are needed for seed data.
-- ================================================================

-- ================================================================
-- 6. COURSES
-- Global curriculum templates (modules/course). NOT tenant-scoped -
-- both centers' classes reference these by course_id. `code` is
-- globally unique (CourseService.existsByCode).
-- ================================================================
INSERT INTO courses (code, name, level, description, default_duration, default_monthly_fee, default_max_students, is_active, created_at, updated_at) VALUES
('TOEIC-FOUND', 'TOEIC Foundation',          'BEGINNER',     'Build fundamental English skills for TOEIC test preparation. Focus on basic grammar, vocabulary, and listening comprehension.', 12, 1200000.00, 25, b'1', NOW(), NOW()),
('TOEIC-500',   'TOEIC 500+',                'INTERMEDIATE', 'Target TOEIC score 500-650. Intensive practice on Listening Part 1-4 and Reading Part 5-6.',                                     12, 1800000.00, 20, b'1', NOW(), NOW()),
('TOEIC-650',   'TOEIC 650+',                'INTERMEDIATE', 'Target TOEIC score 650-800. Advanced strategies for Reading Part 7 and Listening Part 3-4.',                                     12, 2200000.00, 20, b'1', NOW(), NOW()),
('ENG-COMM',    'English Communication',     'BEGINNER',     'Practical English for daily communication. Focus on speaking fluency, pronunciation, and conversational skills.',                10, 1000000.00, 30, b'1', NOW(), NOW()),
('VSTEP-B1',    'VSTEP B1 Preparation',      'INTERMEDIATE', 'Prepare for VSTEP B1 certificate. Covers Reading, Listening, Writing, and Speaking modules.',                                    14, 1500000.00, 25, b'1', NOW(), NOW()),
('TOEIC-800',   'TOEIC 800+',                'ADVANCED',     'Target TOEIC score 800-990. Advanced test-taking strategies, business vocabulary, and complex grammar structures.',              14, 2500000.00, 20, b'1', NOW(), NOW()),
('VSTEP-B2',    'VSTEP B2 Preparation',      'ADVANCED',     'Prepare for VSTEP B2 certificate. Advanced academic English with essay writing and presentation skills.',                        16, 2000000.00, 20, b'1', NOW(), NOW()),
('BIZ-ENG',     'Business English',          'INTERMEDIATE', 'English for professional environments. Email writing, meeting skills, negotiation, and business presentations.',                  12, 2000000.00, 25, b'1', NOW(), NOW()),
('ENG-GRAM',    'English Grammar & Writing', 'INTERMEDIATE', 'Master English grammar structures and develop academic writing skills including essays, reports, and formal letters.',            12, 1500000.00, 25, b'1', NOW(), NOW()),
('TOEIC-SW',    'TOEIC Speaking & Writing',  'ADVANCED',     'Prepare for TOEIC Speaking and Writing tests. Pronunciation, Q&A responses, and email composition.',                             10, 2200000.00, 20, b'1', NOW(), NOW());

-- ================================================================
-- 7. ROOMS
-- Tenant-scoped (modules/room). `code` is unique per center
-- (RoomService.existsByCodeAndCenter_Id), not globally.
-- Center 1 (HCM) -> rooms 1-5, Center 2 (Hanoi) -> rooms 6-10.
-- ================================================================
INSERT INTO rooms (code, name, capacity, description, is_active, center_id, created_at, updated_at) VALUES
-- Center 1 - HCM
('101', 'Room 101', 25, 'Ground floor classroom, HCM campus', b'1', 1, NOW(), NOW()),
('102', 'Room 102', 20, 'Ground floor classroom, HCM campus', b'1', 1, NOW(), NOW()),
('201', 'Room 201', 20, 'Second floor classroom, HCM campus', b'1', 1, NOW(), NOW()),
('202', 'Room 202', 30, 'Second floor classroom, HCM campus', b'1', 1, NOW(), NOW()),
('103', 'Room 103', 25, 'Ground floor classroom, HCM campus', b'1', 1, NOW(), NOW()),
-- Center 2 - Hanoi
('301', 'Room 301', 20, 'Third floor classroom, Hanoi campus',  b'1', 2, NOW(), NOW()),
('302', 'Room 302', 20, 'Third floor classroom, Hanoi campus',  b'1', 2, NOW(), NOW()),
('401', 'Room 401', 25, 'Fourth floor classroom, Hanoi campus', b'1', 2, NOW(), NOW()),
('402', 'Room 402', 25, 'Fourth floor classroom, Hanoi campus', b'1', 2, NOW(), NOW()),
('303', 'Room 303', 20, 'Third floor classroom, Hanoi campus',  b'1', 2, NOW(), NOW());

-- ================================================================
-- 8. TEACHER CENTER PROFILES
-- ================================================================
INSERT INTO teacher_center_profile (teacher_user_id, center_id, salary, currency, created_at, updated_at) VALUES
(4, 1, 18000000.00, 'VND', NOW(), NOW()),
(5, 1, 20000000.00, 'VND', NOW(), NOW()),
(6, 2, 17000000.00, 'VND', NOW(), NOW()),
(7, 2, 19000000.00, 'VND', NOW(), NOW());

-- ================================================================
-- 9. CLASSES (ALL ENGLISH-ONLY)
-- Center 1 (HCM): 5 classes | Center 2 (Hanoi): 5 classes
-- course_id links 1:1 to the matching course in section 6 above.
-- status = IN_PROGRESS (not is_active - that column no longer
-- exists on the entity) since every class here already has
-- schedules, enrollments, attendance and grading history.
-- ================================================================
INSERT INTO classes (name, center_id, course_id, teacher_id, max_students, description, vstep_level, monthly_fee, status, create_at) VALUES
-- Center 1 - HCM
('TOEIC Foundation',        1, 1,  4, 25, 'Build fundamental English skills for TOEIC test preparation. Focus on basic grammar, vocabulary, and listening comprehension.', 'BEGINNER',      1200000.00, 'IN_PROGRESS', NOW()),
('TOEIC 500+',              1, 2,  4, 20, 'Target TOEIC score 500-650. Intensive practice on Listening Part 1-4 and Reading Part 5-6.',                  'INTERMEDIATE',  1800000.00, 'IN_PROGRESS', NOW()),
('TOEIC 650+',              1, 3,  5, 20, 'Target TOEIC score 650-800. Advanced strategies for Reading Part 7 and Listening Part 3-4.',                  'INTERMEDIATE',  2200000.00, 'IN_PROGRESS', NOW()),
('English Communication',   1, 4,  5, 30, 'Practical English for daily communication. Focus on speaking fluency, pronunciation, and conversational skills.', 'BEGINNER',      1000000.00, 'IN_PROGRESS', NOW()),
('VSTEP B1 Preparation',    1, 5,  4, 25, 'Prepare for VSTEP B1 certificate. Covers Reading, Listening, Writing, and Speaking modules.',               'INTERMEDIATE',  1500000.00, 'IN_PROGRESS', NOW()),
-- Center 2 - Hanoi
('TOEIC 800+',              2, 6,  6, 20, 'Target TOEIC score 800-990. Advanced test-taking strategies, business vocabulary, and complex grammar structures.', 'ADVANCED',  2500000.00, 'IN_PROGRESS', NOW()),
('VSTEP B2 Preparation',    2, 7,  6, 20, 'Prepare for VSTEP B2 certificate. Advanced academic English with essay writing and presentation skills.',    'ADVANCED',       2000000.00, 'IN_PROGRESS', NOW()),
('Business English',        2, 8,  7, 25, 'English for professional environments. Email writing, meeting skills, negotiation, and business presentations.', 'INTERMEDIATE',  2000000.00, 'IN_PROGRESS', NOW()),
('English Grammar & Writing',2, 9, 7, 25, 'Master English grammar structures and develop academic writing skills including essays, reports, and formal letters.', 'INTERMEDIATE', 1500000.00, 'IN_PROGRESS', NOW()),
('TOEIC Speaking & Writing', 2, 10, 6, 20, 'Prepare for TOEIC Speaking and Writing tests. Pronunciation, Q&A responses, and email composition.',        'ADVANCED',       2200000.00, 'IN_PROGRESS', NOW());

-- ================================================================
-- 10. SCHEDULES (2 per class = 20 schedules)
-- day_of_week uses Java DayOfWeek enum: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
-- room_id points at the rooms inserted in section 7 (FK, replaces
-- the old free-text `room` varchar column).
-- ================================================================
INSERT INTO schedules (class_id, center_id, teacher_user_id, room_id, day_of_week, start_time, end_time, is_active, created_at) VALUES
-- Center 1 - HCM
-- Class 1: TOEIC Foundation (Teacher David, Room 101, Mon & Wed)
(1, 1, 4, 1, 'MONDAY',    '08:00:00', '10:00:00', b'1', NOW()),
(1, 1, 4, 1, 'WEDNESDAY', '08:00:00', '10:00:00', b'1', NOW()),
-- Class 2: TOEIC 500+ (Teacher David, Room 102, Tue & Thu)
(2, 1, 4, 2, 'TUESDAY',   '08:00:00', '10:00:00', b'1', NOW()),
(2, 1, 4, 2, 'THURSDAY',  '08:00:00', '10:00:00', b'1', NOW()),
-- Class 3: TOEIC 650+ (Teacher Emma, Room 201, Mon & Wed)
(3, 1, 5, 3, 'MONDAY',    '13:30:00', '15:30:00', b'1', NOW()),
(3, 1, 5, 3, 'WEDNESDAY', '13:30:00', '15:30:00', b'1', NOW()),
-- Class 4: English Communication (Teacher Emma, Room 202, Tue & Thu)
(4, 1, 5, 4, 'TUESDAY',   '13:30:00', '15:00:00', b'1', NOW()),
(4, 1, 5, 4, 'THURSDAY',  '13:30:00', '15:00:00', b'1', NOW()),
-- Class 5: VSTEP B1 (Teacher David, Room 103, Fri & Sat)
(5, 1, 4, 5, 'FRIDAY',    '09:00:00', '11:00:00', b'1', NOW()),
(5, 1, 4, 5, 'SATURDAY',  '09:00:00', '11:00:00', b'1', NOW()),

-- Center 2 - Hanoi
-- Class 6: TOEIC 800+ (Teacher James, Room 301, Mon & Wed)
(6, 2, 6, 6, 'MONDAY',    '08:00:00', '10:00:00', b'1', NOW()),
(6, 2, 6, 6, 'WEDNESDAY', '08:00:00', '10:00:00', b'1', NOW()),
-- Class 7: VSTEP B2 (Teacher James, Room 302, Tue & Thu)
(7, 2, 6, 7, 'TUESDAY',   '08:00:00', '10:00:00', b'1', NOW()),
(7, 2, 6, 7, 'THURSDAY',  '08:00:00', '10:00:00', b'1', NOW()),
-- Class 8: Business English (Teacher Sophie, Room 401, Mon & Wed)
(8, 2, 7, 8, 'MONDAY',    '14:00:00', '16:00:00', b'1', NOW()),
(8, 2, 7, 8, 'WEDNESDAY', '14:00:00', '16:00:00', b'1', NOW()),
-- Class 9: English Grammar & Writing (Teacher Sophie, Room 402, Tue & Thu)
(9, 2, 7, 9, 'TUESDAY',   '14:00:00', '16:00:00', b'1', NOW()),
(9, 2, 7, 9, 'THURSDAY',  '14:00:00', '16:00:00', b'1', NOW()),
-- Class 10: TOEIC Speaking & Writing (Teacher James, Room 303, Fri & Sat)
(10, 2, 6, 10, 'FRIDAY',   '09:00:00', '11:00:00', b'1', NOW()),
(10, 2, 6, 10, 'SATURDAY', '09:00:00', '11:00:00', b'1', NOW());

-- ================================================================
-- 11. CLASS ENROLLMENTS
-- ~4-6 students per class, some students in 2 classes
-- ================================================================
INSERT INTO class_enrollments (student_user_id, class_id, center_id, enrolled_by_user_id, status, enrolled_at) VALUES
-- Center 1 Enrollments
-- Class 1: TOEIC Foundation (Students 10-15)
(10, 1, 1, 2, 'ACTIVE', NOW()), (11, 1, 1, 2, 'ACTIVE', NOW()), (12, 1, 1, 2, 'ACTIVE', NOW()),
(13, 1, 1, 2, 'ACTIVE', NOW()), (14, 1, 1, 2, 'ACTIVE', NOW()), (15, 1, 1, 2, 'ACTIVE', NOW()),
-- Class 2: TOEIC 500+ (Students 12,13,16,17,18,19) - some overlap
(12, 2, 1, 2, 'ACTIVE', NOW()), (13, 2, 1, 2, 'ACTIVE', NOW()), (16, 2, 1, 2, 'ACTIVE', NOW()),
(17, 2, 1, 2, 'ACTIVE', NOW()), (18, 2, 1, 2, 'ACTIVE', NOW()), (19, 2, 1, 2, 'ACTIVE', NOW()),
-- Class 3: TOEIC 650+ (Students 16,17,18,20,21,22)
(16, 3, 1, 2, 'ACTIVE', NOW()), (17, 3, 1, 2, 'ACTIVE', NOW()), (18, 3, 1, 2, 'ACTIVE', NOW()),
(20, 3, 1, 2, 'ACTIVE', NOW()), (21, 3, 1, 2, 'ACTIVE', NOW()), (22, 3, 1, 2, 'ACTIVE', NOW()),
-- Class 4: English Communication (Students 23-28)
(23, 4, 1, 2, 'ACTIVE', NOW()), (24, 4, 1, 2, 'ACTIVE', NOW()), (25, 4, 1, 2, 'ACTIVE', NOW()),
(26, 4, 1, 2, 'ACTIVE', NOW()), (27, 4, 1, 2, 'ACTIVE', NOW()), (28, 4, 1, 2, 'ACTIVE', NOW()),
-- Class 5: VSTEP B1 (Students 20,21,29,30,31,32,33,34)
(20, 5, 1, 2, 'ACTIVE', NOW()), (21, 5, 1, 2, 'ACTIVE', NOW()), (29, 5, 1, 2, 'ACTIVE', NOW()),
(30, 5, 1, 2, 'ACTIVE', NOW()), (31, 5, 1, 2, 'ACTIVE', NOW()), (32, 5, 1, 2, 'ACTIVE', NOW()),
(33, 5, 1, 2, 'ACTIVE', NOW()), (34, 5, 1, 2, 'ACTIVE', NOW()),
-- 1 dropped enrollment
(14, 4, 1, 2, 'DROPPED', NOW()),

-- Center 2 Enrollments
-- Class 6: TOEIC 800+ (Students 35-40)
(35, 6, 2, 3, 'ACTIVE', NOW()), (36, 6, 2, 3, 'ACTIVE', NOW()), (37, 6, 2, 3, 'ACTIVE', NOW()),
(38, 6, 2, 3, 'ACTIVE', NOW()), (39, 6, 2, 3, 'ACTIVE', NOW()), (40, 6, 2, 3, 'ACTIVE', NOW()),
-- Class 7: VSTEP B2 (Students 37,38,41,42,43,44)
(37, 7, 2, 3, 'ACTIVE', NOW()), (38, 7, 2, 3, 'ACTIVE', NOW()), (41, 7, 2, 3, 'ACTIVE', NOW()),
(42, 7, 2, 3, 'ACTIVE', NOW()), (43, 7, 2, 3, 'ACTIVE', NOW()), (44, 7, 2, 3, 'ACTIVE', NOW()),
-- Class 8: Business English (Students 45-50)
(45, 8, 2, 3, 'ACTIVE', NOW()), (46, 8, 2, 3, 'ACTIVE', NOW()), (47, 8, 2, 3, 'ACTIVE', NOW()),
(48, 8, 2, 3, 'ACTIVE', NOW()), (49, 8, 2, 3, 'ACTIVE', NOW()), (50, 8, 2, 3, 'ACTIVE', NOW()),
-- Class 9: English Grammar & Writing (Students 51-56)
(51, 9, 2, 3, 'ACTIVE', NOW()), (52, 9, 2, 3, 'ACTIVE', NOW()), (53, 9, 2, 3, 'ACTIVE', NOW()),
(54, 9, 2, 3, 'ACTIVE', NOW()), (55, 9, 2, 3, 'ACTIVE', NOW()), (56, 9, 2, 3, 'ACTIVE', NOW()),
-- Class 10: TOEIC Speaking & Writing (Students 41,42,57,58,59)
(41, 10, 2, 3, 'ACTIVE', NOW()), (42, 10, 2, 3, 'ACTIVE', NOW()), (57, 10, 2, 3, 'ACTIVE', NOW()),
(58, 10, 2, 3, 'ACTIVE', NOW()), (59, 10, 2, 3, 'DROPPED', NOW());

-- ================================================================
-- 12. ATTENDANCE (4 weeks of data: June 15 - July 8, 2026)
-- ================================================================
INSERT INTO attendances (student_user_id, schedule_id, center_id, date, status, marked_by_user_id, note, created_at) VALUES
-- === CLASS 1: TOEIC Foundation, Sched 1 (Mon) ===
-- Week 1 - June 15
(10, 1, 1, '2026-06-15', 'PRESENT', 4, NULL, NOW()),
(11, 1, 1, '2026-06-15', 'PRESENT', 4, NULL, NOW()),
(12, 1, 1, '2026-06-15', 'LATE',    4, 'Arrived 10 minutes late', NOW()),
(13, 1, 1, '2026-06-15', 'PRESENT', 4, NULL, NOW()),
(14, 1, 1, '2026-06-15', 'ABSENT',  4, 'Unexcused absence', NOW()),
(15, 1, 1, '2026-06-15', 'PRESENT', 4, NULL, NOW()),
-- Week 2 - June 22
(10, 1, 1, '2026-06-22', 'PRESENT', 4, NULL, NOW()),
(11, 1, 1, '2026-06-22', 'PRESENT', 4, NULL, NOW()),
(12, 1, 1, '2026-06-22', 'PRESENT', 4, NULL, NOW()),
(13, 1, 1, '2026-06-22', 'EXCUSED', 4, 'Family emergency', NOW()),
(14, 1, 1, '2026-06-22', 'PRESENT', 4, NULL, NOW()),
(15, 1, 1, '2026-06-22', 'PRESENT', 4, NULL, NOW()),
-- Week 3 - June 29
(10, 1, 1, '2026-06-29', 'PRESENT', 4, NULL, NOW()),
(11, 1, 1, '2026-06-29', 'ABSENT',  4, 'No notification', NOW()),
(12, 1, 1, '2026-06-29', 'PRESENT', 4, NULL, NOW()),
(13, 1, 1, '2026-06-29', 'PRESENT', 4, NULL, NOW()),
(14, 1, 1, '2026-06-29', 'LATE',    4, 'Arrived 20 minutes late', NOW()),
(15, 1, 1, '2026-06-29', 'PRESENT', 4, NULL, NOW()),
-- Week 4 - July 6
(10, 1, 1, '2026-07-06', 'PRESENT', 4, NULL, NOW()),
(11, 1, 1, '2026-07-06', 'PRESENT', 4, NULL, NOW()),
(12, 1, 1, '2026-07-06', 'PRESENT', 4, NULL, NOW()),
(13, 1, 1, '2026-07-06', 'PRESENT', 4, NULL, NOW()),
(14, 1, 1, '2026-07-06', 'PRESENT', 4, NULL, NOW()),
(15, 1, 1, '2026-07-06', 'EXCUSED', 4, 'Doctor appointment', NOW()),

-- === CLASS 1: TOEIC Foundation, Sched 2 (Wed) ===
-- Week 1 - June 17
(10, 2, 1, '2026-06-17', 'PRESENT', 4, NULL, NOW()),
(11, 2, 1, '2026-06-17', 'LATE',    4, 'Traffic jam', NOW()),
(12, 2, 1, '2026-06-17', 'PRESENT', 4, NULL, NOW()),
(13, 2, 1, '2026-06-17', 'PRESENT', 4, NULL, NOW()),
(14, 2, 1, '2026-06-17', 'PRESENT', 4, NULL, NOW()),
(15, 2, 1, '2026-06-17', 'ABSENT',  4, NULL, NOW()),

-- === CLASS 4: English Communication, Sched 7 (Tue) ===
-- Week 1 - June 16
(23, 7, 1, '2026-06-16', 'PRESENT', 5, NULL, NOW()),
(24, 7, 1, '2026-06-16', 'PRESENT', 5, NULL, NOW()),
(25, 7, 1, '2026-06-16', 'PRESENT', 5, NULL, NOW()),
(26, 7, 1, '2026-06-16', 'ABSENT',  5, NULL, NOW()),
(27, 7, 1, '2026-06-16', 'PRESENT', 5, NULL, NOW()),
(28, 7, 1, '2026-06-16', 'LATE',    5, NULL, NOW()),

-- === CLASS 8: Business English, Sched 16 (Mon) ===
-- Week 1 - June 15
(45, 16, 2, '2026-06-15', 'PRESENT', 7, NULL, NOW()),
(46, 16, 2, '2026-06-15', 'PRESENT', 7, NULL, NOW()),
(47, 16, 2, '2026-06-15', 'LATE',    7, 'Stuck in traffic', NOW()),
(48, 16, 2, '2026-06-15', 'PRESENT', 7, NULL, NOW()),
(49, 16, 2, '2026-06-15', 'ABSENT',  7, NULL, NOW()),
(50, 16, 2, '2026-06-15', 'PRESENT', 7, NULL, NOW()),
-- Week 2 - June 22
(45, 16, 2, '2026-06-22', 'PRESENT', 7, NULL, NOW()),
(46, 16, 2, '2026-06-22', 'PRESENT', 7, NULL, NOW()),
(47, 16, 2, '2026-06-22', 'PRESENT', 7, NULL, NOW()),
(48, 16, 2, '2026-06-22', 'PRESENT', 7, NULL, NOW()),
(49, 16, 2, '2026-06-22', 'LATE',    7, NULL, NOW()),
(50, 16, 2, '2026-06-22', 'PRESENT', 7, NULL, NOW()),

-- === CLASS 6: TOEIC 800+, Sched 11 (Mon) ===
-- Week 1 - June 15
(35, 11, 2, '2026-06-15', 'PRESENT', 6, NULL, NOW()),
(36, 11, 2, '2026-06-15', 'PRESENT', 6, NULL, NOW()),
(37, 11, 2, '2026-06-15', 'PRESENT', 6, NULL, NOW()),
(38, 11, 2, '2026-06-15', 'LATE',    6, NULL, NOW()),
(39, 11, 2, '2026-06-15', 'ABSENT',  6, 'Sick - informed via phone', NOW()),
(40, 11, 2, '2026-06-15', 'PRESENT', 6, NULL, NOW()),

-- === CLASS 9: English Grammar & Writing, Sched 19 (Tue) ===
-- Week 1 - June 16
(51, 19, 2, '2026-06-16', 'PRESENT', 7, NULL, NOW()),
(52, 19, 2, '2026-06-16', 'PRESENT', 7, NULL, NOW()),
(53, 19, 2, '2026-06-16', 'ABSENT',  7, NULL, NOW()),
(54, 19, 2, '2026-06-16', 'PRESENT', 7, NULL, NOW()),
(55, 19, 2, '2026-06-16', 'PRESENT', 7, NULL, NOW()),
(56, 19, 2, '2026-06-16', 'PRESENT', 7, NULL, NOW());

-- ================================================================
-- 13. FEE RECORDS (June & July 2026)
-- Mix: PAID, PARTIAL, UNPAID
-- ================================================================
INSERT INTO fee_records (center_id, student_user_id, class_id, amount, paid_amount, month, due_date, status, created_at) VALUES
-- Center 1 - HCM Fee Records
-- TOEIC Foundation: 1,200,000 VND/month
(1, 10, 1, 1200000.00, 1200000.00, '2026-06', '2026-06-05', 'PAID',    NOW()),
(1, 11, 1, 1200000.00, 1200000.00, '2026-06', '2026-06-05', 'PAID',    NOW()),
(1, 12, 1, 1200000.00,  600000.00, '2026-06', '2026-06-05', 'PARTIAL', NOW()),
(1, 13, 1, 1200000.00, 1200000.00, '2026-06', '2026-06-05', 'PAID',    NOW()),
(1, 14, 1, 1200000.00,        0.00, '2026-06', '2026-06-05', 'UNPAID',  NOW()),
(1, 15, 1, 1200000.00, 1200000.00, '2026-06', '2026-06-05', 'PAID',    NOW()),
-- July
(1, 10, 1, 1200000.00, 1200000.00, '2026-07', '2026-07-05', 'PAID',    NOW()),
(1, 11, 1, 1200000.00,        0.00, '2026-07', '2026-07-05', 'UNPAID',  NOW()),
(1, 12, 1, 1200000.00,        0.00, '2026-07', '2026-07-05', 'UNPAID',  NOW()),

-- TOEIC 650+: 2,200,000 VND/month
(1, 16, 3, 2200000.00, 2200000.00, '2026-06', '2026-06-05', 'PAID',    NOW()),
(1, 17, 3, 2200000.00, 1100000.00, '2026-06', '2026-06-05', 'PARTIAL', NOW()),
(1, 18, 3, 2200000.00, 2200000.00, '2026-06', '2026-06-05', 'PAID',    NOW()),

-- English Communication: 1,000,000 VND/month
(1, 23, 4, 1000000.00, 1000000.00, '2026-06', '2026-06-05', 'PAID',    NOW()),
(1, 24, 4, 1000000.00, 1000000.00, '2026-06', '2026-06-05', 'PAID',    NOW()),
(1, 25, 4, 1000000.00,        0.00, '2026-06', '2026-06-05', 'UNPAID',  NOW()),

-- Center 2 - Hanoi Fee Records
-- TOEIC 800+: 2,500,000 VND/month
(2, 35, 6, 2500000.00, 2500000.00, '2026-06', '2026-06-05', 'PAID',    NOW()),
(2, 36, 6, 2500000.00, 2500000.00, '2026-06', '2026-06-05', 'PAID',    NOW()),
(2, 37, 6, 2500000.00, 1250000.00, '2026-06', '2026-06-05', 'PARTIAL', NOW()),
(2, 38, 6, 2500000.00,        0.00, '2026-06', '2026-06-05', 'UNPAID',  NOW()),

-- Business English: 2,000,000 VND/month
(2, 45, 8, 2000000.00, 2000000.00, '2026-06', '2026-06-05', 'PAID',    NOW()),
(2, 46, 8, 2000000.00, 2000000.00, '2026-06', '2026-06-05', 'PAID',    NOW()),
(2, 47, 8, 2000000.00, 1000000.00, '2026-06', '2026-06-05', 'PARTIAL', NOW()),
(2, 48, 8, 2000000.00,        0.00, '2026-07', '2026-07-05', 'UNPAID',  NOW()),

-- English Grammar & Writing: 1,500,000 VND/month
(2, 51, 9, 1500000.00, 1500000.00, '2026-06', '2026-06-05', 'PAID',    NOW()),
(2, 52, 9, 1500000.00, 1500000.00, '2026-06', '2026-06-05', 'PAID',    NOW()),
(2, 53, 9, 1500000.00,        0.00, '2026-06', '2026-06-05', 'UNPAID',  NOW());

-- ================================================================
-- 14. PAYMENTS (Cash payments collected by cashiers)
-- ================================================================
INSERT INTO payments (fee_record_id, center_id, student_user_id, collected_by_user_id, amount, method, note, created_at) VALUES
-- Center 1 - HCM Payments
(1,  1, 10, 8, 1200000.00, 'CASH', 'Full payment for June',             '2026-06-03 09:15:00'),
(2,  1, 11, 8, 1200000.00, 'CASH', 'Full payment for June',             '2026-06-04 10:30:00'),
(3,  1, 12, 8,  600000.00, 'CASH', 'Partial payment - will pay rest later', '2026-06-05 14:00:00'),
(4,  1, 13, 8, 1200000.00, 'CASH', 'Full payment for June',             '2026-06-02 08:45:00'),
(6,  1, 15, 8, 1200000.00, 'CASH', 'Full payment for June',             '2026-06-03 11:00:00'),
(7,  1, 10, 8, 1200000.00, 'CASH', 'Full payment for July',             '2026-07-02 09:00:00'),
-- TOEIC 650+
(10, 1, 16, 8, 2200000.00, 'CASH', 'Full payment for June',             '2026-06-03 15:30:00'),
(11, 1, 17, 8, 1100000.00, 'CASH', 'Partial payment',                   '2026-06-06 10:00:00'),
(12, 1, 18, 8, 2200000.00, 'SEPAY','Bank transfer - paid in full',      '2026-06-01 08:00:00'),
-- English Communication
(13, 1, 23, 8, 1000000.00, 'CASH', 'Full payment for June',             '2026-06-04 16:00:00'),
(14, 1, 24, 8, 1000000.00, 'CASH', 'Full payment for June',             '2026-06-05 09:30:00'),

-- Center 2 - Hanoi Payments
(16, 2, 35, 9, 2500000.00, 'CASH', 'Full payment for June',             '2026-06-02 10:00:00'),
(17, 2, 36, 9, 2500000.00, 'SEPAY','Bank transfer',                     '2026-06-03 14:00:00'),
(18, 2, 37, 9, 1250000.00, 'CASH', 'Partial payment for June',          '2026-06-07 11:00:00'),
-- Business English
(20, 2, 45, 9, 2000000.00, 'CASH', 'Full payment for June',             '2026-06-01 09:00:00'),
(21, 2, 46, 9, 2000000.00, 'CASH', 'Full payment for June',             '2026-06-02 15:00:00'),
(22, 2, 47, 9, 1000000.00, 'CASH', 'Partial payment',                   '2026-06-05 10:30:00'),
-- English Grammar & Writing
(24, 2, 51, 9, 1500000.00, 'CASH', 'Full payment for June',             '2026-06-03 08:30:00'),
(25, 2, 52, 9, 1500000.00, 'SEPAY','Bank transfer',                     '2026-06-04 13:00:00');

-- ================================================================
-- 15. ESSAY RUBRICS (English Writing Only)
-- ================================================================
INSERT INTO essay_rubrics (title, description, max_score, clazz_id, created_by_user_id, center_id, is_active, created_at) VALUES
-- Center 1
('Opinion Essay Rubric', 'Evaluate opinion essays: thesis statement, supporting arguments, coherence, grammar, vocabulary. For TOEIC Writing practice.', 100.0, 3, 5, 1, b'1', NOW()),
('Email Writing Rubric', 'Evaluate business and informal emails: format, tone, clarity, grammar, and appropriateness for TOEIC Writing tasks.', 50.0, 3, 5, 1, b'1', NOW()),
('VSTEP Writing Task 1 Rubric', 'Evaluate VSTEP B1 Writing Task 1: letter/email writing - task completion, organization, vocabulary, grammar.', 100.0, 5, 4, 1, b'1', NOW()),
-- Center 2
('Graph Description Rubric', 'Evaluate graph/chart description essays: data interpretation, structure, vocabulary for trends, accuracy. For VSTEP B2 & TOEIC.', 100.0, 7, 6, 2, b'1', NOW()),
('Business Email Rubric', 'Evaluate professional business emails: formal tone, structure, clarity, appropriate register for Business English class.', 50.0, 8, 7, 2, b'1', NOW()),
('Academic Essay Rubric', 'Evaluate academic English essays: thesis development, evidence use, academic vocabulary, cohesion, and mechanics for VSTEP B2.', 100.0, 7, 6, 2, b'1', NOW()),
('TOEIC Writing Rubric', 'Evaluate TOEIC Writing tasks: picture description, email response, opinion essay with TOEIC scoring criteria.', 100.0, 10, 6, 2, b'1', NOW());

-- ================================================================
-- 16. ESSAY RUBRIC CRITERIA
-- ================================================================
INSERT INTO essay_rubric_criteria (rubric_id, name, description, weight, max_score) VALUES
-- Opinion Essay Rubric (rubric 1)
(1, 'Thesis Statement',    'Clear and arguable thesis statement in the introduction',   20.0, 20.0),
(1, 'Argument Development','Well-developed supporting arguments with examples',          30.0, 30.0),
(1, 'Organization',        'Logical paragraph structure with smooth transitions',        20.0, 20.0),
(1, 'Grammar & Mechanics', 'Correct grammar, punctuation, and spelling',                15.0, 15.0),
(1, 'Vocabulary',          'Appropriate and varied vocabulary choice',                  15.0, 15.0),
-- Email Writing Rubric (rubric 2)
(2, 'Format & Structure',  'Correct email format with subject, greeting, body, closing', 20.0, 10.0),
(2, 'Task Completion',     'All required points addressed appropriately',                30.0, 15.0),
(2, 'Tone & Register',     'Appropriate tone for the context (formal/informal)',         20.0, 10.0),
(2, 'Grammar',             'Grammatical accuracy throughout the email',                  15.0, 7.5),
(2, 'Clarity',             'Clear and concise expression',                              15.0, 7.5),
-- VSTEP Writing Task 1 Rubric (rubric 3)
(3, 'Task Fulfillment',    'Complete response to all bullet points in the prompt',       25.0, 25.0),
(3, 'Organization',        'Clear structure with appropriate paragraphing',              25.0, 25.0),
(3, 'Vocabulary',          'Range and accuracy of vocabulary',                           25.0, 25.0),
(3, 'Grammar',             'Range and accuracy of grammatical structures',               25.0, 25.0),
-- Graph Description Rubric (rubric 4)
(4, 'Data Interpretation',  'Accurate description of data trends and key figures',       25.0, 25.0),
(4, 'Structure',            'Clear introduction, overview, and detail paragraphs',       25.0, 25.0),
(4, 'Trend Vocabulary',     'Appropriate use of trend language (increase, decline, etc.)',25.0, 25.0),
(4, 'Accuracy',             'Correct figures, units, and comparisons',                   25.0, 25.0),
-- Business Email Rubric (rubric 5)
(5, 'Professional Format',  'Correct business email structure',                          25.0, 12.5),
(5, 'Clarity & Conciseness','Clear message without unnecessary words',                   25.0, 12.5),
(5, 'Politeness & Tone',    'Professional and courteous language',                       25.0, 12.5),
(5, 'Grammar & Spelling',   'Error-free writing',                                        25.0, 12.5),
-- Academic Essay Rubric (rubric 6)
(6, 'Thesis & Argument',    'Clear thesis supported by logical arguments',               25.0, 25.0),
(6, 'Evidence Use',         'Effective use of examples and evidence',                    25.0, 25.0),
(6, 'Academic Style',       'Appropriate academic vocabulary and tone',                  25.0, 25.0),
(6, 'Cohesion & Coherence', 'Smooth flow between paragraphs and ideas',                  25.0, 25.0),
-- TOEIC Writing Rubric (rubric 7)
(7, 'Task Completion',      'Fully addresses the writing prompt',                        30.0, 30.0),
(7, 'Organization',         'Well-organized with clear progression',                     25.0, 25.0),
(7, 'Grammar',              'Grammatical range and accuracy',                            25.0, 25.0),
(7, 'Vocabulary',           'Appropriate word choice and collocations',                  20.0, 20.0);

-- ================================================================
-- 17. ESSAY SUBMISSIONS (English writing tasks only)
-- ================================================================
INSERT INTO essay_submissions (student_user_id, center_id, clazz_id, rubric_id, content, status, graded_by_user_id, feedback, total_score, submitted_at, graded_at) VALUES
-- Center 1 Submissions
-- TOEIC 650+ class (class 3) - Opinion Essay
(16, 1, 3, 1,
 'In my opinion, learning English is essential for career development in the modern world. Firstly, English is the international language of business, and most multinational companies require employees to communicate effectively in English. Secondly, many academic resources, including research papers and online courses, are primarily available in English. However, some people argue that translation technology reduces the need to learn English, but I believe that direct communication skills cannot be replaced by machines. In conclusion, investing time in learning English is a wise decision for anyone who wants to advance their career.',
 'GRADED', 5, 'Good thesis statement and clear arguments. Work on varying your sentence structure. Some minor grammar issues: "are primarily available" should be "are available primarily". Score: 82/100', 82, '2026-06-10 10:00:00', '2026-06-12 14:00:00'),

(17, 1, 3, 1,
 'I think that studying abroad is a valuable experience for young people. When students go to another country, they can learn about new cultures and improve their language skills. Also, they become more independent because they have to take care of themselves. On the other hand, studying abroad is very expensive and some students feel homesick. Despite these challenges, I believe the benefits outweigh the disadvantages. In my view, every student should consider studying abroad if they have the opportunity.',
 'GRADED', 5, 'Good effort! Your essay has a clear structure. Try to use more specific examples to support your points. Improve vocabulary: replace "good" and "bad" with more precise words. Score: 75/100', 75, '2026-06-11 09:30:00', '2026-06-13 15:00:00'),

(18, 1, 3, 1,
 'The internet has changed education dramatically. Students can now access information from anywhere at any time. Online learning platforms like Coursera and edX offer courses from top universities for free. This makes education more accessible to people in developing countries. However, online learning also has disadvantages such as lack of face-to-face interaction and the need for self-discipline. I believe that a blended approach combining online and traditional learning is the best way forward.',
 'SUBMITTED', NULL, NULL, NULL, '2026-06-20 11:00:00', NULL),

-- VSTEP B1 class (class 5) - VSTEP Writing Task 1
(29, 1, 5, 3,
 'Dear Mr. Johnson,\n\nI am writing to apply for the English teaching assistant position that I saw advertised on your website. I am currently a third-year student majoring in English Linguistics at the University of Social Sciences and Humanities.\n\nI have experience tutoring high school students in English for the past two years. I am patient, responsible, and passionate about helping others improve their English skills. I also have an IELTS score of 7.0, which demonstrates my English proficiency.\n\nI would be grateful for the opportunity to discuss my application further. I am available for an interview at your convenience.\n\nYours sincerely,\nNguyen Hong Phu',
 'GRADED', 4, 'Excellent letter! Good structure with all required elements. Minor improvements: add more specific examples of your tutoring achievements. Score: 88/100', 88, '2026-06-15 08:00:00', '2026-06-17 10:00:00'),

(30, 1, 5, 3,
 'Dear Sir or Madam,\n\nI am writing to complain about an English language course I purchased from your center last month. The course was advertised as including 20 hours of speaking practice with native teachers, but I only received 10 hours.\n\nI paid 5 million VND for this course and I feel that I did not receive what was promised. I would like to request a partial refund or additional speaking sessions to make up for the missing hours.\n\nI look forward to your prompt response.\n\nYours faithfully,\nHoang Thi Thu',
 'GRADED', 4, 'Good complaint letter with clear points. Work on using more formal language in the opening. The structure is correct. Score: 80/100', 80, '2026-06-16 09:00:00', '2026-06-18 11:00:00'),

(31, 1, 5, 3,
 'Dear Ms. Emma,\n\nI am writing to thank you for the wonderful English communication course. Before joining your class, I was very shy and could not speak English confidently. After three months, I can now have conversations with foreigners without feeling nervous.\n\nYour teaching methods, especially the role-play activities, helped me improve my speaking skills significantly. I also enjoyed the pronunciation practice sessions.\n\nThank you again for your dedication and patience.\n\nBest regards,\nVu Kim Uyen',
 'SUBMITTED', NULL, NULL, NULL, '2026-07-02 14:00:00', NULL),

-- Center 2 Submissions
-- VSTEP B2 class (class 7) - Academic Essay
(41, 2, 7, 6,
 'Climate change represents one of the most pressing challenges facing humanity in the twenty-first century. This essay will examine the primary causes of climate change and evaluate potential solutions. The burning of fossil fuels for energy production is widely recognized as the main contributor to greenhouse gas emissions. Additionally, deforestation reduces the planet''s capacity to absorb carbon dioxide. To address these issues, governments must invest in renewable energy sources such as solar and wind power. Furthermore, international cooperation through agreements like the Paris Accord is essential. In conclusion, while the challenges are significant, a combination of technological innovation and policy changes can mitigate the effects of climate change.',
 'GRADED', 6, 'Excellent academic essay! Strong thesis, well-developed arguments, and appropriate academic vocabulary. Minor issue: consider adding a counterargument paragraph. Score: 90/100', 90, '2026-06-12 13:00:00', '2026-06-14 16:00:00'),

(42, 2, 7, 6,
 'Social media has transformed the way people communicate and share information. Platforms such as Facebook, Instagram, and TikTok have billions of users worldwide. On the positive side, social media allows people to stay connected with friends and family across long distances. It also provides a platform for businesses to reach customers and for activists to raise awareness about important issues. However, social media also has negative effects including addiction, cyberbullying, and the spread of misinformation. In my opinion, users need to be educated about responsible social media use to maximize its benefits while minimizing its harms.',
 'GRADED', 6, 'Well-structured essay with balanced arguments. Improve academic tone by avoiding personal pronouns in some sections. Good use of specific examples. Score: 85/100', 85, '2026-06-14 10:00:00', '2026-06-16 14:00:00'),

-- Business English class (class 8) - Business Email
(45, 2, 8, 5,
 'Subject: Request for Meeting - Q3 Marketing Strategy\n\nDear Mr. James,\n\nI hope this email finds you well. I am writing to request a meeting to discuss our Q3 marketing strategy for the new English course launch.\n\nI would like to propose the following agenda items:\n1. Review of Q2 campaign performance\n2. Target audience analysis for the new TOEIC preparation course\n3. Budget allocation for digital marketing channels\n4. Timeline for content creation\n\nWould Wednesday, July 15th at 2:00 PM be convenient for you? Please let me know if you would like to add any items to the agenda.\n\nI look forward to hearing from you.\n\nBest regards,\nNguyen Khanh Linh\nMarketing Coordinator',
 'GRADED', 7, 'Professional business email with clear purpose and well-organized agenda. Perfect format and tone. Minor suggestion: add a brief background sentence. Score: 48/50', 48, '2026-06-18 09:00:00', '2026-06-19 11:00:00'),

(46, 2, 8, 5,
 'Subject: Inquiry About Business English Course\n\nDear Owlexa Team,\n\nI am interested in enrolling in your Business English course that I saw advertised on your website. Could you please provide me with more information about the following:\n\n- Course schedule and duration\n- Qualification of the instructors\n- Whether the course includes any certification upon completion\n- The total cost and available payment plans\n\nI am available to start from August 2026 and would prefer evening classes if available.\n\nThank you for your assistance.\n\nKind regards,\nTran Hoai Nam',
 'SUBMITTED', NULL, NULL, NULL, '2026-07-01 10:00:00', NULL),

-- TOEIC Speaking & Writing class (class 10)
(57, 2, 10, 7,
 'The picture shows a busy office environment with several people working at their desks. In the foreground, a woman is talking on the phone while typing on her computer. Behind her, two colleagues are having a discussion near a whiteboard. The office appears modern with large windows that let in natural light. There are plants on some desks, which suggests the company cares about creating a pleasant work environment. Overall, the image conveys a sense of productive teamwork in a professional setting.',
 'GRADED', 6, 'Good picture description with clear organization from foreground to background. Add more specific details about what people are wearing or doing. Score: 78/100', 78, '2026-06-20 15:00:00', '2026-06-22 10:00:00');

-- ================================================================
-- 18. ESSAY GRADING RESULTS
-- ================================================================
INSERT INTO essay_grading_results (submission_id, total_score, max_score, feedback, graded_at) VALUES
(1, 82.0, 100.0, 'Good thesis statement and clear arguments. Work on varying your sentence structure. Some minor grammar issues. Score: 82/100', '2026-06-12 14:00:00'),
(2, 75.0, 100.0, 'Good effort! Your essay has a clear structure. Try to use more specific examples to support your points. Score: 75/100', '2026-06-13 15:00:00'),
(4, 88.0, 100.0, 'Excellent letter! Good structure with all required elements. Minor improvements: add more specific examples. Score: 88/100', '2026-06-17 10:00:00'),
(5, 80.0, 100.0, 'Good complaint letter with clear points. Work on using more formal language in the opening. Score: 80/100', '2026-06-18 11:00:00'),
(7, 90.0, 100.0, 'Excellent academic essay! Strong thesis, well-developed arguments, and appropriate academic vocabulary. Score: 90/100', '2026-06-14 16:00:00'),
(8, 85.0, 100.0, 'Well-structured essay with balanced arguments. Improve academic tone. Good use of specific examples. Score: 85/100', '2026-06-16 14:00:00'),
(9, 48.0,  50.0, 'Professional business email with clear purpose and well-organized agenda. Perfect format and tone. Score: 48/50', '2026-06-19 11:00:00'),
(10,78.0, 100.0, 'Good picture description with clear organization. Add more specific details. Score: 78/100', '2026-06-22 10:00:00');

-- ================================================================
-- 19. ESSAY CRITERIA SCORES
-- ================================================================
INSERT INTO essay_criteria_scores (grading_result_id, criteria_id, score, max_score, feedback) VALUES
-- Grading Result 1 (Opinion Essay - Submission 1)
(1, 1, 17.0, 20.0, 'Good thesis, could be more specific'),
(1, 2, 24.0, 30.0, 'Arguments are clear but need more supporting examples'),
(1, 3, 17.0, 20.0, 'Good paragraph structure, transitions could improve'),
(1, 4, 12.0, 15.0, 'Minor grammar errors throughout'),
(1, 5, 12.0, 15.0, 'Adequate vocabulary, could use more variety'),
-- Grading Result 2 (Opinion Essay - Submission 2)
(2, 1, 15.0, 20.0, 'Thesis is present but somewhat simplistic'),
(2, 2, 22.0, 30.0, 'Arguments need more depth and examples'),
(2, 3, 15.0, 20.0, 'Basic organization, transitions are weak'),
(2, 4, 11.0, 15.0, 'Several grammar mistakes'),
(2, 5, 12.0, 15.0, 'Vocabulary is simple but appropriate'),
-- Grading Result 3 (VSTEP Task 1 - Submission 4)
(3, 9,  22.0, 25.0, 'All bullet points addressed'),
(3, 10, 23.0, 25.0, 'Well-organized with clear paragraphs'),
(3, 11, 22.0, 25.0, 'Good range of vocabulary'),
(3, 12, 21.0, 25.0, 'Minor grammatical errors'),
-- Grading Result 5 (Academic Essay - Submission 7)
(5, 17, 23.0, 25.0, 'Strong thesis with clear argument'),
(5, 18, 22.0, 25.0, 'Good evidence, could include more data'),
(5, 19, 23.0, 25.0, 'Excellent academic vocabulary'),
(5, 20, 22.0, 25.0, 'Smooth transitions between paragraphs'),
-- Grading Result 7 (Business Email - Submission 9)
(7, 21, 12.0, 12.5, 'Perfect format'),
(7, 22, 12.0, 12.5, 'Clear and concise message'),
(7, 23, 12.0, 12.5, 'Professional and courteous'),
(7, 24, 12.0, 12.5, 'No grammatical errors');

-- ================================================================
-- 20. MOCK TESTS (English Only: TOEIC, VSTEP, Grammar, Vocabulary, Pronunciation)
-- ================================================================
INSERT INTO mock_tests (title, description, center_id, created_by_user_id, level, duration, total_questions, is_active, created_at) VALUES
-- Center 1 - HCM
('TOEIC Listening Part 1 - Photographs', 'Practice TOEIC Listening Part 1: Listen to statements and choose the best description of each photograph. 10 questions.', 1, 4, 'BEGINNER', 15, 10, b'1', NOW()),
('TOEIC Reading Part 5 - Incomplete Sentences', 'Practice TOEIC Reading Part 5: Choose the best word or phrase to complete each sentence. Tests grammar and vocabulary. 20 questions.', 1, 5, 'INTERMEDIATE', 25, 20, b'1', NOW()),
('TOEIC Vocabulary Builder', 'Test your TOEIC vocabulary knowledge with words commonly found in TOEIC tests. 15 questions covering business, travel, and office contexts.', 1, 5, 'BEGINNER', 20, 15, b'1', NOW()),
('English Grammar Fundamentals', 'Test your understanding of basic English grammar: tenses, articles, prepositions, and sentence structure. 15 questions.', 1, 4, 'BEGINNER', 20, 15, b'1', NOW()),
('TOEIC Reading Part 7 - Reading Comprehension', 'Practice TOEIC Reading Part 7: Read passages and answer comprehension questions. Tests reading speed and understanding. 10 questions.', 1, 5, 'ADVANCED', 30, 10, b'1', NOW()),

-- Center 2 - Hanoi
('VSTEP Reading Practice Test', 'Full VSTEP Reading section simulation with authentic question types. 20 questions covering multiple passages.', 2, 6, 'INTERMEDIATE', 40, 20, b'1', NOW()),
('TOEIC Grammar Mastery', 'Advanced TOEIC grammar test covering conditional sentences, relative clauses, passive voice, and more. 20 questions.', 2, 7, 'ADVANCED', 25, 20, b'1', NOW()),
('English Pronunciation & Stress Patterns', 'Test your knowledge of English pronunciation, word stress, and sentence intonation patterns. 10 questions.', 2, 7, 'INTERMEDIATE', 15, 10, b'1', NOW()),
('Business English Vocabulary', 'Test your business English vocabulary: meetings, negotiations, presentations, and corporate communication. 15 questions.', 2, 6, 'INTERMEDIATE', 20, 15, b'1', NOW()),
('VSTEP Listening Simulation', 'Practice VSTEP Listening section: short conversations, lectures, and announcements. 15 questions.', 2, 6, 'ADVANCED', 25, 15, b'1', NOW());

-- ================================================================
-- 21. MOCK TEST QUESTIONS (English content only)
-- ================================================================
INSERT INTO mock_test_questions (mock_test_id, question_text, optiona, optionb, optionc, optiond, correct_answer, explanation, sort_order, created_at, updated_at) VALUES
-- ===== MOCK TEST 1: TOEIC Listening Part 1 (10 questions) =====
(1, 'Which sentence best describes a photograph of people in a meeting room?',
 'The people are eating lunch together.',
 'The people are attending a business meeting.',
 'The people are playing sports outside.',
 'The people are sleeping at their desks.',
 'B', 'In TOEIC Part 1, photographs often show business settings. A meeting room suggests people are attending a meeting.', 1, NOW(), NOW()),
(1, 'What would you most likely hear describing a photo of a woman at a computer?',
 'She is reading a book in the library.',
 'She is typing on a keyboard at her desk.',
 'She is cooking dinner in the kitchen.',
 'She is driving a car on the highway.',
 'B', 'Photographs of people at computers typically involve typing or working. Typing on a keyboard is the most logical description.', 2, NOW(), NOW()),
(1, 'Which statement best describes a picture of an airport terminal?',
 'Passengers are waiting to board their flights.',
 'Students are studying in a classroom.',
 'Doctors are performing surgery.',
 'Chefs are preparing meals.',
 'A', 'Airport terminals are associated with passengers and flights. The other options describe unrelated settings.', 3, NOW(), NOW()),
(1, 'What best describes a photograph showing a man holding a briefcase?',
 'He is going swimming at the beach.',
 'He is preparing for a business trip.',
 'He is cooking dinner for his family.',
 'He is watching a movie at home.',
 'B', 'A briefcase is associated with business activities. A business trip is the most logical context.', 4, NOW(), NOW()),
(1, 'Which sentence fits a photo of shelves full of books?',
 'The restaurant is very crowded tonight.',
 'The library has a large collection of books.',
 'The garden is full of beautiful flowers.',
 'The beach is empty in the morning.',
 'B', 'Shelves of books indicate a library or bookstore setting. Libraries are the most common association.', 5, NOW(), NOW()),
(1, 'What describes a picture of a waiter serving food?',
 'The customer is paying the bill at the counter.',
 'The waiter is bringing dishes to the table.',
 'The chef is chopping vegetables in the kitchen.',
 'The manager is hiring new staff members.',
 'B', 'A waiter serving food suggests bringing dishes to customers. This is a typical restaurant scene description.', 6, NOW(), NOW()),
(1, 'Which statement matches a photo of a construction site?',
 'Workers are building a new office tower.',
 'Teachers are grading exam papers.',
 'Musicians are recording a new album.',
 'Farmers are harvesting rice in the field.',
 'A', 'Construction sites involve building structures. Office towers are commonly built at construction sites.', 7, NOW(), NOW()),
(1, 'What best describes people standing in a line at a bank?',
 'They are waiting to see a doctor.',
 'They are queuing to deposit money.',
 'They are ordering food at a cafe.',
 'They are boarding a sightseeing bus.',
 'B', 'People standing in line at a bank are typically waiting for banking services like deposits or withdrawals.', 8, NOW(), NOW()),
(1, 'Which sentence describes a photo of a bicycle parked near a building?',
 'The car is speeding down the highway.',
 'The bicycle is leaning against the wall.',
 'The train is arriving at the station.',
 'The airplane is taking off from the runway.',
 'B', 'A parked bicycle near a building would most likely be described as leaning against the wall.', 9, NOW(), NOW()),
(1, 'What best fits an image of two people shaking hands?',
 'They are arguing about a disagreement.',
 'They are greeting each other in a business setting.',
 'They are competing in a sports match.',
 'They are saying goodbye at the airport.',
 'B', 'Handshakes in business contexts typically represent greetings, agreements, or introductions.', 10, NOW(), NOW()),

-- ===== MOCK TEST 2: TOEIC Reading Part 5 (20 questions - sample 10) =====
(2, 'The new employee ______ at the reception desk since 8:00 AM this morning.',
 'has been sitting',
 'is sitting',
 'was sitting',
 'will be sitting',
 'A', 'Present perfect continuous has been sitting is used for actions that started in the past and continue to the present, with since indicating the starting point.', 1, NOW(), NOW()),
(2, 'The marketing report must be submitted ______ Friday at the latest.',
 'by',
 'until',
 'during',
 'for',
 'A', 'By is used to indicate a deadline. By Friday means on or before Friday.', 2, NOW(), NOW()),
(2, 'Neither the manager nor his assistants ______ available for the meeting yesterday.',
 'was',
 'were',
 'is',
 'are',
 'B', 'With neither...nor, the verb agrees with the nearest subject. Assistants is plural, so were is correct. Past tense because of yesterday.', 3, NOW(), NOW()),
(2, 'The company plans to ______ its operations to Southeast Asian markets next year.',
 'expand',
 'expands',
 'expanded',
 'expanding',
 'A', 'After plans to, the base form of the verb is required: to expand.', 4, NOW(), NOW()),
(2, 'If the shipment ______ on time, we would have completed the order by now.',
 'arrived',
 'had arrived',
 'would arrive',
 'arrives',
 'B', 'Third conditional (past unreal): If + had + past participle, would have + past participle. This describes an unreal past situation.', 5, NOW(), NOW()),
(2, 'The conference room is large ______ to accommodate up to 200 participants.',
 'enough',
 'too',
 'such',
 'so',
 'A', 'Large enough to is the correct structure. Enough follows the adjective and is followed by an infinitive.', 6, NOW(), NOW()),
(2, 'Ms. Thompson asked ______ the financial documents had been reviewed.',
 'whether',
 'weather',
 'rather',
 'either',
 'A', 'Whether is used to introduce an indirect question. Weather refers to climate conditions and is commonly confused.', 7, NOW(), NOW()),
(2, 'The success of the project depends ______ the cooperation of all team members.',
 'on',
 'in',
 'at',
 'for',
 'A', 'Depends on is the correct collocation in English. Depend is always followed by on or upon.', 8, NOW(), NOW()),
(2, 'Despite ______ hard for the presentation, she felt nervous when speaking.',
 'preparing',
 'prepared',
 'to prepare',
 'prepare',
 'A', 'After despite (a preposition), we use a gerund (-ing form). Despite preparing is correct.', 9, NOW(), NOW()),
(2, 'The CEO recommended that every department ______ its budget proposal by Monday.',
 'submit',
 'submits',
 'submitted',
 'submitting',
 'A', 'After verbs like recommend, suggest, insist, the subjunctive mood is used: that + subject + base form of verb.', 10, NOW(), NOW()),

-- ===== MOCK TEST 3: TOEIC Vocabulary Builder (15 questions - sample 10) =====
(3, 'The company will ______ a new product line next quarter.',
 'launch',
 'eat',
 'destroy',
 'sleep',
 'A', 'Launch a product is a common business collocation meaning to introduce or release a new product to the market.', 1, NOW(), NOW()),
(3, 'Please find the ______ document attached to this email.',
 'requested',
 'delicious',
 'sunny',
 'wooden',
 'A', 'Requested document means the document that was asked for. Common in business email correspondence.', 2, NOW(), NOW()),
(3, 'The employee received a ______ for her outstanding performance.',
 'promotion',
 'sandwich',
 'umbrella',
 'headache',
 'A', 'Receive a promotion means to be advanced to a higher position. This is common workplace vocabulary.', 3, NOW(), NOW()),
(3, 'We need to ______ the meeting until next week due to scheduling conflicts.',
 'postpone',
 'celebrate',
 'paint',
 'sing',
 'A', 'Postpone a meeting means to delay or reschedule it. Frequently used in TOEIC business contexts.', 4, NOW(), NOW()),
(3, 'The company offers competitive ______ to attract talented employees.',
 'salaries',
 'weather',
 'furniture',
 'recipes',
 'A', 'Competitive salaries refers to pay rates that are as good as or better than other companies. Key business vocabulary.', 5, NOW(), NOW()),
(3, 'Please ______ your signature at the bottom of the contract.',
 'provide',
 'cook',
 'dance',
 'jump',
 'A', 'Provide your signature is formal business language meaning to sign a document.', 6, NOW(), NOW()),
(3, 'The flight ______ was delayed due to bad weather conditions.',
 'departure',
 'breakfast',
 'painting',
 'garden',
 'A', 'Flight departure refers to when an airplane leaves. Common travel vocabulary in TOEIC.', 7, NOW(), NOW()),
(3, 'We appreciate your ______ in this matter and look forward to your reply.',
 'patience',
 'hunger',
 'noise',
 'height',
 'A', 'Appreciate your patience is a polite business expression thanking someone for waiting or being understanding.', 8, NOW(), NOW()),
(3, 'The seminar will cover topics ______ from basic accounting to advanced finance.',
 'ranging',
 'singing',
 'swimming',
 'cooking',
 'A', 'Ranging from...to... means covering a variety of topics between two extremes. Common in course descriptions.', 9, NOW(), NOW()),
(3, 'All employees must ______ the safety training session before starting work.',
 'attend',
 'ignore',
 'avoid',
 'skip',
 'A', 'Attend a training session means to be present at and participate in the training. Required workplace vocabulary.', 10, NOW(), NOW()),

-- ===== MOCK TEST 4: English Grammar Fundamentals (15 questions - sample 5) =====
(4, 'She ______ to the gym every morning before work.',
 'goes',
 'go',
 'going',
 'gone',
 'A', 'Third person singular she requires the verb + s/es: goes. Present simple for habitual actions.', 1, NOW(), NOW()),
(4, 'I have been studying English ______ three years.',
 'for',
 'since',
 'during',
 'while',
 'A', 'For is used with a duration of time (three years). Since would be used with a specific starting point.', 2, NOW(), NOW()),
(4, 'There ______ many students in the TOEIC class this semester.',
 'are',
 'is',
 'was',
 'has',
 'A', 'Many students is plural, so the plural verb are is required. There are is the correct structure.', 3, NOW(), NOW()),
(4, 'The book ______ by the teacher was very helpful for the exam.',
 'recommended',
 'recommending',
 'recommend',
 'recommends',
 'A', 'Past participle recommended is used as a reduced relative clause: The book that was recommended by the teacher.', 4, NOW(), NOW()),
(4, 'You should practice speaking English every day ______ you want to improve.',
 'if',
 'although',
 'because',
 'despite',
 'A', 'If introduces a condition. The sentence means: practicing is necessary on the condition that you want to improve.', 5, NOW(), NOW()),

-- ===== MOCK TEST 6: VSTEP Reading Practice (20 questions - sample 8) =====
(6, 'According to the passage, what is the main benefit of bilingual education?',
 'Higher salaries for teachers',
 'Improved cognitive flexibility in students',
 'Reduced school operating costs',
 'Increased playground time',
 'B', 'VSTEP reading passages on bilingual education typically highlight cognitive benefits like flexibility and problem-solving skills.', 1, NOW(), NOW()),
(6, 'The word proficient in paragraph 2 is closest in meaning to:',
 'Skilled',
 'Beginner',
 'Lazy',
 'Confused',
 'A', 'Proficient means skilled or competent in a particular area. This is a common vocabulary question type in VSTEP.', 2, NOW(), NOW()),
(6, 'What does the author imply about traditional teaching methods?',
 'They are always better than modern methods.',
 'They may not address individual learning styles.',
 'They are less expensive to implement.',
 'They have been completely abandoned.',
 'B', 'VSTEP passages often imply criticism of one-size-fits-all traditional methods versus personalized modern approaches.', 3, NOW(), NOW()),
(6, 'Which of the following is NOT mentioned as a challenge in online learning?',
 'Lack of face-to-face interaction',
 'Technical difficulties with internet connection',
 'Higher tuition fees compared to traditional classes',
 'Need for strong self-discipline',
 'C', 'The passage discusses interaction, technical issues, and self-discipline but does not claim online learning has higher tuition fees.', 4, NOW(), NOW()),
(6, 'The main purpose of the passage is to:',
 'Entertain readers with funny classroom stories',
 'Argue for the benefits of experiential learning',
 'Describe the history of education in Vietnam',
 'Provide instructions for building a school',
 'B', 'VSTEP reading passages commonly have a persuasive purpose. This passage advocates for experiential learning methods.', 5, NOW(), NOW()),
(6, 'According to the text, critical thinking skills can be developed by:',
 'Memorizing long lists of vocabulary words',
 'Analyzing and evaluating different perspectives',
 'Copying model answers repeatedly',
 'Taking multiple-choice tests only',
 'B', 'Critical thinking develops through analysis and evaluation. Memorization and repetition do not build critical thinking.', 6, NOW(), NOW()),
(6, 'The phrase bridge the gap in the passage most likely means:',
 'Build an actual bridge',
 'Reduce the difference between two things',
 'Create a new problem',
 'Destroy a connection',
 'B', 'Bridge the gap is an idiom meaning to reduce differences or connect two separate things. Common in academic texts.', 7, NOW(), NOW()),
(6, 'What conclusion does the author draw about lifelong learning?',
 'It is only necessary for teachers.',
 'It is essential for career adaptability in the modern economy.',
 'It should be limited to university years.',
 'It has no impact on professional success.',
 'B', 'VSTEP passages commonly conclude that lifelong learning is crucial for adapting to changing job markets.', 8, NOW(), NOW()),

-- ===== MOCK TEST 8: English Pronunciation (10 questions - sample 5) =====
(8, 'Which word has a different stress pattern from the others?',
 'PHOtograph',
 'phoTOGraphy',
 'PHOtographer',
 'photoGRAPHic',
 'B', 'phoTOGraphy has stress on the second syllable, while the others have stress on the first syllable.', 1, NOW(), NOW()),
(8, 'Which of the following words has the underlined part pronounced differently? (The ed ending)',
 'worked',
 'stopped',
 'played',
 'washed',
 'C', 'Played ends with /d/ sound (voiced), while worked, stopped, and washed end with /t/ (voiceless).', 2, NOW(), NOW()),
(8, 'Choose the word where ch is pronounced differently:',
 'chemistry',
 'character',
 'champion',
 'stomach',
 'C', 'Champion has /tʃ/ sound, while the others have /k/. This tests knowledge of ch pronunciation patterns.', 3, NOW(), NOW()),
(8, 'Which word has the primary stress on the second syllable?',
 'HAPpy',
 'comPUter',
 'ELephant',
 'BEAUtiful',
 'B', 'comPUter has stress on the second syllable. The others are stressed on the first syllable.', 4, NOW(), NOW()),
(8, 'In which word is the final s pronounced as /ɪz/?',
 'cats',
 'dogs',
 'watches',
 'trees',
 'C', 'The -es ending after ch is pronounced /ɪz/. Watches follows the rule: after s, z, sh, ch, j, x sounds.', 5, NOW(), NOW());

-- ================================================================
-- 22. MOCK TEST ATTEMPTS
-- ================================================================
INSERT INTO mock_test_attempts (mock_test_id, student_user_id, center_id, status, score, max_score, total_questions, correct_answers, test_title_snapshot, time_spent_seconds, started_at, submitted_at, completed_at) VALUES
-- Center 1 Attempts
(1, 10, 1, 'COMPLETED', 8,  10, 10, 8,  'TOEIC Listening Part 1 - Photographs',          720,  '2026-06-16 09:00:00', '2026-06-16 09:12:00', '2026-06-16 09:12:00'),
(1, 11, 1, 'COMPLETED', 6,  10, 10, 6,  'TOEIC Listening Part 1 - Photographs',          850,  '2026-06-16 09:15:00', '2026-06-16 09:29:10', '2026-06-16 09:29:10'),
(2, 16, 1, 'COMPLETED', 16, 20, 20, 16, 'TOEIC Reading Part 5 - Incomplete Sentences',  1400,  '2026-06-18 14:00:00', '2026-06-18 14:23:20', '2026-06-18 14:23:20'),
(2, 17, 1, 'COMPLETED', 12, 20, 20, 12, 'TOEIC Reading Part 5 - Incomplete Sentences',  1600,  '2026-06-18 14:30:00', '2026-06-18 14:56:40', '2026-06-18 14:56:40'),
(3, 12, 1, 'COMPLETED', 11, 15, 15, 11, 'TOEIC Vocabulary Builder',                      900,  '2026-06-20 10:00:00', '2026-06-20 10:15:00', '2026-06-20 10:15:00'),
(4, 23, 1, 'COMPLETED', 13, 15, 15, 13, 'English Grammar Fundamentals',                   1100, '2026-06-22 08:00:00', '2026-06-22 08:18:20', '2026-06-22 08:18:20'),
(2, 18, 1, 'IN_PROGRESS', NULL, NULL, NULL, NULL, 'TOEIC Reading Part 5 - Incomplete Sentences', 300, '2026-07-08 15:00:00', NULL, NULL),

-- Center 2 Attempts
(6, 35, 2, 'COMPLETED', 14, 20, 20, 14, 'VSTEP Reading Practice Test',                   2200, '2026-06-17 08:00:00', '2026-06-17 08:36:40', '2026-06-17 08:36:40'),
(6, 36, 2, 'COMPLETED', 17, 20, 20, 17, 'VSTEP Reading Practice Test',                   2000, '2026-06-17 09:00:00', '2026-06-17 09:33:20', '2026-06-17 09:33:20'),
(7, 41, 2, 'COMPLETED', 15, 20, 20, 15, 'TOEIC Grammar Mastery',                          1300, '2026-06-19 10:00:00', '2026-06-19 10:21:40', '2026-06-19 10:21:40'),
(7, 42, 2, 'COMPLETED', 18, 20, 20, 18, 'TOEIC Grammar Mastery',                          1100, '2026-06-19 14:00:00', '2026-06-19 14:18:20', '2026-06-19 14:18:20'),
(9, 45, 2, 'COMPLETED', 10, 15, 15, 10, 'Business English Vocabulary',                     800,  '2026-06-21 11:00:00', '2026-06-21 11:13:20', '2026-06-21 11:13:20'),
(10,38, 2, 'COMPLETED', 11, 15, 15, 11, 'VSTEP Listening Simulation',                     1400, '2026-06-23 13:00:00', '2026-06-23 13:23:20', '2026-06-23 13:23:20'),
(8, 51, 2, 'COMPLETED', 7,  10, 10, 7,  'English Pronunciation & Stress Patterns',         600,  '2026-06-25 09:00:00', '2026-06-25 09:10:00', '2026-06-25 09:10:00');

-- ================================================================
-- 23. MOCK TEST ATTEMPT ANSWERS (for a few completed attempts)
-- ================================================================
-- Attempt 1: Student 10, TOEIC Listening Part 1 (8/10 correct)
INSERT INTO mock_test_attempt_answers (attempt_id, question_id, question_text, student_answer, is_correct, correct_answer, created_at, updated_at) VALUES
(1, 1,  'Which sentence best describes a photograph of people in a meeting room?', 'B', TRUE,  'B', NOW(), NOW()),
(1, 2,  'What would you most likely hear describing a photo of a woman at a computer?', 'B', TRUE,  'B', NOW(), NOW()),
(1, 3,  'Which statement best describes a picture of an airport terminal?', 'A', TRUE,  'A', NOW(), NOW()),
(1, 4,  'What best describes a photograph showing a man holding a briefcase?', 'B', TRUE,  'B', NOW(), NOW()),
(1, 5,  'Which sentence fits a photo of shelves full of books?', 'B', TRUE,  'B', NOW(), NOW()),
(1, 6,  'What describes a picture of a waiter serving food?', 'C', FALSE, 'B', NOW(), NOW()), -- Wrong: chose chef
(1, 7,  'Which statement matches a photo of a construction site?', 'A', TRUE,  'A', NOW(), NOW()),
(1, 8,  'What best describes people standing in a line at a bank?', 'B', TRUE,  'B', NOW(), NOW()),
(1, 9,  'Which sentence describes a photo of a bicycle parked near a building?', 'C', FALSE, 'B', NOW(), NOW()), -- Wrong: chose train
(1, 10, 'What best fits an image of two people shaking hands?', 'B', TRUE,  'B', NOW(), NOW());

-- Attempt 7: Student 41, TOEIC Grammar Mastery (15/20 correct - sample 5 answers)
INSERT INTO mock_test_attempt_answers (attempt_id, question_id, question_text, student_answer, is_correct, correct_answer, created_at, updated_at) VALUES
(7, 31, 'According to the passage, what is the main benefit of bilingual education?', 'B', TRUE,  'B', NOW(), NOW()),
(7, 32, 'The word "proficient" in paragraph 2 is closest in meaning to:', 'A', TRUE,  'A', NOW(), NOW()),
(7, 33, 'What does the author imply about traditional teaching methods?', 'B', TRUE,  'B', NOW(), NOW()),
(7, 34, 'Which of the following is NOT mentioned as a challenge in online learning?', 'C', TRUE,  'C', NOW(), NOW()),
(7, 35, 'The main purpose of the passage is to:', 'A', FALSE, 'B', NOW(), NOW()); -- Wrong: chose entertain

-- Attempt 3: Student 16, TOEIC Reading Part 5 (16/20 correct - sample 5)
INSERT INTO mock_test_attempt_answers (attempt_id, question_id, question_text, student_answer, is_correct, correct_answer, created_at, updated_at) VALUES
(3, 11, 'The new employee ______ at the reception desk since 8:00 AM this morning.', 'A', TRUE,  'A', NOW(), NOW()),
(3, 12, 'The marketing report must be submitted ______ Friday at the latest.', 'A', TRUE,  'A', NOW(), NOW()),
(3, 13, 'Neither the manager nor his assistants ______ available for the meeting yesterday.', 'C', FALSE, 'B', NOW(), NOW()), -- Wrong: chose "is" instead of "were"
(3, 14, 'The company plans to ______ its operations to Southeast Asian markets next year.', 'A', TRUE,  'A', NOW(), NOW()),
(3, 15, 'If the shipment ______ on time, we would have completed the order by now.', 'D', FALSE, 'B', NOW(), NOW()); -- Wrong: chose "arrives" instead of "had arrived"

-- ================================================================
-- 24. STUDENT DOCUMENTS (English learning materials)
-- ================================================================
INSERT INTO student_documents (student_user_id, center_id, clazz_id, document_type, title, file_url, description, created_at) VALUES
-- Center 1
(10, 1, 1, 'PDF', 'TOEIC Foundation - Course Syllabus', '/documents/toeic-foundation-syllabus.pdf', 'Complete course outline for TOEIC Foundation class including weekly topics and learning objectives', NOW()),
(12, 1, 1, 'PDF', 'TOEIC Vocabulary List - Week 1-4', '/documents/toeic-vocab-week1-4.pdf', 'Essential TOEIC vocabulary with definitions and example sentences for the first month', NOW()),
(16, 1, 3, 'VIDEO', 'TOEIC Reading Strategies Tutorial', '/videos/toeic-reading-strategies.mp4', 'Video tutorial covering time management and skimming techniques for TOEIC Reading Part 7', NOW()),
(23, 1, 4, 'PDF', 'English Pronunciation Guide', '/documents/pronunciation-guide.pdf', 'Comprehensive guide to English phonetics with audio links and practice exercises', NOW()),
(29, 1, 5, 'PDF', 'VSTEP B1 Writing Templates', '/documents/vstep-b1-templates.pdf', 'Model answers and templates for VSTEP B1 Writing Task 1 and Task 2', NOW()),
(15, 1, 1, 'OTHER', 'TOEIC Practice Audio Files - Listening', '/audio/toeic-listening-pack.zip', 'Collection of TOEIC listening practice audio files for Parts 1-4', NOW()),

-- Center 2
(35, 2, 6, 'PDF', 'TOEIC 800+ Advanced Grammar Guide', '/documents/toeic-800-grammar.pdf', 'Advanced English grammar topics for TOEIC 800+ target score including conditionals and subjunctive', NOW()),
(41, 2, 7, 'PDF', 'VSTEP B2 Essay Collection', '/documents/vstep-b2-essays.pdf', 'Collection of high-scoring VSTEP B2 essays with teacher annotations and feedback', NOW()),
(45, 2, 8, 'VIDEO', 'Business Presentation Skills', '/videos/business-presentation.mp4', 'Video lesson on delivering effective business presentations in English', NOW()),
(51, 2, 9, 'PDF', 'English Grammar Workbook - Intermediate', '/documents/grammar-workbook.pdf', 'Interactive grammar exercises covering tenses, clauses, and sentence patterns', NOW()),
(57, 2, 10,'PDF', 'TOEIC Speaking Question Bank', '/documents/toeic-speaking-questions.pdf', 'Collection of TOEIC Speaking test questions with model responses and scoring guides', NOW()),
(38, 2, 6, 'PDF', 'Business Vocabulary Flashcards', '/documents/business-vocab-cards.pdf', 'Printable flashcards for essential business English vocabulary with definitions', NOW());

COMMIT;

-- ================================================================
-- VERIFICATION QUERIES
-- ================================================================
SELECT '=== SEED DATA SUMMARY ===' AS '';
SELECT 'users' AS entity, COUNT(*) AS row_count FROM users
UNION ALL SELECT 'permissions', COUNT(*) FROM permissions
UNION ALL SELECT 'user_permission', COUNT(*) FROM user_permission
UNION ALL SELECT 'centers', COUNT(*) FROM centers
UNION ALL SELECT 'membership', COUNT(*) FROM membership
UNION ALL SELECT 'courses', COUNT(*) FROM courses
UNION ALL SELECT 'rooms', COUNT(*) FROM rooms
UNION ALL SELECT 'teacher_center_profile', COUNT(*) FROM teacher_center_profile
UNION ALL SELECT 'classes', COUNT(*) FROM classes
UNION ALL SELECT 'schedules', COUNT(*) FROM schedules
UNION ALL SELECT 'class_enrollments', COUNT(*) FROM class_enrollments
UNION ALL SELECT 'attendances', COUNT(*) FROM attendances
UNION ALL SELECT 'fee_records', COUNT(*) FROM fee_records
UNION ALL SELECT 'payments', COUNT(*) FROM payments
UNION ALL SELECT 'essay_rubrics', COUNT(*) FROM essay_rubrics
UNION ALL SELECT 'essay_rubric_criteria', COUNT(*) FROM essay_rubric_criteria
UNION ALL SELECT 'essay_submissions', COUNT(*) FROM essay_submissions
UNION ALL SELECT 'essay_grading_results', COUNT(*) FROM essay_grading_results
UNION ALL SELECT 'essay_criteria_scores', COUNT(*) FROM essay_criteria_scores
UNION ALL SELECT 'mock_tests', COUNT(*) FROM mock_tests
UNION ALL SELECT 'mock_test_questions', COUNT(*) FROM mock_test_questions
UNION ALL SELECT 'mock_test_attempts', COUNT(*) FROM mock_test_attempts
UNION ALL SELECT 'mock_test_attempt_answers', COUNT(*) FROM mock_test_attempt_answers
UNION ALL SELECT 'student_documents', COUNT(*) FROM student_documents;

SELECT '=== ENGLISH CONTENT VERIFICATION ===' AS '';
SELECT 'Courses (English only):' AS check_name, GROUP_CONCAT(name SEPARATOR ' | ') AS result FROM courses;
SELECT 'Classes (English only):' AS check_name, GROUP_CONCAT(name SEPARATOR ' | ') AS result FROM classes;
SELECT 'Mock Tests (English only):' AS check_name, GROUP_CONCAT(title SEPARATOR ' | ') AS result FROM mock_tests;
SELECT 'Essay Rubrics (English only):' AS check_name, GROUP_CONCAT(title SEPARATOR ' | ') AS result FROM essay_rubrics;