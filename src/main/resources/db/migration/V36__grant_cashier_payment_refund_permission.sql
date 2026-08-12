-- Keep compatibility with the existing PAYMENT_REFUND umbrella permission.
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'CASHIER', id FROM permissions
WHERE code = 'PAYMENT_REFUND';
