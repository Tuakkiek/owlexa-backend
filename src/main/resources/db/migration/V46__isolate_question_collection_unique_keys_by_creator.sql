ALTER TABLE `question_collections`
  DROP INDEX `uk_question_collections_center_code`,
  DROP INDEX `uk_question_collections_center_active_name`;

ALTER TABLE `question_collections`
  ADD UNIQUE KEY `uk_question_collections_center_creator_code` (`center_id`, `created_by`, `code`),
  ADD UNIQUE KEY `uk_question_collections_center_creator_active_name` (`center_id`, `created_by`, `active_name`);
