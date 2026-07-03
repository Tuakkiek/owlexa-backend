-- ============================================================
-- Add clazz_id to student_documents table.
-- Safe: only adds column if it doesn't already exist.
-- ============================================================
-- How to run manually:
--   mysql -u root -p owlexa_db < V6__add_clazz_id_to_student_documents.sql
--
-- How to run inside MySQL CLI:
--   SOURCE V6__add_clazz_id_to_student_documents.sql;
-- ============================================================

DELIMITER $$

DROP PROCEDURE IF EXISTS migrate_add_clazz_id_to_student_documents$$

CREATE PROCEDURE migrate_add_clazz_id_to_student_documents()
BEGIN
    DECLARE has_column INT DEFAULT 0;

    SELECT COUNT(*) INTO has_column
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name   = 'student_documents'
      AND column_name  = 'clazz_id';

    IF has_column = 0 THEN
        ALTER TABLE student_documents
            ADD COLUMN clazz_id BIGINT NULL
            AFTER center_id;

        -- Add FK if the foreign key constraint doesn't already exist
        -- (MySQL doesn't have IF NOT EXISTS for constraints, so we suppress
        --  the error via a handler — or simply skip if it was added before.)
        -- Safe to run multiple times.
        -- ALTER TABLE student_documents
        --     ADD CONSTRAINT fk_student_documents_clazz
        --     FOREIGN KEY (clazz_id) REFERENCES classes(id)
        --     ON DELETE SET NULL;

        -- Add index for the repository query: findAllByClazzIdAndCenterId...
        CREATE INDEX idx_student_documents_clazz_center
            ON student_documents(clazz_id, center_id);
    END IF;

END$$

DELIMITER ;

CALL migrate_add_clazz_id_to_student_documents();
DROP PROCEDURE IF EXISTS migrate_add_clazz_id_to_student_documents;
