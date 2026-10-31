-- P09: última linha de defesa para lançamentos financeiros idempotentes.
ALTER TABLE financial_ledger ADD CONSTRAINT uk_financial_ledger_source_type UNIQUE (source_ref, entry_type);
