-- Thêm field phục vụ nghỉ ngang (drop) có lý do + thời điểm hiệu lực
ALTER TABLE class_enrollments
  ADD COLUMN drop_reason VARCHAR(30) NULL,
  ADD COLUMN dropped_at DATETIME(6) NULL,
  ADD COLUMN dropped_by_user_id BIGINT NULL,
  ADD CONSTRAINT fk_class_enrollments_dropped_by
    FOREIGN KEY (dropped_by_user_id) REFERENCES users (id);

-- Thêm field phục vụ chuyển lớp (transfer) — nối 2 enrollment với nhau
ALTER TABLE class_enrollments
  ADD COLUMN transferred_to_enrollment_id BIGINT NULL,
  ADD COLUMN transferred_from_enrollment_id BIGINT NULL,
  ADD CONSTRAINT fk_class_enrollments_transferred_to
    FOREIGN KEY (transferred_to_enrollment_id) REFERENCES class_enrollments (id),
  ADD CONSTRAINT fk_class_enrollments_transferred_from
    FOREIGN KEY (transferred_from_enrollment_id) REFERENCES class_enrollments (id);

-- Thêm giá trị TRANSFERRED vào enum status
-- V2 original: enum('ACTIVE','DROPPED','PENDING','SUSPENDED')
ALTER TABLE class_enrollments
  MODIFY COLUMN status ENUM('ACTIVE','DROPPED','PENDING','SUSPENDED','TRANSFERRED') COLLATE utf8mb4_unicode_ci NOT NULL;
