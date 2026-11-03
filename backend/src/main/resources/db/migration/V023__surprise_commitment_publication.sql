-- V023: prova de compromisso publicado e exigencia de surpresa derivada do regulamento.
-- Fecha o SEC-0003 (abertura sem prova previa de compromisso):
--   * campaigns.surprise_commitment_published_at: so e preenchido quando o compromisso
--     criptografico existe e foi publicado no endpoint publico (GET /surprises/.../commitment).
--   * campaign_regulations.surprise_required: o requisito de surpresa passa a ser declarado no
--     regulamento (fonte de verdade) e refletido na campanha, nao apenas por um flag manual.
ALTER TABLE campaigns
    ADD COLUMN surprise_commitment_published_at TIMESTAMP(6) NULL AFTER surprise_enabled;

ALTER TABLE campaign_regulations
    ADD COLUMN surprise_required BOOLEAN NOT NULL DEFAULT FALSE AFTER document_uri;
