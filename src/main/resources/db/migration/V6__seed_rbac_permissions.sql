-- Seed the stable permission catalog after all application tables exist.
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

-- Seed OWNER permissions
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'OWNER', id FROM permissions;

-- Seed MANAGER permissions (Everything EXCEPT CENTER_SETTINGS_UPDATE, SALARY_APPROVE, PAYMENT_VOID)
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'MANAGER', id FROM permissions 
WHERE code NOT IN ('CENTER_SETTINGS_UPDATE', 'SALARY_APPROVE', 'PAYMENT_VOID');

-- Seed ACADEMIC_STAFF permissions
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

-- Seed CASHIER permissions
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'CASHIER', id FROM permissions
WHERE code IN (
    'STUDENT_VIEW',
    'FEE_VIEW', 'FEE_GENERATE', 'FEE_ADJUST',
    'PAYMENT_VIEW', 'PAYMENT_COLLECT', 'PAYMENT_REFUND',
    'REPORT_FINANCE_VIEW', 'DASHBOARD_FINANCE'
);

-- Seed TEACHER permissions
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'TEACHER', id FROM permissions
WHERE code IN (
    'CLASS_VIEW', 'SCHEDULE_VIEW', 'ATTENDANCE_MARK',
    'ESSAY_VIEW', 'ESSAY_GRADE',
    'TEST_VIEW', 'TEST_GRADE',
    'DOCUMENT_VIEW', 'DOCUMENT_UPLOAD'
);

-- Seed STUDENT permissions
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'STUDENT', id FROM permissions
WHERE code IN (
    'STUDENT_VIEW', 'SCHEDULE_VIEW',
    'ESSAY_VIEW', 'ESSAY_SUBMIT',
    'PAYMENT_VIEW'
);
