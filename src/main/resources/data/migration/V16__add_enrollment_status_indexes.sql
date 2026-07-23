-- V16: Add composite index on class_enrollments for enrollment-status-filtered queries.
-- This optimizes the student schedule lookup (student_user_id + center_id + status)
-- and the dropped-enrollment owner view (class_id + status).
-- Migration is non-breaking and fully backward-compatible.

CREATE INDEX idx_enrollments_student_center_status
    ON class_enrollments (student_user_id, center_id, status);

CREATE INDEX idx_enrollments_class_status
    ON class_enrollments (class_id, status);
