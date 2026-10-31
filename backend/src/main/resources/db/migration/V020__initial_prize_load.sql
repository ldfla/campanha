-- V020: modelo de carga inicial de prêmios nos números
--
-- campaign_numbers
--   Colunas de INICIALIZAÇÃO (cobertas pelo hash de auditoria, imutáveis após a carga):
--     prize_cents, prize_defined_at
--   Colunas de OPERAÇÃO (atualizáveis, fora do hash e sem PII):
--     purchased_at, purchase_modality, correlation_id, charity_institution, referrer_phone
--
-- campaign_number_buyers (1:1 com o número) — PII do comprador isolada em tabela própria,
--   com pseudônimo para exibição/auditoria sem expor identidade integral.

ALTER TABLE campaign_numbers
    ADD COLUMN prize_cents BIGINT NULL AFTER status,
    ADD COLUMN prize_defined_at TIMESTAMP(6) NULL AFTER prize_cents,
    ADD COLUMN purchased_at TIMESTAMP(6) NULL AFTER prize_defined_at,
    ADD COLUMN purchase_modality VARCHAR(30) NULL AFTER purchased_at,
    ADD COLUMN correlation_id CHAR(36) NULL AFTER purchase_modality,
    ADD COLUMN charity_institution VARCHAR(150) NULL AFTER correlation_id,
    ADD COLUMN referrer_phone VARCHAR(20) NULL AFTER charity_institution,
    ADD CONSTRAINT chk_number_prize CHECK (prize_cents IS NULL OR prize_cents > 0),
    ADD CONSTRAINT chk_number_prize_pair CHECK ((prize_cents IS NULL) = (prize_defined_at IS NULL)),
    ADD CONSTRAINT chk_number_charity_sold CHECK (
        (status IN ('ASSIGNED', 'FROZEN') AND charity_institution IS NOT NULL) OR (status NOT IN ('ASSIGNED', 'FROZEN'))
    ),
    ADD INDEX idx_number_prize (campaign_id, prize_cents);

CREATE TABLE campaign_number_buyers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    number_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    buyer_user_id BIGINT NOT NULL,
    buyer_phone VARCHAR(20) NOT NULL,
    buyer_first_name VARCHAR(100) NOT NULL,
    buyer_surname VARCHAR(100) NOT NULL,
    buyer_pseudonym VARCHAR(40) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_number_buyer (number_id),
    INDEX idx_number_buyer_campaign (campaign_id),
    INDEX idx_number_buyer_user (buyer_user_id),
    CONSTRAINT chk_number_buyer_phone_e164 CHECK (buyer_phone LIKE '+%'),
    CONSTRAINT fk_number_buyer_number FOREIGN KEY (number_id) REFERENCES campaign_numbers(id),
    CONSTRAINT fk_number_buyer_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_number_buyer_user FOREIGN KEY (buyer_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE campaign_prize_loads (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    number_min INT NOT NULL,
    number_max INT NOT NULL,
    tier1_count INT NOT NULL,
    tier2_count INT NOT NULL,
    tier1_cents BIGINT NOT NULL,
    tier2_cents BIGINT NOT NULL,
    defined_at TIMESTAMP(6) NOT NULL,
    file_sha256 CHAR(64) NOT NULL,
    generated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_prize_load_campaign (campaign_id),
    CONSTRAINT chk_prize_load_range CHECK (number_min >= 0 AND number_max <= 99999 AND number_max > number_min),
    CONSTRAINT chk_prize_load_tiers CHECK (tier1_count >= 0 AND tier2_count >= 0 AND (tier1_count + tier2_count) > 0),
    CONSTRAINT chk_prize_load_cents CHECK (tier1_cents > 0 AND tier2_cents > 0),
    CONSTRAINT fk_prize_load_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
