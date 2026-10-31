package la.meleva.fundraiser.compliance;

import java.time.Instant;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplianceManagementService {
    private final CampaignRepository campaignRepository;
    private final CampaignRegulationRepository regulationRepository;
    private final CampaignAuthorizationRepository authorizationRepository;
    private final MembershipRepository membershipRepository;

    ComplianceManagementService(CampaignRepository campaignRepository, CampaignRegulationRepository regulationRepository,
                                CampaignAuthorizationRepository authorizationRepository, MembershipRepository membershipRepository) {
        this.campaignRepository = campaignRepository;
        this.regulationRepository = regulationRepository;
        this.authorizationRepository = authorizationRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public CampaignRegulation createRegulation(Long actorId, String campaignPublicId, String sha256, String documentUri) {
        validateSha256(sha256);
        Campaign campaign = campaign(campaignPublicId, actorId);
        int version = regulationRepository.findHighestVersionByCampaignId(campaign.getId()) + 1;
        CampaignRegulation regulation = regulationRepository.save(
                new CampaignRegulation(campaign.getId(), version, sha256, documentUri));
        campaign.setCurrentRegulationId(regulation.getId());
        return regulation;
    }

    @Transactional
    public CampaignAuthorization createAuthorization(Long actorId, String campaignPublicId, String issuer,
                                                      String certificateNo, Instant validFrom, Instant validTo,
                                                      String documentUri) {
        if (issuer == null || issuer.isBlank() || certificateNo == null || certificateNo.isBlank()) {
            throw ErrorCode.VALIDATION_ERROR.toException();
        }
        Campaign campaign = campaign(campaignPublicId, actorId);
        return authorizationRepository.save(new CampaignAuthorization(campaign.getId(), issuer, certificateNo,
                validFrom, validTo, documentUri));
    }

    private Campaign campaign(String publicId, Long actorId) {
        Campaign campaign = campaignRepository.findByPublicId(publicId)
                .orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        if (membershipRepository.findActiveByUserAndOrganization(actorId, campaign.getOrganizationId()).isEmpty()) {
            throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        }
        return campaign;
    }

    private void validateSha256(String value) {
        if (value == null || !value.matches("^[a-fA-F0-9]{64}$")) throw ErrorCode.VALIDATION_ERROR.toException();
    }
}
