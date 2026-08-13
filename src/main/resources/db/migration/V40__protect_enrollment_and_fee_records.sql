-- The cleanup of existing duplicate groups must be reviewed and completed
-- before this migration is applied in an environment that already has data.

ALTER TABLE fee_records
    MODIFY COLUMN status enum('CANCELLED','OVERDUE','PAID','PARTIAL','UNPAID')
    COLLATE utf8mb4_unicode_ci NOT NULL;

ALTER TABLE class_enrollments
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE fee_records
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE class_enrollments
    ADD CONSTRAINT uq_class_enrollments_class_student
        UNIQUE (class_id, student_user_id);

ALTER TABLE fee_records
    ADD CONSTRAINT uq_fee_records_student_class_month
        UNIQUE (student_user_id, class_id, month);
