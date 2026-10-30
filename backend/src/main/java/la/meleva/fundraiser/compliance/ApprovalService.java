package la.meleva.fundraiser.compliance;

import la.meleva.fundraiser.campaign.AdminGateMatrix;
import la.meleva.fundraiser.campaign.CampaignMode;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class ApprovalService {

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalDecisionRepository decisionRepository;

    public ApprovalService(ApprovalRequestRepository requestRepository,
                           ApprovalDecisionRepository decisionRepository) {
        this.requestRepository = requestRepository;
        this.decisionRepository = decisionRepository;
    }

    @Transactional
    public ApprovalRequest requestApproval(Long campaignId, GateType gateType, Long requestedBy) {
        var existing = requestRepository.findByCampaignIdAndGateType(campaignId, gateType);
        if (existing.isPresent() && existing.get().getStatus() == ApprovalStatus.PENDING) {
            return existing.get();
        }
        var request = new ApprovalRequest(campaignId, gateType, requestedBy);
        return requestRepository.save(request);
    }

    @Transactional
    public ApprovalDecision decide(Long requestId, Long reviewerUserId, String decision, String reason) {
        var request = requestRepository.findById(requestId)
                .orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);

        if (request.getRequestedBy().equals(reviewerUserId)) {
            throw ErrorCode.APPROVAL_SELF_APPROVAL.toException();
        }

        if ("APPROVED".equals(decision)) {
            request.approve();
        } else {
            request.reject();
        }
        requestRepository.save(request);

        var approvalDecision = new ApprovalDecision(requestId, reviewerUserId, decision, reason);
        return decisionRepository.save(approvalDecision);
    }

    @Transactional(readOnly = true)
    public boolean areAllGatesApproved(Long campaignId, CampaignMode mode) {
        Set<GateType> required = AdminGateMatrix.requiredGates(mode);
        if (required.isEmpty()) return true;

        List<ApprovalRequest> requests = requestRepository.findByCampaignId(campaignId);
        for (GateType gate : required) {
            boolean approved = requests.stream()
                    .anyMatch(r -> r.getGateType() == gate && r.getStatus() == ApprovalStatus.APPROVED);
            if (!approved) return false;
        }
        return true;
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequest> listByCampaign(Long campaignId) {
        return requestRepository.findByCampaignId(campaignId);
    }
}
