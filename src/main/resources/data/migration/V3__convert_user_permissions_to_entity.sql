-- Create permissions table
CREATE TABLE IF NOT EXISTS permissions (
                                           id BIGINT NOT NULL AUTO_INCREMENT,
                                           code VARCHAR(100) NOT NULL,
                                           description VARCHAR(255),
                                           PRIMARY KEY (id),
                                           UNIQUE KEY uk_permissions_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Rename old table (nếu tồn tại)
CREATE TABLE IF NOT EXISTS user_permissions (
    user_id BIGINT,
    permission_id BIGINT
);
ALTER TABLE user_permissions RENAME TO user_permissions_legacy;

-- Create new user_permissions table
CREATE TABLE user_permissions (
                                  id BIGINT NOT NULL AUTO_INCREMENT,
                                  user_id BIGINT NOT NULL,
                                  permission_id BIGINT NOT NULL,
                                  granted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (id),
                                  UNIQUE KEY uk_user_permissions_user_permission (user_id, permission_id),
                                  KEY idx_user_permissions_user_id (user_id),
                                  KEY idx_user_permissions_permission_id (permission_id),
                                  CONSTRAINT fk_user_permissions_user
                                      FOREIGN KEY (user_id) REFERENCES users(id)
                                          ON DELETE CASCADE
                                          ON UPDATE CASCADE,
                                  CONSTRAINT fk_user_permissions_permission
                                      FOREIGN KEY (permission_id) REFERENCES permissions(id)
                                          ON DELETE CASCADE
                                          ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Migrate data
INSERT INTO user_permissions (user_id, permission_id, granted_at)
SELECT user_id, permission_id, CURRENT_TIMESTAMP
FROM user_permissions_legacy;

-- Drop old table
DROP TABLE user_permissions_legacy;

-- Seed permissions
INSERT INTO permissions (code, description) VALUES
                                                ('VIEW_STUDENT', 'Can view student data'),
                                                ('EDIT_FEE', 'Can edit fee records'),
                                                ('VIEW_SALARY', 'Can view salary data'),
                                                ('CENTER_CREATE', 'Can create center')
ON DUPLICATE KEY UPDATE
    description = VALUES(description);