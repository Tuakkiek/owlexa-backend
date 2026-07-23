ALTER TABLE analytics_class_performance 
CHANGE last_updated_at updated_at DATETIME(6) NOT NULL,
CHANGE total_submissions submitted_count INT NOT NULL DEFAULT 0,
ADD COLUMN graded_count INT NOT NULL DEFAULT 0,
ADD COLUMN late_submission_count INT NOT NULL DEFAULT 0,
ADD COLUMN missing_submission_count INT NOT NULL DEFAULT 0,
ADD COLUMN highest_score DOUBLE,
ADD COLUMN lowest_score DOUBLE,
ADD COLUMN pass_rate DOUBLE NOT NULL DEFAULT 0.0;

ALTER TABLE analytics_rubric_weakness 
CHANGE last_updated_at updated_at DATETIME(6) NOT NULL,
ADD COLUMN submission_count INT NOT NULL DEFAULT 0,
ADD COLUMN max_score DOUBLE,
ADD COLUMN percentage DOUBLE NOT NULL DEFAULT 0.0;

CREATE TABLE analytics_student_performance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    center_id BIGINT NOT NULL,
    average_score DOUBLE NOT NULL DEFAULT 0.0,
    completion_rate DOUBLE NOT NULL DEFAULT 0.0,
    late_submissions INT NOT NULL DEFAULT 0,
    missing_homework INT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_student_class (student_id, class_id)
);

CREATE TABLE analytics_question_performance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    homework_id BIGINT NOT NULL,
    center_id BIGINT NOT NULL,
    correct_rate DOUBLE NOT NULL DEFAULT 0.0,
    wrong_rate DOUBLE NOT NULL DEFAULT 0.0,
    average_score DOUBLE NOT NULL DEFAULT 0.0,
    difficulty_indicator VARCHAR(50),
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_question_homework (question_id, homework_id)
);

CREATE INDEX idx_class_homework ON analytics_class_performance(class_id, homework_id);
CREATE INDEX idx_student_class ON analytics_student_performance(student_id, class_id);
CREATE INDEX idx_question_homework ON analytics_question_performance(question_id, homework_id);
