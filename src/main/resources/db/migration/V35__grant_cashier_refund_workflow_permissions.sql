-- Cashiers need the full refund workflow for direct refund handling from payment history.
INSERT IGNORE INTO role_permission (role, permission_id)
SELECT 'CASHIER', id FROM permissions
WHERE code IN ('REFUND_REQUEST', 'REFUND_APPROVE', 'REFUND_PAY');
