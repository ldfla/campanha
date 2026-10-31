ALTER TABLE campaigns
    ADD COLUMN max_entries_per_user INT NOT NULL DEFAULT 100,
    ADD CONSTRAINT chk_campaign_max_entries_per_user CHECK (max_entries_per_user BETWEEN 1 AND 100000);
