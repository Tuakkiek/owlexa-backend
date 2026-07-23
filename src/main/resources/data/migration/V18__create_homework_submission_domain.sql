CREATE TABLE homework_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    homework_id BIGINT NOT NULL,
    student_user_id BIGINT NOT NULL,
    attempt_number INT NOT NULL DEFAULT 1,
    status VARCHAR(50) NOT NULL,
    started_at DATETIME(6),
    last_saved_at DATETIME(6),
    submitted_at DATETIME(6),
    graded_by_user_id BIGINT,
    graded_at DATETIME(6),
    teacher_feedback TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    center_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_hw_sub_homework FOREIGN KEY (homework_id) REFERENCES homework(id) ON DELETE CASCADE,
    CONSTRAINT fk_hw_sub_student FOREIGN KEY (student_user_id) REFERENCES users(id),
    CONSTRAINT fk_hw_sub_grader FOREIGN KEY (graded_by_user_id) REFERENCES users(id)
);

CREATE TABLE homework_question_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    homework_submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    text_answer TEXT,
    score DOUBLE,
    is_correct BOOLEAN,
    teacher_feedback TEXT,
    ai_feedback TEXT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_hw_q_sub_submission FOREIGN KEY (homework_submission_id) REFERENCES homework_submissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_hw_q_sub_question FOREIGN KEY (question_id) REFERENCES homework_questions(id) ON DELETE CASCADE
);

CREATE TABLE homework_submission_attachments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_submission_id BIGINT NOT NULL,
    file_url VARCHAR(2000) NOT NULL,
    file_name VARCHAR(255),
    file_type VARCHAR(100),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_hw_sub_att_question_sub FOREIGN KEY (question_submission_id) REFERENCES homework_question_submissions(id) ON DELETE CASCADE
);

CREATE TABLE homework_question_submission_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_submission_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_hw_q_sub_opt_q_sub FOREIGN KEY (question_submission_id) REFERENCES homework_question_submissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_hw_q_sub_opt_opt FOREIGN KEY (option_id) REFERENCES homework_question_options(id) ON DELETE CASCADE,
    CONSTRAINT uk_hw_q_sub_opt UNIQUE (question_submission_id, option_id)
);

CREATE TABLE homework_rubric_criterion_scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_submission_id BIGINT NOT NULL,
    criterion_id BIGINT NOT NULL,
    score DOUBLE,
    comment TEXT,
    grader_type VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_hw_rub_crit_score_q_sub FOREIGN KEY (question_submission_id) REFERENCES homework_question_submissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_hw_rub_crit_score_crit FOREIGN KEY (criterion_id) REFERENCES homework_rubric_criteria(id) ON DELETE CASCADE
);
