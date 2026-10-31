package la.meleva.fundraiser.participation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignNumberBuyerRepository extends JpaRepository<CampaignNumberBuyer, Long> {
    Optional<CampaignNumberBuyer> findByNumberId(Long numberId);
    long countByCampaignId(Long campaignId);
}
