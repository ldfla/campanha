# Modelo de Domínio — Plataforma Ação entre Amigos

## Domínios e Responsabilidades

### Identity
Usuários, sessões, WebAuthn/passkeys, RBAC por objeto/campanha/organização.

- **Entidades:** User, UserIdentity, Membership, WebAuthnCredential, Session, ConsentReceipt, PrivacyRequest
- **Papéis:** VISITOR, PARTICIPANT, PROMOTER, SUPPORT, CAMPAIGN_MANAGER, FINANCE, COMPLIANCE_REVIEWER, DRAW_AUDITOR, SECURITY_ADMIN, SYSTEM_ADMIN
- **Invariantes:** CPF criptografado com HMAC para busca; MFA obrigatório para admins

### Campaign
Ciclo de vida da campanha, prêmios, assets e máquina de estados.

- **Entidades:** Campaign, CampaignRegulation, CampaignPrize, CampaignAsset
- **Estados:** DRAFT → LEGAL_REVIEW → AUTHORIZED → SCHEDULED → OPEN → LOCKED → DRAW_PENDING → DRAWN → AWARDS_PENDING → CLOSED
- **Modos:** DEMO, COMMERCIAL_PROMOTION, PHILANTHROPIC_AUTHORIZED
- **Invariantes:** Prêmio em dinheiro proibido em COMMERCIAL_PROMOTION; publicação exige todos os gates

### Compliance
Autorizações, aprovações, documentos e gates de legalidade.

- **Entidades:** CampaignAuthorization, ComplianceDocument, ApprovalRequest, ApprovalDecision
- **Gates:** G_LEGAL, G_PSP, G_FINANCE, G_SECURITY, G_OPERATIONS, G_LAUNCH
- **Invariantes:** Autoaprovação proibida; dois revisores independentes para autorização

### Participation
Solicitações, elegibilidade, atribuição de números.

- **Entidades:** ParticipationRequest, ParticipationEntry, CampaignNumber, EntryNumberAssignment, EligibilityEvent, ReservationLease
- **Estados entrada:** REQUESTED → ELIGIBILITY_PENDING → ELIGIBLE → ASSIGNED → FROZEN
- **Estados número:** AVAILABLE → RESERVED → ASSIGNED → FROZEN
- **Invariantes:** Chave única (campaign_id, number_value); sem duplicidade mesmo sob concorrência; 00000 a 99999

### Payment
Pedidos, cobranças, webhooks, ledger e reconciliação.

- **Entidades:** Order, OrderItem, Payment, PaymentEvent, Refund, FinancialLedger, ReconciliationRun
- **Estados pedido:** CREATED → AWAITING_PAYMENT → CONFIRMED → RECONCILED
- **Invariantes:** Eventos idempotentes; ledger append-only; cobrança bloqueada em COMMERCIAL_PROMOTION

### Referral
Links de indicação, cliques e atribuição.

- **Entidades:** ReferralLink, ReferralClick, ReferralAttribution
- **Invariantes:** Sem autoindicação; uma entrada atribuída a no máximo um referral

### Ranking
Regras, snapshots e posições.

- **Entidades:** RankingRule, RankingSnapshot, RankingPosition
- **Invariantes:** Desempate determinístico fixado antes da abertura; snapshot congelado antes da apuração

### Drawing
Sorteio, resultados oficiais, fórmula e prêmios-surpresa.

- **Entidades:** DrawRule, Draw, FederalResult, DrawOutcome, SurpriseCommitment
- **Invariantes:** Resultado oficial com dupla verificação; fórmula determinística; compromisso SHA-256 publicado antes da abertura

### Award
Entrega de prêmios e evidências.

- **Entidades:** AwardDelivery
- **Estados:** PENDING_VERIFICATION → VALIDATED → SCHEDULED → DELIVERED | DISPUTED

### Audit
Eventos de auditoria com encadeamento de hashes.

- **Entidades:** AuditEvent
- **Invariantes:** Append-only; chain_prev_hash para detecção de adulteração

### Notification
Templates, outbox de envio e canais.

- **Integração:** Mailpit em DEMO, Twilio/WhatsApp em produção (futuro)

## Números — Especificação

- Intervalo: 00000 a 99999 (100.000 posições)
- Apresentação: sempre com 5 dígitos e zeros à esquerda
- Chave única: `(campaign_id, series_code, number_value)`
- MVP: série única (series_code = 'A')
