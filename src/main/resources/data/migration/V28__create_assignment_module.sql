CREATE TABLE assignments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    center_id BIGINT NOT NULL,
    assessment_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description LONGTEXT NULL,
    open_at DATETIME(6) NULL,
    due_at DATETIME(6) NULL,
    attempt_limit INT NULL,
    assessment_snapshot_at DATETIME(6) NULL,
    created_by BIGINT NOT NULL,
    updated_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6) NULL,

    CONSTRAINT fk_assignments_center
        FOREIGN KEY (center_id) REFERENCES centers(id),
    CONSTRAINT fk_assignments_assessment
        FOREIGN KEY (assessment_id) REFERENCES assessments(id),
    CONSTRAINT fk_assignments_created_by
        FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_assignments_updated_by
        FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT chk_assignments_attempt_limit
        CHECK (attempt_limit IS NULL OR attempt_limit >= 1),
    CONSTRAINT chk_assignments_time_window
        CHECK (open_at IS NULL OR due_at IS NULL OR open_at < due_at),

    INDEX idx_assignments_center_status_deleted (center_id, status, deleted_at),
    INDEX idx_assignments_center_deleted_updated (center_id, deleted_at, updated_at),
    INDEX idx_assignments_assessment (assessment_id),
    INDEX idx_assignments_center_type_deleted (center_id, type, deleted_at)
);

CREATE TABLE assignment_targets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    class_id BIGINT NULL,
    student_user_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_assignment_targets_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_targets_class
        FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_assignment_targets_student
        FOREIGN KEY (student_user_id) REFERENCES users(id),
    CONSTRAINT chk_assignment_targets_target
        CHECK (
            (target_type = 'CLASS' AND class_id IS NOT NULL AND student_user_id IS NULL)
            OR
            (target_type = 'STUDENT' AND class_id IS NULL AND student_user_id IS NOT NULL)
        ),
    CONSTRAINT uk_assignment_targets_class
        UNIQUE (assignment_id, target_type, class_id),
    CONSTRAINT uk_assignment_targets_student
        UNIQUE (assignment_id, target_type, student_user_id),

    INDEX idx_assignment_targets_assignment_type (assignment_id, target_type),
    INDEX idx_assignment_targets_class (class_id),
    INDEX idx_assignment_targets_student (student_user_id)
);

CREATE TABLE assignment_recipients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    student_user_id BIGINT NOT NULL,
    class_id BIGINT NULL,
    source_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_assignment_recipients_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_recipients_student
        FOREIGN KEY (student_user_id) REFERENCES users(id),
    CONSTRAINT fk_assignment_recipients_class
        FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT chk_assignment_recipients_source
        CHECK (
            (source_type = 'CLASS' AND class_id IS NOT NULL)
            OR
            (source_type = 'STUDENT')
        ),
    CONSTRAINT uk_assignment_recipients_assignment_student
        UNIQUE (assignment_id, student_user_id),

    INDEX idx_assignment_recipients_student_status (student_user_id, status),
    INDEX idx_assignment_recipients_student_assigned (student_user_id, assigned_at),
    INDEX idx_assignment_recipients_class (class_id)
);

CREATE TABLE assignment_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_id BIGINT NOT NULL,
    assessment_item_id BIGINT NULL,
    question_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NULL,
    content LONGTEXT NOT NULL,
    difficulty VARCHAR(30) NULL,
    points DECIMAL(6, 2) NULL,
    explanation LONGTEXT NULL,
    sample_answer LONGTEXT NULL,
    grading_criteria_name VARCHAR(255) NULL,
    grading_criteria_content LONGTEXT NULL,
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_assignment_items_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignments(id) ON DELETE CASCADE,
    CONSTRAINT fk_assignment_items_assessment_item
        FOREIGN KEY (assessment_item_id) REFERENCES assessment_items(id),
    CONSTRAINT uk_assignment_items_assignment_order
        UNIQUE (assignment_id, display_order),

    INDEX idx_assignment_items_assessment_item (assessment_item_id)
);

CREATE TABLE assignment_item_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    assignment_item_id BIGINT NOT NULL,
    content LONGTEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_assignment_item_options_item
        FOREIGN KEY (assignment_item_id) REFERENCES assignment_items(id) ON DELETE CASCADE,
    CONSTRAINT uk_assignment_item_options_item_order
        UNIQUE (assignment_item_id, display_order)
);
