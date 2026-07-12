-- =============================================================================
-- Session Management Migration
-- Owlexa Backend — Production-ready session management
-- Date: 2026-07-11
-- =============================================================================
-- 
-- WHAT THIS MIGRATION DOES:
-- 1. Renames expired_at → inactive_expire_at (sliding expiration)
-- 2. Adds absolute_expire_at (hard re-login after 90 days)
-- 3. Adds device_key (stable device fingerprint for dedup)
-- 4. Adds rotation_count (refresh token rotation counter)
-- 5. Adds revoked_reason + revoked_at (audit trail)
-- 6. Adds new indexes for dedup + cleanup performance
--
-- ROLLBACK (if needed):
--   ALTER TABLE user_sessions
--       CHANGE COLUMN inactive_expire_at expired_at DATETIME NOT NULL,
--       DROP COLUMN absolute_expire_at,
--       DROP COLUMN device_key,
--       DROP COLUMN rotation_count,
--       DROP COLUMN revoked_reason,
--       DROP COLUMN revoked_at,
--       DROP INDEX idx_sessions_user_device,
--       DROP INDEX idx_sessions_cleanup;
-- =============================================================================

-- Step 1: Rename expired_at → inactive_expire_at (semantic rename)
ALTER TABLE user_sessions
    CHANGE COLUMN expired_at inactive_expire_at DATETIME NOT NULL;

-- Step 2: Add new columns
ALTER TABLE user_sessions
    ADD COLUMN device_key VARCHAR(64) NULL AFTER user_agent,
    ADD COLUMN rotation_count INT NOT NULL DEFAULT 0 AFTER device_key,
    ADD COLUMN revoked_reason VARCHAR(50) NULL AFTER rotation_count,
    ADD COLUMN revoked_at DATETIME NULL AFTER revoked_reason,
    ADD COLUMN absolute_expire_at DATETIME NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL 90 DAY) AFTER inactive_expire_at;

-- Step 3: Add indexes
CREATE INDEX idx_sessions_user_device ON user_sessions (user_id, device_key);
CREATE INDEX idx_sessions_cleanup ON user_sessions (is_active, last_used_at);

-- Step 4: Backfill existing data
-- Set device_key for existing sessions (backfill from user_id + user_agent)
-- This is done via application code on next login; existing sessions without
-- device_key will simply not participate in dedup until re-login.
-- Set absolute_expire_at = created_at + 90 days for existing rows
UPDATE user_sessions
SET absolute_expire_at = DATE_ADD(created_at, INTERVAL 90 DAY)
WHERE absolute_expire_at IS NULL;

-- Step 5: Verify
SELECT
    COUNT(*) AS total_sessions,
    SUM(CASE WHEN is_active = 1 THEN 1 ELSE 0 END) AS active_sessions,
    SUM(CASE WHEN is_active = 0 THEN 1 ELSE 0 END) AS inactive_sessions
FROM user_sessions;
