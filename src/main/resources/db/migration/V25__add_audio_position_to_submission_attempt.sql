ALTER TABLE submission_attempts ADD COLUMN audio_position_seconds INT DEFAULT 0;
ALTER TABLE submission_attempts ADD COLUMN audio_completed BOOLEAN DEFAULT FALSE;
