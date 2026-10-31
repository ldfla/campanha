-- V008: verification state and MFA enrollment. Secrets are never stored in plaintext.
ALTER TABLE users ADD COLUMN phone_verified_at TIMESTAMP NULL;

CREATE TABLE mfa_enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    secret_ciphertext VARBINARY(512) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    verified_at TIMESTAMP NULL,
    UNIQUE KEY uk_mfa_user (user_id),
    CONSTRAINT fk_mfa_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
