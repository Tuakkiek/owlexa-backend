-- Owlexa demo seed reset data
-- Run this script in MySQL Workbench or mysql CLI after selecting owlexa_db.

START TRANSACTION;

SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM user_permission;
DELETE FROM payments;
DELETE FROM fee_records;
DELETE FROM attendances;
DELETE FROM schedules;
DELETE FROM class_enrollments;
DELETE FROM classes;
DELETE FROM membership;
DELETE FROM centers;
DELETE FROM users;
DELETE FROM permissions;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1) Permissions
-- ============================================================
INSERT INTO permissions (id, code, description)
VALUES
  (1, 'VIEW_STUDENT', 'Can view student data'),
  (2, 'EDIT_FEE', 'Can edit fee records'),
  (3, 'VIEW_SALARY', 'Can view salary data'),
  (4, 'CENTER_CREATE', 'Can create center'),
  (5, 'MANAGE_CLASS', 'Can manage classes'),
  (6, 'MANAGE_TEACHER', 'Can manage teachers')
ON DUPLICATE KEY UPDATE
  code = VALUES(code),
  description = VALUES(description);

-- ============================================================
-- 2) Users
-- ============================================================
INSERT INTO users (id, phone_number, email, full_name, password, role)
VALUES
  (1,  '0900000001', 'owner1@example.com',   'Owner One',   '123456', 'OWNER'),
  (2,  '0900000002', 'teacher1@example.com', 'Teacher One', '123456', 'TEACHER'),
  (3,  '0900000003', 'teacher2@example.com', 'Teacher Two', '123456', 'TEACHER'),
  (4,  '0900000004', 'cashier1@example.com', 'Cashier One', '123456', 'CASHIER'),
  (5,  '0900000005', 'admin1@example.com',   'Admin One',   '123456', 'ADMIN'),
  (10, '0900000010', 'student10@example.com','Student Ten', '123456', 'STUDENT'),
  (11, '0900000011', 'student11@example.com','Student Eleven', '123456', 'STUDENT'),
  (12, '0900000012', 'student12@example.com','Student Twelve', '123456', 'STUDENT'),
  (13, '0900000013', 'student13@example.com','Student Thirteen', '123456', 'STUDENT'),
  (14, '0900000014', 'student14@example.com','Student Fourteen', '123456', 'STUDENT')
ON DUPLICATE KEY UPDATE
  phone_number = VALUES(phone_number),
  email = VALUES(email),
  full_name = VALUES(full_name),
  password = VALUES(password),
  role = VALUES(role);

-- ============================================================
-- 3) Centers
-- ============================================================
INSERT INTO centers (id, owner_user_id, name, subdomain, created_at)
VALUES
  (100, 1, 'Owlexa Demo Center', 'center-a', NOW())
ON DUPLICATE KEY UPDATE
  owner_user_id = VALUES(owner_user_id),
  name = VALUES(name),
  subdomain = VALUES(subdomain),
  created_at = VALUES(created_at);

-- ============================================================
-- 4) Memberships
-- ============================================================
INSERT INTO membership (id, center_id, user_id, joined_by_user_id, joined_at)
VALUES
  (9000, 100, 1,  NULL, NOW()),
  (9001, 100, 2,  1,    NOW()),
  (9002, 100, 3,  1,    NOW()),
  (9003, 100, 4,  1,    NOW()),
  (9004, 100, 10, 1,    NOW()),
  (9005, 100, 11, 1,    NOW()),
  (9006, 100, 12, 1,    NOW()),
  (9007, 100, 13, 1,    NOW()),
  (9008, 100, 14, 1,    NOW())
ON DUPLICATE KEY UPDATE
  center_id = VALUES(center_id),
  user_id = VALUES(user_id),
  joined_by_user_id = VALUES(joined_by_user_id),
  joined_at = VALUES(joined_at);

-- ============================================================
-- 5) Classes
-- ============================================================
INSERT INTO classes (id, name, vstep_level, max_students, monthly_fee, is_active, center_id)
VALUES
  (200, 'B1 Intensive', 'B1', 30, 1500000, TRUE, 100),
  (201, 'B2 Speaking',   'B2', 24, 1800000, TRUE, 100),
  (202, 'C1 Writing',    'C1', 20, 2000000, TRUE, 100)
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  vstep_level = VALUES(vstep_level),
  max_students = VALUES(max_students),
  monthly_fee = VALUES(monthly_fee),
  is_active = VALUES(is_active),
  center_id = VALUES(center_id);

-- ============================================================
-- 6) Class enrollments
-- ============================================================
INSERT INTO class_enrollments (id, class_id, student_user_id, center_id, enrolled_by_user_id, status, enrolled_at)
VALUES
  (3000, 200, 10, 100, 1, 'ACTIVE', NOW()),
  (3001, 200, 11, 100, 1, 'ACTIVE', NOW()),
  (3002, 201, 12, 100, 1, 'ACTIVE', NOW()),
  (3003, 201, 13, 100, 1, 'ACTIVE', NOW()),
  (3004, 202, 14, 100, 1, 'ACTIVE', NOW()),
  (3005, 202, 10, 100, 1, 'ACTIVE', NOW())
ON DUPLICATE KEY UPDATE
  center_id = VALUES(center_id),
  enrolled_by_user_id = VALUES(enrolled_by_user_id),
  status = VALUES(status),
  enrolled_at = VALUES(enrolled_at);

-- ============================================================
-- 7) Schedules
-- ============================================================
INSERT INTO schedules (id, class_id, center_id, teacher_user_id, day_of_week, start_time, end_time, room, is_active, created_at)
VALUES
  (4000, 200, 100, 2, 1, '18:00:00', '19:30:00', 'Room 101', TRUE, NOW()),
  (4001, 200, 100, 2, 3, '18:00:00', '19:30:00', 'Room 101', TRUE, NOW()),
  (4002, 201, 100, 3, 2, '19:00:00', '20:30:00', 'Room 102', TRUE, NOW()),
  (4003, 201, 100, 3, 4, '19:00:00', '20:30:00', 'Room 102', TRUE, NOW()),
  (4004, 202, 100, 2, 5, '17:30:00', '19:00:00', 'Room 103', TRUE, NOW())
ON DUPLICATE KEY UPDATE
  teacher_user_id = VALUES(teacher_user_id),
  day_of_week = VALUES(day_of_week),
  start_time = VALUES(start_time),
  end_time = VALUES(end_time),
  room = VALUES(room),
  is_active = VALUES(is_active),
  created_at = VALUES(created_at);

-- ============================================================
-- 8) Attendances
-- ============================================================
INSERT INTO attendances (id, schedule_id, student_user_id, center_id, date, status, marked_by_user_id, note, created_at)
VALUES
  (5000, 4000, 10, 100, CURDATE(), 'PRESENT', 2, 'Demo attendance - present', NOW()),
  (5001, 4000, 11, 100, CURDATE(), 'ABSENT', 2, 'Demo attendance - absent', NOW()),
  (5002, 4002, 12, 100, CURDATE(), 'EXCUSED', 3, 'Demo attendance - excused', NOW()),
  (5003, 4003, 13, 100, DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'PRESENT', 3, 'Yesterday demo attendance', NOW()),
  (5004, 4004, 14, 100, CURDATE(), 'PRESENT', 2, 'Demo attendance - present', NOW())
ON DUPLICATE KEY UPDATE
  center_id = VALUES(center_id),
  status = VALUES(status),
  noted_by_user_id = VALUES(noted_by_user_id),
  note = VALUES(note),
  created_at = VALUES(created_at);

-- ============================================================
-- 9) Fee records
-- ============================================================
SET @current_month := DATE_FORMAT(CURDATE(), '%Y-%m');
SET @previous_month := DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m');

INSERT INTO fee_records (id, student_user_id, center_id, class_id, amount, paid_amount, month, due_date, status, created_at)
VALUES
  (6000, 10, 100, 200, 1500000,       0, @current_month, DATE_FORMAT(CURDATE(), '%Y-%m-05'), 'UNPAID',  NOW()),
  (6001, 11, 100, 200, 1500000, 1000000, @current_month, DATE_FORMAT(CURDATE(), '%Y-%m-05'), 'PARTIAL', NOW()),
  (6002, 12, 100, 201, 1800000, 1800000, @current_month, DATE_FORMAT(CURDATE(), '%Y-%m-05'), 'PAID',    NOW()),
  (6003, 13, 100, 201, 1800000,       0, @current_month, DATE_FORMAT(CURDATE(), '%Y-%m-05'), 'UNPAID',  NOW()),
  (6004, 14, 100, 202, 2000000,  500000, @current_month, DATE_FORMAT(CURDATE(), '%Y-%m-05'), 'PARTIAL', NOW()),
  (6005, 10, 100, 202, 2000000, 2000000, @previous_month, DATE_FORMAT(DATE_SUB(CURDATE(), INTERVAL 1 MONTH), '%Y-%m-05'), 'PAID', NOW())
ON DUPLICATE KEY UPDATE
  amount = VALUES(amount),
  paid_amount = VALUES(paid_amount),
  due_date = VALUES(due_date),
  status = VALUES(status),
  created_at = VALUES(created_at);

-- ============================================================
-- 10) Payments
-- ============================================================
INSERT INTO payments (id, fee_record_id, center_id, student_user_id, collected_by_user_id, amount, method, sepay_ref, note, created_at)
VALUES
  (7000, 6001, 100, 11, 4, 500000,  'CASH',  NULL,         'Partial payment - installment 1', NOW()),
  (7001, 6001, 100, 11, 4, 500000,  'SEPAY', 'SEPAY-7001', 'Partial payment - installment 2', NOW()),
  (7002, 6002, 100, 12, 1, 1800000, 'SEPAY', 'SEPAY-7002', 'Full payment by owner', NOW()),
  (7003, 6004, 100, 14, 4, 500000,  'CASH',  NULL,         'Partial payment by cashier', NOW()),
  (7004, 6005, 100, 10, 1, 2000000, 'CASH',  NULL,         'Historical full payment', NOW())
ON DUPLICATE KEY UPDATE
  fee_record_id = VALUES(fee_record_id),
  center_id = VALUES(center_id),
  student_user_id = VALUES(student_user_id),
  collected_by_user_id = VALUES(collected_by_user_id),
  amount = VALUES(amount),
  method = VALUES(method),
  sepay_ref = VALUES(sepay_ref),
  note = VALUES(note),
  created_at = VALUES(created_at);

-- ============================================================
-- 11) User permissions
-- ============================================================
INSERT INTO user_permission (id, user_id, permission_id, granted_at)
VALUES
  (8000, 1, 4, NOW()),
  (8001, 1, 1, NOW()),
  (8002, 1, 2, NOW()),
  (8003, 2, 1, NOW()),
  (8004, 3, 1, NOW()),
  (8005, 4, 2, NOW()),
  (8006, 5, 3, NOW())
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id),
  permission_id = VALUES(permission_id),
  granted_at = VALUES(granted_at);

COMMIT;

-- Quick checks after running:
-- SELECT * FROM users;
-- SELECT * FROM centers;
-- SELECT * FROM membership;
-- SELECT * FROM classes;
-- SELECT * FROM class_enrollments;
-- SELECT * FROM schedules;
-- SELECT * FROM attendances;
-- SELECT * FROM fee_records;
-- SELECT * FROM payments;
