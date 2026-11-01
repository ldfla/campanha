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
import la.meleva.fundraiser.campaign.CampaignStatus;
import la.meleva.fundraiser.ranking.RankingScope;
import la.meleva.fundraiser.ranking.RankingService;
import la.meleva.fundraiser.ranking.dto.CreateSnapshotRequest;
import la.meleva.fundraiser.shared.ErrorCode;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Operações internas simuladas; não cria instruções de pagamento ou resultados de apuração. */
@Service
public class AwardDeliveryService {
    private static final Pattern VERIFIED_PHONE=Pattern.compile("^\\+?[1-9]\\d{10,14}$");
    private static final java.util.Set<CampaignStatus> RANKING_AWARD_WINDOW = java.util.EnumSet.of(
            CampaignStatus.LOCKED, CampaignStatus.DRAW_PENDING, CampaignStatus.DRAWN, CampaignStatus.AWARDS_PENDING);
    private final CampaignRepository campaigns; private final AwardDeliveryRepository deliveries;
    private final AuditEventRepository audits; private final JdbcTemplate jdbc; private final Validator validator;
    private final RankingService rankings; private final ApplicationEventPublisher events;
    public AwardDeliveryService(CampaignRepository campaigns,AwardDeliveryRepository deliveries,AuditEventRepository audits,JdbcTemplate jdbc,Validator validator,RankingService rankings,ApplicationEventPublisher events){
        this.campaigns=campaigns;this.deliveries=deliveries;this.audits=audits;this.jdbc=jdbc;this.validator=validator;this.rankings=rankings;this.events=events;
    }

    /**
     * P05A-09: gera, uma única vez por escopo, as entregas das posições premiadas a partir de um snapshot tirado
     * e verificado aqui, com a campanha já congelada — o operador não escolhe snapshot nem posição.
     * Prêmio em dinheiro não gera entrega (AwardTransferPolicy) e fica listado como bloqueado.
     */
    @Transactional
    public RankingAwardResult createRankingSimulations(Long actorId,String campaignId,RankingScope scope){
        if(scope==null) throw ErrorCode.VALIDATION_ERROR.toException();
        Campaign campaign=authorizedCampaign(actorId,campaignId);
        if(!campaign.getMode().isDemo()) throw ErrorCode.CAMPAIGN_NOT_AUTHORIZED.toException();
        if(!RANKING_AWARD_WINDOW.contains(campaign.getStatus())) throw ErrorCode.RANKING_AWARD_REQUIRES_LOCK.toException();
        var previous=jdbc.queryForList("SELECT s.public_id FROM award_deliveries d JOIN ranking_snapshots s ON s.id=d.ranking_snapshot_id "
                + "WHERE s.campaign_id=? AND s.scope=? LIMIT 1",String.class,campaign.getId(),scope.name());
        String snapshotPublicId=previous.isEmpty()
                ? rankings.snapshot(actorId,campaignId,new CreateSnapshotRequest(scope,null)).snapshotId()
                : previous.getFirst();
        rankings.export(actorId,campaignId,snapshotPublicId);
        Long snapshotId=jdbc.queryForObject("SELECT id FROM ranking_snapshots WHERE public_id=? AND campaign_id=?",Long.class,snapshotPublicId,campaign.getId());
        var prizes=jdbc.query("SELECT id,type,ranking_position FROM campaign_prizes WHERE campaign_id=? AND ranking_scope=? AND status='ACTIVE' ORDER BY ranking_position",
                (rs,row)->new RankingPrize(rs.getLong(1),rs.getString(2),rs.getInt(3)),campaign.getId(),scope.name());
        var created=new java.util.ArrayList<RankingAward>(); var vacant=new java.util.ArrayList<Integer>(); var cashBlocked=new java.util.ArrayList<Integer>();
        for(RankingPrize prize:prizes){
            if("CASH".equalsIgnoreCase(prize.type())){cashBlocked.add(prize.position());continue;}
            var winner=jdbc.queryForList("SELECT user_id FROM ranking_positions WHERE snapshot_id=? AND position=?",Long.class,snapshotId,prize.position());
            if(winner.isEmpty()){vacant.add(prize.position());continue;}
            var delivery=deliveries.findByRankingSnapshotIdAndRankingPosition(snapshotId,prize.position()).orElseGet(()->{
                var saved=deliveries.saveAndFlush(AwardDelivery.forRankingPosition(snapshotId,prize.position(),prize.id(),winner.getFirst()));
                audit(actorId,saved,"AWARD_RANKING_SIMULATION_CREATED"); return saved;});
            created.add(new RankingAward(prize.position(),delivery.getId()));
        }
        return new RankingAwardResult(snapshotPublicId,scope,created,vacant,cashBlocked);
    }

    @Transactional
    public AwardDelivery createSimulation(Long actorId,String campaignId,Long outcomeId){
        if(outcomeId==null) throw ErrorCode.VALIDATION_ERROR.toException();
        Campaign campaign=authorizedCampaign(actorId,campaignId);
        if(!campaign.getMode().isDemo()) throw ErrorCode.CAMPAIGN_NOT_AUTHORIZED.toException();
        AwardTransferPolicy.requireNonCash(prizeTypeFor(campaign.getId(),outcomeId));
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

    @Transactional
    public AwardDelivery deliver(Long actorId,String campaignId,Long deliveryId,String evidenceUri){
        var delivery=authorizedDelivery(actorId,campaignId,deliveryId);
        requireVerifiedContact(delivery.getBeneficiaryUserId());
        if(evidenceUri==null || evidenceUri.isBlank()) throw ErrorCode.VALIDATION_ERROR.toException();
        delivery.deliver(evidenceUri.strip(),sha256(evidenceUri.strip()),java.time.Instant.now());
        audit(actorId,delivery,"AWARD_SIMULATION_DELIVERED"); return delivery;
    }

    @Transactional
    public AwardDelivery dispute(Long actorId,String campaignId,Long deliveryId,String reason){
        var delivery=authorizedDelivery(actorId,campaignId,deliveryId);
        if(reason==null || reason.isBlank() || reason.strip().length()>500) throw ErrorCode.VALIDATION_ERROR.toException();
        delivery.dispute(java.time.Instant.now(),reason);
        audit(actorId,delivery,"AWARD_SIMULATION_DISPUTED"); return delivery;
    }

    @Transactional(readOnly=true)
    public DeliverySummary summary(Long actorId,String campaignId){
        if(actorId==null || campaignId==null || campaignId.isBlank()) throw ErrorCode.VALIDATION_ERROR.toException();
        var campaign=campaigns.findByPublicId(campaignId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        if(!campaign.getMode().isDemo()) throw ErrorCode.CAMPAIGN_NOT_AUTHORIZED.toException();
        Integer active=jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE id=? AND status='ACTIVE'",Integer.class,actorId);
        Integer authorized=jdbc.queryForObject("SELECT COUNT(*) FROM memberships WHERE user_id=? AND organization_id=? AND status='ACTIVE' AND role IN ('CAMPAIGN_MANAGER','SYSTEM_ADMIN')",Integer.class,actorId,campaign.getOrganizationId());
        if(active==null || active!=1 || authorized==null || authorized!=1) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        var rows=jdbc.query("SELECT d.status, COUNT(*) FROM award_deliveries d "
                        + "LEFT JOIN draw_outcomes o ON o.id=d.outcome_id LEFT JOIN draws dr ON dr.id=o.draw_id "
                        + "LEFT JOIN ranking_snapshots s ON s.id=d.ranking_snapshot_id "
                        + "WHERE dr.campaign_id=? OR s.campaign_id=? GROUP BY d.status ORDER BY d.status",
                (rs,row)->new StatusCount(rs.getString(1),rs.getLong(2)),campaign.getId(),campaign.getId());
        return new DeliverySummary(campaignId,rows,"Resumo sem dados pessoais; entrega exclusivamente simulada.");
    }

    private AwardDelivery authorizedDelivery(Long actorId,String campaignId,Long deliveryId){
        if(deliveryId==null) throw ErrorCode.VALIDATION_ERROR.toException();
        var campaign=authorizedCampaign(actorId,campaignId);
        if(!campaign.getMode().isDemo()) throw ErrorCode.CAMPAIGN_NOT_AUTHORIZED.toException();
        var delivery=deliveries.findLockedById(deliveryId).orElseThrow(ErrorCode.RESOURCE_NOT_FOUND::toException);
        Long expected=delivery.getOutcomeId()!=null ? eligibleBeneficiary(campaign.getId(),delivery.getOutcomeId())
                : rankingBeneficiary(campaign.getId(),delivery);
        if(!delivery.getBeneficiaryUserId().equals(expected)) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        return delivery;
    }

    private Campaign authorizedCampaign(Long actorId,String publicId){
        if(actorId==null || publicId==null || publicId.isBlank()) throw ErrorCode.VALIDATION_ERROR.toException();
        var campaign=campaigns.findLockedByPublicId(publicId).orElseThrow(ErrorCode.CAMPAIGN_NOT_FOUND::toException);
        if(jdbc.queryForList("SELECT id FROM users WHERE id=? AND status='ACTIVE' FOR SHARE",Long.class,actorId).isEmpty()
                || jdbc.queryForList("SELECT id FROM memberships WHERE user_id=? AND organization_id=? AND status='ACTIVE' AND role IN ('CAMPAIGN_MANAGER','SYSTEM_ADMIN') FOR SHARE",Long.class,actorId,campaign.getOrganizationId()).isEmpty()) throw ErrorCode.AUTH_RESOURCE_ACCESS_DENIED.toException();
        return campaign;
    }

    private String prizeTypeFor(Long campaignId,Long outcomeId){
        var types=jdbc.queryForList("SELECT p.type FROM draw_outcomes o JOIN draws d ON d.id=o.draw_id JOIN campaign_prizes p ON p.id=o.prize_id AND p.campaign_id=d.campaign_id WHERE o.id=? AND d.campaign_id=?",String.class,outcomeId,campaignId);
        if(types.size()!=1) throw ErrorCode.PARTICIPATION_NOT_ELIGIBLE.toException();
        return types.getFirst();
    }

    private Long eligibleBeneficiary(Long campaignId,Long outcomeId){        // Contrato interno de fixture DEMO; não constitui regra de apuração real nem substitui a P12.
        var beneficiaries=jdbc.queryForList("SELECT e.user_id FROM draw_outcomes o JOIN draws d ON d.id=o.draw_id JOIN federal_results f ON f.id=d.federal_result_id JOIN participation_entries e ON e.id=o.entry_id AND e.campaign_id=d.campaign_id JOIN entry_number_assignments a ON a.entry_id=e.id JOIN campaign_numbers n ON n.id=a.campaign_number_id AND n.entry_id=e.id AND n.campaign_id=d.campaign_id AND n.number_value=o.winning_number JOIN campaign_prizes p ON p.id=o.prize_id AND p.campaign_id=d.campaign_id WHERE o.id=? AND d.campaign_id=? AND o.decision_code='DEMO_MATCH' AND d.status='PUBLISHED' AND f.verified_by_two=TRUE AND e.status='FROZEN' AND e.source='DEMO_SIMULATION' AND n.status='FROZEN' AND p.status='ACTIVE' AND p.type<>'CASH' FOR SHARE",Long.class,outcomeId,campaignId);
        if(beneficiaries.size()!=1) throw ErrorCode.PARTICIPATION_NOT_ELIGIBLE.toException();
        return beneficiaries.getFirst();
    }
    private Long rankingBeneficiary(Long campaignId,AwardDelivery delivery){
        var beneficiaries=jdbc.queryForList("SELECT p.user_id FROM ranking_positions p JOIN ranking_snapshots s ON s.id=p.snapshot_id "
                + "JOIN campaign_prizes z ON z.id=? AND z.campaign_id=s.campaign_id AND z.status='ACTIVE' AND z.type<>'CASH' "
                + "JOIN users u ON u.id=p.user_id AND u.status='ACTIVE' "
                + "WHERE s.id=? AND s.campaign_id=? AND p.position=? FOR SHARE",Long.class,delivery.getPrizeId(),delivery.getRankingSnapshotId(),campaignId,delivery.getRankingPosition());
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
        events.publishEvent(new AwardDeliveryChangedEvent(delivery.getId(),delivery.getStatus()));
    }

    private static String sha256(String value){
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException ex){throw new IllegalStateException("award.hash.unavailable",ex);}
    }

    public record StatusCount(String status,long count) { }

    private record RankingPrize(Long id,String type,int position) { }
    public record RankingAward(int position,Long deliveryId) { }
    /** Sem dados pessoais: posições e ids internos da entrega, para o operador seguir o fluxo P13. */
    public record RankingAwardResult(String snapshotId,RankingScope scope,java.util.List<RankingAward> created,
                                     java.util.List<Integer> vacantPositions,java.util.List<Integer> cashBlockedPositions) {
        public RankingAwardResult { created=java.util.List.copyOf(created); vacantPositions=java.util.List.copyOf(vacantPositions); cashBlockedPositions=java.util.List.copyOf(cashBlockedPositions); }
    }

    public record DeliverySummary(String campaignId,java.util.List<StatusCount> byStatus,String notice) {
        public DeliverySummary { byStatus = java.util.List.copyOf(byStatus); }
    }
}
