package la.meleva.fundraiser.compliance;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CampaignRegulationRepository extends JpaRepository<CampaignRegulation, Long> {
    Optional<CampaignRegulation> findByCampaignIdAndVersion(Long campaignId, int version);

    @Query("select coalesce(max(r.version), 0) from CampaignRegulation r where r.campaignId = :campaignId")
    int findHighestVersionByCampaignId(Long campaignId);
}
