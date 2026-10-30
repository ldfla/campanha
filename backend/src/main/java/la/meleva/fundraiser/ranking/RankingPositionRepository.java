package la.meleva.fundraiser.ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RankingPositionRepository extends JpaRepository<RankingPosition,RankingPosition.PositionId>{List<RankingPosition> findByIdSnapshotIdOrderByPosition(Long snapshotId);}
