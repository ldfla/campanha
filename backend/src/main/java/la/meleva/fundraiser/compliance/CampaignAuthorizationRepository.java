package la.meleva.fundraiser.compliance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CampaignAuthorizationRepository extends JpaRepository<CampaignAuthorization,Long>{List<CampaignAuthorization> findByCampaignId(Long campaignId);}
