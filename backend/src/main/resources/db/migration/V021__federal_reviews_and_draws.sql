-- V021: apuração (P12) — revisão em dupla do resultado oficial e carimbo de publicação
ALTER TABLE federal_results
    ADD COLUMN source_kind VARCHAR(50) NULL AFTER contest_identifier;

CREATE TABLE federal_result_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    federal_result_id BIGINT NOT NULL,
    reviewer_user_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_federal_review_user (federal_result_id, reviewer_user_id),
    CONSTRAINT fk_federal_review_result FOREIGN KEY (federal_result_id) REFERENCES federal_results(id),
    CONSTRAINT fk_federal_review_user FOREIGN KEY (reviewer_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE draws
    ADD COLUMN published_at TIMESTAMP(6) NULL AFTER frozen_at,
    ADD INDEX idx_draw_status (campaign_id, status);
