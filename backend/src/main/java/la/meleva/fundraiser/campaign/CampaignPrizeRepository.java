package la.meleva.fundraiser.campaign;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignPrizeRepository extends JpaRepository<CampaignPrize, Long> {

    List<CampaignPrize> findByCampaignIdOrderBySortOrder(Long campaignId);

    List<CampaignPrize> findByCampaignIdAndStatus(Long campaignId, CampaignPrize.PrizeStatus status);
}
