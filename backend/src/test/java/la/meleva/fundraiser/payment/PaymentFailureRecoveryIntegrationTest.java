package la.meleva.fundraiser.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import la.meleva.fundraiser.TestcontainersConfiguration;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.compliance.AuthorizationService;
import la.meleva.fundraiser.identity.Organization;
import la.meleva.fundraiser.identity.OrganizationRepository;
import la.meleva.fundraiser.identity.User;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.notification.InboxEventRepository;
import la.meleva.fundraiser.notification.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * P09-08: timeout de transporte e falha parcial entre PSP e banco. O serviço é montado com um provedor
 * que injeta falhas e cada chamada roda em transação programática, como o proxy {@code @Transactional}.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PaymentFailureRecoveryIntegrationTest {
    @Autowired MockPaymentProvider mock;
    @Autowired CampaignRepository campaigns;
    @Autowired OrganizationRepository organizations;
    @Autowired UserRepository users;
    @Autowired OrderRepository orderRepository;
    @Autowired PaymentRepository payments;
    @Autowired PaymentEventRepository paymentEvents;
    @Autowired InboxEventRepository inbox;
    @Autowired FinancialLedgerEntryRepository ledger;
    @Autowired RefundRepository refunds;
    @Autowired OutboxEventRepository outbox;
    @Autowired AuthorizationService authorizations;
    @Autowired PlatformTransactionManager txManager;
    @Autowired org.springframework.context.ApplicationEventPublisher events;
    @Autowired JdbcTemplate jdbc;

    FaultyProvider psp;
    OrderService service;
    TransactionTemplate tx;

    @BeforeEach
    void setUp() {
        psp = new FaultyProvider(mock);
        service = new OrderService(campaigns, orderRepository, payments, paymentEvents, inbox, ledger, refunds, psp,
                jdbc, outbox, authorizations, events, Clock.systemUTC());
        tx = new TransactionTemplate(txManager);
    }

    @Test
    void chargeCreationTimeoutLeavesNothingAndRetryWithSameKeySucceeds() {
        Fixture fixture = fixture("p09-create-timeout");
        psp.failCreateCharge.set(1);

        assertThatThrownBy(() -> tx.execute(s -> service.createOrder(fixture.campaign().getId(), fixture.user().getId(), 500, "create-timeout-key")))
                .isInstanceOf(UncheckedIOException.class);
        assertThat(countOrders("create-timeout-key")).isZero();

        var receipt = tx.execute(s -> service.createOrder(fixture.campaign().getId(), fixture.user().getId(), 500, "create-timeout-key"));
        assertThat(receipt.status()).isEqualTo(OrderStatus.AWAITING_PAYMENT);
        assertThat(countOrders("create-timeout-key")).isEqualTo(1);
    }

    @Test
    void commitFailureAfterChargeWasCreatedLeavesTheOrphanChargeUnbound() {
        Fixture fixture = fixture("p09-commit-failure");
        String[] orphan = new String[1];

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> {
            orphan[0] = service.createOrder(fixture.campaign().getId(), fixture.user().getId(), 700, "commit-failure-key").chargeId();
            throw new IllegalStateException("falha de commit simulada");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(countOrders("commit-failure-key")).isZero();
        assertThat(mock.getCharge(orphan[0]).state()).isEqualTo(PaymentProvider.ChargeState.PENDING);

        var receipt = tx.execute(s -> service.createOrder(fixture.campaign().getId(), fixture.user().getId(), 700, "commit-failure-key"));
        assertThat(receipt.chargeId()).isNotEqualTo(orphan[0]);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payments WHERE provider_charge_id = ?", Long.class, orphan[0])).isZero();

        mock.setState(orphan[0], PaymentProvider.ChargeState.PAID);
        PaymentStatus afterOrphanPaid = tx.execute(s -> service.reconcile(receipt.paymentId()));
        assertThat(afterOrphanPaid).isEqualTo(PaymentStatus.PENDING);
        assertThat(ledger.findByCampaignId(fixture.campaign().getId())).isEmpty();
    }

    @Test
    void webhookWhoseSourceCheckTimesOutIsNotAcknowledgedAndRedeliveryConfirmsOnce() {
        Fixture fixture = fixture("p09-webhook-timeout");
        var receipt = tx.execute(s -> service.createOrder(fixture.campaign().getId(), fixture.user().getId(), 900, "webhook-timeout-key"));
        mock.setState(receipt.chargeId(), PaymentProvider.ChargeState.PAID);
        String signed = mock.signedWebhook("p09-evt-timeout", receipt.chargeId(), PaymentProvider.ChargeState.PAID);
        int split = signed.lastIndexOf(':');
        psp.failGetCharge.set(1);

        assertThatThrownBy(() -> tx.execute(s -> service.receiveWebhook(signed.substring(0, split), signed.substring(split + 1))))
                .isInstanceOf(UncheckedIOException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM inbox_events WHERE source_event_id = 'p09-evt-timeout'", Long.class)).isZero();
        assertThat(payments.findById(receipt.paymentId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PENDING);

        Boolean redelivered = tx.execute(s -> service.receiveWebhook(signed.substring(0, split), signed.substring(split + 1)));
        Boolean duplicate = tx.execute(s -> service.receiveWebhook(signed.substring(0, split), signed.substring(split + 1)));
        assertThat(redelivered).isTrue();
        assertThat(duplicate).isFalse();
        assertThat(payments.findById(receipt.paymentId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(ledger.findByCampaignId(fixture.campaign().getId())).hasSize(1);
    }

    @Test
    void lostRefundResponseIsRetriedWithoutRefundingTwice() {
        Fixture fixture = fixture("p09-refund-lost");
        var receipt = tx.execute(s -> service.createOrder(fixture.campaign().getId(), fixture.user().getId(), 1200, "refund-lost-key"));
        mock.setState(receipt.chargeId(), PaymentProvider.ChargeState.PAID);
        tx.execute(s -> service.reconcile(receipt.paymentId()));
        psp.loseRefundResponse.set(1);

        assertThatThrownBy(() -> tx.executeWithoutResult(s -> service.refund(receipt.paymentId(), "cancelamento com timeout")))
                .isInstanceOf(UncheckedIOException.class);
        assertThat(mock.getCharge(receipt.chargeId()).state()).isEqualTo(PaymentProvider.ChargeState.REFUNDED);
        assertThat(payments.findById(receipt.paymentId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CONFIRMED);

        tx.executeWithoutResult(s -> service.refund(receipt.paymentId(), "nova tentativa do operador"));

        assertThat(psp.refundCalls.get()).isEqualTo(1);
        assertThat(payments.findById(receipt.paymentId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refunds WHERE payment_id = ?", Long.class, receipt.paymentId())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT SUM(amount_cents) FROM financial_ledger WHERE source_ref = ?", Long.class,
                receipt.orderPublicId())).isZero();
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> service.refund(receipt.paymentId(), "terceira tentativa")))
                .isInstanceOf(la.meleva.fundraiser.shared.ErrorCode.BusinessException.class);
    }

    @Test
    void workerIsolatesAFailingPaymentRetriesTransientTimeoutsAndKeepsPendingOnesScheduled() {
        Fixture fixture = fixture("p09-worker");
        var timeout = tx.execute(s -> service.createOrder(fixture.campaign().getId(), fixture.user().getId(), 300, "worker-timeout-key"));
        var paid = tx.execute(s -> service.createOrder(fixture.campaign().getId(), fixture.user().getId(), 400, "worker-paid-key"));
        var pending = tx.execute(s -> service.createOrder(fixture.campaign().getId(), fixture.user().getId(), 500, "worker-pending-key"));
        var poisoned = tx.execute(s -> service.createOrder(fixture.campaign().getId(), fixture.user().getId(), 600, "worker-poison-key"));
        mock.setState(timeout.chargeId(), PaymentProvider.ChargeState.PAID);
        mock.setState(paid.chargeId(), PaymentProvider.ChargeState.PAID);
        jdbc.update("UPDATE payments SET amount_cents = 1 WHERE id = ?", poisoned.paymentId());
        isolateOutboxTo(timeout.paymentId(), paid.paymentId(), pending.paymentId(), poisoned.paymentId());
        psp.failGetChargeFor = timeout.chargeId();
        psp.failGetCharge.set(1);
        var worker = new ReconciliationWorker(outbox, service, txManager, Clock.systemUTC());

        worker.reconcilePending();

        assertThat(statusOf(paid)).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(statusOf(timeout)).isEqualTo(PaymentStatus.PENDING);
        assertThat(outboxRow(timeout.paymentId())).isEqualTo("PENDING|1|UncheckedIOException");
        assertThat(outboxRow(paid.paymentId())).isEqualTo("PROCESSED|0|");
        assertThat(outboxRow(pending.paymentId())).isEqualTo("PENDING|0|");
        assertThat(outboxRow(poisoned.paymentId())).isEqualTo("PENDING|1|PAYMENT_PROVIDER_ERROR");
        assertThat(jdbc.queryForObject("SELECT available_at > UTC_TIMESTAMP() FROM outbox_events WHERE aggregate_id = ? AND event_type = ?",
                Boolean.class, pending.paymentId(), ReconciliationWorker.EVENT_TYPE)).isTrue();

        jdbc.update("UPDATE outbox_events SET available_at = TIMESTAMPADD(SECOND, -1, UTC_TIMESTAMP()) WHERE aggregate_id = ? AND event_type = ?",
                timeout.paymentId(), ReconciliationWorker.EVENT_TYPE);
        worker.reconcilePending();

        assertThat(statusOf(timeout)).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(outboxRow(timeout.paymentId())).startsWith("PROCESSED|1|");
        assertThat(ledger.findByCampaignId(fixture.campaign().getId())).hasSize(2);
    }

    /** Eventos de outras classes de teste no mesmo banco não podem entrar no lote deste cenário. */
    private void isolateOutboxTo(Long... paymentIds) {
        String mine = String.join(",", java.util.Arrays.stream(paymentIds).map(String::valueOf).toList());
        jdbc.update("UPDATE outbox_events SET available_at = TIMESTAMPADD(DAY, 1, UTC_TIMESTAMP()) WHERE status = 'PENDING' "
                + "AND event_type = ? AND aggregate_id NOT IN (" + mine + ")", ReconciliationWorker.EVENT_TYPE);
        // available_at é TIMESTAMP sem fração: o MySQL arredonda para cima e o evento recém-criado ficaria no futuro.
        jdbc.update("UPDATE outbox_events SET available_at = TIMESTAMPADD(SECOND, -1, UTC_TIMESTAMP()) WHERE event_type = ? "
                + "AND aggregate_id IN (" + mine + ")", ReconciliationWorker.EVENT_TYPE);
    }

    private String outboxRow(Long paymentId) {
        return jdbc.queryForObject("SELECT CONCAT(status, '|', attempt_count, '|', COALESCE(error_message, '')) FROM outbox_events "
                + "WHERE aggregate_id = ? AND event_type = ?", String.class, paymentId, ReconciliationWorker.EVENT_TYPE);
    }

    private PaymentStatus statusOf(OrderService.OrderReceipt receipt) {
        return payments.findById(receipt.paymentId()).orElseThrow().getStatus();
    }

    private long countOrders(String key) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM orders WHERE idempotency_key = ?", Long.class, key);
    }

    private Fixture fixture(String slug) {
        Organization org = organizations.save(new Organization("Org " + slug, "ASSOCIATION"));
        User user = users.save(new User("Pessoa", "Teste", "+5511" + (900000000L + Math.abs(slug.hashCode() % 99999999))));
        return new Fixture(campaigns.save(new Campaign(org.getId(), slug, slug, CampaignMode.DEMO)), user);
    }

    private record Fixture(Campaign campaign, User user) {}

    /** Decorador do PSP simulado que injeta timeout de transporte antes ou depois da operação remota. */
    static final class FaultyProvider implements PaymentProvider {
        final MockPaymentProvider delegate;
        final AtomicInteger failCreateCharge = new AtomicInteger();
        final AtomicInteger failGetCharge = new AtomicInteger();
        final AtomicInteger loseRefundResponse = new AtomicInteger();
        final AtomicInteger refundCalls = new AtomicInteger();
        volatile String failGetChargeFor;

        FaultyProvider(MockPaymentProvider delegate) { this.delegate = delegate; }

        @Override public String name() { return delegate.name(); }

        @Override public Charge createCharge(ChargeRequest request) {
            if (failCreateCharge.getAndUpdate(n -> Math.max(0, n - 1)) > 0) throw timeout();
            return delegate.createCharge(request);
        }

        @Override public Charge getCharge(String chargeId) {
            boolean targeted = failGetChargeFor == null || failGetChargeFor.equals(chargeId);
            if (targeted && failGetCharge.getAndUpdate(n -> Math.max(0, n - 1)) > 0) throw timeout();
            return delegate.getCharge(chargeId);
        }

        @Override public RefundResult refund(String chargeId, long amountCents, String reason) {
            refundCalls.incrementAndGet();
            RefundResult result = delegate.refund(chargeId, amountCents, reason);
            if (loseRefundResponse.getAndUpdate(n -> Math.max(0, n - 1)) > 0) throw timeout();
            return result;
        }

        @Override public WebhookEvent verifyWebhook(String payload, String signature) {
            return delegate.verifyWebhook(payload, signature);
        }

        private static UncheckedIOException timeout() {
            return new UncheckedIOException(new SocketTimeoutException("timeout simulado do PSP"));
        }
    }
}
