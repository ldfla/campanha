package la.meleva.fundraiser.payment;

import la.meleva.fundraiser.shared.ErrorCode;

/** Deliberadamente sem endpoints ou credenciais até contrato/homologação de PSP. */
public final class DisabledLivePaymentProvider implements PaymentProvider {
    private RuntimeException disabled(){ return ErrorCode.CAMPAIGN_LIVE_OPERATIONS_DISABLED.toException(); }
    @Override public String name(){return "LIVE_DISABLED";}
    @Override public Charge createCharge(ChargeRequest request){throw disabled();}
    @Override public Charge getCharge(String chargeId){throw disabled();}
    @Override public RefundResult refund(String chargeId,long amountCents,String reason){throw disabled();}
    @Override public WebhookEvent verifyWebhook(String payload,String signature){throw disabled();}
}
