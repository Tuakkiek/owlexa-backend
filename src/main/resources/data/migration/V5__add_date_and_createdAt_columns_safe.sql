-- ============================================================
-- Safe migration: rename session_date -> date (attendances)
-- and create_at -> created_at (classes, schedules)
-- WITHOUT losing existing data.
-- ============================================================
-- How to run manually:
--   mysql -u root -p owlexa_db < V5__add_date_and_createdAt_columns_safe.sql
--
-- How to run inside MySQL CLI:
--   SOURCE V5__add_date_and_createdAt_columns_safe.sql;
-- ============================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_date_and_created_at_columns$$

CREATE PROCEDURE migrate_date_and_created_at_columns()
BEGIN
    DECLARE has_session_date INT DEFAULT 0;
    DECLARE has_date INT DEFAULT 0;
    DECLARE has_create_at_classes INT DEFAULT 0;
    DECLARE has_created_at_classes INT DEFAULT 0;
    DECLARE has_create_at_schedules INT DEFAULT 0;
    DECLARE has_created_at_schedules INT DEFAULT 0;

    -- ── Detect columns in attendances ────────────────────────────
    SELECT COUNT(*) INTO has_session_date
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name   = 'attendances'
      AND column_name  = 'session_date';

    SELECT COUNT(*) INTO has_date
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name   = 'attendances'
      AND column_name  = 'date';

    -- ── Detect columns in classes ─────────────────────────────────
    SELECT COUNT(*) INTO has_create_at_classes
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name   = 'classes'
      AND column_name  = 'create_at';

    SELECT COUNT(*) INTO has_created_at_classes
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name   = 'classes'
      AND column_name  = 'created_at';

    -- ── Detect columns in schedules ───────────────────────────────
    SELECT COUNT(*) INTO has_create_at_schedules
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name   = 'schedules'
      AND column_name  = 'create_at';

    SELECT COUNT(*) INTO has_created_at_schedules
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name   = 'schedules'
      AND column_name  = 'created_at';

    -- ════════════════════════════════════════════════════════════
    --  MIGRATE: attendances  (session_date  →  date)
    -- ════════════════════════════════════════════════════════════
    IF has_session_date = 1 AND has_date = 0 THEN

        -- Step 1: Add new column as NULL (no NOT NULL conflict)
        ALTER TABLE attendances
            ADD COLUMN date DATE NULL AFTER center_id;

        -- Step 2: Copy valid dates using YEAR() check.
        --   YEAR(session_date) > 0 safely filters out '0000-00-00'
        --   without triggering strict-mode truncation on comparison.
        UPDATE attendances
        SET date = session_date
        WHERE date IS NULL
          AND YEAR(session_date) > 0;

        -- Step 3: Fallback for rows that had '0000-00-00' — use today.
        --   (This is safe: no existing real attendance data is overwritten
        --    because those rows had an invalid date anyway.)
        UPDATE attendances
        SET date = CURDATE()
        WHERE date IS NULL;

        -- Step 4: Enforce NOT NULL (all rows now have a real date)
        ALTER TABLE attendances
            MODIFY COLUMN date DATE NOT NULL;

        -- Step 5: Drop old column
        ALTER TABLE attendances
            DROP COLUMN session_date;

    END IF;

    -- ════════════════════════════════════════════════════════════
    --  MIGRATE: classes  (create_at  →  created_at)
    -- ════════════════════════════════════════════════════════════
    IF has_create_at_classes = 1 AND has_created_at_classes = 0 THEN

        ALTER TABLE classes
            ADD COLUMN created_at DATETIME(6) NULL AFTER center_id;

        -- Use YEAR() check for DATETIME columns too
        UPDATE classes
        SET created_at = create_at
        WHERE created_at IS NULL
          AND YEAR(create_at) > 0;

        UPDATE classes
        SET created_at = CURRENT_TIMESTAMP(6)
        WHERE created_at IS NULL;

        ALTER TABLE classes
            MODIFY COLUMN created_at DATETIME(6) NOT NULL;

        ALTER TABLE classes
            DROP COLUMN create_at;

    END IF;

    -- ════════════════════════════════════════════════════════════
    --  MIGRATE: schedules  (create_at  →  created_at)
    -- ════════════════════════════════════════════════════════════
    IF has_create_at_schedules = 1 AND has_created_at_schedules = 0 THEN

        ALTER TABLE schedules
            ADD COLUMN created_at DATETIME(6) NULL AFTER center_id;

        UPDATE schedules
        SET created_at = create_at
        WHERE created_at IS NULL
          AND YEAR(create_at) > 0;

        UPDATE schedules
        SET created_at = CURRENT_TIMESTAMP(6)
        WHERE created_at IS NULL;

        ALTER TABLE schedules
            MODIFY COLUMN created_at DATETIME(6) NOT NULL;

        ALTER TABLE schedules
            DROP COLUMN create_at;

    END IF;

END$$

DELIMITER ;

-- Run the migration
CALL migrate_date_and_created_at_columns();

-- Clean up
DROP PROCEDURE IF EXISTS migrate_date_and_created_at_columns;
