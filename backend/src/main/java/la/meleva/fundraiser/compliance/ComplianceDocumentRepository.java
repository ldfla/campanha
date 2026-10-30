package la.meleva.fundraiser.compliance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ComplianceDocumentRepository extends JpaRepository<ComplianceDocument,Long>{List<ComplianceDocument> findByCampaignIdAndClassification(Long campaignId,String classification);}
