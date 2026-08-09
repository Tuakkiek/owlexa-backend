-- Seed DOCUMENT_DELETE permission for TEACHER role if not already assigned
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'TEACHER', id FROM permissions
WHERE code = 'DOCUMENT_DELETE';
