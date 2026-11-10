ALTER TABLE financial_expenses ADD UNIQUE KEY uk_financial_expense_id_campaign (id, campaign_id);

CREATE TABLE financial_expense_reversals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    public_id CHAR(36) NOT NULL,
    campaign_id BIGINT NOT NULL,
    expense_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    idempotency_key CHAR(36) NOT NULL,
    reason VARCHAR(200) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_financial_expense_reversal_public (public_id),
    UNIQUE KEY uk_financial_expense_reversal_expense (expense_id),
    UNIQUE KEY uk_financial_expense_reversal_key (campaign_id, idempotency_key),
    CONSTRAINT fk_financial_expense_reversal_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id),
    CONSTRAINT fk_financial_expense_reversal_expense FOREIGN KEY (expense_id, campaign_id) REFERENCES financial_expenses(id, campaign_id),
    CONSTRAINT fk_financial_expense_reversal_actor FOREIGN KEY (actor_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
