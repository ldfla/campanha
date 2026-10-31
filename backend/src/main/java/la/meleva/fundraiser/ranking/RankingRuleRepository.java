package la.meleva.fundraiser.ranking;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingRuleRepository extends JpaRepository<RankingRule, Long> {

    Optional<RankingRule> findByCampaignIdAndScopeAndRuleVersion(Long campaignId, RankingScope scope, int ruleVersion);

    Optional<RankingRule> findTopByCampaignIdAndScopeOrderByRuleVersionDesc(Long campaignId, RankingScope scope);

    Optional<RankingRule> findTopByCampaignIdAndScopeAndEffectiveAtLessThanEqualOrderByRuleVersionDesc(
            Long campaignId, RankingScope scope, Instant instant);

    List<RankingRule> findByCampaignIdAndScopeOrderByRuleVersionDesc(Long campaignId, RankingScope scope);
}
