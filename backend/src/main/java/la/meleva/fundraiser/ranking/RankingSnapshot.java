package la.meleva.fundraiser.ranking;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="ranking_snapshots") public class RankingSnapshot {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="campaign_id",nullable=false) private Long campaignId; @Column(name="ranking_rule_id",nullable=false) private Long rankingRuleId; @Column(name="cutoff_at",nullable=false) private Instant cutoffAt; @Column(name="snapshot_hash",nullable=false,length=64) private String snapshotHash; @Column(name="generated_at",nullable=false) private Instant generatedAt;
 protected RankingSnapshot() {} public RankingSnapshot(Long campaignId,Long rankingRuleId,Instant cutoffAt,String snapshotHash){this.campaignId=campaignId;this.rankingRuleId=rankingRuleId;this.cutoffAt=cutoffAt;this.snapshotHash=snapshotHash;this.generatedAt=Instant.now();} public Long getId(){return id;}
}
