package la.meleva.fundraiser.campaign;

/** Publicado na transação da transição; ouvintes síncronos gravam efeitos atômicos (ex.: notificações no outbox). */
public record CampaignStatusChangedEvent(Long campaignId, CampaignStatus from, CampaignStatus to) { }
