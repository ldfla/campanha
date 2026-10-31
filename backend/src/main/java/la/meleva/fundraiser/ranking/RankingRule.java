package la.meleva.fundraiser.ranking;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="ranking_rules") public class RankingRule {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="campaign_id",nullable=false) private Long campaignId; @Column(name="rule_version",nullable=false) private int ruleVersion; @Column(nullable=false,length=50) private String metric; @Column(name="tie_breaker",nullable=false) private String tieBreaker; @Column(name="legally_approved",nullable=false) private boolean legallyApproved; @Column(name="effective_at",nullable=false) private Instant effectiveAt; @Column(name="created_at",nullable=false) private Instant createdAt;
 protected RankingRule() {} public RankingRule(Long campaignId,int ruleVersion,String metric,String tieBreaker,boolean legallyApproved,Instant effectiveAt){this.campaignId=campaignId;this.ruleVersion=ruleVersion;this.metric=metric;this.tieBreaker=tieBreaker;this.legallyApproved=legallyApproved;this.effectiveAt=effectiveAt;this.createdAt=Instant.now();} public Long getId(){return id;}
}
