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
import org.springframework.context.ApplicationEventPublisher;
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

    private final ApplicationEventPublisher events;

    public CampaignService(CampaignRepository campaignRepository,
                           CampaignPrizeRepository prizeRepository, MembershipRepository memberships, AuditEventRepository audits,
                           SurpriseCommitmentRepository surpriseCommitments, ApprovalService approvals,
                           AuthorizationService authorizations, ApplicationEventPublisher events) {
        this.events = events;
        this.campaignRepository = campaignRepository;
        this.prizeRepository = prizeRepository;
        this.memberships = memberships;
        this.audits = audits;
        this.surpriseCommitments = surpriseCommitments;
        this.approvals = approvals;
        this.authorizations = authorizations;
    }

    @Transactional
    public Campaign create(CreateCampaignRequest request, Long actorId) {
        Long organizationId = managedOrganization(actorId, request.organizationId());
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
    public Campaign update(Long actorId, String publicId, UpdateCampaignRequest request) {
        var campaign = editableCampaign(actorId, publicId);
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
        events.publishEvent(new CampaignStatusChangedEvent(campaign.getId(), oldStatus, targetStatus));
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
    public CampaignPrize addPrize(Long actorId, String campaignPublicId, PrizeRequest request) {
        var campaign = editableCampaign(actorId, campaignPublicId);

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
        assignRankingPosition(prize, request);
        return prizeRepository.save(prize);
    }

    @Transactional(readOnly = true)
    public List<CampaignPrize> listPrizes(String campaignPublicId) {
        var campaign = findByPublicId(campaignPublicId);
        return prizeRepository.findByCampaignIdOrderBySortOrder(campaign.getId());
    }

    @Transactional
    public CampaignPrize updatePrize(Long actorId, String campaignPublicId, Long prizeId, PrizeRequest request) {
        var prize = prizeOf(editableCampaign(actorId, campaignPublicId), prizeId);
        prize.update(request.description(), request.valueCents(), request.quantity(), request.sortOrder());
        assignRankingPosition(prize, request);
        return prizeRepository.save(prize);
    }

    @Transactional
    public void deactivatePrize(Long actorId, String campaignPublicId, Long prizeId) {
        var prize = prizeOf(editableCampaign(actorId, campaignPublicId), prizeId);
        prize.deactivate();
        prizeRepository.save(prize);
    }

    private static final List<UserRole> MANAGER_ROLES = List.of(UserRole.CAMPAIGN_MANAGER, UserRole.SYSTEM_ADMIN);

    /** P20-F25: escrita exige papel de gestão na organização dona da campanha e campanha ainda editável. */
    private Campaign editableCampaign(Long actorId, String publicId) {
        var campaign = campaignRepository.findLockedByPublicId(publicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        if (actorId == null || memberships.findActiveByUserAndOrganization(actorId, campaign.getOrganizationId()).stream()
                .noneMatch(membership -> MANAGER_ROLES.contains(membership.getRole()))) {
            throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        }
        if (!campaign.getStatus().isEditable()) throw ErrorCode.CAMPAIGN_NOT_EDITABLE.toException();
        return campaign;
    }

    private Long managedOrganization(Long actorId, String organizationPublicId) {
        var managed = actorId == null ? List.<Membership>of() : memberships.findActiveByUserId(actorId).stream()
                .filter(membership -> MANAGER_ROLES.contains(membership.getRole())).toList();
        var organizations = managed.stream().map(Membership::getOrganization).distinct().toList();
        if (organizationPublicId != null && !organizationPublicId.isBlank()) {
            return organizations.stream().filter(org -> org.getPublicId().equals(organizationPublicId)).findFirst()
                    .orElseThrow(ErrorCode.AUTH_RESOURCE_ACCESS_DENIED::toException).getId();
        }
        if (organizations.isEmpty()) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        if (organizations.size() > 1) throw ErrorCode.VALIDATION_ERROR.toException();
        return organizations.getFirst().getId();
    }

    /** Prêmio de outra campanha responde como inexistente, sem revelar que o id existe. */
    private CampaignPrize prizeOf(Campaign campaign, Long prizeId) {
        return prizeRepository.findById(prizeId).filter(prize -> prize.getCampaignId().equals(campaign.getId()))
                .orElseThrow(ErrorCode.PRIZE_NOT_FOUND::toException);
    }

    private void assignRankingPosition(CampaignPrize prize, PrizeRequest request) {
        if ((request.rankingScope() == null) != (request.rankingPosition() == null)) {
            throw ErrorCode.VALIDATION_ERROR.toException();
        }
        boolean taken = request.rankingScope() != null && prizeRepository.findByCampaignIdOrderBySortOrder(prize.getCampaignId()).stream()
                .anyMatch(other -> !java.util.Objects.equals(other.getId(), prize.getId())
                        && other.getStatus() == CampaignPrize.PrizeStatus.ACTIVE
                        && other.getRankingScope() == request.rankingScope()
                        && request.rankingPosition().equals(other.getRankingPosition()));
        if (taken) throw ErrorCode.VALIDATION_ERROR.toException();
        prize.assignRankingPosition(request.rankingScope(), request.rankingPosition());
    }
}
