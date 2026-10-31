package la.meleva.fundraiser.payment;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import la.meleva.fundraiser.notification.OutboxEventRepository;
import java.time.Instant;

/** Reconcilia pendências sem SKIP LOCKED; a seleção não é fila de sorteio/ranking. */
@Component
public class ReconciliationWorker {
    private final OutboxEventRepository outbox; private final OrderService orders;
    public ReconciliationWorker(OutboxEventRepository outbox,OrderService orders){this.outbox=outbox;this.orders=orders;}
    @Scheduled(fixedDelayString="${fundraiser.payment.reconciliation-delay-ms:300000}") @Transactional
    public void reconcilePending(){outbox.lockAvailableBatch(Instant.now(),100).stream().filter(e->e.getEventType().equals("PAYMENT_RECONCILIATION_REQUESTED")).forEach(e->{orders.reconcile(e.getAggregateId());e.processed();});}
}
