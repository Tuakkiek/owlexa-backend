ALTER TABLE `questions`
    ADD COLUMN `question_code` VARCHAR(32) NULL;

UPDATE `questions`
SET `question_code` = CONCAT('Q-', LPAD(`id`, 6, '0'))
WHERE `question_code` IS NULL;

ALTER TABLE `questions`
    MODIFY `question_code` VARCHAR(32) NOT NULL;

CREATE UNIQUE INDEX `uk_questions_question_code`
    ON `questions` (`question_code`);
