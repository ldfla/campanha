-- P15: canal de suporte interno (sem dados pessoais no conteúdo além do autor)
CREATE TABLE support_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id CHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    subject VARCHAR(150) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_support_public_id (public_id),
    INDEX idx_support_status (status),
    CONSTRAINT fk_support_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
