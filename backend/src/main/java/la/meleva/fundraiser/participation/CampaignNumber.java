package la.meleva.fundraiser.participation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "campaign_numbers")
public class CampaignNumber {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "campaign_id", nullable = false) private Long campaignId;
    @Column(name = "series_code", nullable = false, length = 1) private String seriesCode;
    @Column(name = "number_value", nullable = false) private int numberValue;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private NumberStatus status;
    @Column(name = "entry_id") private Long entryId;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private int version;

    protected CampaignNumber() {}
    public CampaignNumber(Long campaignId, String seriesCode, int numberValue) {
        if (numberValue < 0 || numberValue > 99999) throw new IllegalArgumentException("Número fora do intervalo de cinco dígitos");
        this.campaignId = campaignId; this.seriesCode = seriesCode; this.numberValue = numberValue;
        this.status = NumberStatus.AVAILABLE; this.createdAt = Instant.now(); this.updatedAt = createdAt;
    }
    public Long getId() { return id; }
    public Long getCampaignId() { return campaignId; }
    public String getSeriesCode() { return seriesCode; }
    public int getNumberValue() { return numberValue; }
    public NumberStatus getStatus() { return status; }
    public Long getEntryId() { return entryId; }
    public void assignTo(Long entryId) { this.entryId = entryId; this.status = NumberStatus.ASSIGNED; this.updatedAt = Instant.now(); }
    public void freeze() { if (status == NumberStatus.ASSIGNED) { this.status = NumberStatus.FROZEN; this.updatedAt = Instant.now(); } }
    public void invalidate() { if (status == NumberStatus.FROZEN) { this.status = NumberStatus.INVALIDATED; this.updatedAt = Instant.now(); } }
}
