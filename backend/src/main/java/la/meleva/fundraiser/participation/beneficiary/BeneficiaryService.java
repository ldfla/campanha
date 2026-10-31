package la.meleva.fundraiser.participation.beneficiary;

import java.util.List;
import java.util.Objects;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BeneficiaryService {
    private final CampaignRepository campaigns;
    private final BeneficiaryRepository beneficiaries;
    private final MembershipRepository memberships;
    private final UserRepository users;

    BeneficiaryService(CampaignRepository campaigns, BeneficiaryRepository beneficiaries,
                       MembershipRepository memberships, UserRepository users) {
        this.campaigns = campaigns;
        this.beneficiaries = beneficiaries;
        this.memberships = memberships;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<BeneficiaryView> list(String campaignId) {
        Objects.requireNonNull(campaignId, "campaignId");
        var campaign = campaigns.findByPublicId(campaignId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        return beneficiaries.findByCampaignIdAndActiveTrueOrderByIdAsc(campaign.getId(), PageRequest.of(0, 100))
                .stream().map(BeneficiaryService::view).toList();
    }

    @Transactional
    public BeneficiaryView create(Long actorId, String campaignId, String name) {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(campaignId, "campaignId");
        Objects.requireNonNull(name, "name");
        var campaign = campaigns.findLockedByPublicId(campaignId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        var actor = users.findById(actorId).filter(user -> user.isActive())
                .orElseThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED::toException);
        boolean allowed = memberships.findActiveByUserAndOrganization(actor.getId(), campaign.getOrganizationId()).stream()
                .anyMatch(membership -> membership.getRole() == UserRole.CAMPAIGN_MANAGER
                        || membership.getRole() == UserRole.SYSTEM_ADMIN);
        if (!allowed) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        if (!campaign.getStatus().isEditable()) throw ErrorCode.CAMPAIGN_NOT_EDITABLE.toException();
        if (name.isBlank() || name.strip().length() > 150) throw ErrorCode.VALIDATION_ERROR.toException();
        return view(beneficiaries.save(new BeneficiaryEntity(campaign.getId(), name)));
    }

    private static BeneficiaryView view(BeneficiaryEntity beneficiary) {
        return new BeneficiaryView(beneficiary.getPublicId(), beneficiary.getName());
    }

    public record BeneficiaryView(String id, String name) { }
}
