-- Cleanup for the accidental weekly schedule demo migration.
-- Run this once on the local database if V15__add_weekly_schedule_ranges_and_demo_data.sql
-- was already applied before it was removed from the codebase.

START TRANSACTION;

-- Remove dependent rows first.
DELETE FROM attendances
WHERE schedule_id BETWEEN 101 AND 113;

DELETE FROM class_enrollments
WHERE id BETWEEN 101 AND 104
   OR class_id BETWEEN 101 AND 104;

DELETE FROM schedules
WHERE id BETWEEN 101 AND 113
   OR class_id BETWEEN 101 AND 104
   OR type = 'PRACTICE_CLASS';

DELETE FROM classes
WHERE id BETWEEN 101 AND 104;

DELETE FROM rooms
WHERE id BETWEEN 101 AND 106;

DELETE FROM courses
WHERE id BETWEEN 101 AND 102;

-- If Flyway recorded the removed migration, delete its history row so validation
-- does not fail on the next backend restart.
DELETE FROM flyway_schema_history
WHERE version = '15'
  AND script = 'V15__add_weekly_schedule_ranges_and_demo_data.sql';

COMMIT;

-- Revert the accidental schema additions. Run these after the cleanup deletes
-- every PRACTICE_CLASS row.
ALTER TABLE schedules
    MODIFY COLUMN type enum('CANCELLED','EXAM','ONLINE_CLASS','THEORY_CLASS') COLLATE utf8mb4_unicode_ci NOT NULL;

ALTER TABLE schedules
    DROP COLUMN starts_on,
    DROP COLUMN ends_on;
