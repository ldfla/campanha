package la.meleva.fundraiser.payment;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface FinancialScenarioRepository extends JpaRepository<FinancialScenario, Long> {
    Optional<FinancialScenario> findByPublicIdAndCampaignId(String publicId, Long campaignId);
    Page<FinancialScenario> findAllByCampaignId(Long campaignId, Pageable pageable);
}
