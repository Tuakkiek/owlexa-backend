INSERT IGNORE INTO `sepay_webhook_events` (
    `matched_payment_id`,
    `processed_at`,
    `received_at`,
    `sepay_transaction_id`,
    `transfer_amount`,
    `content`,
    `processing_note`,
    `account_number`,
    `gateway`,
    `payment_code`,
    `raw_payload`,
    `reference_code`,
    `sub_account`,
    `transaction_date`,
    `transfer_type`,
    `processing_status`
)
SELECT
    p.id,
    '2026-08-16 10:02:30.000000',
    '2026-08-16 10:02:25.000000',
    99014001,
    CAST(p.amount AS UNSIGNED),
    CONCAT('DUPLICATE_PAYMENT ', COALESCE(p.sepay_ref, p.receipt_number), ' 0901000140'),
    CONCAT('DUPLICATE_PAYMENT: seeded duplicate transfer for student 0901000140 against payment ', p.id),
    '70740011223344',
    'MB',
    COALESCE(p.sepay_ref, p.receipt_number),
    CONCAT(
        '{"id":99014001,"gateway":"MB","studentPhone":"0901000140","duplicateOfPaymentId":',
        p.id,
        '}'
    ),
    'FT-DUP-0901000140',
    'OWLEXA01',
    '2026-08-16 10:02:20',
    'in',
    'DUPLICATE_PAYMENT'
FROM `payments` p
JOIN `users` u ON u.id = p.student_user_id
WHERE u.phone_number = '0901000140'
  AND p.status = 'ACTIVE'
ORDER BY
    CASE p.method
        WHEN 'SEPAY' THEN 1
        WHEN 'QR_CODE' THEN 2
        WHEN 'BANK_TRANSFER' THEN 3
        ELSE 4
    END,
    p.created_at DESC,
    p.id DESC
LIMIT 1;
