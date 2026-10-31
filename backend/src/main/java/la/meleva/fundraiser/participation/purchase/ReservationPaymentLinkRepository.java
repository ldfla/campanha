package la.meleva.fundraiser.participation.purchase;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationPaymentLinkRepository extends JpaRepository<ReservationPaymentLink, Long> {
    Optional<ReservationPaymentLink> findByRequestId(Long requestId);
}
