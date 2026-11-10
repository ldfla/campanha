-- Os chamados anteriores permanecem sem organização até o próprio autor encaminhá-los.
ALTER TABLE support_requests
    ADD COLUMN organization_id BIGINT NULL,
    ADD INDEX idx_support_organization_status (organization_id, status, id),
    ADD CONSTRAINT fk_support_organization FOREIGN KEY (organization_id) REFERENCES organizations(id);
