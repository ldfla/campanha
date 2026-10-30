package la.meleva.fundraiser.payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface FinancialLedgerEntryRepository extends JpaRepository<FinancialLedgerEntry,Long>{List<FinancialLedgerEntry> findByCampaignId(Long campaignId);}
