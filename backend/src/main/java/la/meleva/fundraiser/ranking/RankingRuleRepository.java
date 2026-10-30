package la.meleva.fundraiser.ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface RankingRuleRepository extends JpaRepository<RankingRule,Long>{Optional<RankingRule> findByCampaignIdAndRuleVersion(Long campaignId,int ruleVersion);}
