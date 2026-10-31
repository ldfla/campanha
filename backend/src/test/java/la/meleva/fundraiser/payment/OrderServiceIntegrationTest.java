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
    private Fixture fixture(CampaignMode mode,String slug){var org=organizations.save(new Organization("Org "+slug,"ASSOCIATION"));var user=users.save(new User("Pessoa","Teste","+5511"+Math.abs(slug.hashCode())));return new Fixture(campaigns.save(new Campaign(org.getId(),slug,slug,mode)),user);}
    record Fixture(Campaign campaign,User user){}
}
