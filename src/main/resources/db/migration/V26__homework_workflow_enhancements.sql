-- V26__homework_workflow_enhancements.sql
-- Phase 1: Add new columns to support the redesigned 4-stage homework workflow
-- See: HOMEWORK_WORKFLOW_REDESIGN.md

-- ============================================================
-- 1. homework_templates: Quiz/Essay creation enhancements
-- ============================================================
ALTER TABLE homework_templates
    ADD COLUMN IF NOT EXISTS show_answer_after_submit BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS ai_scoring_criteria TEXT,
    ADD COLUMN IF NOT EXISTS word_limit_min INT,
    ADD COLUMN IF NOT EXISTS word_limit_max INT;

-- ============================================================
-- 2. homework_assignments: Assignment configuration
-- ============================================================
ALTER TABLE homework_assignments
    ADD COLUMN IF NOT EXISTS submission_limit INT DEFAULT 1;

-- ============================================================
-- 3. homework_submissions: Student submission tracking
-- ============================================================
ALTER TABLE homework_submissions
    ADD COLUMN IF NOT EXISTS word_count INT,
    ADD COLUMN IF NOT EXISTS time_spent INT,
    ADD COLUMN IF NOT EXISTS total_score DOUBLE,
    ADD COLUMN IF NOT EXISTS is_late BOOLEAN DEFAULT FALSE;

-- ============================================================
-- 4. homework_rubric_criterion_scores: AI-assisted grading
-- ============================================================
ALTER TABLE homework_rubric_criterion_scores
    ADD COLUMN IF NOT EXISTS ai_suggested_score DOUBLE,
    ADD COLUMN IF NOT EXISTS ai_feedback TEXT;
