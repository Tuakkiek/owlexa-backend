-- Normalize tenant ownership, repair demo schedule data, and remove legacy
-- columns/tables that are no longer represented by the JPA model.

-- Courses are tenant-scoped. Clone the shared center-1 courses before moving
-- center-2 classes, so class -> course never crosses tenant boundaries.
INSERT INTO courses (
    center_id, code, name, description, default_duration, default_session_count,
    default_monthly_fee, default_teacher_user_id, is_active, created_at, updated_at
)
SELECT
    2,
    'TOEIC-750-TD',
    c.name,
    c.description,
    c.default_duration,
    c.default_session_count,
    c.default_monthly_fee,
    11,
    c.is_active,
    NOW(6),
    NOW(6)
FROM courses c
WHERE c.id = 1
  AND NOT EXISTS (SELECT 1 FROM courses x WHERE x.center_id = 2 AND x.code = 'TOEIC-750-TD');

INSERT INTO courses (
    center_id, code, name, description, default_duration, default_session_count,
    default_monthly_fee, default_teacher_user_id, is_active, created_at, updated_at
)
SELECT
    2,
    'IELTS-ACAD-TD',
    c.name,
    c.description,
    c.default_duration,
    c.default_session_count,
    c.default_monthly_fee,
    12,
    c.is_active,
    NOW(6),
    NOW(6)
FROM courses c
WHERE c.id = 2
  AND NOT EXISTS (SELECT 1 FROM courses x WHERE x.center_id = 2 AND x.code = 'IELTS-ACAD-TD');

INSERT INTO courses (
    center_id, code, name, description, default_duration, default_session_count,
    default_monthly_fee, default_teacher_user_id, is_active, created_at, updated_at
)
SELECT
    2,
    'COMM-COMM-TD',
    c.name,
    c.description,
    c.default_duration,
    c.default_session_count,
    c.default_monthly_fee,
    11,
    c.is_active,
    NOW(6),
    NOW(6)
FROM courses c
WHERE c.id = 4
  AND NOT EXISTS (SELECT 1 FROM courses x WHERE x.center_id = 2 AND x.code = 'COMM-COMM-TD');

INSERT INTO courses (
    center_id, code, name, description, default_duration, default_session_count,
    default_monthly_fee, default_teacher_user_id, is_active, created_at, updated_at
)
SELECT
    2,
    'VSTEP-B2-TD',
    c.name,
    c.description,
    c.default_duration,
    c.default_session_count,
    c.default_monthly_fee,
    12,
    c.is_active,
    NOW(6),
    NOW(6)
FROM courses c
WHERE c.id = 3
  AND NOT EXISTS (SELECT 1 FROM courses x WHERE x.center_id = 2 AND x.code = 'VSTEP-B2-TD');

UPDATE classes cl
JOIN courses c ON c.center_id = 2 AND c.code = 'TOEIC-750-TD'
SET cl.course_id = c.id
WHERE cl.id = 6 AND cl.center_id = 2;

UPDATE classes cl
JOIN courses c ON c.center_id = 2 AND c.code = 'IELTS-ACAD-TD'
SET cl.course_id = c.id
WHERE cl.id = 7 AND cl.center_id = 2;

UPDATE classes cl
JOIN courses c ON c.center_id = 2 AND c.code = 'COMM-COMM-TD'
SET cl.course_id = c.id
WHERE cl.id = 8 AND cl.center_id = 2;

UPDATE classes cl
JOIN courses c ON c.center_id = 2 AND c.code = 'VSTEP-B2-TD'
SET cl.course_id = c.id
WHERE cl.id = 10 AND cl.center_id = 2;

-- Keep the one-off mock test inside the class window.
UPDATE classes
SET end_date = '2026-09-12'
WHERE id = 2 AND end_date < '2026-09-12';

-- The practice event was sharing B201 with an active VSTEP lesson.
UPDATE schedule_events
SET room_id = 1
WHERE id = 1128 AND room_id = 3;

-- Complete the active classes that still have enrolled students but lost their
-- old weekly schedules during the rule-based schedule migration.
UPDATE classes
SET start_date = '2026-08-17', end_date = '2026-10-14'
WHERE id = 7;

UPDATE classes
SET start_date = '2026-08-15', end_date = '2026-10-18'
WHERE id = 8;

UPDATE classes
SET end_date = '2026-09-24'
WHERE id = 108 AND end_date IS NULL;

INSERT INTO schedule_recurring_rules (
    center_id, class_id, room_id, teacher_user_id, repeat_type, days_of_week,
    start_date, end_date, start_time, end_time, type, is_active, created_at, updated_at
)
SELECT 2, 7, 6, 12, 'WEEKLY', '1,3', '2026-08-17', '2026-10-14',
       '18:00:00', '20:30:00', 'THEORY_CLASS', b'1', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM schedule_recurring_rules WHERE class_id = 7 AND center_id = 2
);

INSERT INTO schedule_recurring_rules (
    center_id, class_id, room_id, teacher_user_id, repeat_type, days_of_week,
    start_date, end_date, start_time, end_time, type, is_active, created_at, updated_at
)
SELECT 2, 8, 5, 11, 'WEEKLY', '6,7', '2026-08-15', '2026-10-18',
       '09:00:00', '11:00:00', 'THEORY_CLASS', b'1', NOW(6), NOW(6)
WHERE NOT EXISTS (
    SELECT 1 FROM schedule_recurring_rules WHERE class_id = 8 AND center_id = 2
);

-- Generate the missing lesson events from the repaired weekly rules.
INSERT INTO schedule_events (
    center_id, class_id, recurring_rule_id, room_id, teacher_user_id,
    event_date, start_time, end_time, lesson_number, event_type, status,
    title, note, created_at, updated_at
)
SELECT o.center_id, o.class_id, o.rule_id, o.room_id, o.teacher_user_id,
       o.event_date, o.start_time, o.end_time, o.lesson_number,
       'LESSON', 'SCHEDULED', c.name, NULL, NOW(6), NOW(6)
FROM (
    SELECT r.id AS rule_id, r.center_id, r.class_id, r.room_id, r.teacher_user_id,
           r.start_time, r.end_time,
           DATE_ADD(r.start_date, INTERVAL day_offsets.day_offset DAY) AS event_date,
           ROW_NUMBER() OVER (
               PARTITION BY r.id
               ORDER BY DATE_ADD(r.start_date, INTERVAL day_offsets.day_offset DAY)
           ) AS lesson_number
    FROM schedule_recurring_rules r
    CROSS JOIN (
        SELECT ones.n + tens.n * 10 AS day_offset
        FROM (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
              UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
              UNION ALL SELECT 8 UNION ALL SELECT 9) ones
        CROSS JOIN (SELECT 0 AS n UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
                    UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
                    UNION ALL SELECT 8 UNION ALL SELECT 9) tens
    ) day_offsets
    WHERE r.class_id IN (7, 8)
      AND r.center_id = 2
      AND r.is_active = b'1'
      AND DATE_ADD(r.start_date, INTERVAL day_offsets.day_offset DAY) <= r.end_date
      AND FIND_IN_SET(
              IF(DAYOFWEEK(DATE_ADD(r.start_date, INTERVAL day_offsets.day_offset DAY)) = 1,
                 7, DAYOFWEEK(DATE_ADD(r.start_date, INTERVAL day_offsets.day_offset DAY)) - 1),
              r.days_of_week
          ) > 0
) o
JOIN classes c ON c.id = o.class_id
WHERE o.lesson_number <= CASE o.class_id WHEN 7 THEN 18 WHEN 8 THEN 20 END
  AND NOT EXISTS (
      SELECT 1 FROM schedule_events e
      WHERE e.recurring_rule_id = o.rule_id AND e.event_date = o.event_date
  );

-- These columns/tables belonged to removed legacy models and are empty.
-- The legacy columns are absent in some databases because earlier migrations
-- already created the normalized schema. Resolve constraints by column rather
-- than relying on Hibernate-generated names, which differ between databases.
SET @drop_sql = (
    SELECT IFNULL(
        CONCAT(
            'ALTER TABLE `class_enrollments` ',
            GROUP_CONCAT(DISTINCT CONCAT(
                'DROP FOREIGN KEY `', REPLACE(constraint_name, '`', '``'), '`'
            ) SEPARATOR ', ')
        ),
        'SELECT 1'
    )
    FROM information_schema.key_column_usage
    WHERE constraint_schema = DATABASE()
      AND table_name = 'class_enrollments'
      AND column_name = 'student_id'
      AND referenced_table_name IS NOT NULL
);
PREPARE drop_stmt FROM @drop_sql;
EXECUTE drop_stmt;
DEALLOCATE PREPARE drop_stmt;

SET @drop_sql = (
    SELECT IF(COUNT(*) = 0, 'SELECT 1', CONCAT(
        'ALTER TABLE `class_enrollments` ',
        GROUP_CONCAT(CONCAT('DROP COLUMN `', column_name, '`') SEPARATOR ', ')
    ))
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'class_enrollments'
      AND column_name = 'student_id'
);
PREPARE drop_stmt FROM @drop_sql;
EXECUTE drop_stmt;
DEALLOCATE PREPARE drop_stmt;

SET @drop_sql = (
    SELECT IF(COUNT(*) = 0, 'SELECT 1', CONCAT(
        'ALTER TABLE `classes` ',
        GROUP_CONCAT(CONCAT('DROP COLUMN `', column_name, '`') SEPARATOR ', ')
    ))
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'classes'
      AND column_name IN ('max_students', 'fee_per_month', 'level')
);
PREPARE drop_stmt FROM @drop_sql;
EXECUTE drop_stmt;
DEALLOCATE PREPARE drop_stmt;

SET @drop_sql = (
    SELECT IF(COUNT(*) = 0, 'SELECT 1', CONCAT(
        'ALTER TABLE `users` ',
        GROUP_CONCAT(CONCAT('DROP COLUMN `', column_name, '`') SEPARATOR ', ')
    ))
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'users'
      AND column_name IN ('center_id', 'role_id')
);
PREPARE drop_stmt FROM @drop_sql;
EXECUTE drop_stmt;
DEALLOCATE PREPARE drop_stmt;

ALTER TABLE courses
    MODIFY COLUMN center_id BIGINT NOT NULL;

DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS teachers;
DROP TABLE IF EXISTS roles;
