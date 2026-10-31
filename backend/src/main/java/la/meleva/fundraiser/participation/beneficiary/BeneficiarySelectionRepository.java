package la.meleva.fundraiser.participation.beneficiary;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiarySelectionRepository extends JpaRepository<BeneficiarySelection, Long> {
    Optional<BeneficiarySelection> findByRequestId(Long requestId);
}
