package la.meleva.fundraiser.drawing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface DrawRuleRepository extends JpaRepository<DrawRule,Long>{Optional<DrawRule> findByCampaignIdAndVersion(Long campaignId,int version); Optional<DrawRule> findTopByCampaignIdOrderByVersionDesc(Long campaignId);}
