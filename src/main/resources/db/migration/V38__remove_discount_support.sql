-- Discount support is intentionally removed from the current billing model.
-- Preserve the already-discounted payable amount before dropping the field.
-- Keep this as a forward migration so existing databases converge without
-- modifying any already-applied Flyway migration.
UPDATE fee_records
SET amount = amount - COALESCE(discount_amount, 0)
WHERE COALESCE(discount_amount, 0) > 0;

DROP TABLE IF EXISTS discounts;

ALTER TABLE fee_records
    DROP COLUMN discount_amount;
