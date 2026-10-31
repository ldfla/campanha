package la.meleva.fundraiser.participation;

import java.time.Instant;
import la.meleva.fundraiser.notification.OutboxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Consome somente eventos de expiração; o bloqueio da campanha protege o estoque. */
@Component
public class ReservationExpiryWorker {
    private final OutboxEventRepository outbox;
    private final ReservationService reservations;

    ReservationExpiryWorker(OutboxEventRepository outbox, ReservationService reservations) {
        this.outbox = outbox; this.reservations = reservations;
    }

    @Scheduled(fixedDelayString = "${fundraiser.reservation.expiry-delay-ms:30000}")
    @Transactional
    public void expirePending() {
        Instant now = Instant.now();
        outbox.lockAvailableBatchByEventType("RESERVATION_EXPIRY_REQUESTED", now, 1).stream().findFirst()
                .ifPresent(event -> {
                    if (reservations.expireByRequestId(event.getAggregateId(), now)) event.processed();
                });
    }
}
