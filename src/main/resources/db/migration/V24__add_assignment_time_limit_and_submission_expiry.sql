ALTER TABLE assignments ADD COLUMN time_limit_minutes INT;
ALTER TABLE submission_attempts ADD COLUMN expires_at TIMESTAMP;
