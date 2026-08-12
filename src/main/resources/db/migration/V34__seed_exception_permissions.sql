-- Seed new permissions for enrollment exceptions & refund workflow
INSERT INTO permissions (code, description) VALUES
  ('ENROLLMENT_DROP', 'Cho học viên nghỉ ngang'),
  ('ENROLLMENT_TRANSFER', 'Chuyển học viên sang lớp khác'),
  ('REFUND_REQUEST', 'Tạo yêu cầu hoàn tiền'),
  ('REFUND_APPROVE', 'Duyệt/từ chối yêu cầu hoàn tiền'),
  ('REFUND_PAY', 'Xác nhận đã chi tiền hoàn')
ON DUPLICATE KEY UPDATE description = VALUES(description);

-- OWNER gets all new permissions
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'OWNER', id FROM permissions
WHERE code IN ('ENROLLMENT_DROP','ENROLLMENT_TRANSFER','REFUND_REQUEST','REFUND_APPROVE','REFUND_PAY');

-- MANAGER gets all new permissions (like OWNER minus sensitive ops)
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'MANAGER', id FROM permissions
WHERE code IN ('ENROLLMENT_DROP','ENROLLMENT_TRANSFER','REFUND_REQUEST','REFUND_APPROVE','REFUND_PAY');

-- ACADEMIC_STAFF gets drop, transfer, and refund request
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'ACADEMIC_STAFF', id FROM permissions
WHERE code IN ('ENROLLMENT_DROP','ENROLLMENT_TRANSFER','REFUND_REQUEST');

-- CASHIER gets refund payout confirmation
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'CASHIER', id FROM permissions
WHERE code IN ('REFUND_PAY');
