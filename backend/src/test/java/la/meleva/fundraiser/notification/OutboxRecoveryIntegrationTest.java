package la.meleva.fundraiser.notification;

import java.time.Instant;
import la.meleva.fundraiser.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class OutboxRecoveryIntegrationTest {

    @Autowired OutboxEventRepository outbox;

    @Test
    void transientFailureReschedulesAndEventuallyDeadLetters() {
        var event = outbox.saveAndFlush(new OutboxEvent("USER", 1L, "NOTIFICATION_ACCOUNT_REGISTERED_V1", "{}"));
        Instant later = Instant.now().plusSeconds(300);

        event.recordFailure("timeout transitório do provedor", later);
        outbox.saveAndFlush(event);

        assertThat(event.getStatus()).isEqualTo("PENDING");
        assertThat(event.getAttemptCount()).isEqualTo(1);
        assertThat(event.getAvailableAt()).isAfter(Instant.now());
        // Reagendado: não é elegível antes de available_at (recuperação sem reprocessar cedo demais).
        assertThat(outbox.lockAvailableBatch(Instant.now(), 50))
                .noneMatch(candidate -> candidate.getId().equals(event.getId()));

        for (int attempt = 1; attempt < event.getMaxAttempts(); attempt++) {
            event.recordFailure("falha persistente", later);
        }
        outbox.saveAndFlush(event);
        assertThat(event.isExhausted()).isTrue();
        assertThat(event.getStatus()).isEqualTo("DEAD_LETTER");
    }

    @Test
    void processedEventIsNotReprocessed() {
        var event = outbox.saveAndFlush(new OutboxEvent("USER", 1L, "NOTIFICATION_ACCOUNT_REGISTERED_V1", "{}"));
        event.processed();
        outbox.saveAndFlush(event);
        assertThat(event.getStatus()).isEqualTo("PROCESSED");
        assertThat(outbox.lockAvailableBatch(Instant.now(), 50))
                .noneMatch(candidate -> candidate.getId().equals(event.getId()));
    }
}
