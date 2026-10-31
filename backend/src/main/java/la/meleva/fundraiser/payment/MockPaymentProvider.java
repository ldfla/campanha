package la.meleva.fundraiser.payment;

import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** PSP determinístico local. Não efetua operação financeira externa. */
@Component @Primary
public class MockPaymentProvider implements PaymentProvider {
    private static final String SIGNING_KEY = "mock-payment-local-only";
    private final Map<String, Charge> charges = new ConcurrentHashMap<>();
    @Override public String name(){ return "MOCK"; }
    @Override public Charge createCharge(ChargeRequest request) {
        if(request.amountCents() <= 0) throw ErrorCode.PAYMENT_PROVIDER_ERROR.toException();
        var charge = new Charge("mock_" + UUID.randomUUID(), request.txid(), request.amountCents(), ChargeState.PENDING);
        charges.put(charge.id(), charge); return charge;
    }
    @Override public Charge getCharge(String chargeId) { return charges.getOrDefault(chargeId, new Charge(chargeId, "", 0, ChargeState.FAILED)); }
    @Override public RefundResult refund(String chargeId,long amountCents,String reason) {
        var charge=getCharge(chargeId); if(charge.state()!=ChargeState.PAID || amountCents!=charge.amountCents()) throw ErrorCode.PAYMENT_PROVIDER_ERROR.toException();
        charges.put(chargeId,new Charge(charge.id(),charge.txid(),charge.amountCents(),ChargeState.REFUNDED));
        return new RefundResult("mock_refund_"+UUID.randomUUID(),amountCents);
    }
    @Override public WebhookEvent verifyWebhook(String payload,String signature) {
        if(!signature(payload).equals(signature)) throw ErrorCode.PAYMENT_WEBHOOK_INVALID.toException();
        var parts=payload.split("\\|", -1); if(parts.length!=3) throw ErrorCode.PAYMENT_WEBHOOK_INVALID.toException();
        var charge=getCharge(parts[1]); return new WebhookEvent(parts[0],parts[1],charge.txid(),ChargeState.valueOf(parts[2]));
    }
    public void setState(String chargeId, ChargeState state) { var charge=getCharge(chargeId); charges.put(chargeId,new Charge(charge.id(),charge.txid(),charge.amountCents(),state)); }
    public String signedWebhook(String eventId,String chargeId,ChargeState state) { String payload=eventId+"|"+chargeId+"|"+state; return payload+":"+signature(payload); }
    public String signature(String payload) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((SIGNING_KEY+payload).getBytes(StandardCharsets.UTF_8))); } catch(NoSuchAlgorithmException e){ throw new IllegalStateException(e); } }
}
