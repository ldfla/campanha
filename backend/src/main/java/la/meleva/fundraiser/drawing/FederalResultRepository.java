package la.meleva.fundraiser.drawing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface FederalResultRepository extends JpaRepository<FederalResult,Long>{Optional<FederalResult> findByContestIdentifier(String contestIdentifier);}
