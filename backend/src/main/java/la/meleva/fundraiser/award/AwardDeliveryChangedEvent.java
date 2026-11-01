package la.meleva.fundraiser.award;

/** Publicado na transação de cada mudança de estado da entrega. */
public record AwardDeliveryChangedEvent(Long deliveryId, AwardDeliveryStatus status) { }
