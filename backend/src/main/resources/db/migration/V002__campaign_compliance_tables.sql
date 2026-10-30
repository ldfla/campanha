-- V002: Campaign management and compliance tables

CREATE TABLE campaigns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id CHAR(36) NOT NULL,
    organization_id BIGINT NOT NULL,
    slug VARCHAR(100) NOT NULL,
    mode VARCHAR(30) NOT NULL DEFAULT 'DEMO',
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    title VARCHAR(255) NOT NULL,
    description TEXT,
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    timezone VARCHAR(50) NOT NULL DEFAULT 'America/Sao_Paulo',
    series_count INT NOT NULL DEFAULT 1,
    number_min INT NOT NULL DEFAULT 0,
    number_max INT NOT NULL DEFAULT 99999,
    current_regulation_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_campaign_public_id (public_id),
    UNIQUE KEY uk_campaign_slug (slug),
    INDEX idx_campaign_org (organization_id),
    INDEX idx_campaign_status (status),
    INDEX idx_campaign_mode_status (mode, status),
    CONSTRAINT chk_campaign_number_range CHECK (number_min >= 0 AND number_max <= 99999 AND number_min <= number_max),
    CONSTRAINT fk_campaign_org FOREIGN KEY (organization_id) REFERENCES organizations(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE campaign_regulations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    version INT NOT NULL,
    document_sha256 CHAR(64) NOT NULL,
    document_uri VARCHAR(1024),
    approved_at TIMESTAMP NULL,
    approval_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_regulation_version (campaign_id, version),
    CONSTRAINT fk_regulation_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE campaigns
    ADD CONSTRAINT fk_campaign_regulation FOREIGN KEY (current_regulation_id) REFERENCES campaign_regulations(id);

CREATE TABLE campaign_authorizations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    issuer VARCHAR(255),
    certificate_no VARCHAR(100),
    valid_from TIMESTAMP NULL,
    valid_to TIMESTAMP NULL,
    approval_scope_json JSON,
    document_uri VARCHAR(1024),
    verified_at TIMESTAMP NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_auth_campaign (campaign_id),
    CONSTRAINT fk_auth_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE compliance_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    organization_id BIGINT NULL,
    campaign_id BIGINT NULL,
    kind VARCHAR(50) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    sha256 CHAR(64) NOT NULL,
    classification VARCHAR(30) NOT NULL DEFAULT 'PRIVATE',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_doc_org (organization_id),
    INDEX idx_doc_campaign (campaign_id),
    CONSTRAINT fk_doc_org FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_doc_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE approval_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    gate_type VARCHAR(30) NOT NULL,
    requested_by BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL,
    INDEX idx_approval_campaign (campaign_id),
    INDEX idx_approval_status (status),
    CONSTRAINT fk_approval_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_approval_requester FOREIGN KEY (requested_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE approval_decisions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id BIGINT NOT NULL,
    reviewer_user_id BIGINT NOT NULL,
    decision VARCHAR(30) NOT NULL,
    reason TEXT,
    decided_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_decision_request (request_id),
    CONSTRAINT fk_decision_request FOREIGN KEY (request_id) REFERENCES approval_requests(id),
    CONSTRAINT fk_decision_reviewer FOREIGN KEY (reviewer_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE campaign_prizes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    value_cents BIGINT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    quantity INT NOT NULL DEFAULT 1,
    eligibility_rule_id BIGINT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_prize_campaign (campaign_id),
    CONSTRAINT chk_prize_value CHECK (value_cents >= 0),
    CONSTRAINT fk_prize_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE campaign_assets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    alt_text VARCHAR(500),
    license_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_asset_campaign (campaign_id),
    CONSTRAINT fk_asset_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
