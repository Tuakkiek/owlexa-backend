CREATE TABLE grading_criteria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    center_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,

    CONSTRAINT fk_grading_criteria_center
        FOREIGN KEY (center_id) REFERENCES centers(id),
    CONSTRAINT fk_grading_criteria_created_by
        FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_grading_criteria_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(id),

    INDEX idx_grading_criteria_center_deleted (center_id, deleted_at),
    INDEX idx_grading_criteria_center_name_deleted (center_id, name, deleted_at)
);
