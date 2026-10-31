package la.meleva.fundraiser.participation;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * Registro da carga inicial de prêmios: guarda o hash do arquivo canônico das colunas de
 * inicialização (número, prêmio, timestamp) para conferência a qualquer momento.
 */
@Entity
@Table(name = "campaign_prize_loads")
public class CampaignPrizeLoad {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "campaign_id", nullable = false, updatable = false) private Long campaignId;
    @Column(name = "number_min", nullable = false) private int numberMin;
    @Column(name = "number_max", nullable = false) private int numberMax;
    @Column(name = "tier1_count", nullable = false) private int tier1Count;
    @Column(name = "tier2_count", nullable = false) private int tier2Count;
    @Column(name = "tier1_cents", nullable = false) private long tier1Cents;
    @Column(name = "tier2_cents", nullable = false) private long tier2Cents;
    @Column(name = "defined_at", nullable = false) private Instant definedAt;
    @Column(name = "file_sha256", nullable = false, length = 64) private String fileSha256;
    @Column(name = "generated_at", nullable = false) private Instant generatedAt;

    protected CampaignPrizeLoad() {}

    public CampaignPrizeLoad(Long campaignId, int numberMin, int numberMax, int tier1Count, int tier2Count,
                             long tier1Cents, long tier2Cents, Instant definedAt, String fileSha256, Instant generatedAt) {
        this.campaignId = Objects.requireNonNull(campaignId, "campaignId");
        this.numberMin = numberMin; this.numberMax = numberMax;
        this.tier1Count = tier1Count; this.tier2Count = tier2Count;
        this.tier1Cents = tier1Cents; this.tier2Cents = tier2Cents;
        this.definedAt = Objects.requireNonNull(definedAt, "definedAt");
        this.fileSha256 = Objects.requireNonNull(fileSha256, "fileSha256");
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
    }

    public Long getId() { return id; }
    public Long getCampaignId() { return campaignId; }
    public int getNumberMin() { return numberMin; }
    public int getNumberMax() { return numberMax; }
    public int getTier1Count() { return tier1Count; }
    public int getTier2Count() { return tier2Count; }
    public long getTier1Cents() { return tier1Cents; }
    public long getTier2Cents() { return tier2Cents; }
    public Instant getDefinedAt() { return definedAt; }
    public String getFileSha256() { return fileSha256; }
    public Instant getGeneratedAt() { return generatedAt; }
}
