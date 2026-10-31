package la.meleva.fundraiser.drawing;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SurpriseCommitmentRepository extends JpaRepository<SurpriseCommitment,Long>{List<SurpriseCommitment> findByCampaignId(Long campaignId); boolean existsByCampaignIdAndEncryptedSecretIsNotNull(Long campaignId);}
