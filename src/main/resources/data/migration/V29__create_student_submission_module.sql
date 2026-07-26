CREATE TABLE submission_attempts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_recipient_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    attempt_number INT NOT NULL,
    assignment_title_snapshot VARCHAR(255) NOT NULL,
    assignment_type_snapshot VARCHAR(50) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    last_saved_at DATETIME(6) NULL,
    submitted_at DATETIME(6) NULL,
    auto_score DECIMAL(8, 2) NULL,
    max_score DECIMAL(8, 2) NULL,
    active_attempt_key BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_submission_attempts_recipient
        FOREIGN KEY (assignment_recipient_id) REFERENCES assignment_recipients(id),
    CONSTRAINT uk_submission_attempts_recipient_number
        UNIQUE (assignment_recipient_id, attempt_number),
    CONSTRAINT uk_submission_attempts_active_key
        UNIQUE (active_attempt_key),
    CONSTRAINT chk_submission_attempts_attempt_number
        CHECK (attempt_number >= 1),
    CONSTRAINT chk_submission_attempts_auto_score
        CHECK (auto_score IS NULL OR auto_score >= 0),
    CONSTRAINT chk_submission_attempts_max_score
        CHECK (max_score IS NULL OR max_score >= 0),
    CONSTRAINT chk_submission_attempts_score_bounds
        CHECK (
            auto_score IS NULL
            OR max_score IS NULL
            OR auto_score <= max_score
        ),

    INDEX idx_submission_attempts_recipient_status (assignment_recipient_id, status),
    INDEX idx_submission_attempts_status_updated (status, updated_at),
    INDEX idx_submission_attempts_submitted_at (submitted_at)
);

CREATE TABLE submission_answers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    attempt_id BIGINT NOT NULL,
    assignment_item_id BIGINT NOT NULL,
    answer_text LONGTEXT NULL,
    auto_score DECIMAL(8, 2) NULL,
    max_score DECIMAL(8, 2) NULL,
    graded_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_submission_answers_attempt
        FOREIGN KEY (attempt_id) REFERENCES submission_attempts(id),
    CONSTRAINT fk_submission_answers_assignment_item
        FOREIGN KEY (assignment_item_id) REFERENCES assignment_items(id),
    CONSTRAINT uk_submission_answers_attempt_item
        UNIQUE (attempt_id, assignment_item_id),
    CONSTRAINT chk_submission_answers_auto_score
        CHECK (auto_score IS NULL OR auto_score >= 0),
    CONSTRAINT chk_submission_answers_max_score
        CHECK (max_score IS NULL OR max_score >= 0),
    CONSTRAINT chk_submission_answers_score_bounds
        CHECK (
            auto_score IS NULL
            OR max_score IS NULL
            OR auto_score <= max_score
        ),

    INDEX idx_submission_answers_assignment_item (assignment_item_id)
);

CREATE TABLE submission_answer_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_answer_id BIGINT NOT NULL,
    assignment_item_option_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_submission_answer_options_answer
        FOREIGN KEY (submission_answer_id) REFERENCES submission_answers(id),
    CONSTRAINT fk_submission_answer_options_assignment_option
        FOREIGN KEY (assignment_item_option_id) REFERENCES assignment_item_options(id),
    CONSTRAINT uk_submission_answer_options_answer_option
        UNIQUE (submission_answer_id, assignment_item_option_id),

    INDEX idx_submission_answer_options_assignment_option (assignment_item_option_id)
);
