-- V011: Ranking escopo, snapshots auditáveis e índices de posição

ALTER TABLE ranking_rules
    ADD COLUMN scope VARCHAR(30) NOT NULL DEFAULT 'PROMOTER' AFTER rule_version,
    DROP INDEX uk_ranking_rule_version,
    ADD UNIQUE KEY uk_ranking_rule_scope_version (campaign_id, scope, rule_version);

ALTER TABLE ranking_snapshots
    ADD COLUMN public_id CHAR(36) NOT NULL AFTER id,
    ADD COLUMN scope VARCHAR(30) NOT NULL DEFAULT 'PROMOTER' AFTER ranking_rule_id,
    ADD COLUMN payload_json LONGTEXT NOT NULL AFTER snapshot_hash,
    ADD COLUMN entry_count INT NOT NULL DEFAULT 0 AFTER payload_json,
    ADD UNIQUE KEY uk_snapshot_public_id (public_id),
    ADD UNIQUE KEY uk_snapshot_rule_cutoff (ranking_rule_id, cutoff_at),
    ADD INDEX idx_snapshot_campaign_scope (campaign_id, scope, generated_at);

ALTER TABLE ranking_positions
    ADD INDEX idx_position_snapshot_order (snapshot_id, position);

-- Precisão de microssegundos para que limites (cutoff) e eventos não sejam arredondados
-- para o segundo seguinte pelo MySQL, preservando a ordem e a reprodutibilidade.
ALTER TABLE participation_entries
    MODIFY COLUMN issued_at TIMESTAMP(6) NULL,
    MODIFY COLUMN validated_at TIMESTAMP(6) NULL;

ALTER TABLE referral_attributions
    MODIFY COLUMN attributed_at TIMESTAMP(6) NULL;

ALTER TABLE ranking_rules
    MODIFY COLUMN effective_at TIMESTAMP(6) NOT NULL;

ALTER TABLE ranking_snapshots
    MODIFY COLUMN cutoff_at TIMESTAMP(6) NOT NULL,
    MODIFY COLUMN generated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6);
