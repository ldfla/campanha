package la.meleva.fundraiser.payment;

/** Publicado na mesma transação em que o estorno integral é registrado; ouvintes síncronos são atômicos com ele. */
public record PaymentRefundedEvent(Long orderId, Long campaignId, Long paymentId) { }
