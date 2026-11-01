package la.meleva.fundraiser.operations;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.participation.ParticipationService;
import la.meleva.fundraiser.participation.ReservationCheckoutService;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryEntity;
import la.meleva.fundraiser.participation.beneficiary.BeneficiaryRepository;
import la.meleva.fundraiser.participation.purchase.PurchaseOffer;
import la.meleva.fundraiser.participation.purchase.PurchaseOfferRepository;
import la.meleva.fundraiser.payment.MockPaymentProvider;
import la.meleva.fundraiser.payment.OrderRepository;
import la.meleva.fundraiser.payment.OrderService;
import la.meleva.fundraiser.payment.Payment;
import la.meleva.fundraiser.payment.PaymentProvider;
import la.meleva.fundraiser.payment.PaymentRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import la.meleva.fundraiser.shared.ErrorCode.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.mysql.MySQLContainer;

/**
 * P18-02: 500 concorrentes, último número, falta de espaço e deadlock. Medições impressas com prefixo [P18-02]
 * e registradas em docs/progress sem arredondar a favor.
 */
@SpringBootTest(properties = {
        "spring.datasource.hikari.maximum-pool-size=50",
        "spring.datasource.hikari.connection-timeout=240000"})
@Import(TestcontainersConfiguration.class)
class ConcurrencyLimitsIntegrationTest {
    private static final int CONTENDERS = 500;
    private static final int CAPACITY = 400;

    @Autowired ParticipationService participations;
    @Autowired ReservationCheckoutService checkout;
    @Autowired OrderService orderService;
    @Autowired MockPaymentProvider mock;
    @Autowired CampaignRepository campaigns;
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @Autowired BeneficiaryRepository beneficiaries;
    @Autowired PurchaseOfferRepository offers;
    @Autowired OrderRepository orders;
    @Autowired PaymentRepository payments;
    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @Autowired MySQLContainer mysql;

    @Test
    void fiveHundredContendersForFourHundredNumbersFillThePoolExactlyOnce() throws Exception {
        var fixture = openCampaign("p18-500-contenders", CAPACITY);
        var people = new ArrayList<User>();
        for (int i = 0; i < CONTENDERS; i++) {
            people.add(users.save(new User("Carga", "Q" + i, "+551195500" + "%04d".formatted(i))));
        }
        long deadlocksBefore = deadlocks();
        var ready = new CountDownLatch(CONTENDERS);
        var go = new CountDownLatch(1);

        long started = System.nanoTime();
        var numbers = new HashSet<Integer>();
        int exhausted = 0;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<Integer>> tasks = people.stream().<Callable<Integer>>map(user -> () -> {
                ready.countDown();
                go.await();
                return participations.request(user.getId(), fixture.campaign().getPublicId(), UUID.randomUUID().toString(), 1,
                        fixture.beneficiary().getPublicId()).numbers().getFirst().numberValue();
            }).toList();
            var futures = new ArrayList<Future<Integer>>();
            for (var task : tasks) futures.add(executor.submit(task));
            ready.await();
            go.countDown();
            for (var future : futures) {
                try {
                    assertThat(numbers.add(future.get(300, TimeUnit.SECONDS))).as("número duplicado").isTrue();
                } catch (ExecutionException failure) {
                    assertThat(failure.getCause()).isInstanceOfSatisfying(BusinessException.class,
                            ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.NUMBER_POOL_EXHAUSTED));
                    exhausted++;
                }
            }
        }
        long elapsedMillis = (System.nanoTime() - started) / 1_000_000;
        System.out.println("[P18-02] contenders=" + CONTENDERS + " capacity=" + CAPACITY + " assigned=" + numbers.size()
                + " exhausted=" + exhausted + " elapsed_ms=" + elapsedMillis + " deadlocks=" + (deadlocks() - deadlocksBefore));

        assertThat(numbers).hasSize(CAPACITY).contains(0, CAPACITY - 1);
        assertThat(exhausted).isEqualTo(CONTENDERS - CAPACITY);
        assertThat(participations.availableCount(fixture.campaign().getPublicId())).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM campaign_numbers WHERE campaign_id = ? AND status = 'ASSIGNED'",
                Long.class, fixture.campaign().getId())).isEqualTo(CAPACITY);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participation_entries WHERE campaign_id = ? AND status = 'ASSIGNED'",
                Long.class, fixture.campaign().getId())).isEqualTo(CAPACITY);
        assertThat(deadlocks() - deadlocksBefore).isZero();
    }

    @Test
    void demoRequestsCheckoutConfirmationsAndRefundsInParallelDoNotDeadlock() throws Exception {
        var fixture = openCampaign("p18-mixed-locks", 500);
        PurchaseOffer offer = offers.save(new PurchaseOffer(fixture.campaign().getId(), 2, 250));
        var toRefund = new ArrayList<Payment>();
        for (int i = 0; i < 20; i++) {
            var buyer = users.save(new User("Estorno", "R" + i, "+551195600" + "%04d".formatted(i)));
            var started = startPaidCheckout(fixture, offer, buyer);
            checkout.confirm(buyer.getId(), fixture.campaign().getPublicId(), started.requestPublicId());
            toRefund.add(started.payment());
        }
        var toConfirm = new ArrayList<StartedCheckout>();
        for (int i = 0; i < 20; i++) {
            toConfirm.add(startPaidCheckout(fixture, offer, users.save(new User("Confirma", "C" + i, "+551195700" + "%04d".formatted(i)))));
        }
        var demoBuyers = new ArrayList<User>();
        for (int i = 0; i < 60; i++) {
            demoBuyers.add(users.save(new User("Demo", "D" + i, "+551195800" + "%04d".formatted(i))));
        }
        long deadlocksBefore = deadlocks();
        var tasks = new ArrayList<Callable<Void>>();
        toRefund.forEach(payment -> tasks.add(() -> { orderService.refund(payment.getId(), "estorno concorrente"); return null; }));
        toConfirm.forEach(started -> tasks.add(() -> {
            checkout.confirm(started.buyerId(), fixture.campaign().getPublicId(), started.requestPublicId()); return null; }));
        demoBuyers.forEach(user -> tasks.add(() -> {
            participations.request(user.getId(), fixture.campaign().getPublicId(), UUID.randomUUID().toString(), 1,
                    fixture.beneficiary().getPublicId()); return null; }));
        java.util.Collections.shuffle(tasks, new java.util.Random(18));

        long started = System.nanoTime();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (var future : executor.invokeAll(tasks, 300, TimeUnit.SECONDS)) future.get();
        }
        System.out.println("[P18-02] mixed_tasks=" + tasks.size() + " elapsed_ms=" + (System.nanoTime() - started) / 1_000_000
                + " deadlocks=" + (deadlocks() - deadlocksBefore));

        assertThat(deadlocks() - deadlocksBefore).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participation_entries WHERE campaign_id = ? AND status = 'INVALIDATED'",
                Long.class, fixture.campaign().getId())).isEqualTo(40);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM participation_entries WHERE campaign_id = ? AND status = 'ASSIGNED'",
                Long.class, fixture.campaign().getId())).isEqualTo(40 + 60);
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT number_value) = COUNT(*) FROM campaign_numbers "
                + "WHERE campaign_id = ? AND entry_id IS NOT NULL", Boolean.class, fixture.campaign().getId())).isTrue();
    }

    @Test
    void deadlockCounterDetectsADeliberateDeadlock() throws Exception {
        jdbc.execute("CREATE TABLE IF NOT EXISTS p18_deadlock_probe (id INT PRIMARY KEY, v INT NOT NULL) ENGINE=InnoDB");
        jdbc.update("INSERT IGNORE INTO p18_deadlock_probe (id, v) VALUES (1, 0), (2, 0)");
        long before = deadlocks();
        var bothLockedFirstRow = new CountDownLatch(2);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var a = executor.submit(() -> crossUpdate(1, 2, bothLockedFirstRow));
            var b = executor.submit(() -> crossUpdate(2, 1, bothLockedFirstRow));
            int victims = (a.get(30, TimeUnit.SECONDS) ? 0 : 1) + (b.get(30, TimeUnit.SECONDS) ? 0 : 1);
            assertThat(victims).isEqualTo(1);
        }
        assertThat(deadlocks() - before).isEqualTo(1);
    }

    /** true = transação concluída; false = escolhida como vítima de deadlock (erro 1213). */
    private boolean crossUpdate(int first, int second, CountDownLatch bothLockedFirstRow) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.executeUpdate("UPDATE p18_deadlock_probe SET v = v + 1 WHERE id = " + first);
                bothLockedFirstRow.countDown();
                bothLockedFirstRow.await();
                statement.executeUpdate("UPDATE p18_deadlock_probe SET v = v + 1 WHERE id = " + second);
                connection.commit();
                return true;
            } catch (SQLException failure) {
                connection.rollback();
                if (failure.getErrorCode() == 1213) return false;
                throw failure;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /** O usuário da aplicação não tem PROCESS (menor privilégio); a métrica é lida com root só no contêiner de teste. */
    private long deadlocks() throws SQLException {
        try (Connection root = java.sql.DriverManager.getConnection(mysql.getJdbcUrl(), "root", mysql.getPassword());
             var statement = root.createStatement();
             var result = statement.executeQuery("SELECT `COUNT` FROM information_schema.INNODB_METRICS WHERE NAME = 'lock_deadlocks'")) {
            result.next();
            return result.getLong(1);
        }
    }

    private StartedCheckout startPaidCheckout(Fixture fixture, PurchaseOffer offer, User buyer) {
        var view = checkout.start(buyer.getId(), fixture.campaign().getPublicId(), UUID.randomUUID().toString(),
                offer.getPublicId(), fixture.beneficiary().getPublicId(), null);
        var order = orders.findByPublicId(view.orderPublicId()).orElseThrow();
        Payment payment = payments.findByOrderId(order.getId()).orElseThrow();
        mock.setState(payment.getProviderChargeId(), PaymentProvider.ChargeState.PAID);
        orderService.reconcile(payment.getId());
        return new StartedCheckout(buyer.getId(), view.reservation().requestPublicId(), payment);
    }

    private Fixture openCampaign(String slug, int capacity) {
        var organization = organizations.save(new Organization("Organização " + slug, "ASSOCIATION"));
        var campaign = new Campaign(organization.getId(), "Campanha " + slug, slug, CampaignMode.DEMO);
        campaign.setNumberRange(0, capacity - 1);
        var saved = campaigns.save(campaign);
        participations.initializeSeries(saved.getPublicId());
        var beneficiary = beneficiaries.save(new BeneficiaryEntity(saved.getId(), "Entidade " + slug));
        for (CampaignStatus next : List.of(CampaignStatus.LEGAL_REVIEW, CampaignStatus.AUTHORIZED,
                CampaignStatus.SCHEDULED, CampaignStatus.OPEN)) {
            saved.transitionTo(next);
        }
        return new Fixture(campaigns.save(saved), beneficiary);
    }

    private record Fixture(Campaign campaign, BeneficiaryEntity beneficiary) {}

    private record StartedCheckout(Long buyerId, String requestPublicId, Payment payment) {}
}
