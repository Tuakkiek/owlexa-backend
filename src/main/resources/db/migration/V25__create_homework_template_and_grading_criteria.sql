-- Clean up old obsolete tables from V17 if they exist
DROP TABLE IF EXISTS homework_rubric_criteria;
DROP TABLE IF EXISTS homework_rubrics;
DROP TABLE IF EXISTS homework_question_options;
DROP TABLE IF EXISTS homework_questions;
DROP TABLE IF EXISTS homeworks;

-- Create Grading Criteria table
CREATE TABLE grading_criteria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    teacher_id BIGINT NOT NULL,
    center_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6)
);

-- Create Homework Templates table
CREATE TABLE homework_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    instructions TEXT,
    homework_type VARCHAR(50),
    estimated_time INT,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    version INT NOT NULL DEFAULT 1,
    parent_template_id BIGINT,
    grading_criteria_id BIGINT,
    max_score DOUBLE,
    teacher_id BIGINT NOT NULL,
    center_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6)
);

-- Create Homework Questions table
CREATE TABLE homework_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    homework_template_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    question_text TEXT NOT NULL,
    attached_image_url VARCHAR(1000),
    attached_audio_url VARCHAR(1000),
    attached_file_url VARCHAR(1000),
    max_score DOUBLE,
    sort_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_hq_template FOREIGN KEY (homework_template_id) REFERENCES homework_templates(id) ON DELETE CASCADE
);

-- Create Homework Question Options table
CREATE TABLE homework_question_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    sort_order INT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_hqo_question FOREIGN KEY (question_id) REFERENCES homework_questions(id) ON DELETE CASCADE
);

-- Create Homework Rubrics table
CREATE TABLE homework_rubrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    max_score DOUBLE,
    CONSTRAINT fk_hr_question FOREIGN KEY (question_id) REFERENCES homework_questions(id) ON DELETE CASCADE,
    CONSTRAINT uk_hr_question UNIQUE (question_id)
);

-- Create Homework Rubric Criteria table
CREATE TABLE homework_rubric_criteria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rubric_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    max_score DOUBLE,
    display_order INT NOT NULL,
    CONSTRAINT fk_hrc_rubric FOREIGN KEY (rubric_id) REFERENCES homework_rubrics(id) ON DELETE CASCADE
);
