-- P13: evidência de entrega, contestação e unicidade por resultado
ALTER TABLE award_deliveries
    ADD COLUMN delivery_evidence_sha256 CHAR(64) NULL AFTER delivery_evidence_uri,
    ADD COLUMN disputed_at TIMESTAMP(6) NULL AFTER delivered_at,
    ADD COLUMN dispute_reason VARCHAR(500) NULL AFTER disputed_at;

-- Um resultado de apuração corresponde a no máximo uma entrega.
ALTER TABLE award_deliveries
    ADD CONSTRAINT uk_delivery_outcome UNIQUE (outcome_id);
