UPDATE class_enrollments
SET status = 'DROPPED',
    dropped_at = COALESCE(dropped_at, CURRENT_TIMESTAMP(6))
WHERE status = 'TRANSFERRED';

ALTER TABLE class_enrollments
    DROP FOREIGN KEY fk_class_enrollments_transferred_to;

ALTER TABLE class_enrollments
    DROP FOREIGN KEY fk_class_enrollments_transferred_from;

ALTER TABLE class_enrollments
    DROP COLUMN transferred_to_enrollment_id,
    DROP COLUMN transferred_from_enrollment_id;

ALTER TABLE class_enrollments
    MODIFY COLUMN status ENUM('ACTIVE','DROPPED','PENDING','SUSPENDED')
    COLLATE utf8mb4_unicode_ci NOT NULL;

ALTER TABLE assignment_recipients
    MODIFY COLUMN status ENUM('ASSIGNED','REVOKED')
    COLLATE utf8mb4_unicode_ci NOT NULL;

DELETE FROM role_permission
WHERE permission_id IN (
    SELECT id FROM permissions WHERE code = 'ENROLLMENT_TRANSFER'
);

DELETE FROM user_permission
WHERE permission_id IN (
    SELECT id FROM permissions WHERE code = 'ENROLLMENT_TRANSFER'
);

DELETE FROM permissions
WHERE code = 'ENROLLMENT_TRANSFER';
