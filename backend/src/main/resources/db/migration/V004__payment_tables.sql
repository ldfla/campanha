-- V004: Payment, orders, ledger and reconciliation tables

CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id CHAR(36) NOT NULL,
    campaign_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'CREATED',
    total_cents BIGINT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    mode VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP NULL,
    idempotency_key CHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_order_public_id (public_id),
    UNIQUE KEY uk_order_idemp (campaign_id, user_id, idempotency_key),
    INDEX idx_order_campaign_user (campaign_id, user_id),
    INDEX idx_order_status (status),
    CONSTRAINT chk_order_total CHECK (total_cents >= 0),
    CONSTRAINT fk_order_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_amount_cents BIGINT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    reference_id BIGINT NULL,
    CONSTRAINT chk_item_amount CHECK (unit_amount_cents >= 0),
    CONSTRAINT fk_item_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,
    provider_charge_id VARCHAR(255),
    txid VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    amount_cents BIGINT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    confirmed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_payment_provider_charge (provider, provider_charge_id),
    UNIQUE KEY uk_payment_txid (provider, txid),
    INDEX idx_payment_order (order_id),
    INDEX idx_payment_status (status),
    CONSTRAINT chk_payment_amount CHECK (amount_cents >= 0),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payment_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    provider_event_id VARCHAR(255) NOT NULL,
    end_to_end_id VARCHAR(255),
    event_type VARCHAR(50) NOT NULL,
    payload_sha256 CHAR(64) NOT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL,
    UNIQUE KEY uk_event_provider (payment_id, provider_event_id),
    INDEX idx_event_payment (payment_id),
    CONSTRAINT fk_event_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE refunds (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    provider_refund_id VARCHAR(255),
    amount_cents BIGINT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reason TEXT,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL,
    CONSTRAINT chk_refund_amount CHECK (amount_cents > 0),
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE financial_ledger (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    source_ref VARCHAR(100) NOT NULL,
    entry_type VARCHAR(50) NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    posted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_ledger_campaign (campaign_id),
    INDEX idx_ledger_type (entry_type),
    CONSTRAINT fk_ledger_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reconciliation_runs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP NULL,
    discrepancies INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'RUNNING'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
