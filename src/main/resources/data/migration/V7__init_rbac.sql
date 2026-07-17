-- Create role_permission mapping table
CREATE TABLE role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role VARCHAR(50) NOT NULL,
    permission_id BIGINT NOT NULL,
    CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES permissions (id),
    UNIQUE KEY uk_role_permission (role, permission_id)
);

-- Add type column to user_permission to support ALLOW/DENY
ALTER TABLE user_permission ADD COLUMN type VARCHAR(20) NOT NULL DEFAULT 'ALLOW';
