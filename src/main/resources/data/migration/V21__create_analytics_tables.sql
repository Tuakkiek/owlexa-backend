CREATE TABLE analytics_class_performance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    homework_id BIGINT NOT NULL,
    center_id BIGINT NOT NULL,
    total_submissions INT NOT NULL DEFAULT 0,
    average_score DOUBLE NOT NULL DEFAULT 0.0,
    last_updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_class_homework (class_id, homework_id)
);

CREATE TABLE analytics_rubric_weakness (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    homework_id BIGINT NOT NULL,
    rubric_criterion_id BIGINT NOT NULL,
    center_id BIGINT NOT NULL,
    average_score DOUBLE NOT NULL DEFAULT 0.0,
    last_updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_class_criterion (class_id, rubric_criterion_id)
);
