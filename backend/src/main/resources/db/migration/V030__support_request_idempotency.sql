-- Chamados anteriores e clientes legados sem chave permanecem válidos.
ALTER TABLE support_requests
    ADD COLUMN idempotency_key CHAR(36) NULL,
    ADD COLUMN idempotency_fingerprint CHAR(64) NULL,
    ADD UNIQUE KEY uk_support_author_idempotency (user_id, idempotency_key),
    ADD CONSTRAINT chk_support_idempotency_pair CHECK (
        (idempotency_key IS NULL AND idempotency_fingerprint IS NULL)
        OR (idempotency_key IS NOT NULL AND idempotency_fingerprint IS NOT NULL)
    );
