package la.meleva.fundraiser.participation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.participation.purchase.PurchaseOffer;
import la.meleva.fundraiser.participation.purchase.PurchaseOfferRepository;
import la.meleva.fundraiser.payment.MockPaymentProvider;
import la.meleva.fundraiser.payment.OrderService;
import la.meleva.fundraiser.payment.PaymentProvider;
import la.meleva.fundraiser.shared.ErrorCode;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryEntity;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryRepository;
import la.meleva.fundraiser.referral.ReferralService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ReservationServiceIntegrationTest {
    @Autowired ReservationService reservations;
    @Autowired ParticipationService participations;
    @Autowired CampaignRepository campaigns;
    @Autowired CampaignNumberRepository numbers;
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired ReservationExpiryWorker expiryWorker;
    @Autowired ReservationCheckoutService checkout;
    @Autowired PurchaseOfferRepository offers;
    @Autowired MockPaymentProvider provider;
    @Autowired OrderService orderService;
    @Autowired BeneficiaryRepository beneficiaries;
    @Autowired ReferralService referrals;

    @Test
    void expiryIsIdempotentUnderConcurrentWorkersAndReturnsStock() throws Exception {
        var campaign = openCampaign("lease-expiry", 3);
        var user = users.save(new User("Pessoa", "Reserva", "+55119999001301"));
        var view = reservations.reserveDemo(user.getId(), campaign.getPublicId(), UUID.randomUUID().toString(), 2);
        long requestId = jdbc.queryForObject("SELECT id FROM participation_requests WHERE public_id=?", Long.class, view.requestPublicId());
        assertThat(numbers.countByCampaignIdAndStatus(campaign.getId(), NumberStatus.RESERVED)).isEqualTo(2);
        assertThat(reservations.expireByRequestId(requestId, view.expiresAt().minusSeconds(1))).isFalse();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<java.util.concurrent.Future<Boolean>>();
            for (int i = 0; i < 16; i++) futures.add(executor.submit(() -> reservations.expireByRequestId(requestId, view.expiresAt())));
            for (var future : futures) assertThat(future.get(30, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(numbers.countByCampaignIdAndStatus(campaign.getId(), NumberStatus.AVAILABLE)).isEqualTo(3);
        assertThat(numbers.countByCampaignIdAndStatus(campaign.getId(), NumberStatus.RESERVED)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reservation_leases WHERE request_id=? AND status='EXPIRED'", Long.class, requestId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participation_entries WHERE request_id=? AND status='REJECTED'", Long.class, requestId)).isEqualTo(2);
    }

    @Test
    void oneHundredConcurrentReservationsHaveNoDuplicateNumbers() throws Exception {
        var campaign = openCampaign("lease-concurrency", 120);
        var people = new ArrayList<User>();
        for (int index = 0; index < 100; index++) {
            people.add(users.save(new User("Pessoa", "Reserva", "+5511977%04d".formatted(index))));
        }
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = people.stream().<java.util.concurrent.Callable<ReservationService.ReservationView>>map(user ->
                    () -> reservations.reserveDemo(user.getId(), campaign.getPublicId(), UUID.randomUUID().toString(), 1)).toList();
            var futures = executor.invokeAll(tasks, 90, TimeUnit.SECONDS);
            var allocated = new java.util.HashSet<String>();
            for (var future : futures) assertThat(allocated.add(future.get().numbers().getFirst())).isTrue();
            assertThat(allocated).hasSize(100);
        }
        assertThat(numbers.countByCampaignIdAndStatus(campaign.getId(), NumberStatus.RESERVED)).isEqualTo(100);
        assertThat(numbers.countByCampaignIdAndStatus(campaign.getId(), NumberStatus.ASSIGNED)).isZero();
    }

    @Test
    void outboxWorkerReleasesExpiredReservation() {
        var campaign = openCampaign("lease-worker", 2);
        var user = users.save(new User("Pessoa", "Worker", "+55119999001302"));
        var view = reservations.reserveDemo(user.getId(), campaign.getPublicId(), UUID.randomUUID().toString(), 1);
        Long requestId = jdbc.queryForObject("SELECT id FROM participation_requests WHERE public_id=?", Long.class, view.requestPublicId());
        jdbc.update("UPDATE reservation_leases SET expires_at=TIMESTAMPADD(SECOND,-5,UTC_TIMESTAMP()) WHERE request_id=?", requestId);
        jdbc.update("UPDATE outbox_events SET available_at=TIMESTAMPADD(SECOND,-5,UTC_TIMESTAMP()) WHERE aggregate_id=? AND event_type='RESERVATION_EXPIRY_REQUESTED'", requestId);
        expiryWorker.expirePending();
        assertThat(numbers.countByCampaignIdAndStatus(campaign.getId(), NumberStatus.AVAILABLE)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT status FROM outbox_events WHERE aggregate_id=? AND event_type='RESERVATION_EXPIRY_REQUESTED'", String.class, requestId)).isEqualTo("PROCESSED");
    }

    @Test
    void assignsOnlyAfterMatchingMockPaymentIsReconciled() {
        var campaign = openCampaign("lease-payment", 4);
        var user = users.save(new User("Pessoa", "Pagamento", "+55119999001303"));
        var promoter = users.save(new User("Pessoa", "Indicação", "+55119999001305"));
        var beneficiary = beneficiaries.save(new BeneficiaryEntity(campaign.getId(), "Entidade teste"));
        var referral = referrals.link(campaign.getId(), promoter.getId()).getCode();
        var offer = offers.save(new PurchaseOffer(campaign.getId(), 2, 250));
        String key = UUID.randomUUID().toString();
        var start = checkout.start(user.getId(), campaign.getPublicId(), key, offer.getPublicId(), beneficiary.getPublicId(), referral);
        assertThat(checkout.start(user.getId(), campaign.getPublicId(), key, offer.getPublicId(), beneficiary.getPublicId(), referral)).isEqualTo(start);
        assertThat(start.amountCents()).isEqualTo(250);
        assertThat(referrals.metrics(campaign.getId(), promoter.getId()).participations()).isZero();
        assertThatThrownBy(() -> checkout.confirm(user.getId(), campaign.getPublicId(), start.reservation().requestPublicId()))
                .isInstanceOf(ErrorCode.BusinessException.class);
        assertThat(numbers.countByCampaignIdAndStatus(campaign.getId(), NumberStatus.ASSIGNED)).isZero();
        Long paymentId = jdbc.queryForObject("SELECT p.id FROM payments p JOIN orders o ON o.id=p.order_id WHERE o.public_id=?", Long.class, start.orderPublicId());
        String chargeId = jdbc.queryForObject("SELECT provider_charge_id FROM payments WHERE id=?", String.class, paymentId);
        provider.setState(chargeId, PaymentProvider.ChargeState.PAID);
        orderService.reconcile(paymentId);
        checkout.confirm(user.getId(), campaign.getPublicId(), start.reservation().requestPublicId());
        checkout.confirm(user.getId(), campaign.getPublicId(), start.reservation().requestPublicId());
        assertThat(referrals.metrics(campaign.getId(), promoter.getId()).participations()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM beneficiary_selections WHERE request_id=(SELECT id FROM participation_requests WHERE public_id=?) AND beneficiary_id=?", Long.class, start.reservation().requestPublicId(), beneficiary.getId())).isEqualTo(1L);
        assertThat(numbers.countByCampaignIdAndStatus(campaign.getId(), NumberStatus.ASSIGNED)).isEqualTo(2);
        assertThat(numbers.countByCampaignIdAndStatus(campaign.getId(), NumberStatus.RESERVED)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reservation_leases WHERE request_id=(SELECT id FROM participation_requests WHERE public_id=?) AND status='CONSUMED'", Long.class, start.reservation().requestPublicId())).isEqualTo(2);
    }

    @Test
    void paymentAfterReservationDeadlineDoesNotAssignNumbers() {
        var campaign = openCampaign("lease-late-payment", 2);
        var user = users.save(new User("Pessoa", "Atraso", "+55119999001304"));
        var offer = offers.save(new PurchaseOffer(campaign.getId(), 1, 100));
        var start = checkout.start(user.getId(), campaign.getPublicId(), UUID.randomUUID().toString(), offer.getPublicId());
        Long requestId = jdbc.queryForObject("SELECT id FROM participation_requests WHERE public_id=?", Long.class, start.reservation().requestPublicId());
        Long paymentId = jdbc.queryForObject("SELECT p.id FROM payments p JOIN orders o ON o.id=p.order_id WHERE o.public_id=?", Long.class, start.orderPublicId());
        String chargeId = jdbc.queryForObject("SELECT provider_charge_id FROM payments WHERE id=?", String.class, paymentId);
        provider.setState(chargeId, PaymentProvider.ChargeState.PAID);
        orderService.reconcile(paymentId);
        jdbc.update("UPDATE reservation_leases SET expires_at=TIMESTAMPADD(SECOND,-5,UTC_TIMESTAMP()) WHERE request_id=?", requestId);
        assertThatThrownBy(() -> checkout.confirm(user.getId(), campaign.getPublicId(), start.reservation().requestPublicId()))
                .isInstanceOf(ErrorCode.BusinessException.class)
                .extracting(exception -> ((ErrorCode.BusinessException) exception).errorCode()).isEqualTo(ErrorCode.PAYMENT_ORDER_EXPIRED);
        assertThat(numbers.countByCampaignIdAndStatus(campaign.getId(), NumberStatus.ASSIGNED)).isZero();
        assertThat(reservations.expireByRequestId(requestId, java.time.Instant.now())).isTrue();
        assertThat(numbers.countByCampaignIdAndStatus(campaign.getId(), NumberStatus.AVAILABLE)).isEqualTo(2);
    }

    private Campaign openCampaign(String slug, int capacity) {
        var org = organizations.save(new Organization("Organização " + slug, "ASSOCIATION"));
        var campaign = new Campaign(org.getId(), "Campanha teste", slug, CampaignMode.DEMO);
        campaign.setNumberRange(0, capacity - 1);
        campaign.transitionTo(CampaignStatus.LEGAL_REVIEW);
        campaign.transitionTo(CampaignStatus.AUTHORIZED);
        campaign.transitionTo(CampaignStatus.SCHEDULED);
        campaign.transitionTo(CampaignStatus.OPEN);
        campaign = campaigns.save(campaign);
        participations.initializeSeries(campaign.getPublicId());
        return campaign;
    }
}
