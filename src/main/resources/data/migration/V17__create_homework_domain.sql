CREATE TABLE homework (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    instructions TEXT,
    status VARCHAR(50) NOT NULL,
    due_date DATETIME(6),
    published_at DATETIME(6),
    closed_at DATETIME(6),
    max_score DOUBLE,
    allow_late_submission BOOLEAN DEFAULT FALSE,
    allow_resubmit BOOLEAN DEFAULT FALSE,
    publish_score_immediately BOOLEAN DEFAULT FALSE,
    show_answer_after_grading BOOLEAN DEFAULT FALSE,
    clazz_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    center_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE homework_questions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    homework_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    question_text TEXT NOT NULL,
    attached_image_url VARCHAR(1000),
    attached_audio_url VARCHAR(1000),
    attached_file_url VARCHAR(1000),
    max_score DOUBLE,
    sort_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_hq_homework FOREIGN KEY (homework_id) REFERENCES homework(id) ON DELETE CASCADE
);

CREATE TABLE homework_question_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    sort_order INT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_hqo_question FOREIGN KEY (question_id) REFERENCES homework_questions(id) ON DELETE CASCADE
);

CREATE TABLE homework_rubrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    max_score DOUBLE,
    CONSTRAINT fk_hr_question FOREIGN KEY (question_id) REFERENCES homework_questions(id) ON DELETE CASCADE,
    CONSTRAINT uk_hr_question UNIQUE (question_id)
);

CREATE TABLE homework_rubric_criteria (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rubric_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    max_score DOUBLE,
    display_order INT NOT NULL,
    CONSTRAINT fk_hrc_rubric FOREIGN KEY (rubric_id) REFERENCES homework_rubrics(id) ON DELETE CASCADE
);
