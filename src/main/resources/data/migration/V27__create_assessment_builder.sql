CREATE TABLE assessments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    center_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description LONGTEXT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,

    CONSTRAINT fk_assessments_center
        FOREIGN KEY (center_id) REFERENCES centers(id),
    CONSTRAINT fk_assessments_created_by
        FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_assessments_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(id),

    INDEX idx_assessments_center_deleted_updated (center_id, deleted_at, updated_at),
    INDEX idx_assessments_center_type_deleted (center_id, type, deleted_at),
    INDEX idx_assessments_center_status_deleted (center_id, status, deleted_at),
    INDEX idx_assessments_center_title_deleted (center_id, title, deleted_at)
);

CREATE TABLE assessment_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assessment_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    question_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NULL,
    content LONGTEXT NOT NULL,
    difficulty VARCHAR(30) NULL,
    points DECIMAL(6, 2) NULL,
    explanation LONGTEXT NULL,
    sample_answer LONGTEXT NULL,
    grading_criteria_id BIGINT NULL,
    grading_criteria_name VARCHAR(255) NULL,
    grading_criteria_content LONGTEXT NULL,
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_assessment_items_assessment
        FOREIGN KEY (assessment_id) REFERENCES assessments(id) ON DELETE CASCADE,
    CONSTRAINT fk_assessment_items_question
        FOREIGN KEY (question_id) REFERENCES questions(id),
    CONSTRAINT fk_assessment_items_grading_criteria
        FOREIGN KEY (grading_criteria_id) REFERENCES grading_criteria(id),
    CONSTRAINT uk_assessment_items_assessment_question
        UNIQUE (assessment_id, question_id),
    CONSTRAINT uk_assessment_items_assessment_order
        UNIQUE (assessment_id, display_order),

    INDEX idx_assessment_items_question (question_id)
);

CREATE TABLE assessment_item_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assessment_item_id BIGINT NOT NULL,
    content LONGTEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_assessment_item_options_item
        FOREIGN KEY (assessment_item_id) REFERENCES assessment_items(id) ON DELETE CASCADE,
    CONSTRAINT uk_assessment_item_options_item_order
        UNIQUE (assessment_item_id, display_order)
);
