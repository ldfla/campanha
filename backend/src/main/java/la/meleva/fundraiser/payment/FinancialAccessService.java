package la.meleva.fundraiser.payment;

import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.stereotype.Component;

@Component
class FinancialAccessService {
    private final CampaignRepository campaigns;
    private final MembershipRepository memberships;
    private final UserRepository users;

    FinancialAccessService(CampaignRepository campaigns, MembershipRepository memberships, UserRepository users) {
        this.campaigns = campaigns;
        this.memberships = memberships;
        this.users = users;
    }

    Campaign requireAccess(Long actorId, String campaignId) {
        var campaign = campaigns.findByPublicId(campaignId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        var actor = users.findById(actorId).filter(user -> user.isActive())
                .orElseThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED::toException);
        boolean allowed = memberships.findActiveByUserAndOrganization(actor.getId(), campaign.getOrganizationId())
                .stream().anyMatch(membership -> membership.getRole() == UserRole.FINANCE
                        || membership.getRole() == UserRole.CAMPAIGN_MANAGER
                        || membership.getRole() == UserRole.SYSTEM_ADMIN);
        if (!allowed) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        return campaign;
    }
}
