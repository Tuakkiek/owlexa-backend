-- Simplify TEACHER and CASHIER RBAC so each role permission maps to a visible page.

INSERT IGNORE INTO permissions (code, description) VALUES
('TEACHER_DASHBOARD', 'Access teacher dashboard page'),
('TEACHER_SCHEDULE', 'Access teacher schedule page'),
('TEACHER_ATTENDANCE', 'Access teacher attendance page'),
('TEACHER_GRADING_CRITERIA', 'Access teacher grading criteria page'),
('TEACHER_QUESTION_BANK', 'Access teacher question bank page'),
('TEACHER_ASSESSMENTS', 'Access teacher assessment builder page'),
('TEACHER_DOCUMENTS', 'Access teacher documents page'),
('TEACHER_ASSIGNMENTS', 'Access teacher assignments page'),
('CASHIER_DASHBOARD', 'Access cashier dashboard page'),
('CASHIER_PAYMENTS', 'Access cashier payment collection page'),
('CASHIER_PAYMENT_HISTORY', 'Access cashier payment history page');

-- Preserve the most obvious disabled overrides during the migration.
INSERT IGNORE INTO user_permission (user_id, permission_id, granted_at)
SELECT up.user_id, p_new.id, NOW(6)
FROM user_permission up
JOIN users u ON u.id = up.user_id AND u.role = 'TEACHER'
JOIN permissions p_old ON p_old.id = up.permission_id
JOIN permissions p_new ON p_new.code = 'TEACHER_SCHEDULE'
WHERE p_old.code = 'SCHEDULE_VIEW';

INSERT IGNORE INTO user_permission (user_id, permission_id, granted_at)
SELECT up.user_id, p_new.id, NOW(6)
FROM user_permission up
JOIN users u ON u.id = up.user_id AND u.role = 'TEACHER'
JOIN permissions p_old ON p_old.id = up.permission_id
JOIN permissions p_new ON p_new.code = 'TEACHER_ATTENDANCE'
WHERE p_old.code = 'ATTENDANCE_MARK';

INSERT IGNORE INTO user_permission (user_id, permission_id, granted_at)
SELECT up.user_id, p_new.id, NOW(6)
FROM user_permission up
JOIN users u ON u.id = up.user_id AND u.role = 'TEACHER'
JOIN permissions p_old ON p_old.id = up.permission_id
JOIN permissions p_new ON p_new.code IN ('TEACHER_GRADING_CRITERIA', 'TEACHER_ASSIGNMENTS')
WHERE p_old.code = 'ESSAY_GRADE';

INSERT IGNORE INTO user_permission (user_id, permission_id, granted_at)
SELECT up.user_id, p_new.id, NOW(6)
FROM user_permission up
JOIN users u ON u.id = up.user_id AND u.role = 'TEACHER'
JOIN permissions p_old ON p_old.id = up.permission_id
JOIN permissions p_new ON p_new.code IN ('TEACHER_QUESTION_BANK', 'TEACHER_ASSESSMENTS')
WHERE p_old.code = 'TEST_VIEW';

INSERT IGNORE INTO user_permission (user_id, permission_id, granted_at)
SELECT up.user_id, p_new.id, NOW(6)
FROM user_permission up
JOIN users u ON u.id = up.user_id AND u.role = 'TEACHER'
JOIN permissions p_old ON p_old.id = up.permission_id
JOIN permissions p_new ON p_new.code = 'TEACHER_DOCUMENTS'
WHERE p_old.code = 'DOCUMENT_VIEW';

INSERT IGNORE INTO user_permission (user_id, permission_id, granted_at)
SELECT up.user_id, p_new.id, NOW(6)
FROM user_permission up
JOIN users u ON u.id = up.user_id AND u.role = 'CASHIER'
JOIN permissions p_old ON p_old.id = up.permission_id
JOIN permissions p_new ON p_new.code = 'CASHIER_DASHBOARD'
WHERE p_old.code = 'DASHBOARD_FINANCE';

INSERT IGNORE INTO user_permission (user_id, permission_id, granted_at)
SELECT up.user_id, p_new.id, NOW(6)
FROM user_permission up
JOIN users u ON u.id = up.user_id AND u.role = 'CASHIER'
JOIN permissions p_old ON p_old.id = up.permission_id
JOIN permissions p_new ON p_new.code = 'CASHIER_PAYMENTS'
WHERE p_old.code = 'PAYMENT_COLLECT';

INSERT IGNORE INTO user_permission (user_id, permission_id, granted_at)
SELECT up.user_id, p_new.id, NOW(6)
FROM user_permission up
JOIN users u ON u.id = up.user_id AND u.role = 'CASHIER'
JOIN permissions p_old ON p_old.id = up.permission_id
JOIN permissions p_new ON p_new.code = 'CASHIER_PAYMENT_HISTORY'
WHERE p_old.code IN ('PAYMENT_VIEW', 'PAYMENT_REFUND');

-- Replace role defaults for TEACHER/CASHIER with page-level permissions only.
DELETE rp
FROM role_permission rp
WHERE rp.role IN ('TEACHER', 'CASHIER');

INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'TEACHER', id
FROM permissions
WHERE code IN (
    'TEACHER_DASHBOARD',
    'TEACHER_SCHEDULE',
    'TEACHER_ATTENDANCE',
    'TEACHER_GRADING_CRITERIA',
    'TEACHER_QUESTION_BANK',
    'TEACHER_ASSESSMENTS',
    'TEACHER_DOCUMENTS',
    'TEACHER_ASSIGNMENTS'
);

INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'CASHIER', id
FROM permissions
WHERE code IN (
    'CASHIER_DASHBOARD',
    'CASHIER_PAYMENTS',
    'CASHIER_PAYMENT_HISTORY'
);

-- Existing access tokens may still carry old authorities. Force these users to sign in again.
UPDATE user_sessions us
JOIN users u ON u.id = us.user_id
SET us.is_active = b'0',
    us.revoked_at = NOW(6),
    us.revoked_reason = 'PERMISSION_SCHEMA_CHANGED'
WHERE u.role IN ('TEACHER', 'CASHIER')
  AND us.is_active = b'1';
