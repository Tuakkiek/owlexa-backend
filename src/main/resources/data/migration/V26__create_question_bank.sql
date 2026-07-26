CREATE TABLE questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    center_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NULL,
    content LONGTEXT NOT NULL,
    difficulty VARCHAR(30) NULL,
    points DECIMAL(6, 2) NULL,
    grading_criteria_id BIGINT NULL,
    explanation LONGTEXT NULL,
    sample_answer LONGTEXT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,

    CONSTRAINT fk_questions_center
        FOREIGN KEY (center_id) REFERENCES centers(id),
    CONSTRAINT fk_questions_grading_criteria
        FOREIGN KEY (grading_criteria_id) REFERENCES grading_criteria(id),
    CONSTRAINT fk_questions_created_by
        FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_questions_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(id),

    INDEX idx_questions_center_deleted_updated (center_id, deleted_at, updated_at),
    INDEX idx_questions_center_type_deleted (center_id, type, deleted_at),
    INDEX idx_questions_center_difficulty_deleted (center_id, difficulty, deleted_at),
    INDEX idx_questions_center_grading_criteria_deleted (center_id, grading_criteria_id, deleted_at)
);

CREATE TABLE question_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    content LONGTEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_question_options_question
        FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,

    INDEX idx_question_options_question_order (question_id, display_order)
);
