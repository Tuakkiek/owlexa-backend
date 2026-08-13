-- Preserve existing generic AI grading records while switching the provider enum.
ALTER TABLE `ai_grading_jobs`
    MODIFY COLUMN `model_provider` enum('OPENAI','GEMINI') NOT NULL;

UPDATE `ai_grading_jobs`
SET `model_provider` = 'GEMINI'
WHERE `model_provider` = 'OPENAI';

ALTER TABLE `ai_grading_jobs`
    MODIFY COLUMN `model_provider` enum('GEMINI') NOT NULL;
