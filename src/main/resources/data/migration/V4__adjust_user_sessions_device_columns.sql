-- Keep legacy sessions readable and stop truncating raw user-agent values.

UPDATE user_sessions
SET device_type = 'DESKTOP'
WHERE device_type = 'WEB';

ALTER TABLE user_sessions
    MODIFY COLUMN user_agent TEXT NULL;
