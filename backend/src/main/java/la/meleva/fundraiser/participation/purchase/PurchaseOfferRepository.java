package la.meleva.fundraiser.participation.purchase;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOfferRepository extends JpaRepository<PurchaseOffer, Long> {
    Optional<PurchaseOffer> findByPublicIdAndCampaignIdAndActiveTrue(String publicId, Long campaignId);

    List<PurchaseOffer> findByCampaignIdAndActiveTrueOrderByIdAsc(Long campaignId, Pageable pageable);
}
