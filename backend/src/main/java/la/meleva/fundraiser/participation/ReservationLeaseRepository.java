package la.meleva.fundraiser.participation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
public interface ReservationLeaseRepository extends JpaRepository<ReservationLease, Long> { List<ReservationLease> findByStatusAndExpiresAtBefore(String status, Instant expiresAt); }
