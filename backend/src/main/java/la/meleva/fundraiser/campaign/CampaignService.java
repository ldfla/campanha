package la.meleva.fundraiser.campaign;

import la.meleva.fundraiser.campaign.dto.CreateCampaignRequest;
import la.meleva.fundraiser.campaign.dto.PrizeRequest;
import la.meleva.fundraiser.campaign.dto.UpdateCampaignRequest;
import la.meleva.fundraiser.shared.ErrorCode;
import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.identity.Membership;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.drawing.SurpriseCommitmentRepository;
import la.meleva.fundraiser.compliance.ApprovalService;
import la.meleva.fundraiser.compliance.AuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final CampaignPrizeRepository prizeRepository;
    private final MembershipRepository memberships;
    private final AuditEventRepository audits;
    private final SurpriseCommitmentRepository surpriseCommitments;
    private final ApprovalService approvals;
    private final AuthorizationService authorizations;

    public CampaignService(CampaignRepository campaignRepository,
                           CampaignPrizeRepository prizeRepository, MembershipRepository memberships, AuditEventRepository audits,
                           SurpriseCommitmentRepository surpriseCommitments, ApprovalService approvals,
                           AuthorizationService authorizations) {
        this.campaignRepository = campaignRepository;
        this.prizeRepository = prizeRepository;
        this.memberships = memberships;
        this.audits = audits;
        this.surpriseCommitments = surpriseCommitments;
        this.approvals = approvals;
        this.authorizations = authorizations;
    }

    @Transactional
    public Campaign create(CreateCampaignRequest request, Long organizationId) {
        if (campaignRepository.existsBySlug(request.slug())) {
            throw ErrorCode.CAMPAIGN_NOT_EDITABLE.toException();
        }
        var campaign = new Campaign(organizationId, request.title(), request.slug(), request.mode());
        if (request.description() != null) {
            campaign.setDescription(request.description());
        }
        return campaignRepository.save(campaign);
    }

    @Transactional
    public Campaign update(String publicId, UpdateCampaignRequest request) {
        var campaign = findByPublicId(publicId);
        if (request.title() != null) {
            campaign.setTitle(request.title());
        }
        if (request.slug() != null) {
            if (!campaign.getSlug().equals(request.slug()) && campaignRepository.existsBySlug(request.slug())) {
                throw ErrorCode.CAMPAIGN_NOT_EDITABLE.toException();
            }
            campaign.setSlug(request.slug());
        }
        if (request.description() != null) {
            campaign.setDescription(request.description());
        }
        if (request.startsAt() != null) {
            campaign.setStartsAt(request.startsAt());
        }
        if (request.endsAt() != null) {
            campaign.setEndsAt(request.endsAt());
        }
        return campaignRepository.save(campaign);
    }

    @Transactional
    public Campaign transition(Long actorId, String publicId, CampaignStatus targetStatus) {
        var campaign = campaignRepository.findLockedByPublicId(publicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        if (actorId == null || (!memberships.existsByUserIdAndOrganizationIdAndRoleAndStatus(actorId,campaign.getOrganizationId(),UserRole.CAMPAIGN_MANAGER,Membership.MembershipStatus.ACTIVE)
                && !memberships.existsByUserIdAndOrganizationIdAndRoleAndStatus(actorId,campaign.getOrganizationId(),UserRole.SYSTEM_ADMIN,Membership.MembershipStatus.ACTIVE))) {
            throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        }

        if ((targetStatus == CampaignStatus.LEGAL_REVIEW || targetStatus == CampaignStatus.SCHEDULED || targetStatus == CampaignStatus.OPEN)
                && campaign.isSurpriseEnabled() && !surpriseCommitments.existsByCampaignIdAndEncryptedSecretIsNotNull(campaign.getId())) {
            throw ErrorCode.CAMPAIGN_GATES_INCOMPLETE.toException();
        }
        if ((targetStatus == CampaignStatus.OPEN || targetStatus == CampaignStatus.SCHEDULED) && !campaign.getMode().isDemo()) {
            // ADR-0007: abertura exige TODOS os gates aprovados e, quando aplicável, autorização vigente.
            if (!approvals.areAllGatesApproved(campaign.getId(), campaign.getMode())) {
                throw ErrorCode.CAMPAIGN_GATES_INCOMPLETE.toException();
            }
            if (campaign.getMode().requiresAuthorization() && !authorizations.hasEffectiveAuthorization(campaign.getId())) {
                throw ErrorCode.CAMPAIGN_GATES_INCOMPLETE.toException();
            }
        }

        var oldStatus=campaign.getStatus();
        campaign.transitionTo(targetStatus);
        try {
            String hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest((publicId+":"+oldStatus+":"+targetStatus).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            audits.save(new AuditEvent(actorId,"CAMPAIGN_TRANSITIONED","campaign",campaign.getId(),hash));
        } catch (java.security.NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
        return campaignRepository.save(campaign);
    }

    @Transactional(readOnly = true)
    public Campaign findByPublicId(String publicId) {
        return campaignRepository.findByPublicId(publicId)
                .orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
    }

    @Transactional(readOnly = true)
    public Campaign findBySlug(String slug) {
        return campaignRepository.findBySlug(slug)
                .orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
    }

    @Transactional(readOnly = true)
    public List<Campaign> findByOrganization(Long organizationId) {
        return campaignRepository.findByOrganizationId(organizationId);
    }

    @Transactional
    public CampaignPrize addPrize(String campaignPublicId, PrizeRequest request) {
        var campaign = findByPublicId(campaignPublicId);

        if (request.type() == CampaignPrize.PrizeType.CASH && !campaign.getMode().allowsCashPrizes()) {
            throw ErrorCode.PRIZE_CASH_NOT_ALLOWED.toException();
        }

        var prize = new CampaignPrize(
                campaign.getId(),
                request.type(),
                request.description(),
                request.valueCents(),
                request.quantity(),
                request.sortOrder()
        );
        return prizeRepository.save(prize);
    }

    @Transactional(readOnly = true)
    public List<CampaignPrize> listPrizes(String campaignPublicId) {
        var campaign = findByPublicId(campaignPublicId);
        return prizeRepository.findByCampaignIdOrderBySortOrder(campaign.getId());
    }

    @Transactional
    public CampaignPrize updatePrize(Long prizeId, PrizeRequest request) {
        var prize = prizeRepository.findById(prizeId)
                .orElseThrow(ErrorCode.PRIZE_NOT_FOUND::toException);
        prize.update(request.description(), request.valueCents(), request.quantity(), request.sortOrder());
        return prizeRepository.save(prize);
    }

    @Transactional
    public void deactivatePrize(Long prizeId) {
        var prize = prizeRepository.findById(prizeId)
                .orElseThrow(ErrorCode.PRIZE_NOT_FOUND::toException);
        prize.deactivate();
        prizeRepository.save(prize);
    }
}
