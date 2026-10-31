package la.meleva.fundraiser.drawing;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.identity.Membership;
import la.meleva.fundraiser.identity.MembershipRepository;
import la.meleva.fundraiser.identity.UserRole;
import la.meleva.fundraiser.identity.UserRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class SurpriseCommitmentService {
    private final CampaignRepository campaigns;
    private final SurpriseCommitmentRepository commitments;
    private final MembershipRepository memberships;
    private final UserRepository users;
    private final AuditEventRepository audits;
    private final SurpriseRevealApprovalRepository approvals;
    private final JdbcTemplate jdbc;
    private final String encodedKey;
    private final SecureRandom random = new SecureRandom();

    public SurpriseCommitmentService(CampaignRepository campaigns, SurpriseCommitmentRepository commitments, MembershipRepository memberships, UserRepository users, AuditEventRepository audits, SurpriseRevealApprovalRepository approvals, JdbcTemplate jdbc,
            @Value("${app.surprise.encryption-key:}") String encodedKey) {
        this.campaigns=campaigns; this.commitments=commitments; this.memberships=memberships; this.users=users; this.audits=audits; this.approvals=approvals; this.jdbc=jdbc; this.encodedKey=encodedKey;
    }

    @Transactional
    public PublishedCommitment commit(Long actorId, String campaignId, int ruleVersion) {
        var campaign=campaigns.findLockedByPublicId(campaignId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        requireMembership(actorId,campaign.getOrganizationId(),UserRole.CAMPAIGN_MANAGER,UserRole.SYSTEM_ADMIN);
        SurprisePrizePolicy.requireAllowed(campaign.getMode());
        if (campaign.getStatus()!=CampaignStatus.DRAFT || ruleVersion != 1 || !commitments.findByCampaignId(campaign.getId()).isEmpty()) throw ErrorCode.CAMPAIGN_OPERATION_BLOCKED.toException();
        campaign.enableSurprise();
        var secret=new SurpriseProof(random).generate(campaign.getNumberMin(),campaign.getNumberMax());
        var context=new SurpriseProof.Context(campaign.getPublicId(),ruleVersion,0,campaign.getNumberMin(),campaign.getNumberMax(),java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        var commitment=new SurpriseCommitment(campaign.getId(),SurpriseProof.commitment(secret,context),context,actorId);
        commitment.setEncryptedSecret(new SurpriseSecretCipher(encodedKey,random).encrypt(campaignId,secret)); commitments.saveAndFlush(commitment);
        audits.save(new AuditEvent(actorId,"SURPRISE_COMMITTED","surprise_commitment",commitment.getId(),commitment.getCommitmentSha256()));
        return publishedView(commitment,campaign.getPublicId());
    }

    @Transactional
    public void approveReveal(Long actorId,String campaignId,SurpriseRevealApproval.ApprovalRole role) {
        java.util.Objects.requireNonNull(role,"role");
        var campaign=campaigns.findLockedByPublicId(campaignId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        if(!campaign.getMode().isDemo()) throw ErrorCode.CAMPAIGN_NOT_AUTHORIZED.toException();
        if(campaign.getStatus()!=CampaignStatus.DRAWN && campaign.getStatus()!=CampaignStatus.AWARDS_PENDING && campaign.getStatus()!=CampaignStatus.CLOSED) throw ErrorCode.CAMPAIGN_OPERATION_BLOCKED.toException();
        requireMembership(actorId,campaign.getOrganizationId(),UserRole.valueOf(role.name()));
        requirePublishedEvidence(campaign.getId());
        var commitment=commitments.findByCampaignId(campaign.getId()).stream().findFirst().orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if(commitment.getCreatedBy()==null || commitment.getCreatedBy().equals(actorId)) throw ErrorCode.APPROVAL_SELF_APPROVAL.toException();
        var prior=approvals.findByCommitmentIdAndApproverUserId(commitment.getId(),actorId);
        if(prior.isPresent()) {
            if(prior.get().getRole()==role) return;
            throw ErrorCode.APPROVAL_SELF_APPROVAL.toException();
        }
        if(approvals.existsByCommitmentIdAndRole(commitment.getId(),role)) throw ErrorCode.APPROVAL_ALREADY_DECIDED.toException();
        approvals.save(new SurpriseRevealApproval(commitment.getId(),actorId,role));
        audits.save(new AuditEvent(actorId,"SURPRISE_REVEAL_REVIEWED","surprise_commitment",commitment.getId(),commitment.getCommitmentSha256()));
    }

    @Transactional
    public void enable(Long actorId,String campaignId) {
        var campaign=campaigns.findLockedByPublicId(campaignId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        requireMembership(actorId,campaign.getOrganizationId(),UserRole.CAMPAIGN_MANAGER,UserRole.SYSTEM_ADMIN);
        if(!campaign.getMode().isDemo()) throw ErrorCode.CAMPAIGN_NOT_AUTHORIZED.toException();
        if(!campaign.isSurpriseEnabled()) {
            campaign.enableSurprise();
            try { var hash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest((campaignId+":SURPRISE_ENABLED").getBytes(StandardCharsets.UTF_8))); audits.save(new AuditEvent(actorId,"SURPRISE_ENABLED","campaign",campaign.getId(),hash)); }
            catch(java.security.NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
        }
    }

    @Transactional
    public SurpriseProof.Secret reveal(Long actorId, String campaignId) {
        var campaign=campaigns.findLockedByPublicId(campaignId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        requireMembership(actorId,campaign.getOrganizationId(),UserRole.DRAW_AUDITOR,UserRole.SYSTEM_ADMIN);
        if (!campaign.getMode().isDemo()) throw ErrorCode.CAMPAIGN_NOT_AUTHORIZED.toException();
        if (campaign.getStatus()!=CampaignStatus.DRAWN && campaign.getStatus()!=CampaignStatus.AWARDS_PENDING && campaign.getStatus()!=CampaignStatus.CLOSED) throw ErrorCode.CAMPAIGN_OPERATION_BLOCKED.toException();
        requirePublishedEvidence(campaign.getId());
        var commitment=commitments.findByCampaignId(campaign.getId()).stream().findFirst().orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        var reviews=approvals.findByCommitmentId(commitment.getId());
        if(reviews.size()!=2 || reviews.stream().map(SurpriseRevealApproval::getApproverUserId).distinct().count()!=2 || commitment.getCreatedBy()==null || reviews.stream().anyMatch(review->commitment.getCreatedBy().equals(review.getApproverUserId()))) throw ErrorCode.CAMPAIGN_GATES_INCOMPLETE.toException();
        for(var review:reviews) requireMembership(review.getApproverUserId(),campaign.getOrganizationId(),UserRole.valueOf(review.getRole().name()));
        var secret=new SurpriseSecretCipher(encodedKey,random).decrypt(campaignId,commitment.getEncryptedSecret());
        if(!SurpriseProof.verify(secret,publishedView(commitment,campaignId).context(),commitment.getCommitmentSha256())) throw ErrorCode.DRAW_SURPRISE_COMMITMENT_FAILED.toException();
        if(!commitment.isRevealed()) {commitment.reveal();audits.save(new AuditEvent(actorId,"SURPRISE_REVEALED","surprise_commitment",commitment.getId(),commitment.getCommitmentSha256()));}
        return secret;
    }

    @Transactional(readOnly=true)
    public PublishedCommitment published(String campaignId) {
        var campaign=campaigns.findByPublicId(campaignId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        var commitment=commitments.findByCampaignId(campaign.getId()).stream().findFirst().orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        return publishedView(commitment,campaign.getPublicId());
    }

    /** Recalcula o compromisso a partir de um segredo revelado e confirma a igualdade. Não altera estado. */
    @Transactional(readOnly=true)
    public boolean verify(String campaignId,java.util.List<Integer> tier1,java.util.List<Integer> tier2,String saltHex) {
        java.util.Objects.requireNonNull(tier1,"tier1"); java.util.Objects.requireNonNull(tier2,"tier2"); java.util.Objects.requireNonNull(saltHex,"saltHex");
        var campaign=campaigns.findByPublicId(campaignId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        var commitment=commitments.findByCampaignId(campaign.getId()).stream().findFirst().orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if (commitment.getProofNumberMin()==null || commitment.getProofNumberMax()==null || commitment.getProofRegulationVersion()==null) throw ErrorCode.DRAW_SURPRISE_COMMITMENT_FAILED.toException();
        var context=new SurpriseProof.Context(campaign.getPublicId(),commitment.getRuleVersion(),commitment.getProofRegulationVersion(),commitment.getProofNumberMin(),commitment.getProofNumberMax(),commitment.getCommittedAt());
        return SurpriseProof.verify(new SurpriseProof.Secret(tier1,tier2,saltHex),context,commitment.getCommitmentSha256());
    }

    private PublishedCommitment publishedView(SurpriseCommitment commitment,String campaignId) {
        if(commitment.getProofNumberMin()==null || commitment.getProofNumberMax()==null || commitment.getProofRegulationVersion()==null) throw ErrorCode.DRAW_SURPRISE_COMMITMENT_FAILED.toException();
        return new PublishedCommitment(commitment.getCommitmentSha256(),new SurpriseProof.Context(campaignId,commitment.getRuleVersion(),commitment.getProofRegulationVersion(),commitment.getProofNumberMin(),commitment.getProofNumberMax(),commitment.getCommittedAt()),SurpriseProof.DEMO_RELEASE_POLICY,true);
    }

    private void requireMembership(Long actorId,Long organizationId,UserRole... roles) {
        if(actorId==null || users.lockForAuthorization(actorId).filter(la.meleva.fundraiser.identity.User::isActive).isEmpty()
                || memberships.lockActiveForAuthorization(actorId,organizationId,Arrays.asList(roles),Membership.MembershipStatus.ACTIVE,la.meleva.fundraiser.identity.User.UserStatus.ACTIVE).isEmpty()) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
    }

    private void requirePublishedEvidence(Long campaignId) {
        var rows=jdbc.queryForList("SELECT d.id FROM draws d JOIN federal_results f ON f.id=d.federal_result_id WHERE d.campaign_id=? AND d.status='PUBLISHED' AND f.verified_by_two=TRUE LIMIT 1 FOR SHARE",Long.class,campaignId);
        if(rows.isEmpty()) throw ErrorCode.CAMPAIGN_GATES_INCOMPLETE.toException();
    }

    public record PublishedCommitment(String hash,SurpriseProof.Context context,String releasePolicy,boolean simulation) {}
}
