-- V23: AI Scoring Infrastructure

ALTER TABLE homework_question_submissions
    ADD COLUMN ai_scoring_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN ai_scored_at DATETIME(6);

CREATE TABLE ai_scoring_jobs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id   BIGINT NOT NULL,
    question_sub_id BIGINT NOT NULL,
    center_id       BIGINT NOT NULL,
    status          VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    attempt_count   INT NOT NULL DEFAULT 0,
    model_used      VARCHAR(100),
    prompt_tokens   INT,
    response_tokens INT,
    error_message   TEXT,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    INDEX idx_ai_job_submission (submission_id),
    INDEX idx_ai_job_question_sub (question_sub_id),
    INDEX idx_ai_job_status (status)
);
