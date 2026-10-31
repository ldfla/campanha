package la.meleva.fundraiser.ranking;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingPositionRepository extends JpaRepository<RankingPosition, RankingPosition.PositionId> {

    List<RankingPosition> findByIdSnapshotIdOrderByPosition(Long snapshotId);
}
