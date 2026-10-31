CREATE TABLE beneficiary_entities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id CHAR(36) NOT NULL,
    campaign_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_beneficiary_public_id (public_id),
    INDEX idx_beneficiary_campaign_active (campaign_id, active),
    CONSTRAINT fk_beneficiary_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE beneficiary_selections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    beneficiary_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_beneficiary_selection_request (request_id),
    INDEX idx_beneficiary_selection_entity (beneficiary_id),
    CONSTRAINT fk_beneficiary_selection_request FOREIGN KEY (request_id) REFERENCES participation_requests(id),
    CONSTRAINT fk_beneficiary_selection_entity FOREIGN KEY (beneficiary_id) REFERENCES beneficiary_entities(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE campaign_purchase_offers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id CHAR(36) NOT NULL,
    campaign_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    total_cents BIGINT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_purchase_offer_public_id (public_id),
    INDEX idx_purchase_offer_campaign_active (campaign_id, active),
    CONSTRAINT chk_purchase_offer_quantity CHECK (quantity BETWEEN 1 AND 100),
    CONSTRAINT chk_purchase_offer_total CHECK (total_cents > 0),
    CONSTRAINT fk_purchase_offer_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reservation_payment_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    order_id BIGINT NOT NULL,
    offer_id BIGINT NOT NULL,
    total_cents BIGINT NOT NULL,
    referral_code VARCHAR(20) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_reservation_payment_request (request_id),
    UNIQUE KEY uk_reservation_payment_order (order_id),
    CONSTRAINT chk_reservation_payment_total CHECK (total_cents > 0),
    CONSTRAINT fk_reservation_payment_request FOREIGN KEY (request_id) REFERENCES participation_requests(id),
    CONSTRAINT fk_reservation_payment_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_reservation_payment_offer FOREIGN KEY (offer_id) REFERENCES campaign_purchase_offers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
