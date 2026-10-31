package la.meleva.fundraiser.award;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
public interface AwardDeliveryRepository extends JpaRepository<AwardDelivery,Long>{
 List<AwardDelivery> findByBeneficiaryUserId(Long beneficiaryUserId);
 Optional<AwardDelivery> findByOutcomeId(Long outcomeId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<AwardDelivery> findLockedById(Long id);
}
