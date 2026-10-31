package la.meleva.fundraiser.participation.beneficiary;

import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeneficiaryRepository extends JpaRepository<BeneficiaryEntity, Long> {
    Optional<BeneficiaryEntity> findByPublicIdAndCampaignIdAndActiveTrue(String publicId, Long campaignId);
    List<BeneficiaryEntity> findByCampaignIdAndActiveTrueOrderByIdAsc(Long campaignId, Pageable pageable);
}
