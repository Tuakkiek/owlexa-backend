-- Migration V15: Migrate legacy ClassStatus values to the new business statuses (PLANNED, ACTIVE, FINISHED)
-- Legacy statuses inspected and identified in the classes table: 'IN_PROGRESS', 'ARCHIVED'.

-- 1. Temporarily add the new enum values ('PLANNED', 'ACTIVE') to the allowed ENUM list,
--    while preserving existing legacy enum values, so MySQL does not raise validation errors during UPDATE.
ALTER TABLE classes MODIFY COLUMN status ENUM('ARCHIVED','CANCELLED','FINISHED','FULL','IN_PROGRESS','OPEN','PLANNING','PLANNED','ACTIVE') NOT NULL;

-- 2. Migrate existing status values
UPDATE classes SET status = 'ACTIVE' WHERE status = 'IN_PROGRESS';
UPDATE classes SET status = 'FINISHED' WHERE status = 'ARCHIVED';

-- 3. Finalize the column definition to match the new JPA mapping: ENUM('PLANNED', 'ACTIVE', 'FINISHED')
ALTER TABLE classes MODIFY COLUMN status ENUM('PLANNED', 'ACTIVE', 'FINISHED') NOT NULL DEFAULT 'PLANNED';
