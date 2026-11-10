package la.meleva.fundraiser.payment;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface FinancialExpenseRepository extends JpaRepository<FinancialExpense, Long> {
    Optional<FinancialExpense> findByCampaignIdAndIdempotencyKey(Long campaignId, String key);
    Optional<FinancialExpense> findByCampaignIdAndPublicId(Long campaignId, String publicId);
    Page<FinancialExpense> findAllByCampaignId(Long campaignId, Pageable pageable);
}
