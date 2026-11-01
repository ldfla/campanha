package la.meleva.fundraiser.award;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

@Entity @Table(name="award_deliveries") public class AwardDelivery {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="outcome_id") private Long outcomeId; @Column(name="ranking_snapshot_id") private Long rankingSnapshotId; @Column(name="ranking_position") private Integer rankingPosition; @Column(name="prize_id") private Long prizeId; @Column(name="beneficiary_user_id",nullable=false) private Long beneficiaryUserId; @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private AwardDeliveryStatus status; @Column(name="delivery_evidence_uri",length=1024) private String deliveryEvidenceUri; @Column(name="delivery_evidence_sha256",length=64) private String deliveryEvidenceSha256; @Column(name="delivered_at") private Instant deliveredAt; @Column(name="disputed_at") private Instant disputedAt; @Column(name="dispute_reason",length=500) private String disputeReason; @Column(name="created_at",nullable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt;

 protected AwardDelivery() {}
 public AwardDelivery(Long outcomeId,Long beneficiaryUserId){this.outcomeId=Objects.requireNonNull(outcomeId,"outcomeId");this.beneficiaryUserId=Objects.requireNonNull(beneficiaryUserId,"beneficiaryUserId");this.status=AwardDeliveryStatus.PENDING_VERIFICATION;this.createdAt=Instant.now();this.updatedAt=createdAt;}
 /** P05A-09: entrega originada de posição premiada de um snapshot de ranking lacrado. */
 public static AwardDelivery forRankingPosition(Long rankingSnapshotId,int rankingPosition,Long prizeId,Long beneficiaryUserId){
     var delivery=new AwardDelivery();
     delivery.rankingSnapshotId=Objects.requireNonNull(rankingSnapshotId,"rankingSnapshotId");
     if(rankingPosition<1) throw new IllegalArgumentException("award.ranking_position_invalid");
     delivery.rankingPosition=rankingPosition; delivery.prizeId=Objects.requireNonNull(prizeId,"prizeId");
     delivery.beneficiaryUserId=Objects.requireNonNull(beneficiaryUserId,"beneficiaryUserId");
     delivery.status=AwardDeliveryStatus.PENDING_VERIFICATION; delivery.createdAt=Instant.now(); delivery.updatedAt=delivery.createdAt;
     return delivery;
 }
 public Long getId(){return id;}
 public Long getRankingSnapshotId(){return rankingSnapshotId;} public Integer getRankingPosition(){return rankingPosition;} public Long getPrizeId(){return prizeId;}
 public Long getOutcomeId(){return outcomeId;} public Long getBeneficiaryUserId(){return beneficiaryUserId;} public AwardDeliveryStatus getStatus(){return status;}
 public String getDeliveryEvidenceUri(){return deliveryEvidenceUri;} public String getDeliveryEvidenceSha256(){return deliveryEvidenceSha256;}
 public Instant getDeliveredAt(){return deliveredAt;} public Instant getDisputedAt(){return disputedAt;} public String getDisputeReason(){return disputeReason;}
 void transitionTo(AwardDeliveryStatus target){AwardDeliveryStateMachine.validate(status,target);this.status=target;this.updatedAt=Instant.now();}
 void deliver(String evidenceUri,String evidenceSha256,Instant when){
     Objects.requireNonNull(evidenceUri,"evidenceUri"); Objects.requireNonNull(evidenceSha256,"evidenceSha256"); Objects.requireNonNull(when,"when");
     if(evidenceUri.isBlank()) throw new IllegalArgumentException("award.evidence.blank");
     transitionTo(AwardDeliveryStatus.DELIVERED);
     this.deliveryEvidenceUri=evidenceUri; this.deliveryEvidenceSha256=evidenceSha256; this.deliveredAt=when;
 }
 void dispute(Instant when,String reason){
     Objects.requireNonNull(when,"when"); Objects.requireNonNull(reason,"reason");
     if(reason.isBlank() || reason.strip().length()>500) throw new IllegalArgumentException("award.dispute.reason_invalid");
     transitionTo(AwardDeliveryStatus.DISPUTED);
     this.disputedAt=when; this.disputeReason=reason.strip();
 }
}
