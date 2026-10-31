ALTER TABLE surprise_commitments ADD COLUMN encrypted_secret TEXT NULL;
ALTER TABLE surprise_commitments ADD COLUMN proof_number_min INT NULL, ADD COLUMN proof_number_max INT NULL, ADD COLUMN proof_regulation_version INT NULL;
ALTER TABLE surprise_commitments ADD CONSTRAINT uk_surprise_campaign UNIQUE (campaign_id);
