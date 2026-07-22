-- Add type column, defaulting to THEORY_CLASS for existing schedules that are active
ALTER TABLE schedules ADD COLUMN type VARCHAR(50) DEFAULT 'THEORY_CLASS' NOT NULL;

-- If a schedule was inactive, make it CANCELLED
UPDATE schedules SET type = 'CANCELLED' WHERE is_active = false;

-- Drop the old is_active column
ALTER TABLE schedules DROP COLUMN is_active;
