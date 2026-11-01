package la.meleva.fundraiser.drawing;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
public interface FederalResultRepository extends JpaRepository<FederalResult,Long>{
    Optional<FederalResult> findByContestIdentifier(String contestIdentifier);
    @Lock(LockModeType.PESSIMISTIC_WRITE) Optional<FederalResult> findLockedById(Long id);
}
