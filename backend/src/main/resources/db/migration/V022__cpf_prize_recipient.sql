-- V022: CPF do contemplado (apenas para recebimento do prêmio) — cifrado em repouso + índice HMAC
ALTER TABLE users
    ADD COLUMN cpf_encrypted VARCHAR(512) NULL AFTER email_normalized,
    ADD COLUMN cpf_hmac CHAR(64) NULL AFTER cpf_encrypted,
    ADD UNIQUE KEY uk_user_cpf_hmac (cpf_hmac);
