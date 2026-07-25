-- V27__drop_all_homework_tables.sql
-- Clean up all homework related tables

DROP TABLE IF EXISTS homework_rubric_criterion_scores;
DROP TABLE IF EXISTS homework_question_submission_options;
DROP TABLE IF EXISTS homework_question_submissions;
DROP TABLE IF EXISTS homework_submission_attachments;
DROP TABLE IF EXISTS homework_submissions;
DROP TABLE IF EXISTS homework_assignments;
DROP TABLE IF EXISTS homework_rubric_criteria;
DROP TABLE IF EXISTS homework_rubrics;
DROP TABLE IF EXISTS homework_question_options;
DROP TABLE IF EXISTS homework_questions;
DROP TABLE IF EXISTS homework_templates;
DROP TABLE IF EXISTS grading_criteria;

DROP TABLE IF EXISTS analytics_student_performance;
DROP TABLE IF EXISTS analytics_rubric_weakness;
DROP TABLE IF EXISTS analytics_question_performance;
DROP TABLE IF EXISTS analytics_class_performance;
DROP TABLE IF EXISTS ai_scoring_jobs;
