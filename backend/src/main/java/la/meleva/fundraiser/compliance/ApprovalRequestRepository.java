package la.meleva.fundraiser.compliance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {

    List<ApprovalRequest> findByCampaignId(Long campaignId);

    Optional<ApprovalRequest> findByCampaignIdAndGateType(Long campaignId, GateType gateType);

    List<ApprovalRequest> findByCampaignIdAndStatus(Long campaignId, ApprovalStatus status);
}
