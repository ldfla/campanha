-- Autorizações independentes aplicáveis apenas ao fluxo de demonstração.
ALTER TABLE surprise_commitments ADD COLUMN created_by BIGINT NULL;
ALTER TABLE surprise_commitments ADD CONSTRAINT fk_surprise_creator FOREIGN KEY (created_by) REFERENCES users(id);

CREATE TABLE surprise_reveal_approvals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    commitment_id BIGINT NOT NULL,
    approver_user_id BIGINT NOT NULL,
    approval_role VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_surprise_approval_user (commitment_id, approver_user_id),
    UNIQUE KEY uk_surprise_approval_role (commitment_id, approval_role),
    CONSTRAINT fk_surprise_approval_commitment FOREIGN KEY (commitment_id) REFERENCES surprise_commitments(id),
    CONSTRAINT fk_surprise_approval_user FOREIGN KEY (approver_user_id) REFERENCES users(id),
    CONSTRAINT chk_surprise_approval_role CHECK (approval_role IN ('DRAW_AUDITOR', 'COMPLIANCE_REVIEWER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
