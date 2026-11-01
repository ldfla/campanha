package la.meleva.fundraiser.payment;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import la.meleva.fundraiser.notification.OutboxEvent;
import la.meleva.fundraiser.notification.OutboxEventRepository;
import la.meleva.fundraiser.shared.ErrorCode.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reconcilia pagamentos pendentes a partir do outbox. Cada pagamento roda em transação própria:
 * timeout do PSP ou inconsistência em um item não desfaz nem bloqueia os demais do lote.
 */
@Component
public class ReconciliationWorker {
    static final String EVENT_TYPE = "PAYMENT_RECONCILIATION_REQUESTED";
    private static final int BATCH_SIZE = 100;
    private static final Duration PENDING_RECHECK = Duration.ofMinutes(1);
    private static final Duration MAX_BACKOFF = Duration.ofMinutes(30);

    private final OutboxEventRepository outbox;
    private final OrderService orders;
    private final TransactionTemplate batchTx;
    private final TransactionTemplate itemTx;
    private final Clock clock;

    @Autowired
    public ReconciliationWorker(OutboxEventRepository outbox, OrderService orders, PlatformTransactionManager txManager) {
        this(outbox, orders, txManager, Clock.systemUTC());
    }

    ReconciliationWorker(OutboxEventRepository outbox, OrderService orders, PlatformTransactionManager txManager, Clock clock) {
        this.outbox = outbox;
        this.orders = orders;
        this.batchTx = new TransactionTemplate(txManager);
        this.itemTx = new TransactionTemplate(txManager);
        this.itemTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${fundraiser.payment.reconciliation-delay-ms:300000}")
    public void reconcilePending() {
        batchTx.executeWithoutResult(status -> {
            Instant now = Instant.now(clock);
            for (OutboxEvent event : outbox.lockAvailableBatchByEventType(EVENT_TYPE, now, BATCH_SIZE)) {
                reconcileOne(event, now);
            }
        });
    }

    private void reconcileOne(OutboxEvent event, Instant now) {
        PaymentStatus result;
        try {
            result = itemTx.execute(status -> orders.reconcile(event.getAggregateId()));
        } catch (RuntimeException failure) {
            event.recordFailure(reason(failure), now.plus(backoff(event.getAttemptCount())));
            return;
        }
        if (result == PaymentStatus.PENDING) {
            event.reschedule(now.plus(PENDING_RECHECK));
        } else {
            event.processed();
        }
    }

    private static Duration backoff(int attemptsSoFar) {
        Duration next = Duration.ofMinutes(1L << Math.min(attemptsSoFar, 5));
        return next.compareTo(MAX_BACKOFF) > 0 ? MAX_BACKOFF : next;
    }

    /** Só o código do erro: mensagens de exceção de adaptadores podem carregar dados do PSP. */
    private static String reason(RuntimeException failure) {
        return failure instanceof BusinessException business ? business.errorCode().name() : failure.getClass().getSimpleName();
    }
}
