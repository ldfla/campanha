package la.meleva.fundraiser.participation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EligibilityEventRepository extends JpaRepository<EligibilityEvent, Long> { List<EligibilityEvent> findByEntryIdOrderByCheckedAt(Long entryId); }
