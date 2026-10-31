package la.meleva.fundraiser.ranking;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingSnapshotRepository extends JpaRepository<RankingSnapshot, Long> {

    List<RankingSnapshot> findByCampaignIdOrderByGeneratedAtDesc(Long campaignId);

    List<RankingSnapshot> findByCampaignIdAndScopeOrderByCutoffAtDesc(Long campaignId, RankingScope scope);

    Optional<RankingSnapshot> findByPublicIdAndCampaignId(String publicId, Long campaignId);

    Optional<RankingSnapshot> findByRankingRuleIdAndCutoffAt(Long rankingRuleId, Instant cutoffAt);
}
