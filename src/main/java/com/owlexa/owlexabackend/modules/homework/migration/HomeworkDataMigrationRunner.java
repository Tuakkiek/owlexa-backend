package com.owlexa.owlexabackend.modules.homework.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class HomeworkDataMigrationRunner {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void runMigration() {
        log.info("Checking if homework data migration is needed...");

        try {
            // Check if homework table exists
            Integer tableExists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'homework'",
                    Integer.class
            );

            if (tableExists == null || tableExists == 0) {
                log.info("Legacy 'homework' table not found. Skipping migration.");
                return;
            }

            Integer homeworkCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM homework", Integer.class);
            if (homeworkCount == null || homeworkCount == 0) {
                log.info("No records in 'homework' table. Skipping migration.");
                return;
            }

            log.info("Found {} records in legacy 'homework' table. Starting migration to template/assignment architecture...", homeworkCount);

            // 1. Insert into homework_template
            log.info("Migrating templates...");
            jdbcTemplate.update("""
                INSERT INTO homework_template (id, title, description, max_score, created_at, updated_at, center_id, teacher_id, homework_type, difficulty, archived, version)
                SELECT id, title, description, max_score, created_at, updated_at, center_id, teacher_id, 'MIXED', 'MEDIUM', false, 1
                FROM homework h
                ON DUPLICATE KEY UPDATE id=id
            """);

            // 2. Insert into homework_assignment
            log.info("Migrating assignments...");
            jdbcTemplate.update("""
                INSERT INTO homework_assignment (id, clazz_id, teacher_id, available_from, due_date, close_at, status, is_grades_released, allow_late_submission, allow_resubmit, publish_score_immediately, show_answer_after_grading, center_id, homework_template_id, created_at, updated_at)
                SELECT id, clazz_id, teacher_id, created_at, due_date, due_date, status, is_grades_released, false, false, publish_score_immediately, show_answer_after_grading, center_id, id, created_at, updated_at
                FROM homework h
                ON DUPLICATE KEY UPDATE id=id
            """);

            // 3. Update homework_question to link to homework_template
            log.info("Updating questions...");
            jdbcTemplate.update("""
                UPDATE homework_question
                SET homework_template_id = homework_id
                WHERE homework_template_id IS NULL AND homework_id IS NOT NULL
            """);

            // 4. Update homework_submission to link to homework_assignment
            log.info("Updating submissions...");
            jdbcTemplate.update("""
                UPDATE homework_submission
                SET homework_assignment_id = homework_id
                WHERE homework_assignment_id IS NULL AND homework_id IS NOT NULL
            """);

            // 5. Update analytics_class_performance to link to homework_assignment
            log.info("Updating analytics_class_performance...");
            jdbcTemplate.update("""
                UPDATE analytics_class_performance
                SET homework_assignment_id = homework_id
                WHERE homework_assignment_id IS NULL AND homework_id IS NOT NULL
            """);

            // 6. Update analytics_rubric_weakness to link to homework_assignment
            log.info("Updating analytics_rubric_weakness...");
            try {
                jdbcTemplate.update("""
                    UPDATE analytics_rubric_weakness
                    SET homework_assignment_id = homework_id
                    WHERE homework_assignment_id IS NULL AND homework_id IS NOT NULL
                """);
            } catch (Exception ignored) {
                // Ignore if homework_id was never part of analytics_rubric_weakness
            }

            // Optionally drop the old table and columns, but it's safer to just leave them or drop them later
            // We'll rename the homework table to _legacy_homework to prevent accidental queries and allow rollback
            log.info("Renaming legacy homework table to _legacy_homework...");
            jdbcTemplate.execute("RENAME TABLE homework TO _legacy_homework");
            
            // Drop foreign keys if needed, but RENAME handles most cases.
            // We won't drop the homework_id columns on child tables yet, to be safe.

            log.info("Homework data migration completed successfully!");

        } catch (Exception e) {
            log.error("Failed to execute homework data migration. Error: {}", e.getMessage(), e);
        }
    }
}
