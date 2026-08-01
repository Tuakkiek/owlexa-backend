-- ================================================================
-- OWLEXA ENGLISH LEARNING CENTER - SEED DATA v7.0 (Flyway Migration)
-- Full schema coverage: 43 tables (V1 to V5 baseline plus QuestionCollection)
-- Enforces multi-tenancy, FK integrity, realistic English training data,
-- BCrypt password hashing, and active assessment & AI grading flows.
-- Default password for ALL users: "password123"
-- BCrypt Hash: $2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS
-- ================================================================

-- 1. USERS (22 Users)
INSERT IGNORE INTO `users` (`id`, `email`, `full_name`, `password`, `phone_number`, `role`) VALUES
(1, 'owner@owlexa.edu.vn', 'Kiều Mỹ Linh', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0901111111', 'OWNER'),
(2, 'manager.d1@owlexa.edu.vn', 'Trần Thị Thanh Hằng', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0902222221', 'MANAGER'),
(3, 'manager.thuduc@owlexa.edu.vn', 'Lê Hoàng Long', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0902222222', 'MANAGER'),
(4, 'academic.d1@owlexa.edu.vn', 'Nguyễn Văn Minh', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0903333331', 'ACADEMIC_STAFF'),
(5, 'academic.thuduc@owlexa.edu.vn', 'Phạm Thị Ngọc', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0903333332', 'ACADEMIC_STAFF'),
(6, 'cashier.d1@owlexa.edu.vn', 'Đỗ Thu Hà', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0904444441', 'CASHIER'),
(7, 'cashier.thuduc@owlexa.edu.vn', 'Vũ Mai Hương', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0904444442', 'CASHIER'),
(8, 'teacher.nam@owlexa.edu.vn', 'Nguyễn Thành Nam', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0905555551', 'TEACHER'),
(9, 'teacher.hoa@owlexa.edu.vn', 'Lê Thị Hoa', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0905555552', 'TEACHER'),
(10, 'teacher.john@owlexa.edu.vn', 'Johnathan Edward Smith', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0905555553', 'TEACHER'),
(11, 'teacher.trang@owlexa.edu.vn', 'Phạm Huyền Trang', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0905555554', 'TEACHER'),
(12, 'teacher.david@owlexa.edu.vn', 'David Paul Wilson', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0905555555', 'TEACHER'),
(13, 'student.an@owlexa.edu.vn', 'Nguyễn Văn An', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0906666601', 'STUDENT'),
(14, 'student.binh@owlexa.edu.vn', 'Trần Thị Bình', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0906666602', 'STUDENT'),
(15, 'student.cuong@owlexa.edu.vn', 'Lê Quốc Cường', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0906666603', 'STUDENT'),
(16, 'student.dung@owlexa.edu.vn', 'Phạm Thùy Dung', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0906666604', 'STUDENT'),
(17, 'student.yen@owlexa.edu.vn', 'Hoàng Thị Hải Yến', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0906666605', 'STUDENT'),
(18, 'student.phong@owlexa.edu.vn', 'Vũ Hoàng Phong', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0906666606', 'STUDENT'),
(19, 'student.giang@owlexa.edu.vn', 'Đặng Hương Giang', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0906666607', 'STUDENT'),
(20, 'student.hieu@owlexa.edu.vn', 'Bùi Trung Hiếu', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0906666608', 'STUDENT'),
(21, 'student.khang@owlexa.edu.vn', 'Ngô Minh Khang', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0906666609', 'STUDENT'),
(22, 'student.linh@owlexa.edu.vn', 'Nguyễn Diệu Linh', '$2a$10$0FxtjwbmKTcDr2hLPwqb1.0DGjQWU25XP93zOodK1UjPf2t.twWvS', '0906666610', 'STUDENT');

-- 2. CENTERS (2 Multi-Tenant Centers)
INSERT IGNORE INTO `centers` (`id`, `owner_user_id`, `name`, `subdomain`, `created_at`) VALUES
(1, 1, 'Owlexa Central Branch - District 1', 'd1-main', '2026-01-10 08:00:00'),
(2, 1, 'Owlexa Innovation Center - Thu Duc', 'thuduc-branch', '2026-02-01 08:00:00');

-- 3. MEMBERSHIP
INSERT IGNORE INTO `membership` (`id`, `center_id`, `user_id`, `joined_by_user_id`, `joined_at`) VALUES
(1, 1, 1, 1, '2026-01-10 08:00:00.000000'),
(2, 2, 1, 1, '2026-02-01 08:00:00.000000'),
(3, 1, 2, 1, '2026-01-10 08:30:00.000000'),
(4, 2, 3, 1, '2026-02-01 08:30:00.000000'),
(5, 1, 4, 1, '2026-01-11 09:00:00.000000'),
(6, 2, 5, 1, '2026-02-02 09:00:00.000000'),
(7, 1, 6, 1, '2026-01-11 09:30:00.000000'),
(8, 2, 7, 1, '2026-02-02 09:30:00.000000'),
(9, 1, 8, 2, '2026-01-15 10:00:00.000000'),
(10, 1, 9, 2, '2026-01-15 10:30:00.000000'),
(11, 1, 10, 2, '2026-01-15 11:00:00.000000'),
(12, 2, 11, 3, '2026-02-05 10:00:00.000000'),
(13, 2, 12, 3, '2026-02-05 10:30:00.000000'),
(14, 1, 13, 4, '2026-02-10 14:00:00.000000'),
(15, 1, 14, 4, '2026-02-10 14:30:00.000000'),
(16, 1, 15, 4, '2026-02-11 09:00:00.000000'),
(17, 1, 16, 4, '2026-02-11 09:30:00.000000'),
(18, 1, 17, 4, '2026-02-12 10:00:00.000000'),
(19, 2, 18, 5, '2026-02-15 14:00:00.000000'),
(20, 2, 19, 5, '2026-02-15 14:30:00.000000'),
(21, 2, 20, 5, '2026-02-16 09:00:00.000000'),
(22, 2, 21, 5, '2026-02-16 09:30:00.000000'),
(23, 2, 22, 5, '2026-02-17 10:00:00.000000');

-- 4. USER_PERMISSION (Custom overrides if needed)
INSERT IGNORE INTO `user_permission` (`id`, `user_id`, `permission_id`, `granted_at`)
SELECT 1, 2, id, NOW() FROM permissions WHERE code = 'CENTER_SETTINGS_UPDATE';

-- 5. USER_SESSIONS
INSERT IGNORE INTO `user_sessions` (`id`, `user_id`, `center_id`, `refresh_token_hash`, `device_key`, `device_type`, `device_name`, `ip_address`, `user_agent`, `is_active`, `rotation_count`, `created_at`, `last_used_at`, `expired_at`, `absolute_expire_at`) VALUES
('sess-owner-001', 1, 1, 'hash_owner_001', 'dev-key-macbook-pro', 'DESKTOP', 'MacBook Pro M3', '14.225.22.10', 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', 1, 0, '2026-07-20 08:00:00.000000', '2026-07-27 10:00:00.000000', '2026-08-20 08:00:00.000000', '2026-10-20 08:00:00.000000'),
('sess-teacher-nam-001', 8, 1, 'hash_teacher_nam', 'dev-key-ipad-air', 'TABLET', 'iPad Air 5', '118.69.182.45', 'Mozilla/5.0 (iPad; CPU OS 17_4 like Mac OS X)', 1, 1, '2026-07-25 09:00:00.000000', '2026-07-27 11:30:00.000000', '2026-08-25 09:00:00.000000', '2026-10-25 09:00:00.000000'),
('sess-student-an-001', 13, 1, 'hash_student_an', 'dev-key-iphone-15', 'MOBILE', 'iPhone 15 Pro', '27.72.105.88', 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X)', 1, 2, '2026-07-26 14:00:00.000000', '2026-07-27 12:00:00.000000', '2026-08-26 14:00:00.000000', '2026-10-26 14:00:00.000000');

-- 6. COURSES (English Curriculum Catalog)
INSERT IGNORE INTO `courses` (`id`, `code`, `name`, `description`, `default_duration`, `default_max_students`, `default_monthly_fee`, `is_active`, `created_at`, `updated_at`) VALUES
(1, 'TOEIC-750', 'TOEIC Intensive 750+', 'Chương trình luyện thi TOEIC cấp tốc mục tiêu 750+ cho sinh viên và người đi làm.', 48, 20, 2500000.00, 1, '2026-01-15 08:00:00.000000', '2026-01-15 08:00:00.000000'),
(2, 'IELTS-ACAD', 'IELTS Academic Master 6.5-7.5', 'Khóa học luyện thi IELTS toàn diện 4 kỹ năng với giáo viên bản ngữ và Việt Nam.', 72, 15, 4500000.00, 1, '2026-01-15 08:30:00.000000', '2026-01-15 08:30:00.000000'),
(3, 'VSTEP-B2', 'VSTEP B2 Readiness', 'Luyện thi chứng chỉ Tiếng Anh B2 theo khung năng lực 6 bậc dùng cho Việt Nam.', 36, 25, 2000000.00, 1, '2026-01-16 09:00:00.000000', '2026-01-16 09:00:00.000000'),
(4, 'COMM-COMM', 'General English Communication', 'Tiếng Anh giao tiếp phản xạ tự nhiên dành cho người mất gốc.', 36, 18, 1800000.00, 1, '2026-01-16 09:30:00.000000', '2026-01-16 09:30:00.000000'),
(5, 'BIZ-ENG', 'Business English & Email Writing', 'Tiếng Anh thương mại, kỹ năng thuyết trình, đàm phán và viết email chuyên nghiệp.', 24, 15, 3000000.00, 1, '2026-01-17 10:00:00.000000', '2026-01-17 10:00:00.000000'),
(6, 'PRON-PRO', 'American Accent & Pronunciation', 'Chuẩn hóa phát âm IPA, ngữ điệu và nối âm chuẩn Anh - Mỹ.', 24, 12, 2200000.00, 1, '2026-01-17 10:30:00.000000', '2026-01-17 10:30:00.000000');

-- 7. ROOMS
INSERT IGNORE INTO `rooms` (`id`, `center_id`, `code`, `name`, `capacity`, `description`, `is_active`, `created_at`, `updated_at`) VALUES
(1, 1, 'D1-101', 'Phòng Oxford 101', 25, 'Phòng trang bị máy chiếu 4K, điều hòa và hệ thống âm thanh vòm.', 1, '2026-01-12 08:00:00.000000', '2026-01-12 08:00:00.000000'),
(2, 1, 'D1-102', 'Phòng Cambridge 102', 20, 'Phòng giao tiếp bàn tròn hiện đại.', 1, '2026-01-12 08:30:00.000000', '2026-01-12 08:30:00.000000'),
(3, 1, 'D1-201', 'Phòng Harvard Computer Lab', 20, 'Phòng máy tính 20 máy cho thi thử TOEIC/IELTS trên máy.', 1, '2026-01-12 09:00:00.000000', '2026-01-12 09:00:00.000000'),
(4, 1, 'D1-ONLINE', 'Phòng Học Zoom Online D1', 100, 'Phòng trực tuyến Zoom Studio HD.', 1, '2026-01-12 09:30:00.000000', '2026-01-12 09:30:00.000000'),
(5, 2, 'TD-A01', 'Phòng Stanford A01', 30, 'Phòng học cơ sở Thủ Đức, bảng tương tác thông minh.', 1, '2026-02-03 08:00:00.000000', '2026-02-03 08:00:00.000000'),
(6, 2, 'TD-A02', 'Phòng MIT A02', 20, 'Phòng luyện Speaking kỹ năng cao.', 1, '2026-02-03 08:30:00.000000', '2026-02-03 08:30:00.000000'),
(7, 2, 'TD-LAB', 'Phòng Listening & Tech Lab', 25, 'Phòng Lab trang bị tai nghe chuyên dụng chống ồn.', 1, '2026-02-03 09:00:00.000000', '2026-02-03 09:00:00.000000'),
(8, 2, 'TD-ONLINE', 'Phòng Học Zoom Online Thủ Đức', 100, 'Phòng trực tuyến Zoom Studio HD.', 1, '2026-02-03 09:30:00.000000', '2026-02-03 09:30:00.000000');

-- 8. CLASSES (Note column name is `create_at`)
INSERT IGNORE INTO `classes` (`id`, `center_id`, `course_id`, `name`, `description`, `max_students`, `monthly_fee`, `status`, `create_at`) VALUES
(1, 1, 1, 'TOEIC-750-K24', 'Lớp TOEIC 750+ Khóa 24 - Tối 2-4-6', 20, 2500000.00, 'ACTIVE', '2026-02-01 08:00:00.000000'),
(2, 1, 2, 'IELTS-MASTER-K12', 'Lớp IELTS Master 7.0 - Tối 3-5-7', 15, 4500000.00, 'ACTIVE', '2026-02-01 08:30:00.000000'),
(3, 1, 3, 'VSTEP-B2-K08', 'Lớp VSTEP B2 Mục Tiêu Đầu Ra - Sáng T7-CN', 25, 2000000.00, 'ACTIVE', '2026-02-02 09:00:00.000000'),
(4, 1, 4, 'COMM-BASIC-K30', 'Lớp Tiếng Anh Giao Tiếp Cơ Bản - Tối 2-4-6', 18, 1800000.00, 'ACTIVE', '2026-02-02 09:30:00.000000'),
(5, 1, 5, 'BIZ-ENG-PRO', 'Lớp Tiếng Anh Doanh Nghiệp - Tối 3-5', 15, 3000000.00, 'PLANNED', '2026-02-03 10:00:00.000000'),
(6, 2, 1, 'TD-TOEIC-750-K01', 'Lớp TOEIC 750+ Thủ Đức K01 - Tối 3-5-7', 20, 2400000.00, 'ACTIVE', '2026-02-10 08:00:00.000000'),
(7, 2, 2, 'TD-IELTS-ACAD-K03', 'Lớp IELTS Academic Thủ Đức K03 - Tối 2-4-6', 15, 4400000.00, 'ACTIVE', '2026-02-10 08:30:00.000000'),
(8, 2, 4, 'TD-COMM-K05', 'Lớp Phản Xạ Giao Tiếp Thủ Đức - Sáng T7-CN', 18, 1750000.00, 'ACTIVE', '2026-02-11 09:00:00.000000'),
(9, 2, 6, 'TD-PRON-K02', 'Lớp Chuẩn Hóa Phát Âm K02 - Chiều CN', 12, 2100000.00, 'FINISHED', '2026-01-05 09:00:00.000000'),
(10, 2, 3, 'TD-VSTEP-K04', 'Lớp Luyện Thi VSTEP B2 Thủ Đức', 25, 1950000.00, 'PLANNED', '2026-02-15 10:00:00.000000');

-- 9. TEACHER CENTER PROFILE
INSERT IGNORE INTO `teacher_center_profile` (`id`, `center_id`, `teacher_user_id`, `salary`, `currency`, `created_at`, `updated_at`) VALUES
(1, 1, 8, 25000000.00, 'VND', '2026-01-15 10:00:00.000000', '2026-01-15 10:00:00.000000'),
(2, 1, 9, 22000000.00, 'VND', '2026-01-15 10:30:00.000000', '2026-01-15 10:30:00.000000'),
(3, 1, 10, 45000000.00, 'VND', '2026-01-15 11:00:00.000000', '2026-01-15 11:00:00.000000'),
(4, 2, 11, 24000000.00, 'VND', '2026-02-05 10:00:00.000000', '2026-02-05 10:00:00.000000'),
(5, 2, 12, 42000000.00, 'VND', '2026-02-05 10:30:00.000000', '2026-02-05 10:30:00.000000');

-- 10. SCHEDULES
INSERT IGNORE INTO `schedules` (`id`, `center_id`, `class_id`, `room_id`, `teacher_user_id`, `day_of_week`, `start_time`, `end_time`, `type`, `created_at`) VALUES
(1, 1, 1, 1, 8, 'MONDAY', '18:30:00', '20:30:00', 'THEORY_CLASS', '2026-02-01 09:00:00.000000'),
(2, 1, 1, 1, 8, 'WEDNESDAY', '18:30:00', '20:30:00', 'THEORY_CLASS', '2026-02-01 09:00:00.000000'),
(3, 1, 1, 3, 8, 'FRIDAY', '18:30:00', '20:30:00', 'EXAM', '2026-02-01 09:00:00.000000'),
(4, 1, 2, 2, 10, 'TUESDAY', '18:00:00', '20:30:00', 'THEORY_CLASS', '2026-02-01 09:30:00.000000'),
(5, 1, 2, 2, 10, 'THURSDAY', '18:00:00', '20:30:00', 'THEORY_CLASS', '2026-02-01 09:30:00.000000'),
(6, 1, 2, 2, 10, 'SATURDAY', '18:00:00', '20:30:00', 'THEORY_CLASS', '2026-02-01 09:30:00.000000'),
(7, 1, 3, 1, 9, 'SATURDAY', '08:30:00', '11:30:00', 'THEORY_CLASS', '2026-02-02 10:00:00.000000'),
(8, 1, 3, 1, 9, 'SUNDAY', '08:30:00', '11:30:00', 'THEORY_CLASS', '2026-02-02 10:00:00.000000'),
(9, 1, 4, 2, 9, 'MONDAY', '19:00:00', '20:30:00', 'THEORY_CLASS', '2026-02-02 10:30:00.000000'),
(10, 1, 4, 2, 9, 'WEDNESDAY', '19:00:00', '20:30:00', 'THEORY_CLASS', '2026-02-02 10:30:00.000000'),
(11, 2, 6, 5, 11, 'TUESDAY', '18:30:00', '20:30:00', 'THEORY_CLASS', '2026-02-10 09:00:00.000000'),
(12, 2, 6, 5, 11, 'THURSDAY', '18:30:00', '20:30:00', 'THEORY_CLASS', '2026-02-10 09:00:00.000000'),
(13, 2, 7, 6, 12, 'MONDAY', '18:00:00', '20:30:00', 'THEORY_CLASS', '2026-02-10 09:30:00.000000'),
(14, 2, 7, 6, 12, 'WEDNESDAY', '18:00:00', '20:30:00', 'THEORY_CLASS', '2026-02-10 09:30:00.000000'),
(15, 2, 8, 5, 11, 'SATURDAY', '09:00:00', '11:00:00', 'THEORY_CLASS', '2026-02-11 10:00:00.000000');

-- 11. CLASS ENROLLMENTS
INSERT IGNORE INTO `class_enrollments` (`id`, `center_id`, `class_id`, `student_user_id`, `enrolled_by_user_id`, `status`, `enrolled_at`) VALUES
(1, 1, 1, 13, 4, 'ACTIVE', '2026-02-05 10:00:00.000000'),
(2, 1, 1, 14, 4, 'ACTIVE', '2026-02-05 10:30:00.000000'),
(3, 1, 1, 15, 4, 'ACTIVE', '2026-02-06 09:00:00.000000'),
(4, 1, 2, 16, 4, 'ACTIVE', '2026-02-06 09:30:00.000000'),
(5, 1, 2, 17, 4, 'ACTIVE', '2026-02-07 10:00:00.000000'),
(6, 1, 3, 13, 4, 'ACTIVE', '2026-02-07 10:30:00.000000'),
(7, 1, 4, 14, 4, 'ACTIVE', '2026-02-08 11:00:00.000000'),
(8, 2, 6, 18, 5, 'ACTIVE', '2026-02-12 14:00:00.000000'),
(9, 2, 6, 19, 5, 'ACTIVE', '2026-02-12 14:30:00.000000'),
(10, 2, 7, 20, 5, 'ACTIVE', '2026-02-13 09:00:00.000000'),
(11, 2, 7, 21, 5, 'ACTIVE', '2026-02-13 09:30:00.000000'),
(12, 2, 8, 22, 5, 'ACTIVE', '2026-02-14 10:00:00.000000');

-- 12. ATTENDANCES & TEACHER ATTENDANCES
INSERT IGNORE INTO `attendances` (`id`, `center_id`, `schedule_id`, `student_user_id`, `marked_by_user_id`, `status`, `note`, `date`, `created_at`) VALUES
(1, 1, 1, 13, 8, 'PRESENT', 'Đến lớp đúng giờ, phát biểu tích cực.', '2026-07-20', '2026-07-20 20:35:00.000000'),
(2, 1, 1, 14, 8, 'PRESENT', 'Đi đúng giờ.', '2026-07-20', '2026-07-20 20:35:00.000000'),
(3, 1, 1, 15, 8, 'LATE', 'Đi trễ 15 phút do kẹt xe.', '2026-07-20', '2026-07-20 20:35:00.000000'),
(4, 1, 2, 13, 8, 'PRESENT', 'Hoàn thành bài tập về nhà.', '2026-07-22', '2026-07-22 20:35:00.000000'),
(5, 1, 2, 14, 8, 'ABSENT', 'Nghỉ học có phép (bị sốt).', '2026-07-22', '2026-07-22 20:35:00.000000'),
(6, 2, 11, 18, 11, 'PRESENT', 'Tích cực trao đổi nhóm.', '2026-07-21', '2026-07-21 20:35:00.000000'),
(7, 2, 11, 19, 11, 'PRESENT', 'Đến đúng giờ.', '2026-07-21', '2026-07-21 20:35:00.000000');

INSERT IGNORE INTO `teacher_attendances` (`id`, `center_id`, `teacher_user_id`, `marked_by_user_id`, `status`, `note`, `date`, `created_at`) VALUES
(1, 1, 8, 4, 'PRESENT', 'Giảng dạy đầy đủ 2 ca.', '2026-07-20', '2026-07-20 21:00:00.000000'),
(2, 1, 10, 4, 'PRESENT', 'Đúng giờ, nhiệt tình.', '2026-07-21', '2026-07-21 21:00:00.000000'),
(3, 2, 11, 5, 'PRESENT', 'Hoàn thành giáo án.', '2026-07-21', '2026-07-21 21:00:00.000000');

-- 13. STUDENT DOCUMENTS (Note column name is `clazz_id`)
INSERT IGNORE INTO `student_documents` (`id`, `center_id`, `clazz_id`, `student_user_id`, `title`, `description`, `file_url`, `document_type`, `created_at`) VALUES
(1, 1, 1, 13, 'Syllabus TOEIC 750+ Intensive', 'Giáo trình & Lộ trình 24 buổi TOEIC 750+', 'https://cdn.owlexa.edu.vn/docs/toeic-750-syllabus.pdf', 'PDF', '2026-02-05 11:00:00.000000'),
(2, 1, 2, 16, 'IELTS Academic Writing Task 2 Guide', 'Tài liệu hướng dẫn triển khai ý bài viết Essay Task 2', 'https://cdn.owlexa.edu.vn/docs/ielts-writing-guide.pdf', 'PDF', '2026-02-06 10:00:00.000000'),
(3, 2, 6, 18, 'Video Bài Giảng Listening Part 3 & 4', 'Video quay lại buổi học Listening nâng cao', 'https://cdn.owlexa.edu.vn/videos/toeic-listening-p34.mp4', 'VIDEO', '2026-02-12 15:00:00.000000');

-- 14. FEE RECORDS & DISCOUNTS & INSTALLMENTS
INSERT IGNORE INTO `fee_records` (`id`, `center_id`, `class_id`, `student_user_id`, `month`, `amount`, `discount_amount`, `paid_amount`, `due_date`, `status`, `created_at`) VALUES
(1, 1, 1, 13, '2026-07', 2500000.00, 200000.00, 2300000.00, '2026-07-10', 'PAID', '2026-07-01 08:00:00.000000'),
(2, 1, 1, 14, '2026-07', 2500000.00, 0.00, 2500000.00, '2026-07-10', 'PAID', '2026-07-01 08:00:00.000000'),
(3, 1, 1, 15, '2026-07', 2500000.00, 0.00, 1000000.00, '2026-07-10', 'PARTIAL', '2026-07-01 08:00:00.000000'),
(4, 1, 2, 16, '2026-07', 4500000.00, 500000.00, 4000000.00, '2026-07-10', 'PAID', '2026-07-01 08:00:00.000000'),
(5, 1, 2, 17, '2026-07', 4500000.00, 0.00, 0.00, '2026-07-05', 'OVERDUE', '2026-07-01 08:00:00.000000'),
(6, 2, 6, 18, '2026-07', 2400000.00, 0.00, 2400000.00, '2026-07-10', 'PAID', '2026-07-01 08:00:00.000000'),
(7, 2, 6, 19, '2026-07', 2400000.00, 0.00, 0.00, '2026-07-28', 'UNPAID', '2026-07-01 08:00:00.000000');

INSERT IGNORE INTO `discounts` (`id`, `center_id`, `fee_record_id`, `created_by_user_id`, `name`, `type`, `value`, `reason`, `created_at`) VALUES
(1, 1, 1, 6, 'Ưu đãi Đăng ký sớm (Early Bird)', 'FIXED', 200000.00, 'Học viên đăng ký trước ngày 05/02/2026.', '2026-07-01 08:30:00.000000'),
(2, 1, 4, 6, 'Học bổng Cựu Học Viên', 'FIXED', 500000.00, 'Giảm 500k cho cựu học viên hoàn thành khóa TOEIC.', '2026-07-01 09:00:00.000000');

INSERT IGNORE INTO `installments` (`id`, `center_id`, `fee_record_id`, `expected_amount`, `paid_amount`, `due_date`, `status`) VALUES
(1, 1, 3, 1000000.00, 1000000.00, '2026-07-05', 'PAID'),
(2, 1, 3, 1500000.00, 0.00, '2026-08-05', 'PENDING');

-- 15. PAYMENTS & REFUNDS & SEPAY WEBHOOK
INSERT IGNORE INTO `payments` (`id`, `center_id`, `fee_record_id`, `student_user_id`, `collected_by_user_id`, `receipt_number`, `idempotency_key`, `amount`, `method`, `status`, `sepay_ref`, `note`, `created_at`, `expires_at`) VALUES
(1, 1, 1, 13, 6, 'REC-202607-001', 'idemp-001', 2300000.00, 'SEPAY', 'ACTIVE', 'SEPAY-TX-998811', 'Thanh toán HP TOEIC qua ngân hàng SePay', '2026-07-02 10:00:00.000000', NULL),
(2, 1, 2, 14, 6, 'REC-202607-002', 'idemp-002', 2500000.00, 'CASH', 'ACTIVE', NULL, 'Đã thu tiền mặt tại quầy Quận 1', '2026-07-03 14:00:00.000000', NULL),
(3, 1, 3, 15, 6, 'REC-202607-003', 'idemp-003', 1000000.00, 'BANK_TRANSFER', 'ACTIVE', NULL, 'Đợt 1 HP TOEIC', '2026-07-04 11:00:00.000000', NULL),
(4, 1, 4, 16, 6, 'REC-202607-004', 'idemp-004', 4000000.00, 'SEPAY', 'ACTIVE', 'SEPAY-TX-998822', 'Thanh toán học phí IELTS Master', '2026-07-04 15:30:00.000000', NULL),
(5, 2, 6, 18, 7, 'REC-202607-005', 'idemp-005', 2400000.00, 'BANK_TRANSFER', 'ACTIVE', NULL, 'Thu tiền học phí Thủ Đức', '2026-07-05 09:00:00.000000', NULL);

INSERT IGNORE INTO `sepay_webhook_events` (`id`, `sepay_transaction_id`, `gateway`, `transaction_date`, `account_number`, `sub_account`, `transfer_type`, `transfer_amount`, `payment_code`, `content`, `reference_code`, `raw_payload`, `processing_status`, `processing_note`, `matched_payment_id`, `received_at`, `processed_at`) VALUES
(1, 998811, 'MBBank', '2026-07-02 10:00:00', '70740011223344', 'OWLEXA01', 'in', 2300000, 'OWX131', 'OWX131 NGUYEN VAN AN THANH TOAN HOC PHI', 'FT261839001', '{"id":998811,"gateway":"MBBank"}', 'MATCHED', 'Tự động khớp lệnh giao dịch SePay', 1, '2026-07-02 10:00:01.000000', '2026-07-02 10:00:02.000000'),
(2, 998822, 'MBBank', '2026-07-04 15:30:00', '70740011223344', 'OWLEXA01', 'in', 4000000, 'OWX164', 'OWX164 PHAM THUY DUNG CHUYEN KHOAN IELTS', 'FT261839002', '{"id":998822,"gateway":"MBBank"}', 'MATCHED', 'Tự động khớp lệnh giao dịch SePay', 4, '2026-07-04 15:30:01.000000', '2026-07-04 15:30:02.000000');

INSERT IGNORE INTO `audit_logs` (`id`, `center_id`, `user_id`, `action`, `entity_type`, `entity_id`, `description`, `ip_address`, `created_at`) VALUES
(1, 1, 6, 'CREATE_PAYMENT', 'PAYMENT', 1, 'Tạo biên lai thanh toán học phí REC-202607-001 số tiền 2,300,000 VND', '14.225.22.10', '2026-07-02 10:00:02.000000'),
(2, 1, 8, 'MARK_ATTENDANCE', 'ATTENDANCE', 1, 'Điểm danh buổi học TOEIC-750-K24 ngày 2026-07-20', '118.69.182.45', '2026-07-20 20:35:00.000000');

-- 16. GRADING CRITERIA & QUESTION COLLECTIONS & QUESTIONS & OPTIONS
INSERT IGNORE INTO `grading_criteria` (`id`, `center_id`, `created_by`, `updated_by`, `name`, `content`, `created_at`, `updated_at`) VALUES
(1, 1, 8, 8, 'Tiêu chí Chấm IELTS Essay Task 2 (TR, CC, LR, GRA)', 'Task Response (25%), Coherence & Cohesion (25%), Lexical Resource (25%), Grammatical Range & Accuracy (25%).', '2026-02-15 08:00:00.000000', '2026-02-15 08:00:00.000000'),
(2, 1, 8, 8, 'Tiêu chí Chấm VSTEP Writing Part 2', 'Bố cục bài viết (20%), Nội dung ý tưởng (30%), Từ vựng (25%), Ngữ pháp & Cấu trúc câu (25%).', '2026-02-15 08:30:00.000000', '2026-02-15 08:30:00.000000');

INSERT IGNORE INTO `question_collections` (`id`, `center_id`, `code`, `name`, `description`, `created_by`, `updated_by`, `created_at`, `updated_at`) VALUES
(1, 1, 'TOEIC_TEST_1', 'TOEIC Test 1', 'Bộ câu hỏi TOEIC hoàn chỉnh dùng cho luyện nghe và đọc.', 8, 8, '2026-02-16 08:00:00.000000', '2026-02-16 08:00:00.000000'),
(2, 1, 'TOEIC_TEST_2', 'TOEIC Test 2', 'Bộ câu hỏi TOEIC bổ sung cho Question Picker.', 8, 8, '2026-02-16 08:10:00.000000', '2026-02-16 08:10:00.000000'),
(3, 1, 'GRAMMAR_PRACTICE', 'Grammar Practice', 'Bài luyện tập ngữ pháp theo chủ điểm.', 8, 8, '2026-02-16 08:20:00.000000', '2026-02-16 08:20:00.000000'),
(4, 1, 'IELTS_SAMPLE', 'IELTS Sample', 'Câu hỏi mẫu IELTS Reading và Writing.', 10, 10, '2026-02-16 08:30:00.000000', '2026-02-16 08:30:00.000000');

INSERT IGNORE INTO `questions` (`id`, `center_id`, `collection_id`, `section_code`, `display_order`, `created_by`, `updated_by`, `grading_criteria_id`, `type`, `difficulty`, `points`, `content`, `sample_answer`, `explanation`, `created_at`, `updated_at`) VALUES
(1, 1, 1, 'PART_1', 1, 8, 8, NULL, 'MULTIPLE_CHOICE', 'EASY', 1.00, '', NULL, 'The woman is stepping onto a bus.', '2026-02-20 09:00:00.000000', '2026-02-20 09:00:00.000000'),
(2, 1, 1, 'PART_1', 2, 8, 8, NULL, 'MULTIPLE_CHOICE', 'EASY', 1.00, '', NULL, 'Several workers are repairing the road.', '2026-02-20 09:01:00.000000', '2026-02-20 09:01:00.000000'),
(3, 1, 1, 'PART_1', 3, 8, 8, NULL, 'MULTIPLE_CHOICE', 'EASY', 1.00, '', NULL, 'A presenter is pointing at a chart.', '2026-02-20 09:02:00.000000', '2026-02-20 09:02:00.000000'),
(4, 1, 1, 'PART_1', 4, 8, 8, NULL, 'MULTIPLE_CHOICE', 'EASY', 1.00, '', NULL, 'Dishes have been placed on the counter.', '2026-02-20 09:03:00.000000', '2026-02-20 09:03:00.000000'),
(5, 1, 1, 'PART_2', 5, 8, 8, NULL, 'MULTIPLE_CHOICE', 'MEDIUM', 1.00, '', NULL, 'The response directly answers where the meeting will be held.', '2026-02-20 09:04:00.000000', '2026-02-20 09:04:00.000000'),
(6, 1, 1, 'PART_2', 6, 8, 8, NULL, 'MULTIPLE_CHOICE', 'MEDIUM', 1.00, '', NULL, 'The response confirms the revised delivery time.', '2026-02-20 09:05:00.000000', '2026-02-20 09:05:00.000000'),
(7, 1, 1, 'PART_5', 7, 8, 8, NULL, 'MULTIPLE_CHOICE', 'EASY', 1.00, 'The committee _______ to approve the new marketing budget proposed by the director yesterday.', NULL, 'The singular subject and past-time marker require "decided".', '2026-02-20 09:06:00.000000', '2026-02-20 09:06:00.000000'),
(8, 1, 1, 'PART_5', 8, 8, 8, NULL, 'MULTIPLE_CHOICE', 'MEDIUM', 1.00, 'All candidates are required to submit their updated resumes prior to the scheduled _______ next Monday.', NULL, '"Scheduled" modifies the noun "interview".', '2026-02-20 09:07:00.000000', '2026-02-20 09:07:00.000000'),
(9, 1, 2, 'PART_1', 1, 8, 8, NULL, 'MULTIPLE_CHOICE', 'EASY', 1.00, '', NULL, 'Passengers are waiting beside a train.', '2026-02-20 09:10:00.000000', '2026-02-20 09:10:00.000000'),
(10, 1, 2, 'PART_1', 2, 8, 8, NULL, 'MULTIPLE_CHOICE', 'EASY', 1.00, '', NULL, 'A guest is speaking with a receptionist.', '2026-02-20 09:11:00.000000', '2026-02-20 09:11:00.000000'),
(11, 1, 2, 'PART_2', 3, 8, 8, NULL, 'MULTIPLE_CHOICE', 'MEDIUM', 1.00, '', NULL, 'The response accepts the invitation.', '2026-02-20 09:12:00.000000', '2026-02-20 09:12:00.000000'),
(12, 1, 2, 'PART_2', 4, 8, 8, NULL, 'MULTIPLE_CHOICE', 'MEDIUM', 1.00, '', NULL, 'The response explains when the technician will arrive.', '2026-02-20 09:13:00.000000', '2026-02-20 09:13:00.000000'),
(13, 1, 3, 'GRAMMAR', 1, 8, 8, NULL, 'MULTIPLE_CHOICE', 'EASY', 1.00, 'She _______ at this company since 2022.', NULL, '"Since 2022" requires the present perfect.', '2026-02-20 09:20:00.000000', '2026-02-20 09:20:00.000000'),
(14, 1, 3, 'GRAMMAR', 2, 8, 8, NULL, 'MULTIPLE_CHOICE', 'MEDIUM', 1.00, 'If the weather improves, we _______ the event outdoors.', NULL, 'The first conditional uses will plus the base verb.', '2026-02-20 09:21:00.000000', '2026-02-20 09:21:00.000000'),
(15, 1, 3, 'GRAMMAR', 3, 8, 8, NULL, 'MULTIPLE_CHOICE', 'MEDIUM', 1.00, 'The final report _______ by the audit team yesterday.', NULL, 'A past passive construction is required.', '2026-02-20 09:22:00.000000', '2026-02-20 09:22:00.000000'),
(16, 1, 3, 'GRAMMAR', 4, 8, 8, NULL, 'MULTIPLE_CHOICE', 'HARD', 1.00, 'The consultant _______ advised us specializes in data security.', NULL, '"Who" introduces a defining relative clause for a person.', '2026-02-20 09:23:00.000000', '2026-02-20 09:23:00.000000'),
(17, 1, 4, 'WRITING', 1, 10, 10, 1, 'ESSAY', 'HARD', 10.00, 'Write an essay discussing the advantages and disadvantages of using Artificial Intelligence tools in higher education.', 'In recent years, Artificial Intelligence has transformed higher education. While it offers personalized learning and administrative efficiency, it also creates challenges involving academic integrity and reduced human interaction.', 'Present a clear introduction, balanced body paragraphs, and a cohesive conclusion using formal academic vocabulary.', '2026-02-20 10:00:00.000000', '2026-02-20 10:00:00.000000'),
(18, 1, 4, 'READING', 2, 10, 10, NULL, 'MULTIPLE_CHOICE', 'MEDIUM', 2.00, 'City planners increasingly view parks as essential infrastructure because they reduce heat, absorb rainwater, and provide residents with places to exercise and socialize. What is the main purpose of the passage?', NULL, 'The passage summarizes several benefits of urban parks.', '2026-02-20 10:05:00.000000', '2026-02-20 10:05:00.000000'),
(19, 1, 4, 'WRITING', 3, 10, 10, 1, 'ESSAY', 'MEDIUM', 10.00, 'Some companies allow employees to work remotely several days a week. Discuss whether the benefits outweigh the disadvantages.', NULL, 'Support the position with relevant reasons and examples.', '2026-02-20 10:10:00.000000', '2026-02-20 10:10:00.000000');

INSERT IGNORE INTO `question_options` (`id`, `question_id`, `display_order`, `content`, `is_correct`, `created_at`, `updated_at`) VALUES
(1, 1, 1, 'She is boarding a bus.', 1, '2026-02-20 09:00:00.000000', '2026-02-20 09:00:00.000000'),
(2, 1, 2, 'She is opening a suitcase.', 0, '2026-02-20 09:00:00.000000', '2026-02-20 09:00:00.000000'),
(3, 1, 3, 'She is crossing a bridge.', 0, '2026-02-20 09:00:00.000000', '2026-02-20 09:00:00.000000'),
(4, 1, 4, 'She is buying a newspaper.', 0, '2026-02-20 09:00:00.000000', '2026-02-20 09:00:00.000000'),
(5, 2, 1, 'Workers are repairing a road.', 1, '2026-02-20 09:01:00.000000', '2026-02-20 09:01:00.000000'),
(6, 2, 2, 'Cars are parked inside a garage.', 0, '2026-02-20 09:01:00.000000', '2026-02-20 09:01:00.000000'),
(7, 2, 3, 'A bridge is being painted.', 0, '2026-02-20 09:01:00.000000', '2026-02-20 09:01:00.000000'),
(8, 2, 4, 'People are waiting at a crossing.', 0, '2026-02-20 09:01:00.000000', '2026-02-20 09:01:00.000000'),
(9, 3, 1, 'A presenter is pointing at a chart.', 1, '2026-02-20 09:02:00.000000', '2026-02-20 09:02:00.000000'),
(10, 3, 2, 'The audience is leaving the room.', 0, '2026-02-20 09:02:00.000000', '2026-02-20 09:02:00.000000'),
(11, 3, 3, 'A screen is being removed.', 0, '2026-02-20 09:02:00.000000', '2026-02-20 09:02:00.000000'),
(12, 3, 4, 'Some chairs are being stacked.', 0, '2026-02-20 09:02:00.000000', '2026-02-20 09:02:00.000000'),
(13, 4, 1, 'Dishes are arranged on a counter.', 1, '2026-02-20 09:03:00.000000', '2026-02-20 09:03:00.000000'),
(14, 4, 2, 'Customers are reading menus outside.', 0, '2026-02-20 09:03:00.000000', '2026-02-20 09:03:00.000000'),
(15, 4, 3, 'A table is being carried upstairs.', 0, '2026-02-20 09:03:00.000000', '2026-02-20 09:03:00.000000'),
(16, 4, 4, 'The kitchen is being cleaned.', 0, '2026-02-20 09:03:00.000000', '2026-02-20 09:03:00.000000'),
(17, 5, 1, 'In the conference room on the second floor.', 1, '2026-02-20 09:04:00.000000', '2026-02-20 09:04:00.000000'),
(18, 5, 2, 'Yes, I met her yesterday.', 0, '2026-02-20 09:04:00.000000', '2026-02-20 09:04:00.000000'),
(19, 5, 3, 'About thirty minutes ago.', 0, '2026-02-20 09:04:00.000000', '2026-02-20 09:04:00.000000'),
(20, 6, 1, 'It should arrive by three o’clock.', 1, '2026-02-20 09:05:00.000000', '2026-02-20 09:05:00.000000'),
(21, 6, 2, 'The loading dock is behind the building.', 0, '2026-02-20 09:05:00.000000', '2026-02-20 09:05:00.000000'),
(22, 6, 3, 'I ordered two boxes.', 0, '2026-02-20 09:05:00.000000', '2026-02-20 09:05:00.000000'),
(23, 7, 1, 'decide', 0, '2026-02-20 09:06:00.000000', '2026-02-20 09:06:00.000000'),
(24, 7, 2, 'decides', 0, '2026-02-20 09:06:00.000000', '2026-02-20 09:06:00.000000'),
(25, 7, 3, 'decided', 1, '2026-02-20 09:06:00.000000', '2026-02-20 09:06:00.000000'),
(26, 7, 4, 'deciding', 0, '2026-02-20 09:06:00.000000', '2026-02-20 09:06:00.000000'),
(27, 8, 1, 'interview', 1, '2026-02-20 09:07:00.000000', '2026-02-20 09:07:00.000000'),
(28, 8, 2, 'interviewed', 0, '2026-02-20 09:07:00.000000', '2026-02-20 09:07:00.000000'),
(29, 8, 3, 'interviewer', 0, '2026-02-20 09:07:00.000000', '2026-02-20 09:07:00.000000'),
(30, 8, 4, 'interviewing', 0, '2026-02-20 09:07:00.000000', '2026-02-20 09:07:00.000000'),
(31, 9, 1, 'Passengers are waiting beside a train.', 1, '2026-02-20 09:10:00.000000', '2026-02-20 09:10:00.000000'),
(32, 9, 2, 'Luggage is being loaded onto a plane.', 0, '2026-02-20 09:10:00.000000', '2026-02-20 09:10:00.000000'),
(33, 9, 3, 'A ticket counter has closed.', 0, '2026-02-20 09:10:00.000000', '2026-02-20 09:10:00.000000'),
(34, 9, 4, 'A bus is leaving a station.', 0, '2026-02-20 09:10:00.000000', '2026-02-20 09:10:00.000000'),
(35, 10, 1, 'A guest is speaking with a receptionist.', 1, '2026-02-20 09:11:00.000000', '2026-02-20 09:11:00.000000'),
(36, 10, 2, 'The lobby furniture is being delivered.', 0, '2026-02-20 09:11:00.000000', '2026-02-20 09:11:00.000000'),
(37, 10, 3, 'A room key is lying on the floor.', 0, '2026-02-20 09:11:00.000000', '2026-02-20 09:11:00.000000'),
(38, 10, 4, 'Guests are carrying tables outside.', 0, '2026-02-20 09:11:00.000000', '2026-02-20 09:11:00.000000'),
(39, 11, 1, 'I’d be happy to join you.', 1, '2026-02-20 09:12:00.000000', '2026-02-20 09:12:00.000000'),
(40, 11, 2, 'At the restaurant across the street.', 0, '2026-02-20 09:12:00.000000', '2026-02-20 09:12:00.000000'),
(41, 11, 3, 'The menu was printed yesterday.', 0, '2026-02-20 09:12:00.000000', '2026-02-20 09:12:00.000000'),
(42, 12, 1, 'The technician will be here this afternoon.', 1, '2026-02-20 09:13:00.000000', '2026-02-20 09:13:00.000000'),
(43, 12, 2, 'It is next to the copy room.', 0, '2026-02-20 09:13:00.000000', '2026-02-20 09:13:00.000000'),
(44, 12, 3, 'Please print three copies.', 0, '2026-02-20 09:13:00.000000', '2026-02-20 09:13:00.000000'),
(45, 13, 1, 'has worked', 1, '2026-02-20 09:20:00.000000', '2026-02-20 09:20:00.000000'),
(46, 13, 2, 'worked', 0, '2026-02-20 09:20:00.000000', '2026-02-20 09:20:00.000000'),
(47, 13, 3, 'is working', 0, '2026-02-20 09:20:00.000000', '2026-02-20 09:20:00.000000'),
(48, 13, 4, 'will work', 0, '2026-02-20 09:20:00.000000', '2026-02-20 09:20:00.000000'),
(49, 14, 1, 'will hold', 1, '2026-02-20 09:21:00.000000', '2026-02-20 09:21:00.000000'),
(50, 14, 2, 'held', 0, '2026-02-20 09:21:00.000000', '2026-02-20 09:21:00.000000'),
(51, 14, 3, 'would hold', 0, '2026-02-20 09:21:00.000000', '2026-02-20 09:21:00.000000'),
(52, 14, 4, 'holding', 0, '2026-02-20 09:21:00.000000', '2026-02-20 09:21:00.000000'),
(53, 15, 1, 'was completed', 1, '2026-02-20 09:22:00.000000', '2026-02-20 09:22:00.000000'),
(54, 15, 2, 'completed', 0, '2026-02-20 09:22:00.000000', '2026-02-20 09:22:00.000000'),
(55, 15, 3, 'has completing', 0, '2026-02-20 09:22:00.000000', '2026-02-20 09:22:00.000000'),
(56, 15, 4, 'is complete', 0, '2026-02-20 09:22:00.000000', '2026-02-20 09:22:00.000000'),
(57, 16, 1, 'who', 1, '2026-02-20 09:23:00.000000', '2026-02-20 09:23:00.000000'),
(58, 16, 2, 'which', 0, '2026-02-20 09:23:00.000000', '2026-02-20 09:23:00.000000'),
(59, 16, 3, 'where', 0, '2026-02-20 09:23:00.000000', '2026-02-20 09:23:00.000000'),
(60, 16, 4, 'whose', 0, '2026-02-20 09:23:00.000000', '2026-02-20 09:23:00.000000'),
(61, 18, 1, 'To explain why urban parks are valuable infrastructure.', 1, '2026-02-20 10:05:00.000000', '2026-02-20 10:05:00.000000'),
(62, 18, 2, 'To compare several forms of public transportation.', 0, '2026-02-20 10:05:00.000000', '2026-02-20 10:05:00.000000'),
(63, 18, 3, 'To argue that cities should remove exercise facilities.', 0, '2026-02-20 10:05:00.000000', '2026-02-20 10:05:00.000000'),
(64, 18, 4, 'To describe a new residential development.', 0, '2026-02-20 10:05:00.000000', '2026-02-20 10:05:00.000000');

-- 17. ASSESSMENTS & ASSESSMENT ITEMS & OPTIONS
INSERT IGNORE INTO `assessments` (`id`, `center_id`, `created_by`, `updated_by`, `title`, `description`, `status`, `created_at`, `updated_at`) VALUES
(1, 1, 8, 8, 'TOEIC Grammar & Vocabulary Midterm Test', 'Đề thi giữa kỳ đánh giá từ vựng và ngữ pháp TOEIC.', 'PUBLISHED', '2026-03-01 08:00:00.000000', '2026-03-01 08:00:00.000000'),
(2, 1, 10, 10, 'IELTS Academic Essay Writing Assessment', 'Đề kiểm tra kỹ năng viết Essay Task 2.', 'PUBLISHED', '2026-03-01 09:00:00.000000', '2026-03-01 09:00:00.000000'),
(3, 1, 8, 8, 'TOEIC Test 1 Listening Practice', 'Bài luyện nghe sử dụng các câu Part 1 và Part 2 từ TOEIC Test 1.', 'PUBLISHED', '2026-03-01 10:00:00.000000', '2026-03-01 10:00:00.000000');

INSERT IGNORE INTO `assessment_items` (`id`, `assessment_id`, `question_id`, `grading_criteria_id`, `display_order`, `title`, `question_type`, `difficulty`, `points`, `content`, `sample_answer`, `explanation`, `grading_criteria_name`, `grading_criteria_content`, `created_at`, `updated_at`) VALUES
(1, 1, 7, NULL, 1, 'Budget approval', 'MULTIPLE_CHOICE', 'EASY', 1.00, 'The committee _______ to approve the new marketing budget proposed by the director yesterday.', NULL, 'The singular subject and past-time marker require "decided".', NULL, NULL, '2026-03-01 08:10:00.000000', '2026-03-01 08:10:00.000000'),
(2, 1, 8, NULL, 2, 'Job interview', 'MULTIPLE_CHOICE', 'MEDIUM', 1.00, 'All candidates are required to submit their updated resumes prior to the scheduled _______ next Monday.', NULL, '"Scheduled" modifies the noun "interview".', NULL, NULL, '2026-03-01 08:15:00.000000', '2026-03-01 08:15:00.000000'),
(3, 2, 17, 1, 1, 'Artificial intelligence in education', 'ESSAY', 'HARD', 10.00, 'Write an essay discussing the advantages and disadvantages of using Artificial Intelligence tools in higher education.', 'In recent years, Artificial Intelligence has transformed higher education.', 'Present a clear introduction, balanced body paragraphs, and a cohesive conclusion.', 'Tiêu chí Chấm IELTS Essay Task 2', 'Task Response, CC, LR, GRA', '2026-03-01 09:10:00.000000', '2026-03-01 09:10:00.000000');

INSERT IGNORE INTO `assessment_item_options` (`id`, `assessment_item_id`, `display_order`, `content`, `is_correct`, `created_at`, `updated_at`) VALUES
(1, 1, 1, 'decide', 0, '2026-03-01 08:10:00.000000', '2026-03-01 08:10:00.000000'),
(2, 1, 2, 'decides', 0, '2026-03-01 08:10:00.000000', '2026-03-01 08:10:00.000000'),
(3, 1, 3, 'decided', 1, '2026-03-01 08:10:00.000000', '2026-03-01 08:10:00.000000'),
(4, 1, 4, 'deciding', 0, '2026-03-01 08:10:00.000000', '2026-03-01 08:10:00.000000'),
(5, 2, 1, 'interview', 1, '2026-03-01 08:15:00.000000', '2026-03-01 08:15:00.000000'),
(6, 2, 2, 'interviewed', 0, '2026-03-01 08:15:00.000000', '2026-03-01 08:15:00.000000'),
(7, 2, 3, 'interviewer', 0, '2026-03-01 08:15:00.000000', '2026-03-01 08:15:00.000000'),
(8, 2, 4, 'interviewing', 0, '2026-03-01 08:15:00.000000', '2026-03-01 08:15:00.000000');

INSERT IGNORE INTO `assessment_items` (`id`, `assessment_id`, `question_id`, `grading_criteria_id`, `display_order`, `title`, `question_type`, `difficulty`, `points`, `content`, `sample_answer`, `explanation`, `grading_criteria_name`, `grading_criteria_content`, `created_at`, `updated_at`)
SELECT
    q.id + 3,
    3,
    q.id,
    NULL,
    q.display_order,
    NULL,
    q.type,
    q.difficulty,
    q.points,
    q.content,
    NULL,
    q.explanation,
    NULL,
    NULL,
    '2026-03-01 10:10:00.000000',
    '2026-03-01 10:10:00.000000'
FROM `questions` q
WHERE q.id BETWEEN 1 AND 6;

INSERT IGNORE INTO `assessment_item_options` (`id`, `assessment_item_id`, `display_order`, `content`, `is_correct`, `created_at`, `updated_at`)
SELECT
    qo.id + 8,
    qo.question_id + 3,
    qo.display_order,
    qo.content,
    qo.is_correct,
    '2026-03-01 10:10:00.000000',
    '2026-03-01 10:10:00.000000'
FROM `question_options` qo
WHERE qo.question_id BETWEEN 1 AND 6;

-- 18. ASSIGNMENTS & TARGETS & RECIPIENTS
INSERT IGNORE INTO `assignments` (`id`, `center_id`, `assessment_id`, `created_by`, `updated_by`, `title`, `description`, `status`, `attempt_limit`, `open_at`, `due_at`, `assessment_snapshot_at`, `created_at`, `updated_at`) VALUES
(1, 1, 1, 8, 8, 'Bài kiểm tra giữa kỳ TOEIC K24', 'Học viên lớp TOEIC K24 hoàn thành trước hạn 31/07.', 'ACTIVE', 2, '2026-07-20 00:00:00.000000', '2026-07-31 23:59:59.000000', '2026-07-20 08:00:00.000000', '2026-07-20 08:00:00.000000', '2026-07-20 08:00:00.000000'),
(2, 1, 2, 10, 10, 'Bài tập Viết IELTS Essay Task 2', 'Nộp bài essay về chủ đề AI trong giáo dục đại học.', 'ACTIVE', 1, '2026-07-20 00:00:00.000000', '2026-07-31 23:59:59.000000', '2026-07-20 08:30:00.000000', '2026-07-20 08:30:00.000000', '2026-07-20 08:30:00.000000'),
(3, 1, 3, 8, 8, 'TOEIC Test 1 Listening Practice', 'Nghe audio chung và trả lời sáu câu Part 1, Part 2.', 'ACTIVE', 2, '2026-07-20 00:00:00.000000', '2026-07-31 23:59:59.000000', '2026-07-20 09:00:00.000000', '2026-07-20 09:00:00.000000', '2026-07-20 09:00:00.000000');

INSERT IGNORE INTO `assignment_targets` (`id`, `assignment_id`, `target_type`, `class_id`, `student_user_id`, `created_at`, `updated_at`) VALUES
(1, 1, 'CLASS', 1, NULL, '2026-07-20 08:05:00.000000', '2026-07-20 08:05:00.000000'),
(2, 2, 'CLASS', 2, NULL, '2026-07-20 08:35:00.000000', '2026-07-20 08:35:00.000000'),
(3, 3, 'CLASS', 1, NULL, '2026-07-20 09:05:00.000000', '2026-07-20 09:05:00.000000');

INSERT IGNORE INTO `assignment_recipients` (`id`, `assignment_id`, `student_user_id`, `class_id`, `source_type`, `status`, `assigned_at`, `created_at`, `updated_at`) VALUES
(1, 1, 13, 1, 'CLASS', 'ASSIGNED', '2026-07-20 08:05:00.000000', '2026-07-20 08:05:00.000000', '2026-07-20 08:05:00.000000'),
(2, 1, 14, 1, 'CLASS', 'ASSIGNED', '2026-07-20 08:05:00.000000', '2026-07-20 08:05:00.000000', '2026-07-20 08:05:00.000000'),
(3, 1, 15, 1, 'CLASS', 'ASSIGNED', '2026-07-20 08:05:00.000000', '2026-07-20 08:05:00.000000', '2026-07-20 08:05:00.000000'),
(4, 2, 16, 2, 'CLASS', 'ASSIGNED', '2026-07-20 08:35:00.000000', '2026-07-20 08:35:00.000000', '2026-07-20 08:35:00.000000'),
(5, 2, 17, 2, 'CLASS', 'ASSIGNED', '2026-07-20 08:35:00.000000', '2026-07-20 08:35:00.000000', '2026-07-20 08:35:00.000000'),
(6, 3, 13, 1, 'CLASS', 'ASSIGNED', '2026-07-20 09:05:00.000000', '2026-07-20 09:05:00.000000', '2026-07-20 09:05:00.000000'),
(7, 3, 14, 1, 'CLASS', 'ASSIGNED', '2026-07-20 09:05:00.000000', '2026-07-20 09:05:00.000000', '2026-07-20 09:05:00.000000'),
(8, 3, 15, 1, 'CLASS', 'ASSIGNED', '2026-07-20 09:05:00.000000', '2026-07-20 09:05:00.000000', '2026-07-20 09:05:00.000000');

-- 19. ASSIGNMENT ITEMS & OPTIONS
INSERT IGNORE INTO `assignment_items` (`id`, `assignment_id`, `assessment_item_id`, `display_order`, `title`, `question_type`, `difficulty`, `points`, `content`, `sample_answer`, `explanation`, `grading_criteria_name`, `grading_criteria_content`, `created_at`, `updated_at`) VALUES
(1, 1, 1, 1, 'Budget approval', 'MULTIPLE_CHOICE', 'EASY', 1.00, 'The committee _______ to approve the new marketing budget proposed by the director yesterday.', NULL, 'The singular subject and past-time marker require "decided".', NULL, NULL, '2026-07-20 08:05:00.000000', '2026-07-20 08:05:00.000000'),
(2, 1, 2, 2, 'Job interview', 'MULTIPLE_CHOICE', 'MEDIUM', 1.00, 'All candidates are required to submit their updated resumes prior to the scheduled _______ next Monday.', NULL, '"Scheduled" modifies the noun "interview".', NULL, NULL, '2026-07-20 08:05:00.000000', '2026-07-20 08:05:00.000000'),
(3, 2, 3, 1, 'Artificial intelligence in education', 'ESSAY', 'HARD', 10.00, 'Write an essay discussing the advantages and disadvantages of using Artificial Intelligence tools in higher education.', 'In recent years, Artificial Intelligence has transformed higher education.', 'Present a clear introduction, balanced body paragraphs, and a cohesive conclusion.', 'Tiêu chí Chấm IELTS Essay Task 2', 'Task Response, CC, LR, GRA', '2026-07-20 08:35:00.000000', '2026-07-20 08:35:00.000000');

INSERT IGNORE INTO `assignment_item_options` (`id`, `assignment_item_id`, `display_order`, `content`, `is_correct`, `created_at`, `updated_at`) VALUES
(1, 1, 1, 'decide', 0, '2026-07-20 08:05:00.000000', '2026-07-20 08:05:00.000000'),
(2, 1, 2, 'decides', 0, '2026-07-20 08:05:00.000000', '2026-07-20 08:05:00.000000'),
(3, 1, 3, 'decided', 1, '2026-07-20 08:05:00.000000', '2026-07-20 08:05:00.000000'),
(4, 1, 4, 'deciding', 0, '2026-07-20 08:05:00.000000', '2026-07-20 08:05:00.000000'),
(5, 2, 1, 'interview', 1, '2026-07-20 08:05:00.000000', '2026-07-20 08:05:00.000000'),
(6, 2, 2, 'interviewed', 0, '2026-07-20 08:05:00.000000', '2026-07-20 08:05:00.000000'),
(7, 2, 3, 'interviewer', 0, '2026-07-20 08:05:00.000000', '2026-07-20 08:05:00.000000'),
(8, 2, 4, 'interviewing', 0, '2026-07-20 08:05:00.000000', '2026-07-20 08:05:00.000000');

INSERT IGNORE INTO `assignment_items` (`id`, `assignment_id`, `assessment_item_id`, `display_order`, `title`, `question_type`, `difficulty`, `points`, `content`, `sample_answer`, `explanation`, `grading_criteria_name`, `grading_criteria_content`, `created_at`, `updated_at`)
SELECT
    ai.id,
    3,
    ai.id,
    ai.display_order,
    ai.title,
    ai.question_type,
    ai.difficulty,
    ai.points,
    ai.content,
    ai.sample_answer,
    ai.explanation,
    ai.grading_criteria_name,
    ai.grading_criteria_content,
    '2026-07-20 09:05:00.000000',
    '2026-07-20 09:05:00.000000'
FROM `assessment_items` ai
WHERE ai.assessment_id = 3;

INSERT IGNORE INTO `assignment_item_options` (`id`, `assignment_item_id`, `display_order`, `content`, `is_correct`, `created_at`, `updated_at`)
SELECT
    aio.id,
    aio.assessment_item_id,
    aio.display_order,
    aio.content,
    aio.is_correct,
    '2026-07-20 09:05:00.000000',
    '2026-07-20 09:05:00.000000'
FROM `assessment_item_options` aio
WHERE aio.assessment_item_id BETWEEN 4 AND 9;

-- 20. SUBMISSION ATTEMPTS & SUBMISSION ANSWERS & OPTIONS
INSERT IGNORE INTO `submission_attempts` (`id`, `assignment_recipient_id`, `attempt_number`, `assignment_title_snapshot`, `status`, `auto_score`, `max_score`, `started_at`, `submitted_at`, `last_saved_at`, `created_at`, `updated_at`) VALUES
(1, 1, 1, 'Bài kiểm tra giữa kỳ TOEIC K24', 'SUBMITTED', 2.00, 2.00, '2026-07-21 14:00:00.000000', '2026-07-21 14:15:00.000000', '2026-07-21 14:15:00.000000', '2026-07-21 14:00:00.000000', '2026-07-21 14:15:00.000000'),
(2, 4, 1, 'Bài tập Viết IELTS Essay Task 2', 'SUBMITTED', NULL, 10.00, '2026-07-22 19:00:00.000000', '2026-07-22 20:10:00.000000', '2026-07-22 20:10:00.000000', '2026-07-22 19:00:00.000000', '2026-07-22 20:10:00.000000');

INSERT IGNORE INTO `submission_answers` (`id`, `attempt_id`, `assignment_item_id`, `answer_text`, `auto_score`, `max_score`, `graded_at`, `created_at`, `updated_at`) VALUES
(1, 1, 1, NULL, 1.00, 1.00, '2026-07-21 14:15:00.000000', '2026-07-21 14:05:00.000000', '2026-07-21 14:15:00.000000'),
(2, 1, 2, NULL, 1.00, 1.00, '2026-07-21 14:15:00.000000', '2026-07-21 14:10:00.000000', '2026-07-21 14:15:00.000000'),
(3, 2, 3, 'Artificial Intelligence (AI) has emerged as one of the most transformative technologies in modern higher education. On the one hand, AI-powered tools such as adaptive learning platforms and intelligent tutoring systems provide students with personalized learning experiences tailored to their individual pace. Furthermore, administrative duties like grading multiple-choice exams can be automated, allowing professors to focus more on mentoring and research. On the other hand, the overreliance on AI tools raises severe concerns regarding academic integrity, such as plagiarism and unauthorized generation of essays. Moreover, excessive reliance on virtual tutors may diminish critical face-to-face interpersonal interactions between students and instructors. In conclusion, while AI brings unprecedented efficiency and customized support to higher education, educational institutions must establish ethical frameworks to prevent academic misconduct.', NULL, 10.00, NULL, '2026-07-22 20:10:00.000000', '2026-07-22 20:10:00.000000');

INSERT IGNORE INTO `submission_answer_options` (`id`, `submission_answer_id`, `assignment_item_option_id`, `created_at`) VALUES
(1, 1, 3, '2026-07-21 14:05:00.000000'),
(2, 2, 5, '2026-07-21 14:10:00.000000');

-- 21. AI GRADING JOBS & RESULTS & ITEM RESULTS
INSERT IGNORE INTO `ai_grading_jobs` (`id`, `submission_attempt_id`, `requested_by`, `model_provider`, `model_name`, `prompt_builder_version`, `prompt_template_version`, `system_prompt`, `user_prompt`, `temperature`, `max_tokens`, `status`, `started_at`, `completed_at`, `failed_at`, `error_message`, `created_at`, `updated_at`) VALUES
(1, 2, 10, 'OPENAI', 'gpt-5.6-sol', 'v1.2.0', 'v2.1.0', 'You are an expert IELTS Writing examiner. Grade the essay according to Task Response, Coherence & Cohesion, Lexical Resource, and Grammatical Range & Accuracy.', 'Grade essay for Attempt #2...', 0.20, 2000, 'COMPLETED', '2026-07-23 08:00:00.000000', '2026-07-23 08:00:15.000000', NULL, NULL, '2026-07-23 08:00:00.000000', '2026-07-23 08:00:15.000000');

INSERT IGNORE INTO `ai_grading_results` (`id`, `job_id`, `submission_attempt_id`, `ai_score`, `max_score`, `confidence`, `summary`, `overall_feedback`, `raw_response`, `created_at`, `updated_at`) VALUES
(1, 1, 2, 8.50, 10.00, 0.9450, 'Excellent response with balanced arguments and sophisticated vocabulary.', 'The student demonstrates strong academic writing skills. The essay addresses both pros and cons effectively, using cohesive devices naturally and maintaining high grammatical accuracy.', '{"score":8.5,"band":7.5,"feedback":"Strong academic tone..."}', '2026-07-23 08:00:15.000000', '2026-07-23 08:00:15.000000');

INSERT IGNORE INTO `ai_grading_item_results` (`id`, `result_id`, `submission_answer_id`, `assignment_item_id`, `ai_score`, `max_score`, `confidence`, `feedback`, `rubric_analysis`, `created_at`, `updated_at`) VALUES
(1, 1, 3, 3, 8.50, 10.00, 0.9450, 'Task Response: 8.5/10. Clear main ideas and logical progression. Coherence & Cohesion: 8.5/10. Effective use of transition words (On the one hand, Furthermore, In conclusion). Lexical Resource: 8.5/10. Used strong collocations like "transformative technologies", "academic misconduct".', 'Task Response: 8.5, CC: 8.5, LR: 8.5, GRA: 8.5', '2026-07-23 08:00:15.000000', '2026-07-23 08:00:15.000000');

-- 22. TEACHER REVIEWS & TEACHER REVIEW ITEMS
INSERT IGNORE INTO `teacher_reviews` (`id`, `submission_attempt_id`, `selected_ai_grading_result_id`, `created_by`, `updated_by`, `finalized_by`, `released_by`, `final_score`, `max_score`, `version`, `status`, `overall_comment`, `created_at`, `updated_at`, `finalized_at`, `released_at`) VALUES
(1, 2, 1, 10, 10, 10, 10, 8.50, 10.00, 1, 'RELEASED', 'Bài viết rất xuất sắc, lập luận chặt chẽ và từ vựng thuật ngữ học thuật phong phú. Thầy đồng ý với đánh giá 8.5/10 của AI.', '2026-07-24 09:00:00.000000', '2026-07-24 09:10:00.000000', '2026-07-24 09:05:00.000000', '2026-07-24 09:10:00.000000');

INSERT IGNORE INTO `teacher_review_items` (`id`, `review_id`, `assignment_item_id`, `submission_answer_id`, `display_order_snapshot`, `question_title_snapshot`, `final_score`, `max_score`, `item_comment`, `created_at`, `updated_at`) VALUES
(1, 1, 3, 3, 1, 'Artificial intelligence in education', 8.50, 10.00, 'Lập luận sắc bén. Cần chú ý ngắt đoạn cân đối hơn một chút.', '2026-07-24 09:00:00.000000', '2026-07-24 09:10:00.000000');

-- ================================================================
-- END OF SEED DATA v7.0
-- ================================================================
