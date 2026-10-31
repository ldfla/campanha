package la.meleva.fundraiser.notification;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import la.meleva.fundraiser.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OutboxIsolationIntegrationTest {
    @Autowired OutboxEventRepository events;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;

    @Test
    @Transactional
    void unrelatedEventsCannotOccupyThePaymentWorkerBatch() {
        for (int i = 0; i < 105; i++) {
            events.save(new OutboxEvent("NOTIFICATION", (long) i + 1, "NOTIFICATION_REQUESTED", "{}"));
        }
        var payment = events.saveAndFlush(new OutboxEvent("PAYMENT", 1L, "PAYMENT_RECONCILIATION_REQUESTED", "{}"));
        var batch = events.lockAvailableBatchByEventType("PAYMENT_RECONCILIATION_REQUESTED", Instant.now().plusSeconds(5), 100);
        assertThat(batch).extracting(OutboxEvent::getId).contains(payment.getId());
        assertThat(batch).allMatch(event -> event.getEventType().equals("PAYMENT_RECONCILIATION_REQUESTED"));
    }

    @Test
    void concurrentWorkersClaimDisjointBatchesAndRetryRemainingEvents() throws Exception {
        String type = "NOTIFICATION_CONCURRENCY_TEST";
        var saved = events.saveAllAndFlush(java.util.stream.LongStream.rangeClosed(1, 20)
                .mapToObj(id -> new OutboxEvent("NOTIFICATION", id, type, "{}"))
                .toList());
        var bothClaimed = new CountDownLatch(2);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            java.util.concurrent.Callable<List<Long>> claim = () ->
                    new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status -> {
                        var batch = events.lockAvailableBatchByEventType(type, Instant.now().plusSeconds(5), 10);
                        bothClaimed.countDown();
                        try {
                            if (!bothClaimed.await(15, TimeUnit.SECONDS)) throw new IllegalStateException("Workers não iniciaram");
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(exception);
                        }
                        batch.forEach(OutboxEvent::processed);
                        return batch.stream().map(OutboxEvent::getId).toList();
                    });
            var first = executor.submit(claim);
            var second = executor.submit(claim);
            var firstIds = first.get(30, TimeUnit.SECONDS);
            var secondIds = second.get(30, TimeUnit.SECONDS);
            assertThat(firstIds).hasSizeLessThanOrEqualTo(10).doesNotHaveDuplicates();
            assertThat(secondIds).hasSizeLessThanOrEqualTo(10).doesNotHaveDuplicates();
            var allIds = new java.util.HashSet<>(firstIds);
            for (Long id : secondIds) assertThat(allIds.add(id)).isTrue();
            var remaining = new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status -> {
                var batch = events.lockAvailableBatchByEventType(type, Instant.now().plusSeconds(5), 20);
                batch.forEach(OutboxEvent::processed);
                return batch.stream().map(OutboxEvent::getId).toList();
            });
            for (Long id : remaining) assertThat(allIds.add(id)).isTrue();
            assertThat(allIds).containsExactlyInAnyOrderElementsOf(saved.stream().map(OutboxEvent::getId).toList());
        }
    }
}
