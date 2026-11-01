package la.meleva.fundraiser.drawing;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import la.meleva.fundraiser.shared.ErrorCode;

@Entity @Table(name="draws") public class Draw {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="public_id",nullable=false,updatable=false) private String publicId; @Column(name="campaign_id",nullable=false) private Long campaignId; @Column(name="draw_rule_id",nullable=false) private Long drawRuleId; @Column(name="federal_result_id") private Long federalResultId; @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private DrawStatus status; @Column(name="frozen_at") private Instant frozenAt; @Column(name="published_at") private Instant publishedAt; @Column(name="entries_merkle_root_or_hash",length=64) private String entriesMerkleRootOrHash; @Column(name="created_at",nullable=false) private Instant createdAt; @Column(name="updated_at",nullable=false) private Instant updatedAt;
 protected Draw() {}
 public Draw(Long campaignId,Long drawRuleId){this.publicId=UUID.randomUUID().toString();this.campaignId=campaignId;this.drawRuleId=drawRuleId;this.status=DrawStatus.PENDING;this.createdAt=Instant.now();this.updatedAt=createdAt;}
 public Long getId(){return id;}
 public String getPublicId(){return publicId;}
 public Long getCampaignId(){return campaignId;}
 public Long getDrawRuleId(){return drawRuleId;}
 public Long getFederalResultId(){return federalResultId;}
 public DrawStatus getStatus(){return status;}
 public Instant getFrozenAt(){return frozenAt;}
 public Instant getPublishedAt(){return publishedAt;}
 public String getEntriesHash(){return entriesMerkleRootOrHash;}
 public void freeze(String entriesHash,Instant at){ if(status!=DrawStatus.PENDING) throw ErrorCode.DRAW_STATE_INVALID.toException(); if(entriesHash==null||entriesHash.isBlank()) throw ErrorCode.VALIDATION_ERROR.toException(); this.entriesMerkleRootOrHash=entriesHash; this.frozenAt=at; this.updatedAt=at; }
 public void bindResult(Long federalResultId,Instant at){ if(status!=DrawStatus.PENDING&&status!=DrawStatus.RESULTS_IMPORTED) throw ErrorCode.DRAW_STATE_INVALID.toException(); this.federalResultId=federalResultId; this.status=DrawStatus.RESULTS_IMPORTED; this.updatedAt=at; }
 public void computed(Instant at){ if(status!=DrawStatus.RESULTS_IMPORTED) throw ErrorCode.DRAW_STATE_INVALID.toException(); this.status=DrawStatus.COMPUTED; this.updatedAt=at; }
 public void publish(Instant at){ if(status==DrawStatus.PUBLISHED) throw ErrorCode.DRAW_ALREADY_PUBLISHED.toException(); if(status!=DrawStatus.COMPUTED) throw ErrorCode.DRAW_STATE_INVALID.toException(); this.status=DrawStatus.PUBLISHED; this.publishedAt=at; this.updatedAt=at; }
}
