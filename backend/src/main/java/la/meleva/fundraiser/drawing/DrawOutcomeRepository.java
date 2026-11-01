package la.meleva.fundraiser.drawing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DrawOutcomeRepository extends JpaRepository<DrawOutcome,Long>{
    List<DrawOutcome> findByDrawId(Long drawId);
    void deleteByDrawId(Long drawId);
}
