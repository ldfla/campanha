-- Combos de 5 a 5.000 números; compra mínima de 5 números por R$ 5,00 para diluir a taxa mínima por Pix do PSP.
-- Ofertas antigas abaixo do mínimo saem de venda, mas continuam gravadas porque pedidos já as referenciam.
UPDATE campaign_purchase_offers SET active = FALSE WHERE quantity < 5 OR total_cents < 500;

ALTER TABLE campaign_purchase_offers
    DROP CHECK chk_purchase_offer_quantity,
    ADD CONSTRAINT chk_purchase_offer_quantity CHECK (quantity BETWEEN 1 AND 5000),
    ADD CONSTRAINT chk_purchase_offer_active_minimum CHECK (active = FALSE OR (quantity >= 5 AND total_cents >= 500));

-- O limite antigo de 100 números por participante impediria os combos de 1.000 e 5.000.
ALTER TABLE campaigns ALTER COLUMN max_entries_per_user SET DEFAULT 10000;
UPDATE campaigns SET max_entries_per_user = 10000 WHERE max_entries_per_user = 100;
