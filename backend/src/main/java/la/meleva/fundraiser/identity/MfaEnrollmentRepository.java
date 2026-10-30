package la.meleva.fundraiser.identity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface MfaEnrollmentRepository extends JpaRepository<MfaEnrollment, Long> { Optional<MfaEnrollment> findByUserId(Long userId); }
