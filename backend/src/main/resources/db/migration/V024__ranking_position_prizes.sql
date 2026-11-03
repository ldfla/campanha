-- P05A-09: prêmio por posição do ranking, parametrizado no backend, e entrega vinculada ao snapshot lacrado.

ALTER TABLE campaign_prizes
    ADD COLUMN ranking_scope VARCHAR(20) NULL,
    ADD COLUMN ranking_position INT NULL,
    ADD CONSTRAINT ck_campaign_prizes_ranking
        CHECK ((ranking_scope IS NULL AND ranking_position IS NULL)
            OR (ranking_scope IN ('PROMOTER', 'PARTICIPANT') AND ranking_position BETWEEN 1 AND 1000)),
    ADD CONSTRAINT uk_campaign_prizes_ranking_position UNIQUE (campaign_id, ranking_scope, ranking_position);

ALTER TABLE award_deliveries
    MODIFY COLUMN outcome_id BIGINT NULL,
    ADD COLUMN ranking_snapshot_id BIGINT NULL,
    ADD COLUMN ranking_position INT NULL,
    ADD COLUMN prize_id BIGINT NULL,
    ADD CONSTRAINT fk_delivery_ranking_snapshot FOREIGN KEY (ranking_snapshot_id) REFERENCES ranking_snapshots(id),
    ADD CONSTRAINT fk_delivery_prize FOREIGN KEY (prize_id) REFERENCES campaign_prizes(id),
    ADD CONSTRAINT uk_delivery_ranking_position UNIQUE (ranking_snapshot_id, ranking_position),
    ADD CONSTRAINT ck_delivery_source
        CHECK ((outcome_id IS NOT NULL AND ranking_snapshot_id IS NULL AND ranking_position IS NULL)
            OR (outcome_id IS NULL AND ranking_snapshot_id IS NOT NULL AND ranking_position IS NOT NULL
                AND prize_id IS NOT NULL));
