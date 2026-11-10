package la.meleva.fundraiser.payment;

import java.util.Optional;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface FinancialExpenseReversalRepository extends JpaRepository<FinancialExpenseReversal, Long> {
    Optional<FinancialExpenseReversal> findByExpenseId(Long expenseId);
    Optional<FinancialExpenseReversal> findByCampaignIdAndIdempotencyKey(Long campaignId, String key);
    @Query("select r.expenseId from FinancialExpenseReversal r where r.expenseId in :ids")
    List<Long> findReversedExpenseIds(Collection<Long> ids);
}
