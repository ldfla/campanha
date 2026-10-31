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

@Entity
@Table(name = "ranking_rules")
public class RankingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RankingScope scope;

    @Column(name = "rule_version", nullable = false)
    private int ruleVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RankingMetric metric;

    @Column(name = "tie_breaker", nullable = false, length = 100)
    private String tieBreaker;

    @Column(name = "legally_approved", nullable = false)
    private boolean legallyApproved;

    @Column(name = "effective_at", nullable = false)
    private Instant effectiveAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RankingRule() {}

    public RankingRule(Long campaignId, RankingScope scope, int ruleVersion, RankingMetric metric,
                       String tieBreaker, boolean legallyApproved, Instant effectiveAt) {
        if (scope == null || metric == null || !metric.appliesTo(scope)) {
            throw new IllegalArgumentException("Métrica incompatível com o escopo do ranking");
        }
        if (tieBreaker == null || tieBreaker.isBlank()) {
            throw new IllegalArgumentException("Desempate obrigatório");
        }
        this.campaignId = campaignId;
        this.scope = scope;
        this.ruleVersion = ruleVersion;
        this.metric = metric;
        this.tieBreaker = tieBreaker;
        this.legallyApproved = legallyApproved;
        this.effectiveAt = effectiveAt;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }

    public Long getCampaignId() { return campaignId; }

    public RankingScope getScope() { return scope; }

    public int getRuleVersion() { return ruleVersion; }

    public RankingMetric getMetric() { return metric; }

    public String getTieBreaker() { return tieBreaker; }

    public boolean isLegallyApproved() { return legallyApproved; }

    public Instant getEffectiveAt() { return effectiveAt; }
}
