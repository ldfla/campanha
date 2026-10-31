package la.meleva.fundraiser.award;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import la.meleva.fundraiser.audit.AuditEvent;
import la.meleva.fundraiser.audit.AuditEventRepository;
import la.meleva.fundraiser.campaign.Campaign;
import la.meleva.fundraiser.campaign.CampaignRepository;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operações internas simuladas; não cria instruções de pagamento ou resultados de apuração. */
@Service
public class AwardDeliveryService {
    private static final Pattern VERIFIED_PHONE=Pattern.compile("^\\+?[1-9]\\d{10,14}$");
    private final CampaignRepository campaigns; private final AwardDeliveryRepository deliveries;
    private final AuditEventRepository audits; private final JdbcTemplate jdbc; private final Validator validator;
    public AwardDeliveryService(CampaignRepository campaigns,AwardDeliveryRepository deliveries,AuditEventRepository audits,JdbcTemplate jdbc,Validator validator){
        this.campaigns=campaigns;this.deliveries=deliveries;this.audits=audits;this.jdbc=jdbc;this.validator=validator;
    }

    @Transactional
    public AwardDelivery createSimulation(Long actorId,String campaignId,Long outcomeId){
        if(outcomeId==null) throw ErrorCode.VALIDATION_ERROR.toException();
        Campaign campaign=authorizedCampaign(actorId,campaignId);
        if(!campaign.getMode().isDemo()) throw ErrorCode.CAMPAIGN_NOT_AUTHORIZED.toException();
        Long beneficiary=eligibleBeneficiary(campaign.getId(),outcomeId);
        var existing=deliveries.findByOutcomeId(outcomeId); if(existing.isPresent()) {
            if(!existing.get().getBeneficiaryUserId().equals(beneficiary)) throw ErrorCode.PARTICIPATION_NOT_ELIGIBLE.toException();
            return existing.get();
        }
        var delivery=deliveries.saveAndFlush(new AwardDelivery(outcomeId,beneficiary));
        audit(actorId,delivery,"AWARD_SIMULATION_CREATED"); return delivery;
    }

    @Transactional
    public AwardDelivery validateBeneficiary(Long actorId,String campaignId,Long deliveryId){
        var delivery=authorizedDelivery(actorId,campaignId,deliveryId);
        requireVerifiedContact(delivery.getBeneficiaryUserId());
        delivery.transitionTo(AwardDeliveryStatus.VALIDATED); audit(actorId,delivery,"AWARD_BENEFICIARY_VALIDATED"); return delivery;
    }

    @Transactional
    public AwardDelivery schedule(Long actorId,String campaignId,Long deliveryId){
        var delivery=authorizedDelivery(actorId,campaignId,deliveryId);
        requireVerifiedContact(delivery.getBeneficiaryUserId());
        delivery.transitionTo(AwardDeliveryStatus.SCHEDULED); audit(actorId,delivery,"AWARD_SIMULATION_SCHEDULED"); return delivery;
    }

    private AwardDelivery authorizedDelivery(Long actorId,String campaignId,Long deliveryId){
        if(deliveryId==null) throw ErrorCode.VALIDATION_ERROR.toException();
        var campaign=authorizedCampaign(actorId,campaignId);
        if(!campaign.getMode().isDemo()) throw ErrorCode.CAMPAIGN_NOT_AUTHORIZED.toException();
        var delivery=deliveries.findLockedById(deliveryId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        if(!delivery.getBeneficiaryUserId().equals(eligibleBeneficiary(campaign.getId(),delivery.getOutcomeId()))) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        return delivery;
    }

    private Campaign authorizedCampaign(Long actorId,String publicId){
        if(actorId==null || publicId==null || publicId.isBlank()) throw ErrorCode.VALIDATION_ERROR.toException();
        var campaign=campaigns.findLockedByPublicId(publicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        if(jdbc.queryForList("SELECT id FROM users WHERE id=? AND status='ACTIVE' FOR SHARE",Long.class,actorId).isEmpty()
                || jdbc.queryForList("SELECT id FROM memberships WHERE user_id=? AND organization_id=? AND status='ACTIVE' AND role IN ('CAMPAIGN_MANAGER','SYSTEM_ADMIN') FOR SHARE",Long.class,actorId,campaign.getOrganizationId()).isEmpty()) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        return campaign;
    }

    private Long eligibleBeneficiary(Long campaignId,Long outcomeId){
        // Contrato interno de fixture DEMO; não constitui regra de apuração real nem substitui a P12.
        var beneficiaries=jdbc.queryForList("SELECT e.user_id FROM draw_outcomes o JOIN draws d ON d.id=o.draw_id JOIN federal_results f ON f.id=d.federal_result_id JOIN participation_entries e ON e.id=o.entry_id AND e.campaign_id=d.campaign_id JOIN entry_number_assignments a ON a.entry_id=e.id JOIN campaign_numbers n ON n.id=a.campaign_number_id AND n.entry_id=e.id AND n.campaign_id=d.campaign_id AND n.number_value=o.winning_number JOIN campaign_prizes p ON p.id=o.prize_id AND p.campaign_id=d.campaign_id WHERE o.id=? AND d.campaign_id=? AND o.decision_code='DEMO_MATCH' AND d.status='PUBLISHED' AND f.verified_by_two=TRUE AND e.status='FROZEN' AND e.source='DEMO_SIMULATION' AND n.status='FROZEN' AND p.status='ACTIVE' AND p.type<>'CASH' FOR SHARE",Long.class,outcomeId,campaignId);
        if(beneficiaries.size()!=1) throw ErrorCode.PARTICIPATION_NOT_ELIGIBLE.toException();
        return beneficiaries.getFirst();
    }
    private void requireVerifiedContact(Long beneficiaryId){
        var verified=jdbc.query("SELECT phone_number,phone_verified_at,email_normalized,email_verified_at FROM users WHERE id=? AND status='ACTIVE' FOR SHARE",
                (rs,row)->(rs.getTimestamp(2)!=null && validPhone(rs.getString(1))) || (rs.getTimestamp(4)!=null && validEmail(rs.getString(3))),beneficiaryId);
        if(verified.size()!=1 || !verified.getFirst()) throw ErrorCode.PARTICIPATION_NOT_ELIGIBLE.toException();
    }
    private boolean validPhone(String value){return value!=null && VERIFIED_PHONE.matcher(value).matches();}
    private boolean validEmail(String value){return value!=null && validator.validate(new VerifiedEmail(value)).isEmpty();}
    private record VerifiedEmail(@NotBlank @Email @Size(max=320) String value) {}
    private void audit(Long actorId,AwardDelivery delivery,String action){
        try {var hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((action+":"+delivery.getId()+":"+delivery.getStatus()).getBytes(StandardCharsets.UTF_8)));audits.save(new AuditEvent(actorId,action,"award_delivery",delivery.getId(),hash));}
        catch(NoSuchAlgorithmException ex){throw new IllegalStateException("award.audit.hash.unavailable",ex);}
    }
}
