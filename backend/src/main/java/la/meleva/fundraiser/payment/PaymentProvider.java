package la.meleva.fundraiser.payment;

/** Adaptador de PSP; valores monetários são sempre centavos BRL. */
public interface PaymentProvider {
    String name();
    Charge createCharge(ChargeRequest request);
    Charge getCharge(String chargeId);
    RefundResult refund(String chargeId, long amountCents, String reason);
    WebhookEvent verifyWebhook(String payload, String signature);

    record ChargeRequest(String txid, long amountCents, String description) { }
    record Charge(String id, String txid, long amountCents, ChargeState state) { }
    record RefundResult(String id, long amountCents) { }
    record WebhookEvent(String eventId, String chargeId, String txid, ChargeState state) { }
    enum ChargeState { PENDING, PAID, EXPIRED, FAILED, REFUNDED }
}
