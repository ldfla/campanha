package la.meleva.fundraiser.ranking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ranking_snapshots")
public class RankingSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, updatable = false, length = 36)
    private String publicId;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "ranking_rule_id", nullable = false)
    private Long rankingRuleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RankingScope scope;

    @Column(name = "cutoff_at", nullable = false)
    private Instant cutoffAt;

    @Column(name = "snapshot_hash", nullable = false, length = 64)
    private String snapshotHash;

    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "entry_count", nullable = false)
    private int entryCount;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    protected RankingSnapshot() {}

    public RankingSnapshot(Long campaignId, Long rankingRuleId, RankingScope scope, Instant cutoffAt,
                           Instant generatedAt, String payloadJson, String snapshotHash, int entryCount) {
        this.publicId = UUID.randomUUID().toString();
        this.campaignId = campaignId;
        this.rankingRuleId = rankingRuleId;
        this.scope = scope;
        this.cutoffAt = cutoffAt;
        this.generatedAt = generatedAt;
        this.payloadJson = payloadJson;
        this.snapshotHash = snapshotHash;
        this.entryCount = entryCount;
    }

    public Long getId() { return id; }

    public String getPublicId() { return publicId; }

    public Long getCampaignId() { return campaignId; }

    public Long getRankingRuleId() { return rankingRuleId; }

    public RankingScope getScope() { return scope; }

    public Instant getCutoffAt() { return cutoffAt; }

    public String getSnapshotHash() { return snapshotHash; }

    public String getPayloadJson() { return payloadJson; }

    public int getEntryCount() { return entryCount; }

    public Instant getGeneratedAt() { return generatedAt; }
}
