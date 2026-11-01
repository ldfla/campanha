package la.meleva.fundraiser.payment;

import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.compliance.AuthorizationService;
import la.meleva.fundraiser.notification.InboxEventRepository;
import la.meleva.fundraiser.notification.OutboxEvent;
import la.meleva.fundraiser.notification.OutboxEventRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class OrderService {
    private final CampaignRepository campaigns; private final OrderRepository orders; private final PaymentRepository payments;
    private final PaymentEventRepository paymentEvents; private final InboxEventRepository inbox; private final FinancialLedgerEntryRepository ledger;
    private final RefundRepository refunds; private final PaymentProvider provider; private final Clock clock; private final JdbcTemplate jdbc; private final OutboxEventRepository outbox; private final AuthorizationService authorizations;
    @Autowired
    public OrderService(CampaignRepository campaigns, OrderRepository orders, PaymentRepository payments, PaymentEventRepository paymentEvents, InboxEventRepository inbox, FinancialLedgerEntryRepository ledger, RefundRepository refunds, PaymentProvider provider, JdbcTemplate jdbc, OutboxEventRepository outbox, AuthorizationService authorizations) {
        this(campaigns, orders, payments, paymentEvents, inbox, ledger, refunds, provider, jdbc, outbox, authorizations, Clock.systemUTC());
    }
    OrderService(CampaignRepository campaigns, OrderRepository orders, PaymentRepository payments, PaymentEventRepository paymentEvents, InboxEventRepository inbox, FinancialLedgerEntryRepository ledger, RefundRepository refunds, PaymentProvider provider, JdbcTemplate jdbc, OutboxEventRepository outbox, AuthorizationService authorizations, Clock clock) {
        this.campaigns=campaigns;this.orders=orders;this.payments=payments;this.paymentEvents=paymentEvents;this.inbox=inbox;this.ledger=ledger;this.refunds=refunds;this.provider=provider;this.jdbc=jdbc;this.outbox=outbox;this.authorizations=authorizations;this.clock=clock;
    }
    @Transactional
    public OrderReceipt createOrder(Long campaignId, Long userId, long amountCents, String idempotencyKey) {
        var prior=orders.findByCampaignIdAndUserIdAndIdempotencyKey(campaignId,userId,idempotencyKey); if(prior.isPresent()) return receipt(prior.get(),payments.findByOrderId(prior.get().getId()).orElseThrow());
        Campaign campaign=campaigns.findById(campaignId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        assertSandboxBillable(campaign); if(amountCents<=0) throw ErrorCode.VALIDATION_ERROR.toException();
        var order=new Order(campaignId,userId,amountCents,campaign.getMode().name(),idempotencyKey,Instant.now(clock).plus(Duration.ofMinutes(15))); orders.saveAndFlush(order);
        String txid="demo-"+UUID.randomUUID(); var charge=provider.createCharge(new PaymentProvider.ChargeRequest(txid,amountCents,"Participação sandbox"));
        var payment=new Payment(order.getId(),provider.name(),amountCents); payment.bindCharge(charge.id(),txid); payments.save(payment); order.awaitingPayment(); outbox.save(new OutboxEvent("PAYMENT",payment.getId(),"PAYMENT_RECONCILIATION_REQUESTED","{}"));
        return receipt(order,payment);
    }
    @Transactional
    public boolean receiveWebhook(String payload,String signature) {
        var webhook=provider.verifyWebhook(payload,signature); String hash=sha256(payload);
        if (jdbc.update("INSERT IGNORE INTO inbox_events (source, source_event_id, payload_hash, received_at) VALUES (?, ?, ?, UTC_TIMESTAMP())", provider.name(), webhook.eventId(), hash) == 0) return false;
        var payment=payments.findLockedByProviderAndProviderChargeId(provider.name(),webhook.chargeId()).orElseThrow(ErrorCode.PAYMENT_PROVIDER_ERROR::toException);
        var source=verifiedSource(payment);
        if(webhook.state()==PaymentProvider.ChargeState.REFUNDED && source.state()!=PaymentProvider.ChargeState.REFUNDED)
            throw ErrorCode.PAYMENT_PROVIDER_ERROR.toException();
        var event=new PaymentEvent(payment.getId(),webhook.eventId(),webhook.state().name(),hash); paymentEvents.save(event);
        applyProviderState(payment,source.state()); event.processed(); return true;
    }
    @Transactional
    public void reconcile(Long paymentId) {
        var payment=payments.findLockedById(paymentId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException); var order=orders.findById(payment.getOrderId()).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        var state=verifiedSource(payment).state();
        if(order.isExpired(Instant.now(clock)) && payment.getStatus()==PaymentStatus.PENDING && state!=PaymentProvider.ChargeState.REFUNDED){payment.expire();order.expire();return;}
        applyProviderState(payment,state);
        if(payment.getStatus()==PaymentStatus.CONFIRMED){order.reconcile(); postLedgerOnce(order,"PAYMENT_CONFIRMED",payment.getAmountCents());}
    }
    @Transactional
    public void refund(Long paymentId,String reason) {
        var payment=payments.findLockedById(paymentId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException); if(payment.getStatus()!=PaymentStatus.CONFIRMED) throw ErrorCode.PAYMENT_PROVIDER_ERROR.toException();
        var order=orders.findById(payment.getOrderId()).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException); order.refundPending();
        var result=provider.refund(payment.getProviderChargeId(),payment.getAmountCents(),reason); var refund=new Refund(paymentId,payment.getAmountCents(),reason); refund.complete(result.id()); refunds.save(refund); payment.refund();order.refunded();postLedgerOnce(order,"PAYMENT_REFUND",-payment.getAmountCents());
    }
    private void applyProviderState(Payment payment, PaymentProvider.ChargeState state) {
        var order=orders.findById(payment.getOrderId()).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if(payment.getStatus()==PaymentStatus.CONFIRMED && (state==PaymentProvider.ChargeState.EXPIRED || state==PaymentProvider.ChargeState.FAILED || state==PaymentProvider.ChargeState.PENDING)) return;
        if(payment.getStatus()==PaymentStatus.REFUNDED) return;
        if(payment.getStatus()==PaymentStatus.EXPIRED && state==PaymentProvider.ChargeState.REFUNDED)
            throw ErrorCode.PAYMENT_PROVIDER_ERROR.toException();
        if(payment.getStatus()==PaymentStatus.EXPIRED) return;
        if(state==PaymentProvider.ChargeState.REFUNDED && payment.getStatus()!=PaymentStatus.CONFIRMED)
            throw ErrorCode.PAYMENT_PROVIDER_ERROR.toException();
        if(state==PaymentProvider.ChargeState.PAID){ if(order.isExpired(Instant.now(clock))){payment.expire();order.expire();return;} payment.confirm();order.confirm();postLedgerOnce(order,"PAYMENT_CONFIRMED",payment.getAmountCents()); }
        else if(state==PaymentProvider.ChargeState.EXPIRED){payment.expire();order.expire();}
        else if(state==PaymentProvider.ChargeState.FAILED){payment.fail();}
        else if(state==PaymentProvider.ChargeState.REFUNDED && payment.getStatus()==PaymentStatus.CONFIRMED){
            // O contrato MOCK atual só admite estorno integral; provedores reais exigem contrato próprio.
            if (!"MOCK".equals(payment.getProvider()) || !"MOCK".equals(provider.name())) throw ErrorCode.PAYMENT_PROVIDER_ERROR.toException();
            var charge=provider.getCharge(payment.getProviderChargeId());
            if(charge.state()!=PaymentProvider.ChargeState.REFUNDED || charge.amountCents()!=payment.getAmountCents()
                    || !java.util.Objects.equals(payment.getTxid(),charge.txid())) throw ErrorCode.PAYMENT_PROVIDER_ERROR.toException();
            var refund=new Refund(payment.getId(),payment.getAmountCents(),"Estorno integral confirmado pelo provedor simulado");
            refund.complete(null); refunds.save(refund);
            payment.refund();order.refunded();postLedgerOnce(order,"PAYMENT_REFUND",-payment.getAmountCents());
        }
    }
    private void postLedgerOnce(Order order,String type,long amount){String ref=order.getPublicId();if(!ledger.existsBySourceRefAndEntryType(ref,type))ledger.save(new FinancialLedgerEntry(order.getCampaignId(),ref,type,amount));}
    private PaymentProvider.Charge verifiedSource(Payment payment) {
        var charge=provider.getCharge(payment.getProviderChargeId());
        if(!java.util.Objects.equals(charge.id(),payment.getProviderChargeId())
                || !java.util.Objects.equals(charge.txid(),payment.getTxid())
                || charge.amountCents()!=payment.getAmountCents()) throw ErrorCode.PAYMENT_PROVIDER_ERROR.toException();
        return charge;
    }
    private void assertSandboxBillable(Campaign campaign) {
        var mode = campaign.getMode();
        if (mode == CampaignMode.COMMERCIAL_PROMOTION) throw ErrorCode.PAYMENT_NOT_ALLOWED.toException();
        if (mode.isDemo()) return;
        // ADR-0007: título de capitalização premiável só cobra com autorização verificada e vigente.
        if (mode == CampaignMode.CAPITALIZATION_PRIZE_TITLE && authorizations.hasEffectiveAuthorization(campaign.getId())) return;
        throw ErrorCode.CAMPAIGN_NOT_AUTHORIZED.toException();
    }
    private OrderReceipt receipt(Order order,Payment payment){return new OrderReceipt(order.getPublicId(),payment.getId(),payment.getProviderChargeId(),payment.getTxid(),order.getStatus());}
    private static String sha256(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    public record OrderReceipt(String orderPublicId,Long paymentId,String chargeId,String txid,OrderStatus status) {}
}
