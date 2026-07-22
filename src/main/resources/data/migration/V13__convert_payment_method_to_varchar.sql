-- Convert payments.method from ENUM to VARCHAR to support new PaymentMethod values
-- without requiring DDL changes when the Java enum is extended.
ALTER TABLE payments MODIFY COLUMN method VARCHAR(20) NOT NULL;
