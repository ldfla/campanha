package la.meleva.fundraiser.payment;

import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.shared.ErrorCode.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Import(TestcontainersConfiguration.class)
class OrderServiceIntegrationTest {
    @Autowired OrderService orders; @Autowired MockPaymentProvider provider; @Autowired CampaignRepository campaigns;
    @Autowired OrganizationRepository organizations; @Autowired UserRepository users; @Autowired PaymentRepository payments;
    @Autowired FinancialLedgerEntryRepository ledger;
    @Autowired JdbcTemplate jdbc;

    @Test void confirmsOnlyVerifiedUniqueWebhookAndReconcilesAgainstProvider() {
        var fixture=fixture(CampaignMode.DEMO,"payment-demo");
        var receipt=orders.createOrder(fixture.campaign().getId(),fixture.user().getId(),5600,"order-idempotency-a");
        assertThat(orders.createOrder(fixture.campaign().getId(),fixture.user().getId(),5600,"order-idempotency-a").paymentId()).isEqualTo(receipt.paymentId());
        provider.setState(receipt.chargeId(), PaymentProvider.ChargeState.PAID);
        String signed=provider.signedWebhook("evt-paid-1",receipt.chargeId(),PaymentProvider.ChargeState.PAID); int separator=signed.lastIndexOf(':');
        assertThat(orders.receiveWebhook(signed.substring(0,separator),signed.substring(separator+1))).isTrue();
        assertThat(orders.receiveWebhook(signed.substring(0,separator),signed.substring(separator+1))).isFalse();
        orders.reconcile(receipt.paymentId());
        assertThat(payments.findById(receipt.paymentId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(ledger.findByCampaignId(fixture.campaign().getId())).hasSize(1);
    }

    @Test void rejectsBadSignatureAndCommercialPromotionAndCanRefund() {
        var fixture=fixture(CampaignMode.DEMO,"payment-refund"); var receipt=orders.createOrder(fixture.campaign().getId(),fixture.user().getId(),100,"idemp-refund");
        assertThatThrownBy(()->orders.receiveWebhook("bad|"+receipt.chargeId()+"|PAID","wrong")).isInstanceOf(BusinessException.class);
        provider.setState(receipt.chargeId(),PaymentProvider.ChargeState.PAID); orders.reconcile(receipt.paymentId()); orders.refund(receipt.paymentId(),"cancelamento de teste");
        assertThat(payments.findById(receipt.paymentId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(ledger.findByCampaignId(fixture.campaign().getId())).hasSize(2);
        var commercial=fixture(CampaignMode.COMMERCIAL_PROMOTION,"payment-commercial");
        assertThatThrownBy(()->orders.createOrder(commercial.campaign().getId(),commercial.user().getId(),100,"idemp-commercial")).isInstanceOf(BusinessException.class);
    }
    @Test void latePaymentAfterOrderExpiryDoesNotConfirmOrPostRevenue() {
        var fixture=fixture(CampaignMode.DEMO,"payment-late"); var receipt=orders.createOrder(fixture.campaign().getId(),fixture.user().getId(),250,"late-payment-key");
        jdbc.update("UPDATE orders SET expires_at = TIMESTAMPADD(MINUTE, -1, UTC_TIMESTAMP()) WHERE public_id = ?",receipt.orderPublicId());
        provider.setState(receipt.chargeId(),PaymentProvider.ChargeState.PAID); orders.reconcile(receipt.paymentId());
        assertThat(payments.findById(receipt.paymentId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(ledger.findByCampaignId(fixture.campaign().getId())).isEmpty();
    }
    @Test void ignoresOutOfOrderExpirationAfterPaymentWasConfirmed() {
        var fixture=fixture(CampaignMode.DEMO,"payment-order-events"); var receipt=orders.createOrder(fixture.campaign().getId(),fixture.user().getId(),250,"ordered-events-key");
        provider.setState(receipt.chargeId(),PaymentProvider.ChargeState.PAID); orders.reconcile(receipt.paymentId());
        String paid=provider.signedWebhook("evt-order-paid",receipt.chargeId(),PaymentProvider.ChargeState.PAID); int split=paid.lastIndexOf(':'); orders.receiveWebhook(paid.substring(0,split),paid.substring(split+1));
        String expired=provider.signedWebhook("evt-order-expired",receipt.chargeId(),PaymentProvider.ChargeState.EXPIRED); split=expired.lastIndexOf(':'); orders.receiveWebhook(expired.substring(0,split),expired.substring(split+1));
        assertThat(payments.findById(receipt.paymentId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(ledger.findByCampaignId(fixture.campaign().getId())).hasSize(1);
    }
    @Test void repeatedConfirmationDoesNotMoveOriginalConfirmationTime() {
        var fixture=fixture(CampaignMode.DEMO,"payment-confirmed-time");
        var receipt=orders.createOrder(fixture.campaign().getId(),fixture.user().getId(),250,"confirmation-time-key");
        provider.setState(receipt.chargeId(),PaymentProvider.ChargeState.PAID);
        orders.reconcile(receipt.paymentId());
        jdbc.update("UPDATE payments SET confirmed_at = TIMESTAMPADD(DAY, -1, UTC_TIMESTAMP()) WHERE id = ?", receipt.paymentId());
        var original=jdbc.queryForObject("SELECT confirmed_at FROM payments WHERE id=?", java.sql.Timestamp.class, receipt.paymentId());
        orders.reconcile(receipt.paymentId());
        var after=jdbc.queryForObject("SELECT confirmed_at FROM payments WHERE id=?", java.sql.Timestamp.class, receipt.paymentId());
        assertThat(after).isEqualTo(original);
        assertThat(ledger.findByCampaignId(fixture.campaign().getId())).hasSize(1);
    }
    @Test void refundedProviderStateRequiresMatchingMockAndIsRecordedOnceUnderConcurrency() throws Exception {
        var fixture=fixture(CampaignMode.DEMO,"payment-provider-refund");
        var receipt=orders.createOrder(fixture.campaign().getId(),fixture.user().getId(),1000,"provider-refund-key");
        provider.setState(receipt.chargeId(),PaymentProvider.ChargeState.PAID);
        orders.reconcile(receipt.paymentId());
        String forged=provider.signedWebhook("refund-before-provider",receipt.chargeId(),PaymentProvider.ChargeState.REFUNDED);
        int separator=forged.lastIndexOf(':');
        assertThatThrownBy(() -> orders.receiveWebhook(forged.substring(0,separator),forged.substring(separator+1)))
                .isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refunds WHERE payment_id=?",Long.class,receipt.paymentId())).isZero();
        provider.setState(receipt.chargeId(),PaymentProvider.ChargeState.REFUNDED);
        int workers=16;
        var ready=new CountDownLatch(workers);
        var start=new CountDownLatch(1);
        try (var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            var futures=new ArrayList<java.util.concurrent.Future<?>>();
            for(int i=0;i<workers;i++) futures.add(executor.submit(() -> {
                ready.countDown(); start.await(); orders.reconcile(receipt.paymentId()); return null;
            }));
            ready.await(); start.countDown();
            for(var future:futures) future.get();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refunds WHERE payment_id=? AND status='COMPLETED'",Long.class,receipt.paymentId())).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT SUM(amount_cents) FROM refunds WHERE payment_id=?",Long.class,receipt.paymentId())).isEqualTo(1000L);
        assertThat(ledger.findByCampaignId(fixture.campaign().getId())).hasSize(2);
        assertThat(payments.findById(receipt.paymentId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }
    @Test void reorderedRefundAndPaidEventsCannotConfirmWhenProviderIsAlreadyRefunded() {
        var fixture=fixture(CampaignMode.DEMO,"payment-reordered-refund");
        var receipt=orders.createOrder(fixture.campaign().getId(),fixture.user().getId(),1000,"out-of-order-refund-key");
        provider.setState(receipt.chargeId(),PaymentProvider.ChargeState.REFUNDED);
        for(var state : java.util.List.of(PaymentProvider.ChargeState.REFUNDED, PaymentProvider.ChargeState.PAID)) {
            String event=provider.signedWebhook("reordered-"+state,receipt.chargeId(),state);
            int separator=event.lastIndexOf(':');
            assertThatThrownBy(() -> orders.receiveWebhook(event.substring(0,separator),event.substring(separator+1)))
                    .isInstanceOf(BusinessException.class);
        }
        assertThat(payments.findById(receipt.paymentId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(ledger.findByCampaignId(fixture.campaign().getId())).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inbox_events WHERE source='MOCK' AND source_event_id LIKE 'reordered-%'", Long.class)).isZero();
    }
    @Test void inconsistentProviderAmountCannotConfirmAndRefundAfterExpiryIsNotAcknowledged() {
        var fixture=fixture(CampaignMode.DEMO,"payment-source-inconsistent");
        var receipt=orders.createOrder(fixture.campaign().getId(),fixture.user().getId(),1000,"source-inconsistent-key");
        provider.setState(receipt.chargeId(),PaymentProvider.ChargeState.PAID);
        jdbc.update("UPDATE payments SET amount_cents=999 WHERE id=?",receipt.paymentId());
        assertThatThrownBy(() -> orders.reconcile(receipt.paymentId())).isInstanceOf(BusinessException.class);
        assertThat(payments.findById(receipt.paymentId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(ledger.findByCampaignId(fixture.campaign().getId())).isEmpty();
        jdbc.update("UPDATE payments SET amount_cents=1000,status='EXPIRED' WHERE id=?",receipt.paymentId());
        provider.setState(receipt.chargeId(),PaymentProvider.ChargeState.REFUNDED);
        String event=provider.signedWebhook("expired-refund-event",receipt.chargeId(),PaymentProvider.ChargeState.REFUNDED);
        int separator=event.lastIndexOf(':');
        assertThatThrownBy(() -> orders.receiveWebhook(event.substring(0,separator),event.substring(separator+1)))
                .isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inbox_events WHERE source_event_id='expired-refund-event'",Long.class)).isZero();
    }
    private Fixture fixture(CampaignMode mode,String slug){var org=organizations.save(new Organization("Org "+slug,"ASSOCIATION"));var user=users.save(new User("Pessoa","Teste","+5511"+Math.abs(slug.hashCode())));return new Fixture(campaigns.save(new Campaign(org.getId(),slug,slug,mode)),user);}
    record Fixture(Campaign campaign,User user){}
}
