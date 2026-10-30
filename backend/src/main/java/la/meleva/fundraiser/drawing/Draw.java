package la.meleva.fundraiser.drawing;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="draws") public class Draw {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="public_id",nullable=false,updatable=false) private String publicId; @Column(name="campaign_id",nullable=false) private Long campaignId; @Column(name="draw_rule_id",nullable=false) private Long drawRuleId; @Column(name="federal_result_id") private Long federalResultId; @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private DrawStatus status; @Column(name="frozen_at") private Instant frozenAt; @Column(name="entries_merkle_root_or_hash",length=64) private String entriesMerkleRootOrHash; @Column(name="created_at",nullable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected Draw() {} public Draw(Long campaignId,Long drawRuleId){this.publicId=UUID.randomUUID().toString();this.campaignId=campaignId;this.drawRuleId=drawRuleId;this.status=DrawStatus.PENDING;this.createdAt=Instant.now();this.updatedAt=createdAt;} public Long getId(){return id;}
}
