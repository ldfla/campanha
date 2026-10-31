package la.meleva.fundraiser.award;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="award_deliveries") public class AwardDelivery {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="outcome_id",nullable=false) private Long outcomeId; @Column(name="beneficiary_user_id",nullable=false) private Long beneficiaryUserId; @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private AwardDeliveryStatus status; @Column(name="delivery_evidence_uri",length=1024) private String deliveryEvidenceUri; @Column(name="delivered_at") private Instant deliveredAt; @Column(name="created_at",nullable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected AwardDelivery() {} public AwardDelivery(Long outcomeId,Long beneficiaryUserId){this.outcomeId=java.util.Objects.requireNonNull(outcomeId,"outcomeId");this.beneficiaryUserId=java.util.Objects.requireNonNull(beneficiaryUserId,"beneficiaryUserId");this.status=AwardDeliveryStatus.PENDING_VERIFICATION;this.createdAt=Instant.now();this.updatedAt=createdAt;} public Long getId(){return id;}
 public Long getOutcomeId(){return outcomeId;} public Long getBeneficiaryUserId(){return beneficiaryUserId;} public AwardDeliveryStatus getStatus(){return status;}
 void transitionTo(AwardDeliveryStatus target){AwardDeliveryStateMachine.validate(status,target);this.status=target;this.updatedAt=Instant.now();}
}
