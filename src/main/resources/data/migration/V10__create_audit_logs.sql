CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    center_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NULL,
    entity_id BIGINT NULL,
    description TEXT NULL,
    ip_address VARCHAR(45) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_audit_logs_center FOREIGN KEY (center_id) REFERENCES centers(id),
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users(id),
    INDEX idx_audit_logs_center_created (center_id, created_at DESC)
);
