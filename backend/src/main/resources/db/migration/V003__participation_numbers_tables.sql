-- V003: Participation, numbers and eligibility tables

CREATE TABLE campaign_numbers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    series_code CHAR(1) NOT NULL DEFAULT 'A',
    number_value INT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    entry_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_number (campaign_id, series_code, number_value),
    INDEX idx_number_status (campaign_id, status),
    INDEX idx_number_entry (entry_id),
    CONSTRAINT chk_number_value CHECK (number_value BETWEEN 0 AND 99999),
    CONSTRAINT fk_number_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE participation_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id CHAR(36) NOT NULL,
    campaign_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    idempotency_key CHAR(36) NOT NULL,
    requested_count INT NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_request_idemp (campaign_id, user_id, idempotency_key),
    UNIQUE KEY uk_request_public_id (public_id),
    INDEX idx_request_campaign_user (campaign_id, user_id),
    CONSTRAINT fk_request_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_request_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE participation_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id CHAR(36) NOT NULL,
    campaign_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    request_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    eligibility_snapshot_hash CHAR(64),
    issued_at TIMESTAMP NULL,
    validated_at TIMESTAMP NULL,
    source VARCHAR(50) NOT NULL DEFAULT 'DIRECT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_entry_public_id (public_id),
    INDEX idx_entry_campaign_user (campaign_id, user_id),
    INDEX idx_entry_campaign_status (campaign_id, status),
    CONSTRAINT fk_entry_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_entry_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_entry_request FOREIGN KEY (request_id) REFERENCES participation_requests(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE campaign_numbers
    ADD CONSTRAINT fk_number_entry FOREIGN KEY (entry_id) REFERENCES participation_entries(id);

CREATE TABLE entry_number_assignments (
    entry_id BIGINT NOT NULL,
    campaign_number_id BIGINT NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (entry_id, campaign_number_id),
    UNIQUE KEY uk_assignment_number (campaign_number_id),
    CONSTRAINT fk_assignment_entry FOREIGN KEY (entry_id) REFERENCES participation_entries(id),
    CONSTRAINT fk_assignment_number FOREIGN KEY (campaign_number_id) REFERENCES campaign_numbers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE eligibility_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_id BIGINT NOT NULL,
    rule_version INT NOT NULL,
    result VARCHAR(30) NOT NULL,
    reason_code VARCHAR(100),
    checked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_eligibility_entry (entry_id),
    CONSTRAINT fk_eligibility_entry FOREIGN KEY (entry_id) REFERENCES participation_entries(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE reservation_leases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    number_id BIGINT NOT NULL,
    request_id BIGINT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    version INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_lease_campaign (campaign_id),
    INDEX idx_lease_status_expires (status, expires_at),
    CONSTRAINT fk_lease_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_lease_number FOREIGN KEY (number_id) REFERENCES campaign_numbers(id),
    CONSTRAINT fk_lease_request FOREIGN KEY (request_id) REFERENCES participation_requests(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
