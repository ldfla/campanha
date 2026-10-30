package la.meleva.fundraiser.participation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CampaignNumberRepository extends JpaRepository<CampaignNumber, Long> {
    Optional<CampaignNumber> findByCampaignIdAndSeriesCodeAndNumberValue(Long campaignId, String seriesCode, int numberValue);
    long countByCampaignIdAndStatus(Long campaignId, NumberStatus status);
}
