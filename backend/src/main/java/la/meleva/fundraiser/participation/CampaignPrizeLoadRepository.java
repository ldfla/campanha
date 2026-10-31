package la.meleva.fundraiser.participation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignPrizeLoadRepository extends JpaRepository<CampaignPrizeLoad, Long> {
    Optional<CampaignPrizeLoad> findByCampaignId(Long campaignId);
}
