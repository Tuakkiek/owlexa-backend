-- Drop the type column from user_permission.
-- The column was added by V7 to support ALLOW/DENY overrides.
-- After RBAC simplification (Phases 1-4), the column is no longer
-- mapped by the UserPermission entity and is never read or written
-- by the application.
ALTER TABLE user_permission DROP COLUMN type;
