package la.meleva.fundraiser.payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun,Long>{List<ReconciliationRun> findByProviderOrderByStartedAtDesc(String provider);}
