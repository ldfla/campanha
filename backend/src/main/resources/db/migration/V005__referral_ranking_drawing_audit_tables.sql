-- V005: Referrals, rankings, drawings, awards and audit tables

-- Referrals
CREATE TABLE referral_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    promoter_user_id BIGINT NOT NULL,
    code VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_referral_code (code),
    INDEX idx_referral_campaign (campaign_id),
    INDEX idx_referral_promoter (promoter_user_id),
    CONSTRAINT fk_referral_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_referral_user FOREIGN KEY (promoter_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE referral_clicks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    referral_link_id BIGINT NOT NULL,
    anonymous_id_hash CHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_click_link (referral_link_id),
    CONSTRAINT fk_click_link FOREIGN KEY (referral_link_id) REFERENCES referral_links(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE referral_attributions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    referral_link_id BIGINT NOT NULL,
    referred_user_id BIGINT NOT NULL,
    entry_id BIGINT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    attributed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_attribution_entry (campaign_id, entry_id),
    INDEX idx_attribution_link (referral_link_id),
    INDEX idx_attribution_referred (referred_user_id),
    CONSTRAINT fk_attribution_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_attribution_link FOREIGN KEY (referral_link_id) REFERENCES referral_links(id),
    CONSTRAINT fk_attribution_user FOREIGN KEY (referred_user_id) REFERENCES users(id),
    CONSTRAINT fk_attribution_entry FOREIGN KEY (entry_id) REFERENCES participation_entries(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Rankings
CREATE TABLE ranking_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    rule_version INT NOT NULL,
    metric VARCHAR(50) NOT NULL,
    tie_breaker VARCHAR(100) NOT NULL,
    legally_approved BOOLEAN NOT NULL DEFAULT FALSE,
    effective_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_ranking_rule_version (campaign_id, rule_version),
    CONSTRAINT fk_ranking_rule_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ranking_snapshots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    ranking_rule_id BIGINT NOT NULL,
    cutoff_at TIMESTAMP NOT NULL,
    snapshot_hash CHAR(64) NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_snapshot_campaign (campaign_id),
    CONSTRAINT fk_snapshot_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_snapshot_rule FOREIGN KEY (ranking_rule_id) REFERENCES ranking_rules(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE ranking_positions (
    snapshot_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    position INT NOT NULL,
    score BIGINT NOT NULL DEFAULT 0,
    tiebreak_value VARCHAR(100),
    PRIMARY KEY (snapshot_id, user_id),
    CONSTRAINT fk_position_snapshot FOREIGN KEY (snapshot_id) REFERENCES ranking_snapshots(id),
    CONSTRAINT fk_position_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Drawing
CREATE TABLE draw_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    version INT NOT NULL,
    source_kind VARCHAR(50) NOT NULL,
    formula_json JSON NOT NULL,
    fallback_json JSON,
    approved_regulation_sha256 CHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_draw_rule_version (campaign_id, version),
    CONSTRAINT fk_draw_rule_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE federal_results (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contest_identifier VARCHAR(50) NOT NULL,
    drawn_at TIMESTAMP NULL,
    source_url VARCHAR(1024),
    source_document_hash CHAR(64),
    result_payload JSON NOT NULL,
    verified_by_two BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_federal_contest (contest_identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE draws (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id CHAR(36) NOT NULL,
    campaign_id BIGINT NOT NULL,
    draw_rule_id BIGINT NOT NULL,
    federal_result_id BIGINT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    frozen_at TIMESTAMP NULL,
    entries_merkle_root_or_hash CHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_draw_public_id (public_id),
    INDEX idx_draw_campaign (campaign_id),
    CONSTRAINT fk_draw_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_draw_rule FOREIGN KEY (draw_rule_id) REFERENCES draw_rules(id),
    CONSTRAINT fk_draw_federal FOREIGN KEY (federal_result_id) REFERENCES federal_results(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE draw_outcomes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    draw_id BIGINT NOT NULL,
    prize_id BIGINT NOT NULL,
    winning_number INT NOT NULL,
    entry_id BIGINT NULL,
    decision_code VARCHAR(50) NOT NULL,
    evidence_hash CHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_outcome_draw (draw_id),
    CONSTRAINT chk_outcome_number CHECK (winning_number BETWEEN 0 AND 99999),
    CONSTRAINT fk_outcome_draw FOREIGN KEY (draw_id) REFERENCES draws(id),
    CONSTRAINT fk_outcome_prize FOREIGN KEY (prize_id) REFERENCES campaign_prizes(id),
    CONSTRAINT fk_outcome_entry FOREIGN KEY (entry_id) REFERENCES participation_entries(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE surprise_commitments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    commitment_sha256 CHAR(64) NOT NULL,
    encrypted_secret_object_key VARCHAR(512),
    committed_at TIMESTAMP NOT NULL,
    revealed_at TIMESTAMP NULL,
    rule_version INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_surprise_campaign (campaign_id),
    CONSTRAINT fk_surprise_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Awards
CREATE TABLE award_deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    outcome_id BIGINT NOT NULL,
    beneficiary_user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    delivery_evidence_uri VARCHAR(1024),
    delivered_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_delivery_outcome (outcome_id),
    INDEX idx_delivery_beneficiary (beneficiary_user_id),
    CONSTRAINT fk_delivery_outcome FOREIGN KEY (outcome_id) REFERENCES draw_outcomes(id),
    CONSTRAINT fk_delivery_user FOREIGN KEY (beneficiary_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Audit
CREATE TABLE audit_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_id BIGINT NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    before_hash CHAR(64),
    after_hash CHAR(64),
    correlation_id CHAR(36),
    chain_prev_hash CHAR(64),
    event_hash CHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_actor (actor_id),
    INDEX idx_audit_correlation (correlation_id),
    INDEX idx_audit_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
