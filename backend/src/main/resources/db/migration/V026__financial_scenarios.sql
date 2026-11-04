CREATE TABLE financial_scenarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id CHAR(36) NOT NULL,
    campaign_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    price_cents BIGINT NOT NULL,
    unit_cost_cents BIGINT NOT NULL,
    capacity INT NOT NULL,
    quantity INT NOT NULL,
    fee_basis_points INT NOT NULL,
    tax_basis_points INT NOT NULL,
    target_profit_cents BIGINT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_financial_scenario_public_id (public_id),
    INDEX idx_financial_scenario_campaign (campaign_id, id),
    CONSTRAINT fk_financial_scenario_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT chk_financial_scenario_values CHECK (price_cents >= 0 AND unit_cost_cents >= 0 AND target_profit_cents >= 0 AND capacity BETWEEN 0 AND 100000 AND quantity BETWEEN 0 AND capacity AND fee_basis_points >= 0 AND tax_basis_points >= 0 AND fee_basis_points + tax_basis_points <= 10000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE financial_scenario_costs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scenario_id BIGINT NOT NULL,
    category VARCHAR(30) NOT NULL,
    cents BIGINT NOT NULL,
    INDEX idx_financial_scenario_cost_scenario (scenario_id),
    CONSTRAINT fk_financial_scenario_cost FOREIGN KEY (scenario_id) REFERENCES financial_scenarios(id),
    CONSTRAINT chk_financial_scenario_cost_cents CHECK (cents >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
