package la.meleva.fundraiser.notification;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Instant;
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
}
