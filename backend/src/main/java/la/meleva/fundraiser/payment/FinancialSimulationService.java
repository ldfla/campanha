package la.meleva.fundraiser.payment;

import java.util.List;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.Membership;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialSimulationService {
    private final CampaignRepository campaigns;
    private final MembershipRepository memberships;
    private final UserRepository users;

    FinancialSimulationService(CampaignRepository campaigns, MembershipRepository memberships, UserRepository users) {
        this.campaigns = campaigns;
        this.memberships = memberships;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public FinancialSimulation.Result calculate(Long actorId, String campaignId,
                                                 FinancialSimulation.Scenario scenario) {
        var campaign = campaigns.findByPublicId(campaignId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        var actor = users.findById(actorId).filter(user -> user.isActive())
                .orElseThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED::toException);
        List<Membership> active = memberships.findActiveByUserAndOrganization(actor.getId(), campaign.getOrganizationId());
        boolean allowed = active.stream().anyMatch(membership -> membership.getRole() == UserRole.FINANCE
                || membership.getRole() == UserRole.CAMPAIGN_MANAGER
                || membership.getRole() == UserRole.SYSTEM_ADMIN);
        if (!allowed) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        try {
            return FinancialSimulation.calculate(scenario);
        } catch (IllegalArgumentException | ArithmeticException exception) {
            throw ErrorCode.VALIDATION_ERROR.toException();
        }
    }
}
