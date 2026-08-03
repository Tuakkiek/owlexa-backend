-- V22: Make student_user_id nullable in student_documents
-- Allows class-level documents (uploaded by owner/teacher) without a specific student target
ALTER TABLE `student_documents` MODIFY COLUMN `student_user_id` BIGINT NULL;

-- Add uploader_user_id to track who uploaded the document (owner or teacher)
ALTER TABLE `student_documents` ADD COLUMN `uploader_user_id` BIGINT NULL AFTER `student_user_id`;
ALTER TABLE `student_documents` ADD CONSTRAINT `fk_student_documents_uploader_user_id`
    FOREIGN KEY (`uploader_user_id`) REFERENCES `users` (`id`);
